

package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */


import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

public class ShopChestPlayerListener extends PlayerListener {

    public static final Logger log = Logger.getLogger("Minecraft");

    // This is what happens when a player opens a chest
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        
        if(event.isCancelled()) { return; }
        // Make sure the previous transaction is completed before the player does anything else
        if(ShopChest.isPlayerInShop(event.getPlayer())) {
            ShopChest.closeChest(event.getPlayer());
        }
        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if(!event.getClickedBlock().getType().equals(Material.CHEST)) {
                return;
            }

            if(ShopChest.isChestShop(event.getClickedBlock().getLocation())) {
                if(!ShopChest.openChest(event.getPlayer(), event.getClickedBlock())) {
                    event.setCancelled(true);
                }
            }
            
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {

        if(ShopChest.isPlayerInShop(event.getPlayer()) && !ShopChest.states.get(event.getPlayer()).notified) {
            ShopChest.states.get(event.getPlayer()).notified = true;
            event.getPlayer().sendMessage(ShopChest.shopChestPrefix + "Don't forget to punch a block to complete the transaction!");
        }

    }

    @Override
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if(ShopChest.isPlayerInShop(event.getPlayer())) {
            event.getPlayer().sendMessage(ShopChest.shopChestPrefix + "Dropping items while shopping is prohibited!");
            event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        if(ShopChest.isPlayerInShop(event.getPlayer())) {
            ShopChest.closeChest(event.getPlayer());
        }
    }


}
