package com.atlassian.ctt.data

enum class ARI(
    val value: String,
) {
    JIRA_PROJECT("jira/classic:project"),
    JIRA_ISSUE("jira:issue"),
    JIRA_FILTER("jira:filter"),
    JIRA_SPRINT("jira/classic/software:sprint"),
    JIRA_BOARD("jira/classic/software:rapidView"),
    JIRA_ISSUETYPE("jira/classic:issueType"),
    JIRA_STATUS("jira/classic:issueStatus"),
    JIRA_VERSION("jira:projectVersion"),
    JIRA_DASHBOARD("jira:portalPage"),
    JIRA_EPIC("jira:issue"),
    JIRA_COMMENT("jira:comment"),
    JIRA_CUSTOM_FIELD_OPTION("jira/classic:customFieldOption"),
    JIRA_CUSTOM_FIELD("jira/classic:customField"),
    JIRA_ISSUE_LINK("jira:issueLink"),
    JIRA_ISSUE_LINK_TYPE("jira:issueLinkType"),
    JIRA_PRIORITY("jira:priority"),
    JIRA_PRIORITY_SCHEMES("jira:priorityScheme"),
    JIRA_PROJECT_CATEGORY("jira:projectCategory"),
    JIRA_RESOLUTION("jira:resolution"),
    JIRA_ROLE("jira:projectRole"),
    JIRA_SCREEN("jira/classic:screen"),
    JIRA_COMPONENT("jira:projectComponent"),
    ;

    companion object {
        fun fromValue(value: String): ARI? = entries.find { it.value == value }
    }
}
