

package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileReader;

public class ShopChest extends JavaPlugin {
    public static final Logger log = Logger.getLogger("Minecraft");
    public static HashMap<Integer, ShopPrice> standardPricing = new HashMap<Integer, ShopPrice>();
    public static HashMap<Integer, String> itemNames = new HashMap<Integer, String>();
    public static HashMap<Location, ChestShop> shops = new HashMap<Location, ChestShop>();
    public static HashMap<Player, ShopState> states = new HashMap<Player, ShopState>();
    public static HashMap<Player, Location> selected = new HashMap<Player, Location>();
    public static HashMap<String, ShopOwner> owners = new HashMap<String, ShopOwner>();
    public static String iConomyVersion = null;

    public static final String shopChestPrefix =  "§2[ShopChest]§f ";

    /**
     * onEnable - Performs necessary tasks to initialize the plugin
     */
    @Override
    public void onEnable() {
        
        PluginManager pm = this.getServer().getPluginManager();
        ShopChestPlayerListener pl = new ShopChestPlayerListener();
        ShopChestBlockListener bl = new ShopChestBlockListener();
        ShopChestPluginListener plugin_listener = new ShopChestPluginListener();
        pm.registerEvent(Type.PLAYER_INTERACT, pl, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_MOVE, pl, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_DROP_ITEM, pl, Priority.Normal, this);
        pm.registerEvent(Type.PLAYER_QUIT, pl, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_DAMAGE, bl, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_PLACE, bl, Priority.Normal, this);
        pm.registerEvent(Type.PLUGIN_ENABLE, plugin_listener, Priority.Normal, this);


        loadStandardPricing("plugins/ShopChest/prices.txt");
        loadShops("plugins/ShopChest/shops.txt");
        loadShopOwners("plugins/ShopChest/owners.txt");

        log.info("[ShopChest] ShopChest Enabled!");
    }

    /**
     * onDisable - Performs necessary tasks to shutdown the plugin
     */
    @Override
    public void onDisable() {
        log.info("[ShopChest] Shutting down");
        saveShops("plugins/ShopChest/shops.txt");
        saveShopOwners("plugins/ShopChest/owners.txt");
    }

    //---[INFORMATIONAL GETTERS]-----------------------------------------------------------------------------------------

    /**
     * getStandardPrice - Attempts to load the price of an item
     * @param item The integral representation of an item ID / data pair (see: ItemData.java)
     * @return A ShopPrice object with appropriate price values, or a ShopPrice with price values 0.0f, 0.0f if the price data cannot be loaded
     */
    public static ShopPrice getStandardPrice(int item) {
        if(standardPricing.containsKey(item)) {
            return standardPricing.get(item);
        }
        else {
            return new ShopPrice(0.0f, 0.0f);
        }
    }

    /**
     * getItemName - Attempts to recall the name of an item
     * @param item The integral representation of an item ID / data pair (see: ItemData.java)
     * @return The name of the item according to the price file, or the Material name if the loaded values don't contain a name for that item
     */
    public static String getItemName(int item) {
        if(itemNames.containsKey(item)) {
            return itemNames.get(item);
        }
        else {
            Material m = Material.getMaterial(ItemData.getId(item));
            if(m != null) {
                return m.name();
            }
            else {
                return "#" + ItemData.toString(item);
            }
        }
    }

    /**
     * getOwner - Attempts to retrieve the ShopOwner object with the given name
     * @param name The name of the ShopOwner to be retrieved
     * @return The ShopOwner object with the given name, or null if such a ShopOwner does not exist
     */
    public static ShopOwner getOwner(String name) {
        return owners.get(name);
    }

    /**
     * isChestShop - Determines whether a chest shop exists at a given location
     * @param loc The Location of the chest
     * @return True if a chest shop exists at the location, false otherwise
     */
    public static boolean isChestShop(Location loc) {
        return shops.containsKey(loc);
    }

