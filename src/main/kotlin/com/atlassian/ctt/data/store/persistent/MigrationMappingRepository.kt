package com.atlassian.ctt.data.store.persistent

import com.atlassian.ctt.data.store.MigrationMapping
import org.springframework.data.repository.CrudRepository

/*
 * MigrationMappingRepository is a JPA repository that provides CRUD operations for MigrationMapping entities.
 * This repository is used by PersistentMigrationStore to store and retrieve the mapping information.
 * Note that the methods are defined by Spring Data JPA and not needed to be implemented.
 */

interface MigrationMappingRepository : CrudRepository<MigrationMapping, Long> {
    fun findByServerUrlAndEntityTypeAndServerId(
        serverUrl: String,
        entityType: String,
        serverId: Long,
    ): MigrationMapping?

    fun findByServerUrlAndEntityTypeAndCloudId(
        serverUrl: String,
        entityType: String,
        cloudId: Long,
    ): MigrationMapping?
}
