# Barrel Loot Generation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add barrel loot generation to BetterStructures (Lootify system). Barrels found in generated structures fill with food-by-default loot, tuned to 1-3 items per barrel. Server owners can disable barrel fills, swap the loot table, or override per-schematic.

**Architecture:** Mirror the existing chest pipeline. `ChestContents` already operates on `org.bukkit.block.Container`, which `Barrel` implements — no changes needed to the loot-rolling itself. The work is (1) detect barrels inherently in both pasting pipelines (modules + schematics) — no sign marker; (2) add a second `ChestContents` slot ("barrelContents") on generators/schematics pointing to a separate treasure file; (3) route the fill to chest- or barrel-contents based on the placed block type; (4) ship a `treasure_barrel_food.yml` premade with food-only defaults and `mean: 1`, `standardDeviation: 0.7` so each barrel rolls 1-3 items; (5) add a `generateLootInBarrels` flag (default `true`) that lets users opt out per-generator.

**Tech Stack:** Java 17, Paper/Spigot API (`org.bukkit.block.Container`, `Barrel`, `Material.BARREL`), WorldEdit, Lombok, Gradle. Built via `./gradlew build`.

**Design decisions (locked in):**
- **Inherent detection** — any `BARREL` block in a schematic or module is detected and filled. No `[barrel]` sign marker.
- **`generateLootInBarrels: true`** (default) on `GeneratorConfigFields` and `ModulesConfigFields`. Setting `false` skips barrel fills for that generator. No schematic-level override of this flag (YAGNI).
- **Separate `barrelTreasureFilename`** on `GeneratorConfigFields` / `SchematicConfigField` / `ModulesConfigFields`. Defaults to `"treasure_barrel_food"`.
- **Food-only default** with three rarity tiers (common / rare / epic, weights 60 / 30 / 10) mirroring the chest table shape.
- **1-3 items per barrel** via `mean: 1`, `standardDeviation: 0.7` on the barrel treasure config (the existing `ceil(gaussian) + 1` formula in `ChestContents.java:166-168` puts the distribution at 1-3 with a short tail).
- Reuse `ChestContents` class as-is — it's already container-agnostic. (Renaming would break public-API users.)
- Keep the existing `ChestFillEvent` for barrels too — it already accepts `Container`. No new event class.

