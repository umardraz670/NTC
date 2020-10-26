/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

/**
 *
 * @author Dell 7010
 */
public class products {
    private String description;
    private int qty;
    private float rate,amount;

    public products(String Description, int qty, float rate, float amount) {
        this.description = Description;
        this.qty = qty;
        this.rate = rate;
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String Description) {
        this.description = Description;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    
    
    
    
}
