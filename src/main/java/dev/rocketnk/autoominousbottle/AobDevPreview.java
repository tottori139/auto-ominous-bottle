package dev.rocketnk.autoominousbottle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Только для разработки (gradlew runPreview): открывает настройки, по очереди показывает каждую вкладку,
 * сохраняет скриншоты в run/screenshots и закрывает игру. В обычной игре не включается.
 */
final class AobDevPreview {
	static final boolean ENABLED = Boolean.getBoolean("autoominousbottle.devPreview");

	private static final int TICKS_PER_SHOT = 20;

	private static AobConfigScreen screen;
	private static int ticks;
	private static int tab;

	private AobDevPreview() {
	}

	static void tick(Minecraft mc) {
		if (screen == null) {
			if (mc.gui.overlay() == null && mc.gui.screen() instanceof TitleScreen title) {
				screen = new AobConfigScreen(title);
				screen.selectTab(0);
				mc.gui.setScreen(screen);
			}
			return;
		}
		if (++ticks < TICKS_PER_SHOT) return;
		ticks = 0;

		Screenshot.grab(mc.gameDirectory, "aob_tab_" + tab + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {
		});
		tab++;
		if (tab < AobConfigScreen.tabCount()) {
			screen.selectTab(tab);
		} else {
			mc.stop();
		}
	}
}
