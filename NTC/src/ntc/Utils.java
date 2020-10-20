/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

import java.text.ParseException;
import javax.swing.text.MaskFormatter;

/**
 *
 * @author Dell 7010
 */
public class Utils {
    public static String SALE_DAY;
    public static float CASH_SALE=0;
    public static float CREDIT_SALE=0;
    public static float SALE_LOSS=0;
    public static float SALE_RETURN=0;
    public static long USER_ID=0;
    public static MaskFormatter formatCellNumber() {
        MaskFormatter formatter = null;
        try {
            formatter = new MaskFormatter("03#########");
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return formatter;
    }
    
}
