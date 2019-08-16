package com.chattriggers.website.api

import com.chattriggers.website.ModuleController
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import java.io.File

const val METADATA_NAME = "metadata.json"
const val SCRIPTS_NAME = "scripts.zip"

fun moduleRoutes() {
    crud("modules/:module-id", ModuleController())

    get("modules/:module-name/metadata", ::getMetadata)
    get("module/:module-name/scripts", ::getScripts)
}

fun getMetadata(ctx: Context) {
    val module = ctx.pathParam("module-name").toLowerCase()
    val file = File("storage/$module/$METADATA_NAME")

    if (!file.exists()) throw NotFoundResponse("No module with that module-id")

    ctx.status(200).contentType("application/json").result(file.inputStream())
}

fun getScripts(ctx: Context) {
    val module = ctx.pathParam("module-name").toLowerCase()
    val file = File("storage/$module/$SCRIPTS_NAME")

    if (!file.exists()) throw NotFoundResponse("No module with that module-id")

    ctx.status(200).contentType("application/zip").result(file.inputStream())
}