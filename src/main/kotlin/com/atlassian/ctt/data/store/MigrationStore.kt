package com.atlassian.ctt.data.store

/* MigrationStore is scoped to a cloud
 * Responsible for storing and retrieving the mapping between server and cloud ids
 * Multiple servers can be mapped to a single cloud
 * Multiple destination cloud ids are not supported
 */
abstract class MigrationStore {
    abstract fun store(mapping: MigrationMapping)

    abstract fun getCloudId(
        serverURL: String,
        entityType: String,
        serverId: Long,
    ): Long?

    abstract fun getServerId(
        serverURL: String,
        entityType: String,
        cloudId: Long,
    ): Long?
}
