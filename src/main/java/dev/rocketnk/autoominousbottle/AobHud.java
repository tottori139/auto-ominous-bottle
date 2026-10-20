package dev.rocketnk.autoominousbottle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Маленький счётчик в левом верхнем углу. */
public final class AobHud {
	private static final int TITLE_COLOR = 0xFFFFAA00;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int STATE_COLOR = 0xFFAAAAAA;
	private static final int LINE_HEIGHT = 10;

	private AobHud() {
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		AobConfig config = AutoOminousBottleClient.config;
		Minecraft mc = Minecraft.getInstance();
		if (!config.enabled || !config.showHud || mc.player == null) return;

		int x = 4;
		int y = 4;
		graphics.text(mc.font, Component.translatable("autoominousbottle.hud.title"), x, y, TITLE_COLOR, true);
		y += LINE_HEIGHT;
		graphics.text(mc.font, Component.translatable("autoominousbottle.hud.raids", config.raidsWon), x, y, TEXT_COLOR, true);
		y += LINE_HEIGHT;
		graphics.text(mc.font, Component.translatable("autoominousbottle.hud.bottles", AobController.countBottles(mc.player)), x, y, TEXT_COLOR, true);
		y += LINE_HEIGHT;
		graphics.text(mc.font, stateText(), x, y, STATE_COLOR, true);
	}

	private static Component stateText() {
		return switch (AobController.state()) {
			case RAID -> Component.translatable("autoominousbottle.hud.state.raid");
			case OMEN -> Component.translatable("autoominousbottle.hud.state.omen");
			case WAIT -> Component.translatable("autoominousbottle.hud.state.wait", AobController.secondsUntilDrink());
			case DRINKING -> Component.translatable("autoominousbottle.hud.state.drinking");
			case EATING -> Component.translatable("autoominousbottle.hud.state.eating");
			case NO_BOTTLES -> Component.translatable("autoominousbottle.hud.state.no_bottles");
			case IDLE -> Component.translatable("autoominousbottle.hud.state.idle");
		};
	}
}
