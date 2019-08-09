package com.chattriggers.website.config

import java.io.File
import java.util.*

object Config {
    private val properties: Properties

    val db: DbConfig

    init {
        val file = File(".env.properties")
        properties = Properties().apply { load(file.inputStream()) }

        db = DbConfig(
            properties.getProperty("db.jdbcUrl"),
            properties.getProperty("db.username"),
            properties.getProperty("db.password")
        )
    }
}