    /**
     * isPlayerInShop - Determines whether the specified player is engaged in a transaction
     * @param p The Player who needs to be checked
     * @return True if the player is engaged in an unfinished transaction, false otherwise
     */
    public static boolean isPlayerInShop(Player p) {
        return states.containsKey(p);
    }

    /**
     * hasPermission - Attempts to determine whether a player has access to a given node
     * @param p The player for which the permission is being determined
     * @param permission The permission node to check for
     * @return True if the player has the permission, false otherwise
     */
    public static boolean hasPermission(Player p, String permission) {
        // Try Permissions plugin first
        if(ShopChestPermissionsLink.initialized) {
            return ShopChestPermissionsLink.hasPermission(p, permission);
        }
        // Then Bukkit built-in permissions
        else if(p.hasPermission(permission)) {
            return true;
        }
        // Finally, defaults - Only OPs can delete other people's chests
        else {
            if(permission.equals("shopchest.delete.others") && !p.isOp()) {
                return false;
            }
            else {
                return true;
            }
        }
    }

    //---[PLAYER INTERACTION   ]-----------------------------------------------------------------------------------------

    /**
     * showShopInfo - Displays information about the selected shop to the player (Prices, Items that can be sold, etc)
     * @param p The Player to send the information to
     * @param cs The ChestShop about which the Player is to be shown information
     */
    public static void showShopInfo(Player p, ChestShop cs) {
        String msg = shopChestPrefix + "You've selected ";
        if(p.getName().equals(cs.owner)) {
            msg += "your ";
        }
        else {
            msg += "§2" + cs.owner + "§f's ";
        }
        msg += "shop §2" + cs.name;
        p.sendMessage(msg);
        if(cs.sale > 0.0f) {
            p.sendMessage("This shop is currently having a sale of §c" + (int)(cs.sale * 100) + "% §foff");
        }
        String prices = "You can §abuy§f: ";
        ItemStack[] totalContents;
        ItemStack[] chestContents = combineStacks(cloneInventory(cs.chestBlock.getInventory()));
        if(cs.chestNeighbor != null) {
            ItemStack[] neighborContents = combineStacks(cloneInventory(cs.chestNeighbor.getInventory()));
            totalContents = new ItemStack[chestContents.length + neighborContents.length];
            for(int i = 0; i < chestContents.length; i++) {
                totalContents[i] = chestContents[i];
            }
            for(int j = 0; j < neighborContents.length; j++) {
                totalContents[j + chestContents.length] = neighborContents[j];
            }
            totalContents = combineStacks(totalContents);
        }
        else {
            totalContents = chestContents;
        }
        for(ItemStack it : totalContents) {
            it.setAmount(1);
            prices += "§b" + getItemName(ItemData.fromItemStack(it)) + ": §e" + formatAmount(cs.getPrice(it, false)) + "§f, ";
        }
        p.sendMessage(prices);

        String buyPrices = "You can §csell§f: ";
        for(int item : cs.buyItems) {
            buyPrices += "§b" + getItemName(item) + ": §e" + formatAmount(cs.getPrice(item, true)) + "§f, ";
        }
        p.sendMessage(buyPrices);
    }

    /**
     * openChest - Begins a transaction for a Player who has selected a chest
     * @param p The Player entering the transaction
     * @param b The Block the player selected
     * @return True if the transaction is initiated successfully, false otherwise
     */
    public static boolean openChest(Player p, Block b) { 
        if(!shops.containsKey(b.getLocation())) {
            log.severe("[ShopChest] ChestShop the player is opening doesn't exist!  This should never happen!");
            return false;
        }
        else if(shops.get(b.getLocation()).lock != null) {
            p.sendMessage(shopChestPrefix + "That chest is already locked to §3" + shops.get(b.getLocation()).lock.getName());
            return false;
        }
        else if(!hasPermission(p, "shopchest.shop")) {
            p.sendMessage(shopChestPrefix + "You don't have permission to shop!");
            return false;
        }
        else {
            ChestShop cs = shops.get(b.getLocation());
            Chest c = (Chest)b.getState();
            cs.lock = p;
            ShopState state = new ShopState(p, c, cs);
            states.put(p, state);
            p.sendMessage(shopChestPrefix + "Welcome to §2" + (cs.owner.equals(p.getName()) ? "your" : cs.owner) + "§f's shop §2" + cs.name + "§f!");
            if(cs.sale > 0.0f) {
                p.sendMessage(shopChestPrefix + "This shop is currently having a sale of §c" + (int)(cs.sale * 100) + "%§f off!");
            }
            return true;
        }
    }

