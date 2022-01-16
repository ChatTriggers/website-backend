package com.chattriggers.website.api

import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import java.io.File

fun handleOldMetadata(ctx: Context) {
    val (releaseFolder, modVersion) = getReleaseFolder(ctx, "0.18.4")
        ?: throw NotFoundResponse("No release applicable for specified mod version.")

    val file = File(releaseFolder, METADATA_NAME)

    ctx.status(200)
        .header("CT-Version", modVersion)
        .contentType("application/json")
        .result(file.inputStream())
}

fun handleOldScripts(ctx: Context) {
    val (releaseFolder, modVersion) = getReleaseFolder(ctx, "0.18.4")
        ?: throw NotFoundResponse("No release applicable for specified mod version.")

    val file = File(releaseFolder, SCRIPTS_NAME)

    ctx.status(200)
        .header("CT-Version", modVersion)
        .contentType("application/zip")
        .result(file.inputStream())
}
