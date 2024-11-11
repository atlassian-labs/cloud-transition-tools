package com.atlassian.ctt.config

import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.loader.MigrationScope
import com.atlassian.ctt.data.loader.jcma.JCMAMigrationMappingLoader
import com.atlassian.ctt.data.store.MigrationStore
import com.atlassian.ctt.data.store.memory.MemoryMigrationStore
import com.atlassian.ctt.data.store.persistent.MigrationMappingRepository
import com.atlassian.ctt.data.store.persistent.PersistentMigrationStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class CTTServiceConfig(
    private val env: Environment,
) {
    @Value("\${ctt.cloudURL}")
    internal lateinit var cloudURL: String

    @Value("\${ctt.data.store}")
    internal lateinit var dataStoreType: String

    @Value("\${ctt.data.loader}")
    internal lateinit var dataLoaderType: String

    @Bean
    fun migrationStore(repository: MigrationMappingRepository?): MigrationStore =
        when (dataStoreType) {
            "memory" -> MemoryMigrationStore()
            "persistent" -> {
                requireNotNull(repository) { "Repository must be provided for persistent data store type" }
                PersistentMigrationStore(repository)
            }

            else -> throw IllegalArgumentException("Unsupported data store type: $dataStoreType")
        }

    fun migrationMappingLoader(
        serverURL: String,
        authHeader: String,
    ): MigrationMappingLoader {
        val migrationScope = MigrationScope(cloudURL, serverURL)
        return when (dataLoaderType) {
            "jcma" -> JCMAMigrationMappingLoader(migrationScope, authHeader)
            else -> throw IllegalArgumentException("Unsupported data loader type: $dataLoaderType")
        }
    }
}
