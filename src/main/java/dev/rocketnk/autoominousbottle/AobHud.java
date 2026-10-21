package dev.rocketnk.autoominousbottle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Статус мода на экране и его сообщения. Три стиля: иконка с текстом, только иконка,
 * или одна сжатая строка над хотбаром. Иконка стоит в одном из 8 мест: 4 угла и 4 середины краёв.
 */
public final class AobHud {
	public static final int ICON_SIZE = 16;
	private static final int MARGIN = 4;
	private static final int GAP = 4;
	private static final int LINE_HEIGHT = 10;
	/** Строка в стиле «только текст» и низ-по-центру стоят выше хотбара и полосок здоровья/голода. */
	private static final int ABOVE_HOTBAR = 62;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int STATE_COLOR = 0xFFC8C8D0;
	private static final long MESSAGE_MILLIS = 5000;
	private static final long MESSAGE_FADE_MILLIS = 1000;
	private static final int MAX_MESSAGES = 3;
	/** Длинные сообщения переносятся на эту ширину. */
	private static final int MESSAGE_WIDTH = 200;

	private enum Align { START, CENTER, END }

	private record Message(Component text, long shownAt) {
	}

	private static final List<Message> MESSAGES = new ArrayList<>();

	/** Верх HUD, когда он стоит над хотбаром, иначе -1. Бутылочка встаёт выше, чтобы его не закрывать. */
	private static int aboveHotbarTop = -1;

	private AobHud() {
	}

	public static int aboveHotbarTop() {
		return aboveHotbarTop;
	}

	/** Имя иконки для текущего состояния мода. */
	public static String stateIcon() {
		if (!AutoOminousBottleClient.config.enabled) return "off";
		return switch (AobController.state()) {
			case RAID -> "raid";
			case OMEN -> "omen";
			case WAIT -> "wait";
			case DRINKING -> "drinking";
			case EATING -> "eating";
			case NO_BOTTLES -> "no_bottles";
			case IDLE -> "on";
		};
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

	public static void clearMessages() {
		MESSAGES.clear();
	}

	/** Сообщение мода рядом со статусом вместо чата. Держится несколько секунд и гаснет. */
	public static void showMessage(Component text) {
		MESSAGES.add(new Message(text, Util.getMillis()));
		while (MESSAGES.size() > MAX_MESSAGES) MESSAGES.removeFirst();
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		AobConfig config = AutoOminousBottleClient.config;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

		AobConfig.HudStyle style = config.hudStyle == null ? AobConfig.HudStyle.FULL : config.hudStyle;
		boolean showStatus = config.showHud && (config.enabled || config.hudWhenOff);
		boolean showIcon = showStatus && style != AobConfig.HudStyle.TEXT;

		// Текст статуса: две строки рядом с иконкой или одна сжатая строка без неё.
		List<Component> lines = new ArrayList<>();
		if (showStatus && style != AobConfig.HudStyle.ICON) {
			if (!config.enabled) {
				lines.add(stateText());
			} else if (style == AobConfig.HudStyle.TEXT) {
				lines.add(Component.translatable("autoominousbottle.hud.compact",
						stateText(), config.raidsWon, AobController.countBottles(mc.player)));
			} else {
				lines.add(Component.translatable("autoominousbottle.hud.counts", config.raidsWon, AobController.countBottles(mc.player)));
				lines.add(stateText());
			}
		}
		int textWidth = 0;
		for (Component line : lines) textWidth = Math.max(textWidth, mc.font.width(line));
		int blockWidth = (showIcon ? ICON_SIZE : 0) + (showIcon && !lines.isEmpty() ? GAP : 0) + textWidth;
		int blockHeight = Math.max(showIcon ? ICON_SIZE : 0, lines.size() * LINE_HEIGHT);

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
		aboveHotbarTop = -1;
		if (totalHeight == 0) return;

		// Положение: в стиле «только текст» — всегда над хотбаром по центру.
		AobConfig.HudPosition position = style == AobConfig.HudStyle.TEXT ? AobConfig.HudPosition.BOTTOM_CENTER
				: config.hudPosition == null ? AobConfig.HudPosition.TOP_LEFT : config.hudPosition;
		Align horizontal = switch (position) {
			case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> Align.START;
			case TOP_CENTER, BOTTOM_CENTER -> Align.CENTER;
			case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> Align.END;
		};
		Align vertical = switch (position) {
			case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> Align.START;
			case MIDDLE_LEFT, MIDDLE_RIGHT -> Align.CENTER;
			case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Align.END;
		};
		int bottomOffset = position == AobConfig.HudPosition.BOTTOM_CENTER ? ABOVE_HOTBAR : MARGIN;

		float scale = config.hudScale / 100.0F;
		float x = switch (horizontal) {
			case START -> MARGIN;
			case CENTER -> (graphics.guiWidth() - totalWidth * scale) / 2;
			case END -> graphics.guiWidth() - MARGIN - totalWidth * scale;
		};
		float y = switch (vertical) {
			case START -> MARGIN;
			case CENTER -> (graphics.guiHeight() - totalHeight * scale) / 2;
			case END -> graphics.guiHeight() - bottomOffset - totalHeight * scale;
		};
		if (position == AobConfig.HudPosition.BOTTOM_CENTER) aboveHotbarTop = (int) y;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);

		// Внизу экрана сообщения идут над статусом, иначе — под ним, чтобы не уезжать за край.
		boolean messagesAbove = vertical == Align.END;
		int blockY = messagesAbove ? messagesHeight + gap : 0;
		int messagesY = messagesAbove ? 0 : blockHeight + gap;

		int blockX = alignX(horizontal, totalWidth, blockWidth);
		// У правого края иконка стоит справа от текста, чтобы прижиматься к краю.
		boolean iconRight = horizontal == Align.END;
		if (showIcon) {
			int iconX = blockX + (iconRight ? blockWidth - ICON_SIZE : 0);
			AobIcons.draw(graphics, stateIcon(), iconX, blockY + (blockHeight - ICON_SIZE) / 2, ICON_SIZE);
		}
		int textX = blockX + (showIcon && !iconRight ? ICON_SIZE + GAP : 0);
		int textY = blockY + (blockHeight - lines.size() * LINE_HEIGHT) / 2 + 1;
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			int lineX = textX + alignX(horizontal, textWidth, mc.font.width(line));
			graphics.text(mc.font, line, lineX, textY + i * LINE_HEIGHT, i == 0 ? TEXT_COLOR : STATE_COLOR, true);
		}

		for (int i = 0; i < messageLines.size(); i++) {
			FormattedCharSequence line = messageLines.get(i);
			graphics.text(mc.font, line, alignX(horizontal, totalWidth, mc.font.width(line)),
					messagesY + i * LINE_HEIGHT, messageColors.get(i), true);
		}

		graphics.pose().popMatrix();
	}

	private static int alignX(Align align, int containerWidth, int width) {
		return switch (align) {
			case START -> 0;
			case CENTER -> (containerWidth - width) / 2;
			case END -> containerWidth - width;
		};
	}
}
