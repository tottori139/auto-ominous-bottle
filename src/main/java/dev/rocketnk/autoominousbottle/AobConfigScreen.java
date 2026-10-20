package dev.rocketnk.autoominousbottle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * Экран настроек из Mod Menu: сверху панель статуса с главным выключателем, под ней вкладки.
 * Переключатели показывают иконку (галочка/крестик) вместо «ВКЛ/ВЫКЛ».
 */
public class AobConfigScreen extends Screen {
	private enum Tab { GENERAL, DRINK, FOOD, JUMP, HUD, SOUNDS }

	private static final int CONTENT_WIDTH = 300;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ROW = 22;
	private static final int TAB_GAP = 2;
	private static final int PANEL_TOP = 18;
	private static final int PANEL_HEIGHT = 30;
	private static final int PANEL_ICON = 24;
	private static final int TABS_TOP = PANEL_TOP + PANEL_HEIGHT + 6;
	private static final int OPTIONS_TOP = TABS_TOP + BUTTON_HEIGHT + 6;
	private static final int TOGGLE_ICON = 16;
	private static final int MAIN_BUTTON_WIDTH = 90;
	private static final int[] JUMP_INTERVALS = {0, 1, 2, 3, 5};

	/** Последняя открытая вкладка, чтобы после «Назначить клавишу» вернуться туда же. */
	private static Tab tab = Tab.GENERAL;

	private record Toggle(Button button, BooleanSupplier state) {
	}

	private final Screen parent;
	private final List<Toggle> toggles = new ArrayList<>();
	private int left;
	private int row;
	private boolean resetArmed;

	public AobConfigScreen(Screen parent) {
		super(Component.translatable("autoominousbottle.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		AobConfig config = AutoOminousBottleClient.config;
		toggles.clear();
		left = width / 2 - CONTENT_WIDTH / 2;
		row = 0;
		resetArmed = false;

		Button main = Button.builder(Component.translatable("autoominousbottle.config.enabled"),
						button -> AutoOminousBottleClient.setEnabled(!config.enabled))
				.bounds(left + CONTENT_WIDTH - MAIN_BUTTON_WIDTH - 5, PANEL_TOP + (PANEL_HEIGHT - BUTTON_HEIGHT) / 2, MAIN_BUTTON_WIDTH, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("autoominousbottle.config.enabled.tip")))
				.build();
		toggles.add(new Toggle(addRenderableWidget(main), () -> config.enabled));

		// Ширина вкладки — по длине названия, остаток делим поровну: «Основное» длиннее «Еды».
		Tab[] tabs = Tab.values();
		Component[] labels = new Component[tabs.length];
		int textTotal = 0;
		for (int i = 0; i < tabs.length; i++) {
			labels[i] = Component.translatable("autoominousbottle.tab." + tabs[i].name().toLowerCase());
			textTotal += font.width(labels[i]);
		}
		int spare = Math.max(0, CONTENT_WIDTH - (tabs.length - 1) * TAB_GAP - textTotal);
		int x = left;
		for (int i = 0; i < tabs.length; i++) {
			Tab target = tabs[i];
			int tabWidth = i == tabs.length - 1
					? left + CONTENT_WIDTH - x
					: font.width(labels[i]) + spare / tabs.length;
			Button button = Button.builder(labels[i], b -> {
				tab = target;
				rebuildWidgets();
			}).bounds(x, TABS_TOP, tabWidth, BUTTON_HEIGHT).build();
			button.active = target != tab;
			addRenderableWidget(button);
			x += tabWidth + TAB_GAP;
		}

		switch (tab) {
			case GENERAL -> initGeneral(config);
			case DRINK -> initDrink(config);
			case FOOD -> initFood(config);
			case JUMP -> initJump(config);
			case HUD -> initHud(config);
			case SOUNDS -> initSounds(config);
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 - 100, height - 26, 200, BUTTON_HEIGHT).build());
	}

