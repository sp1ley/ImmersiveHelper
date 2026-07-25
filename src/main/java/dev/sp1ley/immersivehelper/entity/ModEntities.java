package dev.sp1ley.immersivehelper.entity;

import dev.sp1ley.immersivehelper.ImmersiveHelper;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<GuideEntity> GUIDE = register(
            "guide",
            EntityType.Builder.<GuideEntity>of(GuideEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
    );

    private ModEntities() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                ImmersiveHelper.id(name)
        );
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(GUIDE, GuideEntity.createAttributes());
        ImmersiveHelper.LOGGER.info("Registered Immersive Helper entities");
    }
}
