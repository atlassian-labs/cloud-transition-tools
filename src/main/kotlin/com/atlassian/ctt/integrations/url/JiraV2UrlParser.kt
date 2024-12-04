package com.atlassian.ctt.integrations.url

import org.springframework.stereotype.Component
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder

/*
* URL Parser implementation for Version 2 of Jira Server/Data Center API
* Note that the entity(path param) is always followed by its value which is the case in Version 2.0 of Jira DC API.
* Refer: https://developer.atlassian.com/server/jira/platform/rest/v10000/
* urls for Jira's REST API resource have the following structure:
* http://host:port/context/rest/api-name/api-version/resource-name/resource-value
*/

// Major version that we support.
// Minor versions are ignored and assumed to be compatible

const val API_PATH_SEGMENT_COUNT = 3
const val API_TYPE = "rest"
val API_VERSION_MAP =
    mapOf(
        "api" to 2,
        "auth" to 1,
        "agile" to 1,
    )

@Component
class JiraV2URLParser : URLParser {
    @Throws(MalformedURLException::class)
    override fun parseURL(urlString: String): URLParts {
        val invalidUrlMessage = "Invalid URL"
        val url = URL(URLDecoder.decode(urlString, "UTF-8"))
        if (url.protocol == null || url.authority == null) {
            throw MalformedURLException(invalidUrlMessage + "URL does not contain a valid scheme or authority")
        }
        val baseUrl = url.protocol + "://" + url.authority
        val pathSegments = url.path.split("/").filter { it.isNotEmpty() }

        if (pathSegments.size < API_PATH_SEGMENT_COUNT) {
            throw MalformedURLException(invalidUrlMessage + "URL does not contain enough path segments")
        }
        val (apiType, apiName, apiVersion) = pathSegments.take(API_PATH_SEGMENT_COUNT)
        if (apiType != API_TYPE) {
            throw MalformedURLException(invalidUrlMessage + "URL Sanitisation is only supported for REST APIs")
        }

        if (apiName !in API_VERSION_MAP || apiVersion.toDoubleOrNull()?.toInt() != API_VERSION_MAP[apiName]) {
            throw MalformedURLException(
                invalidUrlMessage + "URL Sanitisation is not supported for the given API version",
            )
        }
        val apiPath = pathSegments.take(API_PATH_SEGMENT_COUNT).joinToString("/")
        val urlParams = pathSegments.drop(API_PATH_SEGMENT_COUNT)
        val queryParams =
            url.query?.split("&")?.map {
                val (key, value) = it.split("=", limit = 2)
                key to value
            } ?: emptyList()
        return URLParts(baseUrl, apiPath, urlParams, queryParams)
    }
}
