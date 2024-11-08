package com.atlassian.ctt.data.store.memory

import com.atlassian.ctt.data.MigrationMapping
import com.atlassian.ctt.data.store.EntityIdPair
import com.atlassian.ctt.data.store.MigrationStore

/*
 * MemoryMigrationStore is an implementation of MigrationStore that stores the mapping information in memory.
 * This is useful for testing and development purposes.
 * Please do not use this to persistently store the mapping information and also to store large amounts of data.
 */

class MemoryMigrationStore : MigrationStore() {
    private val serverToCloud = mutableMapOf<EntityIdPair, Long>()
    private val cloudToServer = mutableMapOf<EntityIdPair, Long>()

    override fun store(mapping: MigrationMapping) {
        serverToCloud[EntityIdPair(mapping.entityType, mapping.serverId)] = mapping.cloudId
        cloudToServer[EntityIdPair(mapping.entityType, mapping.cloudId)] = mapping.serverId
    }

    override fun getCloudId(
        entityType: String,
        serverId: Long,
    ): Long? = serverToCloud[EntityIdPair(entityType, serverId)]

    override fun getServerId(
        entityType: String,
        cloudId: Long,
    ): Long? = cloudToServer[EntityIdPair(entityType, cloudId)]
}
