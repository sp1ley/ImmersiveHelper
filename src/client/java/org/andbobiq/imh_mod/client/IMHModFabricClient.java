package org.andbobiq.imh_mod.client;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import org.andbobiq.imh_mod.client.renderer.GuideRenderer;
import org.andbobiq.imh_mod.registry.ModEntities;

public class IMHModFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		EntityRendererRegistry.register(
				ModEntities.GUIDE,
				GuideRenderer::new
		);

	}
}