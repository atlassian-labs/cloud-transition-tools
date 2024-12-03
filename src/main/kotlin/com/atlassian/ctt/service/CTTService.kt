package com.atlassian.ctt.service

import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.MigrationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import kotlin.jvm.Throws

/* Core CTT API Service Layer
 * This service layer provides APIs for fixing integration problems after migration
 * It provides APIs to translate server IDs to cloud IDs and more APIs for custom integration fixes
 * Scoped for a cloud with single migration Store.
 * Technically each of server URL can be loaded with different types of migration loaders.
 */
@Service
class CTTService(
    @Autowired val cloudURL: String,
    @Qualifier("migrationStore") private val dataStore: MigrationStore,
) : DisposableBean {
    private val logger = KotlinLogging.logger {}
    private val loaders = mutableMapOf<String, MigrationMappingLoader>()

    private val serviceScope = CoroutineScope(Job() + Dispatchers.IO)

    private fun getLoaderStatus(serverURL: String): LoaderStatus {
        return loaders[serverURL]?.getLoaderStatus() ?: run {
            logger.error { "No loader found for server URL $serverURL" }
            return LoaderStatus(
                LoaderStatusCode.NOT_LOADED,
                "Data mapping not loaded for $serverURL. Please load the data mapping",
            )
        }
    }

    private fun throwOnLoaderStatus(
        status: LoaderStatus,
        serverURL: String,
    ) {
        when (status.code) {
            LoaderStatusCode.LOADING -> {
                val loading = "Data Mapping is being Loaded. Please try after some time"
                logger.info { loading }
                throw HttpServerErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    loading,
                )
            }

            LoaderStatusCode.NOT_LOADED -> {
                logger.error { "Data Mapping not loaded for $serverURL. Please load the data mapping" }
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Data Mapping not loaded for $serverURL. Please load the data mapping",
                )
            }

            LoaderStatusCode.FAILED -> {
                logger.error { "Failed to load mapping data for $serverURL. Please check logs" }
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to load mapping data for $serverURL. Please check logs",
                )
            }

            else -> {
                // no action needed
            }
        }
    }

    /* Loads migration data mapping for a server URL
     * If reload is true, data will be loaded again - Pass true for initial load
     * If reload is false, it will skip loading if the loader is already present and return load status.
     */
    fun load(
        loader: MigrationMappingLoader,
        reload: Boolean = false,
    ): LoaderStatus {
        logger.info { "Loading Migration mapping for ${loader.scope}" }
        if (!reload && loaders.containsKey(loader.scope.serverBaseURL)) {
            logger.info {
                "Loader already present for ${loader.scope}." +
                    "Skipping load. ${getLoaderStatus(loader.scope.serverBaseURL)} "
            }
            return getLoaderStatus(loader.scope.serverBaseURL)
        }

        loaders[loader.scope.serverBaseURL] = loader

        serviceScope.launch {
            val status = loader.load(dataStore, reload)
            if (status.code == LoaderStatusCode.FAILED) {
                logger.error { "Failed to load Data Mappings for ${loader.scope}. Error: ${status.message}" }
            }
        }

        return LoaderStatus(LoaderStatusCode.LOADING, "Data Mapping is being Loaded. Please try after some time")
    }

    // Basic lookup APIs
    @Throws(HttpServerErrorException::class)
    fun translateServerIdToCloudId(
        serverURL: String,
        entityType: String,
        serverId: Long,
    ): MigrationMapping {
        val loadStatus = getLoaderStatus(serverURL)
        throwOnLoaderStatus(loadStatus, serverURL)

        val cloudID =
            dataStore.getCloudId(serverURL, entityType, serverId) ?: run {
                logger.warn { "Cloud ID not found for $serverURL, $entityType, $serverId" }
                0
            }

        return MigrationMapping(serverURL, entityType, serverId, cloudID)
    }

    @Throws(HttpServerErrorException::class)
    fun translateCloudIdToServerId(
        serverURL: String,
        entityType: String,
        cloudId: Long,
    ): MigrationMapping {
        val status = getLoaderStatus(serverURL)
        throwOnLoaderStatus(status, serverURL)
        val serverId =
            dataStore.getServerId(serverURL, entityType, cloudId) ?: run {
                logger.warn { "Server ID not found for $serverURL, $entityType, $cloudId" }
                0
            }
        return MigrationMapping(
            serverURL,
            entityType,
            serverId,
            cloudId,
        )
    }

    override fun destroy() {
        serviceScope.cancel()
    }
}
