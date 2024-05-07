package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.inventory.implementation.MainMenu;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.connection.Latency;
import com.vagdedes.spartan.functionality.inventory.InteractiveInventory;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import me.vagdedes.spartan.system.Enums;

import java.util.*;

public class PlayerEvidence {

    enum EvidenceType {
        HACKER(2),
        SUSPECTED(1),
        LEGITIMATE(0);

        private final int requirement;

        EvidenceType(int requirement) {
            this.requirement = requirement;
        }
    }

    // Object

    private final PlayerProfile profile;
    private EvidenceType type;
    final Map<Enums.HackType, String> live; // Live object is used for synchronization
    public final Map<Enums.HackType, String> historical; // Live object is used for synchronization

    PlayerEvidence(PlayerProfile profile) {
        this.profile = profile;
        this.type = EvidenceType.LEGITIMATE;
        this.live = Collections.synchronizedMap(new LinkedHashMap<>(Enums.HackType.values().length));
        this.historical = new LinkedHashMap<>(Enums.HackType.values().length);
    }

    // Separator

    public EvidenceType getType() {
        return type;
    }

    // Separator

    public void clear(boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.clear();
            }
            if (historical) {
                this.historical.clear();
            }
            if (judge) {
                this.judgeLocal();
            }
        }
    }

    public void remove(Enums.HackType hackType, boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.remove(hackType);
            }
            if (historical) {
                this.historical.remove(hackType);
            }
            if (judge) {
                this.judgeLocal();
            }
        }
    }

    public void add(Enums.HackType hackType, String info, boolean live, boolean historical, boolean judge) {
        synchronized (this.live) {
            if (live) {
                this.live.put(hackType, info);
            }
            if (historical) {
                this.historical.put(hackType, info);
            }
            if (judge) {
                this.judgeLocal();
            }
        }
    }

    // Separator

    public Collection<Enums.HackType> getKnowledgeList() {
        synchronized (this.live) {
            return getRawKnowledgeList();
        }
    }

    private Collection<Enums.HackType> getRawKnowledgeList() {
        Collection<Enums.HackType> set = new HashSet<>(live.keySet());
        set.addAll(historical.keySet());
        return set;
    }

    // Separator

    public String getKnowledge(Enums.HackType hackType, String color) {
        synchronized (this.live) {
            String knowledge = historical.get(hackType);

            if (knowledge == null) {
                knowledge = live.get(hackType);
            }
            return knowledge == null ? knowledge : addColor(knowledge, color);
        }
    }

    private String addColor(String knowledge, String color) {
        return knowledge.replace("§r", "§r" + color);
    }

    // Separator

    boolean has(EvidenceType type) {
        return this.type == type;
    }

    public boolean has(Enums.HackType hackType) {
        synchronized (this.live) {
            return live.containsKey(hackType) || historical.containsKey(hackType);
        }
    }

    // Separator

    public Collection<Enums.HackType> calculate(SpartanPlayer player, PlayerViolation playerViolation) {
        synchronized (this.live) {
            if (this.live.containsKey(playerViolation.hackType)
                    || this.historical.containsKey(playerViolation.hackType)) {
                return this.getRawKnowledgeList();
            } else {
                Check check = playerViolation.hackType.getCheck();

                if (check.supportsLiveEvidence) {
                    double ignoredViolations = playerViolation.getIgnoredViolations(player),
                            violationCount = player.getViolations(playerViolation.hackType).getLevel(playerViolation.identity)
                                    - AlgebraUtils.integerCeil(Latency.getDelay(player))
                                    - ignoredViolations;

                    if (violationCount > 0.0) {
                        double ratio = violationCount / ignoredViolations;

                        if (ratio >= Check.standardIgnoredViolations) {
                            this.live.put(playerViolation.hackType,
                                    "Ratio: " + AlgebraUtils.cut(ratio, 2) + "%"
                            );
                            this.judgeLocal();
                            InteractiveInventory.playerInfo.refresh(player.name);
                            MainMenu.refresh();
                            return this.getRawKnowledgeList();
                        }
                    }
                }
                return new ArrayList<>(0);
            }
        }
    }

    // Separator

    void judgeLocal() {
        int count = this.getRawKnowledgeList().size();
        EvidenceType type = this.type;

        for (EvidenceType evidenceType : EvidenceType.values()) {
            if (count >= evidenceType.requirement) {
                type = evidenceType;

                if (evidenceType != EvidenceType.LEGITIMATE) {
                    break;
                }
            }
        }
        this.type = type;
    }

    public void judge() {
        synchronized (this.live) {
            this.judgeLocal();
        }
    }
}
