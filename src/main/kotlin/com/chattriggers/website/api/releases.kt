package com.chattriggers.website.api

import com.chattriggers.website.data.Module
import com.fasterxml.jackson.core.Version
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import org.jetbrains.exposed.sql.transactions.transaction

fun releaseRoutes() {
    val releaseController = ReleaseController()
    crud("modules/{module-id}/releases/{release-id}", releaseController)
    get("modules/{module-id}/releases/{release-id}/verify", releaseController::verify)
}

fun getReleaseForModVersion(module: Module, modVersionString: String) = transaction {
    val modVersion = modVersionString.toVersion()

    module.releases
        .asSequence()
        .filter { it.verified }
        .sortedByDescending { it.releaseVersion.toVersion() }
        .distinctBy { it.modVersion }
        .map { it to it.modVersion.toVersion() }
        .sortedByDescending { it.second }
        .firstOrNull { it.second.majorVersion <= modVersion.majorVersion }
        ?.first
}

fun String.toVersion(): Version {
    val split = this.split(".").map(String::toInt)
    return Version(split[0], split[1], split[2], null, null, null)
}
