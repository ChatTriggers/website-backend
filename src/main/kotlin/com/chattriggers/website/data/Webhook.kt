package com.chattriggers.website.data

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import club.minnced.discord.webhook.send.AllowedMentions
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import com.chattriggers.website.config.DiscordConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.Instant

object Webhook : KoinComponent {
    private val discordConfig = get<DiscordConfig>()
    private lateinit var client: WebhookClient

    fun setupWebhook() {
        client = WebhookClientBuilder(discordConfig.modulesWebhook).apply {
            setDaemon(true)
            setAllowedMentions(AllowedMentions.none())
        }.build()
    }

    fun onModuleCreated(module: PublicModule) {
        sendMessage {
            setTitle(WebhookEmbed.EmbedTitle(
                "Module created: ${module.name}",
                "https://www.chattriggers.com/modules/v/${module.name}",
            ))
            addField(WebhookEmbed.EmbedField(true, "Author", module.owner.name))

            if (module.tags.isNotEmpty())
                addField(WebhookEmbed.EmbedField(true, "Tags", module.tags.joinToString()))

            val description = module.description.let {
                if (it.length > 600) {
                    it.substring(0, 597) + "..."
                } else it
            }.decode()

            if (description.isNotBlank())
                addField(WebhookEmbed.EmbedField(false, "Description", description))

            val image = module.image?.trim() ?: ""
            if (image.isNotBlank())
                setImageUrl(image)
        }
    }

    fun onModuleDeleted(module: PublicModule) {
        sendMessage {
            setTitle(WebhookEmbed.EmbedTitle("Module deleted: ${module.name}", null))
        }
    }

    fun onReleaseCreated(module: PublicModule, release: PublicRelease) {
        if (module.releases.size == 1) {
            // The module created message is not sent until it has at least one release
            onModuleCreated(module)
        }

        sendMessage {
            setTitle(WebhookEmbed.EmbedTitle(
                "Release created for module: ${module.name}",
                "https://www.chattriggers.com/modules/v/${module.name}",
            ))

            addField(WebhookEmbed.EmbedField(true, "Author", module.owner.name))
            addField(WebhookEmbed.EmbedField(true, "Release Version", release.releaseVersion))
            addField(WebhookEmbed.EmbedField(true, "Mod Version", release.modVersion))

            val changelog = release.changelog.let {
                if (it.length > 600) {
                    it.substring(0, 597) + "..."
                } else it
            }.decode()

            if (changelog.isNotBlank())
                addField(WebhookEmbed.EmbedField(false, "Changelog", changelog))
        }
    }

    private fun String.decode() = URLDecoder.decode(replace("\\n", "\n"), Charset.defaultCharset())

    private fun sendMessage(builder: WebhookEmbedBuilder.() -> Unit) {
        val embed = WebhookEmbedBuilder().apply {
            setColor(0x7b2fb5)
            setTimestamp(Instant.now())

            this.builder()
        }.build()

        val message = WebhookMessageBuilder().apply {
            setUsername("ctbot")
            setAvatarUrl("https://www.chattriggers.com/assets/images/logo-icon.png")
            addEmbeds(embed)
        }.build()

        client.send(message).exceptionally {
            println("ERROR: Failed to send webhook message")
            it.printStackTrace()
            null
        }
    }
}