	private void initGeneral(AobConfig config) {
		toggle("confirm_enable", () -> config.confirmEnable, value -> config.confirmEnable = value);
		toggle("auto_disable", () -> config.autoDisable, value -> config.autoDisable = value);
		toggle("low_health_disable", () -> config.lowHealthDisable, value -> config.lowHealthDisable = value);
		slider("low_health", 2, 20, 2, config.lowHealth,
				hp -> Component.translatable("autoominousbottle.config.hearts", hp / 2), value -> config.lowHealth = value);
		toggle("exit_no_bottles", () -> config.exitWhenOutOfBottles, value -> config.exitWhenOutOfBottles = value);

		int half = (CONTENT_WIDTH - TAB_GAP) / 2;
		int y = nextRowY();
		addRenderableWidget(Button.builder(
						Component.translatable("autoominousbottle.config.keybind", AutoOminousBottleClient.toggleKey().getTranslatedKeyMessage()),
						button -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)))
				.bounds(left, y, half, BUTTON_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("autoominousbottle.config.keybind.tip")))
				.build());
		addRenderableWidget(Button.builder(resetLabel(config), button -> {
			// Сброс счётчика — вторым нажатием, первое только спрашивает.
			if (resetArmed) {
				config.raidsWon = 0;
				config.save();
				resetArmed = false;
			} else {
				resetArmed = true;
			}
			button.setMessage(resetLabel(config));
		}).bounds(left + half + TAB_GAP, y, half, BUTTON_HEIGHT).build());
	}

	static int tabCount() {
		return Tab.values().length;
	}

	void selectTab(int index) {
		tab = Tab.values()[index];
		// До первого init() у экрана нулевой размер: кнопки встали бы за край и рендер упал бы.
		if (width > 0) rebuildWidgets();
	}

	private Component resetLabel(AobConfig config) {
		return resetArmed
				? Component.translatable("autoominousbottle.config.reset.confirm")
				: Component.translatable("autoominousbottle.config.reset", config.raidsWon);
	}

	private void initDrink(AobConfig config) {
		toggle("auto_drink", () -> config.autoDrink, value -> config.autoDrink = value);
		slider("drink_delay", 3, 60, 1, config.drinkDelaySeconds,
				seconds -> Component.translatable("autoominousbottle.config.seconds", seconds), value -> config.drinkDelaySeconds = value);
	}

	private void initFood(AobConfig config) {
		toggle("auto_eat", () -> config.autoEat, value -> config.autoEat = value);
		slider("eat_below", 1, 19, 1, config.eatBelowFood,
				food -> Component.literal(String.valueOf(food)), value -> config.eatBelowFood = value);
	}

	private void initJump(AobConfig config) {
		toggle("auto_jump", () -> config.autoJump, value -> config.autoJump = value);
		List<Integer> intervals = new ArrayList<>();
		for (int interval : JUMP_INTERVALS) intervals.add(interval);
		if (!intervals.contains(config.jumpIntervalSeconds)) config.jumpIntervalSeconds = 1;
		add(CycleButton.<Integer>builder(AobConfigScreen::jumpIntervalText, config.jumpIntervalSeconds)
				.withValues(intervals)
				.create(left, nextRowY(), CONTENT_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.jump_interval"),
						(button, value) -> config.jumpIntervalSeconds = value));
	}

	private void initHud(AobConfig config) {
		toggle("hud", () -> config.showHud, value -> config.showHud = value);
		if (config.hudCorner == null) config.hudCorner = AobConfig.HudCorner.TOP_LEFT;
		add(CycleButton.<AobConfig.HudCorner>builder(
						corner -> Component.translatable("autoominousbottle.config.corner." + corner.name().toLowerCase()), config.hudCorner)
				.withValues(AobConfig.HudCorner.values())
				.create(left, nextRowY(), CONTENT_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.hud_corner"),
						(button, value) -> config.hudCorner = value));
		slider("hud_scale", 50, 200, 25, config.hudScale,
				scale -> Component.literal(scale + "%"), value -> config.hudScale = value);
		toggle("hud_compact", () -> config.hudCompact, value -> config.hudCompact = value);
		toggle("hud_when_off", () -> config.hudWhenOff, value -> config.hudWhenOff = value);
	}

	private void initSounds(AobConfig config) {
		toggle("sounds", () -> config.sounds, value -> config.sounds = value);
		AobIntSlider volume = slider("volume", 5, 100, 5, config.soundVolume,
				percent -> Component.literal(percent + "%"), value -> config.soundVolume = value);
		volume.setTooltip(Tooltip.create(Component.translatable("autoominousbottle.config.volume.tip")));
		int half = (CONTENT_WIDTH - TAB_GAP) / 2;
		int y = nextRowY();
		add(Button.builder(Component.translatable("autoominousbottle.config.test_sound.raid_won"), button -> AobSounds.play(AobSounds.RAID_WON))
				.bounds(left, y, half, BUTTON_HEIGHT).build());
		add(Button.builder(Component.translatable("autoominousbottle.config.test_sound.warning"), button -> AobSounds.play(AobSounds.WARNING))
				.bounds(left + half + TAB_GAP, y, half, BUTTON_HEIGHT).build());
	}

	/** Переключатель на всю ширину: подпись по центру, справа иконка состояния. */
	private void toggle(String key, BooleanSupplier get, Consumer<Boolean> set) {
		Button button = Button.builder(Component.translatable("autoominousbottle.config." + key), b -> set.accept(!get.getAsBoolean()))
				.bounds(left, nextRowY(), CONTENT_WIDTH, BUTTON_HEIGHT)
				.build();
		String tip = "autoominousbottle.config." + key + ".tip";
		if (Language.getInstance().has(tip)) button.setTooltip(Tooltip.create(Component.translatable(tip)));
		toggles.add(new Toggle(add(button), get));
	}

	private AobIntSlider slider(String key, int min, int max, int step, int current, IntFunction<Component> format, IntConsumer onChange) {
		AobIntSlider slider = new AobIntSlider(left, nextRowY(), CONTENT_WIDTH, BUTTON_HEIGHT,
				Component.translatable("autoominousbottle.config." + key), min, max, step, current, format, onChange);
		String tip = "autoominousbottle.config." + key + ".tip";
		if (Language.getInstance().has(tip)) slider.setTooltip(Tooltip.create(Component.translatable(tip)));
		return add(slider);
	}

	private <T extends AbstractWidget> T add(T widget) {
		return addRenderableWidget(widget);
	}

	private int nextRowY() {
		return OPTIONS_TOP + row++ * ROW;
	}

	private static Component jumpIntervalText(int seconds) {
		return seconds == 0
				? Component.translatable("autoominousbottle.config.jump_interval.hold")
				: Component.translatable("autoominousbottle.config.jump_interval.every", seconds);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 6, 0xFFFFFFFF);
		extractStatusPanel(graphics);
		for (Toggle toggle : toggles) {
			Button button = toggle.button();
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AobHud.icon(toggle.state().getAsBoolean() ? "toggle_on" : "toggle_off"),
					button.getRight() - TOGGLE_ICON - 2, button.getY() + (BUTTON_HEIGHT - TOGGLE_ICON) / 2, TOGGLE_ICON, TOGGLE_ICON);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		// Подложка панели статуса — в фоне, чтобы не затемнять кнопку «Мод» поверх.
		graphics.fill(left, PANEL_TOP, left + CONTENT_WIDTH, PANEL_TOP + PANEL_HEIGHT, 0x90000000);
	}

	/** Панель статуса: большая иконка состояния, что мод делает сейчас и счётчики. */
	private void extractStatusPanel(GuiGraphicsExtractor graphics) {
		AobConfig config = AutoOminousBottleClient.config;
		int iconY = PANEL_TOP + (PANEL_HEIGHT - PANEL_ICON) / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AobHud.stateIcon(), left + 4, iconY, PANEL_ICON, PANEL_ICON);

		Component counts = minecraft.player != null
				? Component.translatable("autoominousbottle.hud.counts", config.raidsWon, AobController.countBottles(minecraft.player))
				: Component.translatable("autoominousbottle.hud.raids_only", config.raidsWon);
		int textX = left + 4 + PANEL_ICON + 6;
		graphics.text(font, AobHud.stateText(), textX, PANEL_TOP + 5, 0xFFFFFFFF, true);
		graphics.text(font, counts, textX, PANEL_TOP + 17, 0xFFC8C8D0, true);
	}

	@Override
	public void onClose() {
		AutoOminousBottleClient.config.save();
		minecraft.gui.setScreen(parent);
	}
}
