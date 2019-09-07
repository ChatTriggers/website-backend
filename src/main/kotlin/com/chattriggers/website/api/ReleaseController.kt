package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Release
import com.chattriggers.website.data.Releases
import com.chattriggers.website.data.User
import io.javalin.apibuilder.CrudHandler
import io.javalin.core.util.Header
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
     * POST /modules/:module-id/releases/
     * form parameters-
     *  releaseVersion: String
     *  modVersion: String
     *  changelog: String?
     *  module: File
     */
    override fun create(ctx: Context) = voidTransaction {
        val module = moduleOrFail(ctx)

        if (!ctx.isMultipartFormData()) throw BadRequestResponse("Must be multipart/form-data")

        val releaseVersion = formParamOrFail(ctx, "releaseVersion")
        val modVersion = formParamOrFail(ctx, "modVersion")
        val changelog = ctx.formParam("changelog") ?: ""

        val moduleFile = ctx.uploadedFile("module") ?: throw BadRequestResponse("Missing module zip file")

        val oldRelease = Release.find { Releases.modVersion eq modVersion }.firstOrNull()

        if (oldRelease != null) {
            val folder = File("storage/${module.name.toLowerCase()}/${oldRelease.id.value}")
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

        val folder = File("storage/${module.name.toLowerCase()}/${release.id.value}")

        try {
            moduleFile.saveModuleToFolder(folder)
        } catch (e: Exception) {
            release.delete()
            folder.deleteRecursively()
            throw e
        }

        ctx.status(201).json(release.public())
    }

    /**
     * Deletes a release.
     *
     * DELETE /modules/:module-id/releases/:release-id
     */
    override fun delete(ctx: Context, resourceId: String) = voidTransaction {
        val module = moduleOrFail(ctx)

        val uuid = try { UUID.fromString(resourceId) } catch (e: Exception) { throw BadRequestResponse("release-id not a valid UUID.") }

        val release = Release.findById(uuid) ?: throw NotFoundResponse("No release with specified release-id")

        File("storage/${module.name.toLowerCase()}/${release.id.value}").deleteRecursively()

        release.delete()
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
     * GET /modules/:module-id/releases/:release-id
     * query parameters-
     *  file: String
     *      values:
     *      - metadata
     *      - scripts
     *      missing:
     *      release data
     */
    override fun getOne(ctx: Context, resourceId: String) = voidTransaction {
        val currentUser = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        val uuid = try { UUID.fromString(resourceId) } catch (e: Exception) { throw BadRequestResponse("release-id not a valid UUID.") }

        val release = Release.findById(uuid) ?: throw NotFoundResponse("No release with specified release-id")

        val releaseFolder = File("storage/${module.name.toLowerCase()}/${release.id.value}")

        if (!releaseFolder.exists()) throw NotFoundResponse("No release folder found.")

        val file = when (ctx.queryParam("file")) {
            "metadata" -> File(releaseFolder, METADATA_NAME)
            "scripts" -> File(releaseFolder, SCRIPTS_NAME)
            null -> { // file parameter not specified, assume the user wants the json
                ctx.status(200).json(release.public())
                return@voidTransaction
            }
            else -> throw BadRequestResponse("Invalid 'file' query parameter.")
        }

        ctx.status(200).result(file.inputStream())
    }

    /**
     * Create a new Release instance.
     *
     * PATCH /modules/:module-id/releases/:release-id
     * form parameters-
     *  modVersion: String?
     *  changelog: String?
     *  module: File?
     */
    override fun update(ctx: Context, resourceId: String) = voidTransaction {
        val module = moduleOrFail(ctx)

        if (!ctx.isMultipartFormData()
            && ctx.header(Header.CONTENT_TYPE)?.toLowerCase()?.contains("application/x-www-form-urlencoded") == false)
            throw BadRequestResponse("Must be multipart/form-data or application/x-www-form-urlencoded")

        val uuid = try { UUID.fromString(resourceId) } catch (e: Exception) { throw BadRequestResponse("release-id not a valid UUID.") }

        val release = Release.findById(uuid) ?: throw NotFoundResponse("No release with specified release-id")

        ctx.formParam("modVersion")?.let {
            release.modVersion = it
        }

        ctx.formParam("changelog")?.let {
            release.changelog = it
        }

        ctx.uploadedFile("module")?.let {
            val folder = File("storage/${module.name.toLowerCase()}/${release.id.value}")
            val toCopy = File("storage/${module.name.toLowerCase()}/${release.id.value}-backup")

            folder.copyRecursively(toCopy)

            try {
                it.saveModuleToFolder(folder)
            } catch (e: Exception) {
                release.delete()
                folder.deleteRecursively()
                toCopy.copyRecursively(folder)
                throw e
            } finally {
                toCopy.deleteRecursively()
            }
        }

        ctx.status(200).json(release.public())
    }

    private fun moduleOrFail(ctx: Context): Module {
        val currentUser = ctx.sessionAttribute<User>("user") ?: throw UnauthorizedResponse("Not logged in!")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        if (access == Auth.Roles.default && module.owner != currentUser) throw UnauthorizedResponse("No permissions!")

        return module
    }

    companion object {
        fun deleteModule(module: Module) {
            module.releases.forEach {
                File("storage/${module.name.toLowerCase()}/${it.id.value}").deleteRecursively()

                it.delete()
            }
        }
    }
}