package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.component.ComponentXZ;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.component.ComponentY;
import com.vagdedes.spartan.abstraction.check.implementation.movement.morepackets.TimerBalancer;
import com.vagdedes.spartan.abstraction.data.CheckBoundData;
import com.vagdedes.spartan.abstraction.data.EncirclementData;
import com.vagdedes.spartan.abstraction.data.PacketWorld;
import com.vagdedes.spartan.abstraction.event.CPlayerVelocityEvent;
import com.vagdedes.spartan.abstraction.event.PlayerTickEvent;
import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.necessary.protocollib.ProtocolLib;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.PluginBase;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;
import com.vagdedes.spartan.utils.minecraft.entity.AxisAlignedBB;
import com.vagdedes.spartan.utils.minecraft.protocol.ProtocolTools;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlayerProtocol {

    public long activeCreationTime;
    private Player bukkit;
    private EncirclementData encirclementData;
    public final PlayerBukkit bukkitExtra;

    // Custom
    private int hashPosBuffer;
    public int
            rightClickCounter,
            transactionVl,
            flyingTicks;
    public final boolean npc;
    public boolean
            onGround,
            onGroundFrom,
            mutateTeleport,
            sprinting,
            sneaking,
            vehicleStatus,
            simulationFlag,
            teleported,
            pistonTick,
            transactionBoot,
            clickBlocker,
            transactionLocal,
            transactionSentKeep,
            useItemPacket, useItemPacketReset;
    @Setter
    private Location location, from, teleport;
    public String fromWorld;
    public Location simulationStartPoint;
    public byte
            simulationDelayPerTP,
            keepEntity;
    private final List<Location>
            positionHistory,
            positionHistoryLong,
            positionHistoryShort;
    public CPlayerVelocityEvent claimedVelocity;
    public final List<CPlayerVelocityEvent>
            claimedVeloGravity,
            claimedVeloSpeed;
    public long tickTime;
    public final MultiVersion.MCVersion version;
    public final TimerBalancer timerBalancer;

    private Set<AxisAlignedBB> axisMatrixCache;
    @Setter
    private CheckBoundData checkBoundData;
    public final PacketWorld packetWorld;
    public long placeTime, placeHash;
    public PlayerTickEvent lastTickEvent;

    public short transactionId;
    public long
            transactionTime,
            transactionLastTime,
            transactionPing,
            lagTick, oldClickTime;
    public boolean entityHandle;

    private ComponentY componentY;
    private ComponentXZ componentXZ;

    public PlayerProtocol(Player player) {
        long time = System.currentTimeMillis();
        this.activeCreationTime = time;
        this.bukkit = player;
        this.version = MultiVersion.get(player);
        this.packetWorld = new PacketWorld(player);
        this.npc = player.getAddress() == null;

        this.placeTime = time;
        this.placeHash = 0;
        this.onGround = false;
        this.onGroundFrom = false;
        this.sprinting = false;
        this.sneaking = false;
        this.mutateTeleport = false;
        this.location = ProtocolTools.getLoadLocation(player);
        this.from = null;
        this.fromWorld = "";
        this.teleport = null;
        this.simulationFlag = false;
        this.transactionVl = 0;
        this.bukkitExtra = new PlayerBukkit(this);
        this.timerBalancer = new TimerBalancer();
        this.simulationStartPoint = null;
        this.simulationDelayPerTP = 1;
        this.teleported = false;
        this.vehicleStatus = true;
        this.keepEntity = 0;
        this.flyingTicks = 0;
        this.oldClickTime = System.currentTimeMillis();
        this.clickBlocker = false;
        this.tickTime = time;
        this.encirclementData = new EncirclementData();
        this.rightClickCounter = 0;
        this.positionHistoryShort = Collections.synchronizedList(new LinkedList<>());
        this.positionHistory = Collections.synchronizedList(new LinkedList<>());
        this.positionHistoryLong = Collections.synchronizedList(new LinkedList<>());
        this.hashPosBuffer = 0;
        this.claimedVelocity = null;
        this.claimedVeloGravity = new CopyOnWriteArrayList<>();
        this.claimedVeloSpeed = new CopyOnWriteArrayList<>();
        this.entityHandle = false;

        this.axisMatrixCache = new HashSet<>();
        this.checkBoundData = null;
        this.pistonTick = false;
        this.lastTickEvent = null;
        this.transactionId = (short) -1939;
        this.transactionTime = System.currentTimeMillis();
        this.transactionLastTime = System.currentTimeMillis();
        this.transactionPing = 0;
        this.transactionBoot = false;
        this.transactionLocal = false;
        this.transactionSentKeep = false;
        this.lagTick = 0;
        this.componentY = new ComponentY();
        this.componentXZ = new ComponentXZ();
        this.useItemPacket = false;
        this.useItemPacketReset = false;

        // Always last
        this.profile().update(this);
    }

    public final Player bukkit() {
        return this.bukkit;
    }

    public void updateBukkit(Player player) {
        this.bukkit = player;
    }

    void resetActiveCreationTime() {
        this.activeCreationTime = System.currentTimeMillis();
    }

    public long getActiveTimePlayed() {
        return System.currentTimeMillis() - this.activeCreationTime;
    }

    public boolean isUsingVersion(MultiVersion.MCVersion trialVersion) {
        return version.ordinal() == trialVersion.ordinal();
    }

    public boolean isDesync() {
        return this.transactionSentKeep && (System.currentTimeMillis() - this.transactionTime > 55);
    }

    public boolean isBlatantDesync() {
        return this.transactionSentKeep && (System.currentTimeMillis() - this.transactionTime > 150);
    }

    public boolean isSDesync() {
        return this.transactionSentKeep && (System.currentTimeMillis() - this.transactionTime > 400);
    }

    public boolean isUsingVersionOrGreater(MultiVersion.MCVersion trialVersion) {
        return version.ordinal() >= trialVersion.ordinal();
    }

    public void pushHashPosition(Location location) {
        this.hashPosBuffer = Objects.hashCode((location.getX() + location.getY() + location.getZ()));
    }

    public boolean isSameWithHash(Location location) {
        return this.hashPosBuffer == Objects.hashCode((location.getX() + location.getY() + location.getZ()));
    }

    public boolean isOnGround() {
        return packetsEnabled()
                ? this.onGround
                : this.bukkitExtra.isOnGround();
    }

    public boolean isOnGroundFrom() {
        return packetsEnabled()
                ? this.onGroundFrom
                : this.bukkitExtra.isOnGround();
    }

    public boolean isSprinting() {
        return packetsEnabled()
                ? this.sprinting
                : this.bukkit().isSprinting();
    }

    public boolean isSneaking() {
        return packetsEnabled()
                ? this.sneaking
                : this.bukkit().isSneaking();
    }

    public Location getLocation() {
        Location loc = this.packetsEnabled()
                ? this.location
                : ProtocolLib.getLocationOrNull(this.bukkit());
        return loc != null
                ? loc
                : SpartanLocation.bukkitDefault.clone();
    }

    public Location getFromLocation() {
        return this.from != null
                ? this.from
                : this.getLocation();
    }

    public Location getVehicleLocation() {
        Entity vehicle = this.bukkitExtra.getVehicle();

        if (vehicle instanceof LivingEntity || vehicle instanceof Vehicle) {
            return ProtocolLib.getLocationOrNull(vehicle);
        } else {
            return null;
        }
    }

    public Location getLocationOrVehicle() {
        Location vehicleLocation = getVehicleLocation();

        if (vehicleLocation == null) {
            return this.getLocation();
        } else {
            return vehicleLocation;
        }
    }

    public List<Location> getPositionHistory() {
        synchronized (this.positionHistory) {
            return new ArrayList<>(this.positionHistory);
        }
    }

    public List<Location> getPositionHistoryLong() {
        synchronized (this.positionHistoryLong) {
            return new ArrayList<>(this.positionHistoryLong);
        }
    }

    public List<Location> getPositionHistoryShort() {
        synchronized (this.positionHistoryShort) {
            return new ArrayList<>(this.positionHistoryShort);
        }
    }

    public SpartanLocation getPastTickRotation() {
        Location l = this.getLocation().clone();
        l.setYaw(this.from.getYaw());
        l.setPitch(this.from.getPitch());
        return new SpartanLocation(l);
    }

    public boolean teleport(Location location) {
        if (this.getWorld().equals(location.getWorld())) {
            if (PluginBase.isSynchronised()) {
                this.bukkit().leaveVehicle();
            }
            this.bukkitExtra.movement.removeLastLiquidTime();
            this.bukkitExtra.trackers.removeMany(PlayerTrackers.TrackerFamily.VELOCITY);

            if (MultiVersion.folia) {
                this.bukkit().teleportAsync(location);
            } else {
                PluginBase.transferTask(
                        this,
                        () -> this.bukkit().teleport(location)
                );
            }
            return true;
        } else {
            return false;
        }
    }

    public int getPing() {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            return this.bukkit().isOnline() ? this.bukkit().getPing() : 0;
        } else {
            try {
                Object obj = PluginBase.getCraftPlayerMethod(this.bukkit(), "ping");
                return obj instanceof Integer ? Math.max((int) obj, 0) : 0;
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    public UUID getUUID() {
        return ProtocolLib.isTemporary(this.bukkit())
                ? UUID.randomUUID()
                : this.bukkit().getUniqueId();
    }

    public PlayerProfile profile() {
        return ResearchEngine.getPlayerProfile(this);
    }

    // Separator

    public void setOnGround(boolean isOnGround) {
        this.onGroundFrom = this.onGround;
        this.onGround = isOnGround;

        if (this.onGround) {
            this.bukkitExtra.movement.airTicks = 0;
        } else {
            this.bukkitExtra.movement.airTicks++;
        }
    }

    public void setFromLocation(Location location) {
        this.from = location;
    }

    public void addRawLocation(Location location) {
        synchronized (this.positionHistory) {
            this.positionHistory.add(location.clone());

            if (this.positionHistory.size() > 20) {
                Iterator<Location> iterator = this.positionHistory.iterator();
                iterator.next();
                iterator.remove();
            }
        }
        synchronized (this.positionHistoryShort) {
            this.positionHistoryShort.add(location.clone());

            if (this.positionHistoryShort.size() > 5) {
                Iterator<Location> iterator = this.positionHistoryShort.iterator();
                iterator.next();
                iterator.remove();
            }
        }
        synchronized (this.positionHistoryLong) {
            this.positionHistoryLong.add(location.clone());

            if (this.positionHistoryLong.size() > 50) {
                Iterator<Location> iterator = this.positionHistoryLong.iterator();
                iterator.next();
                iterator.remove();
            }
        }
    }

    public World getWorld() {
        return this.getLocation().getWorld();
    }

    public boolean packetsEnabled() {
        return this.bukkitExtra.packetsEnabled();
    }

}