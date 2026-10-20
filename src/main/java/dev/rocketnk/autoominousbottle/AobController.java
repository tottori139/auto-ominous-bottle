package dev.rocketnk.autoominousbottle;

import dev.rocketnk.autoominousbottle.mixin.BossHealthOverlayAccessor;
import dev.rocketnk.autoominousbottle.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Вся логика: следит за полоской рейда и эффектами, пьёт бутылку и ест. */
public final class AobController {
	public enum State { IDLE, RAID, OMEN, WAIT, DRINKING, EATING, NO_BOTTLES }

	private enum Action { DRINK, EAT }

	private static final int HOTBAR_SIZE = 9;
	private static final int MAIN_INVENTORY_END = 36;
	/** Пауза после любого использования предмета, чтобы сервер успел прислать эффекты. */
	private static final int AFTER_USE_COOLDOWN = 40;
	/** Сколько ждём знамение после глотка, прежде чем считать, что бутылка не сработала. */
	private static final int OMEN_CHECK_TICKS = 100;
	private static final int MAX_FAILED_DRINKS = 2;
	private static final int MAX_USE_TICKS = 100;
	private static final int JUMP_PRESS_TICKS = 2;
	/** Задержка ПКМ, которую выставляем каждый тик, пока мод пьёт или ест. */
	private static final int USE_REPEAT_BLOCK_TICKS = 4;
	/** Сколько ждём после «бутылки кончились», прежде чем выйти из мира. */
	private static final int EXIT_DELAY_TICKS = 200;

	/** Еда, которую не трогаем: вредная или слишком ценная. */
	private static final Set<Item> FOOD_BLACKLIST = Set.of(
			Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH,
			Items.CHORUS_FRUIT, Items.SUSPICIOUS_STEW, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);

	private static final Predicate<ItemStack> IS_BOTTLE = stack -> stack.getItem() == Items.OMINOUS_BOTTLE;
	private static final Predicate<ItemStack> IS_FOOD =
			stack -> stack.has(DataComponents.FOOD) && !FOOD_BLACKLIST.contains(stack.getItem());

	private static State state = State.IDLE;
	private static int quietTicks;
	private static int cooldownTicks;
	private static int omenCheckTicks;
	private static int failedDrinks;

	private static Action using;
	private static int usingTicks;
	private static int restoreSlot = -1;
	private static int swappedInventorySlot = -1;
	private static int swappedHotbarSlot = -1;

	private static boolean jumping;
	private static int jumpTicks;

	private static boolean raidBarVisible;
	private static final Set<UUID> countedVictories = new HashSet<>();

	private static boolean warnedNoBottles;
	private static int noBottleTicks;
	private static boolean warnedNoFood;

	private AobController() {
	}

	public static State state() {
		return state;
	}

	public static int secondsUntilDrink() {
		int left = AutoOminousBottleClient.config.drinkDelaySeconds * 20 - quietTicks;
		return Math.max(0, (left + 19) / 20);
	}

	public static void reset() {
		Minecraft mc = Minecraft.getInstance();
		if (using != null && mc.player != null && mc.gameMode != null) {
			stopUsing(mc, mc.player);
		}
		if (jumping) mc.options.keyJump.setDown(false);
		jumping = false;
		jumpTicks = 0;
		state = State.IDLE;
		quietTicks = 0;
		cooldownTicks = 0;
		omenCheckTicks = 0;
		failedDrinks = 0;
		warnedNoBottles = false;
		warnedNoFood = false;
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		if (player == null || mc.gameMode == null) {
			using = null;
			restoreSlot = -1;
			swappedInventorySlot = -1;
			swappedHotbarSlot = -1;
			jumping = false;
			raidBarVisible = false;
			countedVictories.clear();
			state = State.IDLE;
			return;
		}

		AobConfig config = AutoOminousBottleClient.config;
		updateRaidBar(mc, config);
		tickActions(mc, player, config);
		tickJump(mc, player, config);
	}

