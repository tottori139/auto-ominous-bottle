package dev.rocketnk.autoominousbottle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Иконки мода: textures/gui/icon/&lt;имя&gt;.png, кадры друг под другом, тайминг в &lt;имя&gt;.animation.json.
 * Кадрами управляет мод, а не атлас игры, поэтому анимацию можно выключить настройкой — тогда стоит первый кадр.
 */
public final class AobIcons {
	private static final Map<String, Optional<AobFrames>> CACHE = new HashMap<>();

	private AobIcons() {
	}

	/** Сбрасываем при перезагрузке ресурсов: ресурспак мог заменить иконки. */
	public static void clearCache() {
		CACHE.clear();
	}

	public static void draw(GuiGraphicsExtractor graphics, String name, int x, int y, int size) {
		Optional<AobFrames> loaded = CACHE.computeIfAbsent(name, key -> Optional.ofNullable(
				AobFrames.load(Minecraft.getInstance().getResourceManager(), texture(key))));
		if (loaded.isEmpty()) return;
		AobFrames icon = loaded.get();
		AobFrames.Frame frame = icon.animated() && AutoOminousBottleClient.config.animationsActive()
				? icon.frameAt((Util.getMillis() / 50.0) % icon.totalTicks())
				: icon.frames().getFirst();
		icon.draw(graphics, frame, x, y, size, 0xFFFFFFFF);
	}

	private static Identifier texture(String name) {
		return Identifier.fromNamespaceAndPath(AutoOminousBottleClient.MOD_ID, "textures/gui/icon/" + name + ".png");
	}
}
