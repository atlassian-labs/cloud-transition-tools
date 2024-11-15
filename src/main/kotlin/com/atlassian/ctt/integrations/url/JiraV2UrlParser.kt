package com.atlassian.ctt.integrations.url

import org.springframework.stereotype.Component
import java.net.URI
import java.net.URISyntaxException
import java.util.*

/*
* URL Parser implementation for Version 2 of Jira Server/Data Center API
* Note that the assumption here is that the entity(path param) is always followed by its value which is the case in Version 2.0 of Jira DC API
* Refer: https://developer.atlassian.com/server/jira/platform/rest/v10000/
* URIs for Jira's REST API resource have the following structure:
* http://host:port/context/rest/api-name/api-version/resource-name/resource-value
*/

// Major version that we support.
// Minor versions are ignored and assumed to be compatible
val apiVersionMap =
    mapOf(
        "api" to 2,
        "auth" to 1,
        "agile" to 1,
    )

@Component
class JiraV2URLParser : URLParser {
    @Throws(URISyntaxException::class)
    override fun parseURL(url: String): URLParts {
        val uri = URI(url)
        if (uri.scheme == null || uri.authority == null) {
            throw URISyntaxException("Invalid URL", "URL does not contain a valid scheme or authority")
        }
        val baseUrl = uri.scheme + "://" + uri.authority
        val pathSegments = uri.path.split("/").filter { it.isNotEmpty() }

        if (pathSegments.size < 3) {
            throw URISyntaxException("Invalid URL", "URL does not contain enough path segments")
        }
        val (apiType, apiName, apiVersion) = pathSegments.take(3)
        if (apiType != "rest") {
            throw URISyntaxException("Invalid URL", "URL Sanitisation is only supported for REST APIs")
        }

        if (apiName !in apiVersionMap || apiVersion.toDoubleOrNull()?.toInt() != apiVersionMap[apiName]) {
            throw URISyntaxException("Invalid URL", "URL Sanitisation is not supported for the given API version")
        }
        val apiPath = pathSegments.take(3).joinToString("/")
        val urlParams = pathSegments.drop(3)
        val queryParams =
            uri.query?.split("&")?.map {
                val (key, value) = it.split("=")
                key to value
            } ?: emptyList()
        return URLParts(baseUrl, apiPath, urlParams, queryParams)
    }
}
