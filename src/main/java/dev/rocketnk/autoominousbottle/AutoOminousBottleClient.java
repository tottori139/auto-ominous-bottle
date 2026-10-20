package dev.rocketnk.autoominousbottle;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoOminousBottleClient implements ClientModInitializer {
	public static final String MOD_ID = "autoominousbottle";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static AobConfig config;

	/** Сколько тиков ждём второе нажатие. */
	private static final int CONFIRM_TICKS = 60;

	private static KeyMapping toggleKey;
	private static int confirmTicksLeft;
	private static ClientLevel lastLevel;

	@Override
	public void onInitializeClient() {
		config = AobConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		toggleKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.autoominousbottle.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (confirmTicksLeft > 0) confirmTicksLeft--;
			while (toggleKey.consumeClick()) {
				onTogglePressed(client);
			}
			checkAutoDisable(client);
			AobController.tick(client);
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), AobHud::extract);
	}

	public static KeyMapping toggleKey() {
		return toggleKey;
	}

	public static void setEnabled(boolean enabled) {
		setEnabled(enabled, Component.translatable(enabled ? "autoominousbottle.msg.on" : "autoominousbottle.msg.off"));
	}

	public static void disable(Component message) {
		setEnabled(false, message);
	}

	private static void setEnabled(boolean enabled, Component message) {
		config.enabled = enabled;
		config.save();
		AobController.reset();
		AobController.say(message);
		AobSounds.play(enabled ? AobSounds.ENABLE : AobSounds.DISABLE);
	}

	/** Выключение сразу, включение — только вторым нажатием, чтобы не выпить бутылку случайно. */
	private static void onTogglePressed(Minecraft client) {
		if (config.enabled) {
			setEnabled(false);
			return;
		}
		if (!config.confirmEnable || confirmTicksLeft > 0) {
			confirmTicksLeft = 0;
			setEnabled(true);
			return;
		}
		confirmTicksLeft = CONFIRM_TICKS;
		AobSounds.play(AobSounds.CONFIRM);
		client.gui.hud.setOverlayMessage(
				Component.translatable("autoominousbottle.msg.confirm", toggleKey.getTranslatedKeyMessage()), false);
	}

	/** После перезахода, смены измерения или смерти мод выключается, если это не отключено в настройках. */
	private static void checkAutoDisable(Minecraft client) {
		ClientLevel level = client.level;
		boolean levelChanged = level != lastLevel;
		lastLevel = level;
		if (!config.enabled || !config.autoDisable) return;

		if (levelChanged) {
			setEnabled(false, Component.translatable("autoominousbottle.msg.off_world"));
		} else if (client.player != null && client.player.isDeadOrDying()) {
			setEnabled(false, Component.translatable("autoominousbottle.msg.off_death"));
		}
	}
}
