package net.cetendo.catutility.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl.api.YetAnotherConfigLib;
import net.minecraft.text.Text;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("text.config.catutility.title"))
                .category(MyConfig.getConfigCategory())
                .save(MyConfig.INSTANCE::save)
                .build().generateScreen(parent);
    }
}
