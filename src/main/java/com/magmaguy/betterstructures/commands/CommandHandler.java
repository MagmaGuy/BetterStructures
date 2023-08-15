package com.magmaguy.betterstructures.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import com.magmaguy.betterstructures.MetadataHandler;
import com.magmaguy.betterstructures.buildingfitter.FitAnything;
import com.magmaguy.betterstructures.config.DefaultConfig;
import com.magmaguy.betterstructures.config.generators.GeneratorConfigFields;
import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.ChatColorConverter;
import com.magmaguy.betterstructures.util.ItemStackSerialization;
import com.magmaguy.betterstructures.util.WarningMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.text;

public class CommandHandler {
    private BukkitCommandManager<CommandSender> manager;
    private MinecraftHelp<CommandSender> minecraftHelp;
    private BukkitAudiences bukkitAudiences;

    /*
    Commands powered by Cloud
     */

    public CommandHandler() {
        Function<CommandTree, CommandExecutionCoordinator> commandExecutionCoordinator = null;
        try {
            Class<?> c = Class.forName("cloud.commandframework.execution.CommandExecutionCoordinator");
            Method method = c.getDeclaredMethod("simpleCoordinator");
            commandExecutionCoordinator = (Function<CommandTree, CommandExecutionCoordinator>) method.invoke(Function.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            manager = new BukkitCommandManager(
                    /* Owning plugin */ MetadataHandler.PLUGIN,
                    /* Coordinator function */ commandExecutionCoordinator,
                    /* Command Sender -> C */ Function.identity(),
                    /* C -> Command Sender */ Function.identity()
            );
        } catch (final Exception e) {
            new WarningMessage("Failed to initialize the command manager");
            /* Disable the plugin */
            MetadataHandler.PLUGIN.getServer().getPluginManager().disablePlugin(MetadataHandler.PLUGIN);
            return;
        }

        // Create a BukkitAudiences instance (adventure) in order to use the minecraft-extras help system
        bukkitAudiences = BukkitAudiences.create(MetadataHandler.PLUGIN);

        minecraftHelp = new MinecraftHelp<CommandSender>(
                "/betterstructures help",
                bukkitAudiences::sender,
                manager
        );

        // Override the default exception handlers
        new MinecraftExceptionHandler<CommandSender>()
                .withInvalidSyntaxHandler()
                .withInvalidSenderHandler()
                .withNoPermissionHandler()
                .withArgumentParsingHandler()
                .withCommandExecutionHandler()
                .withDecorator(
                        component -> text()
                                .append(text("[", NamedTextColor.DARK_GRAY))
                                .append(text("Example", NamedTextColor.GOLD))
                                .append(text("] ", NamedTextColor.DARK_GRAY))
                                .append(component).build()
                ).apply(manager, bukkitAudiences::sender);

        constructCommands();
    }

    public void constructCommands() {
        // Base command builder
        final Command.Builder<CommandSender> builder = manager.commandBuilder("betterstructures", "bs");

        manager.command(builder.literal("help")
                .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
                .handler(context -> {
                    minecraftHelp.queryCommands(context.getOrDefault("query", ""), context.getSender());
                }));

        ArrayList<String> loadedSchematics = new ArrayList<>();
        SchematicContainer.getSchematics().values().forEach(schematicContainer -> loadedSchematics.add(schematicContainer.getClipboardFilename()));
        ArrayList<String> schematictypes = new ArrayList<>(Arrays.asList(GeneratorConfigFields.StructureType.SURFACE.toString(), GeneratorConfigFields.StructureType.UNDERGROUND_SHALLOW.toString(), GeneratorConfigFields.StructureType.UNDERGROUND_DEEP.toString(), GeneratorConfigFields.StructureType.SKY.toString(), GeneratorConfigFields.StructureType.LIQUID_SURFACE.toString()));
        // /bs place <schematic>
        manager.command(builder.literal("place")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("schematic")
                                .withSuggestionsProvider(((objectCommandContext, s) -> loadedSchematics)),
                        ArgumentDescription.of("File name of the schematic"))
                .argument(StringArgument.<CommandSender>newBuilder("schematicType")
                                .withSuggestionsProvider(((objectCommandContext, s) -> schematictypes)),
                        ArgumentDescription.of("Type of schematic placement"))
                .meta(CommandMeta.DESCRIPTION, "Places the specified schematic at the target location")
                .permission("betterstructures.*")
                .handler(commandContext -> {
                    placeSchematic(commandContext.get("schematic"), commandContext.get("schematicType"), (Player) commandContext.getSender());
                }));

        // /em reload
        manager.command(builder.literal("reload")
                .meta(CommandMeta.DESCRIPTION, "Reloads the plugin. Works almost every time.")
                .senderType(CommandSender.class)
                .permission("betterstructures.*")
                .handler(commandContext -> {
                    MetadataHandler.PLUGIN.onDisable();
                    MetadataHandler.PLUGIN.onLoad();
                    MetadataHandler.PLUGIN.onEnable();
                    commandContext.getSender().sendMessage("[BetterStructures] Reload attempted. This may not 100% work. Restart instead if it didn't!!");
                }));

        ArrayList<String> treasures = new ArrayList<>(TreasureConfig.getTreasureConfigurations().keySet());

        manager.command(builder.literal("lootify")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("generator")
                                .withSuggestionsProvider(((objectCommandContext, s) -> treasures)),
                        ArgumentDescription.of("File name of the generator"))
                .argument(StringArgument.<CommandSender>newBuilder("rarity"),
                        ArgumentDescription.of("Name of the rarity in the loot table"))
                .meta(CommandMeta.DESCRIPTION, "Adds a held item to the loot settings of a generator")
                .argument(StringArgument.<CommandSender>newBuilder("minAmount"))
                .argument(StringArgument.<CommandSender>newBuilder("maxAmount"))
                .argument(StringArgument.<CommandSender>newBuilder("chance"))
                .permission("betterstructures.*")
                .handler(commandContext -> {
                    lootify(commandContext.get("generator"), commandContext.get("rarity"),commandContext.get("minAmount"), commandContext.get("maxAmount"), commandContext.get("chance"), (Player) commandContext.getSender());
                }));

