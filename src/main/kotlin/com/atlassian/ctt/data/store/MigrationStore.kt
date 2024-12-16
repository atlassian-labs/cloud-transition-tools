package com.atlassian.ctt.data.store

/* MigrationStore is scoped to a cloud
 * Responsible for storing and retrieving the mapping between server and cloud ids
 * Multiple servers can be mapped to a single cloud
 * Multiple destination cloud ids are not supported
 */
interface MigrationStore {
    fun store(mapping: MigrationMapping)

    val size: Int

    fun getCloudId(
        serverURL: String,
        entityType: String,
        serverId: Long,
    ): Long?

    fun getServerId(
        serverURL: String,
        entityType: String,
        cloudId: Long,
    ): Long?
}
