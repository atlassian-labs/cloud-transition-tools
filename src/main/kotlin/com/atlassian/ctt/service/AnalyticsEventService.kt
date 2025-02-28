package com.atlassian.ctt.service

import com.atlassian.ctt.config.CTTServiceConfig
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.springframework.stereotype.Service
import java.time.Instant

const val JCMA_EVENT_MAPPING_URL = "/rest/migration/latest/ctt/event"

/* Analytics Event Service
 * This service send out analytics events to Atlassian
 */
@Service
class AnalyticsEventService(
    private val config: CTTServiceConfig,
    private val httpRequestService: HTTPRequestService,
) {
    private val logger = KotlinLogging.logger(this::class.java.name)

    fun sendAnalyticsEvent(
        serverBaseURL: String,
        actionSubject: String,
        additionalAttributes: Map<String, Boolean>,
    ) {
        val rawEvent =
            createAnalyticEvent(
                actionSubject,
                additionalAttributes,
            )
        makePostRequestWithQueryParams(serverBaseURL, rawEvent)
    }

    private fun createAnalyticEvent(
        actionSubject: String,
        additionalAttributes: Map<String, Boolean>,
    ): String {
        val event =
            OperationalEvent(
                timestamp = Instant.now().toEpochMilli(),
                action = "executed",
                actionSubject = actionSubject,
                attributes = additionalAttributes,
            )
        // Serialize to JSON
        val objectMapper = ObjectMapper()
        return objectMapper.writeValueAsString(event)
    }

    private fun makePostRequestWithQueryParams(
        serverBaseURL: String,
        rawEvent: String,
    ) {
        val authHeader = config.serverAuth.get(serverBaseURL)
        val requestUrl = buildHttpUrlFromString("$serverBaseURL$JCMA_EVENT_MAPPING_URL") ?: return
        val headers = mapOf("Authorization" to authHeader)
        val urlWithEvent =
            requestUrl
                .newBuilder()
                .addQueryParameter("event", rawEvent)
                .build()

        httpRequestService.post(
            urlWithEvent,
            headers as Map<String, String>,
        )
    }

    data class OperationalEvent(
        val timestamp: Long,
        val action: String,
        val actionSubject: String,
        val attributes: Map<String, Any>,
    )

    private fun buildHttpUrlFromString(urlString: String): HttpUrl? =
        try {
            urlString.toHttpUrl() // Parses the full URL string into an HttpUrl object
        } catch (e: IllegalArgumentException) {
            logger.error { "Invalid URL: ${e.message}" }
            null
        }
}