    /**
     * closeChest - Closes out the Player's transaction with a chest
     * @param p The Player for whom the transaction is being closed
     */
    public static void closeChest(Player p) {
        ShopState state = states.get(p);
        if(state.shop.owner.equals(p.getName())) {
            p.sendMessage(shopChestPrefix + "No Transaction");
            state.shop.lock = null;
            states.remove(p);
            return;
        }

        // Determine the transaction
        ItemStack[] after = state.getFinalContents();

        // Combine stacks for ease of transaction handling
        ItemStack[] combinedAfter = combineStacks(after);
        HashMap<Integer, ItemStack> combinedBefore = combineStacksHashMap(state.preInventory);
        

        ArrayList<ItemStack> added = new ArrayList<ItemStack>(); // Stuff added rather than taken

        for(ItemStack it : combinedAfter) {
            if(it == null || it.getAmount() == 0) continue;
            int item = ItemData.fromItemStack(it);
            if(!combinedBefore.containsKey(item)) {
                added.add(it);
                continue;
            }
            ItemStack itBefore = combinedBefore.get(item);
            int delta = itBefore.getAmount() - it.getAmount();
            if(delta >= 0) {
                itBefore.setAmount(delta);
                combinedBefore.put(item, itBefore);
            }
            else {
                itBefore.setAmount(0);
                combinedBefore.put(item, itBefore);
                ItemStack add = itBefore.clone();
                add.setAmount(-delta);
                added.add(add);
            }
        }

        ArrayList<ItemStack> buy = new ArrayList<ItemStack>();
        for(ItemStack it : added) {
            int item = ItemData.fromItemStack(it);
            if(state.shop.buyItems.contains(item)) {
                buy.add(it);
            }
            else {
                p.sendMessage(shopChestPrefix + "This shop doesn't allow you to sell §6" + getItemName(ItemData.fromItemStack(it))+"§f!");
                p.getInventory().addItem(splitStack(it));
                if(state.chest.getInventory().removeItem(splitStack(it)) != null && state.neighbor != null) {
                    state.neighbor.getInventory().removeItem(splitStack(it));
                }
            }
        }

        // Process sales [if there are any]
        if(combinedBefore.size() > 0) {
            ShopChestTransaction selltrans = state.shop.processTransaction(p, combinedBefore.values().toArray(new ItemStack[combinedBefore.size()]), false);
            if(iConomyVersion == null) {
                log.severe("[ShopChest] Failed to process transaction: no iConomy version!");
            }
            else if(iConomyVersion.equals("5")) {
                if(ShopChestIConomy5Link.getBalance(p.getName()) < selltrans.finalAmount) {
                    p.sendMessage(shopChestPrefix + "You can't afford that!");
                    for(ItemStack it : combinedBefore.values()) {
                        if(state.chest.getInventory().addItem(splitStack(it)) != null) {
                            state.neighbor.getInventory().addItem(splitStack(it));
                        }
                        p.getInventory().removeItem(splitStack(it));
                    }

                    p.sendMessage(shopChestPrefix + "Transaction cancelled.");
                }
                else {
                    ShopChestIConomy5Link.setBalance(state.shop.owner, ShopChestIConomy5Link.getBalance(state.shop.owner) + selltrans.finalAmount);
                    ShopChestIConomy5Link.setBalance(p.getName(), ShopChestIConomy5Link.getBalance(p.getName()) - selltrans.finalAmount);
                    for(String s : selltrans.playerTransactionLines) {
                        p.sendMessage(s);
                    }
                    if(Bukkit.getServer().getPlayer(state.shop.owner) != null) {
                        Player owner = Bukkit.getServer().getPlayer(state.shop.owner);
                        for(String s : selltrans.ownerTransactionLines) {
                            owner.sendMessage(s);
                        }
                    }
                    try {
                    PrintWriter pw = new PrintWriter(new FileWriter("plugins/ShopChest/transactions.log", true));
                    for(String s : selltrans.logTransactionLines) {
                        pw.println(s);
                    }
                    pw.flush();
                    pw.close();
                    }
                    catch(Exception e) {
                        log.severe("[ShopChest] Failed to write transaction data to log file!");
                        log.severe(e.toString());
                    }
                }
            }
        }

        // Process purchases [from player]
        if(buy.size() > 0) {
            ShopChestTransaction buytrans = state.shop.processTransaction(p, buy.toArray(new ItemStack[buy.size()]), true);
            if(iConomyVersion == null) {
                log.severe("[ShopChest] Failed to process transaction: no iConomy version!");
            }
            else if(iConomyVersion.equals("5")) {
                if(ShopChestIConomy5Link.getBalance(state.shop.owner) < buytrans.finalAmount) {
                    p.sendMessage(shopChestPrefix + "Sorry, the shop can't pay you that much!");
                    for(ItemStack it : buy) {
                        if(state.chest.getInventory().removeItem(splitStack(it)) != null) {
                            state.neighbor.getInventory().removeItem(splitStack(it));
                        }
                        p.getInventory().addItem(splitStack(it));
                    }

                    p.sendMessage(shopChestPrefix + "Transaction cancelled.");
                }
                else {
                    ShopChestIConomy5Link.setBalance(state.shop.owner, ShopChestIConomy5Link.getBalance(state.shop.owner) - buytrans.finalAmount);
                    ShopChestIConomy5Link.setBalance(p.getName(), ShopChestIConomy5Link.getBalance(p.getName()) + buytrans.finalAmount);
                    for(String s : buytrans.playerTransactionLines) {
                        p.sendMessage(s);
                    }
                    if(Bukkit.getServer().getPlayer(state.shop.owner) != null) {
                        Player owner = Bukkit.getServer().getPlayer(state.shop.owner);
                        for(String s : buytrans.ownerTransactionLines) {
                            owner.sendMessage(s);
                        }
                    }
                    try {
                        PrintWriter pw = new PrintWriter(new FileWriter("plugins/ShopChest/transactions.log", true));
                        for(String s : buytrans.logTransactionLines) {
                            pw.println(s);
                        }
                        pw.flush();
                        pw.close();
                    }
                    catch(Exception e) {
                        log.severe("[ShopChest] Failed to write transaction data to log file");
                        log.severe(e.toString());
                    }
                }
            }
            else if(iConomyVersion.equals("6")) { // That son of a bitch renamed everything in iConomy 6 so I have to duplicate this crap
                if(ShopChestIConomy6Link.getBalance(state.shop.owner) < buytrans.finalAmount) {
                    p.sendMessage(shopChestPrefix + "Sorry, the shop can't pay you that much!");
                    for(ItemStack it : buy) {
                        if(state.chest.getInventory().removeItem(splitStack(it)) != null) {
                            state.neighbor.getInventory().removeItem(splitStack(it));
                        }
                        p.getInventory().addItem(splitStack(it));
                    }

                    p.sendMessage(shopChestPrefix + "Transaction cancelled.");
                }
                else {
                    ShopChestIConomy6Link.setBalance(state.shop.owner, ShopChestIConomy5Link.getBalance(state.shop.owner) - buytrans.finalAmount);
                    ShopChestIConomy6Link.setBalance(p.getName(), ShopChestIConomy5Link.getBalance(p.getName()) + buytrans.finalAmount);
                    for(String s : buytrans.playerTransactionLines) {
                        p.sendMessage(s);
                    }
                    if(Bukkit.getServer().getPlayer(state.shop.owner) != null) {
                        Player owner = Bukkit.getServer().getPlayer(state.shop.owner);
                        for(String s : buytrans.ownerTransactionLines) {
                            owner.sendMessage(s);
                        }
                    }
                    try {
                        PrintWriter pw = new PrintWriter(new FileWriter("plugins/ShopChest/transactions.log", true));
                        for(String s : buytrans.logTransactionLines) {
                            pw.println(s);
                        }
                        pw.flush();
                        pw.close();
                    }
                    catch(Exception e) {
                        log.severe("[ShopChest] Failed to write transaction data to log file");
                        log.severe(e.toString());
                    }
                }
            }
        }

        if(combinedBefore.size() == 0 && buy.size() == 0) {
            p.sendMessage(shopChestPrefix + "No Transaction");
        }

        
        state.shop.lock = null;
        states.remove(p);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player ply = null;
        if(sender instanceof Player) {
            ply = (Player)sender;
        }
        else {
            sender.sendMessage("[ShopChest] Shops can only be manipulated by players!");
            return false;
        }

        if(command.getName().equalsIgnoreCase("cshop") || command.getName().equalsIgnoreCase("cs")) {
            String subcmd = (args.length > 0 ? args[0].toLowerCase() : "");
            if(subcmd.equals("create")) {
                return ShopChestCommands.createChestCommand(ply, args);
            }
            else if(subcmd.equals("delete")) {
                return ShopChestCommands.deleteChestCommand(ply, args);
            }
            else if(subcmd.equals("setplayerprice")) {
                return ShopChestCommands.setPlayerPriceCommand(ply, args);
            }
            else if(subcmd.equals("checkprice")) {
                return ShopChestCommands.checkPriceCommand(ply, args);
            }
            else if(subcmd.equals("sale")) {
                return ShopChestCommands.setSaleCommand(ply, args);
            }
            else if(subcmd.equals("setprice")) {
                return ShopChestCommands.setShopPriceCommand(ply, args);
            }
            else if(subcmd.equals("allowbuy")) {
                return ShopChestCommands.addBuyItemCommand(ply, args);
            }
            else if(subcmd.equals("help")) {
                return ShopChestCommands.helpCommand(ply, args);
            }
        }
        return true;
    }

