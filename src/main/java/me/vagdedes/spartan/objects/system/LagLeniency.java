package me.vagdedes.spartan.objects.system;

import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;

public class LagLeniency {

    private final double tps;
    private final int ping;
    private final long time;

    public LagLeniency(SpartanPlayer p) {
        this.tps = TPS.get(p, false);
        this.ping = p.getPing();
        this.time = System.currentTimeMillis();
    }

    public double getTPS() {
        return tps;
    }

    public int getPing() {
        return ping;
    }

    public long getCreationTime() {
        return System.currentTimeMillis() - time;
    }
}
