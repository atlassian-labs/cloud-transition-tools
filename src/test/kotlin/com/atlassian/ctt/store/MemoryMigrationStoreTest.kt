package com.atlassian.ctt.store

import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.memory.MemoryMigrationStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MemoryMigrationStoreTest {
    private val store = MemoryMigrationStore()

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
    fun `load -  valid data`() =
        runBlocking {
            val mappingData = mappingDataCSV.split("\n").drop(1)
            for (line in mappingData) {
                val (entityType, serverId, cloudId) = line.split(",")
                store.store(MigrationMapping("serverURL", entityType, serverId.toLong(), cloudId.toLong()))
            }

            assertEquals(mappingCount, store.size)

            val mapping = MigrationMapping("serverURL", "jira:issue", 17499, 10542)
            assertEquals(10542, store.getCloudId(mapping.serverUrl, mapping.entityType, mapping.serverId))
            assertEquals(null, store.getCloudId(mapping.serverUrl, "jira:portalPage", 17499))
            assertEquals(null, store.getCloudId("server.url.2", "jira:issue", 17499))
            assertEquals(17499, store.getServerId(mapping.serverUrl, mapping.entityType, mapping.cloudId))
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

    @Test
    fun `test multiple server URL with valid data`() =
        runBlocking {
            val mappingData = mappingDataCSV.split("\n").drop(1)
            for (line in mappingData) {
                val (entityType, serverId, cloudId) = line.split(",")
                store.store(MigrationMapping("serverURL", entityType, serverId.toLong(), cloudId.toLong()))
            }

            val server2Data =
                """
                jira:issue,17499,20542
                jira:issue,17589,20452
                jira:comment,16412,21782,
                """.trimIndent().run {
                    split("\n").drop(1)
                }
            for (line in server2Data) {
                val (entityType, serverId, cloudId) = line.split(",")
                store.store(MigrationMapping("serverURL", entityType, serverId.toLong(), cloudId.toLong()))
            }

            assertEquals(mappingCount, store.size)

            val mapping = MigrationMapping("serverURL", "jira:issue", 17499, 10542)
            assertEquals(10542, store.getCloudId(mapping.serverUrl, mapping.entityType, mapping.serverId))
            assertEquals(17499, store.getServerId(mapping.serverUrl, mapping.entityType, mapping.cloudId))
        }
}
