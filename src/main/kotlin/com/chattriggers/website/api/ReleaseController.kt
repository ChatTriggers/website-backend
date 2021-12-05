package com.chattriggers.website.api

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import com.chattriggers.website.Auth
import com.chattriggers.website.config.DiscordConfig
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Release
import com.chattriggers.website.data.Releases
import com.chattriggers.website.data.User
import io.javalin.apibuilder.CrudHandler
import io.javalin.core.util.Header
import io.javalin.http.*
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.awt.Color
import java.io.File
import java.util.*

class ReleaseController : CrudHandler, KoinComponent {
    val releaseWebhook = WebhookClient.withUrl(get<DiscordConfig>().webhookURL)

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
        val currentUser = ctx.sessionAttribute<User>("user") ?: throw UnauthorizedResponse("Not logged in!")
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        if (!ctx.isMultipartFormData()) throw BadRequestResponse("Must be multipart/form-data")

        val releaseVersion = formParamOrFail(ctx, "releaseVersion")

        if (!validateVersion(releaseVersion)) throw BadRequestResponse("Malformed release version parameter. Must conform to the Semver spec.")

        val modVersion = formParamOrFail(ctx, "modVersion")

        if (modVersion.toVersion() !in allowedVersions) throw BadRequestResponse("The provided mod version does not exist.")

        val existingRelease = Release.find {
            (Releases.releaseVersion eq releaseVersion) and
                (Releases.module eq module.id) and
                (Releases.modVersion eq modVersion)
        }.firstOrNull()

        if (existingRelease != null) throw BadRequestResponse("There already exists a release with version number $releaseVersion")

        val changelog = ctx.formParam("changelog") ?: ""

        val moduleFile = ctx.uploadedFile("module") ?: throw BadRequestResponse("Missing module zip file")

        val verificationToken = UUID.randomUUID().toString()

        Release.find {
            (Releases.modVersion eq modVersion) and
                (Releases.module eq module.id) and
                (Releases.verified eq false)
        }.forEach {
            deleteRelease(it)
        }

        val release = Release.new {
            this.module = module
            this.releaseVersion = releaseVersion
            this.modVersion = modVersion
            this.changelog = changelog
            this.createdAt = DateTime.now()
            this.updatedAt = DateTime.now()
            this.verified = false
            this.verificationToken = verificationToken
        }

        if (access in Auth.trustedOrHigher()) {
            release.verificationToken = null
            release.verified = true
        }

        val folder = File("storage/${module.name.lowercase()}/${release.id.value}")

        try {
            moduleFile.saveModuleToFolder(folder, release)
        } catch (e: Exception) {
            release.delete()
            folder.deleteRecursively()
            throw e
        }

        ctx.status(201).json(release.authorized())

        if (!module.hidden && release.verified)
            EventHandler.postEvent(Event.ReleaseCreated(module.public(), release.public()))

