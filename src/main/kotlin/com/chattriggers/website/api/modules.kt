package com.chattriggers.website.api

import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context

fun moduleRoutes() {
    crud("modules/:module-name", ModuleController())
}

class ModuleController : CrudHandler {
    override fun create(ctx: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAll(ctx: Context) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOne(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(ctx: Context, resourceId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}