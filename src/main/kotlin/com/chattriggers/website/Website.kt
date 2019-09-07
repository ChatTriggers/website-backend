package com.chattriggers.website

import com.chattriggers.website.api.makeApiRoutes
import com.chattriggers.website.api.makeCompatRoutes
import com.chattriggers.website.config.Config
import com.chattriggers.website.data.DB
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import org.koin.core.context.startKoin
import org.koin.dsl.module

// Where all of our configuration is managed.
// Making this injected allows for testing with fake db configurations.
val configModule = module {
    single { Config.db }
    single { Config.mail }
}

fun main(args: Array<String>) {
    startKoin {
        modules(listOf(configModule))
    }

    DB.setupDB()

    val production = args.any { it == "--production" }

    val app = Javalin.create {
        Sessions.configure(it)
        Auth.configure(it)

        if (production) it.enableDevLogging()

        it.addStaticFiles("static/", Location.EXTERNAL)
        it.enableCorsForAllOrigins()
    }.start(if (production) 80 else 7000)

    makeApiRoutes(app)
    makeCompatRoutes(app)
}