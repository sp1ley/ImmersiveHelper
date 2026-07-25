package dev.sp1ley.immersivehelper.client.render;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import dev.sp1ley.immersivehelper.ImmersiveHelper;
import dev.sp1ley.immersivehelper.entity.GuideEntity;
import net.minecraft.resources.Identifier;

public final class GuideModel extends GeoModel<GuideEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return ImmersiveHelper.id("geo/guide.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return ImmersiveHelper.id("textures/entity/guide.png");
    }

    @Override
    public Identifier getAnimationResource(GuideEntity animatable) {
        return ImmersiveHelper.id("guide.animation");
    }
}
