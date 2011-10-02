
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;


public class ShopState {
    public Player ply;
    public Chest chest;
    public Chest neighbor;
    public ChestShop shop;

    public boolean notified = false;

    public ItemStack[] preInventory;

    public ShopState(Player p, Chest c, ChestShop s) {
        this.ply = p;
        this.chest = c;
        this.shop = s;
        this.neighbor = ShopChest.findNeighbor(c);
        ItemStack[] clonedInventory = ShopChest.cloneInventory(c.getInventory());
        if(neighbor != null) {
            ItemStack[] clonedNeighbor = ShopChest.cloneInventory(neighbor.getInventory());
            this.preInventory = new ItemStack[clonedInventory.length + clonedNeighbor.length];
            for(int i = 0; i < clonedInventory.length; i++) {
                this.preInventory[i] = clonedInventory[i];
            }
            for(int j = 0; j < clonedNeighbor.length; j++) {
                this.preInventory[j+clonedInventory.length] = clonedNeighbor[j];
            }
        }
        else {
            this.preInventory = clonedInventory;
        }
        
    }

    public ItemStack[] getFinalContents() {
        ItemStack[] chestClone = ShopChest.cloneInventory(chest.getInventory());
        ItemStack[] combined;
        if(neighbor != null) {
            ItemStack[] neighborClone = ShopChest.cloneInventory(neighbor.getInventory());
            combined = new ItemStack[chestClone.length + neighborClone.length];
            for(int i = 0; i < chestClone.length; i++) {
                combined[i] = chestClone[i];
            }
            for(int j = 0; j < neighborClone.length; j++) {
                combined[j+chestClone.length] = neighborClone[j];
            }
        }
        else {
            combined = chestClone;
        }
        return combined;
    }
    
}
