package net.axiiom.skye_statsviewer.listeners;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import net.axiiom.skye_statsviewer.main.StatsViewer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class MobHealth implements Listener {
    private StatsViewer plugin;
    private HashMap<UUID, Long> entitiesHash;
    private long timeUntilRemove;

    public MobHealth(StatsViewer _plugin) {
        this.plugin = _plugin;
        this.entitiesHash = new HashMap();
        this.timeUntilRemove = 4000L;
        (new MobHealth.DisplayTimer()).runTaskTimer(this.plugin, 0L, 4L);
    }

    @EventHandler
    public synchronized void onMobHit(EntityDamageEvent _event) {
        if (_event.getEntity() instanceof Mob) {
            Mob damaged = (Mob)_event.getEntity();
            int hp = this.getHP(damaged, _event.getDamage());
            if (hp >= 1) {
                if (_event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)_event;
                    if (!(event.getDamager() instanceof Player)) {
                        return;
                    }

                    damaged.setCustomName(hp + "" + ChatColor.RED + " ♥");
                    damaged.setCustomNameVisible(true);
                    this.entitiesHash.put(damaged.getUniqueId(), System.currentTimeMillis());
                } else {
                    if (!(_event.getEntity() instanceof Mob)) {
                        return;
                    }

                    if (this.isTracking(damaged)) {
                        damaged.setCustomName(hp + "" + ChatColor.RED + " ♥");
                        this.entitiesHash.put(damaged.getUniqueId(), System.currentTimeMillis());
                    }
                }

            }
        }
    }

    @EventHandler
    public synchronized void onMobDeath(EntityDeathEvent _event) {
        Entity deadEntity = _event.getEntity();
        if (deadEntity instanceof Mob && this.isTracking(deadEntity)) {
            deadEntity.setCustomNameVisible(false);
            this.entitiesHash.remove(deadEntity.getUniqueId());
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

    private int getHP(Mob _damaged, double _damage) {
        double hp = _damaged.getHealth() - _damage;
        if (hp <= 0.0D) {
            return 0;
        } else {
            return hp > 0.0D && hp < 1.0D ? 1 : (int)(hp * 2.0D);
        }
    }

    private class DisplayTimer extends BukkitRunnable {
        private DisplayTimer() {
        }

        public synchronized void run() {
            Iterator var1 = MobHealth.this.entitiesHash.keySet().iterator();

            while(var1.hasNext()) {
                UUID uuid = (UUID)var1.next();
                long timeSinceLastHit = (Long)MobHealth.this.entitiesHash.get(uuid);
                if (timeSinceLastHit + MobHealth.this.timeUntilRemove <= System.currentTimeMillis()) {
                    Mob mob = (Mob)MobHealth.this.plugin.getServer().getEntity(uuid);
                    mob.setCustomNameVisible(false);
                    MobHealth.this.entitiesHash.remove(uuid);
                }
            }

        }
    }
}
