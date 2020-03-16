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

            eventRoutes()
        }
    }
}

fun makeCompatRoutes(app: Javalin) {
    app.routes {
        path("downloads") {
            get("metadata/:module-name", ::handleOldMetadata)
            get("scripts/:module-name", ::handleOldScripts)
        }
        path("tracker") {
            get("special.json") {
                it.status(200).json(object {
                    val supporters: List<String> = listOf()
                    val developers: List<String> = listOf()
                })
            }
            get {
                it.status(200)
            }
        }
        path("versions") {
            get("latest") {
                it.result("ct.js-1.0.0-TODO.jar").status(200)
            }
        }
    }
}
