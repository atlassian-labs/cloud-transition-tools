package com.atlassian.ctt.integrations.api

import com.atlassian.ctt.integrations.url.URLParts
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.URISyntaxException

class JiraV2APIParserTest {
    private val apiParser = JiraV2ApiParser()

    @Test
    fun `serialiseAPI parts should return correct string`(): Unit =
        runBlocking {
            val apiParts =
                APIParts(
                    URLParts(
                        "http://localhost:8080",
                        "rest/api/2",
                        listOf("issue", "ISSUE-1", "createmeta"),
                        listOf(
                            "expand" to "projects.issuetypes.fields",
                        ),
                    ),
                    mapOf(
                        "fields" to
                            mapOf(
                                "summary" to "Test Summary",
                                "description" to "Test Description",
                                "customfield_10000" to "Test Custom Field",
                            ),
                    ),
                )
            val serialisedAPIParts = apiParser.serialiseAPIParts(apiParts)
            val expectedAPIParts =
                mapOf(
                    "url" to
                        "http://localhost:8080/rest/api/2/issue/ISSUE-1/createmeta?expand=projects.issuetypes.fields",
                    "body" to apiParts.body,
                )
            assertEquals(expectedAPIParts, serialisedAPIParts)
        }

    @Test
    fun `Parse should parse URL correctly with empty body`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2/issue/1000/createmeta?expand=projects.issuetypes.fields"
            val apiParts = apiParser.parseAPI(url, null)
            val expectedAPIParts =
                APIParts(
                    URLParts(
                        "http://localhost:8080",
                        "rest/api/2",
                        listOf("issue", "1000", "createmeta"),
                        listOf(
                            "expand" to "projects.issuetypes.fields",
                        ),
                    ),
                    emptyMap(),
                )
            assertEquals(expectedAPIParts, apiParts)
        }

    @Test
    fun `parse should throw exception on invalid body`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2/issue/1000/createmeta?expand=projects.issuetypes.fields"
            val body = "{"
            assertThrows(JsonParseException::class.java) {
                apiParser.parseAPI(url, body)
            }
        }

    @Test
    fun `parse should throw exception on invalid url`(): Unit =
        runBlocking {
            val url = "invalid_url"
            assertThrows(URISyntaxException::class.java) {
                apiParser.parseAPI(url, null)
            }
        }

    @Test
    fun `Parse should parse and flatten body correctly`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2/issue/1000/createmeta?expand=projects.issuetypes.fields"
            val body =
                """
                {
                    "fields": {
                        "summary": "Test Summary",
                        "description": "Test Description",
                        "customfield_10000": "Test Custom Field",
                        "nested" : {
                            "key" :  "value"
                        }
                    },
                    "key1": "value",
                    "key2": 10000,
                    "custom_10000": {
                        "key": "value"
                    }
                }
                """.trimIndent()
            val apiParts = apiParser.parseAPI(url, body)
            val expectedAPIParts =
                APIParts(
                    URLParts(
                        "http://localhost:8080",
                        "rest/api/2",
                        listOf("issue", "1000", "createmeta"),
                        listOf(
                            "expand" to "projects.issuetypes.fields",
                        ),
                    ),
                    mapOf(
                        "fields.summary" to "Test Summary",
                        "fields.description" to "Test Description",
                        "fields.customfield_10000" to "Test Custom Field",
                        "fields.nested.key" to "value",
                        "key1" to "value",
                        "key2" to 10000,
                        "custom_10000.key" to "value",
                    ),
                )
            assertEquals(expectedAPIParts, apiParts)
        }

    @Test
    fun `Parse should parse and then serialize correctly`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2/issue/1000/createmeta?expand=projects.issuetypes.fields"
            val body =
                """
                {
                    "fields": {
                        "summary": "Test Summary",
                        "description": "Test Description",
                        "customfield_10000": "Test Custom Field",
                        "nested" : {
                            "key" :  "value"
                        }
                    },
                    "key1": "value",
                    "key2": 10000,
                    "custom_10000": {
                        "key": "value"
                    }
                }
                """.trimIndent()
            val apiParts = apiParser.parseAPI(url, body)
            val serialisedAPIParts = apiParser.serialiseAPIParts(apiParts)
            val expectedAPIParts =
                mapOf(
                    "url" to "http://localhost:8080/rest/api/2/issue/1000/createmeta?expand=projects.issuetypes.fields",
                    "body" to jacksonObjectMapper().readValue<Map<String, Any>>(body),
                )
            assertEquals(expectedAPIParts, serialisedAPIParts)
        }

    @Test
    fun `custom field as key should be interpreted correctly`() {
        val key = "customfield_10000"
        val customField = apiParser.keyAsCustomField(key)
        assertEquals("customfield_" to 10000L, customField)
        assertNull(apiParser.keyAsCustomField("project"))
    }

    @Test
    fun `key should be sanitised correctly`() {
        val key = "fields.summary"
        val sanitisedKey = apiParser.sanitiseKey(key)
        assertEquals("summary", sanitisedKey)
    }
}
