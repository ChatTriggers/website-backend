package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.api.responses.ModuleMeta
import com.chattriggers.website.api.responses.ModuleResponse
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Modules
import com.chattriggers.website.data.User
import com.chattriggers.website.data.Users
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
                hidden = false
                createdAt = DateTime.now()
                updatedAt = DateTime.now()
            }

            ctx.status(201).json(module.public())
        }
    }

    override fun delete(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(resourceId, user, access)

        if (module.owner != user && access == Auth.Roles.default) throw ForbiddenResponse("Can't delete this module.")

        ReleaseController.deleteModule(module)

        module.delete()

        ctx.status(200).result("Successfully deleted module.")
    }

    override fun getAll(ctx: Context) {
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val modulesResponse = transaction {
            val limit = ctx.queryParam<Int>("limit", "10").get()
            val offset = ctx.queryParam<Int>("offset", "0").get()

            var modifiers: Op<Boolean> = Op.TRUE

            ctx.queryParam<Int>("owner").getOrNull()?.let {
                modifiers = modifiers and Op.build { Modules.owner eq it }
            }

            ctx.queryParam<Boolean>("trusted").getOrNull()?.let {
                modifiers = modifiers and Op.build { Users.rank neq Auth.Roles.default }
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

            if (access != Auth.Roles.default) {
                ctx.queryParam<Boolean>("flagged").getOrNull()?.let {
                    modifiers = modifiers and Op.build { Modules.hidden eq it }
                }
            } else {
                modifiers = modifiers and Op.build { Modules.hidden eq false }
            }

            val preSorted = Module.wrapRows(Modules.innerJoin(Users).slice(Modules.columns).select(modifiers))

            val total = preSorted.count()

            val sortType = (ctx.queryParam("sort") ?: "DATE_CREATED_DESC").toUpperCase()
            val sort = try { SortType.valueOf(sortType).order }
                        catch (e: Exception) { throw BadRequestResponse("Sort type $sortType is not valid") }

            val modules = preSorted.orderBy(sort)
                .limit(limit, offset)
                .map(Module::public)

            ModuleResponse(ModuleMeta(limit, offset, total), modules)
        }

        ctx.status(200).json(modulesResponse)
    }

    override fun getOne(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(resourceId, user, access)

        ctx.status(200).json(module.public())
    }

    override fun update(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(resourceId, user, access)

        if (module.owner != user && access == Auth.Roles.default) {
            throw ForbiddenResponse("Can't edit this module.")
        }

        ctx.formParam("description")?.let {
            module.description = it
        }

        ctx.formParam<String>("image").getOrNull()?.let {
            if (!it.matches(imgurRegex)) throw BadRequestResponse("'image' must be an imgur link.")

            module.image = it
        }

        ctx.formParam("flagged")?.let {
            when (it) {
                "true" -> module.hidden = true
                "false" -> module.hidden = false
                else -> throw BadRequestResponse("'hidden' has to be a boolean")
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