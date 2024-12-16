package com.atlassian.ctt.data.store.memory

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.MigrationStore

data class MappingKey(
    val serverURL: String,
    val entityType: String,
    val id: Long,
)

/*
 * MemoryMigrationStore is an implementation of MigrationStore that stores the mapping information in memory.
 * This is useful for testing and development purposes.
 * Please do not use this to persistently store the mapping information and also to store large amounts of data.
 */
class MemoryMigrationStore : MigrationStore {
    private val serverToCloud = mutableMapOf<MappingKey, Long>()
    private val cloudToServer = mutableMapOf<MappingKey, Long>()

    override fun store(mapping: MigrationMapping) {
        serverToCloud[MappingKey(mapping.serverUrl, mapping.entityType, mapping.serverId)] = mapping.cloudId
        cloudToServer[MappingKey(mapping.serverUrl, mapping.entityType, mapping.cloudId)] = mapping.serverId
    }

    override val size: Int
        get() = serverToCloud.size

    override fun getCloudId(
        serverURL: String,
        entityType: String,
        serverId: Long,
    ): Long? = serverToCloud[MappingKey(serverURL, entityType, serverId)]

    override fun getServerId(
        serverURL: String,
        entityType: String,
        cloudId: Long,
    ): Long? = cloudToServer[MappingKey(serverURL, entityType, cloudId)]
}
