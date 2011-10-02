
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */
import org.bukkit.Bukkit;
import java.util.logging.Logger;

import com.iConomy.iConomy;
import com.iConomy.system.Account;
import com.iConomy.system.Holdings;

public class ShopChestIConomy5Link {
    public static boolean initialized = false;
    public static final Logger log = Logger.getLogger("Minecraft");

    public static boolean init() {
        if(Bukkit.getServer().getPluginManager().getPlugin("iConomy") != null) {
            initialized = true;
            ShopChest.iConomyVersion = "5";
            log.info("[ShopChest] Linked to iConomy 5");
            return true;
        }
        else {
            log.severe("[ShopChest] Failed to link iConomy 5");
            return false;
        }
    }

    public static String format(double amount) {
        return iConomy.format(amount);
    }

    public static double getBalance(String account) {
        Account acc = iConomy.getAccount(account);
        Holdings holdings = (acc != null ? acc.getHoldings() : null);
        if(acc == null || holdings == null) {
            log.severe("[ShopChest] Failed to retrieve iConomy holdings: " + account);
            return 0.0;
        }
        else {
            try {
                return holdings.balance();
            }
            catch (Exception e) {
                log.severe("[ShopChest] Failed to retrieve iConomy balance: " + account);
                log.severe(e.toString());
                return 0.0;
            }
        }
    }

    public static boolean setBalance(String account, double balance) {
        Account acc = iConomy.getAccount(account);
        Holdings holdings = (acc != null ? acc.getHoldings() : null);
        if(acc == null || holdings == null) {
            log.severe("[ShopChest] Failed to retrieve iConomy holdings: " + account);
            return false;
        }
        else {
            try {
                holdings.set(balance);
                return true;
            }
            catch (Exception e) {
                log.severe("[ShopChest] Failed to set iConomy balance: " + account);
                log.severe(e.toString());
                return false;
            }
        }
    }
}
