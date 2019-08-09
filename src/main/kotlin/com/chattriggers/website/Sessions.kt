package com.chattriggers.website

import com.chattriggers.website.config.DbConfig
import io.javalin.core.JavalinConfig
import org.eclipse.jetty.server.session.DatabaseAdaptor
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory
import org.eclipse.jetty.server.session.SessionHandler
import org.koin.core.KoinComponent
import org.koin.core.get

object Sessions : KoinComponent {
    fun sqlSessionHandler() = SessionHandler().apply {
        val dbConfig = get<DbConfig>()

        sessionCache = DefaultSessionCache(this).apply {
            sessionDataStore = JDBCSessionDataStoreFactory().apply {
                setDatabaseAdaptor(DatabaseAdaptor().apply {
                    datasource = dbConfig.dataSource
                })
            }.getSessionDataStore(sessionHandler)
        }

        httpOnly = true
    }

    fun configureJavalin(config: JavalinConfig) {
        config.sessionHandler { sqlSessionHandler() }
    }
}