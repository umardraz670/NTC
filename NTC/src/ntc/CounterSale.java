/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ntc;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Dell 7010
 */
public class CounterSale extends javax.swing.JInternalFrame implements ListSelectionListener {

    /**
     * Creates new form counterSale
     */
    private final DefaultTableModel allProducts, cartTable;
    private final TableRowSorter<DefaultTableModel> tr;
    private int sortedRows = 0;
    private final NumberFormat nf;
    private final Frame parent;
    private Map<String, productsBeans> selectedProducts;
    private int counter;
    private long total = 0, discount = 0, grossTotal = 0, freight = 0, netTotal = 0;
    private Action action;
    private boolean customerCheck = false;
    private long customerID = 1;

    public CounterSale(Frame parent) {
        nf = NumberFormat.getIntegerInstance();
        initComponents();
        action = new AbstractAction("saveInvoice") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(parent, "Message");
                saveInvoice();
            }
        };
        product.grabFocus();
        this.parent = parent;
        tr = new TableRowSorter<>((DefaultTableModel) jXTable2.getModel());
        jXTable2.setRowSorter(tr);
        allProducts = (DefaultTableModel) jXTable2.getModel();
        cartTable = (DefaultTableModel) jXTable1.getModel();
        selectedProducts = new HashMap<>();
        getAllProducts();
        counter = 0;
        
        getRootPane().getInputMap().put(KeyStroke.getKeyStroke("F9"), action);
        

    }

    private void getAllProducts() {
        try (Connection con = db.getConnection()) {
            ResultSet res = con.createStatement().executeQuery("select sku,description,rate from products");
            if (!res.next()) {
                System.out.println("No Records");
            }
            while (res.next()) {
                allProducts.addRow(new Object[]{res.getInt("sku"), res.getString("description"), res.getFloat("rate")});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(rootPane, e.getMessage());
            System.exit(-1);
        }

    }

    private void filter(String query) {
        tr.setRowFilter(RowFilter.regexFilter(query, 1));
        sortedRows = tr.getViewRowCount();
        if (tr.getViewRowCount() > 0) {
            jXTable2.setRowSelectionInterval(0, 0);
        } else {
            System.out.println("No Row");
        }
    }

    private void saveInvoice() {
        long invoice = 0;
        if (customerCheck) {
            if (selectedProducts.size() > 0) {
                try (Connection con = db.getConnection()) {
                    con.createStatement().executeUpdate("savepoint a");
                    ResultSet res = con.createStatement().executeQuery("select max(invoice) from sale_invoices");
                    if (res.next()) {
                        invoice = res.getLong(1);
                    }
                    System.out.println(invoice + 1);
                    PreparedStatement ps = con.prepareStatement("insert into sale_invoices(invoice,total,discount,gross_total,freight,net_total,fk_customer,fk_sale_day,fk_user) values(?,?,?,?,?,?,?,?,?)");
                    ps.setLong(1, invoice + 1);
                    ps.setLong(2, total);
                    ps.setLong(3, discount);
                    ps.setLong(4, grossTotal);
                    ps.setLong(5, freight);
                    ps.setLong(6, (netTotal - discount) + freight);
                    ps.setLong(7, customerID);
                    ps.setString(8, Utils.SALE_DAY);
                    ps.setLong(9, Utils.USER_ID);
                    if (ps.executeUpdate() == 1) {
                        con.commit();
                        String str[] = csvString();
                        ps = con.prepareStatement("insert into invoices_details(skus,qty,rates,fk_invoice) values(?,?,?,?)");
                        ps.setString(1, str[0]);
                        ps.setString(2, str[1]);
                        ps.setString(3, str[2]);
                        ps.setLong(4, invoice);
                        if (ps.executeUpdate() == 1) {
                            con.commit();
                            JOptionPane.showMessageDialog(rootPane, "Sale Invoice ( " + (invoice + 1) + " ) Saved !");
                            nextInvoice();
                        } else {
                            JOptionPane.showMessageDialog(rootPane, "Error While Saving Invoice Data\nCall Your Vander");
                            con.createStatement().executeUpdate("rollback a");
                            nextInvoice();
                        }
                    } else {
                        JOptionPane.showMessageDialog(rootPane, "Error While Saving Invoice \nCall Your Vander");
                        con.createStatement().executeUpdate("rollback a");
                        nextInvoice();
                    }

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(rootPane, e.getMessage());
                    dispose();
                }
            } else {
                JOptionPane.showMessageDialog(rootPane, "Cannot Save Empty Invoice");
            }
        } else {
            JOptionPane.showMessageDialog(rootPane, "Please Select Customer");
            customer.grabFocus();
        }
    }

    private String[] csvString() {
        String str[] = new String[3];
        str[0] = "";
        str[1] = "";
        str[2] = "";
        int i = 0;
        selectedProducts.values().stream().map((b) -> {
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

    private void nextInvoice() {
        customer.setText("03");
        customerCheck = false;
        customerID = 1;
        product.setText("");
        qty.setText("1");
        int rowCount = cartTable.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            cartTable.removeRow(i);
        }
        total=0;
        discount=0;
        grossTotal=0;
        freight=0;
        netTotal=0;
        totalBill.setText("");
        discountBill.setText("");
        grossBill.setText("");
        freightBill.setText("");
        netBill.setText("");
        jCheckBox1.setSelected(true);
        counter=0;
        customer.grabFocus();

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
        product = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        qty = new javax.swing.JFormattedTextField(nf);
        jLabel3 = new javax.swing.JLabel();
        cellNo = new javax.swing.JFormattedTextField(Utils.formatCellNumber());
        jLabel4 = new javax.swing.JLabel();
        customer = new javax.swing.JTextField();
        jCheckBox1 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        totalBill = new javax.swing.JTextField();
        discountBill = new javax.swing.JTextField();
        grossBill = new javax.swing.JTextField();
        freightBill = new javax.swing.JTextField();
        netBill = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jXTable2 = new org.jdesktop.swingx.JXTable();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jXTable1 = new org.jdesktop.swingx.JXTable();

        setClosable(true);
        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle("COUNTER SALE");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Add Parts"));
        jPanel1.setFocusable(false);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel1.setText("Product Code");

        product.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        product.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        product.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                productKeyReleased(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setText("QTY");

        qty.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        qty.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        qty.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                qtyKeyReleased(evt);
            }
        });

        jLabel3.setText("Customer Cell No.");
        jLabel3.setFocusable(false);

        cellNo.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        cellNo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        cellNo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                cellNoKeyReleased(evt);
            }
        });

        jLabel4.setText("Customer Name");

        customer.setEditable(false);
        customer.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        customer.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        customer.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        customer.setFocusable(false);
        customer.setPreferredSize(new java.awt.Dimension(150, 26));

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Print 2 Invoices");

        jButton1.setText("Save & Print");
        jButton1.setFocusable(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Save");
        jButton2.setFocusable(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cellNo, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customer, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(product, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(qty, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(product, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(qty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(cellNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBox1)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(6, 6, 6))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Billing"));

        totalBill.setEditable(false);
        totalBill.setFont(new java.awt.Font("Arial Rounded MT Bold", 1, 30)); // NOI18N
        totalBill.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        totalBill.setBorder(javax.swing.BorderFactory.createTitledBorder("Total"));
        totalBill.setFocusable(false);
        totalBill.setPreferredSize(new java.awt.Dimension(180, 55));

        discountBill.setFont(new java.awt.Font("Arial Rounded MT Bold", 1, 30)); // NOI18N
        discountBill.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        discountBill.setBorder(javax.swing.BorderFactory.createTitledBorder("Discount"));
        discountBill.setPreferredSize(new java.awt.Dimension(180, 55));
        discountBill.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                discountBillKeyReleased(evt);
            }
        });

        grossBill.setEditable(false);
        grossBill.setFont(new java.awt.Font("Arial Rounded MT Bold", 1, 30)); // NOI18N
        grossBill.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        grossBill.setBorder(javax.swing.BorderFactory.createTitledBorder("Gross Total"));
        grossBill.setFocusable(false);
        grossBill.setPreferredSize(new java.awt.Dimension(180, 55));

        freightBill.setFont(new java.awt.Font("Arial Rounded MT Bold", 1, 30)); // NOI18N
        freightBill.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        freightBill.setBorder(javax.swing.BorderFactory.createTitledBorder("Freight"));
        freightBill.setPreferredSize(new java.awt.Dimension(180, 55));
        freightBill.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                freightBillKeyReleased(evt);
            }
        });

        netBill.setEditable(false);
        netBill.setFont(new java.awt.Font("Arial Rounded MT Bold", 1, 30)); // NOI18N
        netBill.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        netBill.setBorder(javax.swing.BorderFactory.createTitledBorder("Net Total"));
        netBill.setFocusable(false);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(totalBill, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(discountBill, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(grossBill, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(freightBill, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(netBill, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(discountBill, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                    .addComponent(totalBill, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(grossBill, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(netBill)
                    .addComponent(freightBill, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("All Products"));

        jXTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Code", "Description", "Rate"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jXTable2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jXTable2.setRowHeight(20);
        jXTable2.setSelectionBackground(new java.awt.Color(51, 51, 255));
        jXTable2.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(jXTable2);
        if (jXTable2.getColumnModel().getColumnCount() > 0) {
            jXTable2.getColumnModel().getColumn(1).setPreferredWidth(150);
        }

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 339, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Products"));

        jXTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No.", "Description", "Qty", "Rate", "Amount"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jXTable1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jXTable1.setRowHeight(25);
        jXTable1.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jXTable1);
        if (jXTable1.getColumnModel().getColumnCount() > 0) {
            jXTable1.getColumnModel().getColumn(0).setPreferredWidth(30);
            jXTable1.getColumnModel().getColumn(1).setPreferredWidth(150);
        }

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
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
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void productKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_productKeyReleased
        // TODO add your handling code here:
        int selectedRow;
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                selectedRow = jXTable2.getSelectedRow();
                if (selectedRow < sortedRows - 1) {
                    jXTable2.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                }
                break;
            case KeyEvent.VK_UP:
                selectedRow = jXTable2.getSelectedRow();
                if (selectedRow > 0) {
                    jXTable2.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                }
                break;
            case KeyEvent.VK_ENTER:
                if (jXTable2.getSelectedRow() > -1) {
                    product.setText(jXTable2.getValueAt(jXTable2.getSelectedRow(), 1).toString());
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
            if (jXTable2.getSelectedRow() > -1) {
                if (selectedProducts.containsKey(product.getText())) {
                    float rate = (float) jXTable2.getValueAt(jXTable2.getSelectedRow(), 2);
                    int quantity = ((Number) qty.getValue()).intValue();
                    float amount = (float) (rate * quantity);

                    selectedProducts.get(product.getText()).setQty(selectedProducts.get(product.getText()).getQty() + quantity);
                    selectedProducts.get(product.getText()).setAmount(selectedProducts.get(product.getText()).getAmount() + amount);

                    cartTable.setValueAt(selectedProducts.get(product.getText()).qty, selectedProducts.get(product.getText()).rowNo, 2);
                    cartTable.setValueAt(selectedProducts.get(product.getText()).amount, selectedProducts.get(product.getText()).rowNo, 4);

                    total += (selectedProducts.get(product.getText()).amount);
                    netTotal += (selectedProducts.get(product.getText()).amount);
                    grossTotal += (selectedProducts.get(product.getText()).amount);
                    totalBill.setText("" + total);
                    netBill.setText("" + ((netTotal - discount) + freight));
                    grossBill.setText("" + grossTotal);
                    product.setText("");
                    qty.setText("1");
                    product.grabFocus();
                    filter("");
                } else {
                    float rate = (float) jXTable2.getValueAt(jXTable2.getSelectedRow(), 2);
                    int sku = (int) jXTable2.getValueAt(jXTable2.getSelectedRow(), 0);
                    int quantity = ((Number) qty.getValue()).intValue();
                    float amount = (float) (rate * quantity);
                    cartTable.addRow(new Object[]{++counter, product.getText(), quantity, rate, amount});
                    selectedProducts.put(product.getText(), new productsBeans(counter - 1, sku, product.getText(), quantity, rate, amount));
                    total += amount;
                    grossTotal += amount;
                    netTotal += amount;
                    totalBill.setText("" + total);
                    netBill.setText("" + ((netTotal - discount) + freight));
                    grossBill.setText("" + grossTotal);
                    product.setText("");
                    qty.setText("1");
                    product.grabFocus();
                    filter("");
                }

            } else {
                JOptionPane.showMessageDialog(parent, "Please Select Product");
                product.setText("");
                filter("");
                product.grabFocus();
            }

        }
    }//GEN-LAST:event_qtyKeyReleased

    private void cellNoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_cellNoKeyReleased
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            try (Connection con = db.getConnection()) {
                PreparedStatement ps = con.prepareStatement("select cust_id,cust_name from customers where cell_no=? and active='YES'");
                ps.setString(1, cellNo.getText());
                ResultSet res = ps.executeQuery();
                if (res.next()) {
                    customerID = res.getLong(1);
                    customer.setText(res.getString(2));
                    customerCheck = true;
                    product.grabFocus();
                } else {
                    customerCheck = false;
                    customer.setText("");
                    new NewCustomer(parent, true, cellNo.getText()).show();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, e.getMessage() + "\nCall Your Vander");
                System.exit(-1);
            }
        }
    }//GEN-LAST:event_cellNoKeyReleased

    private void discountBillKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_discountBillKeyReleased
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (Pattern.matches("[0-9]+", discountBill.getText())) {
                discount = Long.parseLong(discountBill.getText());
                grossBill.setText("" + (total - discount));
                grossTotal = total - discount;
                netBill.setText("" + (netTotal - discount));
            } else {
                System.out.println("Empty");
            }
        }
    }//GEN-LAST:event_discountBillKeyReleased

    private void freightBillKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_freightBillKeyReleased
        // TODO add your handling code here:
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (Pattern.matches("[0-9]+", freightBill.getText())) {
                freight = Long.parseLong(freightBill.getText());
                netBill.setText("" + ((netTotal - discount) + freight));
            } else {
                System.out.println("Empty Input");
            }
        }
    }//GEN-LAST:event_freightBillKeyReleased

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        saveInvoice();
    }//GEN-LAST:event_jButton1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFormattedTextField cellNo;
    private javax.swing.JTextField customer;
    private javax.swing.JTextField discountBill;
    private javax.swing.JTextField freightBill;
    private javax.swing.JTextField grossBill;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private org.jdesktop.swingx.JXTable jXTable1;
    private org.jdesktop.swingx.JXTable jXTable2;
    private javax.swing.JTextField netBill;
    private javax.swing.JTextField product;
    private javax.swing.JFormattedTextField qty;
    private javax.swing.JTextField totalBill;
    // End of variables declaration//GEN-END:variables

    @Override
    public void valueChanged(ListSelectionEvent e) {
        int viewRow = jXTable2.getSelectedRow();
        if (viewRow < 0) {
            //Selection got filtered away.
        } else {
            int modelRow = jXTable2.convertRowIndexToModel(viewRow);
            System.out.println(modelRow);
        }
    }
}
