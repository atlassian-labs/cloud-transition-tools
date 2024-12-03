package com.atlassian.ctt.service

import com.atlassian.ctt.integrations.url.JiraV2URLParamsARIMap
import com.atlassian.ctt.integrations.url.JiraV2URLQueryParamsARIMap
import com.atlassian.ctt.integrations.url.URLParser
import com.atlassian.ctt.integrations.url.URLParts
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URISyntaxException

/*
URL Sanitisation Service.
Sanitises Server URL to Cloud URL by identifying and translating all integer IDs to cloud IDs using CTTService
 */
@Service
class URLSanitisationService(
    private val ctt: CTTService,
    private val urlParser: URLParser,
) {
    private val logger = KotlinLogging.logger(this::class.java.name)

    private fun isIntegerID(value: String) = value.matches(Regex("^[0-9]+$"))

    private fun isJQLQueryField(key: String): Boolean {
        val jqls = listOf("jql", "query", "currentJQL")
        return jqls.contains(key)
    }

    private fun sanitisePathParams(
        serverBaseUrl: String,
        pathParams: List<String>,
    ): List<String> {
        if (pathParams.none { isIntegerID(it) }) {
            logger.info { "URL path params do not contain any integer ID, skipping sanitisation" }
            return pathParams
        }

        // iterate and check for integer params, when found, go back to get entity
        val sanitisedPathParams =
            pathParams.mapIndexed { index, value ->
                if (!isIntegerID(value)) {
                    return@mapIndexed value
                }

                // Note: Assuming immediate preceding path param is the entity, this is true for supported entities
                // cases where multiple path elements form an entity are not supported
                // Example: /rest/api/2/issue/createmeta/{issueId} is an invalid URL in our case.
                // For now there is no requirement for this as we do not support such cases.
                val entity =
                    pathParams.getOrNull(index - 1) ?: run {
                        logger.warn { "No preceding path param found for index $index, skipping translation" }
                        return@mapIndexed value
                    }

                // lookup and translate the id
                val ari =
                    JiraV2URLParamsARIMap[entity]?.value ?: run {
                        logger.warn { "Skipping translation as no ARI mapping found for entity: $entity" }
                        return@mapIndexed value
                    }

                val cloudID = ctt.translateServerIdToCloudId(serverBaseUrl, ari, value.toLong()).cloudId
                cloudID.takeIf { it != 0L }?.toString() ?: run {
                    logger.warn { "Failed to translate server ID $value for entity $entity as there is no mapping." }
                    value
                }
            }

        return sanitisedPathParams
    }

    private fun sanitiseQueryParams(
        serverBaseUrl: String,
        queryParams: List<Pair<String, String>>,
    ): List<Pair<String, String>> {
        val sanitisedQueryParams =
            queryParams.map { (key, value) ->
                if (isJQLQueryField(key)) {
                    TODO("JSWM-2510: JQL sanitisation is not supported yet")
                    // return@map key to value
                }

                if (!isIntegerID(value)) {
                    return@map key to value
                }

                val ari =
                    JiraV2URLQueryParamsARIMap[key]?.value ?: run {
                        logger.warn { "Skipping translation as no ARI mapping found for query param: $key" }
                        return@map key to value
                    }

                val cloudID = ctt.translateServerIdToCloudId(serverBaseUrl, ari, value.toLong()).cloudId
                val cloudValue =
                    cloudID.takeIf { it != 0L }?.toString() ?: run {
                        logger.warn {
                            "Failed to translate server ID $value for query param $key as there is no mapping."
                        }
                        return@map key to value
                    }

                return@map key to cloudValue
            }

        return sanitisedQueryParams
    }

    fun sanitiseURL(serverURL: String): String {
        val url: URLParts
        try {
            url = urlParser.parseURL(serverURL)
        } catch (e: URISyntaxException) {
            logger.error(e) { "Failed to parse URL: $serverURL" }
            throw e
        }
        return sanitiseURLParts(url).toString()
    }

    fun sanitiseURLParts(url: URLParts): URLParts {
        val cloudPathParams = sanitisePathParams(url.baseURL, url.pathParams)
        val cloudQueryParams = sanitiseQueryParams(url.baseURL, url.queryParams)

        return URLParts(ctt.cloudURL, url.apiPath, cloudPathParams, cloudQueryParams)
    }
}