        if (!release.verified) {
            var verificationUrl = "https://chattriggers.com/modules/verify/${module.name}?token=$verificationToken&" +
                "newReleaseId=${release.id}"

            Release.find {
                (Releases.modVersion eq modVersion) and
                    (Releases.module eq module.id) and
                    (Releases.verified eq true)
            }.maxByOrNull { it.releaseVersion.toVersion() }?.let {
                verificationUrl += "&oldReleaseId=${it.id}"
            }

            val embed = WebhookEmbedBuilder()
                .setTitle(WebhookEmbed.EmbedTitle(
                    "Release v${release.releaseVersion} for ${module.name} has been posted",
                    "https://chattriggers.com/modules/v/${module.name}"
                ))
                .setDescription("Please verify this release is safe and non-malicious.\n" +
                    "Click [here]($verificationUrl) to confirm verification.")
                .setColor(Color(60, 197, 197).rgb)
                .build()

            releaseWebhook.send(embed).thenAccept { msg ->
                voidTransaction {
                    release.verificationMessage = msg.id
                }
            }
        }
    }

    /**
     * Deletes a release.
     *
     * DELETE /modules/:module-id/releases/:release-id
     */
    override fun delete(ctx: Context, resourceId: String) = voidTransaction {
        val module = moduleOrFail(ctx)
        val release = releaseOrFail(resourceId)

        deleteRelease(release)
    }

    /**
     * Find all releases for a module.
     *
     * GET modules/:module-id/releases/
     */
    override fun getAll(ctx: Context) = voidTransaction {
        val currentUser = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)
        val authorized = access in Auth.trustedOrHigher() || currentUser == module.owner

        val releaseData = if (authorized) {
            module.authorized()
        } else {
            module.public()
        }

        ctx.status(200).json(releaseData.releases)
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
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)
        val release = releaseOrFail(resourceId)

        val authorized = access in Auth.trustedOrHigher() || currentUser == module.owner
        if (!authorized && !release.verified)
            throw ForbiddenResponse("Module is unverified")

        val releaseFolder = File("storage/${module.name.lowercase()}/${release.id.value}")

        if (!releaseFolder.exists()) throw NotFoundResponse("No release folder found.")

        class ReturnData(val file: File, val contentType: String, val filename: String)

        val data = when (ctx.queryParam("file")) {
            "metadata" -> ReturnData(
                File(releaseFolder, METADATA_NAME),
                "application/json",
                "{module.name}-${release.releaseVersion}-metadata.json"
            )
            "scripts" -> ReturnData(
                File(releaseFolder, SCRIPTS_NAME),
                "application/zip",
                "${module.name}-${release.releaseVersion}.zip"
            )
            null -> { // file parameter not specified, assume the user wants the json
                ctx.status(200).json(release.public())
                return@voidTransaction
            }
            else -> throw BadRequestResponse("Invalid 'file' query parameter.")
        }

        ctx.status(200).contentType(data.contentType)
            .header("Content-Disposition", "attachment; filename=${data.filename}")
            .result(data.file.inputStream())
    }

    /**
     * Updates an existing Release instance.
     *
     * PATCH /modules/:module-id/releases/:release-id
     * form parameters-
     *  modVersion: String?
     *  changelog: String?
     *  module: File?
     */
    override fun update(ctx: Context, resourceId: String) = voidTransaction {
        // Ensure the module actually exists first...
        moduleOrFail(ctx)

        if (!ctx.isMultipartFormData()
            && ctx.header(Header.CONTENT_TYPE)?.lowercase()?.contains("application/x-www-form-urlencoded") == false
        )
            throw BadRequestResponse("Must be multipart/form-data or application/x-www-form-urlencoded")

        val uuid = try {
            UUID.fromString(resourceId)
        } catch (e: Exception) {
            throw BadRequestResponse("release-id not a valid UUID.")
        }

        val release = Release.findById(uuid) ?: throw NotFoundResponse("No release with specified release-id")

        ctx.formParam("modVersion")?.let {
            release.modVersion = it
        }

        ctx.formParam("changelog")?.let {
            release.changelog = it
        }

//        It's been decided it makes no sense to edit a Release's module files. By reusing the same Release,
//        the client has no way to know the module has changed, and thus won't auto-update.
//        Instead, the user ought to make a new Release.

//        ctx.uploadedFile("module")?.let {
//            val folder = File("storage/${module.name.lowercase()}/${release.id.value}")
//            val toCopy = File("storage/${module.name.lowercase()}/${release.id.value}-backup")
//
//            folder.copyRecursively(toCopy)
//
//            try {
//                it.saveModuleToFolder(folder)
//            } catch (e: Exception) {
//                release.delete()
//                folder.deleteRecursively()
//                toCopy.copyRecursively(folder)
//                throw e
//            } finally {
//                toCopy.deleteRecursively()
//            }
//        }

        ctx.status(200).json(release.public())
    }

    /**
     * Verifies a release to be safe and secure, making it public to everyone.
     *
     * GET /modules/:module-id/releases/:release-id/verify
     * query parameters-
     *  verificationToken: String
     */
    fun verify(ctx: Context) = voidTransaction {
        // No authorization on if the module or release should be viewable by the currently logged in user,
        // if there even is one, because if the request has the verification token, we don't care.
        val release = releaseOrFail(ctx.pathParam("release-id"))

        if (release.verified)
            return@voidTransaction run { ctx.status(200) }

        val verificationToken = ctx.queryParam("verificationToken")
            ?: throw UnauthorizedResponse("verificationToken not provided")

        if (verificationToken != release.verificationToken)
            throw ForbiddenResponse("incorrect verificationToken")

        release.verified = true
        release.verificationToken = null

        val module = release.module

        release.verificationMessage?.let {
            releaseWebhook.delete(it)
            release.verificationMessage = null
        }

        if (!module.hidden)
            EventHandler.postEvent(Event.ReleaseCreated(module.public(), release.public()))

        ctx.status(200)
    }

    private fun releaseOrFail(releaseId: String): Release {
        val uuid = try {
            UUID.fromString(releaseId)
        } catch (e: Exception) {
            throw BadRequestResponse("release-id not a valid UUID.")
        }

        return Release.findById(uuid) ?: throw NotFoundResponse("No release with specified release-id")
    }

    private fun moduleOrFail(ctx: Context): Module {
        val currentUser = ctx.sessionAttribute<User>("user") ?: throw UnauthorizedResponse("Not logged in!")
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = getModuleOrFail(ctx.pathParam("module-id"), currentUser, access)

        if (access == Auth.Role.default && module.owner != currentUser) throw ForbiddenResponse("No permissions!")

        return module
    }

    private fun validateVersion(versionString: String): Boolean {
        val components = versionString.split(".")

        if (components.size != 3) return false

        for (component in components) {
            try {
                component.toInt()
            } catch (e: NumberFormatException) {
                return false
            }
        }

        return true
    }

    private fun deleteRelease(release: Release) {
        release.verificationMessage?.let {
            releaseWebhook.delete(it)
        }

        File("storage/${release.module.name.lowercase()}/${release.id.value}").deleteRecursively()
        release.delete()
    }

    companion object {
        fun deleteReleasesForModule(module: Module) {
            module.releases.forEach {
                File("storage/${module.name.lowercase()}/${it.id.value}").deleteRecursively()
                it.delete()
            }
        }
    }
}
