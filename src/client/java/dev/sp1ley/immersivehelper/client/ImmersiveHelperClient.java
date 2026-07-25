package dev.sp1ley.immersivehelper.client;

import dev.sp1ley.immersivehelper.client.render.GuideRenderer;
import dev.sp1ley.immersivehelper.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class ImmersiveHelperClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModEntities.GUIDE, GuideRenderer::new);
    }
}
