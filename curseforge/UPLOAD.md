# Как залить на CurseForge

Всё, что нужно, лежит в этой папке. Сайт: https://authors.curseforge.com → **Create Project** → Minecraft → **Mods**.

## 1. Проект

| Поле | Что ставить |
|---|---|
| Name | `Auto Ominous Bottle` |
| Summary | `Drinks an Ominous Bottle after every raid so your AFK raid farm keeps running. Auto-eat, auto-jump, safety stops.` |
| Description | содержимое `description.md` (в редакторе выбери Markdown) |
| Logo / Avatar | `logo.png` (512×512) |
| Main category | **Utility & QoL** |
| Доп. категории | по желанию: Miscellaneous |
| License | **MIT License** |
| Source / Issues | `https://github.com/tottori139/auto-ominous-bottle` и `.../issues` |

## 2. Файл

| Поле | Что ставить |
|---|---|
| File | `auto-ominous-bottle-1.10.0.jar` (из `build/libs` или со страницы Releases на GitHub) |
| Display name | `Auto Ominous Bottle 1.10.0` |
| Release type | **Release** |
| Game version | **26.2** |
| Mod loader | **Fabric** |
| Environment | **Client** |
| Java | **25** |
| Relations | **Fabric API** — Required Dependency; **Mod Menu** — Optional Dependency |
| Changelog | `First release.` |

## 3. Картинки (вкладка Images)

Загрузи из `screenshots/`:
- `0000_aob_hud_full_top_left.png` — статус в углу;
- `0003_aob_hud_text.png` — режим «только текст»;
- `menu_tab_1.png`, `menu_tab_4.png`, `menu_tab_5_smile.png` — меню.

В описание каждой картинки допиши: `Icons are AI-assisted pixel art.` — у CurseForge такое правило для картинок, сделанных с ИИ.

## 4. Дальше

Проект уходит на ручную проверку модераторами — это может занять какое-то время, до этого он не виден другим.
Новые версии: выпускаешь релиз на GitHub (метка `vX.Y.Z`) и загружаешь тот же jar на CurseForge в **Files → Upload File**.
