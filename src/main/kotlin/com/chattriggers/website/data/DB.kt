package com.chattriggers.website.data

import com.chattriggers.website.config.DbConfig
import org.jetbrains.exposed.sql.Database
import org.koin.core.KoinComponent
import org.koin.core.get

object DB : KoinComponent {
    private val dbConfig = get<DbConfig>()

    fun setupDB() {
        Database.connect(dbConfig.dataSource)
    }
}