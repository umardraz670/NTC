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
public class productsBeans {
    int rowNo;
    int sku;
    String description;
    int qty;
    float rate,amount;

    public productsBeans(int rowNo, int sku, String description, int qty, float rate, float amount) {
        this.rowNo = rowNo;
        this.sku = sku;
        this.description = description;
        this.qty = qty;
        this.rate = rate;
        this.amount = amount;
    }

    public int getRowNo() {
        return rowNo;
    }

    public void setRowNo(int rowNo) {
        this.rowNo = rowNo;
    }

    public int getSku() {
        return sku;
    }

    public void setSku(int sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
