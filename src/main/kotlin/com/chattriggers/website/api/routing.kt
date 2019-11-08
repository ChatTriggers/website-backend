package com.chattriggers.website.api

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path

fun makeApiRoutes(app: Javalin) {
    app.routes {
        path("api") {
            loginRoutes()

            userRoutes()

            moduleRoutes()
            releaseRoutes()
            tagRoutes()
            versionRoutes()
        }
    }
}

fun makeCompatRoutes(app: Javalin) {
    app.routes {
        path("downloads") {
            get("metadata/:module-name", ::handleOldMetadata)
            get("scripts/:module-name", ::handleOldScripts)
        }
        get("tracker") {
            it.status(200)
        }
        path("versions") {
            get("latest") {
                it.result("ct.js-1.0.0-TODO.jar").status(200)
            }
        }
    }
}
