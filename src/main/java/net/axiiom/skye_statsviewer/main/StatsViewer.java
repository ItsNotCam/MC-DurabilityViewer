package net.axiiom.skye_statsviewer.main;

import net.axiiom.skye_statsviewer.listeners.DurabilityViewer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class StatsViewer extends JavaPlugin {
    DurabilityViewer durabilityViewer;

    public void onEnable() {
        this.durabilityViewer = new DurabilityViewer(this);
        this.getServer().getPluginManager().registerEvents(this.durabilityViewer, this);
        this.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[DurabilityViewer] Loaded Successfully!");
    }
}
