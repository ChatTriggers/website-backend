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

    val versions = allowedVersions.groupBy({ "${it.majorVersion}.${it.minorVersion}" }, { it.patchLevel.toString() })

    val filteredVersions = if (ctx.userAgent() == "Mozilla/5.0") {
        // This is a request from in-game, so don't show the 2.0 beta version.
        // This can be removed once 2.0 is out of beta.
        versions.filterKeys { !it.startsWith("2") }
    } else versions

    ctx.status(200).json(filteredVersions)
}

private fun loadVersions() = File(VERSIONS_FILE)
    .readText()
    .split("\n")
    .filter { it.isNotBlank() }
    .map { it.trim().toVersion() }