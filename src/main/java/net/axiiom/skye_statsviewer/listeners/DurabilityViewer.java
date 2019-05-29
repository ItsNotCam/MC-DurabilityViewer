package net.axiiom.skye_statsviewer.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.axiiom.skye_statsviewer.main.StatsViewer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
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
        if (this.isDamageable(oldItem) && this.durability.containsKey(player.getUniqueId())) {
            this.durability.get(player.getUniqueId()).removePlayer(player);
            this.durability.remove(player.getUniqueId());
        }

        if (this.isDamageable(newItem)) {
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

        if (this.isDamageable(_event.getMainHandItem())) {
            this.updateDurability(player, _event.getMainHandItem());
        }

    }

    @EventHandler
    public synchronized void onAnvilRepair(PrepareAnvilEvent _event)
    {
        ItemStack repairedItem = _event.getResult();

        if(repairedItem != null) {
            int maxDurability = repairedItem.getType().getMaxDurability();
            int durability = maxDurability;

            if(repairedItem.hasItemMeta() && ((Damageable) repairedItem.getItemMeta()).hasDamage())
                durability -= ((Damageable) repairedItem.getItemMeta()).getDamage();

            updateLoreDurability(repairedItem, durability, maxDurability);
        }
    }

    @EventHandler
    public synchronized void onMending(PlayerItemMendEvent _event) {
        if(_event.isCancelled())
            return;

        Player player = _event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack mendedItem = _event.getItem();

        if (mendedItem.equals(heldItem)) {
            DurabilityViewer.UpdateDurabilityRunnable udr = new DurabilityViewer.UpdateDurabilityRunnable(player, mendedItem);
            udr.runTaskLater(this.plugin, 1L);
        }

        int maxDurability = mendedItem.getType().getMaxDurability();
        int durability = maxDurability - ((Damageable)mendedItem.getItemMeta()).getDamage();
        int mendedBy = _event.getRepairAmount();

        durability += mendedBy;
        updateLoreDurability(mendedItem, durability, maxDurability);
    }

    @EventHandler
    public synchronized void onItemDamage(PlayerItemDamageEvent _event) {
        if(_event.isCancelled())
            return;

        ItemStack damagedItem = _event.getItem();
        Player player = _event.getPlayer();
        if (isDamageable(damagedItem) && player.getInventory().getItemInMainHand().equals(damagedItem)) {
            DurabilityViewer.UpdateDurabilityRunnable udr = new DurabilityViewer.UpdateDurabilityRunnable(player, damagedItem);
            udr.runTaskLater(this.plugin, 1L);
        }

        int maxDurability = damagedItem.getType().getMaxDurability();
        int damagedBy = _event.getDamage();

        int durability = maxDurability - ((Damageable)damagedItem.getItemMeta()).getDamage() - damagedBy;
        if(durability > maxDurability) durability = maxDurability;

        updateLoreDurability(damagedItem, durability, maxDurability);
    }

    private synchronized void updateLoreDurability(ItemStack _item, int durability, int _maxDurability)
    {
        if(_item.hasItemMeta()) {
            ItemMeta meta = _item.getItemMeta();
            List<String> lore = null;

            String dur = ChatColor.GRAY + "" + ChatColor.ITALIC + "" + ChatColor.DARK_AQUA +  "Durability: "
                    + durability + "/" + _maxDurability;

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
            _item.setItemMeta(meta);
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

    class PickupItemLater extends BukkitRunnable {
        private Player player;
        private ItemStack item;

        public PickupItemLater(Player _player, ItemStack _item) {
            this.player = _player;
            this.item = _item;
        }

        public void run() {
            if (isDamageable(this.item) && this.player.getInventory().getItemInMainHand().equals(this.item)) {
                updateDurability(this.player, this.item);
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
            updateDurability(this.player, this.item);
        }
    }

    class ConstantItemChecker extends BukkitRunnable {
        public void run() {
           for(UUID playerUuid : durability.keySet())
           {
               Player player = Bukkit.getPlayer(playerUuid);
               if(player != null && player.isOnline() && !isDamageable(player.getInventory().getItemInMainHand())) {
                    durability.get(playerUuid).removePlayer(player);
                    durability.remove(playerUuid);
               } else if (player == null || !player.isOnline()) {
                   durability.remove(playerUuid);
               }
           }
        }
    }

    private boolean isDamageable(ItemStack _damageable) {
        if (_damageable == null) {
            return false;
        } else {
            switch(_damageable.getType()) {
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
                case IRON_BOOTS:
                case IRON_CHESTPLATE:
                case IRON_LEGGINGS:
                case IRON_HELMET:

                case GOLDEN_AXE:
                case GOLDEN_HOE:
                case GOLDEN_PICKAXE:
                case GOLDEN_SHOVEL:
                case GOLDEN_SWORD:
                case GOLDEN_BOOTS:
                case GOLDEN_CHESTPLATE:
                case GOLDEN_LEGGINGS:
                case GOLDEN_HELMET:

                case DIAMOND_AXE:
                case DIAMOND_HOE:
                case DIAMOND_PICKAXE:
                case DIAMOND_SHOVEL:
                case DIAMOND_SWORD:
                case DIAMOND_BOOTS:
                case DIAMOND_CHESTPLATE:
                case DIAMOND_LEGGINGS:
                case DIAMOND_HELMET:

                case LEATHER_BOOTS:
                case LEATHER_CHESTPLATE:
                case LEATHER_HELMET:
                case LEATHER_LEGGINGS:

                case BOW:
                case CROSSBOW:
                case FISHING_ROD:
                case SHEARS:
                case TRIDENT:
                case FLINT_AND_STEEL:
                    return true;
                default:
                    return false;
            }
        }
    }
}
