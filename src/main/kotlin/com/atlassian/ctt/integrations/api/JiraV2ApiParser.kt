package com.atlassian.ctt.integrations.api

import com.atlassian.ctt.integrations.url.JiraV2URLParser
import com.atlassian.ctt.integrations.url.URLParser
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.jvm.Throws

class JiraV2ApiParser(
    private val urlParser: URLParser = JiraV2URLParser(),
) : APIParser {
    @Throws(JsonParseException::class)
    override fun parseAPI(
        url: String,
        bodyJson: String?,
    ): APIParts {
        val urlParts = urlParser.parseURL(url)
        val requestBody: Map<String, Any>
        try {
            requestBody = jacksonObjectMapper().readValue(if (bodyJson.isNullOrEmpty()) "{}" else bodyJson)
        } catch (e: JsonParseException) {
            throw JsonParseException("Failed to parse API Request Body.: ${e.message}")
        }

        val flattenedBody = flattenJson(requestBody)
        return APIParts(urlParts, flattenedBody)
    }

    /*
     * Convert the custom field key into a pair of the prefix and the custom field number
     * Returns null if the key is not a custom field key
     */
    override fun keyAsCustomField(key: String): Pair<String, Number>? {
        val customFieldIdentifier = "customfield_"
        val matchResult = Regex("(.*$customFieldIdentifier)(\\d+)").find(key)
        return matchResult?.let {
            it.groupValues[1] to it.groupValues[2].toLong()
        }
    }

    /*
     * Sanitise the key by removing unwanted prefixes.
     * Say for example fields.summary => summary
     * This is done for proper entity lookup
     */
    override fun sanitiseKey(key: String): String = key.replaceFirst("fields.", "")

    /*
     * Flatten the json body for proper entity lookup
     * Say fields : { project : 1000 }  => fields.project : 1000
     */
    private fun flattenJson(
        map: Map<String, Any>,
        parentKey: String = "",
        result: MutableMap<String, Any> = mutableMapOf(),
    ): Map<String, Any> {
        for ((key, value) in map) {
            val newKey = if (parentKey.isEmpty()) key else "$parentKey.$key"
            if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                flattenJson(value as Map<String, Any>, newKey, result)
            } else {
                result[newKey] = value
            }
        }
        return result
    }

    /* Unflatten the Json body for serialisation
     * Say  fields.project : 1000 => fields : { project : 1000 }
     */
    private fun unflattenJson(map: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for ((key, value) in map) {
            val keys = key.split(".")
            var currentMap = result
            for (i in 0 until keys.size - 1) {
                val k = keys[i]
                if (k !in currentMap) {
                    currentMap[k] = mutableMapOf<String, Any>()
                }
                @Suppress("UNCHECKED_CAST")
                currentMap = currentMap[k] as MutableMap<String, Any>
            }
            currentMap[keys.last()] = value
        }
        return result
    }

    // Serialise API parts for creating the response
    override fun serialiseAPIParts(api: APIParts): Map<String, Any> =
        mapOf(
            "url" to api.urlParts.toString(),
            "body" to unflattenJson(api.body),
        )
}
