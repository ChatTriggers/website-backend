package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.User
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.*
import org.jetbrains.exposed.sql.transactions.transaction

fun userRoutes() {
    path("user/{user-id}") {
        post("/trust", ::trust)
        get("/modules", ::modules)
    }
}

private fun trust(ctx: Context) {
    val access = ctx.sessionAttribute<Auth.Role>("role") ?: Auth.Role.default

    if (access != Auth.Role.admin) throw ForbiddenResponse()

    transaction {
        val userId = ctx.pathParamAsClass<Int>("user-id")
            .check({ it >= 0 }, "user ID must not be negative")
            .get()

        val user = User.findById(userId) ?: throw NotFoundResponse("No user found with specified user-id")

        if (user.rank == Auth.Role.trusted) user.rank = Auth.Role.default
        else if (user.rank == Auth.Role.default) user.rank = Auth.Role.trusted

        ctx.status(200).result("Updated user information!")
    }
}

private fun modules(ctx: Context) = voidTransaction {
    val userId = ctx.pathParamAsClass<Int>("user-id")
        .check({ it >= 0 }, "user ID must not be negative")
        .get()

    val user = User.findById(userId) ?: throw NotFoundResponse("No user found with specified user-id")

    ctx.status(200).json(user.modules.map(Module::public).toList())
}