**Out of scope:** trapped barrels (don't exist in MC), per-instance barrel orientation handling beyond what vanilla barrel `BlockData` already does, MMOItems-only barrel tables (users can author those via the generic treasure config).

---

## Files touched (overview)

- **Create:** `src/main/java/com/magmaguy/betterstructures/config/treasures/premade/BarrelFoodTreasureConfig.java`
- **Modify:** `src/main/java/com/magmaguy/betterstructures/util/DefaultChestContents.java` — add `barrelFoodContents()`
- **Modify:** `src/main/java/com/magmaguy/betterstructures/config/treasures/TreasureConfigFields.java` — make `mean` / `standardDeviation` defaults respect the current field value so subclasses can tune (tiny refactor)
- **Modify:** `src/main/java/com/magmaguy/betterstructures/config/generators/GeneratorConfigFields.java` — add `barrelTreasureFilename`, `barrelContents`, `generateLootInBarrels`
- **Modify:** `src/main/java/com/magmaguy/betterstructures/config/modules/ModulesConfigFields.java` — add `barrelTreasureFilename`, `barrelContents`, `generateLootInBarrels`
- **Modify:** `src/main/java/com/magmaguy/betterstructures/config/schematics/SchematicConfigField.java` — add `barrelTreasureFilename` + `barrelContents` (per-schematic override of the treasure file only)
- **Modify:** `src/main/java/com/magmaguy/betterstructures/schematics/SchematicContainer.java` — add `Material.BARREL` to the location-collection check
- **Modify:** `src/main/java/com/magmaguy/betterstructures/buildingfitter/FitAnything.java` — route chest vs barrel in `fillChests()`; respect `generateLootInBarrels`
- **Modify:** `src/main/java/com/magmaguy/betterstructures/modules/ModulePasting.java` — track barrel blocks during paste, fill them after paste, respect `generateLootInBarrels`; widen `isNbtRichMaterial` to exclude `BARREL`

---

## Task 1: Add the tiered food loot map

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/util/DefaultChestContents.java`

**Step 1: Add `barrelFoodContents()`**

Append this method to `DefaultChestContents`, next to `overworldContents()`. Three tiers, weights 60 / 30 / 10, mirroring the chest tables.

```java
public static Map<String, Object> barrelFoodContents() {
    Map<String, Object> items = new HashMap<>();
    Map<String, Object> commonItems = new HashMap<>();
    Map<String, Object> rareItems = new HashMap<>();
    Map<String, Object> epicItems = new HashMap<>();
    List<Map<String, Object>> commonList = new ArrayList<>();
    List<Map<String, Object>> rareList = new ArrayList<>();
    List<Map<String, Object>> epicList = new ArrayList<>();

    // Common — staples and raw foods (peasant's pantry)
    commonList.add(generateEntry(Material.BREAD, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.APPLE, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.CARROT, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.POTATO, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.BEETROOT, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.SWEET_BERRIES, 1, 4, normalWeight));
    commonList.add(generateEntry(Material.GLOW_BERRIES, 1, 4, normalWeight));
    commonList.add(generateEntry(Material.MELON_SLICE, 1, 4, normalWeight));
    commonList.add(generateEntry(Material.DRIED_KELP, 2, 6, normalWeight));
    commonList.add(generateEntry(Material.COOKIE, 2, 6, normalWeight));
    commonList.add(generateEntry(Material.BEEF, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.PORKCHOP, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.MUTTON, 1, 3, normalWeight));
    commonList.add(generateEntry(Material.COD, 1, 4, normalWeight));
    commonList.add(generateEntry(Material.SALMON, 1, 4, normalWeight));
    commonList.add(generateEntry(Material.CHICKEN, 1, 3, rareWeight));
    commonList.add(generateEntry(Material.RABBIT, 1, 3, rareWeight));
    commonList.add(generateEntry(Material.TROPICAL_FISH, 1, 2, extraRareWeight));
    commonList.add(generateEntry(Material.CHORUS_FRUIT, 1, 3, extraRareWeight));

    // Rare — cooked / processed (someone actually fed the fire)
    rareList.add(generateEntry(Material.COOKED_BEEF, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.COOKED_PORKCHOP, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.COOKED_MUTTON, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.COOKED_CHICKEN, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.COOKED_COD, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.COOKED_SALMON, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.COOKED_RABBIT, 1, 2, normalWeight));
    rareList.add(generateEntry(Material.BAKED_POTATO, 1, 3, normalWeight));
    rareList.add(generateEntry(Material.PUMPKIN_PIE, 1, 2, rareWeight));
    rareList.add(generateEntry(Material.HONEY_BOTTLE, 1, 2, rareWeight));
    rareList.add(generateEntry(Material.MUSHROOM_STEW, 1, 1, rareWeight));
    rareList.add(generateEntry(Material.BEETROOT_SOUP, 1, 1, rareWeight));
    rareList.add(generateEntry(Material.SUSPICIOUS_STEW, 1, 1, extraRareWeight));
    rareList.add(generateEntry(Material.RABBIT_STEW, 1, 1, extraRareWeight));

    // Epic — premium (the lord's larder)
    epicList.add(generateEntry(Material.GOLDEN_CARROT, 1, 3, normalWeight));
    epicList.add(generateEntry(Material.GOLDEN_APPLE, 1, 2, rareWeight));
    epicList.add(generateEntry(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, extraRareWeight));
    epicList.add(generateEntry(Material.CAKE, 1, 1, extraRareWeight));

    commonItems.put("weight", 60);
    commonItems.put("items", commonList);
    rareItems.put("weight", 30);
    rareItems.put("items", rareList);
    epicItems.put("weight", 10);
    epicItems.put("items", epicList);
    items.put("common", commonItems);
    items.put("rare", rareItems);
    items.put("epic", epicItems);
    return items;
}
```

**Step 2: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/util/DefaultChestContents.java
git commit -m "feat(lootify): add tiered barrel food loot map"
```

---

## Task 2: Let `TreasureConfigFields` subclasses override `mean` / `standardDeviation` defaults

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/config/treasures/TreasureConfigFields.java`

**Why this is needed:** `processConfigFields()` currently hardcodes the defaults — `processDouble("mean", mean, 4, true)`. If `BarrelFoodTreasureConfig` sets `setMean(1)` in its constructor, that call runs *before* `processConfigFields`, and the literal `4` in `processDouble` would still win as the on-disk default. By passing `mean` itself as the default, a subclass-set value becomes the default written to YAML.

**Step 1: Edit `processConfigFields()`**

In `TreasureConfigFields.java` around lines 54-55, change:

```java
this.mean = processDouble("mean", mean, 4, true);
this.standardDeviation = processDouble("standardDeviation", standardDeviation, 3, true);
```

to:

```java
this.mean = processDouble("mean", mean, mean, true);
this.standardDeviation = processDouble("standardDeviation", standardDeviation, standardDeviation, true);
```

Existing chest treasure configs keep working because their field initial values (declared at lines 37-40) are still `4` and `3` — same defaults, just sourced from the field instead of a literal.

**Step 2: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/config/treasures/TreasureConfigFields.java
git commit -m "refactor(lootify): source mean/stddev defaults from field, not literals"
```

---

## Task 3: Ship the `treasure_barrel_food` premade with tuned mean/stddev

**Files:**
- Create: `src/main/java/com/magmaguy/betterstructures/config/treasures/premade/BarrelFoodTreasureConfig.java`

**Step 1: Create the premade class**

`TreasureConfig.java:13` auto-discovers everything in the `premade` package — no registration needed.

```java
package com.magmaguy.betterstructures.config.treasures.premade;

import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.DefaultChestContents;

public class BarrelFoodTreasureConfig extends TreasureConfigFields {
    public BarrelFoodTreasureConfig() {
        super("treasure_barrel_food", true);
        super.setRawLoot(DefaultChestContents.barrelFoodContents());
        super.setMean(1);
        super.setStandardDeviation(0.7);
    }
}
```

**Step 2: Deploy + verify the YAML file is written**

Run `./gradlew build` then deploy to a testbed ([reference_testbed_setup.md](../../../../../.claude/projects/C--Users-tiago-Documents-MineCraftProjects/memory/reference_testbed_setup.md)). Start the server once cleanly.

Expected: `plugins/BetterStructures/treasures/treasure_barrel_food.yml` exists with:
- `items.common` / `items.rare` / `items.epic` populated
- `mean: 1.0`
- `standardDeviation: 0.7`

If `mean` or `standardDeviation` come out as `4.0` / `3.0`, Task 2's refactor wasn't applied correctly.

**Step 3: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/config/treasures/premade/BarrelFoodTreasureConfig.java
git commit -m "feat(lootify): ship treasure_barrel_food premade (mean=1, stddev=0.7)"
```

---

## Task 4: Add barrel fields to `GeneratorConfigFields`

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/config/generators/GeneratorConfigFields.java`

**Step 1: Add the fields**

After the existing `treasureFilename` / `chestContents` declarations (around lines 41-43), add:

```java
@Getter
@Setter
private String barrelTreasureFilename = "treasure_barrel_food";
@Getter
private ChestContents barrelContents = null;
@Getter
@Setter
private boolean generateLootInBarrels = true;
```

**Step 2: Load them during `processConfigFields()`**

After the existing chest-treasure load (around lines 80-86), append:

```java
// Per-generator barrel loot toggle (default ON)
this.generateLootInBarrels = processBoolean("generateLootInBarrels", generateLootInBarrels, true, false);

// Load barrel treasure config (defaults to the food premade)
this.barrelTreasureFilename = processString("barrelTreasureFilename", barrelTreasureFilename, "treasure_barrel_food", false);
if (generateLootInBarrels) {
    TreasureConfigFields barrelTreasureConfig = TreasureConfig.getConfigFields(barrelTreasureFilename);
    if (barrelTreasureConfig != null) {
        this.barrelContents = new ChestContents(barrelTreasureConfig);
    } else {
        Logger.warn("No valid barrel treasure config found for generator " + filename + " (looked for: " + barrelTreasureFilename + "). Barrels in this generator will be left empty until fixed.");
    }
}
```

**Step 3: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/config/generators/GeneratorConfigFields.java
git commit -m "feat(lootify): generator-level barrel loot config (default on)"
```

---

## Task 5: Add barrel fields to `ModulesConfigFields`

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/config/modules/ModulesConfigFields.java`

**Step 1: Mirror Task 4's field additions**

Same three fields, same defaults. Add Lombok getters (and setter for the string + boolean), wire through `processConfigFields()` the same way.

**Step 2: Add a getter to make the ModulePasting flow read it**

Confirm `ModulesConfigFields` exposes `getBarrelTreasureFilename()`, `getBarrelContents()`, and `isGenerateLootInBarrels()` — Lombok `@Getter` produces these. ModulePasting will read these in Task 8.

**Step 3: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/config/modules/ModulesConfigFields.java
git commit -m "feat(lootify): module-level barrel loot config (default on)"
```

---

## Task 6: Add per-schematic barrel treasure override to `SchematicConfigField`

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/config/schematics/SchematicConfigField.java`

**Step 1: Read the file**

Find the `chestContents` field (line ~35) and the treasure-file load block (lines ~60-67).

**Step 2: Add mirrored barrel fields**

Right after `private ChestContents chestContents = null;`, add:

```java
@Getter
private ChestContents barrelContents = null;
@Getter
@Setter
private String barrelTreasureFilename = null;
```

No `generateLootInBarrels` here — that decision is generator-/module-level. Per-schematic override is treasure-file only, mirroring the existing chest-side override.

**Step 3: Wire the load path**

Where `this.chestContents = generatorConfigFields.getChestContents();` lives (line ~60), also inherit:

```java
this.barrelContents = generatorConfigFields.getBarrelContents();
```

Inside the `treasureConfigFields != null` block (line ~67), parallel to the chest override, look for `barrelTreasureFilename` in the YAML. If present and it resolves to a valid `TreasureConfigFields`, replace `barrelContents` with `new ChestContents(thatConfig)`.

**Step 4: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

**Step 5: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/config/schematics/SchematicConfigField.java
git commit -m "feat(lootify): per-schematic barrelTreasureFilename override"
```

---

## Task 7: Inherent barrel detection + chest/barrel routing in the schematic pipeline

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/schematics/SchematicContainer.java`
- Modify: `src/main/java/com/magmaguy/betterstructures/buildingfitter/FitAnything.java`

**Step 1: Widen the chest-location collector in `SchematicContainer.java`**

At line 74-77, change:

```java
if (minecraftMaterial.equals(Material.CHEST) ||
        minecraftMaterial.equals(Material.TRAPPED_CHEST) ||
        minecraftMaterial.equals(Material.SHULKER_BOX)) {
    chestLocations.add(new Vector(x, y, z));
}
```

to:

```java
if (minecraftMaterial.equals(Material.CHEST) ||
        minecraftMaterial.equals(Material.TRAPPED_CHEST) ||
        minecraftMaterial.equals(Material.SHULKER_BOX) ||
        minecraftMaterial.equals(Material.BARREL)) {
    chestLocations.add(new Vector(x, y, z));
}
```

(`chestLocations` now technically means "loot-bearing container locations." Don't rename — too much surface area for what should be a small change.)

**Step 2: Route chest vs barrel in `FitAnything.fillChests()`**

Replace the existing `fillChests()` (lines ~290-315) with:

```java
private void fillChests() {
    GeneratorConfigFields gen = schematicContainer.getGeneratorConfigFields();
    boolean barrelsEnabled = gen.isGenerateLootInBarrels() && gen.getBarrelContents() != null;
    boolean chestsEnabled = gen.getChestContents() != null;
    if (!barrelsEnabled && !chestsEnabled) return;

    for (Vector chestPosition : schematicContainer.getChestLocations()) {
        Location chestLocation = LocationProjector.project(location, schematicOffset, chestPosition);
        if (!(chestLocation.getBlock().getState() instanceof Container container)) {
            Logger.warn("Expected a container for " + chestLocation.getBlock().getType() + " but didn't get it. Skipping this loot!");
            continue;
        }

        boolean isBarrel = container.getBlock().getType() == Material.BARREL;
        if (isBarrel && !barrelsEnabled) continue;
        if (!isBarrel && !chestsEnabled) continue;

        ChestContents contents;
        String treasureFilename;
        if (isBarrel) {
            contents = schematicContainer.getBarrelContents() != null
                    ? schematicContainer.getBarrelContents()
                    : gen.getBarrelContents();
            treasureFilename = schematicContainer.getSchematicConfigField().getBarrelTreasureFilename() != null
                    ? schematicContainer.getSchematicConfigField().getBarrelTreasureFilename()
                    : gen.getBarrelTreasureFilename();
        } else {
            contents = schematicContainer.getChestContents() != null
                    ? schematicContainer.getChestContents()
                    : gen.getChestContents();
            treasureFilename = schematicContainer.getChestContents() != null
                    ? schematicContainer.getSchematicConfigField().getTreasureFile()
                    : gen.getTreasureFilename();
        }

        if (contents == null) continue;
        contents.rollChestContents(container);

        ChestFillEvent chestFillEvent = new ChestFillEvent(container, treasureFilename);
        Bukkit.getServer().getPluginManager().callEvent(chestFillEvent);
        if (!chestFillEvent.isCancelled()) {
            container.update(true);
        }
    }
}
```

**Step 3: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL. If `getBarrelContents()` / `isGenerateLootInBarrels()` don't resolve, double-check Task 4 declared those fields with Lombok `@Getter` (note: `boolean` getters are `isX()` not `getX()`).

**Step 4: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/schematics/SchematicContainer.java src/main/java/com/magmaguy/betterstructures/buildingfitter/FitAnything.java
git commit -m "feat(lootify): inherent barrel detection + routing in schematic pipeline"
```

---

## Task 8: Inherent barrel detection + fill in `ModulePasting`

**Files:**
- Modify: `src/main/java/com/magmaguy/betterstructures/modules/ModulePasting.java`

**Step 1: Allow `BARREL` to bypass NBT-rich deferral**

Around line 91:

```java
if (m == Material.CHEST || m == Material.TRAPPED_CHEST) return false;
```

becomes:

```java
if (m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL) return false;
```

**Step 2: Track barrel placements during paste**

Inside the same `pasteableList.forEach(...)` loop that already special-cases signs (around lines 190-262), add a check: when the block being placed is a barrel, record its world location in a new `List<Location> barrelsToFill` (declare it alongside `chestsToPlace` at the top of `batchPaste`).

Locate where `pasteableList.add(new Pasteable(pasteLocation, blockData))` is called (around line 261). Just before that line, add:

```java
if (blockData.getMaterial() == Material.BARREL) {
    barrelsToFill.add(pasteLocation);
}
```

Do NOT skip the normal paste path — the barrel still needs to be placed via `pasteableList` so its block data (orientation, etc.) is set correctly. The list just remembers where it ended up for the post-paste fill step.

**Step 3: Fill barrels after paste**

Find the existing chest-fill loop (lines ~386-405). After that loop, add a parallel loop for barrels:

```java
if (moduleGeneratorsConfigFields.isGenerateLootInBarrels()) {
    String barrelTreasureFilename = moduleGeneratorsConfigFields.getBarrelTreasureFilename();
    TreasureConfigFields barrelTreasureFields = TreasureConfig.getConfigFields(barrelTreasureFilename);
    if (barrelTreasureFields != null) {
        ChestContents barrelContents = new ChestContents(barrelTreasureFields);
        for (Location barrelLocation : barrelsToFill) {
            Block block = barrelLocation.getBlock();
            if (block.getType() != Material.BARREL) continue;  // got overwritten somehow
            if (!(block.getState() instanceof Container container)) continue;

            barrelContents.rollChestContents(container);
            ChestFillEvent chestFillEvent = new ChestFillEvent(container, barrelTreasureFilename);
            Bukkit.getServer().getPluginManager().callEvent(chestFillEvent);
            if (!chestFillEvent.isCancelled()) {
                container.update(true);
            }
        }
    } else if (!barrelsToFill.isEmpty()) {
        Logger.warn("Module generator " + moduleGeneratorsConfigFields.getFilename() + " has barrels in its modules but barrelTreasureFilename '" + barrelTreasureFilename + "' did not resolve to a valid treasure config. Barrels will be empty.");
    }
}
```

(Same `new ChestContents(treasureFields)` per-paste construction as the existing chest path on line 396 — not lazy-load-violating because it's a batch paste operation, not per-tick.)

**Step 4: Build check**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

**Step 5: Commit**

```bash
git add src/main/java/com/magmaguy/betterstructures/modules/ModulePasting.java
git commit -m "feat(lootify): inherent barrel detection + fill in module pipeline"
```

---

## Task 9: Full build + testbed verification

**Files:** none modified — verification only.

**Step 1: Full plugin build**

Per [feedback_full_builds.md](../../../../../.claude/projects/C--Users-tiago-Documents-MineCraftProjects/memory/feedback_full_builds.md), produce a usable jar.

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL, jar at `build/libs/BetterStructures-*.jar`.

**Step 2: Deploy + start the server once**

Use the testbed setup ([reference_testbed_setup.md](../../../../../.claude/projects/C--Users-tiago-Documents-MineCraftProjects/memory/reference_testbed_setup.md)). Start the server once so the new `treasure_barrel_food.yml` writes, then stop it and confirm:
- File exists at `plugins/BetterStructures/treasures/treasure_barrel_food.yml`
- Has three rarity tiers
- `mean: 1.0`, `standardDeviation: 0.7`

**Step 3: Verify the schematic pathway**

1. Place a barrel inside a test schematic used by a known generator.
2. Trigger a structure paste.
3. Open the placed barrel.

Expected: 1-3 items, all food, drawn from the table in Task 1. Over ~10 paste runs you should see mostly common-tier items, occasional cooked food, rare epic items.

**Step 4: Verify the module pathway**

1. Place a barrel inside a module schematic (no sign needed).
2. Trigger module pasting.
3. Open the placed barrel.

Expected: same behavior as Step 3.

**Step 5: Verify the per-generator opt-out**

1. In `plugins/BetterStructures/generators/<some_generator>.yml`, set `generateLootInBarrels: false`.
2. Reload (or restart).
3. Trigger a paste with a barrel.

Expected: barrel is placed but empty.

**Step 6: Verify the per-generator treasure override**

1. Reset that generator's `generateLootInBarrels` to default (or remove the key).
2. Set `barrelTreasureFilename: treasure_overworld_surface`.
3. Trigger a paste with a barrel.

Expected: barrel now contains overworld-chest loot (gear, etc.) — proving the override path works end-to-end.

**Step 7: Verify the per-schematic treasure override**

1. Restore `barrelTreasureFilename` to default in the generator.
2. In the schematic config, set `barrelTreasureFilename: treasure_overworld_surface`.
3. Trigger a paste.

Expected: that schematic's barrels carry overworld loot, other schematics in the same generator still carry food.

**Step 8: Verify `ChestFillEvent` fires for barrels**

Optional sanity check: add a temporary `Logger.info` to a `ChestFillEvent` consumer (or write a tiny listener plugin), confirm the event fires with `container.getBlock().getType() == BARREL` and the right `getTreasureConfigFilename()`.

**Step 9: Commit any tweaks**

If verification surfaces real bugs, fix them with focused commits. Don't bundle into the earlier feature commits.

---

## Notes for the executing engineer

- **DRY:** `ChestContents` is reused, not duplicated. Resist the urge to make a `BarrelContents` class.
- **YAGNI:** No `BarrelFillEvent`. `ChestFillEvent` is already container-generic; consumers can branch on `getContainer().getBlock().getType()`. No `[barrel]` sign marker — barrels are inherent.
- **TDD:** This codebase doesn't have unit-test coverage for the chest/loot pipeline (verify: `grep -r "rollChestContents" src/test`). Verification is manual on the testbed — own that explicitly per [feedback_full_builds.md](../../../../../.claude/projects/C--Users-tiago-Documents-MineCraftProjects/memory/feedback_full_builds.md). If you want one unit test, the cheapest valuable one: instantiate `ChestContents` with a synthetic `TreasureConfigFields`, hand it a mock `Container`, assert `rollChestContents` populates the inventory with 1-3 items when `mean=1`/`stddev=0.7`. Don't gate this PR on it.
- **Lazy loading:** Per [feedback_lazy_loading.md](../../../../../.claude/projects/C--Users-tiago-Documents-MineCraftProjects/memory/feedback_lazy_loading.md), the cached path uses one `ChestContents` per generator (built at config load in Task 4). The modules path constructs per-batch (Task 8 step 3) — that matches the existing chest behavior at `ModulePasting:396`, so it's not a regression.
- **Magmacore:** This plan touches only BetterStructures internals. No Magmacore changes, so [reference_magmacore_publish_workflow.md](../../../../../.claude/projects/C--Users-tiago-Documents-MineCraftProjects/memory/reference_magmacore_publish_workflow.md) does not apply.
- **Commits:** One per task. Branch name suggestion: `feat/barrel-loot`.
