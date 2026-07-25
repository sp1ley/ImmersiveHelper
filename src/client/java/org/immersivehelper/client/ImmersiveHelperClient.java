package org.immersivehelper.client;

import org.immersivehelper.client.render.GuideRenderer;
import org.immersivehelper.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class ImmersiveHelperClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModEntities.GUIDE, GuideRenderer::new);
    }
}
