package zeromc.next.plugins.zeroafkhub

import JsonFile.JsonFile
import com.google.common.io.ByteStreams
import io.github.leonardosnt.bungeechannelapi.BungeeChannelApi
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import zeromc.next.plugins.zeroafk.utils.tac
import zeromc.next.plugins.zeroafkhub.db.ZeroDatabase
import zeromc.next.plugins.zeroafkhub.events.PlayerEvents
import zeromc.next.plugins.zeroafkhub.logger.FileLogger
import zeromc.next.plugins.zeroafkhub.logger.LogLevel
import java.lang.Exception
import java.util.UUID
import kotlin.properties.Delegates

class ZeroAFKHub : JavaPlugin(),PluginMessageListener {
    var api: BungeeChannelApi by Delegates.notNull()
    val playersAfk:HashMap<UUID, String> = hashMapOf()
    val startAfk:HashMap<UUID, Long> = hashMapOf()
    var serverName:String = "Unknown"
    var database: ZeroDatabase by Delegates.notNull()

    companion object {
        var plugin: ZeroAFKHub by Delegates.notNull()
    }

    fun info(message: String) {
        logger.info(message.tac())
        //FileLogger.logIntoFile(LogLevel.INFO, message)
    }

    fun warn(message: String) {
        logger.warning(message.tac())
        //FileLogger.logIntoFile(LogLevel.WARN, message)
    }

    fun error(message: String) {
        logger.severe(message.tac())
        //FileLogger.logIntoFile(LogLevel.ERROR, message)
    }

    fun trace(message: String, exception: Exception) {
        logger.severe("$message\n\t-> View the log file for more information.")
        //FileLogger.logIntoFile(LogLevel.TRACE, message, exception)
    }

    override fun onEnable() {
        // Plugin startup logic
        plugin = this
        info("**************************************")
        info("loading configurations for ZeroAFK hub")

        // Loading logger
        //FileLogger.setPath(dataFolder.path+"\\logs")

        // Loading config file
        saveDefaultConfig()
        info("Loaded config file successfully...")

        info("Loading database configurations...")
        val host = config.getString("database.host")
        var port = config.getInt("database.port")
        val username = config.getString("database.user")
        val password = config.getString("database.password")
        val databaseName = config.getString("database.database")

        if(port == 0)
            port = 3306

        if(host.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty() || databaseName.isNullOrEmpty()) {
            info("ZeroAFK hub is not configured. Please check your config file.")
            info("No database configurations were loaded.")
            info("**************************************")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }
        info("Loading database...")
        database = ZeroDatabase(
            connection = null,
            host = host,
            port = port,
            user = username,
            password = password,
            database = databaseName
        )
        info("Initialising database...")
        val initialized = database.init()
        if(!initialized) {
            error("Failed to initialize database")
            error("Please check your config file for any errors.")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }else info("Successfully initialised database...")

        // Data files
        val data = JsonFile("${dataFolder.path}${System.getProperty("file.separator")}data","data")
        if(!data.exists()) {
            info("No data files found, creating default...")
            data.create(
                hashMapOf("default" to "default")
            )
            data.save()
        }
        info("Loaded data files successfully...")

        // Registering channel listeners
        api = BungeeChannelApi.of(this)
        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        this.server.messenger.registerIncomingPluginChannel(this, "BungeeCord", this)
        info("BungeeCord channel registered...")

        // Registering listeners
        Bukkit.getPluginManager().registerEvents(PlayerEvents(), this)

        info("Loaded configurations for ZeroAFK hub")
        info("**************************************")

        try {
            api.registerForwardListener("Server") { channelName: String?, player: Player?, data: ByteArray? ->
                val serverName = String(data!!)
                playersAfk[player!!.uniqueId] = serverName
                startAfk[player.uniqueId] = System.currentTimeMillis()
                info("Received data from server: $serverName")
                info("Loaded player into map: ${playersAfk.containsKey(player.uniqueId)}")
            }

            api.registerForwardListener("Debug") { channelName: String?, player: Player?, data: ByteArray? ->
                val serverName = String(data!!)
                info("[Debug] Received data from server: $serverName")
            }
        }catch (e: Exception) {
            trace("Error while fetching request", e)
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
        this.server.messenger.unregisterOutgoingPluginChannel(this)
        this.server.messenger.unregisterIncomingPluginChannel(this)
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if(channel != "BungeeCord") return

        val `in` = ByteStreams.newDataInput(message)
        val s = `in`.readUTF()
    }
}