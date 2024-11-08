package com.atlassian.ctt.data.loader

enum class LoaderStatusCode {
    NOT_LOADED,
    LOADING,
    LOADED,
    FAILED,
}

data class LoaderStatus(
    val code: LoaderStatusCode,
    val message: String,
)
