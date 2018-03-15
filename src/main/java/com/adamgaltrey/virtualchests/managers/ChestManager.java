package com.adamgaltrey.virtualchests.managers;

import com.adamgaltrey.virtualchests.VirtualChests;
import com.adamgaltrey.virtualchests.data.VirtualInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Adam on 06/11/2014.
 */
public class ChestManager {

    private static Map<String, VirtualInventory> chests = new HashMap<String, VirtualInventory>();

    //map the last known save string to determine if we need to make changes
    private static Map<String, String> saveLog = new HashMap<String, String>();

    private static BukkitRunnable task;

    public static void initSaverTask() {
        if (task == null) {
            task = new BukkitRunnable() {
                public void run() {
                    //run each minute
                    saveCheck();
                }
            };
            task.runTaskTimerAsynchronously(VirtualChests.plugin, 60 * 20, 60 * 20);
        }
    }

    private static void saveCheck() {
        Collection<VirtualInventory> invs = new ArrayList<VirtualInventory>(chests.values());
        for (VirtualInventory vi : invs) {
            //lets update
            String old = saveLog.containsKey(vi.getOwner().toString()) ? saveLog.get(vi.getOwner().toString()) : "";

            String saveString = vi.getSaveString();

            if (old.length() != saveString.length() || !old.equals(saveString)) {
                //strings are different so lets update
                try {
                    VirtualChests.sql.standardQuery("UPDATE " + VirtualChests.table + " SET data='" + saveString + "' WHERE uuid='" + vi.getOwner().toString() + "';");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void onDisable() {
        task.cancel();
        VirtualChests.logger.info("Saving inventory contents...");
        //run on main thread to block, because immediately after sql connection will be terminated
        saveCheck();
    }

    public static void addChest(String uuid, VirtualInventory vi) {
        chests.put(uuid, vi);
    }

    public static VirtualInventory getChest(final String uuid) {
        if (!chests.containsKey(uuid)) {
            //need to make them a new chest
            final VirtualInventory vi = new VirtualInventory(uuid);

            new BukkitRunnable() {
                public void run() {
                    try {
                        VirtualChests.sql.standardQuery("INSERT INTO " + VirtualChests.table + " (uuid,size,pages,data) VALUES ('" + uuid + "', 3, 1, '');");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    chests.put(uuid, vi);
                }
            }.runTaskAsynchronously(VirtualChests.plugin);

            return vi;
        } else {
            return chests.get(uuid);
        }
    }
}
