
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

// NOTE: These two imports require Permissions.jar to be referenced
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import java.util.logging.Logger;

public class ShopChestPermissionsLink {
    public static boolean initialized = false;
    public static PermissionHandler handler;
    public static final Logger log = Logger.getLogger("Minecraft");

    public static boolean hasPermission(Player ply, String permission) {
        return handler.has(ply, permission);
    }

    public static boolean init() {
        Plugin permissions = Bukkit.getServer().getPluginManager().getPlugin("Permissions");
        if(permissions != null) {
            handler  = ((Permissions)permissions).getHandler();
            initialized = true;
            log.info("[ShopChest] Permissions plugin linked");
            return true;
        }
        else {
            log.severe("[ShopChest] Unable to link Permissions plugin");
            return false;
        }
    }
}
