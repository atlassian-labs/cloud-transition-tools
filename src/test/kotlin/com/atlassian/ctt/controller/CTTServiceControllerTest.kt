package com.atlassian.ctt.controller

import com.atlassian.ctt.config.CTTServiceConfig
import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.persistent.MigrationMappingRepository
import com.atlassian.ctt.service.APISanitisationService
import com.atlassian.ctt.service.CTTService
import com.atlassian.ctt.service.URLSanitisationService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.HttpServerErrorException

@WebMvcTest(CTTServiceController::class)
class CTTServiceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var cttService: CTTService

    @MockkBean
    private lateinit var cttConfig: CTTServiceConfig

    @Suppress("unused") // required for mocking
    @MockkBean
    private lateinit var migrationMappingRepository: MigrationMappingRepository

    @MockkBean
    private lateinit var urlSanitisationService: URLSanitisationService

    @MockkBean
    private lateinit var apiSanitisationService: APISanitisationService

    private val serverURL = "serverURL"
    private val entityType = "jira:issue"
    private val serverId = 17499L
    private val cloudId = 10542L

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
        every { cttService.translateServerIdToCloudId(serverURL, entityType, serverId) } throws
            HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

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
        every { cttService.translateCloudIdToServerId(serverURL, entityType, cloudId) } throws
            HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        mockMvc
            .perform(
                get("/rest/v1/cloud-to-server")
                    .param("serverBaseURL", serverURL)
                    .param("entityType", entityType)
                    .param("cloudId", cloudId.toString()),
            ).andExpect(status().isInternalServerError)
    }

    @Test
    fun `sanitise URL success scenario`() {
        val url = "https://serverURL/unsupported/api/2/issue/17499"
        val sanitisedUrl = "https://cloudURL/unsupported/api/2/issue/10900"
        every { urlSanitisationService.sanitiseURL(url) } returns sanitisedUrl

        mockMvc
            .perform(
                get("/rest/v1/url-sanitise")
                    .param("url", url),
            ).andExpect(status().isOk)
            .andExpect(content().string(sanitisedUrl))
    }

    @Test
    fun `sanitise URL failure scenario`() {
        val url = "https://serverURL/unsupported/api/2/issue/17499"
        every { urlSanitisationService.sanitiseURL(url) } throws
            HttpServerErrorException(
                HttpStatus.ACCEPTED,
            )

        mockMvc
            .perform(
                get("/rest/v1/url-sanitise")
                    .param("url", url),
            ).andExpect(status().isAccepted)
    }

    @Test
    fun `sanitise URL failure scenario with exception`() {
        val url = "https://serverURL/unsupported/api/2/issue/17499"
        every { urlSanitisationService.sanitiseURL(url) } throws
            HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        mockMvc
            .perform(
                get("/rest/v1/url-sanitise")
                    .param("url", url),
            ).andExpect(status().isInternalServerError)
    }

    @Test
    fun `sanitise API success scenario`() {
        val url = "https://serverURL/unsupported/api/2/issue/17499"
        val body = """
            {
                "issues" : [10000],
                "boardId" : 10000
            }
        """
        every { apiSanitisationService.sanitiseAPI(url, body) } returns mapOf("url" to url, "body" to body)

        mockMvc
            .perform(
                get("/rest/v1/api-sanitise")
                    .param("url", url)
                    .content(body)
                    .contentType("application/json"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.url").value(url))
            .andExpect(jsonPath("$.body").value(body))
    }

    @Test
    fun `sanitise API failure scenario`() {
        val url = "https://serverURL/unsupported/api/2/issue/17499"
        val body = "{}"

        every { apiSanitisationService.sanitiseAPI(url, body) } throws
            HttpServerErrorException(
                HttpStatus.ACCEPTED,
            )

        mockMvc
            .perform(
                get("/rest/v1/api-sanitise")
                    .param("url", url)
                    .content(body)
                    .contentType("application/json"),
            ).andExpect(status().isAccepted)
    }

    @Test
    fun `sanitise API failure scenario with exception`() {
        val url = "https://serverURL/unsupported/api/2/issue/17499"
        val body = "{}"
        every { apiSanitisationService.sanitiseAPI(url, body) } throws
            HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        mockMvc
            .perform(
                get("/rest/v1/api-sanitise")
                    .param("url", url)
                    .content(body)
                    .contentType("application/json"),
            ).andExpect(status().isInternalServerError)
    }
}