    //---[UTILITIES           ]-----------------------------------------------------------------------------------------

    /**
     * combineStacks - Combines ItemStacks by adding up the quantities of ItemStacks with the same type
     * @param initial An array of the contents to be combined
     * @return An array of the contents combined together -- NOTE: Does not adhere to Minecraft stack sizes, this is to make transaction data easier to work with
     */
    public static ItemStack[] combineStacks(ItemStack[] initial) {
        HashMap<Integer, ItemStack> stacks = new HashMap<Integer, ItemStack>();
        for(ItemStack it : initial) {
            if(it == null || it.getAmount() == 0) continue;
            int item = ItemData.fromItemStack(it);
            if(stacks.containsKey(item)) {
                ItemStack i = stacks.get(item);
                i.setAmount(i.getAmount() + it.getAmount());
                stacks.put(item, i);
            }
            else {
                stacks.put(item, it.clone());
            }

        }
        return stacks.values().toArray(new ItemStack[stacks.size()]);
    }

    /**
     * combineStacks - Combines ItemStacks by adding up the quantities of ItemStacks with the same type
     * @param initial An array of the contents to be combined
     * @return An HashMap of the contents combined together, keyed by item (see ItemData.java for item information) -- NOTE: Does not adhere to Minecraft stack sizes, this is to make transaction data easier to work with
     */
    public static HashMap<Integer, ItemStack> combineStacksHashMap(ItemStack[] initial) {
        HashMap<Integer, ItemStack> stacks = new HashMap<Integer, ItemStack>();
        for(ItemStack it : initial) {
            if(it == null || it.getAmount() == 0) continue;
            int item = ItemData.fromItemStack(it);
            if(stacks.containsKey(item)) {
                ItemStack i = stacks.get(item);
                i.setAmount(i.getAmount() + it.getAmount());
                stacks.put(item, i);
            }
            else {
                stacks.put(item, it.clone());
            }

        }
        return stacks;
    }

