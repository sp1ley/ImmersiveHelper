package org.andbobiq.imh_mod.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import org.andbobiq.imh_mod.IMHModFabric;
import org.andbobiq.imh_mod.entity.GuideEntity;

public class ModEntities {

    private static final RegistryKey<EntityType<?>> GUIDE_KEY =
            RegistryKey.of(
                    RegistryKeys.ENTITY_TYPE,
                    Identifier.of(IMHModFabric.MOD_ID, "guide")
            );

    public static final EntityType<GuideEntity> GUIDE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(IMHModFabric.MOD_ID, "guide"),
                    EntityType.Builder
                            .create(GuideEntity::new, SpawnGroup.CREATURE)
                            .dimensions(0.6F, 1.8F)
                            .build(GUIDE_KEY)
            );

    public static void register() {

        FabricDefaultAttributeRegistry.register(
                GUIDE,
                GuideEntity.createAttributes()
        );

        IMHModFabric.LOGGER.info("Registered entities.");
    }
}