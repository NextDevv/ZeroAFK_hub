package zeromc.next.plugins.zeroafkhub.events

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import zeromc.next.plugins.zeroafk.utils.tac
import zeromc.next.plugins.zeroafkhub.ZeroAFKHub
import zeromc.next.plugins.zeroafkhub.ZeroAFKHub.Companion.plugin
import zeromc.next.plugins.zeroafkhub.db.ZeroDatabase
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant

class PlayerEvents:Listener {

    @EventHandler
    @Suppress("DuplicatedCode")
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.database.create(event.player.uniqueId.toString(), 0L)
        plugin.info("Player ${event.player.name} joined the hub.")
        plugin.info("Player ${event.player.name} is in the map: ${plugin.playersAfk.containsKey(event.player.uniqueId)}")
        object : BukkitRunnable() {
            var notAfkCounter = 0
            override fun run() {
                if(notAfkCounter >= 10)
                    this.cancel()
                if(plugin.playersAfk.containsKey(event.player.uniqueId)) {
                    event.player.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.BLINDNESS, Int.MAX_VALUE, 2
                        )
                    )
                    event.player.sendTitle(plugin.config.getString("afk-settings.afk-title.header")?.tac(),plugin.config.getString("afk-settings.afk-title.footer")?.tac())
                }else notAfkCounter++
            }
        }.runTaskTimer(plugin, 20L, 20L)
        event.player.removePotionEffect(PotionEffectType.BLINDNESS)
    }

    private fun getPlayerStartAfk(player: Player):Long = plugin.startAfk[player.uniqueId] ?: 0L

    @Suppress("DuplicatedCode")
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        with(event.player) {
            plugin.info("Player ${this.name} left the hub.")

            plugin.api.forwardToPlayer(this.name, "Afk", "Afk time".toByteArray())

            val startAfk = getPlayerStartAfk(player = this)
            val endAfk = if(startAfk > 0) System.currentTimeMillis() else 0L
            val duration = endAfk - startAfk

            plugin.info("startAfk: $startAfk, endAfk: $endAfk, duration: $duration")

            val playerTotalAfkDuration = plugin.database[this.uniqueId.toString()] ?: 0L
            val total = playerTotalAfkDuration + duration

            plugin.info("playerTotalAfkDuration: $playerTotalAfkDuration, total: $total")

            if(plugin.database[this.uniqueId.toString()] == null) {
                plugin.database.create(this.uniqueId.toString(), total)
            }else plugin.database[this.uniqueId.toString()] = total

            plugin.playersAfk.remove(this.uniqueId)
        }
    }

    @Suppress("DuplicatedCode")
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if(plugin.playersAfk.containsKey(event.player.uniqueId)) {
            event.player.resetTitle()

            val startAfk = getPlayerStartAfk(player = event.player)
            val endAfk = if(startAfk > 0) System.currentTimeMillis() else 0L
            val duration = endAfk - startAfk

            plugin.info("startAfk: $startAfk, endAfk: $endAfk, duration: $duration")

            val playerTotalAfkDuration = plugin.database[event.player.uniqueId.toString()] ?: 0L
            val total = playerTotalAfkDuration + duration

            plugin.info("playerTotalAfkDuration: $playerTotalAfkDuration, total: $total")

            if(plugin.database[event.player.uniqueId.toString()] == null) {
                plugin.database.create(event.player.uniqueId.toString(), total)
            }else plugin.database[event.player.uniqueId.toString()] = total

            plugin.api.connect(event.player, plugin.playersAfk[event.player.uniqueId])
            plugin.playersAfk.remove(event.player.uniqueId)
        }
    }
}