package org.andbobiq.imh_mod.client.model;

import net.minecraft.util.Identifier;

import org.andbobiq.imh_mod.IMHModFabric;
import org.andbobiq.imh_mod.entity.GuideEntity;

import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class GuideModel extends GeoModel<GuideEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return IMHModFabric.id("npc/guide/geo/guide.geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return IMHModFabric.id("npc/guide/textures/guide.png");
    }

    @Override
    public Identifier getAnimationResource(GuideEntity animatable) {
        return IMHModFabric.id("npc/guide/animations/guide.animation.json");
    }
}