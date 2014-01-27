package org.kaivos.vmoney;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VMoneyPlugin extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		super.onEnable();
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void greetingSign(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == Material.WALL_SIGN
					|| event.getClickedBlock().getType() == Material.SIGN_POST
					|| event.getClickedBlock().getType() == Material.SIGN) {
				Sign block = (Sign) event.getClickedBlock().getState();

				if (block.getLine(0).equalsIgnoreCase("[vbuy]")) {
					// TODO: Buy
				}
				else if (block.getLine(0).equalsIgnoreCase("[vsell]")) {
					// TODO: Sell
				}
			}
		}
	}
	
	
}
