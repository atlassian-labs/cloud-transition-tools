package com.atlassian.ctt.service

import com.atlassian.ctt.data.ARI
import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.loader.MigrationScope
import com.atlassian.ctt.data.store.MigrationStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class CTTServiceTest {
    private val serverURL = "serverURL"
    private var cloudUrl = "cloudURL"
    private val entityType = ARI.JIRA_ISSUE.toString()
    private val serverId = 17499L
    private val cloudId = 10542L

    private val migrationScope = MigrationScope(cloudUrl, serverURL)

    private var dataStore: MigrationStore = mockk(relaxed = true)
    private var cttService: CTTService = CTTService(cloudUrl, dataStore)

    @Test
    fun `test loading with reload = false`(): Unit =
        runBlocking {
            val loader = mockk<MigrationMappingLoader>(relaxed = true)
            val loaderStatus = LoaderStatus(LoaderStatusCode.LOADING, "Loading")
            coEvery { loader.load(dataStore, false) } returns loaderStatus
            assertEquals(loaderStatus.code, cttService.load(loader, false).code)
        }

    @Test
    fun `test load fail with reload = false`(): Unit =
        runBlocking {
            val loaderStatus = LoaderStatus(LoaderStatusCode.FAILED, "Failed")
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns loaderStatus
                    coEvery { load(dataStore, false) } returns loaderStatus
                }

            var status = cttService.load(loader)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            status = cttService.load(loader, false)
            assertEquals(LoaderStatusCode.FAILED, status.code)
        }

    @Test
    fun `test load success with reload = false`(): Unit =
        runBlocking {
            val loaderStatus = LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns loaderStatus
                    coEvery { load(dataStore, false) } returns loaderStatus
                }

            var status = cttService.load(loader, false)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            status = cttService.load(loader, false)
            assertEquals(LoaderStatusCode.LOADED, status.code)
        }

    @Test
    fun `test load success with reload = true`(): Unit =
        runBlocking {
            val loaderStatus = LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns loaderStatus
                    coEvery { load(dataStore, false) } returns loaderStatus
                }

            var status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)
        }

    @Test
    fun `test translateServerIdToCloudId with no loader`(): Unit =
        runBlocking {
            val exception =
                assertThrows(HttpServerErrorException::class.java) {
                    cttService.translateServerIdToCloudId(serverURL, entityType, serverId)
                }
            assertEquals(exception.statusCode, HttpStatus.INTERNAL_SERVER_ERROR)
        }

    @Test
    fun `test translateCloudIdToServer with no loader`(): Unit =
        runBlocking {
            val exception =
                assertThrows(HttpServerErrorException::class.java) {
                    cttService.translateServerIdToCloudId(serverURL, entityType, serverId)
                }
            assertEquals(exception.statusCode, HttpStatus.INTERNAL_SERVER_ERROR)
        }

    @Test
    fun `test translateServerIdToCloudId with loader - LOADING`(): Unit =
        runBlocking {
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns LoaderStatus(LoaderStatusCode.LOADING, "Loading")
                }

            val status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)

            val exception =
                assertThrows(HttpServerErrorException::class.java) {
                    cttService.translateServerIdToCloudId(serverURL, entityType, serverId)
                }

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.statusCode)
        }

    @Test
    fun `test translateServerIdToCloudId with loader - FAILED`(): Unit =
        runBlocking {
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns LoaderStatus(LoaderStatusCode.FAILED, "FAILED")
                }

            val status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)

            val exception =
                assertThrows(HttpServerErrorException::class.java) {
                    cttService.translateServerIdToCloudId(serverURL, entityType, serverId)
                }

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.statusCode)
        }

    @Test
    fun `test translateServerIdToCloudId with loader`(): Unit =
        runBlocking {
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
                }

            val status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            val cloudId = 10542L
            every { dataStore.getCloudId(serverURL, entityType, serverId) } returns cloudId

            val result = cttService.translateServerIdToCloudId(serverURL, entityType, serverId).cloudId
            assertEquals(cloudId, result)
        }

    @Test
    fun `test translateServerIdToCloudId with loader, no valid mapping`(): Unit =
        runBlocking {
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
                }

            val status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            every { dataStore.getCloudId(serverURL, entityType, serverId) } returns null
            val result = cttService.translateServerIdToCloudId(serverURL, entityType, serverId).cloudId
            assertEquals(0L, result)
        }

    @Test
    fun `test translateCloudIdToServer with loader`(): Unit =
        runBlocking {
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
                }

            val status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            every { dataStore.getServerId(serverURL, entityType, cloudId) } returns serverId

            val result = cttService.translateCloudIdToServerId(serverURL, entityType, cloudId).serverId
            assertEquals(serverId, result)
        }

    @Test
    fun `test translateCloudIdToServerId with loader, no valid mapping`(): Unit =
        runBlocking {
            val loader =
                mockk<MigrationMappingLoader>(relaxed = true) {
                    every { scope } returns migrationScope
                    every { getLoaderStatus() } returns LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
                }

            val status = cttService.load(loader, true)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            every { dataStore.getServerId(serverURL, entityType, cloudId) } returns null
            val result = cttService.translateCloudIdToServerId(serverURL, entityType, cloudId).serverId
            assertEquals(0L, result)
        }

    @Test
    fun `test cloud url`(): Unit =
        runBlocking {
            assertEquals(cttService.getCloudURL(), cloudUrl)
        }

    @Test
    fun `destroy service and ensure no crash or failure`(): Unit =
        runBlocking {
            cttService.destroy()
        }
}
