package net.elysium.mod.fabricord;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static net.elysium.mod.fabricord.Fabricord.MOD_ID;

public class ConfigManager {

    private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);

    private static String botToken;
    private static String logChannelID;
    private static String botOnlineStatus;

    public void checkAndCreateConfig() {
        try {
            LOGGER.info("Checking data folders...");

        File dataFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), MOD_ID);
        if (!dataFolder.exists()) {
            LOGGER.info("Data folder not found, Start generation.");
            if (dataFolder.mkdirs()) {
                LOGGER.info("Data folder successfully created.");
            } else {
                LOGGER.error("Failed to create data folder.");
            }
        }

        File configFile = new File(dataFolder, "config.yml");

        if (configFile.exists()) {
            LOGGER.info("Config file already exists. Method execution continuing...");
            loadConfig(dataFolder);
        } else {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (input == null) {
                    LOGGER.error("config.yml is not found in resources.");
                } else {
                    Files.copy(input, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Config file successfully created");
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create config file", e);
            }
        }
    } catch (Exception e) {
        LOGGER.error("An error occurred while checking and creating config:", e);
    }
}

    private void loadConfig(File pluginDataFolder) {
        try {
        File configFile = new File(pluginDataFolder, "config.yml");
        if (!configFile.exists()) return;

        Yaml yaml = new Yaml();
        try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
            Map<String, Object> config = yaml.load(fileInputStream);
            botToken = (String) config.getOrDefault("BotToken", "");
            logChannelID = (String) config.getOrDefault("Log_Channel_ID", "");
            botOnlineStatus = (String) config.getOrDefault("Bot_Online_Status", "");
        } catch (IOException e) {
            LOGGER.error("Failed to load config file", e);
        }
    } catch (Exception e) {
        LOGGER.error("An error occurred while loading config:", e);
    }
}

    public static String getBotToken() {
        return botToken;
    }

    public static String getLogChannelID() {
        return logChannelID;
    }

    public static String getBotOnlineStatus() {
        return botOnlineStatus;
    }
}

