package com.atlassian.ctt.data.store

import com.atlassian.ctt.data.MigrationMapping

data class EntityIdPair(
    val entityType: String,
    val id: Long,
)

/*
 * MigrationStore is an abstract class that defines the contract for storing migration mapping data.
 * Data Stores are responsible for storing the mapping information and providing lookup methods to retrieve the mapping information.
 * MigrationLoader populates the data store with the mapping information.
 */
abstract class MigrationStore {
    abstract fun store(mapping: MigrationMapping)

    abstract fun getCloudId(
        entityType: String,
        serverId: Long,
    ): Long?

    abstract fun getServerId(
        entityType: String,
        cloudId: Long,
    ): Long?
}
