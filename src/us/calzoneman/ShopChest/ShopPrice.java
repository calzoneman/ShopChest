
package us.calzoneman.ShopChest;

/**
 *
 * @author calzoneman
 */
public class ShopPrice {
    public float buy = 0.0f; // Buy from player
    public float sell = 0.0f; // Sell to player

    public ShopPrice(float sell, float buy) {
        this.buy = buy;
        this.sell = sell;
    }
}
