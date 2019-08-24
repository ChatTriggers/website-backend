package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.data.User
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.transactions.transaction

fun userRoutes() {
    path("user/:user-id") {
        post("/trust", ::trust)
        get("/modules", ::modules)
    }
}

private fun trust(ctx: Context) {
    val access = ctx.sessionAttribute<Auth.Roles>("role") ?: Auth.Roles.default

    if (access != Auth.Roles.admin) throw ForbiddenResponse()

    transaction {
        val userId = ctx.pathParam<Int>("user-id").getOrNull()
            ?: throw BadRequestResponse("user-id must be an int.")

        val user = User.findById(userId) ?: throw NotFoundResponse("No user found with specified user-id")

        if (user.rank == Auth.Roles.trusted) user.rank = Auth.Roles.default
        else if (user.rank == Auth.Roles.default) user.rank = Auth.Roles.trusted

        ctx.status(200).result("Updated user information!")
    }
}

private fun modules(ctx: Context) {
    transaction {
        val userId = ctx.pathParam<Int>("user-id").getOrNull()
            ?: throw BadRequestResponse("user-id must be an int.")

        val user = User.findById(userId) ?: throw NotFoundResponse("No user found with specified user-id")

        ctx.status(200).json(user.modules.toList())
    }
}