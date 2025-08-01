package com.github.andre2xu.endgamebosses.data;

import com.github.andre2xu.endgamebosses.EndgameBosses;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BossStateData extends SavedData {
    public enum State {ALIVE, DEAD, DOES_NOT_EXIST}
    private static HashMap<String, State> BOSS_STATES = new HashMap<>();
    private static String active_boss = "";
    private static int boss_spawn_cooldown = 0; // in ticks. This is for auto resetting the active boss to prevent spawning issues



    public BossStateData() {
        BOSS_STATES.put("ender_dragon", State.ALIVE); // set default state of Ender Dragon
    }

    public void addBosses(String[] bossNames) {
        if (bossNames.length > 0) {
            for (String boss_name : bossNames) {
                if (!BOSS_STATES.containsKey(boss_name)) {
                    BOSS_STATES.put(boss_name, State.ALIVE);
                }
            }

            this.setDirty();
        }
    }

    public void setBossState(String bossName, State state) {
        if (BOSS_STATES.containsKey(bossName)) {
            BOSS_STATES.put(bossName, state);
            this.setDirty();
        }
    }

    public State getBossState(String bossName) {
        if (BOSS_STATES.containsKey(bossName)) {
            return BOSS_STATES.get(bossName);
        }

        return State.DOES_NOT_EXIST;
    }

    public boolean isBossAlive(String bossName) {
        return this.getBossState(bossName) == State.ALIVE;
    }

    public void reset() {
        BOSS_STATES = new HashMap<>();
        BOSS_STATES.put("ender_dragon", State.ALIVE);

        active_boss = "";
        boss_spawn_cooldown = 0;
    }

    public void setActiveBoss(@Nullable String bossName) {
        if (BOSS_STATES.containsKey(bossName)) {
            active_boss = bossName;

            boss_spawn_cooldown = 20 * 60 * 60 * 2; // 2 hours
        }
        else {
            active_boss = "";

            boss_spawn_cooldown = 0;
        }
    }

    public String getActiveBoss() {
        return active_boss;
    }

    public void updateBossSpawnCooldown() {
        // NOTE: Only call this in a world/server level tick method, i.e. one that runs all the time in the background

        if (boss_spawn_cooldown > 0) {
            boss_spawn_cooldown--;

            // force reset the active boss when the cooldown hits zero
            if (boss_spawn_cooldown == 0) {
                active_boss = "";
            }
        }
    }



    public static BossStateData createOrGet(MinecraftServer server) {
        /*
        CALL THIS METHOD INSTEAD OF CREATING AN INSTANCE OF THE CLASS

        This method generates a 'boss_state_data.dat' file if it doesn't exist, or loads it if it does exist, and creates a new instance of BossStateData to read from or write to the .dat file. The getter/setter methods can be accessed from the instance returned by this method.

        NOTE: the .dat file can be found in run/saves/[WORLD NAME]/data
        */

        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BossStateData::new, BossStateData::load, DataFixTypes.LEVEL),
                "boss_state_data" // file name
        );
    }

    public static BossStateData load(@NotNull CompoundTag pTag, HolderLookup.@NotNull Provider pRegistries) {
        // OBJECTIVE: Get data from .dat file and pass them to the new instance

        // add bosses
        for (String boss_name : EndgameBosses.BOSSES) {
            if (pTag.contains(boss_name)) {
                BOSS_STATES.put(boss_name, State.ALIVE);
            }
        }

        // load boss states
        BossStateData boss_state_data = new BossStateData();

        for (String boss_name : BOSS_STATES.keySet()) {
            int state_as_integer = pTag.getInt(boss_name);

            if (state_as_integer == 1) {
                boss_state_data.setBossState(boss_name, State.ALIVE);
            }
            else if (state_as_integer == 0) {
                boss_state_data.setBossState(boss_name, State.DEAD);
            }
        }

        // load active boss
        boss_state_data.setActiveBoss(pTag.getString("active_boss"));

        // load boss spawn cooldown
        boss_spawn_cooldown = pTag.getInt("boss_spawn_cooldown");

        return boss_state_data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag pTag, HolderLookup.@NotNull Provider pRegistries) {
        // OBJECTIVE: Save the instance's data to the .dat file (must call the 'setDirty' method for this method to run)

        // save boss states
        for (Map.Entry<String, State> boss : BOSS_STATES.entrySet()) {
            String boss_name = boss.getKey();
            State boss_state = boss.getValue();

            if (boss_state == State.ALIVE) {
                pTag.putInt(boss_name, 1);
            }
            else if (boss_state == State.DEAD) {
                pTag.putInt(boss_name, 0);
            }
        }

        // save active boss
        pTag.putString("active_boss", active_boss);

        // save boss spawn cooldown
        pTag.putInt("boss_spawn_cooldown", boss_spawn_cooldown);

        return pTag;
    }
}