	private static void tickActions(Minecraft mc, LocalPlayer player, AobConfig config) {
		if (!config.enabled) {
			if (using != null) stopUsing(mc, player);
			state = State.IDLE;
			return;
		}

		if (config.lowHealthDisable && player.getHealth() <= config.lowHealth && !player.isDeadOrDying()) {
			AutoOminousBottleClient.disable(Component.translatable("autoominousbottle.msg.off_low_health"));
			AobSounds.play(AobSounds.DANGER);
			return;
		}

		if (using != null) {
			continueUsing(mc, player);
			return;
		}

		if (cooldownTicks > 0) cooldownTicks--;
		checkDrinkWorked(player, config);

		// Пока открыт инвентарь, чат или меню, ничего не трогаем.
		if (mc.gui.screen() != null) return;

		if (config.autoEat && player.getFoodData().getFoodLevel() <= config.eatBelowFood && cooldownTicks == 0) {
			int slot = findSlot(player.getInventory(), IS_FOOD);
			if (slot >= 0) {
				warnedNoFood = false;
				startUsing(mc, player, Action.EAT, slot);
				return;
			}
			if (!warnedNoFood) {
				warnedNoFood = true;
				warn(player, Component.translatable("autoominousbottle.msg.no_food"));
			}
		}

		if (!config.autoDrink) {
			state = State.IDLE;
			return;
		}

		if (raidBarVisible) {
			state = State.RAID;
			quietTicks = 0;
			return;
		}
		if (hasOmen(player)) {
			state = State.OMEN;
			quietTicks = 0;
			return;
		}

		quietTicks++;
		if (quietTicks < config.drinkDelaySeconds * 20 || cooldownTicks > 0 || omenCheckTicks > 0) {
			state = State.WAIT;
			return;
		}

		int slot = findSlot(player.getInventory(), IS_BOTTLE);
		if (slot < 0) {
			state = State.NO_BOTTLES;
			if (!warnedNoBottles) {
				warnedNoBottles = true;
				noBottleTicks = 0;
				warn(player, Component.translatable(config.exitWhenOutOfBottles
						? "autoominousbottle.msg.no_bottles_exit" : "autoominousbottle.msg.no_bottles"));
			}
			if (config.exitWhenOutOfBottles && ++noBottleTicks >= EXIT_DELAY_TICKS) {
				exitWorld(mc);
			}
			return;
		}
		warnedNoBottles = false;
		startUsing(mc, player, Action.DRINK, slot);
	}

	/** Бутылки кончились: выключаемся и выходим из мира, чтобы не стоять зря. */
	private static void exitWorld(Minecraft mc) {
		AutoOminousBottleClient.disable(Component.translatable("autoominousbottle.msg.off"));
		mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
	}

	public static int countBottles(LocalPlayer player) {
		Inventory inventory = player.getInventory();
		int count = 0;
		for (int i = 0; i < MAIN_INVENTORY_END; i++) {
			ItemStack stack = inventory.getItem(i);
			if (IS_BOTTLE.test(stack)) count += stack.getCount();
		}
		return count;
	}

