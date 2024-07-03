package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.api.responses.ModuleMeta
import com.chattriggers.website.api.responses.ModuleResponse
import com.chattriggers.website.data.*
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class ModuleController : CrudHandler {
    private val imgurRegex = """^https?:\/\/(\w+\.)?imgur.com\/[a-zA-Z0-9]{7}\.[a-zA-Z0-9]+$""".toRegex()
    private val nameRegex = """^\w{3,64}$""".toRegex()

    /**
     * Creates a new Module. Does not instantiate any releases.
     *
     * POST /modules
     *
     * Form params:
     *  - name: String
     *  - tags: Array<String>
     *  - description: String
     *  - image: String?
     *  - flagged: boolean?
     */
    override fun create(ctx: Context) {
        val currentUser = ctx.sessionAttribute<User>("user") ?: throw UnauthorizedResponse("Not logged in!")

        voidTransaction {
            val newName = formParamOrFail(ctx, "name")

            if (!newName.matches(nameRegex)) throw BadRequestResponse("Name is invalid. Could use an invalid character, be shorter than 3 characters, or longer than 64 characters.")

            val existing = Module.find { Modules.name eq newName }

            if (!existing.empty()) throw ConflictResponse("Module with name '$newName' already exists!")

            val givenTags = ctx.formParams("tags")

            if (givenTags.any { it !in allowedTags }) throw BadRequestResponse("Unapproved tag.")

            val module = Module.new {
                owner = currentUser
                name = newName
                description = formParamOrFail(ctx, "description")
                image = ctx.formParam("image")
                downloads = 0
                tags = givenTags.joinToString(separator = ",")
                hidden = ctx.formParam("flagged")?.toBoolean() ?: false
                createdAt = DateTime.now()
                updatedAt = DateTime.now()
            }

            val public = module.public()

            ctx.status(201).json(public)
        }
    }

    override fun delete(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = getModuleOrFail(resourceId, user, access)

        if (module.owner != user && access == Auth.Role.default) throw ForbiddenResponse("Can't delete this module.")

        ReleaseController.deleteReleasesForModule(module)

        module.delete()

        ctx.status(200).result("Successfully deleted module.")

        if (!module.hidden)
            Webhook.onModuleDeleted(module.public())
    }

    override fun getAll(ctx: Context) {
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default
        val currentUser = ctx.sessionAttribute<User>("user")?.name

        val modulesResponse = transaction {
            val limit = ctx.queryParamAsClass<Int>("limit").getOrDefault(10)
            val offset = ctx.queryParamAsClass<Int>("offset").getOrDefault(0)

            var modifiers: Op<Boolean> = Op.TRUE

            ctx.queryParamAsClass<Int>("owner").allowNullable().get()?.let {
                modifiers = modifiers and Op.build { Modules.owner eq it }
            }

            ctx.queryParamAsClass<Boolean>("trusted").allowNullable().get()?.let {
                modifiers = modifiers and Op.build { Users.rank neq Auth.Role.default }
            }

            ctx.queryParam("tags")?.split(",")?.let { tags ->
                modifiers = modifiers and Op.build {
                    tags.map { Modules.tags like "%$it%" }
                        .reduce { a, b -> a or b }
                }
            }

            ctx.queryParam("q")?.let {
                modifiers = modifiers and Op.build {
                    (Users.name like "%$it%") or
                            (Modules.name like "%$it%") or
                            (Modules.description like "%$it%") or
                            (Modules.tags like "%$it%")
                }
            }

            if (access != Auth.Role.default) {
                ctx.queryParamAsClass<Boolean>("flagged").allowNullable().get()?.let {
                    modifiers = modifiers and Op.build { Modules.hidden eq it }
                }
            } else {
                modifiers = modifiers and Op.build {
                    ((Modules.hidden eq false) and exists(Releases.select(Releases.module eq Modules.id))).let {
                        if (currentUser != null) {
                            // Do not apply the hidden/releases requirement if the module belongs to the user
                            (Users.name eq currentUser) or it
                        } else it
                    }
                }
            }

            val preSorted = Module.wrapRows(Modules.innerJoin(Users).slice(Modules.columns).select(modifiers))

            val total = preSorted.count()

            val sortType = (ctx.queryParam("sort") ?: "DATE_CREATED_DESC").uppercase()
            val sort = try {
                SortType.valueOf(sortType).order
            } catch (e: Exception) {
                throw BadRequestResponse("Sort type $sortType is not valid")
            }

            val modules = preSorted.orderBy(sort)
                .limit(limit, offset)

            val moduleData =
                if (access in Auth.trustedOrHigher()) modules.map(Module::authorized) else modules.map(Module::public)

            ModuleResponse(ModuleMeta(limit, offset, total), moduleData)
        }

        ctx.status(200).json(modulesResponse)
    }

    override fun getOne(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = try {
            Integer.parseInt(resourceId)
            getModuleOrFail(resourceId, user, access)
        } catch (e: NumberFormatException) {
            Module.find { Modules.name.lowerCase() eq resourceId }
                .firstOrNull() ?: throw BadRequestResponse("No module with specified resourceId")
        }

        val moduleData = if (access in Auth.trustedOrHigher() || module.owner == user) module.authorized() else module.public()

        ctx.status(200).json(moduleData)
    }

    override fun update(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

        val module = getModuleOrFail(resourceId, user, access)

        if (module.owner != user && access == Auth.Role.default) {
            throw ForbiddenResponse("Can't edit this module.")
        }

        ctx.formParam("description")?.let {
            module.description = it
        }

        ctx.formParamAsClass<String>("image").allowNullable().get()?.let {
            if (!it.matches(imgurRegex)) throw BadRequestResponse("'image' must be an imgur link.")

            module.image = it
        }

        ctx.formParam("flagged")?.let {
            when (it) {
                "true" -> module.hidden = true
                "false" -> module.hidden = false
                else -> throw BadRequestResponse("'flagged' has to be a boolean")
            }
        }

        ctx.formParamMap()["tags"]?.let { givenTags ->
            if (givenTags[0].isBlank()) {
                module.tags = ""
                return@let
            }

            if (givenTags.any { it !in allowedTags }) throw BadRequestResponse("Unapproved tag.")

            module.tags = givenTags.joinToString(separator = ",")
        }

        module.updatedAt = DateTime.now()

        ctx.status(200).json(module.public())
    }

    enum class SortType(val order: Pair<Expression<*>, SortOrder>) {
        DATE_CREATED_DESC(Modules.createdAt to SortOrder.DESC),
        DATE_CREATED_ASC(Modules.createdAt to SortOrder.ASC),
        DOWNLOADS_DESC(Modules.downloads to SortOrder.DESC),
        DOWNLOADS_ASC(Modules.downloads to SortOrder.ASC)
    }
}
