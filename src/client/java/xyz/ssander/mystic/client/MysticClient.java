package xyz.ssander.mystic.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityTypes;

import xyz.ssander.mystic.Mystic;
import xyz.ssander.mystic.network.ShieldPayload;

public class MysticClient implements ClientModInitializer {
	public static KeyMapping shieldKey;
	private static boolean shieldKeyWasDown;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(Mystic.id("mystic"));
		shieldKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.mystic.shield", InputConstants.KEY_X, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			sendShieldKey(client);
			MysticPoses.tick(client);
			MysticEffects.tick(client);
		});
		LevelRenderEvents.COLLECT_SUBMITS.register(MysticEffects::render);

		EntityRendererRegistry.register(Mystic.SLING_PORTAL, PortalRenderer::new);

		ModelLayerRegistry.registerModelLayer(CloakModel.LAYER, CloakModel::createLayer);
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
			if (entityType == EntityTypes.PLAYER && renderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new CloakLayer(avatarRenderer, new CloakModel(context.bakeLayer(CloakModel.LAYER))));
			}
		});
	}

	/** Tells the server when the shield key goes down or up. */
	private static void sendShieldKey(Minecraft client) {
		boolean down = client.player != null && client.gui.screen() == null && shieldKey.isDown();
		if (down != shieldKeyWasDown && client.player != null) {
			ClientPlayNetworking.send(new ShieldPayload(down));
		}
		shieldKeyWasDown = down;
	}
}
