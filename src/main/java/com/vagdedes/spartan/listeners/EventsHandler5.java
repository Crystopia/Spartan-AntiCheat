package com.vagdedes.spartan.listeners;

import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.compatibility.manual.abilities.ItemsAdder;
import com.vagdedes.spartan.functionality.connection.PlayerLimitPerIP;
import com.vagdedes.spartan.functionality.connection.cloud.CloudConnections;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.functionality.identifiers.simple.Building;
import com.vagdedes.spartan.functionality.identifiers.simple.SensitiveBlockBreak;
import com.vagdedes.spartan.functionality.notifications.DetectionNotifications;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.InetAddress;

public class EventsHandler5 implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Food(FoodLevelChangeEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            boolean cancelled = e.isCancelled();

            // Detections
            p.getExecutor(Enums.HackType.FastEat).handle(cancelled, e);
            p.getExecutor(Enums.HackType.NoSlowdown).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastHeal).handle(cancelled, e);

            if (p.getViolations(Enums.HackType.FastEat).prevent()
                    || p.getViolations(Enums.HackType.NoSlowdown).prevent()) {
                e.setCancelled(true);
                p.setFoodLevel(n.getFoodLevel(), false);
            } else {
                // Objects (Always after detections)
                p.setFoodLevel(e.getFoodLevel(), false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Regen(EntityRegainHealthEvent e) {
        Entity entity = e.getEntity();

        if (entity instanceof Player) {
            Player n = (Player) entity;
            SpartanPlayer p = SpartanBukkit.getPlayer(n);

            if (p == null) {
                return;
            }
            // Objects
            p.setHealth(n.getHealth());

            // Detections
            p.getExecutor(Enums.HackType.FastHeal).handle(e.isCancelled(), e);

            if (p.getViolations(Enums.HackType.FastHeal).prevent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockBreak(BlockBreakEvent e) {
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Block nb = e.getBlock();
        SpartanBlock b = new SpartanBlock(p, nb);
        boolean cancelled = e.isCancelled();

        // Detections
        if (!ItemsAdder.is(nb)) {
            p.getExecutor(Enums.HackType.NoSwing).handle(cancelled, e);
            p.getExecutor(Enums.HackType.BlockReach).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastBreak).handle(cancelled, e);
            p.getExecutor(Enums.HackType.GhostHand).handle(cancelled, e);
            p.getExecutor(Enums.HackType.ImpossibleActions).handle(cancelled, e);
        }

        // Protections
        SensitiveBlockBreak.run(p, cancelled, b);

        // Detections
        p.getExecutor(Enums.HackType.MorePackets).handle(false, e);
        DetectionNotifications.runMining(p, b, cancelled);

        if (p.getViolations(Enums.HackType.NoSwing).prevent()
                || p.getViolations(Enums.HackType.BlockReach).prevent()
                || p.getViolations(Enums.HackType.ImpossibleActions).prevent()
                || p.getViolations(Enums.HackType.FastBreak).prevent()
                || p.getViolations(Enums.HackType.GhostHand).prevent()
                || p.getViolations(Enums.HackType.XRay).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void BlockPlace(BlockPlaceEvent e) {
        Player n = e.getPlayer();
        SpartanPlayer p = SpartanBukkit.getPlayer(e.getPlayer());

        if (p == null) {
            return;
        }
        Block nb = e.getBlock();
        SpartanBlock b = new SpartanBlock(p, nb);

        if (n.getWorld() != b.world) {
            return;
        }
        Block rba = e.getBlockAgainst();
        BlockFace blockFace = nb.getFace(rba);
        SpartanBlock ba = new SpartanBlock(p, rba);
        boolean cancelled = e.isCancelled();

        // Protections
        Building.runPlace(p, b, blockFace, cancelled);
        BouncingBlocks.judge(p, p.movement.getLocation());

        // Detections
        if (!ItemsAdder.is(nb)) {
            p.getExecutor(Enums.HackType.ImpossibleActions).handle(cancelled, e);
            p.getExecutor(Enums.HackType.BlockReach).handle(cancelled, e);
            p.getExecutor(Enums.HackType.FastPlace).handle(cancelled, e);
        }

        if (p.getViolations(Enums.HackType.FastPlace).prevent()
                || p.getViolations(Enums.HackType.BlockReach).prevent()
                || p.getViolations(Enums.HackType.ImpossibleActions).prevent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void PreLoginEvent(AsyncPlayerPreLoginEvent e) {
        // Features
        if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            InetAddress address = e.getAddress();
            String ipAddress = address != null ? PlayerLimitPerIP.get(address) : null;
            CloudConnections.updatePunishedPlayer(e.getUniqueId(), ipAddress);
        }
    }
}
