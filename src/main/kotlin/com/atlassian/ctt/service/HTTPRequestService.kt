package com.atlassian.ctt.service

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

// Create library-independent types for loose coupling
typealias HTTPCredentials = okhttp3.Credentials

data class HTTPResponse(
    private val response: Response,
) {
    fun isOk() = response.code == 200

    fun isAccepted() = response.code == 202

    fun isError() = !isOk() && !isAccepted()

    fun contentType() = response.header("Content-Type")

    fun text() = response.body?.string()

    fun status() = response.code

    fun byteStream() = response.body?.byteStream()
}

/*
 * Service to make HTTP requests
 */
class HTTPRequestService(
    private val client: OkHttpClient = OkHttpClient(),
) {
    fun get(
        url: String,
        headers: Map<String, String>,
    ): HTTPResponse {
        val request = Request.Builder().url(url)
        headers.forEach { (key, value) -> request.header(key, value) }

        val response: Response = client.newCall(request.build()).execute()
        return HTTPResponse(response)
    }
}