    /**
     * splitStack - Accepts a single ItemStack and splits it into an array of ItemStacks each with a maximum amount of 64
     * @param initial The ItemStack to split
     * @return An array of the split stacks
     */
    public static ItemStack[] splitStack(ItemStack initial) {
        ItemStack initialCopy = initial.clone();
        if(initial.getAmount() <= 64) return new ItemStack[] { initial };
        ItemStack[] after = new ItemStack[initial.getAmount() / 64];
        int i = 0;
        while(initial.getAmount() > 64) {
            ItemStack stack64 = initial.clone();
            stack64.setAmount(64);
            after[i] = stack64;
            initial.setAmount(initial.getAmount() - 64);
            i++;
        }
        after[i] = initial;
        initial = initialCopy;
        return after;
    }

    /**
     * cloneInventory - Clones the ItemStacks in an Inventory so the clones may be modified without modifying the original
     * @param i The Inventory to clone
     * @return An array of clones of the ItemStacks in the Inventory
     */
    public static ItemStack[] cloneInventory(Inventory i) {
        ItemStack[] inv = i.getContents();
        ItemStack[] stack = new ItemStack[inv.length];
        
        for(int j = 0; j < stack.length; j++) {
            if(inv[j] != null) {
                stack[j] = inv[j].clone();
            }
        }
        
        return stack;
    }

