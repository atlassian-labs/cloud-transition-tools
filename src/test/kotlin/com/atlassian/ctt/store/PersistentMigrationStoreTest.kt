package com.atlassian.ctt.store

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.persistent.MigrationMappingRepository
import com.atlassian.ctt.data.store.persistent.PersistentMigrationStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals

class MigrationMappingRepositoryMock : MigrationMappingRepository {
    private val data = mutableListOf<MigrationMapping>()

    override fun findByServerUrlAndEntityTypeAndServerId(
        serverUrl: String,
        entityType: String,
        serverId: Long,
    ): MigrationMapping? = data.find { it.serverUrl == serverUrl && it.entityType == entityType && it.serverId == serverId }

    override fun findByServerUrlAndEntityTypeAndCloudId(
        serverUrl: String,
        entityType: String,
        cloudId: Long,
    ): MigrationMapping? = data.find { it.serverUrl == serverUrl && it.entityType == entityType && it.cloudId == cloudId }

    override fun <S : MigrationMapping?> save(entity: S & Any): S & Any {
        data.add(entity)
        return entity
    }

    override fun <S : MigrationMapping?> saveAll(entities: MutableIterable<S>): MutableIterable<S> {
        TODO("Not yet implemented")
    }

    override fun findAll(): MutableIterable<MigrationMapping> {
        TODO("Not yet implemented")
    }

    override fun findAllById(ids: MutableIterable<Long>): MutableIterable<MigrationMapping> {
        TODO("Not yet implemented")
    }

    override fun existsById(id: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun findById(id: Long): Optional<MigrationMapping> {
        TODO("Not yet implemented")
    }

    override fun count(): Long = data.size.toLong()

    override fun delete(entity: MigrationMapping) {
        TODO("Not yet implemented")
    }

    override fun deleteAllById(ids: MutableIterable<Long>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: MutableIterable<MigrationMapping>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Long) {
        TODO("Not yet implemented")
    }
}

class PersistentMigrationStoreTest {
    private val store = PersistentMigrationStore(MigrationMappingRepositoryMock())

    private val mappingDataCSV =
        """
        entityType,serverId,cloudId
        jira/classic:issueType,10000,10000
        jira:issue,17499,10542
        jira:issue,17589,10452
        jira:projectVersion,10011,10045
        jira:comment,16412,11782
        jira/classic:project,10200,10037
        jira/classic:project,10102,10041
        """.trimIndent()

    private val mappingCount = 7

    @Test
    fun `load - empty data`() =
        runBlocking {
            assertEquals(0, store.size)
            assertEquals(null, store.getCloudId("serverURL", "entityType", 1))
            assertEquals(null, store.getServerId("serverURL", "entityType", 1))
        }

    @Test
    fun `load -  valid data with reload`() =
        runBlocking {
            val mappingData = mappingDataCSV.split("\n").drop(1)
            for (line in mappingData) {
                val (entityType, serverId, cloudId) = line.split(",")
                store.store(MigrationMapping("serverURL", entityType, serverId.toLong(), cloudId.toLong()))
            }

            assertEquals(mappingCount, store.size)

            val mapping = MigrationMapping("serverURL", "jira:issue", 17499, 10542)
            assertEquals(10542, store.getCloudId(mapping.serverUrl, mapping.entityType, mapping.serverId))
            assertEquals(17499, store.getServerId(mapping.serverUrl, mapping.entityType, mapping.cloudId))

            // Reload
            for (line in mappingData) {
                val (entityType, serverId, cloudId) = line.split(",")
                store.store(MigrationMapping("serverURL", entityType, serverId.toLong(), cloudId.toLong()))
            }
            assertEquals(mappingCount, store.size)
            val mappingAfterReload = MigrationMapping("serverURL", "jira:issue", 17499, 10542)
            assertEquals(10542, store.getCloudId(mappingAfterReload.serverUrl, mappingAfterReload.entityType, mappingAfterReload.serverId))
            assertEquals(17499, store.getServerId(mappingAfterReload.serverUrl, mappingAfterReload.entityType, mappingAfterReload.cloudId))
        }

    @Test
    fun `load -  invalid entry`() =
        runBlocking {
            val mappingData = mappingDataCSV.split("\n").drop(1)
            for (line in mappingData) {
                val (entityType, serverId, cloudId) = line.split(",")
                store.store(MigrationMapping("serverURL", entityType, serverId.toLong(), cloudId.toLong()))
            }

            assertEquals(mappingCount, store.size)

            val mapping = MigrationMapping("serverURL", "jira:issue", 1, 2)
            assertEquals(null, store.getCloudId(mapping.serverUrl, mapping.entityType, mapping.serverId))
            assertEquals(null, store.getServerId(mapping.serverUrl, mapping.entityType, mapping.cloudId))
        }
}
