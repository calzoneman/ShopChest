

package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerListener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public class ShopChestPluginListener extends ServerListener {

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        // iConomy
        Plugin iConomy = Bukkit.getServer().getPluginManager().getPlugin("iConomy");
        if(iConomy != null) {
            if(iConomy.getDescription().getVersion().startsWith("5")) {
                ShopChestIConomy5Link.init();
            }
            else if(iConomy.getDescription().getVersion().startsWith("6")) {
                ShopChestIConomy6Link.init();
            }
            else {
                ShopChest.log.severe("[ShopChest] Unknown version of iConomy!");
            }
        }
    }
}
