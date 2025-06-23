package fr.maner.aystonediscord.boot

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fr.maner.aystonediscord.domain.DatabaseConfig
import fr.maner.aystonediscord.domain.model.AystoneInstancesTable
import fr.maner.aystonediscord.domain.model.AystonePlayersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseConnection(
    private val dbConfig: DatabaseConfig
) {
    private lateinit var dataSource: HikariDataSource

    fun connect() {
        val jdbcUrl = "jdbc:postgresql://${dbConfig.host}:${dbConfig.port}/${dbConfig.name}"
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbConfig.username
            this.password = dbConfig.password
            this.schema = dbConfig.schema
            this.maximumPoolSize = 10
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            this.validate()
        }

        dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(AystoneInstancesTable, AystonePlayersTable)
        }

        println("Connected to database: $jdbcUrl")
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
            println("Database connection closed")
        }
    }
}