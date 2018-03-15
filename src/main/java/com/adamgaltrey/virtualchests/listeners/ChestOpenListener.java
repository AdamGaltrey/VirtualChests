package com.adamgaltrey.virtualchests.listeners;

import com.adamgaltrey.virtualchests.VirtualChests;
import com.adamgaltrey.virtualchests.data.Loc;
import com.adamgaltrey.virtualchests.data.VirtualInventory;
import com.adamgaltrey.virtualchests.managers.ChestManager;
import com.adamgaltrey.virtualchests.managers.VirtualLocations;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

/**
 * Created by Adam on 06/11/2014.
 */
public class ChestOpenListener implements Listener {

    public ChestOpenListener(Plugin p) {
        Bukkit.getPluginManager().registerEvents(this, p);
    }

    @EventHandler
    private void interact(PlayerInteractEvent evt) {
        if (evt.getClickedBlock() != null && evt.getClickedBlock().getType().equals(Material.CHEST)) {
            //player has clicked a chest so lets get its location
            String id = new Loc(evt.getClickedBlock().getLocation()).toString();
            if (VirtualLocations.isVirtualChest(id)) {
                // player has clicked on a virtual chest
                if (VirtualChests.isInPvPRegion(evt.getPlayer())) {
                    evt.getPlayer().sendMessage(ChatColor.RED + "Virtual chests cannot be accessed inside PvP areas.");
                    return;
                }
                openVirtualChest(evt.getPlayer());
                evt.setCancelled(true);
            }
        }
    }

    private void openVirtualChest(Player p) {
        VirtualInventory vi = ChestManager.getChest(p.getUniqueId().toString());
        vi.displayInventory(p, 1);
    }

}
