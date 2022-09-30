package dev.moru3.points

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.moru3.minepie.config.Config
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.moru3.points.command.PointsCommand
import dev.moru3.points.database.Histories
import dev.moru3.points.database.OperationHistory
import dev.moru3.points.database.Players
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class Points: JavaPlugin() {
    val config: Config by lazy { Config(this,"points.yml").apply { saveDefaultConfig() } }
    val languagesConfig: Config by lazy { Config(this, "languages.yml").apply { saveDefaultConfig() } }
    val pluginCommand: PointsCommand by lazy { PointsCommand(this) }
    lateinit var dataSource: HikariDataSource
    override fun onEnable() {
        config.saveDefaultConfig()
        languagesConfig.saveDefaultConfig()
        super.saveDefaultConfig()
        val config = {path:String->super.getConfig().getString(path)!!}
        val databaseHost = System.getenv("DATABASE_HOST")
        val databaseUsername = System.getenv("DATABASE_USERNAME")
        val databasePassword = System.getenv("DATABASE_PASSWORD")
        val hikariConfig: HikariConfig
        if(databaseUsername!=null&&databaseHost!=null&&databasePassword!=null) {
            hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://${databaseHost}/points"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                username = databaseUsername
                password = databasePassword
                maximumPoolSize = 10
            }
        } else {
            hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://${config("database.host")}/${config("database.name")}"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                username = config("database.username")
                password = config("database.password")
                maximumPoolSize = 10
            }
        }
        dataSource = HikariDataSource(hikariConfig)

        Database.connect(dataSource)
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Players, Histories, OperationHistory)
        }
        this.registerEvent<PlayerJoinEvent> { event ->
            transaction {
                Players.insertIgnore { it[uniqueId] = event.player.uniqueId;it[name] = event.player.name }
            }
        }
        INSTANCE = this
        this.server.getPluginCommand("points")?.apply {
            tabCompleter = pluginCommand
            setExecutor(pluginCommand)
        }?: throw IllegalStateException("failed to load plugin commands.")
    }

    override fun onDisable() {
        dataSource.close()
    }
    companion object {
        lateinit var INSTANCE: Points
            private set
    }
}