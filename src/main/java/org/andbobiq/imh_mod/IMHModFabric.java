package org.andbobiq.imh_mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.andbobiq.imh_mod.registry.ModEntities;

public class IMHModFabric implements ModInitializer {
	public static final String MOD_ID = "imh_mod";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}