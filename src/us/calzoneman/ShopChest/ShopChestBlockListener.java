package us.calzoneman.ShopChest;

import org.bukkit.entity.Player;
import org.bukkit.block.Chest;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 *
 * @author calzoneman
 */
public class ShopChestBlockListener extends BlockListener {
    @Override
    public void onBlockDamage(BlockDamageEvent event) {
        Player p = event.getPlayer();
        
        if(p == null) {
            if(ShopChest.isChestShop(event.getBlock().getLocation())) {
                event.setCancelled(true);
            }
            return;
        }
        if(ShopChest.isPlayerInShop(p)) {
            ShopChest.closeChest(p);
        }
        if(ShopChest.isChestShop(event.getBlock().getLocation())) {
            ChestShop cs = ShopChest.shops.get(event.getBlock().getLocation());
            ShopChest.showShopInfo(p, cs);
            if(!cs.owner.equals(p.getName())) {
                event.setCancelled(true);
            }
        }
        ShopChest.selected.put(p, event.getBlock().getLocation());

    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if(ShopChest.isChestShop(event.getBlock().getLocation())) {
            if(event.getPlayer() == null || !ShopChest.shops.get(event.getBlock().getLocation()).owner.equals(event.getPlayer().getName())) {
                event.setCancelled(true);
            }
        }

    }

    @Override
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;
        if(event.getBlock().getTypeId() != 54) return;
        Chest chest = (Chest)event.getBlock().getState();
        Chest neighbor = ShopChest.findNeighbor(chest);
        if(neighbor != null) {
            if(ShopChest.shops.containsKey(neighbor.getBlock().getLocation())) {
                ChestShop shop = ShopChest.shops.get(neighbor.getBlock().getLocation());
                shop.chestNeighbor = chest;
                ChestShop added = new ChestShop(shop.owner, chest.getBlock().getLocation(), shop.name);
                added.buyItems = shop.buyItems;
                added.specialPricing = shop.specialPricing;
                added.sale = shop.sale;
                added.isNeighbor = true;
                ShopChest.shops.put(chest.getBlock().getLocation(), added);
            }
        }
    }
}
