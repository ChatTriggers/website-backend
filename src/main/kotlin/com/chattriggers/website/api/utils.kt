package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Release
import com.chattriggers.website.data.User
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UploadedFile
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

private val gson = GsonBuilder().setPrettyPrinting().create()

fun getModuleOrFail(resourceId: String, user: User?, access: Auth.Role): Module {
    val moduleId = resourceId.toIntOrNull() ?: throw BadRequestResponse("Module ID must be an integer.")

    val module = Module.findById(moduleId)?.load(Module::owner) ?: throw NotFoundResponse("Module does not exist.")

    if ((module.hidden || module.releases.none { it.verified }) && access == Auth.Role.default && module.owner != user) {
        throw NotFoundResponse("Module does not exist.")
    }

    return module
}

fun voidTransaction(code: Transaction.() -> Unit) = transaction { this.code() }

fun UploadedFile.saveModuleToFolder(folder: File, release: Release) {
    folder.mkdirs()
    val zipToSave = File(folder, SCRIPTS_NAME)
    zipToSave.writeBytes(this.content.readBytes())

    try {
        ZipFile(zipToSave).close()
    } catch (e: Exception) {
        zipToSave.delete()
        throw BadRequestResponse("Module is not a valid zip file!")
    }

    val metadataToSave = File(folder, METADATA_NAME)

    try {
        FileSystems.newFileSystem(zipToSave.toPath(), emptyMap<String, Any>(), null).use {
            val rootFolder = Files.newDirectoryStream(it.rootDirectories.first()).iterator()
            if (!rootFolder.hasNext()) throw Exception("Too small")
            val moduleFolder = rootFolder.next()
            if (rootFolder.hasNext()) throw Exception("Too big")

            Files.copy(
                moduleFolder.resolve("metadata.json"),
                metadataToSave.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
            normalizeMetadata(metadataToSave, release)
        }
    } catch (e: JsonSyntaxException) {
        zipToSave.delete()
        metadataToSave.delete()
        throw BadRequestResponse("Malformed metadata.json file: ${e.message}")
    } catch (e: Exception) {
        zipToSave.delete()
        metadataToSave.delete()
        throw BadRequestResponse("Module missing metadata.json!")
    }
}

fun normalizeMetadata(metadataFile: File, release: Release) {
    val metadata = gson.fromJson<ModuleMetadata>(metadataFile.readText(), object : TypeToken<ModuleMetadata>() {}.type)
    metadata.name = release.module.name
    metadata.version = release.releaseVersion
    metadata.tags = release.module.tags.split(",").ifEmpty { null }
    metadata.pictureLink = release.module.image
    metadata.creator = release.module.owner.name
    metadata.description = release.module.description
    metadataFile.writeText(gson.toJson(metadata))
}

fun formParamOrFail(ctx: Context, param: String): String {
    return ctx.formParam(param) ?: throw BadRequestResponse("'$param' parameter missing.")
}

data class ModuleMetadata(
    var name: String? = null,
    var version: String? = null,
    var tags: List<String>? = null,
    var pictureLink: String? = null,
    var creator: String? = null,
    var description: String? = null
)
