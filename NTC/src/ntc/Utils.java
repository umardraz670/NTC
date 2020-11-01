/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Dell 7010
 */
public class Utils {

    public static String SALE_DAY;
    public static float CASH_SALE = 0;
    public static float CREDIT_SALE = 0;
    public static float SALE_LOSS = 0;
    public static float SALE_RETURN = 0;
    public static long USER_ID = 0;

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

    public static boolean cashSale(float netBill) {
        try (Connection con = db.getConnection()) {
            PreparedStatement ps = con.prepareStatement("update sales set cash_sale=cash_sale+? where sale_day=?");
            ps.setFloat(1, netBill);
            ps.setString(2, SALE_DAY);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            System.exit(-1);
        }
        return false;
    }

    public static void printInvoice(Map<String, Object> parameters, int pages) {
        try {
            JasperReport report = JasperCompileManager.compileReport(new FileInputStream(new File("./src/reports/saleInvoice.jrxml")));
            JasperPrint print = JasperFillManager.fillReport(report, parameters, new JREmptyDataSource());
            JasperViewer.viewReport(print,false);
//            if (pages == 2) {
//                JasperPrintManager.printReport(print, false);
//                JasperPrintManager.printReport(print, false);
//            } else {
//                JasperPrintManager.printReport(print, false);
//            }
            //JasperPrintManager.printReport(print, false);
        } catch (JRException | FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    public static void removeAllRows(DefaultTableModel model) {
        int rowCount = model.getRowCount();
        //Remove rows one by one from the end of the table
        for (int i = rowCount - 1; i >= 0; i--) {
            model.removeRow(i);
        }
    }
    public static String todayDate(){
        return new Date(new java.util.Date().getTime()).toString();
    }
}
