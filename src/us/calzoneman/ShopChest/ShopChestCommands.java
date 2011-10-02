
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import org.bukkit.entity.Player;
import org.bukkit.block.Chest;
import org.bukkit.Location;

public class ShopChestCommands {

    public static boolean createChestCommand(Player ply, String[] args) {
        if(!ShopChest.hasPermission(ply, "shopchest.create")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You don't have permission to create shops!");
            return false;
        }
        String name = (args.length > 1 ? args[1] : "");
        if(name.equals("")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You must provide a shop name!");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage: /cshop create [ShopName]");
            return false;
        }
        if(ShopChest.selected.containsKey(ply)) {
            Location loc = ShopChest.selected.get(ply);
            if(ShopChest.shops.containsKey(loc)) {
                ply.sendMessage(ShopChest.shopChestPrefix + "The chest you've selected is already a shop!");
                return false;
            }
            ChestShop cs = new ChestShop(ply.getName(), loc, name);
            Chest neighbor = ShopChest.findNeighbor((Chest)(loc.getWorld().getBlockAt(loc).getState()));
            cs.chestNeighbor = neighbor;
            if(neighbor != null) {
                ChestShop csn = new ChestShop(ply.getName(), neighbor.getBlock().getLocation(), name);
                csn.chestNeighbor = (Chest)(loc.getWorld().getBlockAt(loc).getState());
                csn.isNeighbor = true;
                ShopChest.shops.put(neighbor.getBlock().getLocation(), csn);
            }
            ShopChest.shops.put(loc, cs);
            ply.sendMessage(ShopChest.shopChestPrefix + "Successfully created shop " + name);
            if(!ShopChest.owners.containsKey(ply.getName())) {
                ShopChest.owners.put(ply.getName(), new ShopOwner(ply.getName()));
            }
            return true;
        }
        else {
            ply.sendMessage(ShopChest.shopChestPrefix + "You must select a chest first!  Do so by left clicking it.");
            return false;
        }
    }

    public static boolean deleteChestCommand(Player ply, String[] args) {
        if(!ShopChest.hasPermission(ply, "shopchest.delete.own") && !ShopChest.hasPermission(ply, "shopchest.delete.others")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You don't have permission to delete shops!");
            return false;
        }
        String name = (args.length > 1 ? args[1] : "");
        if(!name.equals("")) {
            for(ChestShop cs : ShopChest.shops.values()) {
                if(cs.owner.equals(ply.getName()) && cs.name.equals(name)) {
                    if(!ShopChest.hasPermission(ply, "shopchest.delete.own")) {
                        ply.sendMessage("You don't have permission to delete your own shop!");
                        return false;
                    }
                    ShopChest.shops.remove(cs.loc);
                    if(cs.chestNeighbor != null) {
                            ShopChest.shops.remove(cs.chestNeighbor.getBlock().getLocation());
                    }
                    ply.sendMessage(ShopChest.shopChestPrefix + "Successfully deleted your shop §2" + cs.name);
                    return true;
                }
            }
            ply.sendMessage(ShopChest.shopChestPrefix + "Unable to locate your shop §2" + name);
            return false;
        }
        else {
            if(ShopChest.selected.containsKey(ply)) {
                if(ShopChest.shops.containsKey(ShopChest.selected.get(ply))) {
                    ChestShop cs = ShopChest.shops.get(ShopChest.selected.get(ply));
                    if(!cs.owner.equals(ply.getName()) && !ShopChest.hasPermission(ply, "shopchest.delete.others")) {
                        ply.sendMessage(ShopChest.shopChestPrefix + "You aren't allowed to delete others' shops!");
                        return false;
                    }
                    if(cs.owner.equals(ply.getName()) && !ShopChest.hasPermission(ply, "shopchest.destroy.own")) {
                        ply.sendMessage(ShopChest.shopChestPrefix + "You aren't allowed to delete your own shops!");
                        return false;
                    }
                    else {
                        ShopChest.shops.remove(cs.loc);
                        if(cs.chestNeighbor != null) {
                            ShopChest.shops.remove(cs.chestNeighbor.getBlock().getLocation());
                        }
                        ply.sendMessage(ShopChest.shopChestPrefix + "Successfully deleted " + (cs.owner.equals(ply.getName()) ? "your" : "§2" + cs.owner + "§f's") + " shop §2" + cs.name);
                        return true;
                    }
                }
                else {
                    ply.sendMessage(ShopChest.shopChestPrefix + "You must either provide the name of your shop to delete or select a chest");
                    return false;
                }
            }
            else {
                ply.sendMessage(ShopChest.shopChestPrefix + "You must either provide the name of your shop to delete or select a chest");
                return false;
            }
        }
    }

    public static boolean checkPriceCommand(Player ply, String[] args) {
        if(!ShopChest.hasPermission(ply, "shopchest.checkprice")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You don't have permission to check prices!");
            return false;
        }
        String iddata = (args.length > 1 ? args[1] : "");
        int item = -1;
        try {
            item = ItemData.fromString(iddata);
        }
        catch(Exception e) {
            ply.sendMessage(ShopChest.shopChestPrefix + "Failed to parse item ID");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage: /cshop checkprice [id]:[datavalue]");
            return false;
        }
        ShopPrice standardPrice = ShopChest.getStandardPrice(item);
        ply.sendMessage(ShopChest.shopChestPrefix + "Standard price for §a" + ShopChest.getItemName(item) + "§f: §b" + ShopChest.formatAmount(standardPrice.sell) + "§f, §b" + ShopChest.formatAmount(standardPrice.buy));
        if(ShopChest.selected.containsKey(ply) && ShopChest.shops.containsKey(ShopChest.selected.get(ply))) {
            ChestShop cs = ShopChest.shops.get(ShopChest.selected.get(ply));
            ShopOwner so = ShopChest.owners.get(cs.owner);
            if(so.specialPricing.containsKey(item)) {
                ShopPrice playerPrice = so.specialPricing.get(item);
                ply.sendMessage(ShopChest.shopChestPrefix + "§2" + cs.owner + "§f's price for §a" + ShopChest.getItemName(item) + "§f: §b" + ShopChest.formatAmount(playerPrice.sell) + "§f, §b" + ShopChest.formatAmount(playerPrice.buy));
            }
            if(cs.specialPricing.containsKey(item)) {
                ShopPrice shopPrice = cs.specialPricing.get(item);
                ply.sendMessage(ShopChest.shopChestPrefix + "§2" + cs.name + "§f's price for §a" + ShopChest.getItemName(item) + "§f: §b" + ShopChest.formatAmount(shopPrice.sell) + "§f, §b" + ShopChest.formatAmount(shopPrice.buy));
            }
        }
        else {
            ply.sendMessage(ShopChest.shopChestPrefix + "Select a chest to view player- and shop-specific pricing");
        }
        return true;

    }

    public static boolean setPlayerPriceCommand(Player ply, String[] args) {
        if(!ShopChest.hasPermission(ply, "shopchest.price.player")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You don't have permission to set player prices!");
            return false;
        }
        if(!ShopChest.owners.containsKey(ply.getName())) {
            ShopChest.owners.put(ply.getName(), new ShopOwner(ply.getName()));
        }
        ShopOwner owner = ShopChest.owners.get(ply.getName());
        String iddata = (args.length > 1 ? args[1] : "");
        String price1 = (args.length > 2 ? args[2] : "");
        String price2 = (args.length > 3 ? args[3] : "");
        try {
            int item = ItemData.fromString(iddata);
            float sellprice = Float.parseFloat(price1);
            float buyprice = 0.40f * sellprice;
            if(!price2.equals("")) buyprice = Float.parseFloat(price2);
            owner.addPrice(item, new ShopPrice(sellprice, buyprice));
            ply.sendMessage(ShopChest.shopChestPrefix + "New player price for §a" + ShopChest.getItemName(item) + "§f set to §b" + sellprice + "§f, §b" + buyprice);
            return true;
        }
        catch (Exception e) {
            ply.sendMessage(ShopChest.shopChestPrefix + "Unable to set price.  Please be sure you entered the data correctly.");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage: /cshop setplayerprice [id]:[datavalue] [sellprice] [buyprice]");
            return false;
        }
    }

    public static boolean setShopPriceCommand(Player ply, String[] args) {
        if(!ShopChest.hasPermission(ply, "shopchest.price.shop")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You don't have permission to set shop prices!");
            return false;
        }
        if(args.length == 4) {
            if(!ShopChest.selected.containsKey(ply) || (ShopChest.selected.containsKey(ply) && !ShopChest.shops.containsKey(ShopChest.selected.get(ply)))) {
                ply.sendMessage(ShopChest.shopChestPrefix + "You must either select a shop first or provide a shop name");
                return false;
            }
            ChestShop cs = ShopChest.shops.get(ShopChest.selected.get(ply));
            if(!cs.owner.equals(ply.getName())) {
                ply.sendMessage(ShopChest.shopChestPrefix + "You can't set prices for other peoples' shops!");
                return false;
            }
            String iddata = (args.length > 1 ? args[1] : "");
            String price1 = (args.length > 2 ? args[2] : "");
            String price2 = (args.length > 3 ? args[3] : "");
            try {
                int item = ItemData.fromString(iddata);
                float sellprice = Float.parseFloat(price1);
                float buyprice = 0.40f * sellprice;
                if(!price2.equals("")) buyprice = Float.parseFloat(price2);
                cs.specialPricing.put(item, new ShopPrice(sellprice, buyprice));
                if(cs.chestNeighbor != null) {
                    ChestShop neighbor = ShopChest.shops.get(cs.chestNeighbor.getBlock().getLocation());
                    neighbor.specialPricing.put(item, new ShopPrice(sellprice, buyprice));
                }
                ply.sendMessage(ShopChest.shopChestPrefix + "New shop price for §a" + ShopChest.getItemName(item) + "§f set to §b" + sellprice + "§f, §b" + buyprice);
                return true;
            }
            catch (Exception e) {
                ply.sendMessage(ShopChest.shopChestPrefix + "Unable to set price.  Please be sure you entered the data correctly.");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop setprice [id]:[datavalue] [sellprice] [buyprice]");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop setprice [shopname] [id]:[datavalue] [sellprice] [buyprice]");
                return false;
            }
        }
        else if(args.length == 5) {
            String shopname = args[1];
            String iddata = (args.length > 2 ? args[2] : "");
            String price1 = (args.length > 3 ? args[3] : "");
            String price2 = (args.length > 4 ? args[4] : "");
            int item = -1;
            float sellprice = 0.0f;
            float buyprice = 0.0f;
            try {
                item = ItemData.fromString(iddata);
                sellprice = Float.parseFloat(price1);
                buyprice = 0.40f * sellprice;
                if(!price2.equals("")) buyprice = Float.parseFloat(price2);
            }
            catch(Exception e) {
                ply.sendMessage(ShopChest.shopChestPrefix + "Unable to set price.  Please be sure you entered the data correctly.");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop setprice [id]:[datavalue] [sellprice] [buyprice]");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop setprice [shopname] [id]:[datavalue] [sellprice] [buyprice]");
                return false;
            }
            for(ChestShop cs : ShopChest.shops.values()) {
                if(cs.owner.equals(ply.getName()) && cs.name.equals(shopname)) {
                    cs.specialPricing.put(item, new ShopPrice(sellprice, buyprice));
                    if(cs.chestNeighbor != null) {
                        ChestShop neighbor = ShopChest.shops.get(cs.chestNeighbor.getBlock().getLocation());
                        neighbor.specialPricing.put(item, new ShopPrice(sellprice, buyprice));
                    }
                    ply.sendMessage(ShopChest.shopChestPrefix + "New shop price for §a" + ShopChest.getItemName(item) + "§f set to §b" + sellprice + "§f, §b" + buyprice);
                    return true;
                }
            }
            ply.sendMessage(ShopChest.shopChestPrefix + "Unable to locate your shop §2" + shopname);
            return false;
        }
        else {
            ply.sendMessage(ShopChest.shopChestPrefix + "Unable to set price.  Please be sure you entered the data correctly.");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop setprice [id]:[datavalue] [sellprice] [buyprice]");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop setprice [shopname] [id]:[datavalue] [sellprice] [buyprice]");
            return false;
        }
    }

    public static boolean setSaleCommand(Player ply, String[] args) {
        if(!ShopChest.hasPermission(ply, "shopchest.sale")) {
            ply.sendMessage(ShopChest.shopChestPrefix + "You don't have permission to set sale percentages!");
            return false;
        }
        if(args.length == 1) { // No args given other than subcommand
            ply.sendMessage(ShopChest.shopChestPrefix + "You must provide a sale percentage!");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop sale [shopname] [salepercentage]");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop sale [salepercentage]");
            return false;
        }
        else if(args.length == 2) { // 1 arg given to subcommand, should be percentage
            double percentage = 0.0;
            try {
                percentage = Double.parseDouble(args[1]);
            }
            catch(Exception e) {
                ply.sendMessage(ShopChest.shopChestPrefix + "Unable to parse percentage from \"" + args[1] + "\"");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop sale [shopname] [salepercentage]");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop sale [salepercentage]");
                return false;
            }
            float sale = (float)(percentage / 100.0);
            if(ShopChest.selected.containsKey(ply) && ShopChest.shops.containsKey(ShopChest.selected.get(ply))) {
                ChestShop cs = ShopChest.shops.get(ShopChest.selected.get(ply));
                if(!cs.owner.equals(ply.getName())) {
                    ply.sendMessage(ShopChest.shopChestPrefix + "You can't set sales for other peoples' shops!");
                    return false;
                }
                cs.sale = sale;
                if(cs.chestNeighbor != null) {
                    ChestShop neighbor = ShopChest.shops.get(cs.chestNeighbor.getBlock().getLocation());
                    neighbor.sale = sale;
                }
                ply.sendMessage(ShopChest.shopChestPrefix + "Sale for §2" + cs.name + "§f set to §c" + percentage + "§f% off");
            }
        }
        else if(args.length == 3) { // 2 args given to subcommand, should be shop name and percentage
            String shopname = args[1];
            double percentage = 0.0;
            try {
                percentage = Double.parseDouble(args[2]);
            }
            catch(Exception e) {
                ply.sendMessage(ShopChest.shopChestPrefix + "Unable to parse percentage from \"" + args[2] + "\"");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop sale [shopname] [salepercentage]");
                ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop sale [salepercentage]");
                return false;
            }
            float sale = (float)(percentage / 100.0);
            for(ChestShop cs : ShopChest.shops.values()) {
                if(cs.owner.equals(ply.getName()) && cs.name.equals(shopname)) {
                    cs.sale = sale;
                    if(cs.chestNeighbor != null) {
                        ChestShop neighbor = ShopChest.shops.get(cs.chestNeighbor.getBlock().getLocation());
                        neighbor.sale = sale;
                    }
                    ply.sendMessage(ShopChest.shopChestPrefix + "Sale for §2" + cs.name + "§f set to §c" + percentage + "§f% off");
                        return true;
                }
            }
            ply.sendMessage(ShopChest.shopChestPrefix + "Unable to locate your shop §2" + shopname);
            return false;
        }
        else {
            ply.sendMessage(ShopChest.shopChestPrefix + "Unrecognized number of arguments passed");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop sale [shopname] [salepercentage]");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop sale [salepercentage]");
            return false;
        }
        return true;
    }

    public static boolean addBuyItemCommand(Player ply, String[] args) {

        if(args.length == 1 || args.length > 3) {
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop allowbuy [id]:[data]");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop allowbuy [shopname] [id]:[data]");
            return false;
        }
        ChestShop cs = null;
        int item = -1;
        if(args.length == 2) {
            String iddata = args[1];
            try {
                item = ItemData.fromString(iddata);
            }
            catch(Exception e) {
                ply.sendMessage(ShopChest.shopChestPrefix + "Failed to parse id:data value");
                return false;
            }
            if(ShopChest.selected.containsKey(ply) && ShopChest.shops.containsKey(ShopChest.selected.get(ply))) {
                cs = ShopChest.shops.get(ShopChest.selected.get(ply));
                if(!cs.owner.equals(ply.getName())) {
                    ply.sendMessage(ShopChest.shopChestPrefix + "You can't allow item buying for other peoples' shops!");
                    return false;
                }
            }
            else {
                ply.sendMessage(ShopChest.shopChestPrefix + "You must either select a shop first or provide a shop name");
                return false;
            }
        }
        else if(args.length == 3) {
            String shopname = args[1];
            String iddata = args[2];
            for(ChestShop c : ShopChest.shops.values()) {
                if(c.owner.equals(ply.getName()) && c.name.equals(shopname)) {
                    cs = c;
                }
            }
            try {
                item = ItemData.fromString(iddata);
            }
            catch(Exception e) {
                ply.sendMessage(ShopChest.shopChestPrefix + "Failed to parse id:data value");
                return false;
            }
        }
        else {
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 1: /cshop allowbuy [id]:[data]");
            ply.sendMessage(ShopChest.shopChestPrefix + "Usage 2: /cshop allowbuy [shopname] [id]:[data]");
            return false;
        }

        if(cs == null) {
            ply.sendMessage(ShopChest.shopChestPrefix + "Failed to find your shop");
            return false;
        }

        cs.buyItems.add(item);
        if(cs.chestNeighbor != null) {
            ChestShop neighbor = ShopChest.shops.get(cs.chestNeighbor.getBlock().getLocation());
            neighbor.buyItems.add(item);
        }
        ply.sendMessage(ShopChest.shopChestPrefix + "Allowing players to sell §a" + ShopChest.getItemName(item) + " to your shop §2" + cs.name);
        return true;
    }

    public static boolean helpCommand(Player ply, String[] args) {
        if(args.length == 1 || args.length > 2) {
            ply.sendMessage(ShopChest.shopChestPrefix + "Help Topics: ");
            ply.sendMessage("§c/cshop help create §f- information about creating a shop");
            ply.sendMessage("§c/cshop help delete §f- information about deleting a shop");
            ply.sendMessage("§c/cshop help setprice §f- information about setting shop-specific pricing");
            ply.sendMessage("§c/cshop help setplayerprice §f- information about setting owner-specific pricing");
            ply.sendMessage("§c/cshop help checkprice §f- information about checking pricing");
            ply.sendMessage("§c/cshop help sale §f- information about setting shop sales");
            ply.sendMessage("§c/cshop help allowbuy §f- informaiton about allowing buy items");
        }
        else if(args.length == 2) {
            if(args[1].equals("create")) {
                ply.sendMessage("§c/cshop create [shopname] §f- Turns your last-selected shop into a ShopChest shop");
                ply.sendMessage("Select a chest by left-clicking it");
            }
            else if(args[1].equals("delete")) {
                ply.sendMessage("§c/cshop delete §f- Deletes your last-selected chest shop");
                ply.sendMessage("§c/cshop delete [shopname] §f- Deletes your shop with the given name");
                ply.sendMessage("Select a chest by left-clicking it");
            }
            else if(args[1].equals("setprice")) {
                ply.sendMessage("§c/cshop setprice [id:data] [sellprice] [buyprice] §f- Sets shop-specific pricing for your last selected shop");
                ply.sendMessage("§c/cshop setprice [shopname] [id]:[data] [sellprice] [buyprice] §f- Sets shop-specific pricing for your shop with the given name");
                ply.sendMessage("[sellprice] is the price a shopper pays to buy it from your shop, [buyprice] is the price a shopper gets for selling it to your shop");
            }
            else if(args[1].equals("setplayerprice")) {
                ply.sendMessage("§c/cshop setplayerprice [id]:[data] [sellprice] [buyprice] §f- Sets player-specific pricing");
                ply.sendMessage("[sellprice] is the price a shopper pays to buy it from your shop, [buyprice] is the price a shopper gets for selling it to your shop");
            }
            else if(args[1].equals("checkprice")) {
                ply.sendMessage("§c/cshop checkprice [id]:[data] §f- Checks the standard price for an item (Also checks player and shop prices if you've selected a shop beforehand");
            }
            else if(args[1].equals("sale")) {
                ply.sendMessage("§c/cshop sale [percentage] §f- Applies a shop-wide discount of [percentage]% off");
            }
            else if(args[1].equals("allowbuy")) {
                ply.sendMessage("§c/cshop allowbuy [id:data] §f- Allows players to sell [id:data] to your selected shop");
                ply.sendMessage("§c/cshop allowbuy [shopname] [id:data] §f- Allows players to sell [id:data] to your shop with the given name");
            }
        }
        return true;
    }
}
