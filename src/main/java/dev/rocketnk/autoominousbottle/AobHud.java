package dev.rocketnk.autoominousbottle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

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
	private static final long MESSAGE_MILLIS = 5000;
	private static final long MESSAGE_FADE_MILLIS = 1000;
	private static final int MAX_MESSAGES = 3;
	/** Длинные сообщения переносятся на эту ширину. */
	private static final int MESSAGE_WIDTH = 200;

	private record Message(Component text, long shownAt) {
	}

	private static final List<Message> MESSAGES = new ArrayList<>();

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

	/** Сообщение мода под иконкой вместо чата. Держится несколько секунд и гаснет. */
	public static void showMessage(Component text) {
		MESSAGES.add(new Message(text, Util.getMillis()));
		while (MESSAGES.size() > MAX_MESSAGES) MESSAGES.removeFirst();
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		AobConfig config = AutoOminousBottleClient.config;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		// Блок с иконкой — только когда он включён в настройках; сообщения показываем всегда.
		boolean showBlock = config.showHud && (config.enabled || config.hudWhenOff);
		List<Component> lines = new ArrayList<>();
		if (showBlock && !config.hudCompact && config.enabled) {
			lines.add(Component.translatable("autoominousbottle.hud.counts",
					config.raidsWon, AobController.countBottles(mc.player)));
			lines.add(stateText());
		}
		int textWidth = 0;
		for (Component line : lines) textWidth = Math.max(textWidth, mc.font.width(line));
		int blockWidth = showBlock ? ICON_SIZE + (lines.isEmpty() ? 0 : GAP + textWidth) : 0;
		int blockHeight = showBlock ? Math.max(ICON_SIZE, lines.size() * LINE_HEIGHT) : 0;

		long now = Util.getMillis();
		MESSAGES.removeIf(message -> now - message.shownAt() > MESSAGE_MILLIS);
		List<FormattedCharSequence> messageLines = new ArrayList<>();
		List<Integer> messageColors = new ArrayList<>();
		for (Message message : MESSAGES) {
			long left = MESSAGE_MILLIS - (now - message.shownAt());
			int alpha = (int) Math.min(255, 255L * left / MESSAGE_FADE_MILLIS);
			if (alpha < 8) continue;
			for (FormattedCharSequence line : mc.font.split(message.text(), MESSAGE_WIDTH)) {
				messageLines.add(line);
				messageColors.add((alpha << 24) | 0xFFFFFF);
			}
		}
		int messagesWidth = 0;
		for (FormattedCharSequence line : messageLines) messagesWidth = Math.max(messagesWidth, mc.font.width(line));
		int messagesHeight = messageLines.size() * LINE_HEIGHT;

		int gap = blockHeight > 0 && messagesHeight > 0 ? GAP : 0;
		int totalWidth = Math.max(blockWidth, messagesWidth);
		int totalHeight = blockHeight + gap + messagesHeight;
		if (totalHeight == 0) return;

		float scale = config.hudScale / 100.0F;
		AobConfig.HudCorner corner = config.hudCorner == null ? AobConfig.HudCorner.TOP_LEFT : config.hudCorner;
		boolean right = corner == AobConfig.HudCorner.TOP_RIGHT || corner == AobConfig.HudCorner.BOTTOM_RIGHT;
		boolean bottom = corner == AobConfig.HudCorner.BOTTOM_LEFT || corner == AobConfig.HudCorner.BOTTOM_RIGHT;
		float x = right ? graphics.guiWidth() - MARGIN - totalWidth * scale : MARGIN;
		float y = bottom ? graphics.guiHeight() - MARGIN - totalHeight * scale : MARGIN;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);

		// Сверху экрана сообщения идут под иконкой, снизу — над ней, чтобы не уезжать за край.
		int blockY = bottom ? messagesHeight + gap : 0;
		int messagesY = bottom ? 0 : blockHeight + gap;

		if (showBlock) {
			// У правых углов иконка стоит справа от текста, чтобы прижиматься к краю.
			int blockX = right ? totalWidth - blockWidth : 0;
			int iconX = blockX + (right ? blockWidth - ICON_SIZE : 0);
			int textX = blockX + (right ? 0 : ICON_SIZE + GAP);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, stateIcon(), iconX, blockY + (blockHeight - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE);
			int textY = blockY + (blockHeight - lines.size() * LINE_HEIGHT) / 2 + 1;
			for (int i = 0; i < lines.size(); i++) {
				Component line = lines.get(i);
				int lineX = right ? textX + textWidth - mc.font.width(line) : textX;
				graphics.text(mc.font, line, lineX, textY + i * LINE_HEIGHT, i == 0 ? TEXT_COLOR : STATE_COLOR, true);
			}
		}

		for (int i = 0; i < messageLines.size(); i++) {
			FormattedCharSequence line = messageLines.get(i);
			int lineX = right ? totalWidth - mc.font.width(line) : 0;
			graphics.text(mc.font, line, lineX, messagesY + i * LINE_HEIGHT, messageColors.get(i), true);
		}

		graphics.pose().popMatrix();
	}
}
