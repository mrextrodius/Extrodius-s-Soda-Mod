# Upload checklist — Extrodius's Soda Mod v1.0.2 (first public release)

> Prepared 2026-10-12. One CurseForge project + one Modrinth project; each gets
> TWO files (one per MC version). Page text lives in CURSEFORGE-PAGE-DRAFT.md
> (same directory) — updated for 1.0.2.

## The files (already staged, renamed so they can't be confused)

The build outputs in both projects are both named `extrodius_soda-1.0.2.jar`,
so staged copies with unambiguous names are in `~/workspace/mc-modding/upload-staging/`:

| File (staging name) | MC version | NeoForge | Size | sha256 |
|---|---|---|---|---|
| `extrodius_soda-1.0.2-MC26.3-NeoForge26.3.0.23.jar` | 26.3 | 26.3.0.23-beta | 642,008 bytes | `fc54c78dc758548bde567aed6a79e8804423e2b637305cf03dfb847b055692dc` |
| `extrodius_soda-1.0.2-MC26.1.2-NeoForge26.1.2.116.jar` | 26.1.2 | 26.1.2.116 | 642,065 bytes | `4e208c0e7b0ed2578272177180d90b356b4208c78a8e8bdea4c5c5642e3e3b1b` |

Upload each staged file as **Release** on its own project-file slot:
- CurseForge: the 26.3 file gets "26.3" checked as supported game version,
  the 26.1.2 file gets "26.1.2". Loader: NeoForge.
- Modrinth: same pairing under its game-version selectors.

Known risk (already his call): the 1.0.2 retest ran on 26.1.2 only. The 26.3 jar
carries the identical fix but did not get its own 1.0.2 in-game retest.

## Paste-ready changelog for the 1.0.2 file uploads (both sites)

```
1.0.2 — first public release

Use new machines to brew 9 different soda cans, each with their own benefits:
Soda Mixer (dye + sugar + ingredient + empty can -> flat soda), Gasifier
(wheat + fuel -> full CO2 canister), Carbonator (full canister + flat soda ->
drinkable soda, canister returned). Every recipe is Overworld-only — no
Nether or End required. The mixer is hiding a secret recipe.

Balance: soda effects sit between base potions (3:00) and extended (8:00) —
better than base, never better than extended. When a soda buff expires you get
a mild caffeine crash (Nausea + Slowness I); it won't fire if you still have
the effect from another source. Drinking the Graveyard's 8-in-1 also grants
Undying, which saves you from death once, totem-style (a real totem in hand
still takes priority).

Fixes since the first private build:
- A real totem of undying saving you now cancels the pending caffeine crash
  (previously the crash still fired afterwards).
- Phanta Cream returns the empty bucket instead of consuming it.
- Caffeine crash timers are cleared if you die while one is pending.
- Modrinth/CurseForge listing now shows authors: MrExtrodius, and the project
  logo displays correctly (512x512 PNG, wired via iconFile/logoFile).
```

## Internal version history (for future changelogs; 1.0.0/1.0.1 were never published)

- **1.0.0** (2026-10-12): first complete build — 9 sodas, 3 machines, Graveyard
  secret recipe, Undying effect, caffeine crash. Shipped to Zach for shakedown.
- **1.0.1** (2026-10-12): totem save now cancels pending caffeine crash —
  except the 26.x totem check runs before LivingDeathEvent, so this fix
  never fired. Superseded by 1.0.2.
- **1.0.2** (2026-10-12): working totem-crash fix — the tick handler cancels a
  pending crash when all tracked soda effects vanish well before expiry
  (totem-wipe detection), covering all 9 sodas. Confirmed in-game on 26.1.2.

## Project metadata (same on both sites)

- Name: Extrodius's Soda Mod
- Summary: "Use new machines to brew 9 different soda cans, each with their own benefits."
- License: MIT — © Zachary Goshen (MrExtrodius)
- Authors: MrExtrodius
- Logo: bundled in both jars as `logo.png` (512x512, RGB, no ICC profile)