    /**
     * findNeighbor - Finds the neighbor chest for a double chest
     * @param orig The Chest to find the neighbor for
     * @return The Chest object for the neighboring chest, or null if there is none
     */
    public static Chest findNeighbor(Chest orig) {
        Location loc = orig.getBlock().getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        if(orig.getWorld().getBlockTypeIdAt(x + 1, y, z) == 54) {
            return (Chest)orig.getWorld().getBlockAt(x + 1, y, z).getState();
        }
        else if(orig.getWorld().getBlockTypeIdAt(x - 1, y, z) == 54) {
            return (Chest)orig.getWorld().getBlockAt(x - 1, y, z).getState();
        }
        else if(orig.getWorld().getBlockTypeIdAt(x, y, z + 1) == 54) {
            return (Chest)orig.getWorld().getBlockAt(x, y, z + 1).getState();
        }
        else if(orig.getWorld().getBlockTypeIdAt(x, y, z - 1) == 54) {
            return (Chest)orig.getWorld().getBlockAt(x, y, z - 1).getState();
        }

        return null;
    }

    /**
     * formatAmount - Formats a currency amount in a String
     * @param amount - The amount to format
     * @return A formatted string of the amount
     */
    public static String formatAmount(double amount) {
        if(ShopChestIConomy5Link.initialized) {
            return ShopChestIConomy5Link.format(amount);
        }
        else if(ShopChestIConomy6Link.initialized) {
            return ShopChestIConomy6Link.format(amount);
        }
        else {
            return "$" + amount;
        }
    }

    //---[SAVING/LOADING     ]-----------------------------------------------------------------------------------------

    /**
     * loadStandardPricing - Attempts to load price values from file
     * @param filename The path to the file to be loaded
     * @return True if the prices are loaded successfully, false otherwise
     */
    public static boolean loadStandardPricing(String filename) {

        try {

            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = in.readLine();
            while(line != null) {
                // ID:Data;Name;sellprice;buyprice
                if(line.equals("")) continue;
                String[] args = line.split(";");
                int item;
                if(args[0].indexOf(":") >= 0) {
                    String iid = args[0].substring(0, args[0].indexOf(":"));
                    String idata = args[0].substring(args[0].indexOf(":") + 1);
                    item = ItemData.fromIdAndData(Integer.parseInt(iid), Byte.parseByte(idata));
                }
                else {
                    item = ItemData.fromIdAndData(Integer.parseInt(args[0]), (byte)0);
                }
                ShopPrice price = new ShopPrice(Float.parseFloat(args[2]), Float.parseFloat(args[3]));
                standardPricing.put(item, price);
                itemNames.put(item, args[1]);
                line = in.readLine();
            }
            in.close();
            log.info("[ShopChest] Loaded pricing from file: " + filename);
            return true;
        }
        catch (Exception e) {
            log.severe("[ShopChest] Unable to load prices!");
            log.severe(e.toString());
            return false;
        }

        

    }

