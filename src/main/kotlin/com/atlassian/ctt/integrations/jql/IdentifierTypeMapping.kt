package com.atlassian.ctt.integrations.jql

import com.atlassian.ctt.data.ARI
import io.atlassian.migration.sanitisation.sanitiser.model.IdentifierType

object IdentifierTypeMapping {
    /**
     * Mapping between supported IdentifierType and corresponding ARI.
     */
    val typeToAriMap: Map<IdentifierType, ARI> =
        mapOf(
            IdentifierType.PROJECT_IDENTIFIER to ARI.JIRA_PROJECT,
            IdentifierType.CUSTOM_FIELD_IDENTIFIER to ARI.JIRA_CUSTOM_FIELD,
            IdentifierType.FILTER_IDENTIFIER to ARI.JIRA_FILTER,
            IdentifierType.ISSUE_IDENTIFIER to ARI.JIRA_ISSUE,
            IdentifierType.SPRINT_IDENTIFIER to ARI.JIRA_SPRINT,
            IdentifierType.ISSUETYPE_IDENTIFIER to ARI.JIRA_ISSUETYPE,
            IdentifierType.PRIORITY_IDENTIFIER to ARI.JIRA_PRIORITY,
            IdentifierType.CATEGORY_IDENTIFIER to ARI.JIRA_PROJECT_CATEGORY,
            IdentifierType.STATUS_IDENTIFIER to ARI.JIRA_STATUS,
            IdentifierType.RESOLUTION_IDENTIFIER to ARI.JIRA_RESOLUTION,
            IdentifierType.COMPONENT_IDENTIFIER to ARI.JIRA_COMPONENT,
            IdentifierType.PROJECT_VERSION_IDENTIFIER to ARI.JIRA_VERSION,
            IdentifierType.USER_IDENTIFIER to ARI.JIRA_USER,
            IdentifierType.ISSUE_SECURITY_LEVEL_IDENTIFIER to ARI.JIRA_ISSUE_SECURITY_LEVEL,
        )
}
