# BetterStructures

BetterStructures adds schematic structures and modular dungeons to Minecraft worlds. It places content on the surface, underground, in the sky, and on liquid surfaces, with generator rules for terrain, biomes, worlds, spacing, and loot.

[Download](https://nightbreak.io/plugin/betterstructures/) · [Modrinth](https://modrinth.com/plugin/betterstructures) · [Documentation](https://wiki.nightbreak.io/) · [Support](https://discord.gg/nightbreak)

## Features

- Generate standalone WorldEdit schematics or assemble larger builds from modules.
- Install and manage structure and module packs through the in-game setup menu.
- Configure chest and barrel loot, including per-schematic treasure overrides.
- Combine structures with EliteMobs or MythicMobs encounters when those integrations are installed.
- Control generation by world, environment, terrain, and generator configuration.
- Pregenerate terrain with configurable work limits and TPS-based pausing.

## Requirements and installation

Use Java 21 or the newer Java version required by your server, a compatible Spigot/Paper server, and **WorldEdit**. The plugin declares Minecraft API version `1.21.4`; check the download page for the supported server versions of your chosen release. MagmaCore is included in the JAR.

1. Install WorldEdit and put `BetterStructures.jar` in `plugins/`.
2. Start the server and run `/bs initialize` in-game as an administrator.
3. Select the worlds and content you want to use. `/bs setup` opens content management afterward.
4. Explore newly generated terrain to encounter structures. Installing a pack does not populate every existing chunk retroactively.

EliteMobs, MythicMobs, WorldGuard, and the world-generator integrations listed in [plugin.yml](src/main/resources/plugin.yml) are optional. A content pack can require an integration even when the base plugin does not.

## Commands and permissions

The main command is `/betterstructures`, with alias `/bs`. Use tab completion for installed schematic and generator names.

| Command | Purpose |
| --- | --- |
| `/bs initialize` | First-time setup and world selection. |
| `/bs setup` | Install and manage content packs. |
| `/bs reload` | Reload configuration and content. |
| `/bs place <schematic> <type>` | Place a schematic using a surface, sky, liquid-surface, or underground placement type. |
| `/bs generateModules <generator.yml>` | Generate a modular build in a dedicated world. |
| `/bs pregenerate <center> <shape> <radius> <applyWorldBorder>` | Pregenerate chunks; radius is in blocks. |
| `/bs cancelPregenerate` | Cancel pregeneration in the current world. |
| `/bs silent` | Toggle structure-generation notices for yourself. |
| `/bs version` | Show the installed version. |

Pregeneration centers are `HERE`, `WORLD_CENTER`, or `WORLD_SPAWN`; shapes are `SQUARE` or `CIRCLE`. The final argument is `TRUE` or `FALSE`.

Administrative commands use `betterstructures.*`, while setup and module-generation operations also expose specific permissions such as `betterstructures.setup`, `betterstructures.initialize`, and `betterstructures.generatemodules`. See [plugin.yml](src/main/resources/plugin.yml) and the [command implementations](src/main/java/com/magmaguy/betterstructures/commands) for individual checks.

## Configuration

Generated configuration lives under `plugins/BetterStructures/`. Start with the setup menu, then edit the generated files and their inline comments for world choices, structure generators, module generators, schematics, and treasure tables. Keep backups before changing generation on an established world.

The global settings include generation distances, administrator notices, dungeon protection, and the fraction of each tick used for pasting and pregeneration. Generator and schematic settings determine which content can spawn and how its loot is selected.

If nothing appears, check the selected world, installed packs, generator biome and environment restrictions, and whether you are exploring new terrain. Include the relevant generator and schematic configuration with support reports.

## Building and integration

Build with JDK 21 and the included Gradle wrapper:

```powershell
.\gradlew.bat shadowJar
```

On Unix, use `./gradlew shadowJar`. The deployable file is `build/libs/BetterStructures.jar`. Set `MC_DIST_DIR` to copy it to a shared artifact directory. When changing MagmaCore locally, publish the matching MagmaCore version to Maven Local before rebuilding.

The latest release in this checkout is `2.7.1`. Plugin integrations can resolve `com.magmaguy:BetterStructures:2.7.1` from [MagmaGuy's Maven repository](https://repo.magmaguy.com/releases), using Maven `provided` or Gradle `compileOnly` scope.

## License

See [LICENSE](LICENSE). Content packs have their own distribution terms.
