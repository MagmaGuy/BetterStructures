package com.magmaguy.betterstructures.modules;

import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class Messaging {
    private final BossBar completionPercentage;
    private static final HashSet<BossBar> bars = new HashSet<>();
    private long startTime;

    public Messaging(Player player){
        completionPercentage = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        completionPercentage.addPlayer(player);
        bars.add(completionPercentage);
        startTime = System.currentTimeMillis();
    }

    public void updateProgress(double progress, String message) {
        completionPercentage.setProgress(Math.max(Math.min(progress ,1d ),0));
        completionPercentage.setTitle(message);
    }

    public void clearBar() {
        bars.remove(completionPercentage);
        completionPercentage.removeAll();
    }

    public static void shutdown(){
        for (BossBar bar : bars) {
            bar.removeAll();
        }
        bars.clear();
    }

    public void timeMessage(String whatEnded, Player player) {
        // Calculate and display the elapsed time
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = elapsedTime / (1000 * 60 * 60);

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        if (player != null) {
            player.sendMessage(whatEnded + " completed! Time taken: " + timeString);
        }
        Logger.warn("Generation complete! Time taken: " + timeString);
    }
}
