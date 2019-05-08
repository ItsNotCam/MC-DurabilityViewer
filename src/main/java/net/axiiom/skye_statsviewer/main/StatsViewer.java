package net.axiiom.skye_statsviewer.main;

import net.axiiom.skye_statsviewer.listeners.DurabilityViewer;
import net.axiiom.skye_statsviewer.listeners.MobHealth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class StatsViewer extends JavaPlugin {
    private DurabilityViewer durabilityViewer;
    private MobHealth mobHealth;

    public StatsViewer() {
    }

    public void onEnable() {
        this.durabilityViewer = new DurabilityViewer(this);
        this.mobHealth = new MobHealth(this);
        this.getServer().getPluginManager().registerEvents(this.durabilityViewer, this);
        this.getServer().getPluginManager().registerEvents(this.mobHealth, this);
    }

    public boolean onCommand(CommandSender _sender, Command _command, String _label, String[] _args) {
        return false;
    }
}