        manager.command(builder.literal("teleporttocoords")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("worldname"))
                .argument(StringArgument.<CommandSender>newBuilder("x"))
                .argument(StringArgument.<CommandSender>newBuilder("y"))
                .argument(StringArgument.<CommandSender>newBuilder("z"))
                .permission("betterstructures.*")
                .handler(commandContext -> {
                    try {
                        World world = Bukkit.getWorld((String) commandContext.get("worldname"));
                        double x = Double.parseDouble(commandContext.get("x"));
                        double y = Double.parseDouble(commandContext.get("y"));
                        double z = Double.parseDouble(commandContext.get("z"));
                        ((Player) commandContext.getSender()).teleport(new Location(world, x, y, z));
                    } catch (Exception ex) {
                        commandContext.getSender().sendMessage("[BetterStructures] Failed to teleport to location because the location wasn't valid!");
                    }
                })
        );
        // /bs version
        manager.command(builder.literal("version")
                .meta(CommandMeta.DESCRIPTION, "Gets the version of the plugin")
                .senderType(CommandSender.class)
                .permission("betterstructures.version")
                .handler(commandContext ->
                        commandContext.getSender().sendMessage(
                                ChatColorConverter.convert("&8[BetterStructures] &aVersion " +
                                        Bukkit.getPluginManager().getPlugin(
                                                MetadataHandler.PLUGIN.getName()).getDescription().getVersion()))));
        // /bs silent
        manager.command(builder.literal("silent")
                .meta(CommandMeta.DESCRIPTION, "Silences warning admins about new builds")
                .senderType(CommandSender.class)
                .permission("betterstructures.*")
                .handler(commandContext -> {
                    commandContext.getSender().sendMessage(
                            ChatColorConverter.convert("&8[BetterStructures] &2Toggled build warnings to " + DefaultConfig.toggleWarnings() + "!"));
                }));
    }

    private void placeSchematic(String schematicFile, String schematicType, Player player) {
        try {
            SchematicContainer commandSchematicContainer = null;
            for (SchematicContainer schematicContainer : SchematicContainer.getSchematics().values())
                if (schematicContainer.getClipboardFilename().equals(schematicFile)) {
                    commandSchematicContainer = schematicContainer;
                    break;
                }
            if (commandSchematicContainer == null) {
                player.sendMessage("[BetterStructures] Invalid schematic!");
                return;
            }
            GeneratorConfigFields.StructureType structureType;
            try {
                structureType = GeneratorConfigFields.StructureType.valueOf(schematicType);
            } catch (Exception exception) {
                player.sendMessage("[BetterStructures] Failed to get valid schematic type!");
                return;
            }
            FitAnything.commandBasedCreation(player.getLocation().getChunk(), structureType, commandSchematicContainer);
            //new FitSurfaceBuilding(player.getLocation().getChunk(), commandSchematicContainer);
            player.sendMessage("[BetterStructures] Attempted to place " + schematicFile + " !");
        } catch (Exception ex) {
            player.sendMessage("[BetterStructures] Invalid schematic!");
        }
    }

    private void lootify(String generator, String rarity, String minAmount, String maxAmount, String chance, Player player) {
        TreasureConfigFields treasureConfigFields = TreasureConfig.getConfigFields(generator);
        if (treasureConfigFields == null) {
            player.sendMessage("[BetterStructures] Not a valid generator! Try again.");
            return;
        }
        //Verify loot table
        if (treasureConfigFields.getRawLoot().get(rarity) == null) {
            player.sendMessage("[BetterStructures] Not a valid rarity! Try again.");
            return;
        }
        int minAmountInt;
        try {
            minAmountInt = Integer.parseInt(minAmount);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Not a valid minimum amount! Try again.");
            return;
        }
        if (minAmountInt < 1) {
            player.sendMessage("[BetterStructures] Minimum amount should not be less than 1! This value will not be saved.");
            return;
        }
        int maxAmountInt;
        try {
            maxAmountInt = Integer.parseInt(maxAmount);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Not a valid maximum amount! Try again.");
            return;
        }
        if (maxAmountInt > 64) {
            player.sendMessage("[BetterStructures] Maximum amount should not be more than 64! If you want more than one stack, make multiple entries. This value will not be saved.");
            return;
        }
        double chanceDouble;
        try {
            chanceDouble = Double.parseDouble(chance);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Not a valid chance! Try again.");
            return;
        }
        if (chanceDouble > 1) {
            player.sendMessage("[BetterStructures] Chance should never be higher than 1.0! 1.0 is 100%, 0.0 is 0%, 0.5 is 50%! This value will not be saved.");
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType().isAir()) {
            player.sendMessage("[BetterStructures] You need to be holding an item in order to register the item you're holding! This value will not be saved.");
            return;
        }
        String info;
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName())
            info = itemStack.getItemMeta().getDisplayName().replace(" ", "_");
        else if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasLocalizedName())
            info = itemStack.getItemMeta().getLocalizedName();
        else
            info = itemStack.getType().toString();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("serialized", ItemStackSerialization.toBase64(itemStack));
        configMap.put("amount", minAmount +"-"+maxAmount);
        configMap.put("weight", chanceDouble);
        configMap.put("info", info);
        treasureConfigFields.addChestEntry(configMap, rarity, player);
    }
}
