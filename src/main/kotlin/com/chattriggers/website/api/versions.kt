package com.chattriggers.website.api

import com.fasterxml.jackson.core.Version
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

    val versions = allowedVersions.groupBy({
        "${it.majorVersion}.${it.minorVersion}"
    }, { version ->
        val snapshot = if (version.isSnapshot) {
            "-" + version.toString().substringAfter("-")
        } else ""
        version.patchLevel.toString() + snapshot
    })

    ctx.status(200).json(versions)
}

private fun loadVersions() = File(VERSIONS_FILE)
    .readText()
    .split("\n")
    .filter { it.isNotBlank() }
    .map { it.trim().toVersion() }
