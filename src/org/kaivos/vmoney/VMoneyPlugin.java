package org.kaivos.vmoney;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.util.HashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VMoneyPlugin extends JavaPlugin implements Listener {
	
	private HashMap<String, Double> kysyntä;
	private HashMap<String, Double> tarjonta;
	private HashMap<String, Double> hinta;
	
	private Economy economy;
	
	@Override
	public void onEnable() {
		super.onEnable();
		getServer().getPluginManager().registerEvents(this, this);
		
		kysyntä = lataa("kys.dat");
		tarjonta = lataa("tarj.dat");
		hinta = lataa("hint.dat");
		
		setupEconomy();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		tallenna(kysyntä, "kys.dat");
		tallenna(tarjonta, "tarj.dat");
		tallenna(hinta, "hint.dat");
	}
	
	private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equalsIgnoreCase("vmoney")) {
			if (args.length != 1) return false;
			switch (args[0]) {
			case "save":
				tallenna(kysyntä, "kys.dat");
				tallenna(tarjonta, "tarj.dat");
				tallenna(hinta, "hint.dat");
				break;

			default:
				return false;
			}
		}
		return false;
	}
	
	private static final double VERO = 0.9;
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void kyltti(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		if (event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN
					|| event.getClickedBlock().getType() == Material.SIGN_POST
					|| event.getClickedBlock().getType() == Material.SIGN) {
				Sign block = (Sign) event.getClickedBlock().getState();

				if (!block.getLine(0).equalsIgnoreCase("[vshop]")) {
					return;
				}
				
				String artikkeli = block.getLine(1);
				
				if (!artikkeli.matches("\\d*:(\\d*)?x(\\d*)")) {
					player.sendMessage("§f[§bVMoney§f] §eBad format!");
					return;
				}
				String[] splitattu = artikkeli.split("[:x]");
				ItemStack esine = new ItemStack(
						Integer.parseInt(splitattu[0]),
						Integer.parseInt(splitattu[2]),
						Short.parseShort(splitattu[1]));
				
				if (!hinta.containsKey(artikkeli)) {
					if (!player.hasPermission("vmoney.createsign")) {
						player.sendMessage("§f[§bVMoney§f] §eYou lack permission!");
						return;
					}
					double perushinta = Double.parseDouble(block.getLine(2));
					
					hinta.put(artikkeli, perushinta);
					kysyntä.put(artikkeli, 1d);
					tarjonta.put(artikkeli, 1d);
					
					player.sendMessage("§f[§bVMoney§f] §eShop created!");
					return;
				}
				
				double perushinta = hinta.get(artikkeli);
				double nykyinen = perushinta*kerroin(artikkeli);
				
				NumberFormat nfFormat = NumberFormat.getInstance();
				nfFormat.setMaximumFractionDigits(3);
				nfFormat.setGroupingUsed(false);
				
				if (event.getAction() == Action.RIGHT_CLICK_BLOCK && economy.getBalance(player.getName()) > nykyinen) {
					player.getInventory().addItem(esine);
					player.updateInventory();
					economy.withdrawPlayer(player.getName(), nykyinen);
					
					lisääKysyntää(artikkeli, 0.1);
					lisääTarjontaa(artikkeli, -0.1);
					
					double uusiHinta = perushinta*kerroin(artikkeli);
					block.setLine(2, ""+nfFormat.format(uusiHinta));
					block.setLine(3, "§" + (uusiHinta < perushinta ? "c" : "a") + nfFormat.format(uusiHinta/perushinta*100d-100d) + "%");
					block.update();
					
					player.sendMessage("§f[§bVMoney§f] §eItem bought: " + esine.toString() + " (" + nfFormat.format(nykyinen) +")!");
				}
				else if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getInventory().containsAtLeast(esine, esine.getAmount())) {
					economy.depositPlayer(player.getName(), nykyinen*VERO);
					player.getInventory().removeItem(esine);
					player.updateInventory();
					
					lisääTarjontaa(artikkeli, 0.1);
					
					double uusiHinta = perushinta*kerroin(artikkeli);
					block.setLine(2, ""+nfFormat.format(uusiHinta));
					block.setLine(3, "§" + (uusiHinta < perushinta ? "c" : "a") + nfFormat.format(uusiHinta/perushinta*100d-100d) + "%");
					block.update();
					
					player.sendMessage("§f[§bVMoney§f] §eItem sold: " + esine.toString() + " (" + nfFormat.format(nykyinen*VERO) +")!");
				}
				else {
					block.setLine(2, ""+nfFormat.format(nykyinen));
					block.setLine(3, "§" + (nykyinen < perushinta ? "c" : "a") + nfFormat.format(nykyinen/perushinta*100d-100d) + "%");
					block.update();
				}
				
				
			}
		}
	}
	
	@EventHandler
	public void kyltti(SignChangeEvent event) {
		Player player = event.getPlayer();

		if (event.getBlock() != null) {
			if (event.getBlock().getType() == Material.WALL_SIGN
					|| event.getBlock().getType() == Material.SIGN_POST
					|| event.getBlock().getType() == Material.SIGN) {
				if (!event.getLine(0).equalsIgnoreCase("[vshop]")) return;
				
				if (!player.hasPermission("vmoney.createsign")) {
					player.sendMessage("§f[§bVMoney§f] §eYou lack permission!");
					return;
				}
				
				String artikkeli = event.getLine(1);
				
				if (!artikkeli.matches("\\d*:(\\d*)?x\\d*")) {
					player.sendMessage("§f[§bVMoney§f] §eBad format!");
					return;
				}
				
				double perushinta;
				try {
					perushinta = Double.parseDouble(event.getLine(2));
				} catch (NumberFormatException ex) {
					perushinta = hinta.get(artikkeli);
				}
				
				hinta.put(artikkeli, perushinta);
				kysyntä.put(artikkeli, 1d);
				tarjonta.put(artikkeli, 1d);
				
				event.setLine(3, "§aOK");
				
				player.sendMessage("§f[§bVMoney§f] §eShop created!");
			}
		}
	}
	
	public double kerroin(String nimi) {
		return (double)kysyntä(nimi)/(double)tarjonta(nimi);
	}
	
	private double kysyntä(String nimi) {
		if (!kysyntä.containsKey(nimi)) kysyntä.put(nimi, 1d);
		return kysyntä.get(nimi);
	}
	
	private double tarjonta(String nimi) {
		if (!tarjonta.containsKey(nimi)) tarjonta.put(nimi, 1d);
		return tarjonta.get(nimi);
	}
	
	private void lisääKysyntää(String nimi, double määrä) {
		kysyntä.put(nimi, (kysyntä(nimi)+kysyntä(nimi)*(määrä/100d)));
	}
	
	private void lisääTarjontaa(String nimi, double määrä) {
		tarjonta.put(nimi, (tarjonta(nimi)+tarjonta(nimi)*(määrä/100d)));
	}
	
	public void tallenna(HashMap<String, Double> obj,String path)
	{
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(obj);
			oos.flush();
			oos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, Double> lataa(String path)
	{
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			ois.close();
			return (HashMap<String, Double>) result;
		} catch (Exception ex) {
			return new HashMap<String, Double>();
		}
	}
	
}
