package com.chattriggers.website.api

import io.javalin.core.security.Role

enum class Auth : Role {
    ADMIN, TRUSTED, DEFAULT
}