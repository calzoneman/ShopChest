
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */
import org.bukkit.Bukkit;
import java.util.logging.Logger;

import com.iCo6.iConomy;
import com.iCo6.system.Accounts;
import com.iCo6.system.Account;
import com.iCo6.system.Holdings;

public class ShopChestIConomy6Link {
    public static boolean initialized = false;
    public static final Logger log = Logger.getLogger("Minecraft");
    public static Accounts accounts = null;

    public static boolean init() {
        if(Bukkit.getServer().getPluginManager().getPlugin("iConomy") != null) {
            initialized = true;
            ShopChest.iConomyVersion = "6";
            accounts = new Accounts();
            log.info("[ShopChest] Linked to iConomy 6");
            return true;
        }
        else {
            log.severe("[ShopChest] Failed to link iConomy 6");
            return false;
        }
    }

    public static String format(double amount) {
        return iConomy.format(amount);
    }

    public static double getBalance(String account) {
        Account acc = accounts.get(account);
        Holdings holdings = (acc != null ? acc.getHoldings() : null);
        if(acc == null || holdings == null) {
            log.severe("[ShopChest] Failed to retrieve iConomy holdings: " + account);
            return 0.0;
        }
        else {
            try {
                return holdings.getBalance();
            }
            catch (Exception e) {
                log.severe("[ShopChest] Failed to retrieve iConomy balance: " + account);
                log.severe(e.toString());
                return 0.0;
            }
        }
    }

    public static boolean setBalance(String account, double balance) {
        Account acc = accounts.get(account);
        Holdings holdings = (acc != null ? acc.getHoldings() : null);
        if(acc == null || holdings == null) {
            log.severe("[ShopChest] Failed to retrieve iConomy holdings: " + account);
            return false;
        }
        else {
            try {
                holdings.setBalance(balance);
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
