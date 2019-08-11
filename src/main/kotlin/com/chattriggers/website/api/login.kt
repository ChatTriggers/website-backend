package com.chattriggers.website.api

import com.chattriggers.website.data.User
import com.chattriggers.website.data.Users
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

fun loginRoutes() {
    path("account") {
        post("login", ::login)
        post("new", ::new)
    }
}

private fun new(ctx: Context) {

}

private fun login(ctx: Context) {
    val username = ctx.queryParam<String>("username").getOrNull() ?: return ctx.loginFail()
    val password = ctx.queryParam<String>("password").getOrNull() ?: return ctx.loginFail()

    val dbUser = transaction {
        User.find { Users.name eq username }.firstOrNull()
    } ?: return ctx.loginFail()

    // Workaround for old php-era passwords. They changed the version number for no real reason.
    val hashedPw = dbUser.password.replace("$2y$", "$2a$")

    if (BCrypt.checkpw(password, hashedPw)) {
        // User correctly authenticated.
        ctx.sessionAttribute("user", dbUser)

        ctx.status(200).result("Authenticated!")
    } else {
        return ctx.loginFail()
    }
}

private fun Context.loginFail() {
    status(401).result("Authentication Failed.")
}