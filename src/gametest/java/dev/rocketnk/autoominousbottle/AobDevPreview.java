package dev.rocketnk.autoominousbottle;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Только для разработки (gradlew runPreview): открывает настройки, по очереди показывает каждую вкладку,
 * сохраняет скриншоты в run/screenshots и закрывает игру. На вкладке анимации снимает ещё и улыбку.
 * Лежит в тестовом наборе, в выпущенный jar не попадает; включается только флагом из run-конфигурации.
 */
public final class AobDevPreview implements ClientModInitializer {
	static final boolean ENABLED = Boolean.getBoolean("autoominousbottle.devPreview");

	private static final int TICKS_PER_SHOT = 20;
	/** Через сколько тиков после старта анимации бутылочка уже улыбается. */
	private static final int SMILE_TICKS = 70;

	private static AobConfigScreen screen;
	private static int ticks;
	private static int tab;
	private static boolean smileShot;

	@Override
	public void onInitializeClient() {
		if (ENABLED) ClientTickEvents.END_CLIENT_TICK.register(AobDevPreview::tick);
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
		ticks++;
		boolean animationTab = tab == AobConfigScreen.animationTabIndex();
		if (animationTab && ticks == 1) AobAnimation.playPreview();
		if (animationTab && !smileShot && ticks == TICKS_PER_SHOT) {
			shot(mc, "aob_tab_" + tab);
			smileShot = true;
			return;
		}
		if (ticks < (animationTab ? SMILE_TICKS : TICKS_PER_SHOT)) return;

		shot(mc, animationTab ? "aob_tab_" + tab + "_smile" : "aob_tab_" + tab);
		ticks = 0;
		tab++;
		if (tab < AobConfigScreen.tabCount()) {
			screen.selectTab(tab);
		} else {
			mc.stop();
		}
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {
		});
	}
}
