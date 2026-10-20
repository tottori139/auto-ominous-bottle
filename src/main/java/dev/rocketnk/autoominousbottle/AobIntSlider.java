package dev.rocketnk.autoominousbottle;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Ползунок для целого числа с шагом: «Подпись: значение». */
public class AobIntSlider extends AbstractSliderButton {
	private final Component label;
	private final int min;
	private final int max;
	private final int step;
	private final IntFunction<Component> format;
	private final IntConsumer onChange;

	public AobIntSlider(int x, int y, int width, int height, Component label, int min, int max, int step, int current,
			IntFunction<Component> format, IntConsumer onChange) {
		super(x, y, width, height, Component.empty(), toSlider(Math.clamp(current, min, max), min, max));
		this.label = label;
		this.min = min;
		this.max = max;
		this.step = step;
		this.format = format;
		this.onChange = onChange;
		updateMessage();
	}

	private static double toSlider(int value, int min, int max) {
		return (double) (value - min) / (max - min);
	}

	public int current() {
		int steps = (int) Math.round(value * (max - min) / step);
		return Math.min(max, min + steps * step);
	}

	@Override
	protected void updateMessage() {
		setMessage(label.copy().append(": ").append(format.apply(current())));
	}

	@Override
	protected void applyValue() {
		onChange.accept(current());
	}
}
