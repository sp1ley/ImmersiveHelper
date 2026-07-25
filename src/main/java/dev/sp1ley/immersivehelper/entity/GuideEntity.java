package dev.sp1ley.immersivehelper.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public final class GuideEntity extends Mob implements GeoEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public GuideEntity(EntityType<? extends GuideEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Controllers will be registered when the Guide animation resource is added.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
