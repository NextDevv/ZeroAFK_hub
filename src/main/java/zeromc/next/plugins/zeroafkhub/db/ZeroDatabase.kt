package zeromc.next.plugins.zeroafkhub.db

import zeromc.next.plugins.zeroafkhub.ZeroAFKHub.Companion.plugin
import java.sql.Connection
import java.sql.Time

@Suppress("SqlResolve")
class ZeroDatabase(override var connection: Connection?,
                   override var host: String,
                   override var port: Int,
                   override var user: String,
                   override var password: String,
                   override var database: String
) : IDatabase {
    override fun get(uuid: String): Long? {
        val selectSQL = "SELECT * FROM zeroafk WHERE uuid = ?"
        try {
            connection!!.use { conn ->
                conn.prepareStatement(selectSQL).use { stmt ->
                    stmt.setString(1, uuid)
                    stmt.executeQuery().use { rs ->
                        stmt.close()
                        if(rs.next()) {
                            plugin.info("Found uuid=$uuid")
                            return rs.getLong("afk_time")
                        } else {
                            plugin.warn("No rows found for uuid $uuid")
                            return null
                        }
                    }
                }
            }
        }catch (e : Exception) {
            plugin.trace("Failed to get the value from the table", e)
            return null
        }
    }

    override fun set(key: String, value: Long): Long? {
        val updateSQL = "UPDATE zeroafk SET afk_time =? WHERE uuid =?"
        try {
            connection!!.prepareStatement(updateSQL).use { stmt ->
                stmt.setLong(1, value)
                stmt.setString(2, key)
                val rowsUpdated = stmt.executeUpdate()
                stmt.close()
                if(rowsUpdated == 0) {
                    plugin.warn("No rows updated for key $key")
                    return null
                }else {
                    plugin.info("Updated the time for key $key")
                    return value
                }
            }
        }catch (e: Exception) {
            plugin.trace("Failed to set the value in the table", e)
            return null
        }
    }

    override fun minus(key: String) {
        val deleteSQL = "DELETE FROM zeroafk WHERE uuid =?"
        try {
            connection!!.prepareStatement(deleteSQL).use { stmt ->
                stmt.setString(1, key)

                val rowsDeleted = stmt.executeUpdate()
                stmt.close()
                if(rowsDeleted == 0) {
                    plugin.warn("No rows deleted for key $key")
                    return
                }else {
                    plugin.info("Deleted the key $key")
                }
            }
        }catch (e: Exception) {
            plugin.trace("Failed to delete the row from the table", e)
            return
        }
        return super.minus(key)
    }
}