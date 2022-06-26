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
import com.magmaguy.betterstructures.buildingfitter.FitSurfaceBuilding;
import com.magmaguy.betterstructures.schematics.SchematicContainer;
import com.magmaguy.betterstructures.util.WarningMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

        // /bs place <schematic>
        manager.command(builder.literal("place")
                .senderType(Player.class)
                .argument(StringArgument.<CommandSender>newBuilder("schematic")
                                .withSuggestionsProvider(((objectCommandContext, s) -> loadedSchematics)),
                        ArgumentDescription.of("File name of the schematic"))
                .meta(CommandMeta.DESCRIPTION, "Places the specified schematic at the target location")
                .permission("betterstructures.*")
                .handler(commandContext -> {
                    placeSchematic(commandContext.get("schematic"), (Player) commandContext.getSender());
                }));
    }

    private void placeSchematic(String schematicFile, Player player) {
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

            new FitSurfaceBuilding(player.getLocation().getChunk(), commandSchematicContainer);
            player.sendMessage("[BetterStructures] Attempted to place " + schematicFile + " !");
        } catch (Exception ex) {
            player.sendMessage("[BetterStructures] Invalid schematic!");
        }
    }
}
