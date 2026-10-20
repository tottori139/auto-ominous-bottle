package dev.rocketnk.autoominousbottle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Зловещая бутылочка, которая один раз смотрит по сторонам и улыбается, когда мод включился или начал пить.
 * Кадры — квадраты друг под другом в textures/gui/animation/ominous_bottle.png,
 * длительность — в ominous_bottle.animation.json. Ресурспак может заменить оба файла.
 */
public final class AobAnimation {
	private static final Identifier TEXTURE = id("textures/gui/animation/ominous_bottle.png");
	private static final Identifier TIMING = id("textures/gui/animation/ominous_bottle.animation.json");
	private static final int FADE_IN_TICKS = 3;
	private static final int FADE_OUT_TICKS = 8;
	/** Отступ над хотбаром и полосками здоровья/голода. */
	private static final int ABOVE_HOTBAR_OFFSET = 62;

	private record Frame(int index, int ticks) {
	}

	private static List<Frame> frames = List.of();
	private static int frameSize;
	private static int textureWidth;
	private static int textureHeight;
	private static int totalTicks;
	private static long startMillis = -1;

	private AobAnimation() {
	}

	public static void play() {
		if (AutoOminousBottleClient.config.animation) playPreview();
	}

	/** Проиграть даже если анимация выключена — для кнопки в настройках. */
	public static void playPreview() {
		if (load(Minecraft.getInstance().getResourceManager())) startMillis = Util.getMillis();
	}

	/** Читаем заново при каждом запуске: файлы маленькие, а ресурспак мог смениться. */
	private static boolean load(ResourceManager resources) {
		try (InputStream stream = resources.open(TEXTURE); NativeImage image = NativeImage.read(stream)) {
			textureWidth = image.getWidth();
			textureHeight = image.getHeight();
		} catch (Exception e) {
			AutoOminousBottleClient.LOGGER.warn("Не удалось прочитать {}", TEXTURE, e);
			return false;
		}
		frameSize = textureWidth;
		int frameCount = Math.max(1, textureHeight / frameSize);

		List<Frame> loaded = new ArrayList<>();
		try (Reader reader = resources.openAsReader(TIMING)) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			for (JsonElement element : json.getAsJsonArray("frames")) {
				JsonObject frame = element.getAsJsonObject();
				int index = frame.get("index").getAsInt();
				if (index >= 0 && index < frameCount) loaded.add(new Frame(index, Math.max(1, frame.get("time").getAsInt())));
			}
		} catch (Exception e) {
			// Нет файла с таймингом — показываем все кадры по полсекунды.
			loaded.clear();
			for (int i = 0; i < frameCount; i++) loaded.add(new Frame(i, 10));
		}
		if (loaded.isEmpty()) return false;

		frames = loaded;
		totalTicks = 0;
		for (Frame frame : frames) totalTicks += frame.ticks();
		return true;
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		AobConfig config = AutoOminousBottleClient.config;
		int size = config.animationSize;
		int x = graphics.guiWidth() / 2 - size / 2;
		int y = switch (config.animationPosition == null ? AobConfig.AnimationPosition.ABOVE_HOTBAR : config.animationPosition) {
			case ABOVE_HOTBAR -> graphics.guiHeight() - ABOVE_HOTBAR_OFFSET - size;
			case CENTER -> graphics.guiHeight() / 2 - size / 2;
			case TOP -> 12;
		};
		extractAt(graphics, x, y, size);
	}

	/** Рисует текущий кадр в заданном месте. false — анимация сейчас не идёт. */
	public static boolean extractAt(GuiGraphicsExtractor graphics, int x, int y, int size) {
		if (startMillis < 0) return false;
		double ticks = (Util.getMillis() - startMillis) / 50.0;
		if (ticks >= totalTicks) {
			startMillis = -1;
			return false;
		}

		Frame current = frames.getLast();
		double passed = 0;
		for (Frame frame : frames) {
			passed += frame.ticks();
			if (ticks < passed) {
				current = frame;
				break;
			}
		}

		double alpha = Math.min(1.0, Math.min(ticks / FADE_IN_TICKS, (totalTicks - ticks) / FADE_OUT_TICKS));
		int color = ((int) Math.round(alpha * 255) << 24) | 0xFFFFFF;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, current.index() * frameSize,
				size, size, frameSize, frameSize, textureWidth, textureHeight, color);
		return true;
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(AutoOminousBottleClient.MOD_ID, path);
	}
}
