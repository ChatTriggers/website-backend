package com.chattriggers.website.config

import com.zaxxer.hikari.HikariDataSource

class DbConfig(val jdbcUrl: String, val username: String, val password: String) {
    val dataSource = HikariDataSource().also {
        it.jdbcUrl = jdbcUrl
        it.username = username
        it.password = password
    }
}