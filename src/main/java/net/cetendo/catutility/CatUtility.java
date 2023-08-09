package net.cetendo.catutility;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class CatUtility implements ModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("CatUtility");
    public static final MinecraftClient MC = MinecraftClient.getInstance();

    @Override
    public void onInitialize() {
        LOGGER.info("Miao Miao");
    }
}
