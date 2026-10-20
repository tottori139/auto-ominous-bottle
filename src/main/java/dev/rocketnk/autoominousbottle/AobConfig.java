package dev.rocketnk.autoominousbottle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AobConfig {
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
	public boolean sounds = true;
	/** Громкость звуков мода в процентах. */
	public int soundVolume = 100;
	public boolean autoDrink = true;
	public boolean autoEat = true;
	public boolean showHud = true;
	public boolean autoJump = false;
	/** 0 — держать прыжок постоянно. */
	public int jumpIntervalSeconds = 1;
	public int drinkDelaySeconds = 5;
	public int eatBelowFood = 14;
	public int raidsWon = 0;

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
