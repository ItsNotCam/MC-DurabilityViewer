package net.axiiom.skye_statsviewer.listeners;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.axiiom.skye_statsviewer.main.StatsViewer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
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
    public synchronized void onMobSpawn(EntitySpawnEvent _event) {
        if(_event.getEntity() instanceof Mob && !_event.getEntityType().equals(EntityType.VILLAGER)) {
            Mob spawnedMob = (Mob)_event.getEntity();
            double maxHealth = spawnedMob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double health = spawnedMob.getHealth();
            spawnedMob.setCustomName(health + "/" + maxHealth + ChatColor.RED + " ♥");
        }
    }

    @EventHandler
    public synchronized void onMobHit(EntityDamageEvent _event) {
        if(_event.isCancelled())
            return;

        if (_event.getEntity() instanceof Mob && !(_event.getEntity() instanceof EnderDragon || _event.getEntity() instanceof Wither)) {
            Mob damaged = (Mob)_event.getEntity();
            double maxHealth = damaged.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

            if(damaged.getCustomName() != null && !damaged.getCustomName().contains("♥"))
                oldEntityName.put(damaged.getUniqueId(), damaged.getCustomName());

            double hp = getHP(damaged, _event.getDamage());
            hp = round(hp,1);
            if (hp >= 0.1) {
                damaged.setCustomName("" + hp + "/" + maxHealth + ChatColor.RED + " ♥");
                damaged.setCustomNameVisible(true);
                this.entitiesHash.put(damaged.getUniqueId(), System.currentTimeMillis());
            } else {
                damaged.setCustomName("0.0" + "/" + maxHealth + ChatColor.RED + " ♥");
                damaged.setCustomNameVisible(true);
                entitiesHash.remove(damaged.getUniqueId());
            }
        }
    }

    @EventHandler
    public synchronized void onMobHeal(EntityRegainHealthEvent _event) {
        if(_event.isCancelled())
            return;

        if (_event.getEntity() instanceof Mob && !(_event.getEntity() instanceof EnderDragon || _event.getEntity() instanceof Wither)) {
            Mob healed = (Mob) _event.getEntity();
            double maxHealth = healed.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

            if (healed.getCustomName() != null && !healed.getCustomName().contains("♥"))
                oldEntityName.put(healed.getUniqueId(), healed.getCustomName());

            double hp = getHP(healed, -1 * _event.getAmount());
            hp = round(hp, 1);
            hp = hp > maxHealth ? maxHealth : hp;

            healed.setCustomName("" + hp + "/" + maxHealth + ChatColor.RED + " ♥");
            healed.setCustomNameVisible(true);
            this.entitiesHash.put(healed.getUniqueId(), System.currentTimeMillis());
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
