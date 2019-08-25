package com.chattriggers.website.api

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path

fun makeApiRoutes(app: Javalin) {
    app.routes {
        path("api") {
            loginRoutes()

            userRoutes()

            moduleRoutes()
            releaseRoutes()
        }
    }
}