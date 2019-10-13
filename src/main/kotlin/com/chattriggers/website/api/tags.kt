package com.chattriggers.website.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Context
import java.io.File

fun tagRoutes() {
    get("tags", ::getTags)
}

const val TAGS_FILE = "tags.txt"
const val TAGS_CHECK_TIMEOUT = 1000 * 60 * 30
var allowedTags = File(TAGS_FILE).readText().split("\n").map { it.trim() }
var lastTagsCheckTime = System.currentTimeMillis()

fun getTags(ctx: Context) {
    if (System.currentTimeMillis() - lastTagsCheckTime > TAGS_CHECK_TIMEOUT) {
        allowedTags = File(TAGS_FILE).readText().split("\n").map { it.trim() }

        lastTagsCheckTime = System.currentTimeMillis()
    }

    ctx.status(200).json(allowedTags)
}