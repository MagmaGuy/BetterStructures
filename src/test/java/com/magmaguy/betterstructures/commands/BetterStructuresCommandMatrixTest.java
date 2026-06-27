package com.magmaguy.betterstructures.commands;

import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.command.CommandManager;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterStructuresCommandMatrixTest {
    private ServerMock server;
    private PluginMock plugin;
    private PluginCommand betterStructuresCommand;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        plugin = loadPluginWithCommandMetadata();
        resetMagmaCore();
        MagmaCore.createInstance(plugin);

        betterStructuresCommand = plugin.getCommand("betterstructures");
        assertNotNull(betterStructuresCommand);
    }

    @AfterEach
    void tearDown() throws Exception {
        CommandManager.shutdown();
        MagmaCore.shutdown(plugin);
        resetMagmaCore();
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void pluginYmlDeclaresRootCommandAliasAndAdminPermissions() {
        YamlConfiguration pluginYml = loadRealPluginYml();

        assertTrue(pluginYml.isConfigurationSection("commands.betterstructures"));
        assertEquals(List.of("bs"), pluginYml.getStringList("commands.betterstructures.aliases"));

        assertEquals("op", pluginYml.getString("permissions.betterstructures.setup.default"));
        assertEquals("op", pluginYml.getString("permissions.betterstructures.initialize.default"));
        assertEquals("op", pluginYml.getString("permissions.betterstructures.generatemodules.default"));
    }

    @Test
    void rootEntryCommandDisplaysSetupHelpForPermittedPlayers() {
        CommandManager commandManager = new CommandManager(plugin, "betterstructures");
        commandManager.registerCommand(new BetterStructuresCommand());

        PlayerMock player = server.addPlayer("SetupReader");
        player.addAttachment(plugin, "betterstructures.*", true);

        assertTrue(betterStructuresCommand.execute(player, "betterstructures", new String[]{}));
        assertMessageContains(player.nextMessage(), "BetterStructures is a plugin that adds random structures");
        assertMessageContains(player.nextMessage(), "/bs setup");
        assertMessageContains(player.nextMessage(), "/bs recommendedplugins");
        assertMessageContains(player.nextMessage(), "/bs downloadall");
    }

    @Test
    void setupAndInitializeCommandsStopAtSenderAndPermissionGatesBeforeOpeningMenus() {
        CommandManager commandManager = new CommandManager(plugin, "betterstructures");
        commandManager.registerCommand(new SetupCommand());
        commandManager.registerCommand(new FirstTimeSetupCommand());

        assertFalse(betterStructuresCommand.execute(server.getConsoleSender(), "betterstructures", new String[]{"setup"}));
        assertMessageContains(server.getConsoleSender().nextMessage(), "This command must be run as a player!");

        PlayerMock deniedSetupPlayer = server.addPlayer("DeniedSetup");
        deniedSetupPlayer.addAttachment(plugin, "betterstructures.setup", false);
        assertFalse(betterStructuresCommand.execute(deniedSetupPlayer, "betterstructures", new String[]{"setup"}));
        assertMessageContains(deniedSetupPlayer.nextMessage(), "You do not have permission to run this command!");

        PlayerMock deniedInitializePlayer = server.addPlayer("DeniedInitialize");
        deniedInitializePlayer.addAttachment(plugin, "betterstructures.initialize", false);
        assertFalse(betterStructuresCommand.execute(deniedInitializePlayer, "betterstructures", new String[]{"initialize"}));
        assertMessageContains(deniedInitializePlayer.nextMessage(), "You do not have permission to run this command!");
    }

    @Test
    void adminReloadCommandRequiresWildcardPermission() {
        CommandManager commandManager = new CommandManager(plugin, "betterstructures");
        commandManager.registerCommand(new ReloadCommand());

        PlayerMock deniedPlayer = server.addPlayer("DeniedReload");
        deniedPlayer.addAttachment(plugin, "betterstructures.*", false);

        assertFalse(betterStructuresCommand.execute(deniedPlayer, "betterstructures", new String[]{"reload"}));
        assertMessageContains(deniedPlayer.nextMessage(), "You do not have permission to run this command!");
    }

    @Test
    void commandRoutingReportsUnknownCommandsAndOffersSetupSuggestion() {
        CommandManager commandManager = new CommandManager(plugin, "betterstructures");
        commandManager.registerCommand(new SetupCommand());
        commandManager.registerCommand(new BetterStructuresCommand());

        PlayerMock player = server.addPlayer("Typo");
        player.addAttachment(plugin, "betterstructures.setup", true);
        player.addAttachment(plugin, "betterstructures.*", true);

        assertFalse(betterStructuresCommand.execute(player, "betterstructures", new String[]{"set"}));
        assertMessageContains(player.nextMessage(), "Unknown command! Did you mean one of the following?");
        assertMessageContains(player.nextMessage(), "/bs setup");
    }

    @Test
    void commandBodiesStopAtSafeValidationBranchesBeforePaperOnlyWork() {
        CommandManager commandManager = new CommandManager(plugin, "betterstructures");
        commandManager.registerCommand(new LootifyCommand());
        commandManager.registerCommand(new PlaceCommand());
        commandManager.registerCommand(new GenerateModulesCommand());
        commandManager.registerCommand(new PregenerateCommand());

        PlayerMock player = server.addPlayer("Validator");
        player.addAttachment(plugin, "betterstructures.*", true);
        player.addAttachment(plugin, "betterstructures.generatemodules", true);

        assertTrue(betterStructuresCommand.execute(player, "betterstructures",
                new String[]{"lootify", "missing_generator.yml", "common", "1", "1", "1"}));
        assertMessageContains(player.nextMessage(), "Not a valid generator");

        assertTrue(betterStructuresCommand.execute(player, "betterstructures",
                new String[]{"place", "missing.schem", "SURFACE"}));
        assertMessageContains(player.nextMessage(), "Invalid schematic");

        assertTrue(betterStructuresCommand.execute(player, "betterstructures",
                new String[]{"generateModules", "missing_module_generator.yml"}));
        assertMessageContains(player.nextMessage(), "File missing_module_generator.yml not found");

        assertTrue(betterStructuresCommand.execute(player, "betterstructures",
                new String[]{"pregenerate", "HERE", "SQUARE", "-1", "FALSE"}));
        assertMessageContains(player.nextMessage(), "Radius must be 0 or greater");
    }

    private static PluginMock loadPluginWithCommandMetadata() {
        String yaml = """
                name: BetterStructures
                version: 2.6.1
                main: org.mockbukkit.mockbukkit.plugin.PluginMock
                commands:
                  betterstructures:
                    aliases:
                      - bs
                permissions:
                  betterstructures.*:
                    default: op
                  betterstructures.setup:
                    default: op
                  betterstructures.initialize:
                    default: op
                """;
        return MockBukkit.loadWith(PluginMock.class, new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private static YamlConfiguration loadRealPluginYml() {
        YamlConfiguration configuration = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(
                BetterStructuresCommandMatrixTest.class.getResourceAsStream("/plugin.yml"),
                StandardCharsets.UTF_8)) {
            configuration.load(reader);
        } catch (Exception exception) {
            throw new AssertionError("Failed to load BetterStructures plugin.yml", exception);
        }
        return configuration;
    }

    private static void assertMessageContains(String message, String expected) {
        assertTrue(stripped(message).contains(expected), () -> "Expected message to contain: " + expected + ", got: " + message);
    }

    private static String stripped(String message) {
        return ChatColor.stripColor(message);
    }

    @SuppressWarnings("unchecked")
    private static void resetMagmaCore() throws Exception {
        Field instanceField = MagmaCore.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field registeredPluginsField = MagmaCore.class.getDeclaredField("registeredPlugins");
        registeredPluginsField.setAccessible(true);
        ((Map<String, ?>) registeredPluginsField.get(null)).clear();

        Field listenerRegistrationsField = MagmaCore.class.getDeclaredField("listenerRegistrations");
        listenerRegistrationsField.setAccessible(true);
        ((java.util.Set<String>) listenerRegistrationsField.get(null)).clear();
    }
}
