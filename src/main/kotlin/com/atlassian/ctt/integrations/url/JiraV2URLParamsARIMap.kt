package com.atlassian.ctt.integrations.url

import com.atlassian.ctt.data.ARI

// Map of URL params to corresponding ARI
// Note: This is the exhaustive list of URL params that we support which can be queried with ID.
val JiraV2URLParamsARIMap =
    mapOf(
        "project" to ARI.JIRA_PROJECT,
        "issue" to ARI.JIRA_ISSUE,
        "filter" to ARI.JIRA_FILTER,
        "sprint" to ARI.JIRA_SPRINT,
        "board" to ARI.JIRA_BOARD,
        "issuetype" to ARI.JIRA_ISSUETYPE,
        "status" to ARI.JIRA_STATUS,
        "version" to ARI.JIRA_VERSION,
        "dashboard" to ARI.JIRA_DASHBOARD,
        "epic" to ARI.JIRA_EPIC,
        "comment" to ARI.JIRA_COMMENT,
        "customFieldOption" to ARI.JIRA_CUSTOM_FIELD_OPTION,
        "customFields" to ARI.JIRA_CUSTOM_FIELD,
        "issueLink" to ARI.JIRA_ISSUE_LINK,
        "issueLinkType" to ARI.JIRA_ISSUE_LINK_TYPE,
        "priority" to ARI.JIRA_PRIORITY,
        "priorityschemes" to ARI.JIRA_PRIORITY_SCHEMES,
        "projectCategory" to ARI.JIRA_PROJECT_CATEGORY,
        "resolution" to ARI.JIRA_RESOLUTION,
        "role" to ARI.JIRA_ROLE,
        "screen" to ARI.JIRA_SCREEN,
    )

val JiraV2URLQueryParamsARIMap =
    mapOf(
        "projectKeyOrId" to ARI.JIRA_PROJECT,
        "currentProjectId" to ARI.JIRA_PROJECT,
        "projectIds" to ARI.JIRA_PROJECT,
        "projectId" to ARI.JIRA_PROJECT,
        "boardID" to ARI.JIRA_BOARD,
        "issueTypeId" to ARI.JIRA_ISSUETYPE,
        "issueTypeIds" to ARI.JIRA_ISSUETYPE,
        "screenIds" to ARI.JIRA_SCREEN,
        "fieldId" to ARI.JIRA_CUSTOM_FIELD,
    )
