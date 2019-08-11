package com.chattriggers.website.api

import com.chattriggers.website.Auth
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path

fun makeApiRoutes(app: Javalin) {
    app.routes {
        path("api") {
            loginRoutes()
            moduleRoutes()
            get("test", { ctx -> ctx.status(200).result("Success!")}, Auth.adminOnly())
        }
    }
}