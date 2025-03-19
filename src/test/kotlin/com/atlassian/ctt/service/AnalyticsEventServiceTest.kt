package com.atlassian.ctt.service

import com.atlassian.ctt.config.CTTServiceConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnalyticsEventServiceTest {
    @MockkBean
    private var cttConfig = mockk<CTTServiceConfig>()
    private var httpRequestService = mockk<HTTPRequestService>()
    private lateinit var analyticsEventService: AnalyticsEventService

    @BeforeEach
    fun setUp() {
        analyticsEventService = AnalyticsEventService(cttConfig, httpRequestService)
    }

    @Test
    fun `test sendAnalyticsEvent successfully sends event`() {
        val serverBaseURL = "http://localhost"
        val actionSubject = "testAction"
        val additionalAttributes = mapOf("entityTranslated" to true)

        // Configure mocks
        every { httpRequestService.post(any(), any()) } just Runs
        every { cttConfig.serverAuth } returns
            mapOf(serverBaseURL to "auth") as MutableMap<String, String>

        // Act
        analyticsEventService.sendAnalyticsEvent(serverBaseURL, actionSubject, additionalAttributes)

        // Verify
        verify(exactly = 1) { httpRequestService.post(any(), any()) }
    }
}
