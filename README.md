# Auto Ominous Bottle

A small client-side Fabric mod for **Minecraft 26.2** that keeps your raid farm running while you're AFK.
When a raid ends, it drinks the next Ominous Bottle for you, so the next raid starts on its own.

> 💜 100% vibe-coded with Claude — see [a word from the dev](#a-word-from-the-dev).

## Features

- **Auto-drink** — drinks an Ominous Bottle when the raid bar is gone and you have no Bad Omen / Raid Omen.
  Exactly one bottle at a time, never while an omen is active.
- **Auto-eat** — eats when hunger drops to your threshold. Never eats food that hurts you
  (rotten flesh, spider eyes, pufferfish, poisonous potatoes, suspicious stew), chorus fruit or golden apples.
- **Auto-jump** — optional, for farms where you have to jump to trigger the raid. Stops as soon as Raid Omen appears.
- **Puts things back** — bottles and food are taken from the hotbar or inventory; whatever you were holding
  goes back to your hand.
- **Safety** — turns on only with a double key press, turns off after rejoining, changing dimension, dying
  or at low health, and can leave the world when bottles run out.
- **Stop when you want** — after N raids and/or after X hours, then turn the mod off or leave the world.
- **Status on screen** — animated icon + raid/bottle counter in any of 8 positions, or a compact one-line text mode.
- **Raid counter**, sounds, a little Ominous Bottle mascot animation, English / Русский / Українська.

## How to use

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2 and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download `auto-ominous-bottle-<version>.jar` from [Releases](https://github.com/tottori139/auto-ominous-bottle/releases)
   and put it into your `mods` folder.
   [Mod Menu](https://modrinth.com/mod/modmenu) is optional but recommended — it gives you the settings screen.
3. Put Ominous Bottles (and some food) anywhere in your inventory and stand in your raid farm.
4. Press **K twice** (within 3 seconds) to turn the mod on — a status icon shows up in the top-left corner.
   Press **K** once to turn it off. The key can be changed in *Controls → Auto Ominous Bottle*.
5. Go AFK. In singleplayer press **F3 + P** first, so the game doesn't pause when the window loses focus.

## Settings (Mod Menu → Auto Ominous Bottle)

| Tab | What's there |
|---|---|
| General | key confirmation, auto-off, low-health shutoff, leave world when out of bottles, key binding, reset counter |
| Drinking | auto-drink, wait time after a raid, stop after N raids / X time, then turn off or leave the world |
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
- The mod only does what you could do with your own keyboard and mouse: use an item, move items between slots, jump.
  It doesn't send anything special to the server.
- Made for Minecraft 26.2 only.

## Building

```
gradlew build              # jar in build/libs
gradlew runClientGameTest  # in-game tests: eating, one bottle per raid, items put back, jump stop, stop conditions
gradlew runPreview         # opens the settings screen and saves screenshots of every tab to run/screenshots
```

Needs a JDK 25 somewhere Gradle can find it (`JAVA_HOME`, or a JDK installed by IntelliJ / your OS).
GitHub Actions builds every push (`.github/workflows/build.yml`) and keeps the jar as an artifact.
To publish a release, bump `version` in `gradle.properties`, commit, then push a matching tag:
`git tag v1.10.0 && git push origin v1.10.0` — `release.yml` builds the jar and attaches it to the release.

## A word from the dev

I didn't write a single line of this Java myself. I told Claude what I wanted, it wrote the code,
and I playtested it in my raid farm until it stopped doing dumb stuff. So yes, it's vibe-coded —
but it's tested: there are in-game tests for eating, drinking exactly one bottle and putting your sword back.
Something broke? Open an issue.

## Credits

- Made by [tottori139](https://github.com/tottori139), code written with Claude (Anthropic).
- The Ominous Bottle mascot and icons are pixel art based on Minecraft's vanilla Ominous Bottle texture (© Mojang).

## License

MIT — see [LICENSE](LICENSE).
