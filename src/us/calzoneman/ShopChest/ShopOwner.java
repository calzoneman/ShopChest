
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */

import java.util.HashMap;

public class ShopOwner {
    public String name;
    
    public HashMap<Integer, ShopPrice> specialPricing = new HashMap<Integer, ShopPrice>();

    public ShopOwner(String name) {
        this.name = name;
    }

    public void addPrice(int item, ShopPrice price) {
        this.specialPricing.put(item, price);
    }
    
    public boolean hasPrice(int item) {
        return specialPricing.containsKey(item);
    }
    
    public ShopPrice getPrice(int item) {
        if(!hasPrice(item)) {
            return null;
        }
        else {
            return specialPricing.get(item);
        }
    }
    
}
