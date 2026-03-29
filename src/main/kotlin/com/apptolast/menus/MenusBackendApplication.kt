package com.apptolast.menus

import com.apptolast.menus.config.AppConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppConfig::class)
class MenusBackendApplication

fun main(args: Array<String>) {
    runApplication<MenusBackendApplication>(*args)
}
