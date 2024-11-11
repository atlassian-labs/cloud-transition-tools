package com.atlassian.ctt.controller

import com.atlassian.ctt.config.CTTServiceConfig
import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.service.CTTService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/rest/v1")
@Tag(
    name = "Cloud Transition Tools Service",
    description = "Service for translating server IDs to cloud IDs and vice versa",
)
class CTTServiceController(
    private val ctt: CTTService,
    private val config: CTTServiceConfig,
) {
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is up and running")
    fun health(): String = "OK"

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
    ): MigrationMapping =
        try {
            ctt.translateServerIdToCloudId(serverBaseURL, entityType, serverId)
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message, e)
        }

    @GetMapping("/cloud-to-server")
    @Operation(summary = "Translate cloud ID to server ID", description = "Translate a cloud ID to a server ID")
    fun translateCloudToServer(
        @RequestParam serverBaseURL: String,
        @RequestParam entityType: String,
        @RequestParam cloudId: Long,
    ): MigrationMapping =
        try {
            ctt.translateCloudIdToServerId(serverBaseURL, entityType, cloudId)
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message, e)
        }
}
