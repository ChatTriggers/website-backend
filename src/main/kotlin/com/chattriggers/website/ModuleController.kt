package com.chattriggers.website

import com.chattriggers.website.api.METADATA_NAME
import com.chattriggers.website.api.SCRIPTS_NAME
import com.chattriggers.website.api.responses.ModuleMeta
import com.chattriggers.website.api.responses.ModuleResponse
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Modules
import com.chattriggers.website.data.User
import com.chattriggers.website.data.Users
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.*
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

class ModuleController : CrudHandler {
    private val imgurRegex = """^https?:\/\/(\w+\.)?imgur.com\/[a-zA-Z0-9]{7}\.[a-zA-Z0-9]+${'$'}""".toRegex()

    override fun create(ctx: Context) {
        val currentUser = ctx.sessionAttribute<User>("user") ?: throw UnauthorizedResponse("Not logged in!")

        voidTransaction {
            val newName = formParamOrFail(ctx, "name")
            val existing = Module.find { Modules.name eq newName }

            if (!existing.empty()) throw ConflictResponse("Module with name '$newName' already exists!")

            val uploaded = ctx.uploadedFile("module") ?: throw BadRequestResponse("Missing module file!")

            val module = Module.new {
                owner = currentUser
                name = newName
                description = formParamOrFail(ctx, "description")
                image = ctx.formParam("image")
                downloads = 0
                hidden = false
                createdAt = DateTime.now()
                updatedAt = DateTime.now()
            }

            uploaded.saveToZip(newName)

            ctx.status(201).json(module.public())
        }
    }

    private fun formParamOrFail(ctx: Context, param: String): String {
        return ctx.formParam(param) ?: throw BadRequestResponse("'$param' parameter missing.")
    }

    override fun delete(ctx: Context, resourceId: String) = voidTransaction {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val module = getModuleOrFail(resourceId, user, access)

        if (module.owner != user && access == Auth.Roles.default) throw ForbiddenResponse("Can't delete this module.")

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

            ctx.queryParam("q")?.let {
                // TODO: Search tags
                modifiers = modifiers and Op.build {
                    (Users.name like "%$it%") or (toSQLList(Modules.name, Modules.description) match it)
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

            val modules = preSorted.orderBy(Modules.createdAt to SortOrder.DESC).limit(limit, offset).map(Module::public)

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

        ctx.uploadedFile("module")?.saveToZip(module.name)

        module.updatedAt = DateTime.now()

        ctx.status(200).result("Successfully updated module.")
    }

    private fun getModuleOrFail(resourceId: String, user: User?, access: Auth.Roles): Module {
        val moduleId = resourceId.toIntOrNull() ?: throw BadRequestResponse("Module ID must be an integer.")

        val module = Module.findById(moduleId)?.load(Module::owner) ?: throw NotFoundResponse("Module does not exist.")

        if (module.hidden && access == Auth.Roles.default && module.owner != user) {
            throw NotFoundResponse("Module does not exist.")
        }

        return module
    }

    private fun voidTransaction(code: Transaction.() -> Unit) = transaction { this.code() }

    private fun UploadedFile.saveToZip(moduleName: String) {
        val zipToSave = File("storage/${moduleName.toLowerCase()}/$SCRIPTS_NAME")
        zipToSave.mkdirs()
        zipToSave.writeBytes(this.content.readBytes())

        try {
            ZipFile(zipToSave).close()
        } catch (e: Exception) {
            throw BadRequestResponse("Module is not a valid zip!")
        }

        val metadataToSave = File("storage/${moduleName.toLowerCase()}/$METADATA_NAME")

        FileSystems.newFileSystem(zipToSave.toPath(), null).use {
            Files.copy(it.getPath("metadata.json"), metadataToSave.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun <T> toSQLList(first: ExpressionWithColumnType<T>, vararg exprs: ExpressionWithColumnType<T>) = object : ExpressionWithColumnType<T>() {
        override val columnType: IColumnType
            get() = first.columnType

        override fun toSQL(queryBuilder: QueryBuilder): String {
            return "${first.toSQL(queryBuilder)},${exprs.joinToString(separator = ",") { it.toSQL(queryBuilder) }}"
        }
    }
}