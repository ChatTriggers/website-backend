package com.chattriggers.website.api

import com.fasterxml.jackson.core.Version
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Context
import kotlinx.serialization.json.Json
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

    // Only consider mod versions which are available for the requested version. The
    // frontend uses the value "all" to signify it wants every version
    val requestModVersion = ctx.queryParam("modVersion") ?: "1.8.9"
    val versionsToConsider = if (requestModVersion == "all") {
        allowedVersions.keys
    } else {
        allowedVersions.filter {(_, values) ->
            values.any { it == requestModVersion }
        }.keys
    }

    val versions = versionsToConsider.groupBy({
        "${it.majorVersion}.${it.minorVersion}"
    }, { version ->
        val snapshot = if (version.isSnapshot) {
            "-" + version.toString().substringAfter("-")
        } else ""
        version.patchLevel.toString() + snapshot
    })

    ctx.status(200).json(versions)
}

private fun loadVersions(): Map<Version, List<String>> {
    return Json.decodeFromString<Map<String, List<String>>>(File(VERSIONS_FILE).readText())
        .mapKeys { it.key.toVersion() }
}
