package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.api.responses.ModuleMeta
import com.chattriggers.website.api.responses.ModuleResponse
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Modules
import com.chattriggers.website.data.User
import com.chattriggers.website.data.Users
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun moduleRoutes() {
    crud("modules/:module-id", ModuleController())
}

class ModuleController : CrudHandler {
    override fun create(ctx: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(ctx: Context) {
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val limit = ctx.queryParam<Int>("limit", "10").get()
        val offset = ctx.queryParam<Int>("offset", "0").get()

        val modulesResponse = transaction {
            val total = Module.count()

            var modifiers: Op<Boolean> = Op.TRUE

            ctx.queryParam<Int>("owner").getOrNull()?.let {
                modifiers = modifiers and Op.build { Modules.owner eq it }
            }

            ctx.queryParam<Boolean>("trusted").getOrNull()?.let {
                modifiers = modifiers and Op.build { Users.rank neq Auth.Roles.default }
            }

            ctx.queryParam<Boolean>("flagged").getOrNull()?.let {
                modifiers = modifiers and Op.build { Modules.hidden eq it }
            }

            val preSorted = Module.wrapRows(Modules.innerJoin(Users).slice(Modules.columns).select(modifiers))

            val modules = preSorted.orderBy(Modules.createdAt to SortOrder.DESC).limit(limit, offset).map(Module::public)

            ModuleResponse(ModuleMeta(limit, offset, total), modules)
        }

        ctx.status(200).json(modulesResponse)
    }

    override fun getOne(ctx: Context, resourceId: String) {
        val user = ctx.sessionAttribute<User>("user")
        val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

        val moduleId = resourceId.toIntOrNull()

        if (moduleId == null) {
            ctx.status(400).result("Module ID must be an integer.")
            return
        }

        val module = transaction { Module.findById(moduleId) }

        if (module == null) {
            ctx.status(404).result("Module does not exist.")
            return
        }

        if (module.hidden && access == Auth.Roles.default && module.owner != user) {
            ctx.status(404).result("Module does not exist.")
            return
        }
    }

    override fun update(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}