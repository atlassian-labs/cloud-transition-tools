package com.atlassian.ctt.integrations.url

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.MalformedURLException

class JiraV2URLParserTest {
    private val urlParser = JiraV2URLParser()

    @Test
    fun `urlParts toString`() {
        val urlParts =
            URLParts(
                "http://localhost:8080",
                "rest/api/2",
                listOf("issue", "ISSUE-1", "createmeta"),
                listOf(
                    "expand" to "projects.issuetypes.fields",
                ),
            )
        assertEquals(
            "http://localhost:8080/rest/api/2/issue/ISSUE-1/createmeta?expand=projects.issuetypes.fields",
            urlParts.toString(),
        )

        val urlParts2 = URLParts("http://localhost:8080", "rest/api/2", emptyList(), emptyList())
        assertEquals("http://localhost:8080/rest/api/2", urlParts2.toString())

        val urlParts3 =
            URLParts("http://localhost:8080", "rest/api/2", listOf("issue", "ISSUE-1", "createmeta"), emptyList())
        assertEquals("http://localhost:8080/rest/api/2/issue/ISSUE-1/createmeta", urlParts3.toString())
    }

    @Test
    fun `test url parser with invalid url`(): Unit =
        runBlocking {
            val url = "invalidURL"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with invalid protocol`(): Unit =
        runBlocking {
            val url = "localhost:8080/rest/api/2/issue/ISSUE-1"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with invalid path`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with no api path`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/some/reset/api/2"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with no invalid path version`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/some/reset/api/v2"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with valid rest api path`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2"
            val parserdUrlParts = urlParser.parseURL(url)
            assertEquals("http://localhost:8080", parserdUrlParts.baseURL)
            assertEquals("rest/api/2", parserdUrlParts.apiPath)
            assertEquals(emptyList<String>(), parserdUrlParts.pathParams)
            assertEquals(emptyList<String>(), parserdUrlParts.queryParams)
        }

    @Test
    fun `test url parser with valid rest api path minor version`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2.1"
            val parserdUrlParts = urlParser.parseURL(url)
            assertEquals("http://localhost:8080", parserdUrlParts.baseURL)
            assertEquals("rest/api/2.1", parserdUrlParts.apiPath)
            assertEquals(emptyList<String>(), parserdUrlParts.pathParams)
            assertEquals(emptyList<String>(), parserdUrlParts.queryParams)
        }

    @Test
    fun `test url parser with invalid api`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/unknown/2"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with valid rest api path unsupported version`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/1"
            assertThrows(MalformedURLException::class.java) {
                urlParser.parseURL(url)
            }
        }

    @Test
    fun `test url parser with valid agile api path`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/agile/1.0"
            val parserdUrlParts = urlParser.parseURL(url)
            assertEquals("http://localhost:8080", parserdUrlParts.baseURL)
            assertEquals("rest/agile/1.0", parserdUrlParts.apiPath)
            assertEquals(emptyList<String>(), parserdUrlParts.pathParams)
            assertEquals(emptyList<String>(), parserdUrlParts.queryParams)
        }

    @Test
    fun `test url parser with valid auth api path`(): Unit =
        runBlocking {
            val url = "http://server.url:8080/rest/auth/1.0"
            val parserdUrlParts = urlParser.parseURL(url)
            assertEquals("http://server.url:8080", parserdUrlParts.baseURL)
            assertEquals("rest/auth/1.0", parserdUrlParts.apiPath)
            assertEquals(emptyList<String>(), parserdUrlParts.pathParams)
            assertEquals(emptyList<String>(), parserdUrlParts.queryParams)
        }

    @Test
    fun `test url with params`(): Unit =
        runBlocking {
            val url = "http://localhost:8080/rest/api/2/issue/ISSUE-1/createmeta?expand=projects.issuetypes.fields"
            val parserdUrlParts = urlParser.parseURL(url)
            assertEquals("http://localhost:8080", parserdUrlParts.baseURL)
            assertEquals("rest/api/2", parserdUrlParts.apiPath)
            assertEquals(listOf("issue", "ISSUE-1", "createmeta"), parserdUrlParts.pathParams)
            assertEquals(listOf("expand" to "projects.issuetypes.fields"), parserdUrlParts.queryParams)
        }
}
