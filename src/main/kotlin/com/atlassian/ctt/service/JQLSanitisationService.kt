package com.atlassian.ctt.service

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.integrations.jql.IdentifierSet
import com.atlassian.ctt.integrations.jql.IdentifierType
import com.atlassian.ctt.integrations.jql.IdentifierTypeMapping.typeToAriMap
import com.atlassian.ctt.integrations.jql.JQLSanitisationLibrary
import com.atlassian.ctt.integrations.jql.ParseCancellationException
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class JQLSanitisationService(
    private val ctt: CTTService,
    private val jqlSanitisationLibrary: JQLSanitisationLibrary,
) {
    private val logger = KotlinLogging.logger(this::class.java.name)

    /**
     * Sanitises the given JQL string by extracting and replacing identifiers.
     *
     * @param serverBaseURL Base URL of the server.
     * @param jql JQL query to sanitise.
     * @return Sanitised JQL string.
     */
    fun sanitiseJQL(
        serverBaseURL: String,
        jql: String,
    ): String {
        var sanitisedJql = jql
        try {
            logger.info { "Sanitising JQL: $jql" }
            val extractedIdentifiers = jqlSanitisationLibrary.sanitiser.extractIdentifiers(jql)
            logger.info { "JqlLibraryCall: Extracted identifiers $extractedIdentifiers" }

            val mappedIdentifiers =
                getIdentifiersMap(
                    extractedIdentifiers,
                    getMappingsFromIdentifiers(extractedIdentifiers, serverBaseURL),
                )
            logger.info { "JqlLibraryCall: Sanitising JQL: $jql" }
            sanitisedJql =
                jqlSanitisationLibrary.sanitiser.sanitiseJql(
                    jql,
                    mappedIdentifiers,
                )
            logger.info { "JqlLibraryCall: Sanitised JQL: $sanitisedJql" }
        } catch (e: ParseCancellationException) {
            logger.error(e) { "Error parsing JQL. Not sanitising JQL " }
        }
        return sanitisedJql
    }

    /**
     * Converts extracted identifiers and mappings to a structured map.
     *
     * @param extractedIdentifiers List of extracted identifiers.
     * @param mappings List of migration mappings.
     * @return Map of identifier types to their server-to-cloud ID mappings.
     */
    private fun getIdentifiersMap(
        extractedIdentifiers: List<IdentifierSet>,
        mappings: List<MigrationMapping>,
    ): Map<IdentifierType, Map<String, String>> {
        val identifiersMap = mutableMapOf<IdentifierType, MutableMap<String, String>>()

        extractedIdentifiers.forEach { identifierSet ->
            processIdentifierSet(identifierSet, mappings, identifiersMap)
        }

        return identifiersMap
    }

    /**
     * Processes a single `IdentifierSet` by mapping its identifiers to cloud IDs based on the provided mappings.
     * If the `IdentifierSet` contains unsupported or empty identifiers, the method logs a warning and skips processing.
     *
     * @param identifierSet The `IdentifierSet` containing the type and a set of server identifiers to be processed.
     * @param mappings A list of `MigrationMapping` containing server-to-cloud ID mappings.
     * @param identifiersMap The mutable map where the processed mappings will be added.
     */
    private fun processIdentifierSet(
        identifierSet: IdentifierSet,
        mappings: List<MigrationMapping>,
        identifiersMap: MutableMap<IdentifierType, MutableMap<String, String>>,
    ) {
        val identifierType = identifierSet.type
        val identifiers = identifierSet.identifiers

        if (identifiers.isEmpty()) {
            logger.warn { "Empty Identifiers for: $identifierType, skipping." }
            return
        }

        val ari = typeToAriMap[identifierType]
        if (ari == null) {
            logger.warn { "Unsupported IdentifierType: $identifierType, skipping." }
            return
        }

        val relevantMappings = mappings.filter { it.entityType == ari.value }
        relevantMappings.forEach { mapping ->
            addMappingToIdentifiersMap(identifierType, identifiers, mapping, identifiersMap)
        }
    }

    /**
     * Retrieves migration mappings for a list of extracted identifiers.
     *
     * @param extractedIdentifiers List of extracted identifiers.
     * @param serverURL Server base URL.
     * @return List of migration mappings.
     */
    private fun getMappingsFromIdentifiers(
        extractedIdentifiers: List<IdentifierSet>,
        serverURL: String,
    ): List<MigrationMapping> {
        return extractedIdentifiers.flatMap { identifierSet ->
            // Get the entityType from the identifierTypeToEntityType map
            val entityType = typeToAriMap[identifierSet.type]

            if (entityType == null) {
                logger.warn { "EntityType for ${identifierSet.type} not found, skipping." }
                return@flatMap emptyList<MigrationMapping>()
            }

            // For each identifier in the identifiers set, we call translateServerIdToCloudId
            identifierSet.identifiers.map { serverId ->
                val cloudId =
                    ctt
                        .translateServerIdToCloudId(
                            serverURL,
                            entityType.value,
                            serverId.toLong(),
                        ).cloudId
                        .let {
                            if (it == 0L) {
                                logger.warn {
                                    "Failed to translate serverId $serverId to cloudId. Falling back to serverId."
                                }
                                serverId.toLong()
                            } else {
                                it
                            }
                        }
                MigrationMapping(
                    serverUrl = serverURL,
                    entityType = entityType.value,
                    serverId = serverId.toLong(),
                    cloudId = cloudId,
                )
            }
        }
    }

    /**
     * Adds server-to-cloud ID mappings for a given identifier type and mapping.
     *
     * @param identifierType The type of the identifier.
     * @param identifiers The set of server IDs for this identifier type.
     * @param mapping The migration mapping containing the cloud ID.
     * @param identifiersMap The main identifiers map to update.
     */
    private fun addMappingToIdentifiersMap(
        identifierType: IdentifierType,
        identifiers: Set<String>,
        mapping: MigrationMapping,
        identifiersMap: MutableMap<IdentifierType, MutableMap<String, String>>,
    ) {
        // Get the inner mapping for the identifierType or create it if it doesn't exist
        val innerMapping = identifiersMap.getOrPut(identifierType) { mutableMapOf() }

        // Update the inner mapping for each serverId
        identifiers.forEach { serverId ->
            run {
                if (serverId.toLong() == mapping.serverId) {
                    innerMapping[serverId] = mapping.cloudId.toString()
                    logger.info {
                        "Updated mapping for identifierType $identifierType: " +
                            " serverId $serverId -> cloudId ${mapping.cloudId}"
                    }
                }
            }
        }
    }
}
