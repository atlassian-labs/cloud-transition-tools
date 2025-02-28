package com.atlassian.ctt.controller

import com.atlassian.ctt.config.CTTServiceConfig
import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.service.APISanitisationService
import com.atlassian.ctt.service.AnalyticsEventService
import com.atlassian.ctt.service.CTTService
import com.atlassian.ctt.service.JQLSanitisationService
import com.atlassian.ctt.service.URLSanitisationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.server.ResponseStatusException

/*
 * CTT Service Controller
 * Controller for the CTT service
 */
@RestController
@RequestMapping("/rest/v1")
@Tag(
    name = "Cloud Transition Tools Service",
    description = "Service for translating server IDs to cloud IDs and vice versa",
)
class CTTServiceController(
    private val ctt: CTTService,
    private val config: CTTServiceConfig,
    private val urlSanitsationService: URLSanitisationService,
    private val apiSanitisationService: APISanitisationService,
    private val jqlSanitisationService: JQLSanitisationService,
    private val analyticsEventService: AnalyticsEventService,
) {
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is up and running")
    fun health(): ResponseEntity<String> = ResponseEntity.ok("OK")

    @PostMapping("/load")
    @Operation(summary = "Load migration mappings", description = "Load migration mappings for a given server URL")
    fun load(
        @RequestParam serverBaseURL: String,
        @RequestHeader("Authorization") authHeader: String,
        @RequestParam("reload") reload: Boolean,
    ) {
        val loader = config.migrationMappingLoader(serverBaseURL, authHeader)
        val status: LoaderStatus = ctt.load(loader, reload)
        when (status.code) {
            LoaderStatusCode.LOADING -> {
                throw ResponseStatusException(HttpStatus.ACCEPTED, status.message)
            }

            LoaderStatusCode.LOADED -> {
                throw ResponseStatusException(HttpStatus.OK, status.message)
            }

            else -> {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, status.message)
            }
        }
    }

    @GetMapping("/server-to-cloud")
    @Operation(summary = "Translate server ID to cloud ID", description = "Translate a server ID to a cloud ID")
    fun translateServerIdToCloudId(
        @RequestParam serverBaseURL: String,
        @RequestParam entityType: String,
        @RequestParam serverId: Long,
    ): ResponseEntity<out Any> {
        val attributes = mutableMapOf<String, Boolean>()
        return try {
            val mapping = ctt.translateServerIdToCloudId(serverBaseURL, entityType, serverId)
            attributes.apply {
                put("entityTranslated", (mapping.cloudId != 0L))
                put("success", true)
            }
            ResponseEntity.ok(mapping)
        } catch (e: HttpServerErrorException) {
            attributes.apply {
                put("entityTranslated", false)
                put("success", false)
            }

            ResponseEntity(e.message, e.statusCode)
        } finally {
            if (config.sendAnalytics) {
                analyticsEventService.sendAnalyticsEvent(serverBaseURL, "translateServerToCloudId", attributes)
            }
        }
    }

    @GetMapping("/cloud-to-server")
    @Operation(summary = "Translate cloud ID to server ID", description = "Translate a cloud ID to a server ID")
    fun translateCloudToServer(
        @RequestParam serverBaseURL: String,
        @RequestParam entityType: String,
        @RequestParam cloudId: Long,
    ): ResponseEntity<out Any> {
        val attributes = mutableMapOf<String, Boolean>()
        return try {
            val mapping = ctt.translateCloudIdToServerId(serverBaseURL, entityType, cloudId)
            attributes.apply {
                put("entityTranslated", (mapping.serverId != 0L))
                put("success", true)
            }
            ResponseEntity.ok(mapping)
        } catch (e: HttpServerErrorException) {
            attributes.apply {
                put("entityTranslated", false)
                put("success", false)
            }
            ResponseEntity(e.message, e.statusCode)
        } finally {
            if (config.sendAnalytics) {
                analyticsEventService.sendAnalyticsEvent(serverBaseURL, "translateCloudToServerId", attributes)
            }
        }
    }

    @GetMapping("/url-sanitise")
    @Operation(summary = "Sanitise URL", description = "Sanitise a URL")
    fun sanitiseURL(
        @RequestParam url: String,
    ): ResponseEntity<out Any> =
        try {
            val sanitisedUrl = urlSanitsationService.sanitiseURL(url)
            ResponseEntity.ok(sanitisedUrl)
        } catch (e: HttpServerErrorException) {
            ResponseEntity(e.message, e.statusCode)
        }

    @GetMapping("/api-sanitise")
    @Operation(summary = "Sanitise API", description = "Sanitise an API")
    fun sanitiseAPI(
        @RequestParam url: String,
        @RequestBody body: String,
    ): ResponseEntity<out Any> =
        try {
            val sanitisedAPI = apiSanitisationService.sanitiseAPI(url, body)
            ResponseEntity.ok(sanitisedAPI)
        } catch (e: HttpServerErrorException) {
            ResponseEntity(e.message, e.statusCode)
        }

    @GetMapping("/jql-sanitise")
    @Operation(summary = "Sanitise JQL", description = "Sanitise a JQL query")
    fun sanitiseJQL(
        @RequestParam serverBaseURL: String,
        @RequestBody jql: String,
    ): ResponseEntity<out Any> {
        val attributes = mutableMapOf<String, Boolean>()
        return try {
            val sanitisedJQL = jqlSanitisationService.sanitiseJQL(serverBaseURL, jql)
            attributes.apply {
                put("jqlTranslated", (jql == sanitisedJQL))
                put("success", true)
            }
            ResponseEntity.ok(sanitisedJQL)
        } catch (e: HttpServerErrorException) {
            attributes.apply {
                put("jqlTranslated", false)
                put("success", false)
            }
            ResponseEntity(e.message, e.statusCode)
        } finally {
            if (config.sendAnalytics) {
                analyticsEventService.sendAnalyticsEvent(
                    serverBaseURL,
                    "translateJQL",
                    attributes,
                )
            }
        }
    }
}
