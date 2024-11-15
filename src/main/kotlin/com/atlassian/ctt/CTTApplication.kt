package com.atlassian.ctt

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories

@OpenAPIDefinition(
    info =
        Info(
            title = "Cloud Transition Tools API",
            version = "1.0",
            description = "API for Cloud Transition Tools",
        ),
)
@SpringBootApplication
@EnableJdbcRepositories
class CTTApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<CTTApplication>(*args)
}
