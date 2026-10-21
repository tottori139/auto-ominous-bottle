# Auto Ominous Bottle

**Keeps your raid farm running while you're AFK.** When a raid ends, the mod drinks the next Ominous Bottle for you, so the next raid starts on its own.

## Features

- **Auto-drink** — drinks an Ominous Bottle when the raid is over and you have no Bad Omen / Raid Omen. Exactly one bottle at a time.
- **Auto-eat** — eats when you get hungry. Never eats food that hurts you, chorus fruit or golden apples.
- **Auto-jump** — optional, for farms where you have to jump to trigger the raid. Stops as soon as Raid Omen appears.
- **Puts things back** — bottles and food can be anywhere in your inventory; whatever you were holding goes back to your hand.
- **Safety** — turns on only with a double key press; turns off after rejoining, changing dimension, dying or at low health.
- **Stop when you want** — after N raids and/or X hours, then turn off or leave the world.
- **Status on screen** — animated icon with raid and bottle counters in any of 8 positions, or a compact one-line text mode.
- Raid counter, sounds, a small mascot animation, English / Русский / Українська.

## How to use

1. Install **Fabric API**. **Mod Menu** is optional — it gives you the settings screen.
2. Put Ominous Bottles (and some food) in your inventory and stand in your raid farm.
3. Press **K twice** to turn the mod on (a status icon appears in the top-left corner). Press **K** once to turn it off. The key can be changed in Controls.
4. In singleplayer press **F3 + P** first, so the game doesn't pause when you tab out. Go AFK.

## Good to know

- Client-side only. Works in singleplayer and on servers, but **some servers don't allow automation — check the rules first.**
- The mod only does what you could do yourself with keyboard and mouse: use an item, move items between slots, jump.
- Built-in resource packs: 32x / 64x icons and an animated Ominous Bottle item. Icons, animation and sounds can be replaced with your own resource pack — see the GitHub README.
- Minecraft 26.2, Fabric.

## AI disclosure

This mod is vibe-coded: the code was written with Claude (Anthropic's AI) and playtested by me. The icons and mascot are pixel art made with AI help, based on the vanilla Ominous Bottle texture (© Mojang). It's covered by in-game tests (eating, drinking exactly one bottle, putting items back, stopping on time).

Source code, issues and changelog: https://github.com/tottori139/auto-ominous-bottle
