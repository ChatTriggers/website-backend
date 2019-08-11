package com.chattriggers.website

import com.chattriggers.website.api.makeApiRoutes
import com.chattriggers.website.config.Config
import com.chattriggers.website.data.DB
import io.javalin.Javalin
import org.koin.core.context.startKoin
import org.koin.dsl.module

val configModule = module {
    single { Config.db }
}

fun main() {
    startKoin {
        modules(listOf(configModule))
    }

    DB.setupDB()

    val app = Javalin.create {
        Sessions.configure(it)
        Auth.configure(it)
    }.start(7000)

    makeApiRoutes(app)
}