package com.chattriggers.website.data

import com.chattriggers.website.config.DbConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.koin.core.KoinComponent
import org.koin.core.get

object DB : KoinComponent {
    private val dbConfig = get<DbConfig>()

    val dataSource = HikariDataSource().also {
        it.jdbcUrl = dbConfig.jdbcUrl
        it.username = dbConfig.username
        it.password = dbConfig.password
    }

    fun setupDB() {
        Database.connect(dataSource)
    }
}