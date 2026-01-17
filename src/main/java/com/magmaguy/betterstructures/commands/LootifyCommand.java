package com.magmaguy.betterstructures.commands;

import com.magmaguy.betterstructures.config.treasures.TreasureConfig;
import com.magmaguy.betterstructures.config.treasures.TreasureConfigFields;
import com.magmaguy.betterstructures.util.ItemStackSerialization;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.arguments.IntegerCommandArgument;
import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootifyCommand extends AdvancedCommand {
    public LootifyCommand() {
        super(List.of("lootify"));
        ArrayList<String> treasures = new ArrayList<>(TreasureConfig.getTreasureConfigurations().keySet());
        addArgument("generator", new ListStringCommandArgument(treasures,"<treasures>"));
        addArgument("rarity", new ListStringCommandArgument("<rarity>"));
        addArgument("minAmount", new IntegerCommandArgument("<minAmount>"));
        addArgument("maxAmount", new IntegerCommandArgument("<maxAmount>"));
        addArgument("weight", new IntegerCommandArgument("<weight>"));
        setPermission("betterstructures.*");
        setUsage("/betterstructures lootify <generator> <rarity> <minAmount> <maxAmount> <weight>");
        setDescription("Adds a held item to the loot settings of a generator");
    }

    @Override
    public void execute(CommandData commandData) {
        lootify(commandData.getStringArgument("generator"),
                commandData.getStringArgument("rarity"),
                commandData.getStringArgument("minAmount"),
                commandData.getStringArgument("maxAmount"),
                commandData.getStringArgument("weight"),
                commandData.getPlayerSender());
    }
    private void lootify(String generator, String rarity, String minAmount, String maxAmount, String weight, Player player) {
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
        double weightDouble;
        try {
            weightDouble = Double.parseDouble(weight);
        } catch (Exception exception) {
            player.sendMessage("[BetterStructures] Not a valid weight! Try again.");
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
        else if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasItemName())
            info = itemStack.getItemMeta().getItemName();
        else
            info = itemStack.getType().toString();
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("serialized", ItemStackSerialization.deserializeItem(itemStack));
        configMap.put("amount", minAmount +"-"+maxAmount);
        configMap.put("weight", weightDouble);
        configMap.put("info", info);
        treasureConfigFields.addChestEntry(configMap, rarity, player);
    }
}
