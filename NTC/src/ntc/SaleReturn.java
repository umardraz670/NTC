/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Umar Rehman
 */
public class SaleReturn extends javax.swing.JInternalFrame {

    /**
     * Creates new form SaleReturn
     */
    private final DefaultTableModel model, returnModel;
    private final DefaultTableCellRenderer centerRender;
    private final Map<String, items> purchasedProducts;
    private final Map<String, items> goodsReturn;
    private final TableRowSorter<DefaultTableModel> tr;
    private int sortedRows = 0;
    private float netReturn = 0f;
    private PreparedStatement ps;
    private ResultSet res;
    private long customerID;

    public SaleReturn() {
        initComponents();
        tr = new TableRowSorter<>((DefaultTableModel) jTable2.getModel());
        jTable2.setRowSorter(tr);
        model = (DefaultTableModel) jTable2.getModel();
        returnModel = (DefaultTableModel) jTable1.getModel();
        centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        purchasedProducts = new HashMap<>();
        goodsReturn = new HashMap<>();
        customerID = 0;
        for (int i = 0; i < jTable2.getColumnModel().getColumnCount(); i++) {
            if (i == 6) {
                jTable2.getColumnModel().getColumn(i).setCellRenderer(new CellRender());
            } else {
                jTable2.getColumnModel().getColumn(i).setCellRenderer(centerRender);
            }
        }

    }

    private void getInvoice() {
        try (Connection con = db.getConnection()) {
            ps = con.prepareStatement("select "
                    + "c.cust_name,"
                    + "c.cell_no,"
                    + "id.skus,"
                    + "id.qty,"
                    + "id.rates,"
                    + "si.total,"
                    + "si.discount,"
                    + "si.gross_total,"
                    + "si.freight,"
                    + "si.net_total,"
                    + "c.cust_id,"
                    + "si.fk_sale_day "
                    + "from sale_invoices si,customers c,invoices_details id "
                    + "where si.fk_customer=c.cust_id and si.invoice=id.fk_invoice and si.invoice=?");
            ps.setLong(1, ((Number) invoiceNo.getValue()).longValue());
            res = ps.executeQuery();
            if (res.next()) {
                customerName.setText(res.getString(1));
                cellNo.setText(res.getString(2));
                StringTokenizer skuTokenizer, qtyTokenizer, ratesTokenizer;
                skuTokenizer = new StringTokenizer(res.getString(3), ",");
                qtyTokenizer = new StringTokenizer(res.getString(4), ",");
                ratesTokenizer = new StringTokenizer(res.getString(5), ",");
                total.setText("" + res.getFloat(6));
                discount.setText("" + res.getFloat(7));
                gross.setText("" + res.getFloat(8));
                freight.setText("" + res.getFloat(9));
                netBill.setText("" + res.getFloat(10));
                customerID = res.getLong(11);
                date.setText(res.getString(12));
                while (skuTokenizer.hasMoreElements()) {
                    String sku = skuTokenizer.nextToken();
                    int quantity = Integer.parseInt(qtyTokenizer.nextToken());
                    float rates = Float.parseFloat(ratesTokenizer.nextToken());
                    ps = con.prepareStatement("select description from products where sku=?");
                    ps.setInt(1, Integer.parseInt(sku));
                    res = ps.executeQuery();
                    if (res.next()) {
                        String description = res.getString(1);
                        model.addRow(new Object[]{sku, description, quantity, rates, quantity * rates});
                        purchasedProducts.put(description.toUpperCase(), new items(Integer.parseInt(sku), description, quantity, rates, quantity * rates));
                    } else {
                        System.out.println("No Records ");
                    }
                }
                product.grabFocus();

            } else {
                customerName.setText("");
                cellNo.setText("");
                total.setText("");
                discount.setText("");
                gross.setText("");
                freight.setText("");
                netBill.setText("");
                invoiceNo.setValue(0);
                JOptionPane.showMessageDialog(rootPane, "Invoice # Not found !");
                invoiceNo.grabFocus();
            }
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(rootPane, e.getMessage());
            dispose();
        }
    }

