package ru.herobrine1st.fusion.database


import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import ru.herobrinr1st.fusion.database.Database


object DatabaseFactory {
    lateinit var database: Database
        private set

    fun init(
        host: String,
        port: Int,
        database: String,
        username: String,
        password: String,
        dbms: String
    ) {
        val ds = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = "jdbc:$dbms://$host:$port/$database"
            this.username = username
            this.password = password
        })
        val driver = ds.asJdbcDriver()

        this.database = Database(
            driver = driver,
        )

        this.database.transaction {
            val version = driver.getVersion()
            if (version == 0L) driver.migrationBoilerplate {
                Database.Schema.create(driver).value
            } else if (version < Database.Schema.version) driver.migrationBoilerplate {
                Database.Schema.migrate(driver, version.toInt(), Database.Schema.version).value
            }
        }
    }


    // Legacy from v1.5
    private fun SqlDriver.getVersion(): Long {
        execute(null, "CREATE TABLE IF NOT EXISTS metadata(version BIGINT NOT NULL)", 0)
        return executeQuery(
            identifier = null,
            sql = "SELECT IF(COUNT(*) > 0, version, 0) FROM metadata;",
            mapper = {
                it.next()
                it.getLong(0)!!
            },
            parameters = 0,
        ).value
    }

    private fun SqlDriver.setVersion(version: Int) {
        execute(null, "DELETE FROM metadata;", 0).value
        execute(null, "INSERT INTO metadata VALUES (?);", 1) {
            bindLong(0, version.toLong())
        }.value
    }

    private inline fun SqlDriver.migrationBoilerplate(crossinline block: () -> Unit) {
        // SQLDelight wtf?? How to set order???
        execute(null, "SET foreign_key_checks=0", 0).value
        block()
        execute(null, "SET foreign_key_checks=1", 0).value
        setVersion(Database.Schema.version)
    }
}

val applicationDatabase get() = DatabaseFactory.database
