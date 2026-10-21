package dev.rocketnk.autoominousbottle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AobConfig {
	/** Где стоит иконка статуса: 4 угла и 4 середины краёв экрана. */
	public enum HudPosition {
		TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
	}

	/** FULL — иконка и текст, ICON — только иконка, TEXT — одна строка над хотбаром без иконки. */
	public enum HudStyle { FULL, ICON, TEXT }

	/** AUTO — анимации выключены, когда в настройках графики стоит «Быстрая». */
	public enum Animations { ON, OFF, AUTO }

	public enum AnimationPosition { ABOVE_HOTBAR, CENTER, TOP }

	/** Что делать, когда сработало условие остановки. */
	public enum StopAction { DISABLE, EXIT_WORLD }

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("autoominousbottle.json");

	public boolean enabled = false;
	/** Включать клавишей только по второму нажатию. */
	public boolean confirmEnable = true;
	/** Выключать мод после перезахода, смены измерения и смерти. */
	public boolean autoDisable = true;
	/** Выключать мод, когда здоровье упало до lowHealth (в единицах HP, 2 HP = 1 сердце). */
	public boolean lowHealthDisable = true;
	public int lowHealth = 6;
	public boolean exitWhenOutOfBottles = false;
	/** Остановиться после стольких рейдов с момента включения, 0 — не останавливаться. */
	public int stopAfterRaids = 0;
	/** Остановиться через столько минут после включения, 0 — не останавливаться. */
	public int stopAfterMinutes = 0;
	public StopAction stopAction = StopAction.DISABLE;
	/** Бутылочка с глазами при включении мода. */
	public boolean animation = true;
	/** И ещё каждый раз, когда мод начинает пить бутылку. */
	public boolean animationOnDrink = true;
	public AnimationPosition animationPosition = AnimationPosition.ABOVE_HOTBAR;
	/** Размер на экране в пикселях интерфейса. */
	public int animationSize = 32;
	public boolean sounds = true;
	/** Громкость звуков мода в процентах. */
	public int soundVolume = 100;
	public boolean autoDrink = true;
	public boolean autoEat = true;
	public boolean showHud = true;
	public HudPosition hudPosition = HudPosition.TOP_LEFT;
	public HudStyle hudStyle = HudStyle.FULL;
	/** Анимация иконок и бутылочки. Выключенные — иконки стоят на первом кадре. */
	public Animations animations = Animations.ON;
	/** Показывать иконку «выключен», когда мод выключен. */
	public boolean hudWhenOff = false;
	/** Масштаб HUD в процентах. */
	public int hudScale = 100;
	public boolean autoJump = false;
	/** 0 — держать прыжок постоянно. */
	public int jumpIntervalSeconds = 1;
	public int drinkDelaySeconds = 5;
	public int eatBelowFood = 14;
	public int raidsWon = 0;

	public boolean animationsActive() {
		return switch (animations == null ? Animations.ON : animations) {
			case ON -> true;
			case OFF -> false;
			case AUTO -> Minecraft.getInstance().options.graphicsPreset().get() != GraphicsPreset.FAST;
		};
	}

	public static AobConfig load() {
		if (Files.exists(PATH)) {
			try {
				AobConfig config = GSON.fromJson(Files.readString(PATH), AobConfig.class);
				if (config != null) return config;
			} catch (Exception e) {
				AutoOminousBottleClient.LOGGER.warn("Не удалось прочитать {}, беру значения по умолчанию", PATH, e);
			}
		}
		return new AobConfig();
	}

	public void save() {
		try {
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			AutoOminousBottleClient.LOGGER.warn("Не удалось сохранить {}", PATH, e);
		}
	}
}
