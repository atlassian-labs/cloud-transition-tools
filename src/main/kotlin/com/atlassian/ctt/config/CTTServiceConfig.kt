package com.atlassian.ctt.config

import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.loader.MigrationScope
import com.atlassian.ctt.data.loader.jcma.JCMAMigrationMappingLoader
import com.atlassian.ctt.data.store.MigrationStore
import com.atlassian.ctt.data.store.memory.MemoryMigrationStore
import com.atlassian.ctt.data.store.persistent.MigrationMappingRepository
import com.atlassian.ctt.data.store.persistent.PersistentMigrationStore
import com.atlassian.ctt.integrations.api.APIParser
import com.atlassian.ctt.integrations.api.JiraV2ApiParser
import com.atlassian.ctt.integrations.jql.JQLSanitisationLibrary
import com.atlassian.ctt.integrations.url.JiraV2URLParser
import com.atlassian.ctt.integrations.url.URLParser
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class CTTServiceConfig {
    private val logger = KotlinLogging.logger(this::class.java.name)

    @Value("\${ctt.cloudURL:#{null}}")
    internal var cloudURL: String? = null

    @Value("\${ctt.data.store:memory}")
    internal lateinit var dataStoreType: String

    @Value("\${ctt.data.loader:jcma}")
    internal lateinit var dataLoaderType: String

    @Value("\${ctt.sendAnalytics}")
    internal val sendAnalytics: Boolean = true

    internal var serverAuth: MutableMap<String, String> = mutableMapOf()

    @PostConstruct
    fun checkPropertiesFile() {
        val applicationInitFile = "application.properties"
        if (!File(applicationInitFile).exists()) {
            throw BeanCreationException(
                "Error: application.properties file is missing. Sprint application won't be auto-loaded",
            )
        }

        requireNotNull(cloudURL) { "ctt.cloudURL must be set in application.properties file" }
    }

    @Bean
    fun cloudURL(): String = cloudURL!!

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
        val migrationScope = MigrationScope(cloudURL!!, serverURL)
        return when (dataLoaderType) {
            "jcma" -> {
                if (sendAnalytics) {
                    serverAuth[serverURL] = authHeader
                }
                JCMAMigrationMappingLoader(migrationScope, authHeader)
            }
            else -> throw IllegalArgumentException("Unsupported data loader type: $dataLoaderType")
        }
    }

    @Bean
    fun urlParser(): URLParser {
        // Only Jira V2 URL/ API is supported
        return JiraV2URLParser()
    }

    @Bean
    fun apiParser(): APIParser {
        // Only Jira V2 URL/ API is supported
        return JiraV2ApiParser()
    }

    @Bean
    fun jqlSanitisationLibrary(): JQLSanitisationLibrary = JQLSanitisationLibrary()
}
