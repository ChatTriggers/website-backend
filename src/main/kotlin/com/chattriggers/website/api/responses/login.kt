package com.chattriggers.website.api.responses

import io.javalin.http.ConflictResponse

enum class FailureResponses {
    ALREADY_LOGGED_IN,
    NAME_IN_USE,
    EMAIL_IN_USE;

    fun throwResponse(): Nothing = throw ConflictResponse(values().indexOf(this).toString())
}