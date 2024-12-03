package com.atlassian.ctt.service

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.integrations.api.JiraV2ApiParser
import com.atlassian.ctt.integrations.url.JiraV2URLParser
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

class APISanitisationServiceTest {
    // Create CTT Service with dummy data
    private val serverURL = "http://serverURL"
    private var cloudURL = "https://cloudURL"
    private val entityTypes = listOf("jira:issue", "jira/classic:project", "jira/classic:customField")
    private val serverIds = listOf(17499L, 10200L, 17498L)
    private val cloudIds = listOf(10542L, 10300L, 10541L)

    private val cttService = mockk<CTTService>(relaxed = true)
    private val urlParser = JiraV2URLParser()
    private val apiParser = JiraV2ApiParser()
    private val urlService = URLSanitisationService(cttService, urlParser)
    private val apiService = APISanitisationService(cttService, apiParser, urlService)
    private val mockUrl = "$serverURL/rest/api/2/issue/PRJ-123"

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

        every { cttService.cloudURL } returns cloudURL
    }

    @Test
    fun `sanitise body params with empty body`(): Unit =
        runBlocking {
            val result = apiService.sanitiseAPI(mockUrl, "")
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(emptyMap<String, Any>(), result["body"])
        }

    @Test
    fun `sanitise body params with no body`(): Unit =
        runBlocking {
            val result = apiService.sanitiseAPI(mockUrl, null)
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(emptyMap<String, Any>(), result["body"])
        }

    @Test
    fun `sanitise body params with invalid body`(): Unit =
        runBlocking {
            assertThrows(JsonParseException::class.java) {
                apiService.sanitiseAPI(mockUrl, "invalid json")
            }
        }

    @Test
    fun `sanitise body params with invalid url`(): Unit =
        runBlocking {
            assertThrows(URISyntaxException::class.java) {
                apiService.sanitiseAPI("invalid url", "invalid json")
            }
        }

    @Test
    fun `sanitise body params with cloud service down`(): Unit =
        runBlocking {
            every { cttService.cloudURL } throws
                HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cloud Service is down. Please try again later",
                )
            assertThrows(HttpServerErrorException::class.java) {
                apiService.sanitiseAPI(mockUrl, null)
            }
        }

    @Test
    fun `sanitise API with body params without integers`(): Unit =
        runBlocking {
            val body =
                """
                {
                    "key": "jira:issue",
                    "id": "PRJ-123"
                }
                """.trimIndent()
            val bodyMap = jacksonObjectMapper().readValue<Map<String, Any>>(body)
            val result = apiService.sanitiseAPI(mockUrl, body)
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(bodyMap, result["body"])
        }

    @Test
    fun `sanitise API with body nested params without integers`(): Unit =
        runBlocking {
            val body =
                """
                {
                    "key": "jira:issue",
                    "id": "PRJ-123",
                    "fields" : {
                        "project": {
                            "key": "PRJ"
                        }
                    }
                }
                """.trimIndent()
            val bodyMap = jacksonObjectMapper().readValue<Map<String, Any>>(body)
            val result = apiService.sanitiseAPI(mockUrl, body)
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(bodyMap, result["body"])
        }

    @Test
    fun `test seanitise for custom field with no mapping`(): Unit =
        runBlocking {
            val body =
                """
                {
                    "customfield_10000": "Test Custom Field"
                }
                """.trimIndent()
            val bodyMap = jacksonObjectMapper().readValue<Map<String, Any>>(body)
            val result = apiService.sanitiseAPI(mockUrl, body)
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(bodyMap, result["body"])
        }

    @Test
    fun `test sanitise for custom field with mapping`(): Unit =
        runBlocking {
            val body =
                """
                {
                    "customfield_17498": "Test Custom Field"
                }
                """.trimIndent()

            val expectedBody =
                body.replace("customfield_17498", "customfield_10541").run {
                    jacksonObjectMapper().readValue<Map<String, Any>>(this)
                }

            val result = apiService.sanitiseAPI(mockUrl, body)
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(expectedBody, result["body"])
        }

    @Test
    fun `test sanitise with valid id mapping`(): Unit =
        runBlocking {
            val body =
                """
                {
                    "issues": ["ISS-1", 17499, 10000],
                    "issueId": 17499,
                    "boardId" : 10000,
                    "jira/classic:project": 10200,
                    "customfield_17498": 19999,
                    "rankCustomFieldId": 17498,
                    "project" : {
                        "id": 10200
                    },
                    "fields" : {
                        "project": {
                            "id": 10200
                        },
                        "fixVersions": [
                            {
                                "id": 10000
                            }
                        ],
                        "projectIds" : [10300, 10000]
                    }
                }
                """.trimIndent()
            val expectedBody =
                """
                {
                    "issues": ["ISS-1", 10542, 10000],
                    "issueId": 10542,
                    "boardId" : 10000,
                    "jira/classic:project": 10200,
                    "customfield_10541": 19999,
                    "rankCustomFieldId": 10541,
                    "project" : {
                        "id": 10300
                    },
                    "fields" : {
                        "project": {
                            "id": 10300
                        },
                        "fixVersions": [
                            {
                                "id": 10000
                            }
                        ],
                        "projectIds" : [10300, 10000]
                    }
                }
                """.trimIndent().let {
                    jacksonObjectMapper().readValue<Map<String, Any>>(it)
                }
            val result = apiService.sanitiseAPI(mockUrl, body)
            assertEquals(mockUrl.replace(serverURL, cloudURL), result["url"])
            assertEquals(expectedBody.toString(), result["body"].toString())
        }
}
