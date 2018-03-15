package com.adamgaltrey.virtualchests.data;

import com.adamgaltrey.virtualchests.VirtualChests;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Created by Adam on 06/11/2014.
 */
public class VirtualInventory implements Listener {

    private final UUID owner;
    private int size = 3, pages = 1;
    private ItemStack[] data;

    private static final String nullSlot = ParsableItemStack.getParsableString(new ItemStack(Material.AIR)),
            title = ChatColor.LIGHT_PURPLE + "Virtual Inventory";

    private static final ItemStack previousPage, nextPage;

    private String curTitle;

    public void updateSize() {
        size = 6;
        ItemStack[] newArray = new ItemStack[((size * 9) - 2) * pages];
        //copy all data into new array
        for (int i = 0; i < data.length; i++) {
            newArray[i] = data[i];
        }
        data = newArray;
    }

    public void addPages(int pages) {
        this.pages += pages;
        ItemStack[] newArray = new ItemStack[((size * 9) - 2) * this.pages];
        //copy all data into new array
        for (int i = 0; i < data.length; i++) {
            newArray[i] = data[i];
        }
        data = newArray;
    }

    static {
        previousPage = new ItemStack(Material.BOOK);
        nextPage = new ItemStack(Material.BOOK);

        ItemMeta met = previousPage.getItemMeta();
        met.setDisplayName(ChatColor.YELLOW + "Previous Page");
        previousPage.setItemMeta(met);

        met = nextPage.getItemMeta();
        met.setDisplayName(ChatColor.YELLOW + "Next Page");
        nextPage.setItemMeta(met);
    }

    //
    public VirtualInventory(String uuid) {
        //for new inventories
        this.owner = UUID.fromString(uuid);
        size = 3;
        pages = 1;
        //just sort inventory but exclude slots 0 and 8 for page navigation
        data = new ItemStack[((size * 9) - 2) * pages];
    }

    public VirtualInventory(String uuid, int size, int pages, String dataInText) {
        this.owner = UUID.fromString(uuid);
        this.size = size;
        this.pages = pages;

        /*
            Storage format:
            ---------------
            Items separated by #
            Blank slots represented by an air stack
            Leave class to handle sorting
         */

        String[] parts = dataInText.split("#");
        data = new ItemStack[((size * 9) - 2) * pages];

        for (int i = 0; i < parts.length; i++) {
            data[i] = ParsableItemStack.parse(parts[i]);
        }
    }

    public UUID getOwner() {
        return owner;
    }

    public int getSize() {
        return size;
    }

    public int getPages() {
        return pages;
    }

    public ItemStack[] getData() {
        return data;
    }

    private int curPage = 1;
    private boolean registered = false;

    //0-24 items per page (25 total)
    public void displayInventory(Player p, int page) {
        curPage = page;
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(this, VirtualChests.plugin);
            registered = true;
        }
        curTitle = title + " (" + page + "/" + getPages() + ")";
        Inventory inv = Bukkit.createInventory(p, size * 9, curTitle);
        int pointerStart = 25 * (page - 1);
        int realIndex = 0;
        for (int i = 0; i < size * 9; i++) {
            if (i == 0) {
                //set previous
                inv.setItem(i, previousPage);
            } else if (i == 8) {
                //set next
                inv.setItem(i, nextPage);
            } else {
                inv.setItem(i, data[pointerStart + realIndex]);
                realIndex++;
            }
        }
        p.openInventory(inv);
    }

    /*
        EVENTS
     */

    @EventHandler
    private void close(InventoryCloseEvent evt) {
        if (evt.getPlayer() instanceof Player) {
            Player p = (Player) evt.getPlayer();
            if (registered && getOwner().equals(p.getUniqueId())) {
                registered = false;
                HandlerList.unregisterAll(this);
                //dispose of inventory
                savePage(evt.getInventory(), curPage);
            }
        }
    }

    private void savePage(Inventory inv, int page) {
        int realIndex = (page - 1) * 25;
        int actualIndex = 0;
        for (int i = 0; i < (size * 9); i++) {
            if (i == 0 || i == 8) {
                continue;
            } else {
                ItemStack is = inv.getContents()[i];
                data[realIndex + actualIndex] = is;
                //actual index to account for the books, taking up 2 slots

                //System.out.println("Saved item at index <" + page + ">[" + (realIndex + actualIndex) + "]{" + i + "} = " + (is == null ? "NULL" : is.getType().toString()));

                actualIndex++;
            }
        }
    }

    @EventHandler
    private void click(final InventoryClickEvent evt) {
        if (evt.getWhoClicked() instanceof Player) {
            final Player p = (Player) evt.getWhoClicked();
            if (p.getUniqueId().equals(getOwner())) {
                //owner has clicked, so lets handle
                int slot = evt.getRawSlot();
                if (slot == 0 || slot == 8) {
                    //cancel page navigation, else
                    if (slot == 8) {
                        //next
                        if (curPage == getPages()) {
                            p.sendMessage(ChatColor.RED + "There are no more pages.");
                        } else {
                            savePage(evt.getInventory(), curPage);
                            BukkitRunnable now = new BukkitRunnable() {
                                public void run() {
                                    p.closeInventory();
                                    curPage++;
                                    displayInventory(p, curPage);
                                }
                            };
                            now.runTaskLater(VirtualChests.plugin, 2);
                        }
                    } else {
                        //previous
                        if (curPage == 1) {
                            p.sendMessage(ChatColor.RED + "This is the first page.");
                        } else {
                            savePage(evt.getInventory(), curPage);
                            BukkitRunnable now = new BukkitRunnable() {
                                public void run() {
                                    p.closeInventory();
                                    curPage--;
                                    displayInventory(p, curPage);
                                }
                            };
                            now.runTaskLater(VirtualChests.plugin, 2);
                        }
                    }
                    evt.setCancelled(true);
                }
            }
        }
    }

    public String getSaveString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null) {
                s.append(nullSlot);
            } else {
                String form = ParsableItemStack.getParsableString(data[i]);
                s.append(form);
            }
            s.append("#");
        }
        return s.toString();
    }

    public void setPages(int pages) {
        this.pages = pages;

        ItemStack[] newArray = new ItemStack[((size * 9) - 2) * pages];
        //copy all data into new array
        for (int i = 0; i < newArray.length; i++) {
            if (i < data.length) {
                newArray[i] = data[i];
            } else {
                newArray[i] = new ItemStack(Material.AIR);
            }
        }

        data = newArray;
    }

}
