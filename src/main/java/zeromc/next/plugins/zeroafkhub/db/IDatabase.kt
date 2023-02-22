@file:Suppress("SqlResolve")

package zeromc.next.plugins.zeroafkhub.db

import zeromc.next.plugins.zeroafkhub.ZeroAFKHub
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Time

sealed interface IDatabase {
    var connection: Connection?
    var host: String
    var port: Int
    var user: String
    var password: String
    var database: String

    fun init():Boolean{
        // Create the connection
        val url = "jdbc:mysql://${host}:${port}/$database"
        try {
            connection = DriverManager.getConnection(url, user, password)

        }catch (e: Exception) {
            ZeroAFKHub.plugin.trace("Failed to connect to database: $database", e)
            ZeroAFKHub.plugin.error("Possible cause: \n\t- Invalid database name\n\t- Invalid user name\n\t- Invalid password\n\t- Database doesn't exist\n\t- Database offline")
            return false
        }

        val createTableSQL = "CREATE TABLE IF NOT EXISTS zeroafk(uuid VARCHAR(36) NOT NULL PRIMARY KEY, afk_time LONG NOT NULL)"
        connection.use { conn ->
            conn!!.createStatement().use { stmt ->
                stmt.execute(createTableSQL)
                stmt.close()
                ZeroAFKHub.plugin.info("Initialized table 'zeroafk' in database $database")
            }
        }
        return true
    }

    fun exist(uuid:String):Boolean{
        // Check if the uuid exists in the table
        val sql = "SELECT uuid FROM zeroafk WHERE uuid =?"
        try {
            connection.use { conn ->
                conn!!.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, uuid)
                    stmt.executeQuery().use { rs ->
                        stmt.close()
                        if(rs.next()){
                            return true
                        }
                    }
                }
            }
        }catch (e:Exception){
            ZeroAFKHub.plugin.trace("Failed to check if uuid $uuid exists in database $database", e)
            return false
        }
        return false
    }

    fun create(uuid:String, afkTime:Long):Boolean{
        val createTableSQL = "INSERT INTO zeroafk(uuid, afk_time) VALUES(?,?)"
        try {
            connection.use { conn ->
                conn!!.prepareStatement(createTableSQL).use { stmt ->
                    stmt.setString(1, uuid)
                    stmt.setLong(2, afkTime)
                    stmt.execute()
                    stmt.close()
                    return true
                }
            }
        }catch (e:Exception){
            ZeroAFKHub.plugin.trace("Failed to create table 'zeroafk' in database $database", e)
            return false
        }
    }

    operator fun get(key: String): Long?
    operator fun set(key: String, value: Long): Long?
    operator fun minus(key: String) {
        // Remove a key from the database
        return
    }

    fun close() : Boolean{
        // close database
        return if (connection != null) {
            connection!!.close()
            true
        }else false
    }
}