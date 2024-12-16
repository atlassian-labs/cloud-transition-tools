package com.atlassian.ctt.data.store.persistent

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.MigrationStore
import org.springframework.stereotype.Component

/*
 * PersistentMigrationStore is a concrete implementation of MigrationStore that stores the mapping data persistently.
 * This implementation uses a JPA repository to store the mapping information.
 * Any jdbc compliant database can be used to store the mapping information persistently.
 */
@Component
class PersistentMigrationStore(
    private val repository: MigrationMappingRepository,
) : MigrationStore {
    override fun store(mapping: MigrationMapping) {
        if (repository.findByServerUrlAndEntityTypeAndServerId(
                mapping.serverUrl,
                mapping.entityType,
                mapping.serverId,
            ) != null
        ) {
            return
        }
        repository.save(mapping)
    }

    override val size: Int
        get() = repository.count().toInt()

    override fun getCloudId(
        serverURL: String,
        entityType: String,
        serverId: Long,
    ): Long? = repository.findByServerUrlAndEntityTypeAndServerId(serverURL, entityType, serverId)?.cloudId

    override fun getServerId(
        serverURL: String,
        entityType: String,
        cloudId: Long,
    ): Long? = repository.findByServerUrlAndEntityTypeAndCloudId(serverURL, entityType, cloudId)?.serverId
}
