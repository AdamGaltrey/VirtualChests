package com.adamgaltrey.virtualchests.data;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsableItemStack {

	private static Pattern p = Pattern.compile("\\{(.*?):(.*?)\\}");

	public static ItemStack parse(String parse) {

		Matcher matcher = p.matcher(parse);

		Material m = null;
		int quantity = 1;
		short durability = 0;
		short data = 0;

		String displayname = null;
		List<String> lore = new ArrayList<String>();
		HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();

		int matches = 0;

		boolean err = false;

		while (matcher.find()) {
			matches++;
			String get = matcher.group();
			String in = matcher.group().substring(1, get.length() - 1);
			String[] args = in.split(":");
			String key = args[0];
			String value = args[1];
			if (key.equalsIgnoreCase("id")) {
				m = Material.valueOf(value);
				if (m == null) {
					err = true;
					System.err.println("Material name not recognised, recieved " + get);
				}
			} else if (key.equalsIgnoreCase("quantity")) {
				try {
					quantity = Integer.parseInt(value);
				} catch (NumberFormatException nfe) {
					err = true;
					System.err.println("Quantity must be an integer, recieved " + get);
				}
			} else if (key.equalsIgnoreCase("data")) {
				try {
					data = Short.parseShort(value);
				} catch (NumberFormatException nfe) {
					err = true;
					System.err.println("Data must be a short (" + Short.MIN_VALUE + "-" + Short.MAX_VALUE + "), recieved " + get);
				}
			} else if (key.equalsIgnoreCase("durability")) {
				try {
					durability = Short.parseShort(value);
				} catch (NumberFormatException nfe) {
					err = true;
					System.err.println("Data must be a short (" + Short.MIN_VALUE + "-" + Short.MAX_VALUE + "), recieved " + get);
				}
			} else if (key.equalsIgnoreCase("displayname")) {
				displayname = ChatColor.translateAlternateColorCodes('&', value);
			} else if (key.equalsIgnoreCase("lore")) {
				for (String s : value.split(",")) {
					lore.add(ChatColor.translateAlternateColorCodes('&', s));
				}
			} else if (key.equalsIgnoreCase("enchantment")) {
				// key:enchantment:1
				if (args.length == 3) {
					int pow = 1;
					try {
						pow = Integer.parseInt(args[2]);
					} catch (NumberFormatException nfe) {
						err = true;
						System.err.println("Enchantment power must be an integer (" + Integer.MIN_VALUE + "-" + Integer.MAX_VALUE + "), recieved " + get);
						continue;
					}

					Enchantment e = Enchantment.getByName(value);

					if (e == null) {
						err = true;
						System.err.println("Invalid enchantment name, recieved " + get);
					} else {
						enchants.put(e, pow);
					}

				} else {
					err = true;
					System.err.println("Couldn't add enchantment, invalid args length, expect 3, got " + args.length + " for recieved " + get);
				}
			}
		}

		if (matches == 0) {
			err = true;
		} else if (m == null) {
			System.err.println("No ID value found!");
			err = true;
		}

		if (err) {
			printHelp();
			return null;
		} else {
			// no error
			ItemStack i = new ItemStack(m, quantity, data);

			if (durability != 0) {
				i.setDurability(durability);
			}

			if (i.hasItemMeta()) {

				ItemMeta meta = i.getItemMeta();

				if (displayname != null) {
					meta.setDisplayName(displayname);
				}

				if (!lore.isEmpty()) {
					meta.setLore(lore);
				}

				EnchantmentStorageMeta esMeta = null;

				if (!enchants.isEmpty()) {

					if (m == Material.ENCHANTED_BOOK) {
						esMeta = (EnchantmentStorageMeta) meta;
					}

					for (Entry<Enchantment, Integer> entry : enchants.entrySet()) {
						if (m == Material.ENCHANTED_BOOK) {
							esMeta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
						} else {
							meta.addEnchant(entry.getKey(), entry.getValue(), true);
						}
					}
				}

				if (esMeta == null) {
					i.setItemMeta(meta);
				} else {
					esMeta.setDisplayName(displayname);
					esMeta.setLore(lore);
					i.setItemMeta(esMeta);
				}

			}
			
			i.addEnchantments(enchants);

			return i;
		}

	}

	private static void printHelp() {
		System.err.println("Couldn't find any matches! Format is {key:value}");
		System.err.println("Valid keys are : id, quantity, data, durability, displayname, lore, enchantment");
		System.err.println("Seperate multiple lore lines by a comma (,). Seperate enchantment id/name from power with a colon (:)");
		System.err.println("Lore and display name supports colour coding using the ampersand (&) followed by a colour code. Eg &a or &3.");
		System.err.println("Eg: {lore:line1,line2} {enchantment:id:power}");
	}

	public static String getParsableString(ItemStack is) {

		StringBuilder print = new StringBuilder();

		print.append("{").append("id").append(":").append(is.getType().toString()).append("}");
		
		if(is.getDurability() != 0){
			print.append("{").append("durability").append(":").append(is.getDurability()).append("}");
		}

		if (is.getData().getData() != 0) {
			print.append("{").append("data").append(":").append(is.getData().getData()).append("}");
		}

		if (is.getAmount() != 1) {
			print.append("{").append("quantity").append(":").append(is.getAmount()).append("}");
		}

		if (is.hasItemMeta()) {

			ItemMeta meta = is.getItemMeta();

			if (meta.hasDisplayName()) {
				print.append("{").append("displayname").append(":").append(meta.getDisplayName()).append("}");
			}

			StringBuilder out = new StringBuilder();

			if (meta.hasLore()) {
				for (String s : meta.getLore()) {
					out.append(s).append(",");
				}
				print.append("{").append("lore").append(":").append(out.toString()).append("}");
			}

			if (meta.hasEnchants()) {
				for (Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
					print.append("{").append("enchantment").append(":").append(e.getKey().getName()).append(":").append(e.getValue()).append("}");
				}
			}

			if (is.getType() == Material.ENCHANTED_BOOK) {
				EnchantmentStorageMeta esMeta = (EnchantmentStorageMeta) meta;
				if (esMeta.hasStoredEnchants()) {
					for (Entry<Enchantment, Integer> e : esMeta.getStoredEnchants().entrySet()) {
						print.append("{").append("enchantment").append(":").append(e.getKey().getName()).append(":").append(e.getValue()).append("}");
					}
				}
			}

		}

		return print.toString();
	}
}
