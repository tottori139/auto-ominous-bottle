package dev.rocketnk.autoominousbottle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Картинка из квадратных кадров друг под другом плюс необязательный &lt;имя&gt;.animation.json
 * с длительностью кадров: {"frames": [{"index": 0, "time": 10}, ...]}, time в тиках.
 * Без json каждый кадр идёт по полсекунды. Ресурспак может заменить и картинку, и json.
 */
public record AobFrames(Identifier texture, int frameSize, int textureWidth, int textureHeight, List<Frame> frames, int totalTicks) {
	public record Frame(int index, int ticks) {
	}

	private static final int DEFAULT_FRAME_TICKS = 10;

	/** null — картинки нет или её не удалось прочитать. */
	public static AobFrames load(ResourceManager resources, Identifier texture) {
		int width;
		int height;
		try (InputStream stream = resources.open(texture); NativeImage image = NativeImage.read(stream)) {
			width = image.getWidth();
			height = image.getHeight();
		} catch (Exception e) {
			AutoOminousBottleClient.LOGGER.warn("Не удалось прочитать {}", texture, e);
			return null;
		}
		int frameCount = Math.max(1, height / width);

		List<Frame> frames = new ArrayList<>();
		Identifier timing = texture.withPath(path -> path.replaceFirst("\\.png$", ".animation.json"));
		if (resources.getResource(timing).isPresent()) {
			try (Reader reader = resources.openAsReader(timing)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				for (JsonElement element : json.getAsJsonArray("frames")) {
					JsonObject frame = element.getAsJsonObject();
					int index = frame.get("index").getAsInt();
					if (index >= 0 && index < frameCount) frames.add(new Frame(index, Math.max(1, frame.get("time").getAsInt())));
				}
			} catch (Exception e) {
				AutoOminousBottleClient.LOGGER.warn("Не удалось прочитать {}", timing, e);
				frames.clear();
			}
		}
		if (frames.isEmpty()) {
			for (int i = 0; i < frameCount; i++) frames.add(new Frame(i, DEFAULT_FRAME_TICKS));
		}

		int total = 0;
		for (Frame frame : frames) total += frame.ticks();
		return new AobFrames(texture, width, width, height, List.copyOf(frames), total);
	}

	public boolean animated() {
		return frames.size() > 1;
	}

	/** Кадр, который идёт через ticks тиков от начала. */
	public Frame frameAt(double ticks) {
		double passed = 0;
		for (Frame frame : frames) {
			passed += frame.ticks();
			if (ticks < passed) return frame;
		}
		return frames.getLast();
	}

	public void draw(GuiGraphicsExtractor graphics, Frame frame, int x, int y, int size, int color) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, frame.index() * frameSize,
				size, size, frameSize, frameSize, textureWidth, textureHeight, color);
	}
}
