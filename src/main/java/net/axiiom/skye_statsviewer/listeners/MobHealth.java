package net.axiiom.skye_statsviewer.listeners;

import java.text.DecimalFormat;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.axiiom.skye_statsviewer.main.StatsViewer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

public class MobHealth implements Listener {
    private StatsViewer plugin;
    private ConcurrentHashMap<UUID, Long> entitiesHash;
    private ConcurrentHashMap<UUID, String> oldEntityName;
    private long timeUntilRemove;

    public MobHealth(StatsViewer _plugin) {
        this.plugin = _plugin;
        this.entitiesHash = new ConcurrentHashMap<>();
        this.timeUntilRemove = 4000L;
        this.oldEntityName = new ConcurrentHashMap<>();

        (new MobHealth.DisplayTimer()).runTaskTimer(this.plugin, 0L, 20L);
    }

    @EventHandler
    public synchronized void onMobHit(EntityDamageEvent _event) {
        if (_event.getEntity() instanceof Mob) {
            Mob damaged = (Mob)_event.getEntity();

            if(damaged.getCustomName() != null && !damaged.getCustomName().contains("♥"))
                oldEntityName.put(damaged.getUniqueId(), damaged.getCustomName());

            double hp = getHP(damaged, _event.getDamage());
            hp = round(hp,1);
            if (hp >= 1) {
                if (_event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)_event;
                    if (!(event.getDamager() instanceof Player)) {
                        return;
                    }

                    damaged.setCustomName(hp + "" + ChatColor.RED + " ♥");
                    damaged.setCustomNameVisible(true);
                    this.entitiesHash.put(damaged.getUniqueId(), System.currentTimeMillis());
                }
            } else {
                entitiesHash.remove(damaged.getUniqueId());
            }
        }
    }

    @EventHandler
    public synchronized void onCreeperExplosion(EntityExplodeEvent _event) {
        Entity deadEntity = _event.getEntity();
        if(deadEntity instanceof Creeper && this.isTracking(deadEntity)) {
            deadEntity.setCustomNameVisible(false);
            this.entitiesHash.remove(deadEntity.getUniqueId());
        }
    }

    private boolean isTracking(Entity _entity) {
        return this.entitiesHash.containsKey(_entity.getUniqueId());
    }

    private double getHP(Mob _damaged, double _damage) {
        double hp = _damaged.getHealth() - _damage;
        if (hp <= 0.0D) {
            return 0;
        }

        else return hp;
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private class DisplayTimer extends BukkitRunnable {
        public synchronized void run() {
            try {
                for(UUID uuid : entitiesHash.keySet()) {
                    long timeSinceLastHit = MobHealth.this.entitiesHash.get(uuid);

                    if(Bukkit.getEntity(uuid) == null)
                        entitiesHash.remove(uuid);

                    else if (timeSinceLastHit + MobHealth.this.timeUntilRemove <= System.currentTimeMillis()) {
                        Mob mob = (Mob) Bukkit.getEntity(uuid);
                        mob.setCustomNameVisible(false);
                        MobHealth.this.entitiesHash.remove(uuid);

                        if(oldEntityName.containsKey(mob.getUniqueId())) {
                            mob.setCustomName(oldEntityName.get(mob.getUniqueId()));
                            oldEntityName.remove(mob.getUniqueId());
                        }
                    }

                }
            }
            catch(ConcurrentModificationException e) {
                e.printStackTrace();
            }

        }
    }
}
