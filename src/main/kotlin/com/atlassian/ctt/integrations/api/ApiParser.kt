package com.atlassian.ctt.integrations.api

import com.atlassian.ctt.integrations.url.URLParts
import com.fasterxml.jackson.core.JsonParseException
import kotlin.jvm.Throws

data class APIParts(
    val urlParts: URLParts,
    val body: Map<String, Any>,
)

/* API Parser interface
 * Parser given API(url, body) into APIParts. Also gives interface methods for serialising and sanitising keys
 * Concrete implementations can be provided for different API versions, for example JiraV2APIParser
 */
interface APIParser {
    // Parse the given API into APIParts
    @Throws(JsonParseException::class)
    fun parseAPI(
        url: String,
        bodyJson: String?,
    ): APIParts

    // Serialise API Parts into a map
    fun serialiseAPIParts(api: APIParts): Map<String, Any>

    // convert a custom field key to a pair of key and value
    fun keyAsCustomField(key: String): Pair<String, Number>?

    // Sanitise key
    fun sanitiseKey(key: String): String
}
