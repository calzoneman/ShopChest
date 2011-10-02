
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 * 
 * This class is provided as an interface for converting item types for easy storage as Integers.
 */
import org.bukkit.inventory.ItemStack;

public class ItemData {
    public static int fromIdAndData(int id, byte data) {
        return (id << 4) + data;
    }

    public static int fromItemStack(ItemStack it) {
        if(it.getData() != null) {
            return fromIdAndData(it.getTypeId(), it.getData().getData());
        }
        else {
            return fromIdAndData(it.getTypeId(), (byte)0);
        }
    }

    public static int fromString(String s) {
        String[] parts = s.split(":");
        if(parts.length <= 0) { return -1; }
        int id = Integer.parseInt(parts[0]);
        byte data = 0;
        if(parts.length == 2) {
            data = Byte.parseByte(parts[1]);
        }
        return fromIdAndData(id, data);
    }

    public static String toString(int item) {
        String str = ((Integer)getId(item)).toString();
        if(getData(item) != 0) {
            str += ":" + getData(item);
        }
        return str;
    }
    
    public static int getId(int item) {
        return item >> 4;
    }
    
    public static byte getData(int item) {
        return (byte)(item % 16);
    }
}
