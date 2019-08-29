package com.chattriggers.website.api

import com.chattriggers.website.Auth
import com.chattriggers.website.api.responses.FailureResponses
import com.chattriggers.website.data.Emails
import com.chattriggers.website.data.PasswordResets
import com.chattriggers.website.data.User
import com.chattriggers.website.data.Users
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.UnauthorizedResponse
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import java.util.*

fun loginRoutes() {
    path("account") {
        post("login", ::login)
        post("new", ::new)
        get("logout", ::logout)
        get("current", ::current)

        path("reset") {
            get("request", ::requestReset)
            post("complete", ::completeReset)
        }
    }
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
    } ?: throw UnauthorizedResponse("Authentication failed.")

    // Workaround for old php-era passwords. They changed the version number for no real reason.
    val hashedPw = dbUser.password.replace("$2y$", "$2a$")

    if (BCrypt.checkpw(password, hashedPw)) {
        ctx.req.changeSessionId()

        // User correctly authenticated.
        ctx.sessionAttribute("user", dbUser)
        ctx.sessionAttribute("role", dbUser.rank)

        ctx.status(200).json(dbUser.personal())
    } else {
        throw UnauthorizedResponse("Authentication failed.")
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

        val user = User.new {
            name = newName
            email = newEmail
            password = BCrypt.hashpw(formParamOrFail(ctx, "password"), BCrypt.gensalt())
            rank = Auth.Roles.default
            createdAt = DateTime.now()
            updatedAt = DateTime.now()
        }

        ctx.status(201).json(user.personal())
    }
}

private fun logout(ctx: Context) {
    ctx.req.session.invalidate()

    ctx.status(200).result("Logged out!")
}

private fun current(ctx: Context) {
    val user = ctx.sessionAttribute<User>("user") ?: throw NotFoundResponse("No active user.")

    ctx.status(200).json(user.personal())
}

private fun requestReset(ctx: Context) = voidTransaction {
    if (ctx.sessionAttribute<User>("user") != null) {
        throw UnauthorizedResponse("Already logged in!")
    }

    val email = formParamOrFail(ctx, "email")

    val user = User.find { Users.email eq email }.firstOrNull()

    if (user == null) {
        ctx.status(200).result("If there is an account associated with that email, a password reset link has been sent!")
    }

    val randToken = UUID.randomUUID().toString()

    PasswordResets.insert {
        it[this.email] = email
        it[this.token] = randToken
        it[this.expiration] = DateTime.now().plusMinutes(30)
    }

    // Send email
    Emails.sendPasswordReset(email, randToken)

    ctx.status(200).result("If there is an account associated with that email, a password reset link has been sent!")
}

private fun completeReset(ctx: Context) = voidTransaction {
    if (ctx.sessionAttribute<User>("user") != null) {
        throw UnauthorizedResponse("Already logged in!")
    }

    val newPassword = formParamOrFail(ctx, "password")
    val givenToken = formParamOrFail(ctx, "token")

    val query = PasswordResets.select { PasswordResets.token eq givenToken }

    if (query.count() != 1) {
        throw BadRequestResponse("Bad token.")
    }

    val res = query.first()

    if (res[PasswordResets.expiration] < DateTime.now()) {
        throw BadRequestResponse("Password reset expired. Please request a new one.")
    }

    val targetedUser = User.find { Users.email eq (res[PasswordResets.email]) }.firstOrNull() ?: throw BadRequestResponse("Bad token.")

    targetedUser.password = BCrypt.hashpw(newPassword, BCrypt.gensalt())
}