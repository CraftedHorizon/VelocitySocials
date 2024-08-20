package me.fabichan.velocitySocials

import com.google.inject.Inject
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

    private lateinit var commands: Map<String, Map<String, String>>

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        loadConfig()
        registerCommands()
        server.commandManager.register("vs", ReloadCommand())
        logger.info("VelocitySocials Plugin wurde aktiviert!")
    }

    private fun loadConfig() {
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
            commands = Files.newBufferedReader(configPath).use { reader ->
                yaml.load<Map<String, Any>>(reader)["commands"] as? Map<String, Map<String, String>>
                    ?: emptyMap()
            }

            logger.info("Konfiguration erfolgreich geladen.")

        } catch (e: Exception) {
            logger.error("Fehler beim Laden der Konfiguration", e)
        }
    }

    private fun registerCommands() {
        commands.forEach { (key, value) ->
            val commandName = value["commandname"]
            val response = value["response"]

            if (commandName != null && response != null) {
                server.commandManager.register(commandName, SocialCommand(response))
                logger.info("Befehl /$commandName wurde registriert.")
            } else {
                logger.warn("Ungültige Befehlskonfiguration für $key")
            }
        }
    }
    
    private fun unregisterCommands() {
        commands.keys.forEach { server.commandManager.unregister(it) }
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
                        .hoverEvent(HoverEvent.showText(Component.text("Klicke, um den Link zu öffnen.")))
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

    inner class ReloadCommand : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val source = invocation.source()
            loadConfig()
            unregisterCommands()
            registerCommands()
            source.sendMessage(Component.text("Konfiguration erfolgreich neu geladen!").color(NamedTextColor.GREEN))
        }

        override fun hasPermission(invocation: SimpleCommand.Invocation): Boolean {
            return invocation.source().hasPermission("velocitysocials.reload")
        }
    }
}
