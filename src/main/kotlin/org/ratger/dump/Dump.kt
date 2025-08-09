package org.ratger.dump

import org.bukkit.plugin.java.JavaPlugin

class Dump : JavaPlugin() {

    override fun onEnable() {
        logger.severe { "Shitty" }
        server.pluginManager.registerEvents(Other(this), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
