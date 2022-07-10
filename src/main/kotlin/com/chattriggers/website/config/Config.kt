package com.chattriggers.website.config

import java.io.File
import java.util.*

object Config {
    val properties: Properties

    val db: DbConfig
    val mail: MailConfig
    val discord: DiscordConfig

    init {
        val file = File(".env.properties")
        properties = Properties().apply { load(file.inputStream()) }

        db = DbConfig(
            properties.getProperty("db.jdbcUrl"),
            properties.getProperty("db.username"),
            properties.getProperty("db.password")
        )

        mail = MailConfig(
            properties.getProperty("mail.apikey"),
            properties.getProperty("mail.from")
        )

        discord = DiscordConfig(
            properties.getProperty("discord.verify_webhook"),
            properties.getProperty("discord.modules_webhook"),
        )
    }
}

class DbConfig(val jdbcUrl: String, val username: String, val password: String)

class MailConfig(val sendgridKey: String, val fromEmail: String)

class DiscordConfig(val verifyWebhook: String, val modulesWebhook: String)
