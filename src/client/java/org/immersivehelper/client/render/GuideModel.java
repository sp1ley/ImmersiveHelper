package org.immersivehelper.client.render;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import org.immersivehelper.ImmersiveHelper;
import org.immersivehelper.entity.GuideEntity;
import net.minecraft.resources.Identifier;

public final class GuideModel extends GeoModel<GuideEntity> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return ImmersiveHelper.id("guide");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return ImmersiveHelper.id("textures/entity/guide.png");
    }

    @Override
    public Identifier getAnimationResource(GuideEntity animatable) {
        return ImmersiveHelper.id("guide");
    }
}
