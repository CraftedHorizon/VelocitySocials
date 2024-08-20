package me.fabichan.velocitySocials;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import org.slf4j.Logger

@Plugin(
    id = "velocitysocials",
    name = "VelocitySocials",
    version = "1.0",
    description = "Socvi",
    authors = ["Fabi-Chan"]
)
class VelocitySocials @Inject constructor(val logger: Logger) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
    }
}
