package com.atlassian.ctt.service

import com.atlassian.ctt.data.ARI
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.integrations.jql.IdentifierSet
import com.atlassian.ctt.integrations.jql.IdentifierType
import com.atlassian.ctt.integrations.jql.JQLSanitisationLibrary
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JQLSanitisationServiceTest {
    private val serverURL = "http://serverURL"

    @MockK
    private lateinit var cttService: CTTService

    @MockK
    private lateinit var sanitisationLibrary: JQLSanitisationLibrary
    private lateinit var jqlSanitisationService: JQLSanitisationService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        jqlSanitisationService = JQLSanitisationService(cttService, sanitisationLibrary)
    }

    @Test
    fun `test sanitiseJQL successfully sanitises JQL string`() {
        val jql = "project = 10000 AND issue in (20000, 20001) and assignee = 30000"
        val expectedJql = "project = 10500 AND issue in (20500, 20501) AND assignee = 30500"
        val extractedIdentifiers =
            listOf(
                IdentifierSet(
                    IdentifierType.PROJECT_IDENTIFIER,
                    setOf("10000"),
                ),
                IdentifierSet(
                    IdentifierType.ISSUE_IDENTIFIER,
                    setOf("20000", "20001"),
                ),
                IdentifierSet(
                    IdentifierType.USER_IDENTIFIER,
                    setOf("30000"),
                ),
            )

        every { sanitisationLibrary.sanitiser.extractIdentifiers(jql) } returns extractedIdentifiers
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_PROJECT.value, "10000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_PROJECT.toString(),
                serverId = 10000,
                cloudId = 10500,
            )
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUE.value, "20000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_ISSUE.toString(),
                serverId = 20000,
                cloudId = 20500,
            )
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUE.value, "20001".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_ISSUE.toString(),
                serverId = 20001,
                cloudId = 20501,
            )
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_USER.value, "30000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_USER.toString(),
                serverId = 20000,
                cloudId = 30500,
            )
        every { sanitisationLibrary.sanitiser.sanitiseJql(jql, any()) } returns expectedJql

        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)

        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test sanitiseJQL successfully sanitises JQL string without integer IDs`() {
        val jql = "project = SMP AND issue in (SMP-1, SMP-2) OR issuetype = 10000"
        val expectedJql = "project = SMP AND issue in (SMP-1, SMP-2) OR issuetype = 10000"
        val extractedIdentifiers =
            listOf(
                IdentifierSet(
                    IdentifierType.PROJECT_IDENTIFIER,
                    setOf("SMP"),
                ),
                IdentifierSet(
                    IdentifierType.ISSUE_IDENTIFIER,
                    setOf("SMP-1", "SMP-2"),
                ),
            )
        every { sanitisationLibrary.sanitiser.extractIdentifiers(jql) } returns extractedIdentifiers
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_ISSUETYPE.value, "10000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_PROJECT.toString(),
                serverId = 10000,
                cloudId = 10500,
            )
        every { sanitisationLibrary.sanitiser.sanitiseJql(jql, any()) } returns expectedJql
        assertEquals(
            expectedJql,
            jqlSanitisationService.sanitiseJQL(serverURL, jql),
        )
    }

    @Test
    fun `test sanitiseJQL doesn't update JQL when JQL is invalid`() {
        val invalidJQL = "invalid jql"

        // Mock a JsonParseException to be thrown when sanitiseJQL is called
        every { sanitisationLibrary.sanitiser.extractIdentifiers(invalidJQL) } throws
            ParseCancellationException("Invalid JQL")

        val outputJql = jqlSanitisationService.sanitiseJQL(serverURL, invalidJQL)
        assertEquals(outputJql, invalidJQL)
    }

    @Test
    fun `test sanitiseJQL doesn't update JQL when JQL is empty`() {
        val invalidJQL = " "

        // Mock a JsonParseException to be thrown when sanitiseJQL is called
        every { sanitisationLibrary.sanitiser.extractIdentifiers(invalidJQL) } throws
            ParseCancellationException("Invalid JQL")

        val outputJql = jqlSanitisationService.sanitiseJQL(serverURL, invalidJQL)
        assertEquals(outputJql, invalidJQL)
    }

    @Test
    fun `test sanitiseJQL partially sanitises JQL string if cloud ID mapping doesn't exist`() {
        val jql = "project = 10000 AND filter = 20000"
        val expectedJql = "project = 10500 AND filter = 20000"
        val extractedIdentifiers =
            listOf(
                IdentifierSet(
                    IdentifierType.PROJECT_IDENTIFIER,
                    setOf("10000"),
                ),
                IdentifierSet(
                    IdentifierType.FILTER_IDENTIFIER,
                    setOf("20000"),
                ),
            )

        every { sanitisationLibrary.sanitiser.extractIdentifiers(jql) } returns extractedIdentifiers
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_PROJECT.value, "10000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_PROJECT.toString(),
                serverId = 10000,
                cloudId = 10500,
            )
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_FILTER.value, "20000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_FILTER.toString(),
                serverId = 20000,
                cloudId = 0,
            )
        every { sanitisationLibrary.sanitiser.sanitiseJql(jql, any()) } returns expectedJql
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)

        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test sanitiseJQL partially sanitises JQL string if cloud ID identifier isn't supported`() {
        val jql = "project = 10000 AND level = 20000"
        val expectedJql = "project = 10500 AND level = 20000"
        val extractedIdentifiers =
            listOf(
                IdentifierSet(
                    IdentifierType.PROJECT_IDENTIFIER,
                    setOf("10000"),
                ),
                IdentifierSet(
                    IdentifierType.ISSUE_SECURITY_LEVEL_IDENTIFIER,
                    setOf("20000"),
                ),
            )

        every { sanitisationLibrary.sanitiser.extractIdentifiers(jql) } returns extractedIdentifiers
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_PROJECT.value, "10000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_PROJECT.toString(),
                serverId = 10000,
                cloudId = 10500,
            )

        every { sanitisationLibrary.sanitiser.sanitiseJql(jql, any()) } returns expectedJql
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)

        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test sanitiseJQL partially sanitises JQL string if extracted identifier is empty`() {
        val jql = "project = 10000  AND assignee = 30000"
        val expectedJql = "project = 10500 AND assignee = 30000"
        val extractedIdentifiers =
            listOf(
                IdentifierSet(
                    IdentifierType.PROJECT_IDENTIFIER,
                    setOf("10000"),
                ),
                IdentifierSet(
                    IdentifierType.USER_IDENTIFIER,
                    emptySet(),
                ),
            )

        every { sanitisationLibrary.sanitiser.extractIdentifiers(jql) } returns extractedIdentifiers
        every {
            cttService.translateServerIdToCloudId(serverURL, ARI.JIRA_PROJECT.value, "10000".toLong())
        } returns
            MigrationMapping(
                serverUrl = serverURL,
                entityType = ARI.JIRA_PROJECT.toString(),
                serverId = 10000,
                cloudId = 10500,
            )

        every { sanitisationLibrary.sanitiser.sanitiseJql(jql, any()) } returns expectedJql
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)

        assertEquals(expectedJql, sanitizedJQL)
    }

    @Test
    fun `test jql sanitisation for user identifier - not implemented currently`() {
        val jql = "assignee = admin"
        val expectedJql = "assignee = unknown"
        every { sanitisationLibrary.sanitiser.extractIdentifiers(jql) } returns
            listOf(
                IdentifierSet(
                    IdentifierType.USER_IDENTIFIER,
                    setOf("admin"),
                ),
            )
        every { sanitisationLibrary.sanitiser.sanitiseJql(jql, any()) } returns expectedJql
        val sanitizedJQL = jqlSanitisationService.sanitiseJQL(serverURL, jql)

        assertEquals(expectedJql, sanitizedJQL)
    }
}
