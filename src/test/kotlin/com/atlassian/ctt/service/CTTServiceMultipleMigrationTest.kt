package com.atlassian.ctt.service

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class CTTServiceMultipleMigrationTest {
    private val serverURLs = listOf("https://server.url1", "https://server.url2", "https://server.url3")
    private val cloudUrl = "https://cloud.url"
    private val entityMaps =
        mapOf(
            serverURLs[0] to
                mapOf(
                    Pair("jira:issue", 10000L) to 10001L,
                    Pair("jira/classic:project", 10001L) to 10002L,
                ),
            serverURLs[1] to
                mapOf(
                    Pair("jira:priority", 10000L) to 20001L,
                    Pair("jira:priority", 10002L) to 20003L,
                ),
            serverURLs[2] to
                mapOf(
                    Pair("jira:issue", 10000L) to 30001L,
                    Pair("jira:issue", 10003L) to 30004L,
                    Pair("jira/classic/software:sprint", 10000L) to 130006L,
                ),
        )

    private var dataStore: MigrationStore = mockk(relaxed = true)
    private var cttService: CTTService = CTTService(cloudUrl, dataStore)
    private var loader: MigrationMappingLoader = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { dataStore.getCloudId(any(), any(), any()) } answers {
            val serverUrl = arg<String>(0)
            val entityType = arg<String>(1)
            val serverId = arg<Long>(2)
            val mappings: Map<Pair<String, Long>, Long> =
                entityMaps[serverUrl] ?: run {
                    throw HttpServerErrorException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to load mapping data for $serverUrl. Please check logs",
                    )
                }

            mappings[Pair(entityType, serverId)]
        }
    }

    @Test
    fun `proper error should be returned when data is not loaded`(): Unit =
        runBlocking {
            val entityType = "jira:issue"
            val serverId = 10000L
            serverURLs.forEach { serverUrl ->
                run {
                    assertThrows(HttpServerErrorException::class.java) {
                        cttService.translateServerIdToCloudId(serverUrl, entityType, serverId)
                    }
                }
            }
        }

    @Test
    fun `proper cloud id should be returned for a server after successful load`(): Unit =
        runBlocking {
            val serverId = 10000L
            val cloudId = 10001L
            val entityType = "jira:issue"

            val loaderStatus = LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
            every { loader.scope } returns MigrationScope(cloudUrl, serverURLs[0])
            every { loader.getLoaderStatus() } returns loaderStatus
            coEvery { loader.load(dataStore, false) } returns loaderStatus
            var status = cttService.load(loader, false)
            assertEquals(LoaderStatusCode.LOADING, status.code)
            status = cttService.load(loader, false)
            assertEquals(LoaderStatusCode.LOADED, status.code)

            val mapping = cttService.translateServerIdToCloudId(serverURLs[0], entityType, serverId)
            assert(mapping.cloudId == cloudId)

            // unloaded server should throw error
            assertThrows(HttpServerErrorException::class.java) {
                cttService.translateServerIdToCloudId(serverURLs[1], entityType, serverId)
            }
        }

    @Test
    fun `proper cloud id should be returned with multiple servers loaded`(): Unit =
        runBlocking {
            // Load all servers
            val loaderStatus = LoaderStatus(LoaderStatusCode.LOADED, "Loaded")
            coEvery { loader.load(dataStore, false) } returns loaderStatus
            every { loader.getLoaderStatus() } returns loaderStatus
            serverURLs.forEach { serverUrl ->
                run {
                    every { loader.scope } returns MigrationScope(cloudUrl, serverUrl)
                    val status = cttService.load(loader, false)
                    assertEquals(LoaderStatusCode.LOADING, status.code)
                }
            }
            serverURLs.forEach { serverUrl ->
                run {
                    every { loader.scope } returns MigrationScope(cloudUrl, serverUrl)
                    val status = cttService.load(loader, false)
                    assertEquals(LoaderStatusCode.LOADED, status.code)
                }
            }
            serverURLs.forEach { serverUrl ->
                run {
                    entityMaps[serverUrl]?.forEach { (enitypair, cloudId) ->
                        val (entityType, serverId) = enitypair
                        assertEquals(
                            cttService.translateServerIdToCloudId(serverUrl, entityType, serverId).cloudId,
                            cloudId,
                        )
                    }
                }
            }
        }
}
