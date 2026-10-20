package dev.rocketnk.autoominousbottle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Иконка состояния и пара строк рядом с ней в выбранном углу экрана.
 * Иконки — GUI-спрайты autoominousbottle:icon/*, ресурспак может заменить их картинками любого разрешения.
 */
public final class AobHud {
	public static final int ICON_SIZE = 16;
	private static final int MARGIN = 4;
	private static final int GAP = 4;
	private static final int LINE_HEIGHT = 10;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int STATE_COLOR = 0xFFC8C8D0;

	private AobHud() {
	}

	public static Identifier icon(String name) {
		return Identifier.fromNamespaceAndPath(AutoOminousBottleClient.MOD_ID, "icon/" + name);
	}

	/** Иконка для текущего состояния мода. */
	public static Identifier stateIcon() {
		if (!AutoOminousBottleClient.config.enabled) return icon("off");
		return icon(switch (AobController.state()) {
			case RAID -> "raid";
			case OMEN -> "omen";
			case WAIT -> "wait";
			case DRINKING -> "drinking";
			case EATING -> "eating";
			case NO_BOTTLES -> "no_bottles";
			case IDLE -> "on";
		});
	}

	public static Component stateText() {
		if (!AutoOminousBottleClient.config.enabled) return Component.translatable("autoominousbottle.hud.state.off");
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

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		AobConfig config = AutoOminousBottleClient.config;
		Minecraft mc = Minecraft.getInstance();
		if (!config.showHud || mc.player == null) return;
		if (!config.enabled && !config.hudWhenOff) return;

		List<Component> lines = new ArrayList<>();
		if (!config.hudCompact && config.enabled) {
			lines.add(Component.translatable("autoominousbottle.hud.counts",
					config.raidsWon, AobController.countBottles(mc.player)));
			lines.add(stateText());
		}

		int textWidth = 0;
		for (Component line : lines) textWidth = Math.max(textWidth, mc.font.width(line));
		int width = ICON_SIZE + (lines.isEmpty() ? 0 : GAP + textWidth);
		int height = Math.max(ICON_SIZE, lines.size() * LINE_HEIGHT);

		float scale = config.hudScale / 100.0F;
		AobConfig.HudCorner corner = config.hudCorner == null ? AobConfig.HudCorner.TOP_LEFT : config.hudCorner;
		boolean right = corner == AobConfig.HudCorner.TOP_RIGHT || corner == AobConfig.HudCorner.BOTTOM_RIGHT;
		boolean bottom = corner == AobConfig.HudCorner.BOTTOM_LEFT || corner == AobConfig.HudCorner.BOTTOM_RIGHT;
		float x = right ? graphics.guiWidth() - MARGIN - width * scale : MARGIN;
		float y = bottom ? graphics.guiHeight() - MARGIN - height * scale : MARGIN;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);

		// У правых углов иконка стоит справа от текста, чтобы прижиматься к краю.
		int iconX = right ? width - ICON_SIZE : 0;
		int textX = right ? 0 : ICON_SIZE + GAP;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, stateIcon(), iconX, (height - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
		int textY = (height - lines.size() * LINE_HEIGHT) / 2 + 1;
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			int lineX = right ? textX + textWidth - mc.font.width(line) : textX;
			graphics.text(mc.font, line, lineX, textY + i * LINE_HEIGHT, i == 0 ? TEXT_COLOR : STATE_COLOR, true);
		}

		graphics.pose().popMatrix();
	}
}
