package com.atlassian.ctt.controller

import com.atlassian.ctt.data.MigrationMapping
import com.atlassian.ctt.service.CTTService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/rest/v1")
@Tag(name = "Cloud Transition Tools Service", description = "Service for translating server IDs to cloud IDs and vice versa")
class CTTServiceController(
    private val ctt: CTTService,
) {
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is up and running")
    fun health(): String = "OK"

    @GetMapping("/server-to-cloud")
    @Operation(summary = "Translate server ID to cloud ID", description = "Translate a server ID to a cloud ID")
    fun translateServerIdToCloudId(
        @RequestParam entityType: String,
        @RequestParam serverId: Long,
    ): MigrationMapping =
        try {
            ctt.translateServerIdToCloudId(entityType, serverId)
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message, e)
        }

    @GetMapping("/cloud-to-server")
    @Operation(summary = "Translate cloud ID to server ID", description = "Translate a cloud ID to a server ID")
    fun translateCloudToServer(
        @RequestParam entityType: String,
        @RequestParam cloudId: Long,
    ): MigrationMapping =
        try {
            ctt.translateCloudIdToServerId(entityType, cloudId)
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(e.statusCode, e.message, e)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message, e)
        }
}
