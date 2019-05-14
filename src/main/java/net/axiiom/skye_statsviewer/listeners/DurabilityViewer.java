package net.axiiom.skye_statsviewer.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.axiiom.skye_statsviewer.main.StatsViewer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class DurabilityViewer implements Listener {
    private StatsViewer plugin;
    private ConcurrentHashMap<UUID, BossBar> durability;

    public DurabilityViewer(StatsViewer _plugin) {
        this.plugin = _plugin;
        this.durability = new ConcurrentHashMap<>();

        new ConstantItemChecker().runTaskTimer(plugin, 0L, 5L);
    }

    @EventHandler
    public synchronized void onSwitchToTool(PlayerItemHeldEvent _event) {
        Player player = _event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(_event.getNewSlot());
        ItemStack oldItem = player.getInventory().getItem(_event.getPreviousSlot());
        if (this.isTool(oldItem) && this.durability.containsKey(player.getUniqueId())) {
            this.durability.get(player.getUniqueId()).removePlayer(player);
            this.durability.remove(player.getUniqueId());
        }

        if (this.isTool(newItem)) {
            this.updateDurability(player, newItem);
        }

    }

    @EventHandler
    public synchronized void onPickup(EntityPickupItemEvent _event) {
        if (_event.getEntity() instanceof Player) {
            Player player = (Player)_event.getEntity();
            (new DurabilityViewer.PickupItemLater(player, _event.getItem().getItemStack())).runTaskLater(this.plugin, 2L);
        }
    }

    @EventHandler
    public synchronized void onSwapHands(PlayerSwapHandItemsEvent _event) {
        Player player = _event.getPlayer();

        if (this.isTool(_event.getMainHandItem())) {
            this.updateDurability(player, _event.getMainHandItem());
        }

    }

    @EventHandler
    public synchronized void onMending(PlayerItemMendEvent _event) {
        Player player = _event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack mendedItem = _event.getItem();
        if (mendedItem.equals(heldItem) && this.isTool(mendedItem)) {
            DurabilityViewer.UpdateDurabilityRunnable udr = new DurabilityViewer.UpdateDurabilityRunnable(player, heldItem);
            udr.runTaskLater(this.plugin, 2L);
        }

    }

    @EventHandler
    public synchronized void onItemDamage(PlayerItemDamageEvent _event) {
        ItemStack damagedItem = _event.getItem();
        Player player = _event.getPlayer();
        if (isTool(damagedItem)) {
            DurabilityViewer.UpdateDurabilityRunnable udr = new DurabilityViewer.UpdateDurabilityRunnable(player, damagedItem);
            udr.runTaskLater(this.plugin, 1L);
        }

    }

    private synchronized void updateDurability(Player _player, ItemStack _item) {
        int maxDurability = _item.getType().getMaxDurability();
        int durability = maxDurability - ((Damageable)_item.getItemMeta()).getDamage();
        double progress = (double)durability / (double)maxDurability;
        if (this.durability.containsKey(_player.getUniqueId())) {
            this.durability.get(_player.getUniqueId()).removePlayer(_player);
        }

        if (progress >= 0.0D) {
            String title = "Durability: " + durability + "/" + maxDurability;
            BossBar durabilityBar = Bukkit.createBossBar(title, BarColor.GREEN, BarStyle.SOLID, new BarFlag[0]);
            durabilityBar.setProgress(progress);
            durabilityBar.addPlayer(_player);
            if (progress <= 0.33D && progress > 0.1D) {
                durabilityBar.setColor(BarColor.YELLOW);
            } else if (progress <= 0.1D) {
                durabilityBar.setColor(BarColor.RED);
            }

            this.durability.put(_player.getUniqueId(), durabilityBar);
        }

    }

    private boolean isTool(ItemStack _tool) {
        if (_tool == null) {
            return false;
        } else {
            switch(_tool.getType()) {
                case WOODEN_AXE:
                case WOODEN_HOE:
                case WOODEN_PICKAXE:
                case WOODEN_SHOVEL:
                case WOODEN_SWORD:
                case STONE_AXE:
                case STONE_HOE:
                case STONE_PICKAXE:
                case STONE_SHOVEL:
                case STONE_SWORD:
                case IRON_AXE:
                case IRON_HOE:
                case IRON_PICKAXE:
                case IRON_SHOVEL:
                case IRON_SWORD:
                case GOLDEN_AXE:
                case GOLDEN_HOE:
                case GOLDEN_PICKAXE:
                case GOLDEN_SHOVEL:
                case GOLDEN_SWORD:
                case DIAMOND_AXE:
                case DIAMOND_HOE:
                case DIAMOND_PICKAXE:
                case DIAMOND_SHOVEL:
                case DIAMOND_SWORD:
                case BOW:
                case FISHING_ROD:
                case SHEARS:
                case TRIDENT:
                    return true;
                default:
                    return false;
            }
        }
    }

    class PickupItemLater extends BukkitRunnable {
        private Player player;
        private ItemStack item;

        public PickupItemLater(Player _player, ItemStack _item) {
            this.player = _player;
            this.item = _item;
        }

        public void run() {
            if (DurabilityViewer.this.isTool(this.item) && this.player.getInventory().getItemInMainHand().equals(this.item)) {
                DurabilityViewer.this.updateDurability(this.player, this.item);
            }

        }
    }

    class UpdateDurabilityRunnable extends BukkitRunnable {
        private Player player;
        private ItemStack item;

        public UpdateDurabilityRunnable(Player _player, ItemStack _item) {
            this.player = _player;
            this.item = _item;
        }

        public void run() {
            DurabilityViewer.this.updateDurability(this.player, this.item);

            if(item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                List<String> lore = null;

                String dur = ChatColor.GRAY + "" + ChatColor.ITALIC + "" + ChatColor.UNDERLINE +  "Durability: "
                        + (item.getType().getMaxDurability() - ((Damageable) item.getItemMeta()).getDamage())
                        + "/" + item.getType().getMaxDurability();

                if(meta.hasLore())
                    lore = meta.getLore();
                else
                    lore = new ArrayList<>();

                boolean found = false;
                for(int i = 0; i < lore.size() && !found; i++)
                {
                    if(lore.get(i).contains("Durability: ")) {
                        lore.set(i, dur);
                        found = true;
                    }
                }

                if(!found)
                    lore.add(dur);

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
    }

    class ConstantItemChecker extends BukkitRunnable {
        public void run() {
           for(UUID playerUuid : durability.keySet())
           {
               Player player = Bukkit.getPlayer(playerUuid);
               if(player != null && player.isOnline() && !isTool(player.getInventory().getItemInMainHand())) {
                    durability.get(playerUuid).removePlayer(player);
                    durability.remove(playerUuid);
               } else if (player == null || !player.isOnline()) {
                   durability.remove(playerUuid);
               }
           }
        }
    }
}
