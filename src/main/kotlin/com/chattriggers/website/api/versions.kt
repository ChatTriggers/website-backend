package com.chattriggers.website.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Context
import java.io.File

fun versionRoutes() {
    get("versions", ::getVersions)
}

const val VERSIONS_FILE = "versions.txt"
const val VERSIONS_CHECK_TIMEOUT = 1000 * 60 * 30
var allowedVersions = loadVersions()
var lastVersionsCheckTime = System.currentTimeMillis()

fun getVersions(ctx: Context) {
    if (System.currentTimeMillis() - lastVersionsCheckTime > VERSIONS_CHECK_TIMEOUT) {
        allowedVersions = loadVersions()

        lastVersionsCheckTime = System.currentTimeMillis()
    }

    ctx.status(200).json(
        allowedVersions.groupBy({ "${it.majorVersion}.${it.minorVersion}" }, { it.patchLevel.toString() })
    )
}

private fun loadVersions() = File(VERSIONS_FILE)
    .readText()
    .split("\n")
    .filter { !it.isBlank() }
    .map { it.trim().toVersion() }