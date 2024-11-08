package com.atlassian.ctt.data.loader

import com.atlassian.ctt.data.store.MigrationStore

/*
 * MigrationMappingLoader is an abstract class that defines the contract for loading migration mapping data.
 * Data Loaders are scoped to a migration and are responsible for loading mapping information and populating the data store.
 * The data store is decoupled from the loader and can be implemented and scaled independently.
 */
abstract class MigrationMappingLoader(
    val scope: MigrationScope,
    val dataStore: MigrationStore,
) {
    private var loadStatus: LoaderStatus =
        LoaderStatus(LoaderStatusCode.NOT_LOADED, "Data not loaded")

    fun getLoaderStatus() = loadStatus

    fun setLoaderStatus(status: LoaderStatus) {
        loadStatus = status
    }

    abstract suspend fun load(): LoaderStatus

    // can implement more methods like refresh, and callbacks on incremental migrations to reload the data
}
