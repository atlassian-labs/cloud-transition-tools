package com.atlassian.ctt.service

import com.atlassian.ctt.integrations.api.APIParser
import com.atlassian.ctt.integrations.api.APIParts
import com.atlassian.ctt.integrations.api.apiBodyParams
import com.atlassian.ctt.integrations.url.URLParts
import com.fasterxml.jackson.core.JsonParseException
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class APISanitisationService(
    private val ctt: CTTService,
    private val apiParser: APIParser,
    private val urlService: URLSanitisationService,
) {
    private val logger = KotlinLogging.logger(this::class.java.name)

    fun sanitiseAPI(
        url: String,
        body: String?,
    ): Map<String, Any> {
        val apiParts: APIParts
        try {
            apiParts = apiParser.parseAPI(url, body)
        } catch (e: JsonParseException) {
            logger.error(e) { "Error parsing API: $url $body" }
            throw e
        }

        val urlParts = apiParts.urlParts
        val bodyParts = apiParts.body

        // sanitise url
        val cloudURL: URLParts = urlService.sanitiseURLParts(urlParts)

        // sanitise request body
        val cloudBody = sanitiseRequestBodyParts(urlParts.baseURL, bodyParts)

        return apiParser.serialiseAPIParts(APIParts(cloudURL, cloudBody))
    }

    private fun translateEntity(
        serverBaseUrl: String,
        key: String,
        id: Number,
    ): Number {
        val entity = apiParser.sanitiseKey(key)
        val ari =
            apiBodyParams[entity]?.value ?: run {
                logger.warn { "Skipping translation as no ARI mapping found for entity: $entity" }
                return id
            }

        val cloudID = ctt.translateServerIdToCloudId(serverBaseUrl, ari, id.toLong()).cloudId
        return cloudID.takeIf { it != 0L } ?: run {
            logger.warn { "Failed to translate server ID $id for entity $entity as there is no mapping." }
            id
        }
    }

    private fun sanitiseRequestBodyParts(
        serverBaseUrl: String,
        bodyParts: Map<String, Any>,
    ): Map<String, Any> {
        val sanitisedBody =
            bodyParts
                .map { (key, value) ->
                    val customFieldKey = apiParser.keyAsCustomField(key)
                    if (customFieldKey != null) {
                        val (customField, customFieldId) = customFieldKey
                        val cloudCustomField =
                            customField + translateEntity(serverBaseUrl, customField, customFieldId.toLong()).toString()
                        return@map cloudCustomField to value
                    }
                    when (value) {
                        is String -> key to value
                        is Number -> key to translateEntity(serverBaseUrl, key, value)
                        is List<*> -> {
                            val sanitisedList =
                                value.map {
                                    when (it) {
                                        is Number -> translateEntity(serverBaseUrl, key, it)
                                        else -> it
                                    }
                                }
                            key to sanitisedList
                        }
                        else -> {
                            key to value
                        }
                    }
                }.toMap()

        return sanitisedBody
    }
}
