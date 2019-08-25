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
    private fun sqlSessionHandler() = SessionHandler().apply {
        val dbConfig = get<DbConfig>()

        sessionCache = DefaultSessionCache(this).apply {
            // Evict sessions after a week no matter what.
            evictionPolicy = 60 * 60 * 24

            sessionDataStore = JDBCSessionDataStoreFactory().apply {
                setDatabaseAdaptor(DatabaseAdaptor().apply {
                    datasource = dbConfig.dataSource
                })
            }.getSessionDataStore(sessionHandler)
        }

        // Evict session after a day of idling.
        maxInactiveInterval = 60 * 60 * 24
        // Cookie is not accessible from JS. Basic cookie-stealing security.
        httpOnly = true
    }

    fun configure(config: JavalinConfig) {
        config.sessionHandler { sqlSessionHandler() }
    }
}