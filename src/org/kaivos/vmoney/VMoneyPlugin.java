package org.kaivos.vmoney;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VMoneyPlugin extends JavaPlugin implements Listener {
	
	private HashMap<String, Integer> kysyntä;
	private HashMap<String, Integer> tarjonta;
	private HashMap<String, Integer> hinta;
	
	private Economy economy;
	
	@Override
	public void onEnable() {
		super.onEnable();
		getServer().getPluginManager().registerEvents(this, this);
		
		kysyntä = lataa(this.getDataFolder().getAbsolutePath() + "/kys.dat");
		tarjonta = lataa(this.getDataFolder().getAbsolutePath() + "/tarj.dat");
		hinta = lataa(this.getDataFolder().getAbsolutePath() + "/hint.dat");
		
		setupEconomy();
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		tallenna(kysyntä, this.getDataFolder().getAbsolutePath() + "/kys.dat");
		tallenna(tarjonta, this.getDataFolder().getAbsolutePath() + "/tarj.dat");
		tallenna(hinta, this.getDataFolder().getAbsolutePath() + "/hint.dat");
	}
	
	private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void kyltti(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN
					|| event.getClickedBlock().getType() == Material.SIGN_POST
					|| event.getClickedBlock().getType() == Material.SIGN) {
				Sign block = (Sign) event.getClickedBlock().getState();

				if (!block.getLine(0).equalsIgnoreCase("[vbuy]") && !block.getLine(0).equalsIgnoreCase("[vsell]")) return;
				
				String artikkeli = block.getLine(1);
				
				if (!artikkeli.matches("\\d*:(\\d*)?x\\d")) return;
				String[] splitattu = artikkeli.split("[:x]");
				ItemStack esine = new ItemStack(
						Integer.parseInt(splitattu[0]),
						Integer.parseInt(splitattu[1]),
						Short.parseShort(splitattu[2]));
				
				if (!hinta.containsKey(artikkeli)) {
					return;
				}
				
				int perushinta = hinta.get(artikkeli);
				double nykyinen = perushinta*kerroin(artikkeli);
				
				if (block.getLine(0).equalsIgnoreCase("[vbuy]") && economy.getBalance(player.getName()) > nykyinen) {
					player.getInventory().addItem(esine);
					economy.withdrawPlayer(player.getName(), nykyinen);
					
					lisääKysyntää(artikkeli, 1);
					
					double uusiHinta = perushinta*kerroin(artikkeli);
					block.setLine(2, ""+uusiHinta);
					block.setLine(3, "§a+" + (uusiHinta/nykyinen*100d-100d) + "%");
				}
				else if (block.getLine(0).equalsIgnoreCase("[vsell]") && player.getInventory().contains(esine)) {
					economy.depositPlayer(player.getName(), nykyinen/4);
					player.getInventory().remove(esine);
					
					lisääTarjontaa(artikkeli, 1);
					
					double uusiHinta = perushinta*kerroin(artikkeli);
					block.setLine(2, ""+uusiHinta);
					block.setLine(3, "§c-" + (uusiHinta/nykyinen*100d-100d) + "%");
				}
				
				
			}
		}
	}
	
	@EventHandler
	public void kyltti(BlockPlaceEvent event) {
		Player player = event.getPlayer();

		if (event.getBlock() != null) {
			if (event.getBlock().getType() == Material.WALL_SIGN
					|| event.getBlock().getType() == Material.SIGN_POST
					|| event.getBlock().getType() == Material.SIGN) {
				Sign block = (Sign) event.getBlock().getState();

				if (!block.getLine(0).equalsIgnoreCase("[vbuy]") && !block.getLine(0).equalsIgnoreCase("[vsell]")) return;
				
				if (player.hasPermission("vmoney.createsign"));
				
				String artikkeli = block.getLine(1);
				
				if (!artikkeli.matches("\\d*:(\\d*)?x\\d")) return;
				
				int perushinta = Integer.parseInt(block.getLine(0));
				
				hinta.put(artikkeli, perushinta);
				kysyntä.put(artikkeli, 1);
				tarjonta.put(artikkeli, 1);
				
				block.setLine(3, "§aOK");
			}
		}
	}
	
	public double kerroin(String nimi) {
		return (double)kysyntä(nimi)/(double)tarjonta(nimi);
	}
	
	private int kysyntä(String nimi) {
		if (!kysyntä.containsKey(nimi)) kysyntä.put(nimi, 1);
		return kysyntä.get(nimi);
	}
	
	private int tarjonta(String nimi) {
		if (!kysyntä.containsKey(nimi)) kysyntä.put(nimi, 1);
		return kysyntä.get(nimi);
	}
	
	private void lisääKysyntää(String nimi, int määrä) {
		kysyntä.put(nimi, kysyntä(nimi)+määrä);
	}
	
	private void lisääTarjontaa(String nimi, int määrä) {
		tarjonta.put(nimi, tarjonta(nimi)+määrä);
	}
	
	public void tallenna(HashMap<String, Integer> obj,String path)
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
	public HashMap<String, Integer> lataa(String path)
	{
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			ois.close();
			return (HashMap<String, Integer>) result;
		} catch (Exception ex) {
			ex.printStackTrace();
			return new HashMap<String, Integer>();
		}
	}
	
}
