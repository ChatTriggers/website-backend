package com.chattriggers.website.api

import com.chattriggers.website.data.TrackedUser
import com.chattriggers.website.data.TrackedUsers
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun trackingRoutes() {
    path("statistics") {
        get("track", ::tracking)
        get("summary", ::summary)
    }
}

private fun tracking(ctx: Context) {
    val hashQueryParam = ctx.queryParam("hash") ?: throw BadRequestResponse("Expected hash query parameter")
    val hashBytes = Base64.getUrlDecoder().decode(hashQueryParam)
    val hash = String(hashBytes, Charsets.UTF_8)

    transaction {
        val existingUser = TrackedUser.find(Op.build { TrackedUsers.hash eq hash })
        if (existingUser.empty())
            TrackedUser.new { this.hash = hash }

        ctx.status(200)
    }
}

private fun summary(ctx: Context) {
    transaction {
        ctx.status(200).json(StatisticsSummary(TrackedUser.count()))
    }
}

data class StatisticsSummary(val totalUsers: Int)
