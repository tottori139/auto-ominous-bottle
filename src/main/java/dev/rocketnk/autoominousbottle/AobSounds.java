package dev.rocketnk.autoominousbottle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Звуки мода. Каждый — своё событие из assets/autoominousbottle/sounds.json,
 * которое по умолчанию ссылается на ванильный звук, так что ресурспак может подменить любой из них.
 */
public final class AobSounds {
	public static final SoundEvent ENABLE = event("enable");
	public static final SoundEvent DISABLE = event("disable");
	public static final SoundEvent CONFIRM = event("confirm");
	public static final SoundEvent RAID_WON = event("raid_won");
	public static final SoundEvent WARNING = event("warning");
	public static final SoundEvent DANGER = event("danger");

	private AobSounds() {
	}

	public static void play(SoundEvent sound) {
		AobConfig config = AutoOminousBottleClient.config;
		if (!config.sounds || config.soundVolume <= 0) return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, config.soundVolume / 100.0F));
	}

	private static SoundEvent event(String name) {
		return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(AutoOminousBottleClient.MOD_ID, name));
	}
}
