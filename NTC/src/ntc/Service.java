/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Dell 7010
 */
public class Service extends TimerTask{

    public Service() {
        
    }    
    @Override
    public void run() {
        try (Connection con=db.getConnection()){
            con.createStatement().executeUpdate("ALTER SESSION SET NLS_DATE_FORMAT = 'YYYY-MM-DD'");
            PreparedStatement ps=con.prepareStatement("select * from sales where sale_day=?");
            ps.setString(1, new Date(new java.util.Date().getTime()).toString());
            ResultSet res=ps.executeQuery();
            if(!res.next()){
                ps=con.prepareStatement("insert into sales(sale_day) values(?)");
                ps.setString(1, new Date(new java.util.Date().getTime()).toString());
                if(ps.executeUpdate()!=1){
                    JOptionPane.showMessageDialog(null, "Error While Creating Sale Day \nCall Your Vander");
                    System.exit(-1);
                }
                con.commit();
                System.out.println("New Sale Day Added ");
                Utils.SALE_DAY=new Date(new java.util.Date().getTime()).toString();
            }else{
                Utils.SALE_DAY=res.getString("sale_day");
                System.out.println(Utils.SALE_DAY);
                Utils.CASH_SALE=res.getFloat("cash_sale");
                Utils.CREDIT_SALE=res.getFloat("credit_sale");
                Utils.SALE_LOSS=res.getFloat("sale_loss");
                Utils.SALE_RETURN=res.getFloat("sale_return");                
                System.out.println("Sale Day Already Saved");
            }
        } catch (SQLException |ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            System.exit(-1);
        }
    }
    
    
}
