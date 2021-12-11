package com.chattriggers.website.api

import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Modules
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File

const val METADATA_NAME = "metadata.json"
const val SCRIPTS_NAME = "scripts.zip"

fun moduleRoutes() {
    crud("modules/{module-id}", ModuleController())

    // Essentially stuff to be used by the mod. The mod has no knowledge of module-id's,
    // releases, etc.
    // Instead, it gets to pass the module's name and its current mod version,
    // and the server handles all of the hard work finding the correct release version.
    get("modules/{module-name}/metadata", ::getMetadata)
    get("modules/{module-name}/scripts", ::getScripts)
}

fun getMetadata(ctx: Context) {
    val releaseFolder = getReleaseFolder(ctx,
        modVersion = ctx.queryParam("modVersion")
            ?: throw BadRequestResponse("Missing 'modVersion' query parameter.")
    ) ?: throw NotFoundResponse("No release applicable for specified mod version.")

    val file = File(releaseFolder, METADATA_NAME)

    ctx.status(200).contentType("application/json").result(file.inputStream())
}

fun getScripts(ctx: Context) {
    val releaseFolder = getReleaseFolder(
        ctx,
        modVersion = ctx.queryParam("modVersion") ?: throw BadRequestResponse("Missing 'modVersion' query parameter."),
        incrementDownloads = true
    ) ?: throw NotFoundResponse("No release applicable for specified mod version.")

    val file = File(releaseFolder, SCRIPTS_NAME)

    ctx.status(200)
        .contentType("application/zip")
        .result(file.inputStream())
}

fun getReleaseFolder(ctx: Context, modVersion: String, incrementDownloads: Boolean = false) = transaction {
    val moduleName = ctx.pathParam("module-name").lowercase()

    val module = Module.find { Modules.name.lowerCase() eq moduleName }
        .firstOrNull() ?: throw NotFoundResponse("No module with that module-name")

    try {
        val release = getReleaseForModVersion(module, modVersion) ?: return@transaction null

        val folder = File("storage/$moduleName/${release.id.value}")

        if (!folder.exists()) return@transaction null

        if (incrementDownloads) {
            module.downloads++
            module.updatedAt = DateTime.now()

            release.downloads++
            release.updatedAt = DateTime.now()
        }

        return@transaction folder
    } catch (e: Exception) {
        throw BadRequestResponse("Invalid query.")
    }
}