    /**
     * saveShops - Attempts to flush existing shops to disk
     * @param filename The path to the file being saved to
     * @return True if the save is successful, false otherwise
     */
    public static boolean saveShops(String filename) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(filename));
            for(ChestShop cs : shops.values()) {
                if(cs.isNeighbor) continue;
                out.println("Shop " + cs.name);
                out.println("owner: " + cs.owner);
                out.println("loc: " + cs.loc.getWorld().getName() + ";" + cs.loc.getX() + ";" + cs.loc.getY() + ";" + cs.loc.getZ());
                out.println("sale: " + cs.sale);
                out.println("SpecialPricing");
                for(int item : cs.specialPricing.keySet()) {
                    ShopPrice sp = cs.specialPricing.get(item);
                    out.println(ItemData.getId(item) + ":" + ItemData.getData(item) + "=" + sp.sell + "," + sp.buy);
                }
                out.println("EndSpecialPricing");
                out.println("AllowedBuy");
                for(int item : cs.buyItems) {
                    out.println(ItemData.getId(item) + ":" + ItemData.getData(item));
                }
                out.println("EndAllowedBuy");
                out.println("EndShop");
            }
            out.flush();
            out.close();
            log.info("[ShopChest] Flushed shops to file: " + filename);
            return true;
        }
        catch(Exception e) {
            log.severe("[ShopChest] Unable to save shops!");
            log.severe(e.toString());
            return false;
        }
    }

    /**
     * loadShops - Attempts to load shop data from disk
     * @param filename The path to the file to be loaded
     * @return True if the load operation is successful, false otherwise
     */
    public static boolean loadShops(String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = in.readLine();
            while(line != null) {
                if(line.isEmpty()) continue;
                if(line.startsWith("Shop")) {
                    String name = line.substring(5).trim();
                    String owner = "";
                    Location loc = null;
                    float sale = 0.0f;
                    HashMap<Integer, ShopPrice> specialPricing = new HashMap<Integer, ShopPrice>();
                    ArrayList<Integer> buyItems = new ArrayList<Integer>();
                    while(!line.startsWith("EndShop") && line != null) {
                        if(line.isEmpty()) continue;
                        line = in.readLine();
                        if(line.startsWith("owner:")) {
                            owner = line.substring(7).trim();
                        }
                        else if(line.startsWith("sale:")) {
                            sale = Float.parseFloat(line.substring(6).trim());
                        }
                        else if(line.startsWith("loc:")) {
                            String[] locData = line.substring(4).trim().split(";");
                            loc = new Location(Bukkit.getWorld(locData[0]), Double.parseDouble(locData[1]), Double.parseDouble(locData[2]), Double.parseDouble(locData[3]));
                        }
                        else if(line.startsWith("SpecialPricing")) {
                            line = in.readLine();
                            while(!line.startsWith("EndSpecialPricing") && line != null) {
                                if(line.isEmpty()) continue;
                                String[] itemAndPrice = line.split("=");
                                int item = ItemData.fromString(itemAndPrice[0].trim());
                                String[] sellBuy = itemAndPrice[1].trim().split(",");
                                ShopPrice sp = new ShopPrice(Float.parseFloat(sellBuy[0]), Float.parseFloat(sellBuy[1]));
                                specialPricing.put(item, sp);
                                line = in.readLine();
                            }
                        }
                        else if(line.startsWith("AllowedBuy")) {
                            line = in.readLine();
                            while(!line.startsWith("EndAllowedBuy") && line != null) {
                                if(line.isEmpty()) continue;
                                buyItems.add(ItemData.fromString(line.trim()));
                                line = in.readLine();
                            }
                        }
                    }
                    ChestShop cs = new ChestShop(owner, loc, name);
                    cs.sale = sale;
                    cs.buyItems = buyItems;
                    cs.specialPricing = specialPricing;
                    Chest neighbor = findNeighbor((Chest)(loc.getWorld().getBlockAt(loc).getState()));
                    cs.chestNeighbor = neighbor;
                    if(neighbor != null) {
                        ChestShop csn = new ChestShop(owner, neighbor.getBlock().getLocation(), name);
                        csn.sale = sale;
                        csn.buyItems = buyItems;
                        csn.specialPricing = specialPricing;
                        csn.chestNeighbor = (Chest)(loc.getWorld().getBlockAt(loc).getState());
                        csn.isNeighbor = true;
                        shops.put(neighbor.getBlock().getLocation(), csn);
                    }
                    shops.put(loc, cs);
                }
                line = in.readLine();
            }

            in.close();
            log.info("[ShopChest] Loaded shops from file: " + filename);
            return true;
        }
        catch(Exception e) {
            log.severe("[ShopChest] Unable to load shops from file!");
            log.severe(e.toString());
            return false;
        }
    }

    /**
     * loadShopOwners - Attempts to load ShopOwner data from disk
     * @param filename The file path to read from
     * @return True if the load is successful, false otherwise
     */
    public boolean loadShopOwners(String filename) {
        try {
             BufferedReader in = new BufferedReader(new FileReader(filename));
             String line = in.readLine();
             while(line != null) {
                if(line.isEmpty()) continue;
                else if(line.startsWith("Owner")) {
                    String ownername = line.substring(6).trim();
                    HashMap<Integer, ShopPrice> specialPricing = new HashMap<Integer, ShopPrice>();
                    while(!line.startsWith("EndOwner")) {
                        line = in.readLine();
                        if(line.isEmpty()) continue;
                        else if(line.startsWith("SpecialPricing")) {
                            line = in.readLine();
                            while(!line.startsWith("EndSpecialPricing") && line != null) {
                                if(line.isEmpty()) continue;
                                String[] itemAndPrice = line.split("=");
                                int item = ItemData.fromString(itemAndPrice[0].trim());
                                String[] sellBuy = itemAndPrice[1].trim().split(",");
                                ShopPrice sp = new ShopPrice(Float.parseFloat(sellBuy[0]), Float.parseFloat(sellBuy[1]));
                                specialPricing.put(item, sp);
                                line = in.readLine();
                            }
                        }
                    }
                    ShopOwner owner = new ShopOwner(ownername);
                    owner.specialPricing = specialPricing;
                    owners.put(ownername, owner);
                }
                line = in.readLine();
             }
             in.close();
             log.info("[ShopChest] Loaded shop owners from file: " + filename);
            return true;
        }
        catch(Exception e) {
            log.severe("[ShopChest] Unable to load shopowners from file!");
            log.severe(e.toString());
            return false;
        }
    }

    /**
     * saveShopOwners - Attempts to flush ShopOwner data to disk
     * @param filename The file path to write to
     * @return True if the save is successful, false otherwise
     */
    public boolean saveShopOwners(String filename) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(filename));
            for(ShopOwner so : owners.values()) {
                out.println("Owner " + so.name);
                out.println("SpecialPricing");
                for(int item : so.specialPricing.keySet()) {
                    ShopPrice sp = so.specialPricing.get(item);
                    out.println(ItemData.getId(item) + ":" + ItemData.getData(item) + "=" + sp.sell + "," + sp.buy);
                }
                out.println("EndSpecialPricing");
                out.println("EndOwner");
            }
            out.flush();
            out.close();
            log.info("[ShopChest] Saved shop owners to file: " + filename);
            return true;
        }
        catch (Exception e) {
            log.severe("[ShopChest] Unable to save shop owners to disk!");
            log.severe(e.toString());
            return false;
        }
    }

}
