package com.adamgaltrey.virtualchests.managers;

import com.adamgaltrey.virtualchests.VirtualChests;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Adam on 06/11/2014.
 */
public class VirtualLocations {

    // map location tag -> location string
    private static HashMap<String, String> tags = new HashMap<String, String>();

    // fast lookups of chest locations
    private static HashSet<String> locs = new HashSet<String>();

    public static void addLocation(String tag, String loc) {
        tags.put(tag, loc);
        locs.add(loc);
    }

    public static boolean isVirtualChest(String loc) {
        return locs.contains(loc);
    }

    public static boolean saveTag(String tag, String loc) {
        if (tags.containsKey(tag) || locs.contains(loc)) {
            return false;
        } else {
            tags.put(tag, loc);
            FileConfiguration io = YamlConfiguration.loadConfiguration(VirtualChests.generalConfig);
            io.set("virtual.chests." + tag, loc);
            try {
                io.save(VirtualChests.generalConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

}
