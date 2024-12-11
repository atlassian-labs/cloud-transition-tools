package com.atlassian.ctt.service

import com.atlassian.ctt.data.ARI
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.integrations.jql.IdentifierSet
import com.atlassian.ctt.integrations.jql.IdentifierType
import com.atlassian.ctt.integrations.jql.JQLSanitisationLibrary
import com.atlassian.ctt.integrations.jql.ParseCancellationException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException

class JQLSanitisationServiceTest {
    private val serverURL = "http://serverURL"

    // Mock CTT service for lookups
    @MockK
    private lateinit var cttService: CTTService

    private lateinit var sanitisationLibrary: JQLSanitisationLibrary
    private lateinit var jqlSanitisationService: JQLSanitisationService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        sanitisationLibrary = JQLSanitisationLibrary()
        jqlSanitisationService = JQLSanitisationService(cttService, sanitisationLibrary)
        mockCTTServiceLookups()
    }

    private fun mockCTTServiceLookups() {
        val mappings =
            mapOf(
                ARI.JIRA_PROJECT to mapOf("10000" to "10500"),
                ARI.JIRA_ISSUE to mapOf("20000" to "20500", "20001" to "20501"),
                ARI.JIRA_USER to mapOf("30000" to "30500"),
                ARI.JIRA_CUSTOM_FIELD to mapOf("10300" to "30010"),
            )
        mappings.forEach { (entityType, mapping) ->
            mapping.forEach { (serverId, cloudId) ->
                every {
                    cttService.translateServerIdToCloudId(serverURL, entityType.value, serverId.toLong())
                } returns
                    MigrationMapping(
                        serverUrl = serverURL,
                        entityType = entityType.toString(),
                        serverId = serverId.toLong(),
                        cloudId = cloudId.toLong(),
                    )
            }
        }

        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUE.value, 10000)
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_ISSUE.value,
                serverId = 10000,
                cloudId = 0,
            )
    }

    @Test
    fun `test jqlSanitisation Library is integrated and initialised correctly`() {
        val sanitiser = sanitisationLibrary.sanitiser
        val jql = "project = 10000 AND issue in (20000, 20001, 10000, PRJ-1) and assignee = 30000"
        val expected = "project = 10500 AND issue in (20500, 20501, 10000, PRJ-1) AND assignee = 30500"
        val extractedIdentifiers =
            listOf(
                IdentifierSet(
                    IdentifierType.CUSTOM_FIELD_IDENTIFIER,
                    setOf(),
                ),
                IdentifierSet(
                    IdentifierType.PROJECT_IDENTIFIER,
                    setOf("10000"),
                ),
                IdentifierSet(
                    IdentifierType.ISSUE_IDENTIFIER,
                    setOf("20000", "20001", "10000"),
                ),
                IdentifierSet(
                    IdentifierType.USER_IDENTIFIER,
                    setOf("30000"),
                ),
            )
        assertEquals(extractedIdentifiers, sanitiser.extractIdentifiers(jql))
        assertEquals(jqlSanitisationService.sanitiseJQL(serverURL, jql), expected)
    }

    @Test
    fun `test sanitiseJQL successfully sanitises JQL string`() {
        val jql = "project = 10000 AND issue in (20000, 20001) and assignee = 30000"
        val expected = "project = 10500 AND issue in (20500, 20501) AND assignee = 30500"
        assertEquals(jqlSanitisationService.sanitiseJQL(serverURL, jql), expected)
    }

    @Test
    fun `test sanitiseJQL successfully sanitises JQL string without integer IDs`() {
        val jql = "project = SMP AND issue in (SMP-1, SMP-2) OR issuetype = 10000"
        val expected = "project = SMP AND issue in (SMP-1, SMP-2) OR issuetype = 10000"
        every { cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUETYPE.value, 10000) } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_ISSUE.toString(),
                serverId = 10000,
                cloudId = 0,
            )
        assertEquals(jqlSanitisationService.sanitiseJQL(serverURL, jql), expected)
    }

    @Test
    fun `test sanitiseJQL while mappings are being loaded`() {
        val jql = "project = SMP AND issue in (SMP-1, SMP-2) OR issuetype = 10000"
        every { cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUETYPE.value, 10000) } throws
            HttpServerErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Mappings are being loaded",
            )
        assertThrows(HttpServerErrorException::class.java) {
            jqlSanitisationService.sanitiseJQL(serverURL, jql)
        }
    }

    @Test
    fun `test sanitiseJQL doesn't update JQL when JQL is invalid`() {
        val invalidJQL = "invalid jql"
        assertThrows(ParseCancellationException::class.java) {
            sanitisationLibrary.sanitiser.extractIdentifiers(invalidJQL)
        }
        assertEquals(jqlSanitisationService.sanitiseJQL(serverURL, invalidJQL), invalidJQL)
    }

    @Test
    fun `test sanitiseJQL doesn't update JQL when JQL is empty`() {
        val invalidJQL = " "
        val outputJql = jqlSanitisationService.sanitiseJQL(serverURL, invalidJQL)
        assertEquals(outputJql, "")
    }

    @Test
    fun `test sanitiseJQL partially sanitises JQL string if cloud ID mapping doesn't exist`() {
        val jql = "project = 10000 AND filter = 20000"
        val expectedJql = "project = 10500 AND filter = 20000"
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_FILTER.value, "20000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_FILTER.toString(),
                serverId = 20000,
                cloudId = 0,
            )
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)
        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test sanitiseJQL partially sanitises JQL string if cloud ID identifier isn't supported`() {
        val jql = "project = 10000 AND unknown = 20000"
        val expectedJql = "project = 10500 AND unknown = 20000"
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)
        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test jql sanitisation for user identifier`() {
        val jql = "assignee = admin or level = 10000"
        val expectedJql = "assignee = admin OR level = 30500"
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUE_SECURITY_LEVEL.value, "10000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_ISSUE_SECURITY_LEVEL.toString(),
                serverId = 10000,
                cloudId = 30500,
            )
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)
        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test custom field sanitisation`() {
        val input =
            "project = XYZ AND status = Open AND cf[10100] in (High, Critical) AND cf[10300] WAS NOT Completed"
        val expected =
            "project = XYZ AND status = Open AND cf[10100] in (High, Critical) AND cf[30010] was not Completed"

        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_CUSTOM_FIELD.value, "10100".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_CUSTOM_FIELD.toString(),
                serverId = 10100,
                cloudId = 0,
            )
        assertEquals(
            expected,
            jqlSanitisationService.sanitiseJQL(serverURL, input),
        )
    }

    @Test
    fun `test project identifier custom functions`() {
        val input =
            "sprint in (11111, SPRINT-1, 22222) AND status = Open AND assignee in (admin, admin1) " +
                "AND fixVersion = earliestunreleasedversion(10000)"
        val expected =
            "sprint in (12345, SPRINT-1, 23456) AND status = Open AND assignee in (admin, admin1) " +
                "AND fixVersion = earliestunreleasedversion(10500)"

        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_SPRINT.value, 11111)
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_SPRINT.toString(),
                serverId = 11111,
                cloudId = 12345,
            )

        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_SPRINT.value, 22222)
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_SPRINT.toString(),
                serverId = 22222,
                cloudId = 23456,
            )

        assertEquals(
            expected,
            jqlSanitisationService.sanitiseJQL(serverURL, input),
        )
    }
}
