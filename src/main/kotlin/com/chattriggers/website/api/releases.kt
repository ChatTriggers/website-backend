package com.chattriggers.website.api

import com.chattriggers.website.data.Module
import com.fasterxml.jackson.core.Version
import io.javalin.apibuilder.ApiBuilder.crud
import org.jetbrains.exposed.sql.transactions.transaction

fun releaseRoutes() {
    crud("modules/:module-id/releases/:release-id", ReleaseController())
}

fun getReleaseForModVersion(module: Module, modVersionString: String) = transaction {
    val modVersion = modVersionString.toVersion()

    val allReleases = module.releases
    allReleases.map { it to it.modVersion.toVersion() }
        .sortedByDescending { it.second }
        .filter { it.second.majorVersion == modVersion.majorVersion }
        .firstOrNull { modVersion >= it.second }
        ?.first
}

fun String.toVersion(): Version {
    val split = this.split(".").map(String::toInt)
    return Version(split[0], split[1], split[2], null, null, null)
}