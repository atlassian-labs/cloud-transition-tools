package com.atlassian.ctt.data.store.persistent

import com.atlassian.ctt.data.MigrationMapping
import com.atlassian.ctt.data.store.MigrationStore
import org.springframework.stereotype.Component

/*
 * PersistentMigrationStore is a concrete implementation of MigrationStore that stores the migration mapping data in a persistent store.
 * This implementation uses a JPA repository to store the mapping information.
 * Any jdbc compliant database can be used to store the mapping information persistently.
 */
@Component
class PersistentMigrationStore(
    private val repository: MigrationMappingRepository,
) : MigrationStore() {
    override fun store(mapping: MigrationMapping) {
        if (repository.findByEntityTypeAndServerId(mapping.entityType, mapping.serverId) != null) {
            return
        }
        repository.save(mapping)
    }

    override fun getCloudId(
        entityType: String,
        serverId: Long,
    ): Long? = repository.findByEntityTypeAndServerId(entityType, serverId)?.cloudId

    override fun getServerId(
        entityType: String,
        cloudId: Long,
    ): Long? = repository.findByEntityTypeAndCloudId(entityType, cloudId)?.serverId
}
