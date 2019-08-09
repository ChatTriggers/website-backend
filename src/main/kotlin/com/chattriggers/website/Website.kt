package com.chattriggers.website

import com.chattriggers.website.config.Config
import io.javalin.Javalin
import org.koin.core.context.startKoin
import org.koin.dsl.module

val configModule = module {
    single { Config.db }
}

fun main() {
    val app = Javalin.create {
        Sessions.configureJavalin(it)
    }.start(7000)

    val mainModule = module {
        single { app }
    }

    startKoin {
        modules(listOf(configModule, mainModule))
    }


}