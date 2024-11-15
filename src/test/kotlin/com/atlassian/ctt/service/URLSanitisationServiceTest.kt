package com.atlassian.ctt.service

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.integrations.url.JiraV2URLParser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.net.URISyntaxException

class URLSanitisationServiceTest {
    // Create CTT Service with dummy data
    private val serverURL = "http://serverURL"
    private var cloudURL = "https://cloudURL"
    private val entityTypes = listOf("jira:issue", "jira/classic:project", "jira:projectRole")
    private val serverIds = listOf(17499L, 10200L, 17498L)
    private val cloudIds = listOf(10542L, 10300L, 10541L)

    private val cttService = mockk<CTTService>(relaxed = true)
    private val urlParser = JiraV2URLParser()
    private val urlService = URLSanitisationService(cttService, urlParser)

    @BeforeEach
    fun setUp() {
        every { cttService.translateServerIdToCloudId(any(), any(), any()) } answers {
            if (arg<String>(0) != serverURL) {
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Data Mapping not loaded for $serverURL. Please load the data mapping",
                )
            }
            val entityType = arg<String>(1)
            val serverId = arg<Long>(2)
            val idIdx = serverIds.indexOf(serverId)
            val entityIdx = entityTypes.indexOf(entityType)
            if (idIdx >= 0 && idIdx == entityIdx) {
                MigrationMapping(serverURL, entityType, serverId, cloudIds[idIdx])
            } else {
                MigrationMapping(serverURL, entityType, serverId, 0)
            }
        }

        every { cttService.getCloudURL() } returns cloudURL
    }

    @Test
    fun `test url sanitisation with invalid url`(): Unit =
        runBlocking {
            val url = "https://serverURL/unsupported/api/2/issue/17499"
            assertThrows(URISyntaxException::class.java) {
                urlService.sanitiseURL(url)
            }
        }

    @Test
    fun `test url sanitise without integer IDs`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/ISSUE-1"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, url.replace(serverURL, cloudURL))
        }

    @Test
    fun `test url sanitise without integer IDs and params`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/ISSUE-1/role?role=developer"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, url.replace(serverURL, cloudURL))
        }

    @Test
    fun `test url sanitise with integer id`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/17499/role/developer"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, "$cloudURL/rest/api/2/issue/10542/role/developer")
        }

    @Test
    fun `test url sanitise with multiple integer ids`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/project/10200/role/17498"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, "$cloudURL/rest/api/2/project/10300/role/10541")
        }

    @Test
    fun `test url sanitise with query params`(): Unit =
        runBlocking {
            val urls =
                listOf(
                    "$serverURL/rest/agile/1.0/board?projectKeyOrId=10200",
                    "$serverURL/rest/api/2/issue/picker?currentProjectId=10200",
                )
            val sanitisedURLs = urls.map { urlService.sanitiseURL(it) }
            assertEquals(
                sanitisedURLs,
                listOf(
                    "$cloudURL/rest/agile/1.0/board?projectKeyOrId=10300",
                    "$cloudURL/rest/api/2/issue/picker?currentProjectId=10300",
                ),
            )
        }

    @Test
    fun `test url sanitise with query params and path params`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/17499/role/developer?projectKeyOrId=10200"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, "$cloudURL/rest/api/2/issue/10542/role/developer?projectKeyOrId=10300")
        }

    @Test
    fun `test santise query params without mapping`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/17499/role/developer?projectKeyOrId=10000"
            every { cttService.translateServerIdToCloudId(serverURL, "jira:classic:project", 10000) } returns
                MigrationMapping(serverURL, "jira:classic:project", 10000, 0)
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, "$cloudURL/rest/api/2/issue/10542/role/developer?projectKeyOrId=10000")
        }

    @Test
    fun `test sanitise with no mapping`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/17499/role/developer"
            every { cttService.translateServerIdToCloudId(any(), any(), any()) } returns
                MigrationMapping(serverURL, "jira:issue", 17499, 0)
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, url.replace(serverURL, cloudURL))
        }

    @Test
    fun `test sanitise with unknown entity`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/unknown/17499/role/developer"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, url.replace(serverURL, cloudURL))
        }

    @Test
    fun `test sanitise without entity`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/17499/role/developer"
            val sanitisedURL = urlService.sanitiseURL(url)
            assertEquals(sanitisedURL, url.replace(serverURL, cloudURL))
        }

    @Test
    fun `test sanitise with cloud service not loaded`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/17499/role/developer"
            every { cttService.translateServerIdToCloudId(any(), any(), any()) } throws
                HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Data Mapping not loaded for $serverURL. Please load the data mapping",
                )
            assertThrows(HttpServerErrorException::class.java) {
                urlService.sanitiseURL(url)
            }
        }

    @Test
    fun `test jql sanitisation graceful exit`(): Unit =
        runBlocking {
            val url = "$serverURL/rest/api/2/issue/17499/role/developer?jql=project=10200"
            // assert not implemented
            assertThrows(NotImplementedError::class.java) {
                val sanitisedURL = urlService.sanitiseURL(url)
            }
        }
}
