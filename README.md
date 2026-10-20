# Auto Ominous Bottle

A small client-side Fabric mod for **Minecraft 26.2** that keeps your raid farm running while you're AFK.
When a raid ends, it drinks the next Ominous Bottle for you, so the next raid starts on its own.

> Vibe-coded with love 💜 — this whole mod was built by a vibe-code lover chatting with Claude (Anthropic's AI).
> No hand-written Java was harmed in the making of it. Bugs? Open an issue and we'll vibe them away.

## Features

- **Auto-drink** — drinks an Ominous Bottle when the raid bar is gone and you have no Bad Omen / Raid Omen.
  Exactly one bottle at a time, never while an omen is active.
- **Auto-eat** — eats when hunger drops to your threshold (skips rotten flesh, spider eyes, chorus fruit, golden apples).
- **Auto-jump** — optional, for farms where you have to jump to trigger the raid. Stops as soon as Raid Omen appears.
- **Puts things back** — bottles and food are taken from the hotbar or inventory; your sword goes back to your hand.
- **Safety** — turns on only with a double key press, turns off after rejoining, changing dimension, dying
  or at low health, and can leave the world when bottles run out.
- **Status on screen** — animated icon + raid/bottle counter in any of 8 positions, or a compact one-line text mode.
- **Raid counter**, sounds, a little Ominous Bottle mascot animation, English / Русский / Українська.

## How to use

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2 and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Put `auto-ominous-bottle-<version>.jar` into your `mods` folder.
   [Mod Menu](https://modrinth.com/mod/modmenu) is optional but recommended — it gives you the settings screen.
3. Put Ominous Bottles (and some food) anywhere in your inventory and stand in your raid farm.
4. Press **K twice** (within 3 seconds) to turn the mod on. Press **K** once to turn it off.
   The key can be changed in *Controls → Auto Ominous Bottle*.
5. Go AFK. In singleplayer press **F3 + P** first, so the game doesn't pause when the window loses focus.

## Settings (Mod Menu → Auto Ominous Bottle)

| Tab | What's there |
|---|---|
| General | key confirmation, auto-off, low-health shutoff, leave world when out of bottles, key binding, reset counter |
| Drinking | auto-drink, wait time after a raid |
| Food | auto-eat, hunger threshold |
| Jump | auto-jump, jump interval |
| HUD | show/hide, style (icon + text / icon / text only), position, size, animations on/off/auto |
| Animation | mascot animation on enable / on every drink, position, size |
| Sounds | on/off, volume, test buttons |

## Resource packs

Built-in packs (Options → Resource Packs):

- **Auto Ominous Bottle: 32x / 64x** — bigger icons and animation.
- **Animated Ominous Bottle** — the Ominous Bottle item in your inventory looks around.

Everything can be replaced with your own pack:

- icons — `assets/autoominousbottle/textures/gui/icon/<name>.png`
  (`on`, `off`, `omen`, `raid`, `wait`, `drinking`, `eating`, `no_bottles`, `toggle_on`, `toggle_off`):
  square frames stacked vertically, any resolution; frame timing in `<name>.animation.json`
  (`{"frames": [{"index": 0, "time": 10}]}`, time in ticks);
- mascot animation — `assets/autoominousbottle/textures/gui/animation/ominous_bottle.png` + `.animation.json`;
- sounds — `autoominousbottle:enable`, `disable`, `confirm`, `raid_won`, `warning`, `danger` in `sounds.json`.

## Notes

- Client-side only, works in singleplayer and on servers. Some servers don't allow automation — check the rules first.
- The mod only presses keys the way you would; it doesn't send anything special to the server.

## Building

```
gradlew build              # jar in build/libs
gradlew runClientGameTest  # in-game tests: eating, one bottle per raid, items put back, jump stop
```

## License

MIT — see [LICENSE](LICENSE).
