package dev.moru3.points

import dev.moru3.minepie.config.Config
import dev.moru3.points.command.PointsCommand
import org.bukkit.plugin.java.JavaPlugin

class Points: JavaPlugin() {
    val config: Config by lazy { Config(this,"points.yml").apply { saveDefaultConfig() } }
    val languagesConfig: Config by lazy { Config(this, "languages.yml").apply { saveDefaultConfig() } }
    val pluginCommand: PointsCommand by lazy { PointsCommand(this) }
    override fun onEnable() {
        INSTANCE = this
        this.server.getPluginCommand("points")?.apply {
            tabCompleter = pluginCommand
            setExecutor(pluginCommand)
        }?: throw IllegalStateException("failed to load plugin commands.")
    }
    companion object {
        lateinit var INSTANCE: Points
            private set
    }
}