package com.chattriggers.website

import io.javalin.core.JavalinConfig
import io.javalin.core.security.RouteRole
import io.javalin.http.Context

object Auth {
    fun configure(config: JavalinConfig) {
        config.accessManager { handler, ctx, permittedRoles ->
            // If a route doesn't specify what roles can access it, assume everyone can.
            if (permittedRoles.isEmpty()) {
                handler.handle(ctx)
                return@accessManager
            }

            val role = getRoleForContext(ctx)

            if (role in permittedRoles) {
                handler.handle(ctx)
            } else {
                ctx.status(403).result("Forbidden")
            }
        }
    }

    private fun getRoleForContext(ctx: Context): Role {
        return ctx.sessionAttribute<Role>("role") ?: return Role.default
    }

    @Suppress("EnumEntryName")
    enum class Role : RouteRole {
        admin, trusted, default
    }

    fun allRoles() = setOf(Role.default, Role.trusted, Role.admin)
    fun trustedOrHigher() = setOf(Role.trusted, Role.admin)
    fun adminOnly() = setOf(Role.admin)
}