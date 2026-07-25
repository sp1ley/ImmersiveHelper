package org.andbobiq.imh_mod.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;

import org.andbobiq.imh_mod.client.model.GuideModel;
import org.andbobiq.imh_mod.entity.GuideEntity;

import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Environment(EnvType.CLIENT)
public class GuideRenderer extends GeoEntityRenderer<GuideEntity, LivingEntityRenderState> {

    public GuideRenderer(EntityRendererFactory.Context context) {
        super(context, new GuideModel());
    }
}