package com.atlassian.ctt.controller

import com.atlassian.ctt.config.CTTServiceConfig
import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.loader.MigrationScope
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.persistent.MigrationMappingRepository
import com.atlassian.ctt.service.CTTService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.client.HttpServerErrorException

@WebMvcTest(CTTServiceController::class)
class CTTServiceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var cttService: CTTService

    @MockkBean
    private lateinit var cttConfig: CTTServiceConfig

    @MockkBean
    private lateinit var migrationMappingRepository: MigrationMappingRepository

    private val serverURL = "serverURL"
    private var cloudUrl = "cloudURL"
    private val entityType = "jira:issue"
    private val serverId = 17499L
    private val cloudId = 10542L

    private val migrationScope = MigrationScope(cloudUrl, serverURL)

    @Test
    fun `health check should return OK`() {
        mockMvc
            .perform(get("/rest/v1/health"))
            .andExpect(status().isOk)
            .andExpect(content().string("OK"))
    }

    @Test
    fun `load migration mappings LOADED case`() {
        val authHeader = "Bearer token"
        val reload = true
        val loader = mockk<MigrationMappingLoader>()
        val loaderStatus = LoaderStatus(LoaderStatusCode.LOADED, "Loaded successfully")

        every { cttConfig.migrationMappingLoader(serverURL, authHeader) } returns loader
        coEvery { cttService.load(loader, reload) } returns loaderStatus

        mockMvc
            .perform(
                post("/rest/v1/load")
                    .param("serverBaseURL", serverURL)
                    .param("reload", reload.toString())
                    .header("Authorization", authHeader),
            ).andExpect(status().isOk)
            .andExpect(status().reason(loaderStatus.message))
    }

    @Test
    fun `load migration mappings LOADING case`() {
        val authHeader = "Bearer token"
        val reload = true
        val loader = mockk<MigrationMappingLoader>()
        val loaderStatus = LoaderStatus(LoaderStatusCode.LOADING, "Loading in progress")

        every { cttConfig.migrationMappingLoader(serverURL, authHeader) } returns loader
        coEvery { cttService.load(loader, reload) } returns loaderStatus

        mockMvc
            .perform(
                post("/rest/v1/load")
                    .param("serverBaseURL", serverURL)
                    .param("reload", reload.toString())
                    .header("Authorization", authHeader),
            ).andExpect(status().isAccepted)
            .andExpect(status().reason(loaderStatus.message))
    }

    @Test
    fun `load migration mappings FAILED case`() {
        val authHeader = "Bearer token"
        val reload = false
        val loader = mockk<MigrationMappingLoader>()
        val loaderStatus = LoaderStatus(LoaderStatusCode.FAILED, "Loading failed")

        every { cttConfig.migrationMappingLoader(serverURL, authHeader) } returns loader
        coEvery { cttService.load(loader, reload) } returns loaderStatus

        mockMvc
            .perform(
                post("/rest/v1/load")
                    .param("serverBaseURL", serverURL)
                    .param("reload", reload.toString())
                    .header("Authorization", authHeader),
            ).andExpect(status().isInternalServerError)
            .andExpect(status().reason(loaderStatus.message))
    }

    @Test
    fun `translate server ID to cloud ID`() {
        val migrationMapping = MigrationMapping(serverURL, entityType, serverId, cloudId)
        every { cttService.translateServerIdToCloudId(serverURL, entityType, serverId) } returns migrationMapping

        mockMvc
            .perform(
                get("/rest/v1/server-to-cloud")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("serverId", serverId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.serverUrl").value(serverURL))
            .andExpect(jsonPath("$.entityType").value(entityType))
            .andExpect(jsonPath("$.serverId").value(serverId))
            .andExpect(jsonPath("$.cloudId").value(cloudId))
    }

    @Test
    fun `translate server ID to cloud ID, Failure Case HttpServerErrorException`() {
        every { cttService.translateServerIdToCloudId(serverURL, entityType, serverId) } throws
            HttpServerErrorException(
                HttpStatus.ACCEPTED,
            )

        mockMvc
            .perform(
                get("/rest/v1/server-to-cloud")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("serverId", serverId.toString()),
            ).andExpect(status().isAccepted)
    }

    @Test
    fun `translate server ID to cloud ID, Failure Case Exception`() {
        every { cttService.translateServerIdToCloudId(serverURL, entityType, serverId) } throws Exception()

        mockMvc
            .perform(
                get("/rest/v1/server-to-cloud")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("serverId", serverId.toString()),
            ).andExpect(status().isInternalServerError)
    }

    @Test
    fun `translate cloud ID to server ID`() {
        val migrationMapping = MigrationMapping(serverURL, entityType, serverId, cloudId)
        every { cttService.translateCloudIdToServerId(serverURL, entityType, cloudId) } returns migrationMapping

        mockMvc
            .perform(
                get("/rest/v1/cloud-to-server")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("cloudId", cloudId.toString()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.serverUrl").value(serverURL))
            .andExpect(jsonPath("$.entityType").value(entityType))
            .andExpect(jsonPath("$.serverId").value(serverId))
            .andExpect(jsonPath("$.cloudId").value(cloudId))
    }

    @Test
    fun `translate cloud ID to server ID, Failure Case HttpServerErrorException`() {
        every { cttService.translateCloudIdToServerId(serverURL, entityType, cloudId) } throws
            HttpServerErrorException(
                HttpStatus.ACCEPTED,
            )

        mockMvc
            .perform(
                get("/rest/v1/cloud-to-server")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("cloudId", cloudId.toString()),
            ).andExpect(status().isAccepted)
    }

    @Test
    fun `translate cloud ID to server ID, Failure Case Exception`() {
        every { cttService.translateCloudIdToServerId(serverURL, entityType, cloudId) } throws Exception()

        mockMvc
            .perform(
                get("/rest/v1/cloud-to-server")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("cloudId", cloudId.toString()),
            ).andExpect(status().isInternalServerError)
    }
}
