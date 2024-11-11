package com.atlassian.ctt.data.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("MIGRATION_MAPPING")
data class MigrationMapping(
    val serverUrl: String,
    val entityType: String,
    val serverId: Long,
    val cloudId: Long,
    @Id val id: Long? = null,
)
