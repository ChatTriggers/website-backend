package com.chattriggers.website.data

import org.jetbrains.exposed.sql.Table

object PasswordResets : Table() {
    val email = varchar("email", 191)
    val token = varchar("token", 36)
    val expiration = datetime("expiration")
}