package org.kaivos.vmoney;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VMoneyPlugin extends JavaPlugin implements Listener {
	
	private HashMap<String, Integer> kysyntä;
	private HashMap<String, Integer> tarjonta;
	private HashMap<String, Integer> hinta;
	
	@Override
	public void onEnable() {
		super.onEnable();
		getServer().getPluginManager().registerEvents(this, this);
		
		kysyntä = lataa(this.getDataFolder().getAbsolutePath() + "/kys.dat");
		tarjonta = lataa(this.getDataFolder().getAbsolutePath() + "/tarj.dat");
		hinta = lataa(this.getDataFolder().getAbsolutePath() + "/hint.dat");
	}
	
	@Override
	public void onDisable() {
		super.onDisable();
		
		tallenna(kysyntä, this.getDataFolder().getAbsolutePath() + "/kys.dat");
		tallenna(tarjonta, this.getDataFolder().getAbsolutePath() + "/tarj.dat");
		tallenna(hinta, this.getDataFolder().getAbsolutePath() + "/hint.dat");
	}
	
	@EventHandler
	public void kyltti(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN
					|| event.getClickedBlock().getType() == Material.SIGN_POST
					|| event.getClickedBlock().getType() == Material.SIGN) {
				Sign block = (Sign) event.getClickedBlock().getState();

				String artikkeli = block.getLine(1);
				if (!hinta.containsKey(artikkeli)) {
					return;
				}
				
				int perushinta = hinta.get(artikkeli);
				
				if (block.getLine(0).equalsIgnoreCase("[vbuy]")) {
					// TODO: Buy
					
					lisääKysyntää(artikkeli, 1);
					block.setLine(2, ""+perushinta*kysyntä(artikkeli));
				}
				else if (block.getLine(0).equalsIgnoreCase("[vsell]")) {
					// TODO: Sell
					
					lisääTarjontaa(artikkeli, 1);
					block.setLine(2, ""+perushinta*kysyntä(artikkeli));
				}
			}
		}
	}
	
	public int kerroin(String nimi) {
		return kysyntä(nimi)/tarjonta(nimi);
	}
	
	private int kysyntä(String nimi) {
		if (!kysyntä.containsKey(nimi)) kysyntä.put(nimi, 0);
		return kysyntä.get(nimi);
	}
	
	private int tarjonta(String nimi) {
		if (!kysyntä.containsKey(nimi)) kysyntä.put(nimi, 0);
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
