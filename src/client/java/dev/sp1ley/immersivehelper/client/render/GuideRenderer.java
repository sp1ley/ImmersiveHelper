package dev.sp1ley.immersivehelper.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import dev.sp1ley.immersivehelper.entity.GuideEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Environment(EnvType.CLIENT)
public final class GuideRenderer extends GeoEntityRenderer<GuideEntity, LivingEntityRenderState> {
    public GuideRenderer(EntityRendererProvider.Context context) {
        super(context, new GuideModel());
    }
}
