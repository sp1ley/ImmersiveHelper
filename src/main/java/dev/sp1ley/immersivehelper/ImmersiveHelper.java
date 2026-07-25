package dev.sp1ley.immersivehelper;

import dev.sp1ley.immersivehelper.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImmersiveHelper implements ModInitializer {
    public static final String MOD_ID = "immersive_helper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
