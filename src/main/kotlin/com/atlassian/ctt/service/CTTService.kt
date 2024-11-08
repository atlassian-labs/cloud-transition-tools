package com.atlassian.ctt.service

import com.atlassian.ctt.data.MigrationMapping
import com.atlassian.ctt.data.loader.LoaderStatusCode
import com.atlassian.ctt.data.loader.MigrationMappingLoader
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import java.net.HttpRetryException
import kotlin.system.exitProcess

/*
 * Service to provide APIs for Cloud Transition Tools
 * This is a spring service and parameters are auto-wired based on values in application.properties
 * Refer config/CTTServiceConfig.kt for more details
 */
@Service
class CTTService(
    private val loader: MigrationMappingLoader,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun load() {
        logger.info { "Initializing CloudTransitionService" }
        val status = loader.load()
        if (status.code == LoaderStatusCode.FAILED) {
            logger.error { "Failed to load Data Mappings. ${status.message}" }
            exitProcess(1)
        }
    }

    // Basic lookup APIs
    fun translateServerIdToCloudId(
        entityType: String,
        serverId: Long,
    ): MigrationMapping {
        // TODO: Move common loader status check to an annotation and aspect
        when (loader.getLoaderStatus().code) {
            LoaderStatusCode.LOADING -> {
                throw HttpServerErrorException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Data Mapping is being Loaded. Please try after some time",
                )
            }

            LoaderStatusCode.FAILED -> {
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to load Data Mappings. Please check logs",
                )
            }

            LoaderStatusCode.LOADED -> {
                return MigrationMapping(entityType, serverId, loader.dataStore.getCloudId(entityType, serverId) ?: 0)
            }

            else -> {
                throw HttpServerErrorException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to initialize Loader. Please check logs",
                )
            }
        }
    }

    fun translateCloudIdToServerId(
        entityType: String,
        cloudId: Long,
    ): MigrationMapping {
        when (loader.getLoaderStatus().code) {
            LoaderStatusCode.LOADING -> {
                throw HttpRetryException("Data Mapping is being Loaded. Please try after some time", 503)
            }

            LoaderStatusCode.FAILED -> {
                throw HttpRetryException("Failed to load Data Mappings. Please check logs", 503)
            }

            LoaderStatusCode.LOADED -> {
                return MigrationMapping(entityType, loader.dataStore.getServerId(entityType, cloudId) ?: 0, cloudId)
            }

            else -> {
                throw HttpRetryException("Loader is not initialized", 503)
            }
        }
    }
}

/*
 * Service runner to initialise the CTTService on application startup
 */
@Service
class CTTServiceRunner(
    private val service: CTTService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        // launch the coroutine with structured concurrency
        // Note: This is important to avoid memory leaks and to ensure that the coroutine is properly cancelled
        CoroutineScope(Dispatchers.IO).launch {
            service.load()
        }
    }
}
