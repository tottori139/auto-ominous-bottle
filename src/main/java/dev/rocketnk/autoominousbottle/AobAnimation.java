package dev.rocketnk.autoominousbottle;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Зловещая бутылочка, которая один раз смотрит по сторонам и улыбается, когда мод включился или начал пить.
 * Кадры — textures/gui/animation/ominous_bottle.png, тайминг — ominous_bottle.animation.json (см. {@link AobFrames}).
 */
public final class AobAnimation {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
			AutoOminousBottleClient.MOD_ID, "textures/gui/animation/ominous_bottle.png");
	private static final int FADE_IN_TICKS = 3;
	private static final int FADE_OUT_TICKS = 8;
	/** Отступ над хотбаром и полосками здоровья/голода. */
	private static final int ABOVE_HOTBAR_OFFSET = 62;

	private static AobFrames frames;
	private static long startMillis = -1;

	private AobAnimation() {
	}

	public static void play() {
		AobConfig config = AutoOminousBottleClient.config;
		if (config.animation && config.animationsActive()) playPreview();
	}

	/** Проиграть даже если анимация выключена — для кнопки в настройках. Файлы читаем заново: ресурспак мог смениться. */
	public static void playPreview() {
		frames = AobFrames.load(Minecraft.getInstance().getResourceManager(), TEXTURE);
		if (frames != null) startMillis = Util.getMillis();
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		AobConfig config = AutoOminousBottleClient.config;
		int size = config.animationSize;
		int x = graphics.guiWidth() / 2 - size / 2;
		int y = switch (config.animationPosition == null ? AobConfig.AnimationPosition.ABOVE_HOTBAR : config.animationPosition) {
			case ABOVE_HOTBAR -> {
				int top = graphics.guiHeight() - ABOVE_HOTBAR_OFFSET - size;
				int hudTop = AobHud.aboveHotbarTop();
				yield hudTop >= 0 ? Math.min(top, hudTop - size - 2) : top;
			}
			case CENTER -> graphics.guiHeight() / 2 - size / 2;
			case TOP -> 12;
		};
		extractAt(graphics, x, y, size);
	}

	/** Рисует текущий кадр в заданном месте. false — анимация сейчас не идёт. */
	public static boolean extractAt(GuiGraphicsExtractor graphics, int x, int y, int size) {
		if (startMillis < 0 || frames == null) return false;
		double ticks = (Util.getMillis() - startMillis) / 50.0;
		int total = frames.totalTicks();
		if (ticks >= total) {
			startMillis = -1;
			return false;
		}
		double alpha = Math.min(1.0, Math.min(ticks / FADE_IN_TICKS, (total - ticks) / FADE_OUT_TICKS));
		int color = ((int) Math.round(alpha * 255) << 24) | 0xFFFFFF;
		frames.draw(graphics, frames.frameAt(ticks), x, y, size, color);
		return true;
	}
}