    private void filter(String query) {
        tr.setRowFilter(RowFilter.regexFilter(query, 1));
        sortedRows = tr.getViewRowCount();
        if (tr.getViewRowCount() > 0) {
            jTable2.setRowSelectionInterval(0, 0);
        } else {
            System.out.println("No Row");
        }
    }

    private void saveInvoice() {
        long invoice = 0;
        try (Connection con = db.getConnection()) {
            con.createStatement().executeUpdate("savepoint a");
            res = con.createStatement().executeQuery("select max(invoice) from sale_returns");
            if (res.next()) {
                invoice = res.getLong(1);
                System.out.println("Sale Return max Invoice is  "+invoice);
                ps = con.prepareStatement("insert into sale_returns(invoice,customer_fk,sale_invoice_fk,net_return,user_fk) values(?,?,?,?,?)");
                ps.setLong(1, ++invoice);
                ps.setLong(2, customerID);
                ps.setLong(3, ((Number) invoiceNo.getValue()).longValue());
                ps.setFloat(4, netAmount());
                ps.setLong(5, Utils.USER_ID);
                System.out.println("Saving Invoice");
                if (ps.executeUpdate() == 1) {
                    System.out.println("Invoice Saved");
                    ps=con.prepareStatement("select sale_return from sales where sale_day=?");
                    ps.setString(1, date.getText());
                    res = ps.executeQuery();
                    if (res.next()) {
                        float lastAmount = res.getFloat(1);
                        ps = con.prepareStatement("update sales set sale_return=? where sale_day=?");
                        ps.setFloat(1, netAmount()+lastAmount);
                        ps.setString(2, Utils.todayDate());
                        System.out.println("Updating Sale Day Amounts");
                        if (ps.executeUpdate() == 1) {
                            System.out.println("Sale Day Amount Updated");
                            JOptionPane.showMessageDialog(rootPane, "Sale Return Saved !");
                            con.commit();
                        } else {
                            con.createStatement().executeUpdate("rollback a");
                            JOptionPane.showMessageDialog(rootPane, "Error while Updating Sale Return  ");
                        }
                    }
                } else {
                    con.createStatement().executeUpdate("rollback a");
                    JOptionPane.showMessageDialog(rootPane, "Error while Saving Sale Return ");
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPane, e.getMessage());
            dispose();
        }
    }

    private float netAmount() {
        float netTotal = 0;
        netTotal = goodsReturn.values().stream().map(i -> i.amount).reduce(netTotal, (accumulator, _item) -> accumulator + _item);
        return netTotal;
    }

    private String[] csvString() {
        String str[] = new String[3];
        str[0] = "";
        str[1] = "";
        str[2] = "";
        int i = 0;
        goodsReturn.values().stream().map((b) -> {
            str[0] += b.sku + ",";
            return b;
        }).map((b) -> {
            str[1] += b.qty + ",";
            return b;
        }).forEachOrdered((b) -> {
            str[2] += b.rate + ",";
        });
        str[0] = str[0].substring(0, str[0].length() - 1);
        str[1] = str[1].substring(0, str[1].length() - 1);
        str[2] = str[2].substring(0, str[2].length() - 1);
        return str;
    }

    class items {

        int sku;
        String description;
        int qty;
        float rate, amount;

        public items(int sku, String description, int qty, float rate, float amount) {
            this.sku = sku;
            this.description = description;
            this.qty = qty;
            this.rate = rate;
            this.amount = amount;
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

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        invoiceNo = new javax.swing.JFormattedTextField(NumberFormat.getIntegerInstance());
        jLabel2 = new javax.swing.JLabel();
        customerName = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        cellNo = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        date = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        total = new javax.swing.JLabel();
        discount = new javax.swing.JLabel();
        gross = new javax.swing.JLabel();
        freight = new javax.swing.JLabel();
        netBill = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        product = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        qty = new javax.swing.JFormattedTextField(NumberFormat.getIntegerInstance());
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        returnAmount = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();

        setClosable(true);
        setIconifiable(true);
        setTitle("Sale Return");

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel1.setText("Invoice #");

        invoiceNo.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        invoiceNo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        invoiceNo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                invoiceNoKeyReleased(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel2.setText("Customer Name");

        customerName.setEditable(false);
        customerName.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        customerName.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setText("Cell #");

        cellNo.setEditable(false);
        cellNo.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        cellNo.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jButton1.setText("Save ");
        jButton1.setFocusable(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("Date");

        date.setEditable(false);
        date.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        date.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(invoiceNo, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(date)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(customerName, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cellNo, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(invoiceNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6)
                        .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(customerName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)
                        .addComponent(cellNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        total.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        total.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        total.setBorder(javax.swing.BorderFactory.createTitledBorder("Total Bill"));
        total.setPreferredSize(new java.awt.Dimension(165, 80));
        jPanel2.add(total);

        discount.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        discount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        discount.setBorder(javax.swing.BorderFactory.createTitledBorder("Discount"));
        discount.setPreferredSize(new java.awt.Dimension(165, 80));
        jPanel2.add(discount);

        gross.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        gross.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gross.setBorder(javax.swing.BorderFactory.createTitledBorder("Gross Bill"));
        gross.setPreferredSize(new java.awt.Dimension(165, 80));
        jPanel2.add(gross);

        freight.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        freight.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        freight.setBorder(javax.swing.BorderFactory.createTitledBorder("Freight"));
        freight.setPreferredSize(new java.awt.Dimension(165, 80));
        jPanel2.add(freight);

        netBill.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        netBill.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        netBill.setBorder(javax.swing.BorderFactory.createTitledBorder("Net Bill"));
        netBill.setPreferredSize(new java.awt.Dimension(165, 80));
        jPanel2.add(netBill);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Goods Return Entry"));

        jLabel4.setText("Product");

        product.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        product.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                productKeyReleased(evt);
            }
        });

        jLabel5.setText("Qty");

        qty.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        qty.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                qtyKeyReleased(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SKU", "Description", "Qty", "Rate", "Amount"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setRowHeight(25);
        jTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setPreferredWidth(10);
            jTable1.getColumnModel().getColumn(1).setPreferredWidth(80);
            jTable1.getColumnModel().getColumn(2).setPreferredWidth(10);
            jTable1.getColumnModel().getColumn(3).setPreferredWidth(20);
            jTable1.getColumnModel().getColumn(4).setPreferredWidth(20);
        }

        returnAmount.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        returnAmount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        returnAmount.setBorder(javax.swing.BorderFactory.createTitledBorder("Return Amount"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(product, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(qty))
                    .addComponent(returnAmount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(product, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(qty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 308, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(returnAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Purchased Goods"));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SKU", "Description", "Qty", "Rate", "Amount"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, true, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setRowHeight(25);
        jTable2.getTableHeader().setReorderingAllowed(false);
        jTable2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable2MouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTable2);
        if (jTable2.getColumnModel().getColumnCount() > 0) {
            jTable2.getColumnModel().getColumn(0).setPreferredWidth(10);
            jTable2.getColumnModel().getColumn(1).setPreferredWidth(150);
            jTable2.getColumnModel().getColumn(2).setPreferredWidth(10);
            jTable2.getColumnModel().getColumn(3).setPreferredWidth(10);
            jTable2.getColumnModel().getColumn(4).setPreferredWidth(10);
        }

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 872, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void invoiceNoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_invoiceNoKeyReleased
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!"".equals(invoiceNo.getText())) {
                getInvoice();
            }
        }
    }//GEN-LAST:event_invoiceNoKeyReleased

    private void jTable2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable2MouseClicked
        // TODO add your handling code here:
        if (jTable2.getSelectedColumn() == 6) {

        }
    }//GEN-LAST:event_jTable2MouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        if (JOptionPane.showConfirmDialog(rootPane, "Please confirme the Sale Return ", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            saveInvoice();
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    private void productKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_productKeyReleased
        // TODO add your handling code here:
        int selectedRow;
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                selectedRow = jTable2.getSelectedRow();
                if (selectedRow < sortedRows - 1) {
                    jTable2.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                }
                break;
            case KeyEvent.VK_UP:
                selectedRow = jTable2.getSelectedRow();
                if (selectedRow > 0) {
                    jTable2.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                }
                break;
            case KeyEvent.VK_ENTER:
                if (jTable2.getSelectedRow() > -1) {
                    product.setText(jTable2.getValueAt(jTable2.getSelectedRow(), 1).toString());
                    qty.grabFocus();
                } else {
                    qty.grabFocus();
                }
                break;
            default:
                filter("(?i)" + product.getText());
                break;
        }
    }//GEN-LAST:event_productKeyReleased

    private void qtyKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_qtyKeyReleased
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (jTable2.getSelectedRow() > -1) {
                if (purchasedProducts.containsKey(product.getText().toUpperCase())) {
                    int quantity = ((Number) qty.getValue()).intValue();
                    if (goodsReturn.containsKey(product.getText())) {
                        // checking the quantity with purchased good quantity  formulla is   current qty + already entered qty < = purchased Qty
                        if (quantity + goodsReturn.get(product.getText()).qty >= 0 && quantity + goodsReturn.get(product.getText()).qty <= purchasedProducts.get(product.getText()).qty) {
                            float amount = (float) (purchasedProducts.get(product.getText()).rate * quantity);
                            for (int i = 0; i < returnModel.getRowCount(); i++) {
                                if (returnModel.getValueAt(i, 0).equals(purchasedProducts.get(product.getText()).sku)) {
                                    returnModel.setValueAt(quantity + goodsReturn.get(product.getText()).qty, i, 2);
                                    returnModel.setValueAt(amount + Float.parseFloat(returnModel.getValueAt(i, 4).toString()), i, 4);
                                    break;
                                }
                            }
                            goodsReturn.get(product.getText()).qty += quantity;
                            netReturn += amount;
                            returnAmount.setText("" + netReturn);
                            product.setText("");
                            qty.setValue(0);
                            product.grabFocus();
                            filter("");
                        } else {
                            JOptionPane.showMessageDialog(rootPane, "Invalid Quantity Entered ");
                            qty.setValue(0);
                            qty.grabFocus();
                        }
                    } else {
                        float amount = (float) (purchasedProducts.get(product.getText()).rate * quantity);
                        returnModel.addRow(new Object[]{purchasedProducts.get(product.getText()).sku,
                            purchasedProducts.get(product.getText()).description,
                            quantity,
                            purchasedProducts.get(product.getText()).rate,
                            amount});
                        goodsReturn.put(product.getText(), new items(purchasedProducts.get(product.getText()).sku,
                                purchasedProducts.get(product.getText()).description,
                                quantity,
                                purchasedProducts.get(product.getText()).rate, amount));
                        netReturn += amount;
                        returnAmount.setText("" + netReturn);
                        product.setText("");
                        qty.setValue(0);
                        product.grabFocus();
                        filter("");
                    }

                } else {
                    JOptionPane.showMessageDialog(rootPane, "Product Not Found In Sale Invoice");
                    product.setText("");
                    qty.setValue(0);
                    product.grabFocus();
                    filter("");
                }
            }
        }
    }//GEN-LAST:event_qtyKeyReleased


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField cellNo;
    private javax.swing.JTextField customerName;
    private javax.swing.JTextField date;
    private javax.swing.JLabel discount;
    private javax.swing.JLabel freight;
    private javax.swing.JLabel gross;
    private javax.swing.JFormattedTextField invoiceNo;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JLabel netBill;
    private javax.swing.JTextField product;
    private javax.swing.JFormattedTextField qty;
    private javax.swing.JLabel returnAmount;
    private javax.swing.JLabel total;
    // End of variables declaration//GEN-END:variables
}
