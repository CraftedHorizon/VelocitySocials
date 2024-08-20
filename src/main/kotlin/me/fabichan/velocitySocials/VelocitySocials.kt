package me.fabichan.velocitySocials

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.slf4j.Logger
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Plugin(
    id = "velocitysocials",
    name = "VelocitySocials",
    version = "1.0",
    description = "Social Commands for Velocity",
    authors = ["Fabi-Chan"]
)
class VelocitySocials @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @com.velocitypowered.api.plugin.annotation.DataDirectory private val dataDirectory: Path
) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        try {
            val configPath = dataDirectory.resolve("config.yml")

            if (Files.notExists(dataDirectory)) {
                Files.createDirectories(dataDirectory)
            }

            if (Files.notExists(configPath)) {
                javaClass.getResourceAsStream("/config.yml")?.use { inputStream ->
                    Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING)
                    logger.info("Standardkonfiguration wurde erstellt.")
                } ?: throw IllegalStateException("Konfigurationsdatei nicht in den Ressourcen gefunden")
            }

            val yaml = Yaml()
            val config = Files.newBufferedReader(configPath).use { reader ->
                yaml.load<Map<String, Any>>(reader)
            }

            val commands = config["commands"] as? Map<String, Map<String, String>>
            commands?.forEach { (key, value) ->
                val commandName = value["commandname"]
                val response = value["response"]

                if (commandName != null && response != null) {
                    server.commandManager.register(commandName, SocialCommand(response))
                    logger.info("Befehl /$commandName wurde registriert.")
                } else {
                    logger.warn("Ungültige Befehlskonfiguration für $key")
                }
            }

        } catch (e: Exception) {
            logger.error("Fehler beim Laden der Konfiguration", e)
        }

        logger.info("VelocitySocials Plugin wurde aktiviert!")
    }

    inner class SocialCommand(private val response: String) : SimpleCommand {

        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            val message = formatMessage(response)
            source.sendMessage(message)
        }

        private fun formatMessage(response: String): Component {
            val words = response.split(" ")
            val message = Component.text()

            for (word in words) {
                if (word.startsWith("http://") || word.startsWith("https://")) {
                    val link = Component.text(word)
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(word))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open link")))
                    message.append(link).append(Component.space())
                } else {
                    message.append(Component.text(word)).append(Component.space())
                }
            }

            return message.build()
        }

        override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
            return true
        }
    }
}
