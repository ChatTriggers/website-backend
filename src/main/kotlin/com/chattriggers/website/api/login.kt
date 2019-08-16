package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.api.responses.FailureResponses
import com.chattriggers.website.data.User
import com.chattriggers.website.data.Users
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt

fun loginRoutes() {
    path("account") {
        post("login", ::login)
        post("new", ::new)
        get("logout", ::logout)
        get("current", ::current)
    }
}

private fun new(ctx: Context) {
    if (ctx.sessionAttribute<User>("user") != null) FailureResponses.ALREADY_LOGGED_IN.throwResponse()

    transaction {
        val newName = formParamOrFail(ctx, "name")
        val newEmail = formParamOrFail(ctx, "email")

        var existing = User.find { Users.email eq newEmail }

        if (!existing.empty()) FailureResponses.EMAIL_IN_USE.throwResponse()

        existing = User.find { Users.name eq newName }

        if (!existing.empty()) FailureResponses.NAME_IN_USE.throwResponse()

        User.new {
            name = newName
            email = newEmail
            password = BCrypt.hashpw(formParamOrFail(ctx, "password"), BCrypt.gensalt())
            rank = Auth.Roles.default
            createdAt = DateTime.now()
            updatedAt = DateTime.now()
        }

        ctx.status(201).result("User created!")
    }
}

private fun logout(ctx: Context) {
    ctx.req.session.invalidate()

    ctx.status(200).result("Logged out!")
}

private fun login(ctx: Context) {
    if (ctx.sessionAttribute<User>("user") != null) {
        ctx.status(200).json(ctx.sessionAttribute<User>("user")!!)
        return
    }

    val username = formParamOrFail(ctx, "username")
    val password = formParamOrFail(ctx, "password")

    val dbUser = transaction {
        User.find { Users.name eq username }.firstOrNull()
    } ?: return ctx.loginFail()

    // Workaround for old php-era passwords. They changed the version number for no real reason.
    val hashedPw = dbUser.password.replace("$2y$", "$2a$")

    if (BCrypt.checkpw(password, hashedPw)) {
        ctx.req.changeSessionId()

        // User correctly authenticated.
        ctx.sessionAttribute("user", dbUser)
        ctx.sessionAttribute("role", dbUser.rank)

        ctx.status(200).json(dbUser.public())
    } else {
        return ctx.loginFail()
    }
}

private fun current(ctx: Context) {
    val user = ctx.sessionAttribute<User>("user") ?: throw NotFoundResponse("No active user.")

    ctx.status(200).json(user.personal())
}

private fun Context.loginFail() {
    status(401).result("Authentication Failed.")
}

private fun formParamOrFail(ctx: Context, param: String): String {
    return ctx.formParam(param) ?: throw BadRequestResponse("'$param' parameter missing.")
}