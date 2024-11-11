package com.atlassian.ctt.service

import com.atlassian.ctt.data.loader.LoaderStatus
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationMappingLoader
import com.atlassian.ctt.data.store.MigrationMapping
import com.atlassian.ctt.data.store.MigrationStore
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException

/* Core CTT API Service Layer
 * This service layer provides APIs for fixing integration problems after migration
 * It provides APIs to translate server IDs to cloud IDs and more APIs for custom integration fixes
 * Scoped for a cloud with single migration Store. Technically each of server URL can be loaded with different types of migration loaders.
 */
@Service
class CTTService(
    @Value("\${ctt.cloudURL}") private val cloudURL: String,
    @Qualifier("migrationStore") private val dataStore: MigrationStore,
) {
    private val logger = KotlinLogging.logger {}
    private val loaders = mutableMapOf<String, MigrationMappingLoader>()

    private fun getLoaderStatus(serverURL: String): LoaderStatus {
        return loaders[serverURL]?.getLoaderStatus() ?: run {
            logger.error { "No loader found for server URL $serverURL" }
            return LoaderStatus(LoaderStatusCode.NOT_LOADED, "Data mapping not loaded for $serverURL. Please load the data mapping")
        }
    }

    fun getCloudURL(): String = cloudURL

    fun load(
        loader: MigrationMappingLoader,
        reload: Boolean = false,
    ): LoaderStatus {
        logger.info { "Loading Migration mapping for ${loader.scope}" }
        if (!reload && loaders.containsKey(loader.scope.serverBaseURL)) {
            logger.info { "Loader already present for ${loader.scope}. Skipping load. ${getLoaderStatus(loader.scope.serverBaseURL)} " }
            return getLoaderStatus(loader.scope.serverBaseURL)
        }

        loaders[loader.scope.serverBaseURL] = loader
        // TODO: User coroutine scope for scoped handling
        GlobalScope.launch {
            val status = loader.load(dataStore, reload)
            if (status.code == LoaderStatusCode.FAILED) {
                logger.error { "Failed to load Data Mappings for ${loader.scope}. Error: ${status.message}" }
            }
        }

        return LoaderStatus(LoaderStatusCode.LOADING, "Data Mapping is being Loaded. Please try after some time")
    }

    // Basic lookup APIs
    fun translateServerIdToCloudId(
        serverURL: String,
        entityType: String,
        serverId: Long,
    ): MigrationMapping {
        val loadStatus = getLoaderStatus(serverURL)
        when (loadStatus.code) {
            LoaderStatusCode.LOADING -> {
                logger.info { "Data Mapping is being Loaded. Please try after some time" }
                throw HttpServerErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Data Mapping is being Loaded. Please try after some time",
                )
            }
            LoaderStatusCode.LOADED -> {
                val cloudID = dataStore.getCloudId(serverURL, entityType, serverId) ?: 0
                return MigrationMapping(serverURL, entityType, serverId, cloudID)
            }

            LoaderStatusCode.NOT_LOADED -> {
                logger.error { "Data Mapping not loaded for $serverURL. Please load the data mapping" }
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Data Mapping not loaded for $serverURL. Please load the data mapping",
                )
            }
            else -> {
                logger.error { "Failed to load mapping data. Please check logs" }
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to load mapping data. Please check logs",
                )
            }
        }
    }

    fun translateCloudIdToServerId(
        serverURL: String,
        entityType: String,
        cloudId: Long,
    ): MigrationMapping {
        val status = getLoaderStatus(serverURL)
        when (status.code) {
            LoaderStatusCode.LOADING -> {
                logger.info { "Data Mapping is being Loaded. Please try after some time" }
                throw HttpServerErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Data Mapping is being Loaded. Please try after some time",
                )
            }
            LoaderStatusCode.LOADED -> {
                return MigrationMapping(serverURL, entityType, dataStore.getServerId(serverURL, entityType, cloudId) ?: 0, cloudId)
            }

            LoaderStatusCode.NOT_LOADED -> {
                logger.error { "Data Mapping not loaded for $serverURL. Please load the data mapping" }
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Data Mapping not loaded for $serverURL. Please load the data mapping",
                )
            }
            else -> {
                logger.error { "Failed to load mapping data. Please check logs" }
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to load mapping data. Please check logs",
                )
            }
        }
    }
}
