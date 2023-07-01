package com.chattriggers.website.api

import com.chattriggers.website.data.TrackedTimestamp
import com.chattriggers.website.data.TrackedTimestamps
import com.chattriggers.website.data.TrackedUser
import com.chattriggers.website.data.TrackedUsers
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

fun trackingRoutes() {
    path("statistics") {
        get("track", ::tracking)
        get("summary", ::summary)
    }
}

private fun tracking(ctx: Context) {
    val hash = ctx.queryParam("hash") ?: throw BadRequestResponse("Expected hash query parameter")

    // This was only added in 2.0.1, so if it doesn't exist, assume 2.0.0
    val version = ctx.queryParam("version") ?: "2.0.0"

    transaction {
        val existingUser = TrackedUser.find(Op.build { TrackedUsers.hash eq hash })
        val user = if (existingUser.empty()) {
            TrackedUser.new {
                this.hash = hash
                this.version = version
            }
        } else {
            existingUser.first().also {
                it.version = version
            }
        }

        val time = DateTime.now()

        val existingTimestamp = TrackedTimestamp.find {
            (TrackedTimestamps.user eq user.id) and (TrackedTimestamps.time eq time)
        }

        if (existingTimestamp.empty()) {
            TrackedTimestamp.new {
                this.user = user
                this.time = DateTime.now()
            }
        }

        ctx.status(200)
    }
}

private fun summary(ctx: Context) {
    transaction {
        ctx.status(200).json(StatisticsSummary(TrackedUser.count()))
    }
}

data class StatisticsSummary(val totalUsers: Int)
