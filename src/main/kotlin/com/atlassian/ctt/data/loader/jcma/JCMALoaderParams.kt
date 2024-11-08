package com.atlassian.ctt.data.loader.jcma

data class JCMALoaderParams(
    val username: String?,
    val password: String?,
    val pat: String?,
    val reload: Boolean = false,
)
