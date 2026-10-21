package dev.rocketnk.autoominousbottle.test;

import dev.rocketnk.autoominousbottle.AobConfig;
import dev.rocketnk.autoominousbottle.AobController;
import dev.rocketnk.autoominousbottle.AutoOminousBottleClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Проверки в настоящем мире (gradlew runClientGameTest):
 * автоеда, ровно одна бутылка за раз, возврат меча на место, прыжки прекращаются при «Знамении рейда».
 */
public class AobClientGameTest implements FabricClientGameTest {
	private static final int SWORD_SLOT = 0;
	private static final int FOOD_SLOT = 20;
	private static final int BOTTLE_SLOT = 25;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(settings -> {
					settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
					settings.setDifficulty(Difficulty.NORMAL);
				})
				.create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);

			testAutoEat(context, server);
			testDrinksOneBottle(context, server);
			testJumpStopsOnRaidOmen(context, server);
			testStopAfterRaids(context);
			testStopAfterTime(context);
			screenshotHud(context, server);
		}
	}

	/** «Остановиться после 2 рейдов»: после первой победы мод работает, после второй — выключается. */
	private void testStopAfterRaids(ClientGameTestContext context) {
		configure(context, config -> {
			config.autoDrink = false;
			config.autoEat = false;
			config.stopAfterRaids = 2;
			config.stopAction = AobConfig.StopAction.DISABLE;
		});
		enable(context);
		context.runOnClient(mc -> AobController.onRaidWon(mc, AutoOminousBottleClient.config));
		check(context.computeOnClient(mc -> AutoOminousBottleClient.config.enabled), "остановка: после 1 рейда из 2 мод работает");
		context.runOnClient(mc -> AobController.onRaidWon(mc, AutoOminousBottleClient.config));
		check(!context.computeOnClient(mc -> AutoOminousBottleClient.config.enabled), "остановка: после 2 рейдов мод выключился");
		configure(context, config -> config.stopAfterRaids = 0);
	}

	/** «Остановиться через 1 мин»: через минуту с включённым модом он выключается. */
	private void testStopAfterTime(ClientGameTestContext context) {
		configure(context, config -> {
			config.autoDrink = false;
			config.autoEat = false;
			config.stopAfterMinutes = 1;
			config.stopAction = AobConfig.StopAction.DISABLE;
		});
		enable(context);
		context.waitTicks(1100);
		check(context.computeOnClient(mc -> AutoOminousBottleClient.config.enabled), "таймер: через 55 с мод ещё работает");
		context.waitTicks(150);
		check(!context.computeOnClient(mc -> AutoOminousBottleClient.config.enabled), "таймер: через минуту мод выключился");
		configure(context, config -> config.stopAfterMinutes = 0);
	}

	/** Снимки HUD в разных видах и местах — смотреть глазами в run/screenshots. */
	private void screenshotHud(ClientGameTestContext context, TestServerContext server) {
		server.runOnServer(s -> {
			fillHotbar(player(s));
			player(s).getInventory().setItem(BOTTLE_SLOT, new ItemStack(Items.OMINOUS_BOTTLE, 7));
			player(s).addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 2400));
		});
		configure(context, config -> {
			config.autoDrink = true;
			config.autoEat = false;
		});
		enable(context);
		record Variant(String name, AobConfig.HudStyle style, AobConfig.HudPosition position) {
		}
		for (Variant variant : new Variant[] {
				new Variant("full_top_left", AobConfig.HudStyle.FULL, AobConfig.HudPosition.TOP_LEFT),
				new Variant("full_middle_right", AobConfig.HudStyle.FULL, AobConfig.HudPosition.MIDDLE_RIGHT),
				new Variant("icon_bottom_center", AobConfig.HudStyle.ICON, AobConfig.HudPosition.BOTTOM_CENTER),
				new Variant("text", AobConfig.HudStyle.TEXT, AobConfig.HudPosition.TOP_LEFT)}) {
			context.runOnClient(mc -> {
				AutoOminousBottleClient.config.hudStyle = variant.style();
				AutoOminousBottleClient.config.hudPosition = variant.position();
				dev.rocketnk.autoominousbottle.AobHud.clearMessages();
				dev.rocketnk.autoominousbottle.AobHud.showMessage(net.minecraft.network.chat.Component.translatable("autoominousbottle.msg.raid_won", AutoOminousBottleClient.config.raidsWon));
			});
			context.waitTicks(5);
			context.takeScreenshot("aob_hud_" + variant.name());
		}
		context.runOnClient(mc -> {
			AutoOminousBottleClient.config.hudStyle = AobConfig.HudStyle.FULL;
			AutoOminousBottleClient.config.hudPosition = AobConfig.HudPosition.TOP_LEFT;
		});
		server.runOnServer(s -> player(s).removeAllEffects());
		disable(context);
	}

	/** Голод 4, хлеб в инвентаре, хотбар забит, меч в руке: мод ест до порога и всё возвращает на места. */
	private void testAutoEat(ClientGameTestContext context, TestServerContext server) {
		configure(context, config -> {
			config.autoDrink = false;
			config.autoEat = true;
			config.eatBelowFood = 14;
		});
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			fillHotbar(player);
			player.getInventory().setItem(FOOD_SLOT, new ItemStack(Items.BREAD, 5));
			player.getFoodData().setFoodLevel(4);
			player.getFoodData().setSaturation(0);
		});
		enable(context);

		server.waitFor(s -> player(s).getFoodData().getFoodLevel() > 14, 600);
		context.waitTicks(60);
		server.runOnServer(s -> {
			Inventory inventory = player(s).getInventory();
			check(player(s).getFoodData().getFoodLevel() > 14, "автоеда: голод выше порога");
			check(count(inventory, Items.BREAD) == 2, "автоеда: съедено 3 хлеба, осталось " + count(inventory, Items.BREAD));
			check(inventory.getItem(SWORD_SLOT).getItem() == Items.DIAMOND_SWORD, "автоеда: меч на месте");
			check(inventory.getItem(FOOD_SLOT).getItem() == Items.BREAD, "автоеда: хлеб вернулся в свой слот");
			check(inventory.getItem(8).getItem() == Items.DIRT, "автоеда: хотбар вернулся как был");
		});
		check(context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == SWORD_SLOT, "автоеда: в руке снова меч");
		disable(context);
	}

	/** 5 бутылок в инвентаре, хотбар забит: мод пьёт ровно одну и возвращает меч. */
	private void testDrinksOneBottle(ClientGameTestContext context, TestServerContext server) {
		configure(context, config -> {
			config.autoDrink = true;
			config.autoEat = false;
			config.drinkDelaySeconds = 3;
		});
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.removeAllEffects();
			fillHotbar(player);
			player.getInventory().setItem(BOTTLE_SLOT, new ItemStack(Items.OMINOUS_BOTTLE, 5));
		});
		enable(context);

		server.waitFor(s -> player(s).hasEffect(MobEffects.BAD_OMEN) || player(s).hasEffect(MobEffects.RAID_OMEN), 600);
		// Ждём дольше, чем пауза + время глотка: если бы мод пил повторно, это бы уже случилось.
		context.waitTicks(200);
		server.runOnServer(s -> {
			Inventory inventory = player(s).getInventory();
			check(count(inventory, Items.OMINOUS_BOTTLE) == 4, "питьё: выпита ровно одна бутылка, осталось " + count(inventory, Items.OMINOUS_BOTTLE));
			check(inventory.getItem(SWORD_SLOT).getItem() == Items.DIAMOND_SWORD, "питьё: меч на месте");
			check(inventory.getItem(BOTTLE_SLOT).getItem() == Items.OMINOUS_BOTTLE, "питьё: бутылки вернулись в свой слот");
		});
		check(context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot()) == SWORD_SLOT, "питьё: в руке снова меч");
		disable(context);
	}

	/** С «Дурным знамением» мод прыгает, а как только появилось «Знамение рейда» — перестаёт. */
	private void testJumpStopsOnRaidOmen(ClientGameTestContext context, TestServerContext server) {
		configure(context, config -> {
			config.autoDrink = false;
			config.autoEat = false;
			config.autoJump = true;
			config.jumpIntervalSeconds = 0;
		});
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.removeAllEffects();
			player.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 2400));
		});
		enable(context);
		context.waitTicks(20);
		check(context.computeOnClient(mc -> mc.options.keyJump.isDown()), "прыжок: с «Дурным знамением» прыгает");

		server.runOnServer(s -> player(s).addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 2400)));
		context.waitTicks(10);
		check(!context.computeOnClient(mc -> mc.options.keyJump.isDown()), "прыжок: со «Знамением рейда» перестал");

		server.runOnServer(s -> player(s).removeAllEffects());
		disable(context);
	}

	private static void configure(ClientGameTestContext context, Consumer<AobConfig> change) {
		context.runOnClient(mc -> {
			AobConfig config = AutoOminousBottleClient.config;
			config.confirmEnable = false;
			config.autoDisable = false;
			config.autoJump = false;
			config.exitWhenOutOfBottles = false;
			config.stopAfterRaids = 0;
			config.stopAfterMinutes = 0;
			change.accept(config);
		});
	}

	private static void enable(ClientGameTestContext context) {
		context.waitTicks(5);
		context.runOnClient(mc -> {
			mc.player.getInventory().setSelectedSlot(SWORD_SLOT);
			AutoOminousBottleClient.setEnabled(true);
		});
	}

	private static void disable(ClientGameTestContext context) {
		context.runOnClient(mc -> AutoOminousBottleClient.setEnabled(false));
		context.waitTicks(5);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	/** Меч в слоте 0, остальной хотбар — земля, весь инвентарь кроме этого пуст. */
	private static void fillHotbar(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		inventory.clearContent();
		inventory.setItem(SWORD_SLOT, new ItemStack(Items.DIAMOND_SWORD));
		for (int i = 1; i < 9; i++) inventory.setItem(i, new ItemStack(Items.DIRT, 16));
	}

	private static int count(Inventory inventory, Item item) {
		int total = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.getItem() == item) total += stack.getCount();
		}
		return total;
	}

	private static void check(boolean condition, String what) {
		if (!condition) throw new AssertionError("FAILED: " + what);
		System.out.println("[AOB TEST] OK: " + what);
	}
}
