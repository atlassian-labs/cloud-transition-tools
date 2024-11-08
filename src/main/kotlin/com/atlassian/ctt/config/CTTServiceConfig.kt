package com.atlassian.ctt.config

import com.atlassian.ctt.data.loader.*
import com.atlassian.ctt.data.loader.jcma.JCMALoaderParams
import com.atlassian.ctt.data.loader.jcma.JCMAMigrationMappingLoader
import com.atlassian.ctt.data.store.*
import com.atlassian.ctt.data.store.memory.MemoryMigrationStore
import com.atlassian.ctt.data.store.persistent.PersistentMigrationStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class CTTServiceConfig(
    private val env: Environment,
) {
    @Value("\${ctt.migration.scope.serverBaseURL}")
    private lateinit var serverBaseURL: String

    @Value("\${ctt.migration.scope.cloudSiteURL}")
    private lateinit var cloudSiteURL: String

    @Value("\${ctt.data.store}")
    private lateinit var dataStoreType: String

    @Value("\${ctt.data.loader}")
    private lateinit var dataLoaderType: String

    @Bean
    fun migrationScope() = MigrationScope(cloudSiteURL, serverBaseURL)

    @Bean
    fun migrationStore(persistentStore: PersistentMigrationStore): MigrationStore {
        // Reload should be enabled when using memory store
        // TODO: Support multiple migration from different server cloud sites.
        if (!env.getProperty("ctt.data.reload", "true").toBoolean() && dataStoreType == "memory") {
            throw IllegalArgumentException("Reload must be enabled when using memory store")
        }

        return when (dataStoreType) {
            "memory" -> MemoryMigrationStore()
            "persistent" -> persistentStore
            else -> throw IllegalArgumentException("Unsupported data store type: $dataStoreType")
        }
    }

    @Bean
    fun migrationMappingLoader(
        migrationStore: MigrationStore,
        migrationScope: MigrationScope,
    ): MigrationMappingLoader =
        when (dataLoaderType) {
            "jcma" -> {
                // load the params
                val loaderParams =
                    JCMALoaderParams(
                        env.getProperty("ctt.data.loader.params.username"),
                        env.getProperty("ctt.data.loader.params.password"),
                        env.getProperty("ctt.data.loader.params.pat")?.takeIf { it.isNotBlank() },
                        env.getProperty("ctt.data.reload", "true").toBoolean(),
                    )

                JCMAMigrationMappingLoader(migrationScope, migrationStore, loaderParams)
            }
            // Add other loader types here
            else -> throw IllegalArgumentException("Unsupported data loader type: $dataLoaderType")
        }
}
