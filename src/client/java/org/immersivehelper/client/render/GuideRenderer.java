package org.immersivehelper.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import org.immersivehelper.entity.GuideEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Environment(EnvType.CLIENT)
public final class GuideRenderer extends GeoEntityRenderer<GuideEntity, LivingEntityRenderState> {
    public GuideRenderer(EntityRendererProvider.Context context) {
        super(context, new GuideModel());
        withRenderLayer(new AliceSwordLayer(context, this));
    }
}
