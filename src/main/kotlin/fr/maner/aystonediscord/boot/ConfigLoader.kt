package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.domain.AppConfig
import org.yaml.snakeyaml.Yaml
import java.io.File

object ConfigLoader {
    private val yaml = Yaml()

    fun loadConfig(): Result<AppConfig> {
        return runCatching {
            val inputStream = File("config.yaml").takeIf { it.exists() }?.inputStream()
                ?: throw IllegalStateException("config.yaml not found in root folder or resources")

            inputStream.use {
                yaml.loadAs(it, AppConfig::class.java)
            }
        }
    }
}