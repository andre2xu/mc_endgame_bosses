package com.github.andre2xu.endgamebosses.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BossStateData extends SavedData {
    public enum State {ALIVE, DEAD, DOES_NOT_EXIST}
    private final HashMap<String, State> BOSS_STATES = new HashMap<>();
    private static final String[] BOSS_NAMES = {
            "ender_dragon"
            // add names of custom bosses under the Ender Dragon
    };



    public BossStateData() {
        // add bosses to hash map and give them a default state

        BOSS_STATES.put(BOSS_NAMES[0], State.ALIVE); // Ender Dragon

        if (BOSS_NAMES.length > 1) {
            // custom bosses
            for (int i = 1; i < BOSS_NAMES.length; i++) {
                BOSS_STATES.put(BOSS_NAMES[i], State.DOES_NOT_EXIST); // their state will be set to ALIVE when the Ender Dragon is killed
            }
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
        // get data from .dat file and pass them to the new instance

        BossStateData boss_state_data = new BossStateData();

        for (String boss_name : BOSS_NAMES) {
            int state_as_integer = pTag.getInt(boss_name);

            if (state_as_integer == 1) {
                boss_state_data.setBossState(boss_name, State.ALIVE);
            }
            else if (state_as_integer == 0) {
                boss_state_data.setBossState(boss_name, State.DEAD);
            }
        }

        return boss_state_data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag pTag, HolderLookup.@NotNull Provider pRegistries) {
        // save the instance's data to the .dat file (must call the 'setDirty' method for this method to run)

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

        return pTag;
    }
}
