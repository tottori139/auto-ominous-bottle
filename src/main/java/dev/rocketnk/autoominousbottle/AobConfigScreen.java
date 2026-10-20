package dev.rocketnk.autoominousbottle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Экран настроек, который открывается из Mod Menu. Кнопки в две колонки, как в ванильных настройках. */
public class AobConfigScreen extends Screen {
	private static final List<Integer> DRINK_DELAYS = List.of(3, 5, 10, 20, 30);
	private static final List<Integer> EAT_THRESHOLDS = List.of(6, 10, 14, 18);
	private static final List<Integer> JUMP_INTERVALS = List.of(0, 1, 3, 5);
	private static final List<Integer> LOW_HEALTH_VALUES = List.of(4, 6, 8, 10, 14);
	private static final List<Integer> VOLUMES = List.of(25, 50, 75, 100);
	private static final int BUTTON_WIDTH = 150;
	private static final int BUTTON_HEIGHT = 20;
	private static final int COLUMN_GAP = 10;
	private static final int ROW = 24;
	private static final int TOP = 36;

	private final Screen parent;
	private int slot;

	public AobConfigScreen(Screen parent) {
		super(Component.translatable("autoominousbottle.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		AobConfig config = AutoOminousBottleClient.config;
		if (!DRINK_DELAYS.contains(config.drinkDelaySeconds)) config.drinkDelaySeconds = 5;
		if (!EAT_THRESHOLDS.contains(config.eatBelowFood)) config.eatBelowFood = 14;
		if (!JUMP_INTERVALS.contains(config.jumpIntervalSeconds)) config.jumpIntervalSeconds = 1;
		if (!LOW_HEALTH_VALUES.contains(config.lowHealth)) config.lowHealth = 6;
		if (!VOLUMES.contains(config.soundVolume)) config.soundVolume = 100;
		slot = 0;

		// Слева: что делает мод. Справа: его параметры.
		place(CycleButton.onOffBuilder(config.enabled)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.enabled"), (button, value) -> config.enabled = value));
		place(CycleButton.onOffBuilder(config.showHud)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.hud"), (button, value) -> config.showHud = value));

		place(CycleButton.onOffBuilder(config.confirmEnable)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.confirm_enable.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.confirm_enable"), (button, value) -> config.confirmEnable = value));
		place(CycleButton.onOffBuilder(config.autoDisable)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.auto_disable.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.auto_disable"), (button, value) -> config.autoDisable = value));

		place(CycleButton.onOffBuilder(config.autoDrink)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.auto_drink"), (button, value) -> config.autoDrink = value));
		place(CycleButton.<Integer>builder(seconds -> Component.translatable("autoominousbottle.config.seconds", seconds), config.drinkDelaySeconds)
				.withValues(DRINK_DELAYS)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.drink_delay.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.drink_delay"), (button, value) -> config.drinkDelaySeconds = value));

		place(CycleButton.onOffBuilder(config.autoEat)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.auto_eat"), (button, value) -> config.autoEat = value));
		place(CycleButton.<Integer>builder(food -> Component.literal(String.valueOf(food)), config.eatBelowFood)
				.withValues(EAT_THRESHOLDS)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.eat_below.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.eat_below"), (button, value) -> config.eatBelowFood = value));

		place(CycleButton.onOffBuilder(config.autoJump)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.auto_jump.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.auto_jump"), (button, value) -> config.autoJump = value));
		place(CycleButton.<Integer>builder(AobConfigScreen::jumpIntervalText, config.jumpIntervalSeconds)
				.withValues(JUMP_INTERVALS)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.jump_interval"), (button, value) -> config.jumpIntervalSeconds = value));

		place(CycleButton.onOffBuilder(config.lowHealthDisable)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.low_health_disable.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.low_health_disable"), (button, value) -> config.lowHealthDisable = value));
		place(CycleButton.<Integer>builder(hp -> Component.translatable("autoominousbottle.config.hearts", hp / 2), config.lowHealth)
				.withValues(LOW_HEALTH_VALUES)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.low_health"), (button, value) -> config.lowHealth = value));

		place(CycleButton.onOffBuilder(config.sounds)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.sounds"), (button, value) -> config.sounds = value));
		place(CycleButton.<Integer>builder(volume -> Component.literal(volume + "%"), config.soundVolume)
				.withValues(VOLUMES)
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.volume"), (button, value) -> {
					config.soundVolume = value;
					AobSounds.play(AobSounds.CONFIRM);
				}));

		place(CycleButton.onOffBuilder(config.exitWhenOutOfBottles)
				.withTooltip(value -> Tooltip.create(Component.translatable("autoominousbottle.config.exit_no_bottles.tip")))
				.create(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("autoominousbottle.config.exit_no_bottles"), (button, value) -> config.exitWhenOutOfBottles = value));

		place(Button.builder(Component.translatable("autoominousbottle.config.reset", config.raidsWon), button -> {
			config.raidsWon = 0;
			button.setMessage(Component.translatable("autoominousbottle.config.reset", 0));
		}).size(BUTTON_WIDTH * 2 + COLUMN_GAP, BUTTON_HEIGHT).build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 - 100, height - 28, 200, BUTTON_HEIGHT).build());
	}

	/** Ставит виджет в следующую ячейку сетки; широкий виджет занимает целую строку. */
	private void place(AbstractWidget widget) {
		boolean wide = widget.getWidth() > BUTTON_WIDTH;
		if (wide && slot % 2 == 1) slot++;
		int left = width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
		int column = wide ? 0 : slot % 2;
		widget.setPosition(left + column * (BUTTON_WIDTH + COLUMN_GAP), TOP + slot / 2 * ROW);
		slot += wide ? 2 : 1;
		addRenderableWidget(widget);
	}

	private static Component jumpIntervalText(int seconds) {
		return seconds == 0
				? Component.translatable("autoominousbottle.config.jump_interval.hold")
				: Component.translatable("autoominousbottle.config.jump_interval.every", seconds);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 16, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		AutoOminousBottleClient.config.save();
		AobController.reset();
		minecraft.gui.setScreen(parent);
	}
}
