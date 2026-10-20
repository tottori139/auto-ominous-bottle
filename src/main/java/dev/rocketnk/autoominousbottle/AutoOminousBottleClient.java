package dev.rocketnk.autoominousbottle;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoOminousBottleClient implements ClientModInitializer {
	public static final String MOD_ID = "autoominousbottle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static AobConfig config;

	private static KeyMapping toggleKey;

	@Override
	public void onInitializeClient() {
		config = AobConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.autoominousbottle.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				setEnabled(!config.enabled);
			}
			AobController.tick(client);
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), AobHud::extract);
	}

	public static void setEnabled(boolean enabled) {
		config.enabled = enabled;
		config.save();
		AobController.reset();
		AobController.say(Component.translatable(enabled ? "autoominousbottle.msg.on" : "autoominousbottle.msg.off"));
	}
}
