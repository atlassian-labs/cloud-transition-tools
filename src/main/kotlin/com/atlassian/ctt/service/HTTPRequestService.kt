package com.atlassian.ctt.service

import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.springframework.stereotype.Service

// Create library-independent types for loose coupling
typealias HTTPCredentials = okhttp3.Credentials

data class HTTPResponse(
    private val response: Response,
) {
    companion object {
        const val STATUS_OK = 200
        const val STATUS_ACCEPTED = 202
    }

    fun isOk() = response.code == STATUS_OK

    fun isAccepted() = response.code == STATUS_ACCEPTED

    fun isError() = !isOk() && !isAccepted()

    fun contentType() = response.header("Content-Type")

    fun text() = response.body?.string()

    fun status() = response.code

    fun byteStream() = response.body?.byteStream()
}

/*
 * Service to make HTTP requests
 */
@Service
class HTTPRequestService(
    private val client: OkHttpClient = OkHttpClient(),
) {
    private val logger = KotlinLogging.logger(this::class.java.name)

    fun get(
        url: String,
        headers: Map<String, String>,
    ): HTTPResponse {
        val request = Request.Builder().url(url)
        headers.forEach { (key, value) -> request.header(key, value) }

        val response: Response = client.newCall(request.build()).execute()
        return HTTPResponse(response)
    }

    fun post(
        url: HttpUrl,
        headers: Map<String, String>,
    ) {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val request = requestBuilder.post("".toRequestBody(null)).build()

        // Execute the request
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.warn { "Request failed: ${response.code} " }
            } else {
                logger.warn { "Response: ${response.body?.string()}" }
            }
        }
    }
}
