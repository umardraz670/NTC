/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Umar Rehman
 */
public class DailySale extends javax.swing.JInternalFrame {

    /**
     * Creates new form DailySale
     */
    private final String invoicesSQL = "select \n"
            + "si.invoice,u.username,c.cust_name,si.total,si.discount,si.gross_total,si.freight,si.net_total \n"
            + "from \n"
            + "sale_invoices si,sales s,users u,customers c \n"
            + "where\n"
            + "s.sale_day=si.fk_sale_day and \n"
            + "si.fk_user=u.id and \n"
            + "si.fk_customer=c.cust_id and \n"
            + "s.sale_day=? ";
    private final Map<String, Integer> allUsers;
    private DefaultTableModel model;
    private final JFrame parent;

    public DailySale(JFrame parent) {
        initComponents();
        this.parent=parent;
        allUsers = new HashMap<>();
        saleDay.setDate(new Date());
        model = (DefaultTableModel) jXTable1.getModel();
        getUsers();
    }

    private void getUsers() {
        try (Connection con = db.getConnection()) {
            ResultSet res = con.createStatement().executeQuery("select id,username from users");
            int id = 0;
            String str = null;
            while (res.next()) {
                id = res.getInt(1);
                str = res.getString(2);
                users.addItem(str);
            }
            allUsers.put(str, id);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPane, e.getMessage());
            dispose();
        }
    }

    private void getInvoices() {
        System.out.println(new SimpleDateFormat("YYYY-MM-dd").format(saleDay.getDate()));
        try (Connection con = db.getConnection()) {
            if (users.getSelectedIndex() > 0) {
                PreparedStatement ps = con.prepareStatement(invoicesSQL+" and u.id=?");
                ps.setString(1, new SimpleDateFormat("YYYY-MM-dd").format(saleDay.getDate()));
                ps.setInt(2, allUsers.get(users.getSelectedItem().toString()));
                ResultSet res = ps.executeQuery();
                int serial = 1;
                Utils.removeAllRows(model);
                while (res.next()) {
                    model.addRow(new Object[]{serial++,
                        res.getLong(1),
                        res.getString(2),
                        res.getString(3),
                        res.getFloat(4),
                        res.getFloat(5),
                        res.getFloat(6),
                        res.getFloat(7),
                        res.getFloat(8)});
                }
                ps=con.prepareStatement("select cash_sale,credit_sale,sale_return,sale_loss from sales where sale_day=?");
                ps.setString(1, new SimpleDateFormat("YYYY-MM-dd").format(saleDay.getDate()));
                res=ps.executeQuery();
                if(res.next()){
                    cashSale.setText(""+res.getFloat(1));
                    creditSale.setText(""+res.getFloat(2));
                    saleReturn.setText(""+res.getFloat(3));
                    saleLoss.setText(""+res.getFloat(4));
                }
            } else {
                PreparedStatement ps = con.prepareStatement(invoicesSQL);
                ps.setString(1, new SimpleDateFormat("YYYY-MM-dd").format(saleDay.getDate()));
                ResultSet res = ps.executeQuery();
                int serial = 1;
                Utils.removeAllRows(model);
                while (res.next()) {
                    model.addRow(new Object[]{serial++,
                        res.getLong(1),
                        res.getString(2),
                        res.getString(3),
                        res.getFloat(4),
                        res.getFloat(5),
                        res.getFloat(6),
                        res.getFloat(7),
                        res.getFloat(8)});
                }
                ps=con.prepareStatement("select cash_sale,credit_sale,sale_return,sale_loss from sales where sale_day=?");
                ps.setString(1, new SimpleDateFormat("YYYY-MM-dd").format(saleDay.getDate()));
                res=ps.executeQuery();
                if(res.next()){
                    cashSale.setText(""+res.getFloat(1));
                    creditSale.setText(""+res.getFloat(2));
                    saleReturn.setText(""+res.getFloat(3));
                    saleLoss.setText(""+res.getFloat(4));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(rootPane, e.getMessage());
            dispose();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jXTable1 = new org.jdesktop.swingx.JXTable();
        jPanel1 = new javax.swing.JPanel();
        cashSale = new javax.swing.JLabel();
        creditSale = new javax.swing.JLabel();
        saleReturn = new javax.swing.JLabel();
        saleLoss = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        saleDay = new org.jdesktop.swingx.JXDatePicker();
        jLabel2 = new javax.swing.JLabel();
        users = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setTitle("Daily Sale Register");

        jXTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Serial", "Invoice #", "User", "Customer", "Total Bill", "Discount", "Gross Bill", "Freight", "Net Bill"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jXTable1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jXTable1.setRowHeight(25);
        jXTable1.getTableHeader().setReorderingAllowed(false);
        jXTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jXTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jXTable1);
        if (jXTable1.getColumnModel().getColumnCount() > 0) {
            jXTable1.getColumnModel().getColumn(0).setPreferredWidth(20);
            jXTable1.getColumnModel().getColumn(1).setPreferredWidth(50);
            jXTable1.getColumnModel().getColumn(4).setPreferredWidth(50);
            jXTable1.getColumnModel().getColumn(5).setPreferredWidth(50);
            jXTable1.getColumnModel().getColumn(6).setPreferredWidth(50);
            jXTable1.getColumnModel().getColumn(7).setPreferredWidth(50);
            jXTable1.getColumnModel().getColumn(8).setPreferredWidth(50);
        }

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Summery"));

        cashSale.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        cashSale.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cashSale.setBorder(javax.swing.BorderFactory.createTitledBorder("Cash Sale"));
        cashSale.setPreferredSize(new java.awt.Dimension(200, 100));
        jPanel1.add(cashSale);

        creditSale.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        creditSale.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        creditSale.setBorder(javax.swing.BorderFactory.createTitledBorder("Credit Sale"));
        creditSale.setPreferredSize(new java.awt.Dimension(200, 100));
        jPanel1.add(creditSale);

        saleReturn.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        saleReturn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        saleReturn.setBorder(javax.swing.BorderFactory.createTitledBorder("Sale Return"));
        saleReturn.setPreferredSize(new java.awt.Dimension(200, 100));
        jPanel1.add(saleReturn);

        saleLoss.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        saleLoss.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        saleLoss.setBorder(javax.swing.BorderFactory.createTitledBorder("Sale Loss"));
        saleLoss.setPreferredSize(new java.awt.Dimension(200, 100));
        jPanel1.add(saleLoss);

        jLabel1.setText("SALE DAY");
        jLabel1.setPreferredSize(new java.awt.Dimension(60, 28));
        jPanel2.add(jLabel1);

        saleDay.setPreferredSize(new java.awt.Dimension(150, 28));
        jPanel2.add(saleDay);

        jLabel2.setText("USER");
        jLabel2.setPreferredSize(new java.awt.Dimension(40, 28));
        jPanel2.add(jLabel2);

        users.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All Users " }));
        users.setPreferredSize(new java.awt.Dimension(150, 28));
        jPanel2.add(users);

        jButton1.setText("Search");
        jButton1.setFocusable(false);
        jButton1.setPreferredSize(new java.awt.Dimension(80, 28));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 366, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        getInvoices();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jXTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jXTable1MouseClicked
        // TODO add your handling code here:
        if(evt.getClickCount()==2){
            if(jXTable1.getSelectedRow()>-1){
                new SaleInvoiceDetail((long)jXTable1.getValueAt(jXTable1.getSelectedRow(), 1),parent).show();
            }
        }
    }//GEN-LAST:event_jXTable1MouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cashSale;
    private javax.swing.JLabel creditSale;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private org.jdesktop.swingx.JXTable jXTable1;
    private org.jdesktop.swingx.JXDatePicker saleDay;
    private javax.swing.JLabel saleLoss;
    private javax.swing.JLabel saleReturn;
    private javax.swing.JComboBox<String> users;
    // End of variables declaration//GEN-END:variables
}
