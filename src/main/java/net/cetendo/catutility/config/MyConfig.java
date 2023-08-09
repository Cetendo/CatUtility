package net.cetendo.catutility.config;

import dev.isxander.yacl.api.ConfigCategory;
import dev.isxander.yacl.api.Option;
import dev.isxander.yacl.api.OptionGroup;
import dev.isxander.yacl.config.ConfigEntry;
import dev.isxander.yacl.config.ConfigInstance;
import dev.isxander.yacl.config.GsonConfigInstance;
import dev.isxander.yacl.gui.controllers.BooleanController;
import dev.isxander.yacl.gui.controllers.ColorController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.awt.*;

public class MyConfig {
    public static final ConfigInstance<MyConfig> INSTANCE = new GsonConfigInstance<>(
            MyConfig.class,
            FabricLoader.getInstance().getConfigDir().resolve("catutility.json")
    );
    @ConfigEntry public boolean reach = true;
    @ConfigEntry public boolean showTarget = true;
    @ConfigEntry public Color mycolor = Color.white;

    public static ConfigCategory getConfigCategory() {
        MyConfig config = INSTANCE.getConfig();
        MyConfig defaults = INSTANCE.getDefaults();

        return ConfigCategory.createBuilder()
                .name(Text.translatable("text.config.catutility.title"))
                .group(OptionGroup.createBuilder()
                        .name(Text.translatable("text.config.catutility.optiongroup.reach"))
                        .collapsed(true)
                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("text.config.catutility.option.reach"))
                                .tooltip(Text.translatable("text.config.catutility.option.reach.tooltip"))
                                .binding(defaults.reach, () -> config.reach, val -> config.reach = val)
                                .controller(BooleanController::new)
                                .instant(true)
                                .build())
                        .option(Option.createBuilder(boolean.class)
                                .name(Text.translatable("text.config.catutility.option.reach.showTarget"))
                                .tooltip(Text.translatable("text.config.catutility.option.reach.showTarget.tooltip"))
                                .binding(defaults.showTarget, () -> config.showTarget, val -> config.showTarget = val)
                                .controller(BooleanController::new)
                                .instant(true)
                                .build())
                        .option(Option.createBuilder(Color.class)
                                .name(Text.translatable("text.config.catutility.option.color"))
                                .binding(defaults.mycolor, () -> config.mycolor, val -> config.mycolor = val)
                                .controller(ColorController::new)
                                .build())
                        .build())
                .build();
    }
}