	public static void say(Component message) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) AobHud.showMessage(message);
	}

	/**
	 * Прыгаем, пока есть «Дурное знамение»: на некоторых фермах без прыжка оно не превращается в «Знамение рейда».
	 * Как только появилось «Знамение рейда» — рейд уже запущен, прыгать хватит.
	 */
	private static void tickJump(Minecraft mc, LocalPlayer player, AobConfig config) {
		boolean wanted = config.enabled && config.autoJump && using == null && mc.gui.screen() == null
				&& !raidBarVisible && player.hasEffect(MobEffects.BAD_OMEN) && !player.hasEffect(MobEffects.RAID_OMEN);
		if (!wanted) {
			if (jumping) mc.options.keyJump.setDown(false);
			jumping = false;
			jumpTicks = 0;
			return;
		}
		int interval = config.jumpIntervalSeconds * 20;
		boolean press = interval == 0 || jumpTicks % interval < JUMP_PRESS_TICKS;
		jumpTicks++;
		mc.options.keyJump.setDown(press);
		jumping = press;
	}

	private static boolean hasOmen(LocalPlayer player) {
		return player.hasEffect(MobEffects.BAD_OMEN) || player.hasEffect(MobEffects.RAID_OMEN);
	}

	/** Если после глотка знамение так и не появилось, не сливаем все бутылки, а выключаем автопитьё. */
	private static void checkDrinkWorked(LocalPlayer player, AobConfig config) {
		if (omenCheckTicks <= 0) return;
		if (hasOmen(player) || raidBarVisible) {
			omenCheckTicks = 0;
			failedDrinks = 0;
			return;
		}
		if (--omenCheckTicks > 0) return;
		failedDrinks++;
		if (failedDrinks >= MAX_FAILED_DRINKS) {
			failedDrinks = 0;
			config.autoDrink = false;
			config.save();
			warn(player, Component.translatable("autoominousbottle.msg.drink_failed"));
		}
	}

	private static void updateRaidBar(Minecraft mc, AobConfig config) {
		Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) mc.gui.hud.getBossOverlay()).aob$getEvents();
		boolean visible = false;
		for (Map.Entry<UUID, LerpingBossEvent> entry : events.entrySet()) {
			Component name = entry.getValue().getName();
			if (!hasTranslationKey(name, "event.minecraft.raid")) continue;
			visible = true;
			if (hasTranslationKey(name, "event.minecraft.raid.victory") && countedVictories.add(entry.getKey())) {
				config.raidsWon++;
				config.save();
				if (config.enabled) {
					say(Component.translatable("autoominousbottle.msg.raid_won", config.raidsWon));
					AobSounds.play(AobSounds.RAID_WON);
				}
			}
		}
		countedVictories.retainAll(events.keySet());
		raidBarVisible = visible;
	}

	private static boolean hasTranslationKey(Component component, String prefix) {
		if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(prefix)) {
			return true;
		}
		for (Component sibling : component.getSiblings()) {
			if (hasTranslationKey(sibling, prefix)) return true;
		}
		return false;
	}

	/** Сначала хотбар, потом остальной инвентарь. */
	private static int findSlot(Inventory inventory, Predicate<ItemStack> predicate) {
		for (int i = 0; i < MAIN_INVENTORY_END; i++) {
			if (predicate.test(inventory.getItem(i))) return i;
		}
		return -1;
	}

	private static void startUsing(Minecraft mc, LocalPlayer player, Action action, int inventorySlot) {
		Inventory inventory = player.getInventory();
		int hotbarSlot = inventorySlot;
		if (inventorySlot >= HOTBAR_SIZE) {
			hotbarSlot = freeHotbarSlot(inventory);
			swap(mc, player, inventorySlot, hotbarSlot);
			// Запоминаем, чтобы после глотка вернуть предметы на свои места (меч — обратно в хотбар).
			swappedInventorySlot = inventorySlot;
			swappedHotbarSlot = hotbarSlot;
		}

		restoreSlot = inventory.getSelectedSlot();
		inventory.setSelectedSlot(hotbarSlot);
		mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
		blockUseRepeat(mc);
		mc.options.keyUse.setDown(true);

		using = action;
		usingTicks = 0;
		if (action == Action.DRINK && AutoOminousBottleClient.config.animationOnDrink) AobAnimation.play();
		state = action == Action.DRINK ? State.DRINKING : State.EATING;
	}

	/** Пустой слот хотбара, иначе последний слот, который сейчас не в руке. */
	private static int freeHotbarSlot(Inventory inventory) {
		for (int i = 0; i < HOTBAR_SIZE; i++) {
			if (inventory.getItem(i).isEmpty()) return i;
		}
		int selected = inventory.getSelectedSlot();
		return selected == HOTBAR_SIZE - 1 ? HOTBAR_SIZE - 2 : HOTBAR_SIZE - 1;
	}

	/** Меняет местами слот инвентаря (9..35, совпадает с номером в меню игрока) и слот хотбара. */
	private static void swap(Minecraft mc, LocalPlayer player, int inventorySlot, int hotbarSlot) {
		mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, inventorySlot, hotbarSlot, ContainerInput.SWAP, player);
	}

	/** Держим ПКМ, пока предмет не съеден или не выпит. */
	private static void continueUsing(Minecraft mc, LocalPlayer player) {
		usingTicks++;
		// Сервер сообщает «выпито» в начале тика, а ПКМ у нас ещё зажата — без задержки игра сразу
		// начала бы пить следующую бутылку из той же стопки. Поэтому держим задержку ПКМ ненулевой.
		blockUseRepeat(mc);
		// Знамение уже есть — значит бутылка выпита, больше не пьём.
		boolean omenArrived = using == Action.DRINK && hasOmen(player);
		if (player.isUsingItem() && !omenArrived && usingTicks < MAX_USE_TICKS) {
			mc.options.keyUse.setDown(true);
			return;
		}
		Action finished = using;
		stopUsing(mc, player);
		cooldownTicks = AFTER_USE_COOLDOWN;
		if (finished == Action.DRINK) {
			quietTicks = 0;
			omenCheckTicks = OMEN_CHECK_TICKS;
		}
	}

	private static void blockUseRepeat(Minecraft mc) {
		((MinecraftAccessor) mc).aob$setRightClickDelay(USE_REPEAT_BLOCK_TICKS);
	}

	private static void stopUsing(Minecraft mc, LocalPlayer player) {
		blockUseRepeat(mc);
		mc.options.keyUse.setDown(false);
		if (player.isUsingItem()) mc.gameMode.releaseUsingItem(player);
		if (restoreSlot >= 0) player.getInventory().setSelectedSlot(restoreSlot);
		restoreSlot = -1;
		if (swappedInventorySlot >= 0) {
			// Остаток бутылок уходит обратно в инвентарь, а что лежало в хотбаре — возвращается.
			swap(mc, player, swappedInventorySlot, swappedHotbarSlot);
			swappedInventorySlot = -1;
			swappedHotbarSlot = -1;
		}
		using = null;
	}

	private static void warn(LocalPlayer player, Component message) {
		AobHud.showMessage(message);
		AobSounds.play(AobSounds.WARNING);
	}
}
