package com.atlassian.ctt.integrations.api

import com.atlassian.ctt.data.ARI

// Map of API params to corresponding ARI
// Note: This is the exhaustive list of API params that we support which can be queried with ID.
val apiBodyParams =
    mapOf(
        "issues" to ARI.JIRA_ISSUE,
        "issueId" to ARI.JIRA_ISSUE,
        "incompleteIssuesDestinationId" to ARI.JIRA_ISSUE,
        "filterId" to ARI.JIRA_FILTER,
        "boardId" to ARI.JIRA_BOARD,
        "originBoardId" to ARI.JIRA_BOARD,
        "customfield_" to ARI.JIRA_CUSTOM_FIELD,
        "rankCustomFieldId" to ARI.JIRA_CUSTOM_FIELD,
        "project.id" to ARI.JIRA_PROJECT,
        "projectId" to ARI.JIRA_PROJECT,
        "projectIds" to ARI.JIRA_PROJECT,
        "fixVersions.id" to ARI.JIRA_VERSION,
        "issueTypeIds" to ARI.JIRA_ISSUETYPE,
        "issuetype.id" to ARI.JIRA_ISSUETYPE,
        "priority.id" to ARI.JIRA_PRIORITY,
        "versions.id" to ARI.JIRA_VERSION,
        "components.id" to ARI.JIRA_COMPONENT,
        "sprintIds" to ARI.JIRA_SPRINT,
        "sprintToSwapWith" to ARI.JIRA_SPRINT,
        "categoryId" to ARI.JIRA_PROJECT_CATEGORY,
    )
