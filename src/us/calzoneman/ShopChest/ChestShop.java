
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import org.bukkit.block.Chest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.ArrayList;

public class ChestShop {

    // General Information
    public Location loc;
    public String name;
    public String owner;
    public Chest chestBlock;
    public Chest chestNeighbor;
    public boolean isNeighbor = false; // If this is true, the chest is a neighbor and shouldn't be flushed to disk as doing so would create duplicate shop entries

    // Transaction data
    public Player lock = null;

    // Price data
    public float sale = 0.0f;
    public HashMap<Integer, ShopPrice> specialPricing = new HashMap<Integer, ShopPrice>();
    public ArrayList<Integer> buyItems = new ArrayList<Integer>();

    // Items for which durability applies
    public static final ArrayList<Material> hasDamage = new ArrayList<Material>();
    
    static {
        hasDamage.add(Material.WOOD_SWORD);
        hasDamage.add(Material.WOOD_SPADE);
        hasDamage.add(Material.WOOD_PICKAXE);
        hasDamage.add(Material.WOOD_AXE);
        hasDamage.add(Material.STONE_SWORD);
        hasDamage.add(Material.STONE_SPADE);
        hasDamage.add(Material.STONE_PICKAXE);
        hasDamage.add(Material.STONE_AXE);
        hasDamage.add(Material.IRON_SWORD);
        hasDamage.add(Material.IRON_SPADE);
        hasDamage.add(Material.IRON_PICKAXE);
        hasDamage.add(Material.IRON_AXE);
        hasDamage.add(Material.GOLD_SWORD);
        hasDamage.add(Material.GOLD_SPADE);
        hasDamage.add(Material.GOLD_PICKAXE);
        hasDamage.add(Material.GOLD_AXE);
        hasDamage.add(Material.DIAMOND_SWORD);
        hasDamage.add(Material.DIAMOND_SPADE);
        hasDamage.add(Material.DIAMOND_PICKAXE);
        hasDamage.add(Material.DIAMOND_AXE);
        hasDamage.add(Material.FLINT_AND_STEEL);
        hasDamage.add(Material.CHAINMAIL_HELMET);
        hasDamage.add(Material.CHAINMAIL_CHESTPLATE);
        hasDamage.add(Material.CHAINMAIL_LEGGINGS);
        hasDamage.add(Material.CHAINMAIL_BOOTS);
        hasDamage.add(Material.LEATHER_HELMET);
        hasDamage.add(Material.LEATHER_CHESTPLATE);
        hasDamage.add(Material.LEATHER_LEGGINGS);
        hasDamage.add(Material.LEATHER_BOOTS);
        hasDamage.add(Material.IRON_HELMET);
        hasDamage.add(Material.IRON_CHESTPLATE);
        hasDamage.add(Material.IRON_LEGGINGS);
        hasDamage.add(Material.IRON_BOOTS);
        hasDamage.add(Material.GOLD_HELMET);
        hasDamage.add(Material.GOLD_CHESTPLATE);
        hasDamage.add(Material.GOLD_LEGGINGS);
        hasDamage.add(Material.GOLD_BOOTS);
        hasDamage.add(Material.DIAMOND_HELMET);
        hasDamage.add(Material.DIAMOND_CHESTPLATE);
        hasDamage.add(Material.DIAMOND_LEGGINGS);
        hasDamage.add(Material.DIAMOND_BOOTS);
        hasDamage.add(Material.SHEARS);
        hasDamage.add(Material.FISHING_ROD);
    }

    public ChestShop(String owner, Location loc, String name) {
        this.owner = owner;
        this.loc = loc;
        this.name = name;
        this.chestBlock = (Chest)loc.getBlock().getState();
    }

    /**
     *
     * @param it The ItemStack we are pricing
     * @param selling true = selling to shop, false = buying from shop
     * @return price The price of the ItemStack
     */
    public float getPrice(ItemStack it, boolean selling) {
        Material m = it.getType();
        int item = ItemData.fromItemStack(it);
        int quantity = it.getAmount();
        float percentDamage = 0.0f;
        if(hasDamage.contains(m)) {
            percentDamage = it.getDurability() / (float)m.getMaxDurability();
        }

        ShopPrice sprice = ShopChest.getStandardPrice(item);
        ShopOwner so = ShopChest.getOwner(owner);
        if(so != null && so.hasPrice(item)) {
            sprice = so.getPrice(item);
        }
        if(specialPricing.containsKey(item)) {
            sprice = specialPricing.get(item);
        }

        float price = sprice.sell;
        if(selling) {
            price = sprice.buy;
        }
        
        price *= quantity;
        price -= price*percentDamage;
        price -= price*sale;
        return price;

    }

    public float getPrice(int item, boolean selling) {
        ShopPrice sprice = ShopChest.getStandardPrice(item);
        ShopOwner so = ShopChest.getOwner(owner);
        if(so != null && so.hasPrice(item)) {
            sprice = so.getPrice(item);
        }
        if(specialPricing.containsKey(item)) {
            sprice = specialPricing.get(item);
        }

        float price = sprice.sell;
        if(selling) {
            price = sprice.buy;
        }

        price -= price*sale;

        return price;
    }
    
    public ShopChestTransaction processTransaction(Player p, ItemStack[] it, boolean selling) {
        ShopChestTransaction transaction = new ShopChestTransaction();
        float priceTotal = 0.0f;
        transaction.playerTransactionLines.add(ShopChest.shopChestPrefix + (selling ? "Selling" : "Buying") + " transaction for §3" + this.owner + "§f's shop §3" + this.name + "§f: ");
        // Combine stacks for ease of transaction handling
        HashMap<Integer, ItemStack> itemTotals = ShopChest.combineStacksHashMap(it);

        for(ItemStack i : itemTotals.values()) {
            float price = getPrice(i, selling);
            String transactionLine = "§aBUY §6";
            if(selling) {
                transactionLine = "§cSELL §6";
            }
            transactionLine += ShopChest.getItemName(ItemData.fromItemStack(i));
            transactionLine += "§f x " + i.getAmount();
            transactionLine += " (§b$" + price + "§f)";
            transaction.playerTransactionLines.add(transactionLine);
            transaction.ownerTransactionLines.add(ShopChest.shopChestPrefix + "Shop §3" + this.name + "§f " + (selling ? "< " : "> ") + "§3" + p.getName() + (selling ? "§cSELL §6" : "§aBUY §6") + ShopChest.getItemName(ItemData.fromItemStack(i)) + "§f x " + i.getAmount() + " (§b$" + price + "§f)");
            transaction.logTransactionLines.add("[" + this.owner + ":" + this.name + "] " + (selling ? "< " : "> ") + p.getName() + (selling ? " SELL " : " BUY ") + ShopChest.getItemName(ItemData.fromItemStack(i)) + " x " + i.getAmount() + " (" + price + ")");
            priceTotal += price;
        }
        transaction.playerTransactionLines.add("------------------------------------------");
        transaction.playerTransactionLines.add("TOTAL: §b$" + priceTotal);
        transaction.playerTransactionLines.add("------------------------------------------");
        transaction.finalAmount = priceTotal;
        return transaction;
    }

}
