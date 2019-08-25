package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.data.Release
import com.chattriggers.website.data.Releases
import com.chattriggers.website.data.User
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import org.joda.time.DateTime
import java.io.File
import java.util.*

class ReleaseController : CrudHandler {
    /**
     * Create a new Release instance.
     *
     * POST modules/:module-id/releases/
     * form parameters-
     *  releaseVersion: String
     *  modVersion: String
     *  changelog: String
     *  module: File
     */
    override fun create(ctx: Context) = voidTransaction {
        val currentUser = ctx.sessionAttribute<User>("user") ?: throw UnauthorizedResponse("Not logged in!")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        if (access == Auth.Roles.default && module.owner != currentUser) throw UnauthorizedResponse("No permissions!")

        if (!ctx.isMultipartFormData()) throw BadRequestResponse("Must be multipart/form-data")

        val releaseVersion = formParamOrFail(ctx, "releaseVersion")
        val modVersion = formParamOrFail(ctx, "modVersion")
        val changelog = formParamOrFail(ctx, "changelog")

        val moduleFile = ctx.uploadedFile("module") ?: throw BadRequestResponse("Missing module zip file")

        val oldRelease = Release.find { Releases.modVersion eq modVersion }.firstOrNull()

        if (oldRelease != null) {
            val folder = File("storage/${module.name}/${oldRelease.id.value}")
            folder.deleteRecursively()
            oldRelease.delete()
        }

        val release = Release.new {
            this.module = module
            this.releaseVersion = releaseVersion
            this.modVersion = modVersion
            this.changelog = changelog
            this.createdAt = DateTime.now()
            this.updatedAt = DateTime.now()
        }

        val folder = File("storage/${module.name}/${release.id.value}")

        try {
            moduleFile.saveModuleToFolder(folder)
        } catch (e: Exception) {
            release.delete()
            throw e
        }

        ctx.status(200).json(release.public())
    }

    override fun delete(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Find all releases for a module.
     *
     * GET modules/:module-id/releases/
     */
    override fun getAll(ctx: Context) = voidTransaction {
        val currentUser = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        ctx.status(200).json(module.releases.map(Release::public))
    }

    /**
     * Download a release.
     *
     * GET modules/:module-id/releases/:release-id
     * query parameters-
     *  file: String
     *      values:
     *      - metadata
     *      - scripts
     */
    override fun getOne(ctx: Context, resourceId: String) = voidTransaction {
        val currentUser = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        val uuid = try { UUID.fromString(resourceId) } catch (e: Exception) { throw BadRequestResponse("release-id not a valid UUID.") }

        val release = Release.findById(uuid) ?: throw NotFoundResponse("No release with specified release-id")

        val releaseFolder = File("storage/${module.name}/${release.id.value}")

        if (!releaseFolder.exists()) throw NotFoundResponse("No release folder found.")

        val file = when (ctx.queryParam("file")) {
            "metadata" -> File(releaseFolder, METADATA_NAME)
            "scripts" -> File(releaseFolder, SCRIPTS_NAME)
            null -> throw BadRequestResponse("Missing 'file' query parameter.")
            else -> throw BadRequestResponse("Invalid 'file' query parameter.")
        }

        ctx.status(200).result(file.inputStream())
    }

    override fun update(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}