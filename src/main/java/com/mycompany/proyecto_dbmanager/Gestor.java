/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.proyecto_dbmanager;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

/**
 *
 * @author dessi
 */
public class Gestor extends javax.swing.JFrame {

    /**
     * Creates new form Gestor
     */
    public Gestor() {
        initComponents();
        this.setResizable(false);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        Gestor_jDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        url = "jdbc:mysql://localhost:3306/connections";
        user = "root";
        password = "contra";

        jPanel4.setVisible(false);

        ConnectionTree();

    }

    void ConnectionTree() {
        treeModel = (DefaultTreeModel) jTree_C.getModel();
        raiz = (DefaultMutableTreeNode) treeModel.getRoot();
        raiz.removeAllChildren();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection(url, user, password);

            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT name FROM connections_data");

            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);
                raiz.add(new DefaultMutableTreeNode(databaseName));
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        treeModel.reload();
    }

    void SchemasTree() {
        treeModel = (DefaultTreeModel) jTree.getModel();
        raiz = (DefaultMutableTreeNode) treeModel.getRoot();
        raiz.removeAllChildren();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/connections", user, password);

            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT name FROM database_conn WHERE id_connection =" + id_connection);

            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);
                DefaultMutableTreeNode databaseNode = new DefaultMutableTreeNode(databaseName);

                raiz.add(databaseNode);

                // Crear nodos para tablas, vistas, procedimientos, funciones, etc.
                DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode("Tables");

                DefaultMutableTreeNode viewNode = new DefaultMutableTreeNode("Views");
                DefaultMutableTreeNode procedureNode = new DefaultMutableTreeNode("Stored Procedures");
                DefaultMutableTreeNode functionNode = new DefaultMutableTreeNode("Functions");

                databaseNode.add(tableNode);
                databaseNode.add(viewNode);
                databaseNode.add(procedureNode);
                databaseNode.add(functionNode);

                // Obtener las tablas
                Statement infoStatement = connection.createStatement();
                ResultSet infoResultSet = infoStatement.executeQuery("SELECT TABLE_NAME, TABLE_TYPE \n"
                        + "FROM INFORMATION_SCHEMA.TABLES\n"
                        + "WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA = '" + databaseName + "';");

                while (infoResultSet.next()) {
                    String tableName = infoResultSet.getString(1);
                    DefaultMutableTreeNode table = new DefaultMutableTreeNode(tableName);
                    tableNode.add(table);

                    DefaultMutableTreeNode indexNode = new DefaultMutableTreeNode("Indexes");
                    DefaultMutableTreeNode keysNode = new DefaultMutableTreeNode("Foreign Keys");
                    DefaultMutableTreeNode triggersNode = new DefaultMutableTreeNode("Triggers");

                    // Obtener los índices
                    Statement indexStatement = connection.createStatement();
                    ResultSet indexResultSet = indexStatement.executeQuery("SHOW INDEX FROM " + tableName + " IN " + databaseName);
                    while (indexResultSet.next()) {
                        String indexName = indexResultSet.getString("Key_name");
                        indexNode.add(new DefaultMutableTreeNode(indexName));
                    }

                    // Obtener los triggers
                    Statement triggerStatement = connection.createStatement();
                    ResultSet triggerResultSet = triggerStatement.executeQuery("SHOW TRIGGERS FROM `" + databaseName + "`");
                    while (triggerResultSet.next()) {
                        String triggerName = triggerResultSet.getString("Trigger");
                        triggersNode.add(new DefaultMutableTreeNode(triggerName));
                    }
                    triggerResultSet.close();
                    triggerStatement.close();
                    table.add(triggersNode);

                    table.add(indexNode);
                    table.add(keysNode);
                    table.add(triggersNode);
                    indexResultSet.close();
                    indexStatement.close();

                    // Obtener las llaves foráneas
                    Statement fkStatement = connection.createStatement();
                    ResultSet fkResultSet = fkStatement.executeQuery("SELECT\n"
                            + "  kcu.TABLE_NAME,\n"
                            + "  kcu.COLUMN_NAME,\n"
                            + "  kcu.REFERENCED_TABLE_NAME,\n"
                            + "  kcu.REFERENCED_COLUMN_NAME\n"
                            + "FROM\n"
                            + "  information_schema.TABLE_CONSTRAINTS AS tc\n"
                            + "  JOIN information_schema.KEY_COLUMN_USAGE AS kcu\n"
                            + "    ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME\n"
                            + "WHERE\n"
                            + "  tc.CONSTRAINT_TYPE = 'FOREIGN KEY'\n"
                            + "  AND tc.TABLE_NAME = '" + tableName + "'\n"
                            + "  AND tc.TABLE_SCHEMA = '" + databaseName + "'");
                    while (fkResultSet.next()) {
                        String columnName = fkResultSet.getString("COLUMN_NAME");
                        String referencedTableName = fkResultSet.getString("REFERENCED_TABLE_NAME");
                        String referencedColumnName = fkResultSet.getString("REFERENCED_COLUMN_NAME");
                        String fkInfo = columnName + " -> " + referencedTableName + "(" + referencedColumnName + ")";
                        keysNode.add(new DefaultMutableTreeNode(fkInfo));
                    }
                    fkResultSet.close();
                    fkStatement.close();

                    table.add(indexNode);
                    table.add(keysNode);
                }

                //Obtener Views
                infoResultSet = infoStatement.executeQuery("SELECT TABLE_NAME, TABLE_TYPE \n"
                        + "FROM INFORMATION_SCHEMA.TABLES\n"
                        + "WHERE TABLE_TYPE = 'VIEW' AND TABLE_SCHEMA = '" + databaseName + "';");
                while (infoResultSet.next()) {
                    String viewName = infoResultSet.getString(1);
                    DefaultMutableTreeNode view = new DefaultMutableTreeNode(viewName);
                    viewNode.add(view);
                }

                //Obtener Procedures
                infoResultSet = infoStatement.executeQuery("SELECT ROUTINE_NAME, ROUTINE_TYPE FROM information_schema.ROUTINES "
                        + "WHERE ROUTINE_SCHEMA = '" + databaseName + "' AND ROUTINE_TYPE = 'PROCEDURE'");
                while (infoResultSet.next()) {
                    String procedureName = infoResultSet.getString(1);
                    DefaultMutableTreeNode procedure = new DefaultMutableTreeNode(procedureName);
                    procedureNode.add(procedure);
                }

                //Obtener Functions
                infoResultSet = infoStatement.executeQuery("SELECT ROUTINE_NAME, ROUTINE_TYPE FROM information_schema.ROUTINES "
                        + "WHERE ROUTINE_SCHEMA = '" + databaseName + "' AND ROUTINE_TYPE = 'FUNCTION'");
                while (infoResultSet.next()) {
                    String functionName = infoResultSet.getString(1);
                    DefaultMutableTreeNode function = new DefaultMutableTreeNode(functionName);
                    functionNode.add(function);
                }

                infoResultSet.close();
                infoStatement.close();

            }

            resultSet.close();
            statement.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        treeModel.reload();
        jTree.addMouseListener(new DoubleClickListener());
    }

    void LlenarTabla(String nombreTabla, String nameSchema) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();

        model.setRowCount(0);

        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameSchema, "root", "contra");
            Statement st = con.createStatement();
            String query = "SELECT * FROM " + nombreTabla;
            ResultSet rs = st.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();

            int cols = rsmd.getColumnCount();
            String[] colNombre = new String[cols];
            for (int i = 0; i < cols; i++) {
                colNombre[i] = rsmd.getColumnName(i + 1);
            }
            model.setColumnIdentifiers(colNombre);

            Object[] row = new Object[cols];

            if (rs.next()) {
                rs = st.executeQuery(query);
                while (rs.next()) {
                    for (int i = 0; i < cols; i++) {
                        row[i] = rs.getString(i + 1);
                    }
                    model.addRow(row);
                }
                for (int i = 0; i < cols; i++) {
                    row[i] = "";
                }
                model.addRow(row);
            } else {
                for (int i = 0; i < cols; i++) {
                    row[i] = "";
                }
                model.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private class DoubleClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                // Double-click detected
                TreePath path = jTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    nameTable = node.toString();
                    nameSchema = node.getParent().getParent().toString();
                    if (node.getParent().toString().equalsIgnoreCase("Tables")) {
                        try {
                            LlenarTabla(node.toString(), node.getParent().getParent().toString());
                        } catch (SQLException ex) {
                            Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            }
        }
    }

    public static boolean isNumeric(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        char firstChar = input.charAt(0);
        if (!Character.isDigit(firstChar) && firstChar != '-' && firstChar != '+') {
            return false;
        }

        boolean hasDecimalPoint = false;
        for (int i = 1; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '.') {
                if (hasDecimalPoint) {
                    return false; // More than one decimal point
                }
                hasDecimalPoint = true;
            } else if (!Character.isDigit(currentChar)) {
                return false; // Contains non-numeric characters
            }
        }

        return true;
    }

    public void createUserDBA(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantDBAPrivilegesSQL = "GRANT ALL PRIVILEGES ON *.* TO '" + newUsername + "'@'localhost' WITH GRANT OPTION";
                stmt.executeUpdate(grantDBAPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserMaintenanceAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantMaintenanceAdminPrivilegesSQL = "GRANT EVENT, RELOAD, SHOW DATABASES, SHUTDOWN, SUPER ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantMaintenanceAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserProcessAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantProcessAdminPrivilegesSQL = "GRANT RELOAD, SUPER ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantProcessAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserUserAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantUserAdminPrivilegesSQL = "GRANT CREATE USER, RELOAD ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantUserAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserSecurityAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantSecurityAdminPrivilegesSQL = "GRANT CREATE USER, GRANT OPTION, RELOAD, SHOW DATABASES ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantSecurityAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserMonitorAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantMonitorAdminPrivilegesSQL = "GRANT PROCESS ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantMonitorAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserDBManager(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantDBManagerPrivilegesSQL = "GRANT ALTER, ALTER ROUTINE, CREATE, CREATE ROUTINE, CREATE TEMPORARY TABLES, "
                        + "CREATE VIEW, DELETE, DROP, EVENT, GRANT OPTION, INDEX, INSERT, LOCK TABLES, SELECT, SHOW DATABASES, SHOW VIEW ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantDBManagerPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserDBDesigner(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantDBDesignerPrivilegesSQL = "GRANT ALTER, ALTER ROUTINE, CREATE, CREATE ROUTINE, CREATE VIEW, "
                        + "INDEX, SHOW DATABASES, SHOW VIEW, TRIGGER ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantDBDesignerPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserReplicationAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantReplicationAdminPrivilegesSQL = "GRANT REPLICATION CLIENT, REPLICATION SLAVE, SUPER ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantReplicationAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void createUserBackupAdmin(String url, String user, String password, String newUsername, String newPassword)
            throws SQLException {

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            try (Statement stmt = conn.createStatement()) {

                String createUserSQL = "CREATE USER '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
                stmt.executeUpdate(createUserSQL);

                String grantBackupAdminPrivilegesSQL = "GRANT EVENT, LOCK TABLES, SELECT, SHOW DATABASES ON *.* TO '" + newUsername + "'@'localhost'";
                stmt.executeUpdate(grantBackupAdminPrivilegesSQL);

                String flushPrivilegesSQL = "FLUSH PRIVILEGES";
                stmt.executeUpdate(flushPrivilegesSQL);

                JOptionPane.showMessageDialog(null, "User created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
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

        jPopupMenu_CreateConnection = new javax.swing.JPopupMenu();
        Btn_CreateConnection = new javax.swing.JMenuItem();
        Gestor_jDialog = new javax.swing.JDialog();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree = new javax.swing.JTree();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jPopupMenu_NewSchema = new javax.swing.JPopupMenu();
        Btn_NewSchema = new javax.swing.JMenuItem();
        ValidarUser = new javax.swing.JDialog();
        jLabel6 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        Login_User = new javax.swing.JTextField();
        Login_Pass = new javax.swing.JPasswordField();
        jPopupMenu_CCRUD = new javax.swing.JPopupMenu();
        Btn_OpenC = new javax.swing.JMenuItem();
        Btn_EditC = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        Btn_DeleteC = new javax.swing.JMenuItem();
        NameSchema = new javax.swing.JDialog();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        SchemaName = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jPopupMenu_DropSchema = new javax.swing.JPopupMenu();
        Btn_DropSchema = new javax.swing.JMenuItem();
        NewTable = new javax.swing.JDialog();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        DDL_CreateTable = new javax.swing.JTextArea();
        jTextField1 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jPopupMenu_Table = new javax.swing.JPopupMenu();
        CreateTable = new javax.swing.JMenuItem();
        DropTable = new javax.swing.JMenuItem();
        AlterTable = new javax.swing.JMenuItem();
        jPopupMenu_View = new javax.swing.JPopupMenu();
        CreateView = new javax.swing.JMenuItem();
        DropView = new javax.swing.JMenuItem();
        AlterView = new javax.swing.JMenuItem();
        jPopupMenu_Procedure = new javax.swing.JPopupMenu();
        CreateProcedure = new javax.swing.JMenuItem();
        DropProcedure = new javax.swing.JMenuItem();
        AlterProcedure = new javax.swing.JMenuItem();
        jPopupMenu_Function = new javax.swing.JPopupMenu();
        CreateFunction = new javax.swing.JMenuItem();
        DropFunction = new javax.swing.JMenuItem();
        AlterFunction = new javax.swing.JMenuItem();
        Users = new javax.swing.JDialog();
        jLabel16 = new javax.swing.JLabel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jPasswordField1 = new javax.swing.JPasswordField();
        jPasswordField2 = new javax.swing.JPasswordField();
        jPanel8 = new javax.swing.JPanel();
        jCheckBox_MaAd = new javax.swing.JCheckBox();
        jCheckBox_DBA = new javax.swing.JCheckBox();
        jCheckBox_BaAd = new javax.swing.JCheckBox();
        jCheckBox_PrAd = new javax.swing.JCheckBox();
        jCheckBox_SeAd = new javax.swing.JCheckBox();
        jCheckBox_MoAd = new javax.swing.JCheckBox();
        jCheckBox_DBM = new javax.swing.JCheckBox();
        jCheckBox_DBD = new javax.swing.JCheckBox();
        jCheckBox_UsAd = new javax.swing.JCheckBox();
        jCheckBox_ReAd = new javax.swing.JCheckBox();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTableU = new javax.swing.JTable();
        jButton12 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        Users_crud = new javax.swing.JPopupMenu();
        Edit_User = new javax.swing.JMenuItem();
        Drop_User = new javax.swing.JMenuItem();
        Procedure = new javax.swing.JDialog();
        jButton13 = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel25 = new javax.swing.JLabel();
        Function = new javax.swing.JDialog();
        jButton15 = new javax.swing.JButton();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel24 = new javax.swing.JLabel();
        View = new javax.swing.JDialog();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jLabel_nameSchema = new javax.swing.JLabel();
        newView = new javax.swing.JTextField();
        jTabbedPane3 = new javax.swing.JTabbedPane();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        jTextArea_SQLQuery = new javax.swing.JTextArea();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        jTextArea_DDL = new javax.swing.JTextArea();
        jButton14 = new javax.swing.JButton();
        Triggers = new javax.swing.JDialog();
        jLabel29 = new javax.swing.JLabel();
        btn_afterInsert = new javax.swing.JButton();
        btn_beforeInsert = new javax.swing.JButton();
        btn_beforeUpdate = new javax.swing.JButton();
        btn_afterUpdate = new javax.swing.JButton();
        btn_beforeDelete = new javax.swing.JButton();
        btn_afterDelete = new javax.swing.JButton();
        jScrollPane11 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jButton22 = new javax.swing.JButton();
        jButton23 = new javax.swing.JButton();
        jPopupMenu_Triggers = new javax.swing.JPopupMenu();
        CreateTrigger = new javax.swing.JMenuItem();
        DropTrigger = new javax.swing.JMenuItem();
        AlterTrigger = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTree_C = new javax.swing.JTree();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButton_Ok = new javax.swing.JButton();
        jButton_Cancel = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jText_CHName = new javax.swing.JTextField();
        jText_CName = new javax.swing.JTextField();
        jText_User = new javax.swing.JTextField();
        jText_Port = new javax.swing.JTextField();
        jText_Pass = new javax.swing.JPasswordField();

        Btn_CreateConnection.setText("New Connection");
        Btn_CreateConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_CreateConnectionActionPerformed(evt);
            }
        });
        jPopupMenu_CreateConnection.add(Btn_CreateConnection);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Schemas");
        jTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTreeMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTree);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane5.setViewportView(jTable2);

        jButton9.setBackground(new java.awt.Color(153, 255, 153));
        jButton9.setText("+");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jButton10.setBackground(new java.awt.Color(255, 102, 102));
        jButton10.setText("x");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jMenu1.setText("Disconnect");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Users");
        jMenu2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jMenu2MouseClicked(evt);
            }
        });
        jMenuBar1.add(jMenu2);

        Gestor_jDialog.setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout Gestor_jDialogLayout = new javax.swing.GroupLayout(Gestor_jDialog.getContentPane());
        Gestor_jDialog.getContentPane().setLayout(Gestor_jDialogLayout);
        Gestor_jDialogLayout.setHorizontalGroup(
            Gestor_jDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Gestor_jDialogLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(Gestor_jDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(Gestor_jDialogLayout.createSequentialGroup()
                        .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 1000, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        Gestor_jDialogLayout.setVerticalGroup(
            Gestor_jDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(Gestor_jDialogLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(Gestor_jDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 740, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(Gestor_jDialogLayout.createSequentialGroup()
                        .addGroup(Gestor_jDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton9)
                            .addComponent(jButton10))
                        .addGap(7, 7, 7)
                        .addComponent(jScrollPane5)))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        Btn_NewSchema.setText("New Schema");
        Btn_NewSchema.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_NewSchemaActionPerformed(evt);
            }
        });
        jPopupMenu_NewSchema.add(Btn_NewSchema);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel6.setText("Please enter your user and password");

        jLabel9.setText("Name:");

        jLabel10.setText("Password:");

        jButton1.setText("Ok");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });

        jButton2.setText("Cancel");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton2MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout ValidarUserLayout = new javax.swing.GroupLayout(ValidarUser.getContentPane());
        ValidarUser.getContentPane().setLayout(ValidarUserLayout);
        ValidarUserLayout.setHorizontalGroup(
            ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ValidarUserLayout.createSequentialGroup()
                .addGroup(ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(ValidarUserLayout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addGroup(ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel9))
                        .addGap(29, 29, 29)
                        .addGroup(ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(ValidarUserLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jButton1)
                                .addGap(27, 27, 27)
                                .addComponent(jButton2))
                            .addComponent(Login_User)
                            .addComponent(Login_Pass)))
                    .addGroup(ValidarUserLayout.createSequentialGroup()
                        .addContainerGap(83, Short.MAX_VALUE)
                        .addComponent(jLabel6)))
                .addGap(80, 80, 80))
        );
        ValidarUserLayout.setVerticalGroup(
            ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ValidarUserLayout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addComponent(jLabel6)
                .addGap(41, 41, 41)
                .addGroup(ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(Login_User, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(33, 33, 33)
                .addGroup(ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel10)
                    .addComponent(Login_Pass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addGroup(ValidarUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addGap(38, 38, 38))
        );

        Btn_OpenC.setText("Open Connection");
        Btn_OpenC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_OpenCActionPerformed(evt);
            }
        });
        jPopupMenu_CCRUD.add(Btn_OpenC);

        Btn_EditC.setText("Edit Connection");
        Btn_EditC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_EditCActionPerformed(evt);
            }
        });
        jPopupMenu_CCRUD.add(Btn_EditC);
        jPopupMenu_CCRUD.add(jSeparator1);

        Btn_DeleteC.setText("Delete Connection");
        Btn_DeleteC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_DeleteCActionPerformed(evt);
            }
        });
        jPopupMenu_CCRUD.add(Btn_DeleteC);

        jPanel3.setBackground(java.awt.Color.pink);

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Create a new Schema in the current connection");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 41, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel12.setText("Schema's name:");

        jButton3.setText("Cancel");

        jButton4.setText("Ok");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout NameSchemaLayout = new javax.swing.GroupLayout(NameSchema.getContentPane());
        NameSchema.getContentPane().setLayout(NameSchemaLayout);
        NameSchemaLayout.setHorizontalGroup(
            NameSchemaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(NameSchemaLayout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SchemaName, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(60, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, NameSchemaLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addGap(51, 51, 51)
                .addComponent(jButton3)
                .addGap(225, 225, 225))
        );
        NameSchemaLayout.setVerticalGroup(
            NameSchemaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(NameSchemaLayout.createSequentialGroup()
                .addGroup(NameSchemaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(NameSchemaLayout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(82, 82, 82))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, NameSchemaLayout.createSequentialGroup()
                        .addGroup(NameSchemaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel12)
                            .addComponent(SchemaName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(27, 27, 27)))
                .addGroup(NameSchemaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton4)
                    .addComponent(jButton3))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        Btn_DropSchema.setText("Drop Schema");
        Btn_DropSchema.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Btn_DropSchemaActionPerformed(evt);
            }
        });
        jPopupMenu_DropSchema.add(Btn_DropSchema);

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        jLabel13.setText("Schema:");

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        jLabel14.setText("Name:");

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Data Type", "Size", "PK", "Not Null", "Default", "Comment"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Boolean.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane3.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 815, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 385, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Table", jPanel5);

        DDL_CreateTable.setEditable(false);
        DDL_CreateTable.setColumns(20);
        DDL_CreateTable.setRows(5);
        jScrollPane4.setViewportView(DDL_CreateTable);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 730, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(46, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("DDL", jPanel6);

        jButton5.setText("Ok");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setText("Cancel");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jButton7.setBackground(new java.awt.Color(153, 255, 153));
        jButton7.setText("+");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton8.setBackground(new java.awt.Color(255, 102, 102));
        jButton8.setText("x");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel15.setText("_");

        javax.swing.GroupLayout NewTableLayout = new javax.swing.GroupLayout(NewTable.getContentPane());
        NewTable.getContentPane().setLayout(NewTableLayout);
        NewTableLayout.setHorizontalGroup(
            NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(NewTableLayout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, NewTableLayout.createSequentialGroup()
                        .addComponent(jButton5)
                        .addGap(18, 18, 18)
                        .addComponent(jButton6))
                    .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(NewTableLayout.createSequentialGroup()
                            .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jLabel14)
                                .addComponent(jLabel13))
                            .addGap(18, 18, 18)
                            .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                                .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 815, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, NewTableLayout.createSequentialGroup()
                            .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(48, Short.MAX_VALUE))
        );
        NewTableLayout.setVerticalGroup(
            NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(NewTableLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jLabel15))
                .addGap(18, 18, 18)
                .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton7)
                    .addComponent(jButton8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 424, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(NewTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(jButton6))
                .addGap(16, 16, 16))
        );

        CreateTable.setText("Create Table");
        CreateTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateTableActionPerformed(evt);
            }
        });
        jPopupMenu_Table.add(CreateTable);

        DropTable.setText("Drop Table");
        DropTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DropTableActionPerformed(evt);
            }
        });
        jPopupMenu_Table.add(DropTable);

        AlterTable.setText("Alter Table");
        jPopupMenu_Table.add(AlterTable);

        CreateView.setText("Create View");
        CreateView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateViewActionPerformed(evt);
            }
        });
        jPopupMenu_View.add(CreateView);

        DropView.setText("Drop View");
        DropView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DropViewActionPerformed(evt);
            }
        });
        jPopupMenu_View.add(DropView);

        AlterView.setText("Alter View");
        jPopupMenu_View.add(AlterView);

        CreateProcedure.setText("Create Stored Procedure");
        CreateProcedure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateProcedureActionPerformed(evt);
            }
        });
        jPopupMenu_Procedure.add(CreateProcedure);

        DropProcedure.setText("Drop Stored Procedure");
        DropProcedure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DropProcedureActionPerformed(evt);
            }
        });
        jPopupMenu_Procedure.add(DropProcedure);

        AlterProcedure.setText("Alter Stored Procedure");
        jPopupMenu_Procedure.add(AlterProcedure);

        CreateFunction.setText("Create Function");
        CreateFunction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateFunctionActionPerformed(evt);
            }
        });
        jPopupMenu_Function.add(CreateFunction);

        DropFunction.setText("Drop Function");
        DropFunction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DropFunctionActionPerformed(evt);
            }
        });
        jPopupMenu_Function.add(DropFunction);

        AlterFunction.setText("Alter Function");
        jPopupMenu_Function.add(AlterFunction);

        Users.setModal(true);

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        jLabel16.setText("Users and Privileges");

        jTabbedPane2.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane2StateChanged(evt);
            }
        });

        jLabel17.setText("Login name:");

        jLabel18.setText("Password:");

        jLabel19.setText("Confirm Password:");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(103, 103, 103)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel18)
                            .addComponent(jLabel17))
                        .addGap(31, 31, 31)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextField2)
                            .addComponent(jPasswordField1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGap(62, 62, 62)
                        .addComponent(jLabel19)
                        .addGap(31, 31, 31)
                        .addComponent(jPasswordField2, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)))
                .addContainerGap(50, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap(82, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(53, 53, 53)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(51, 51, 51)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel19)
                    .addComponent(jPasswordField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(150, 150, 150))
        );

        jTabbedPane2.addTab("Login", jPanel7);

        jCheckBox_MaAd.setText("MaintenanceAdmin");

        jCheckBox_DBA.setText("DBA");

        jCheckBox_BaAd.setText("BackupAdmin");

        jCheckBox_PrAd.setText("ProcessAdmin");

        jCheckBox_SeAd.setText("SecurityAdmin");

        jCheckBox_MoAd.setText("MonitorAdmin");

        jCheckBox_DBM.setText("DBManager");

        jCheckBox_DBD.setText("DBDesigner");

        jCheckBox_UsAd.setText("UserAdmin");

        jCheckBox_ReAd.setText("ReplicationAdmin");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap(215, Short.MAX_VALUE)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jCheckBox_MoAd, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBox_MaAd, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBox_DBA, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBox_PrAd, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBox_UsAd, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBox_SeAd, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jCheckBox_DBD, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jCheckBox_ReAd, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE)
                        .addComponent(jCheckBox_BaAd, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jCheckBox_DBM, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(179, 179, 179))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jCheckBox_DBA)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_MaAd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_PrAd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_UsAd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_SeAd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_MoAd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_DBM)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_DBD)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_ReAd)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBox_BaAd)
                .addContainerGap(69, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Roles", jPanel8);

        jTableU.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "User", "Host"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTableU.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableUMouseClicked(evt);
            }
        });
        jScrollPane6.setViewportView(jTableU);
        if (jTableU.getColumnModel().getColumnCount() > 0) {
            jTableU.getColumnModel().getColumn(0).setResizable(false);
            jTableU.getColumnModel().getColumn(1).setResizable(false);
        }

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 560, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 378, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Users", jPanel9);

        jButton12.setText("Apply");
        jButton12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton12MouseClicked(evt);
            }
        });

        jButton11.setText("Cancel");
        jButton11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton11MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout UsersLayout = new javax.swing.GroupLayout(Users.getContentPane());
        Users.getContentPane().setLayout(UsersLayout);
        UsersLayout.setHorizontalGroup(
            UsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UsersLayout.createSequentialGroup()
                .addGroup(UsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(UsersLayout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 615, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(UsersLayout.createSequentialGroup()
                        .addGap(244, 244, 244)
                        .addComponent(jLabel16)))
                .addContainerGap(54, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UsersLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jButton12)
                .addGap(37, 37, 37)
                .addComponent(jButton11)
                .addGap(105, 105, 105))
        );
        UsersLayout.setVerticalGroup(
            UsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UsersLayout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel16)
                .addGap(35, 35, 35)
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 462, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(UsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton12)
                    .addComponent(jButton11))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        Edit_User.setText("Alter User");
        Users_crud.add(Edit_User);

        Drop_User.setText("Drop User");
        Drop_User.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Drop_UserActionPerformed(evt);
            }
        });
        Users_crud.add(Drop_User);

        jButton13.setText("Apply");
        jButton13.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton13MouseClicked(evt);
            }
        });

        jLabel20.setText("Name:");

        jLabel21.setText("DDL:");

        jTextField3.setText("new_procedure");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setText("CREATE PROCEDURE 'new_procedure' ()\nBEGIN\n\nEND");
        jScrollPane7.setViewportView(jTextArea1);

        jLabel25.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel25.setText("New Procedure");

        javax.swing.GroupLayout ProcedureLayout = new javax.swing.GroupLayout(Procedure.getContentPane());
        Procedure.getContentPane().setLayout(ProcedureLayout);
        ProcedureLayout.setHorizontalGroup(
            ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ProcedureLayout.createSequentialGroup()
                .addGroup(ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ProcedureLayout.createSequentialGroup()
                        .addGap(309, 309, 309)
                        .addComponent(jLabel25))
                    .addGroup(ProcedureLayout.createSequentialGroup()
                        .addGap(80, 80, 80)
                        .addGroup(ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButton13)
                            .addGroup(ProcedureLayout.createSequentialGroup()
                                .addGroup(ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel21)
                                    .addComponent(jLabel20))
                                .addGap(32, 32, 32)
                                .addGroup(ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jTextField3)
                                    .addComponent(jScrollPane7, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE))))))
                .addContainerGap(95, Short.MAX_VALUE))
        );
        ProcedureLayout.setVerticalGroup(
            ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ProcedureLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel25)
                .addGap(18, 18, 18)
                .addGroup(ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20))
                .addGap(48, 48, 48)
                .addGroup(ProcedureLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel21)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButton13)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jButton15.setText("Apply");
        jButton15.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton15MouseClicked(evt);
            }
        });

        jLabel22.setText("Name:");

        jLabel23.setText("DDL:");

        jTextField4.setText("new_function");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jTextArea2.setText("CREATE FUNCTION 'new_function' ()\nRETURNS INTEGER\nBEGIN\n\nRETURN 1;\nEND");
        jScrollPane8.setViewportView(jTextArea2);

        jLabel24.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel24.setText("New Function");

        javax.swing.GroupLayout FunctionLayout = new javax.swing.GroupLayout(Function.getContentPane());
        Function.getContentPane().setLayout(FunctionLayout);
        FunctionLayout.setHorizontalGroup(
            FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FunctionLayout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton15)
                    .addGroup(FunctionLayout.createSequentialGroup()
                        .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel23)
                            .addComponent(jLabel22))
                        .addGap(32, 32, 32)
                        .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextField4)
                            .addComponent(jScrollPane8, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE))))
                .addContainerGap(95, Short.MAX_VALUE))
            .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FunctionLayout.createSequentialGroup()
                    .addContainerGap(310, Short.MAX_VALUE)
                    .addComponent(jLabel24)
                    .addContainerGap(309, Short.MAX_VALUE)))
        );
        FunctionLayout.setVerticalGroup(
            FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FunctionLayout.createSequentialGroup()
                .addGap(75, 75, 75)
                .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22))
                .addGap(47, 47, 47)
                .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel23)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jButton15)
                .addContainerGap(22, Short.MAX_VALUE))
            .addGroup(FunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, FunctionLayout.createSequentialGroup()
                    .addContainerGap(26, Short.MAX_VALUE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(544, Short.MAX_VALUE)))
        );

        jLabel26.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        jLabel26.setText("Schema:");

        jLabel27.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        jLabel27.setText("Name:");

        jLabel28.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel28.setText("Create View");

        jLabel_nameSchema.setText("_");

        jTabbedPane3.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane3StateChanged(evt);
            }
        });

        jTextArea_SQLQuery.setColumns(20);
        jTextArea_SQLQuery.setRows(5);
        jTextArea_SQLQuery.setText("SELECT \n    \nFROM ");
        jScrollPane9.setViewportView(jTextArea_SQLQuery);

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane3.addTab("SQL Query", jPanel10);

        jTextArea_DDL.setColumns(20);
        jTextArea_DDL.setRows(5);
        jScrollPane10.setViewportView(jTextArea_DDL);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane3.addTab("DDL", jPanel11);

        jButton14.setText("Apply");
        jButton14.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton14MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout ViewLayout = new javax.swing.GroupLayout(View.getContentPane());
        View.getContentPane().setLayout(ViewLayout);
        ViewLayout.setHorizontalGroup(
            ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ViewLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel28)
                .addGap(226, 226, 226))
            .addGroup(ViewLayout.createSequentialGroup()
                .addGap(56, 56, 56)
                .addGroup(ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ViewLayout.createSequentialGroup()
                        .addGroup(ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addComponent(jLabel27))
                        .addGap(34, 34, 34)
                        .addGroup(ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(newView, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
                            .addComponent(jLabel_nameSchema, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jButton14)
                        .addComponent(jTabbedPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(42, 42, 42))
        );
        ViewLayout.setVerticalGroup(
            ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ViewLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel28)
                .addGap(18, 18, 18)
                .addGroup(ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(jLabel_nameSchema))
                .addGap(24, 24, 24)
                .addGroup(ViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newView, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27))
                .addGap(42, 42, 42)
                .addComponent(jTabbedPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 382, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton14)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        jLabel29.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel29.setText("New Trigger");

        btn_afterInsert.setText("After Insert");
        btn_afterInsert.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btn_afterInsertMouseClicked(evt);
            }
        });

        btn_beforeInsert.setText("Before Insert");
        btn_beforeInsert.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btn_beforeInsertMouseClicked(evt);
            }
        });

        btn_beforeUpdate.setText("Before Update");
        btn_beforeUpdate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btn_beforeUpdateMouseClicked(evt);
            }
        });

        btn_afterUpdate.setText("After Update");
        btn_afterUpdate.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btn_afterUpdateMouseClicked(evt);
            }
        });

        btn_beforeDelete.setText("Before Delete");
        btn_beforeDelete.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btn_beforeDeleteMouseClicked(evt);
            }
        });

        btn_afterDelete.setText("After Delete");
        btn_afterDelete.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btn_afterDeleteMouseClicked(evt);
            }
        });

        jTextArea3.setColumns(20);
        jTextArea3.setRows(5);
        jScrollPane11.setViewportView(jTextArea3);

        jButton22.setText("Cancel");
        jButton22.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton22MouseClicked(evt);
            }
        });

        jButton23.setText("Apply");
        jButton23.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton23MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout TriggersLayout = new javax.swing.GroupLayout(Triggers.getContentPane());
        Triggers.getContentPane().setLayout(TriggersLayout);
        TriggersLayout.setHorizontalGroup(
            TriggersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TriggersLayout.createSequentialGroup()
                .addGroup(TriggersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(TriggersLayout.createSequentialGroup()
                        .addContainerGap(551, Short.MAX_VALUE)
                        .addComponent(jButton23)
                        .addGap(18, 18, 18)
                        .addComponent(jButton22))
                    .addGroup(TriggersLayout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addGroup(TriggersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btn_beforeUpdate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_afterInsert, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_beforeInsert, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_afterUpdate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_afterDelete, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_beforeDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 520, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(41, 41, 41))
            .addComponent(jLabel29, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        TriggersLayout.setVerticalGroup(
            TriggersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TriggersLayout.createSequentialGroup()
                .addGroup(TriggersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(TriggersLayout.createSequentialGroup()
                        .addGap(46, 46, 46)
                        .addComponent(jLabel29)
                        .addGap(42, 42, 42)
                        .addComponent(jScrollPane11, javax.swing.GroupLayout.PREFERRED_SIZE, 415, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 29, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TriggersLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_beforeInsert)
                        .addGap(32, 32, 32)
                        .addComponent(btn_afterInsert)
                        .addGap(32, 32, 32)
                        .addComponent(btn_beforeUpdate)
                        .addGap(32, 32, 32)
                        .addComponent(btn_afterUpdate)
                        .addGap(32, 32, 32)
                        .addComponent(btn_beforeDelete)
                        .addGap(32, 32, 32)
                        .addComponent(btn_afterDelete)
                        .addGap(70, 70, 70)))
                .addGroup(TriggersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton23)
                    .addComponent(jButton22))
                .addGap(47, 47, 47))
        );

        CreateTrigger.setText("Create Trigger");
        CreateTrigger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateTriggerActionPerformed(evt);
            }
        });
        jPopupMenu_Triggers.add(CreateTrigger);

        DropTrigger.setText("Drop Trigger");
        DropTrigger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DropTriggerActionPerformed(evt);
            }
        });
        jPopupMenu_Triggers.add(DropTrigger);

        AlterTrigger.setText("Alter Trigger");
        jPopupMenu_Triggers.add(AlterTrigger);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(java.awt.Color.pink);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 40)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("WELCOME BACK");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1383, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(49, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(47, 47, 47))
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("Connections");
        jTree_C.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree_C.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree_CMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jTree_C);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 25)); // NOI18N
        jLabel2.setText("Connect to the server:");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Connection name:");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("User:");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Password:");

        jButton_Ok.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButton_Ok.setText("Ok");
        jButton_Ok.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton_OkMouseClicked(evt);
            }
        });

        jButton_Cancel.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButton_Cancel.setText("Cancel");
        jButton_Cancel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton_CancelMouseClicked(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setText("Hostname:");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setText("Port:");

        jText_CHName.setText("127.0.0.1");

        jText_Port.setText("3306");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(397, 397, 397))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jText_Pass, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addGap(97, 97, 97)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addGap(136, 136, 136)
                                .addComponent(jText_CHName, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(72, 72, 72)
                                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(jPanel4Layout.createSequentialGroup()
                                        .addComponent(jLabel8)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jText_Port, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jText_CName, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jText_User, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGap(293, 293, 293)
                                .addComponent(jButton_Ok)
                                .addGap(148, 148, 148)
                                .addComponent(jButton_Cancel)))))
                .addContainerGap(132, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(jLabel2)
                .addGap(55, 55, 55)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jText_CName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 34, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8)
                    .addComponent(jText_CHName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jText_Port, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(49, 49, 49)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jText_User, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(49, 49, 49)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addComponent(jText_Pass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(73, 73, 73)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton_Ok)
                    .addComponent(jButton_Cancel))
                .addGap(62, 62, 62))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(47, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(179, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 650, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTreeMouseClicked

        if (evt.isMetaDown()) {
            int row = jTree.getClosestRowForLocation(evt.getX(), evt.getY());
            jTree.setSelectionRow(row);
            Object selectedNodeObject = jTree.getSelectionPath().getLastPathComponent();

            if (selectedNodeObject instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedNodeObject;

                treeModel = (DefaultTreeModel) jTree.getModel();
                raiz = (DefaultMutableTreeNode) treeModel.getRoot();

                if (selectedNode.equals(raiz)) {
                    jPopupMenu_NewSchema.show(evt.getComponent(),
                            evt.getX(), evt.getY());
                } else if (selectedNode.getParent() != null && selectedNode.getParent().equals(raiz)) {
                    jPopupMenu_DropSchema.show(evt.getComponent(),
                            evt.getX(), evt.getY());
                } else if (selectedNode.getParent() != null && selectedNode.getParent().getParent() != null
                        && selectedNode.getParent().getParent().equals(raiz)) {
                    nameSchema = selectedNode.getParent().toString();
                    if (selectedNode.toString().equals("Tables")) {
                        jPopupMenu_Table.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateTable.setEnabled(true);
                        DropTable.setEnabled(false);
                        AlterTable.setEnabled(false);
                    } else if (selectedNode.toString().equals("Views")) {
                        jPopupMenu_View.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateView.setEnabled(true);
                        DropView.setEnabled(false);
                        AlterView.setEnabled(false);
                    } else if (selectedNode.toString().equals("Stored Procedures")) {
                        jPopupMenu_Procedure.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateProcedure.setEnabled(true);
                        DropProcedure.setEnabled(false);
                        AlterProcedure.setEnabled(false);
                    } else if (selectedNode.toString().equals("Functions")) {
                        jPopupMenu_Function.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateFunction.setEnabled(true);
                        DropFunction.setEnabled(false);
                        AlterFunction.setEnabled(false);
                    }
                } else if (selectedNode.getParent() != null && selectedNode.getParent().getParent() != null
                        && selectedNode.getParent().getParent().getParent() != null && selectedNode.getParent().getParent().getParent().equals(raiz)) {
                    nameSchema = selectedNode.getParent().getParent().toString();
                    if (selectedNode.getParent().toString().equals("Tables")) {
                        jPopupMenu_Table.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateTable.setEnabled(false);
                        DropTable.setEnabled(true);
                        AlterTable.setEnabled(true);
                    } else if (selectedNode.getParent().toString().equals("Views")) {
                        jPopupMenu_View.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        nameView = selectedNode.toString();
                        CreateView.setEnabled(false);
                        DropView.setEnabled(true);
                        AlterView.setEnabled(true);
                    } else if (selectedNode.getParent().toString().equals("Stored Procedures")) {
                        jPopupMenu_Procedure.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        nameProcedure = selectedNode.toString();
                        CreateProcedure.setEnabled(false);
                        DropProcedure.setEnabled(true);
                        AlterProcedure.setEnabled(true);
                    } else if (selectedNode.getParent().toString().equals("Functions")) {
                        jPopupMenu_Function.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        nameFunction = selectedNode.toString();
                        CreateFunction.setEnabled(false);
                        DropFunction.setEnabled(true);
                        AlterFunction.setEnabled(true);
                    }
                } else if (selectedNode.getParent() != null && selectedNode.getParent().getParent() != null && selectedNode.getParent().getParent().getParent() != null
                        && selectedNode.getParent().getParent().getParent().getParent() != null && selectedNode.getParent().getParent().getParent().getParent().equals(raiz)) {
                    nameSchema = selectedNode.getParent().getParent().getParent().toString();
                    nameTable = selectedNode.getParent().toString();
                    if (selectedNode.toString().equals("Triggers")) {
                        jPopupMenu_Triggers.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateTrigger.setEnabled(true);
                        DropTrigger.setEnabled(false);
                        AlterTrigger.setEnabled(false);
                    }
                }else{
                    nameTrigger = selectedNode.toString();
                    nameSchema = selectedNode.getParent().getParent().getParent().getParent().toString();
                    if (selectedNode.getParent().toString().equals("Triggers")) {
                        jPopupMenu_Triggers.show(evt.getComponent(),
                                evt.getX(), evt.getY());
                        CreateTrigger.setEnabled(false);
                        DropTrigger.setEnabled(true);
                        AlterTrigger.setEnabled(true);
                    }
                }

            }
        }

    }//GEN-LAST:event_jTreeMouseClicked

    private void Btn_CreateConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Btn_CreateConnectionActionPerformed
        jPanel4.setVisible(true);
        operation = "create";
    }//GEN-LAST:event_Btn_CreateConnectionActionPerformed

    private void jButton_OkMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_OkMouseClicked
        //CRUD CONNECTION
        url = "jdbc:mysql://localhost:3306/connections";
        user = "root";
        password = "contra";

        String CName = jText_CName.getText();
        String CUser = jText_User.getText();
        String CPass = jText_Pass.getText();
        String CPort = jText_Port.getText();
        String CHName = jText_CHName.getText();

        if (CName.isEmpty() || CUser.isEmpty() || CPass.isEmpty() || CPort.isEmpty() || CHName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter data before inserting!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (operation.equals("create")) {
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                String consulta = "SELECT COUNT(*) AS existe FROM users_conn WHERE name = ? AND password = ?";

                try (PreparedStatement pstmt = connection.prepareStatement(consulta)) {
                    pstmt.setString(1, CUser);
                    pstmt.setString(2, CPass);

                    try (ResultSet resultado = pstmt.executeQuery()) {
                        if (resultado.next()) {
                            int existe = resultado.getInt("existe");
                            if (existe > 0) {
                                String query = "INSERT INTO connections_data (name, user, password, host, port) VALUES (?, ?, ?, ?, ?)";

                                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                                    preparedStatement.setString(1, CName);
                                    preparedStatement.setString(2, CUser);
                                    preparedStatement.setString(3, CPass);
                                    preparedStatement.setString(4, CHName);
                                    preparedStatement.setString(5, CPort);

                                    int rowsAffected = preparedStatement.executeUpdate();

                                    if (rowsAffected > 0) {
                                        JOptionPane.showMessageDialog(null, "Connection created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                        ConnectionTree();
                                        jPanel4.setVisible(false);
                                        usuarioIngresado = CUser;
                                        contraIngresado = CPass;
                                    } else {
                                        JOptionPane.showMessageDialog(null, "Failed to create the connection!", "Error", JOptionPane.ERROR_MESSAGE);
                                        jText_CName.setText("");
                                        jText_User.setText("");
                                        jText_Pass.setText("");
                                        jText_CHName.setText("127.0.0.1");
                                        jText_Port.setText("3306");
                                    }
                                }
                            } else {
                                JOptionPane.showMessageDialog(null, "An error occurred while creation connection!", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred while creation connection!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (operation.equals("edit")) {
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                String query = "UPDATE connections_data SET name = '" + jText_CName.getText() + "', user = '" + jText_User.getText() + "', password = '"
                        + jText_Pass.getText() + "', host = '" + jText_CHName.getText() + "', port = '" + jText_Port.getText()
                        + "' WHERE id = " + id_connection;
                System.out.println(query);

                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(query);
                    JOptionPane.showMessageDialog(null, "Connection updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    jPanel4.setVisible(false);
                    ConnectionTree();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred while updating the connection!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }


    }//GEN-LAST:event_jButton_OkMouseClicked

    private void jButton_CancelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton_CancelMouseClicked
        jText_CName.setText("");
        jText_User.setText("");
        jText_Pass.setText("");
        jText_CHName.setText("127.0.0.1");
        jText_Port.setText("3306");
        jPanel4.setVisible(false);
    }//GEN-LAST:event_jButton_CancelMouseClicked

    private void jTree_CMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree_CMouseClicked

        if (evt.isMetaDown()) {
            int row = jTree_C.getClosestRowForLocation(evt.getX(), evt.getY());
            jTree_C.setSelectionRow(row);
            Object selectedNodeObject = jTree_C.getSelectionPath().getLastPathComponent();

            if (selectedNodeObject instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedNodeObject;

                treeModel = (DefaultTreeModel) jTree_C.getModel();
                raiz = (DefaultMutableTreeNode) treeModel.getRoot();

                if (selectedNode.equals(raiz)) {
                    jPopupMenu_CreateConnection.show(evt.getComponent(),
                            evt.getX(), evt.getY());
                } else {
                    jPopupMenu_CCRUD.show(evt.getComponent(),
                            evt.getX(), evt.getY());
                }

            }
        }

    }//GEN-LAST:event_jTree_CMouseClicked

    private void Btn_NewSchemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Btn_NewSchemaActionPerformed
        NameSchema.pack();
        NameSchema.setLocationRelativeTo(this);
        NameSchema.setVisible(true);
    }//GEN-LAST:event_Btn_NewSchemaActionPerformed

    private void Btn_OpenCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Btn_OpenCActionPerformed
        Login_User.setText("");
        Login_Pass.setText("");
        ValidarUser.pack();
        ValidarUser.setLocationRelativeTo(this);
        ValidarUser.setVisible(true);
        operation = "read";
    }//GEN-LAST:event_Btn_OpenCActionPerformed

    private void Btn_EditCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Btn_EditCActionPerformed
        Login_User.setText("");
        Login_Pass.setText("");
        ValidarUser.pack();
        ValidarUser.setLocationRelativeTo(this);
        ValidarUser.setVisible(true);
        operation = "edit";
    }//GEN-LAST:event_Btn_EditCActionPerformed

    private void Btn_DeleteCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Btn_DeleteCActionPerformed
        Login_User.setText("");
        Login_Pass.setText("");
        ValidarUser.pack();
        ValidarUser.setLocationRelativeTo(this);
        ValidarUser.setVisible(true);
        operation = "delete";
    }//GEN-LAST:event_Btn_DeleteCActionPerformed

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        //ABRIR, EDITAR O ELIMINAR UNA CONEXIÓN
        url = "jdbc:mysql://localhost:3306/connections";
        user = "root";
        password = "contra";
        id_connection = 0;

        userIn = Login_User.getText();
        contraIn = Login_Pass.getText();

        TreePath path = jTree_C.getLeadSelectionPath();
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            connectionName = node.getUserObject().toString();
        }

        try (Connection conexion = DriverManager.getConnection(url, user, password)) {
            String consulta = "SELECT COUNT(*) AS existe FROM connections_data WHERE user = ? AND password = ? AND name = ?";

            try (PreparedStatement pstmt = conexion.prepareStatement(consulta)) {
                pstmt.setString(1, userIn);
                pstmt.setString(2, contraIn);
                pstmt.setString(3, connectionName);

                try (ResultSet resultado = pstmt.executeQuery()) {
                    if (resultado.next()) {
                        int existe = resultado.getInt("existe");
                        if (existe > 0) {

                            if (operation.equals("read")) {
                                JOptionPane.showMessageDialog(null, "Connection successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                this.setVisible(false);
                                Gestor_jDialog.pack();
                                Gestor_jDialog.setLocationRelativeTo(this);
                                Gestor_jDialog.setVisible(true);

                                consulta = "SELECT * FROM connections_data WHERE name = ?";

                                try (PreparedStatement prepared = conexion.prepareStatement(consulta)) {
                                    prepared.setString(1, connectionName);

                                    try (ResultSet result = prepared.executeQuery()) {
                                        if (result.next()) {
                                            id_connection = result.getInt("id");
                                        }
                                    }
                                }

                                SchemasTree();

                            } else if (operation.equals("edit")) {
                                jPanel4.setVisible(true);
                                ValidarUser.setVisible(false);
                                consulta = "SELECT * FROM connections_data WHERE name = ?";

                                try (PreparedStatement prepared = conexion.prepareStatement(consulta)) {
                                    prepared.setString(1, connectionName);

                                    try (ResultSet result = prepared.executeQuery()) {
                                        if (result.next()) {
                                            id_connection = result.getInt("id");
                                            System.out.println(id_connection);
                                            jText_CName.setText(result.getString("name"));
                                            jText_User.setText(result.getString("user"));
                                            jText_CHName.setText(result.getString("host"));
                                            jText_Port.setText(result.getString("port"));
                                        }
                                    }
                                }

                            } else if (operation.equals("delete")) {
                                try (Statement statement = conexion.createStatement()) {
                                    String query = "DELETE FROM connections_data WHERE name = '" + connectionName + "'";
                                    System.out.println(query);
                                    statement.executeUpdate(query);
                                    JOptionPane.showMessageDialog(null, "Connection deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                    ConnectionTree();
                                }
                            }

                            ValidarUser.setVisible(false);
                        } else {
                            JOptionPane.showMessageDialog(null, "User or password is incorrect!", "Error", JOptionPane.ERROR_MESSAGE);
                            ValidarUser.setVisible(false);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }//GEN-LAST:event_jButton1MouseClicked

    private void jButton2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton2MouseClicked
        ValidarUser.setVisible(false);
    }//GEN-LAST:event_jButton2MouseClicked

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // CREATE DATABASE
        url = "jdbc:mysql://localhost:3306";
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            Statement statement = connection.createStatement();

            String createSchemaQuery = "CREATE DATABASE " + SchemaName.getText();
            statement.executeUpdate(createSchemaQuery);

            statement.execute("USE connections");

            String insertQuery = "INSERT INTO database_conn (name, id_connection) VALUES (?,?)";

            try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
                pstmt.setString(1, SchemaName.getText());
                pstmt.setInt(2, id_connection);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Schema created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                    SchemasTree();
                }
            }

            statement.close();
            connection.close();

            NameSchema.setVisible(false);
            SchemaName.setText("");

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Schema could not be created!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void Btn_DropSchemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Btn_DropSchemaActionPerformed
        //ELIMINAR DATABASE
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
        String dbName = selectedNode.toString();

        url = "jdbc:mysql://localhost:3306/" + dbName;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            Statement statement = connection.createStatement();

            String dropQuery = "DROP DATABASE " + dbName;
            statement.executeUpdate(dropQuery);

            statement.execute("USE connections");

            String insertQuery = "DELETE FROM database_conn WHERE name = ?";

            try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
                pstmt.setString(1, dbName);

                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Database deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    SchemasTree();
                }
            }

            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database could not be deleted!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_Btn_DropSchemaActionPerformed

    private void CreateTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateTableActionPerformed
        NewTable.pack();
        NewTable.setLocationRelativeTo(this);
        NewTable.setVisible(true);
        jLabel15.setText(nameSchema);
        String[] comboBoxOptions = {"INT", "VARCHAR", "DECIMAL", "DATETIME", "BLOB", "-", "BINARY", "BLOB", "LONGBLOB",
            "MEDIUMBLOB", "TINYBLOB", "TINYBLOB", "VARBINARY", "-", "DATE", "DATETIME", "TIME", "TIMESTAMP", "YEAR",
            "-", "GEOMETRY", "GEOMETRYCOLLECTION", "LINESTRING", "MULTILINESTRING", "MULTIPOINT", "MULTIPOLYGON", "POIN",
            "POLYGON", "-", "BIGINT", "DECIMAL", "DOUBLE", "FLOAT", "INT", "MEDIUMINT", "REAL", "SMALLINT", "TINYINT", "-",
            "CHAR", "JSON", "NCHAR", "NVARCHAR", "VARCHAR", "-", "LONGTEXT", "MEDIUMTEXT", "TEXT", "TINYTEXT", "-", "BIT",
            "BOOLEAN", "ENUM", "SET", "-"};

        JComboBox<String> comboBox = new JComboBox<>(comboBoxOptions);
        TableColumn tcTypes = jTable1.getColumnModel().getColumn(1);
        tcTypes.setCellEditor(new DefaultCellEditor(comboBox));
    }//GEN-LAST:event_CreateTableActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.addRow(new Object[]{"", "", "", false, false, "", ""});
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int[] rows = jTable1.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            model.removeRow(rows[i] - i);
        }
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        NewTable.setVisible(false);
        jTextField1.setText("");
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            String createTableQuery = "CREATE TABLE " + jTextField1.getText() + "(";

            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {

                    switch (col) {
                        case 2:
                            if (!model.getValueAt(row, col - 1).equals("INT")) {
                                createTableQuery += "(" + model.getValueAt(row, col) + ") ";
                            }
                            break;
                        case 3:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "PRIMARY KEY ";
                            }
                            break;
                        case 4:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "NOT NULL ";
                            }
                            break;
                        case 5:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "DEFAULT '" + model.getValueAt(row, col) + "' ";
                            }
                            break;
                        case 6:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "COMMENT '" + model.getValueAt(row, col) + "' ";
                            }
                            break;
                        default:
                            createTableQuery += model.getValueAt(row, col) + " ";
                            break;
                    }
                }
                if (row != model.getRowCount() - 1) {
                    createTableQuery += ",\n";
                }
            }
            createTableQuery += ")";

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableQuery);
                JOptionPane.showMessageDialog(null, "Table created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                NewTable.setVisible(false);
                jTextField1.setText("");
                model = (DefaultTableModel) jTable1.getModel();
                model.setRowCount(0);
                SchemasTree();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Table could not be created!" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "There seems to be an error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
        if (jTabbedPane1.getSelectedIndex() == 1) {
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            String createTableQuery = "CREATE TABLE " + jTextField1.getText() + "( ";

            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {

                    switch (col) {
                        case 2:
                            if (!model.getValueAt(row, col - 1).equals("INT")) {
                                createTableQuery += "(" + model.getValueAt(row, col) + ") ";
                            }
                            break;
                        case 3:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "PRIMARY KEY ";
                            }
                            break;
                        case 4:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "NOT NULL ";
                            }
                            break;
                        case 5:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "DEFAULT '" + model.getValueAt(row, col) + "' ";
                            }
                            break;
                        case 6:
                            if (model.getValueAt(row, col).equals(true)) {
                                createTableQuery += "COMMENT '" + model.getValueAt(row, col) + "' ";
                            }
                            break;
                        default:
                            createTableQuery += model.getValueAt(row, col) + " ";
                            break;
                    }
                }
                if (row != model.getRowCount() - 1) {
                    createTableQuery += ",\n";
                }

            }
            createTableQuery += ")";

            DDL_CreateTable.setText(createTableQuery);
        }
    }//GEN-LAST:event_jTabbedPane1StateChanged

    private void DropTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DropTableActionPerformed
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
        String schema = selectedNode.getParent().getParent().toString();

        url = "jdbc:mysql://localhost:3306/" + schema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            Statement statement = connection.createStatement();

            String dropQuery = "DROP TABLE " + selectedNode.toString();
            int rowsAffected = statement.executeUpdate(dropQuery);
            System.out.println(dropQuery);

            JOptionPane.showMessageDialog(null, "Table deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_DropTableActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        if (jTable2.getSelectedRow() != -1) {
            DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
            int columnCount = model.getColumnCount();

            user = "root";
            password = "contra";

            String insertSQL = "INSERT INTO " + nameTable + " (";

            for (int col = 0; col < columnCount; col++) {
                insertSQL += model.getColumnName(col);

                if (col < columnCount - 1) {
                    insertSQL += ", ";
                }
            }

            insertSQL += ") VALUES (";

            for (int col = 0; col < columnCount; col++) {
                Object value = model.getValueAt(jTable2.getSelectedRow(), col);

                if (isNumeric(value.toString())) {
                    insertSQL += value.toString();
                } else {
                    insertSQL += "'" + value.toString() + "'";
                }

                if (col < columnCount - 1) {
                    insertSQL += ", ";
                }
            }

            insertSQL += ");";

            try (
                    Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + nameSchema, user, password); PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
                preparedStatement.executeUpdate();
                JOptionPane.showMessageDialog(null, "Data inserted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                LlenarTabla(nameTable, nameSchema);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: \n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        if (jTable2.getSelectedRow() != -1) {
            url = "jdbc:mysql://localhost:3306/" + nameSchema;
            user = "root";
            password = "contra";

            DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
            String columna = model.getColumnName(0);
            int selectedRow = jTable2.getSelectedRow();

            try (Connection connection = DriverManager.getConnection(url, user, password)) {

                Statement statement = connection.createStatement();

                String deleteQuery = "DELETE FROM " + nameTable + " WHERE " + columna + " = " + model.getValueAt(selectedRow, 0);

                int rowsAffected = statement.executeUpdate(deleteQuery);

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Data deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    int[] selectedRows = jTable2.getSelectedRows();
                    for (int i = selectedRows.length - 1; i >= 0; i--) {
                        model.removeRow(selectedRows[i]);
                    }

                }

            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }//GEN-LAST:event_jButton10ActionPerformed

    private void jMenu2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jMenu2MouseClicked
        jTextField2.setText("");
        jPasswordField1.setText("");
        jPasswordField2.setText("");

        jCheckBox_DBA.setSelected(false);
        jCheckBox_MaAd.setSelected(false);
        jCheckBox_PrAd.setSelected(false);
        jCheckBox_UsAd.setSelected(false);
        jCheckBox_SeAd.setSelected(false);
        jCheckBox_MoAd.setSelected(false);
        jCheckBox_DBM.setSelected(false);
        jCheckBox_DBD.setSelected(false);
        jCheckBox_ReAd.setSelected(false);
        jCheckBox_BaAd.setSelected(false);

        Users.pack();
        Users.setLocationRelativeTo(this);
        Users.setVisible(true);
    }//GEN-LAST:event_jMenu2MouseClicked

    private void jButton12MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton12MouseClicked
        url = "jdbc:mysql://localhost:3306/connections";
        user = "root";
        password = "contra";

        if (jPasswordField1.getText().equals(jPasswordField2.getText())) {
            if (jCheckBox_DBA.isSelected()) {
                try {
                    createUserDBA(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_MaAd.isSelected()) {
                try {
                    createUserMaintenanceAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_PrAd.isSelected()) {
                try {
                    createUserProcessAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_UsAd.isSelected()) {
                try {
                    createUserUserAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_SeAd.isSelected()) {
                try {
                    createUserSecurityAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_MoAd.isSelected()) {
                try {
                    createUserMonitorAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_DBM.isSelected()) {
                try {
                    createUserDBManager(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_DBD.isSelected()) {
                try {
                    createUserDBDesigner(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_ReAd.isSelected()) {
                try {
                    createUserReplicationAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (jCheckBox_BaAd.isSelected()) {
                try {
                    createUserBackupAdmin(url, user, password, jTextField2.getText(), jPasswordField1.getText());
                } catch (SQLException ex) {
                    Logger.getLogger(Gestor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                String insertQuery = "INSERT INTO users_conn (name, password) VALUES (?, ?)";

                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                    preparedStatement.setString(1, jTextField2.getText());
                    preparedStatement.setString(2, jPasswordField1.getText());

                    int rowsAffected = preparedStatement.executeUpdate();

                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jButton12MouseClicked

    private void jButton11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton11MouseClicked
        Users.setVisible(false);
    }//GEN-LAST:event_jButton11MouseClicked

    private void jTabbedPane2StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane2StateChanged
        if (jTabbedPane2.getSelectedIndex() == 2) {
            try {
                url = "jdbc:mysql://localhost:3306/connections";
                user = "root";
                password = "contra";

                connection = DriverManager.getConnection(url, user, password);
                statement = connection.createStatement();
                String query = "SELECT User, Host FROM mysql.user";
                resultSet = statement.executeQuery(query);
                DefaultTableModel model = (DefaultTableModel) jTableU.getModel();

                while (resultSet.next()) {
                    String host = resultSet.getString("User");
                    String user = resultSet.getString("Host");
                    model.addRow(new Object[]{host, user});
                }

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jTabbedPane2StateChanged

    private void jTableUMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableUMouseClicked
        if (evt.isMetaDown())
            Users_crud.show(evt.getComponent(), evt.getX(), evt.getY());
    }//GEN-LAST:event_jTableUMouseClicked

    private void Drop_UserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Drop_UserActionPerformed
        url = "jdbc:mysql://localhost:3306/connections";
        user = "root";
        password = "contra";

        try {
            connection = DriverManager.getConnection(url, user, password);

            String usernameToDrop = (String) jTableU.getValueAt(jTableU.getSelectedRow(), 0);
            String dropUserSQL = "DROP USER '" + usernameToDrop + "'@'localhost'";

            Statement statement = connection.createStatement();
            statement.executeUpdate(dropUserSQL);

            JOptionPane.showMessageDialog(null, "User dropped successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            try {
                connection = DriverManager.getConnection(url, user, password);
                statement = connection.createStatement();
                String query = "SELECT User, Host FROM mysql.user";
                resultSet = statement.executeQuery(query);
                DefaultTableModel model = (DefaultTableModel) jTableU.getModel();

                for (int i = 0; i < jTableU.getRowCount(); i++) {
                    model.removeRow(i);
                    i -= 1;
                }

                while (resultSet.next()) {
                    String host = resultSet.getString("User");
                    String user = resultSet.getString("Host");
                    model.addRow(new Object[]{host, user});
                }

                resultSet.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_Drop_UserActionPerformed

    private void CreateProcedureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateProcedureActionPerformed
        Procedure.pack();
        Procedure.setLocationRelativeTo(this);
        Procedure.setVisible(true);

        jTextField3.setText("new_procedure");
        jTextArea1.setText("CREATE PROCEDURE 'new_procedure' ()\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_CreateProcedureActionPerformed

    private void jButton13MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton13MouseClicked

        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {

            Connection connection = DriverManager.getConnection(url, user, password);

            String sqlQuery = jTextArea1.getText();

            Statement statement = connection.createStatement();
            statement.execute(sqlQuery);

            JOptionPane.showMessageDialog(null, "Stored Procedure created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

    }//GEN-LAST:event_jButton13MouseClicked

    private void jButton15MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton15MouseClicked
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String sqlQuery = jTextArea2.getText();

            Statement statement = connection.createStatement();
            statement.execute(sqlQuery);

            JOptionPane.showMessageDialog(null, "Function created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton15MouseClicked

    private void CreateFunctionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateFunctionActionPerformed
        Function.pack();
        Function.setLocationRelativeTo(this);
        Function.setVisible(true);

        jTextField4.setText("new_function");
        jTextArea2.setText("CREATE FUNCTION 'new_function' ()\n"
                + "RETURNS INTEGER\n"
                + "BEGIN\n"
                + "\n"
                + "RETURN 1;\n"
                + "END");
    }//GEN-LAST:event_CreateFunctionActionPerformed

    private void DropFunctionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DropFunctionActionPerformed
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String sqlQuery = "DROP FUNCTION IF EXISTS " + nameFunction;

            Statement statement = connection.createStatement();
            statement.execute(sqlQuery);

            JOptionPane.showMessageDialog(null, "Function dropped successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_DropFunctionActionPerformed

    private void DropProcedureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DropProcedureActionPerformed
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String sqlQuery = "DROP PROCEDURE IF EXISTS " + nameProcedure;

            Statement statement = connection.createStatement();
            statement.execute(sqlQuery);

            JOptionPane.showMessageDialog(null, "Procedure dropped successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_DropProcedureActionPerformed

    private void CreateViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateViewActionPerformed
        View.pack();
        View.setLocationRelativeTo(this);
        View.setVisible(true);

        jLabel_nameSchema.setText(nameSchema);
        jTextArea_SQLQuery.setText("SELECT\n\n"
                + "FROM ");
    }//GEN-LAST:event_CreateViewActionPerformed

    private void jButton14MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton14MouseClicked
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String createViewQuery = "CREATE VIEW " + newView.getText() + " AS " + jTextArea_SQLQuery.getText();

            Statement statement = connection.createStatement();
            statement.executeUpdate(createViewQuery);

            JOptionPane.showMessageDialog(null, "View created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();
            View.setVisible(false);

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton14MouseClicked

    private void jTabbedPane3StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane3StateChanged
        if (jTabbedPane3.getSelectedIndex() == 1) {
            String createViewQuery = "CREATE VIEW " + newView.getText() + " AS " + jTextArea_SQLQuery.getText();
            jTextArea_DDL.setText(createViewQuery);
        }
    }//GEN-LAST:event_jTabbedPane3StateChanged

    private void DropViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DropViewActionPerformed
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String sqlQuery = "DROP VIEW " + nameView;

            Statement statement = connection.createStatement();
            statement.execute(sqlQuery);

            JOptionPane.showMessageDialog(null, "View dropped successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_DropViewActionPerformed

    private void CreateTriggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateTriggerActionPerformed
        Triggers.pack();
        Triggers.setLocationRelativeTo(this);
        Triggers.setVisible(true);
    }//GEN-LAST:event_CreateTriggerActionPerformed

    private void DropTriggerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DropTriggerActionPerformed
         url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String sqlQuery = "DROP TRIGGER " + nameTrigger;

            Statement statement = connection.createStatement();
            statement.execute(sqlQuery);

            JOptionPane.showMessageDialog(null, "Trigger dropped successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_DropTriggerActionPerformed

    private void btn_beforeInsertMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_beforeInsertMouseClicked
        jTextArea3.setText("CREATE DEFINER = CURRENT_USER TRIGGER `" + nameSchema + "`.`" + nameTable + "_BEFORE_INSERT` BEFORE INSERT ON `" + nameTable + "` FOR EACH ROW\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_btn_beforeInsertMouseClicked

    private void btn_afterInsertMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_afterInsertMouseClicked
        jTextArea3.setText("CREATE DEFINER = CURRENT_USER TRIGGER `" + nameSchema + "`.`" + nameTable + "_AFTER_INSERT` AFTER INSERT ON `" + nameTable + "` FOR EACH ROW\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_btn_afterInsertMouseClicked

    private void btn_beforeUpdateMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_beforeUpdateMouseClicked
        jTextArea3.setText("CREATE DEFINER = CURRENT_USER TRIGGER `" + nameSchema + "`.`" + nameTable + "_AFTER_UPDATE` AFTER UPDATE ON `" + nameTable + "` FOR EACH ROW\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_btn_beforeUpdateMouseClicked

    private void btn_afterUpdateMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_afterUpdateMouseClicked
        jTextArea3.setText("CREATE DEFINER = CURRENT_USER TRIGGER `" + nameSchema + "`.`" + nameTable + "_BEFORE_DELETE` BEFORE DELETE ON `" + nameTable + "` FOR EACH ROW\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_btn_afterUpdateMouseClicked

    private void btn_beforeDeleteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_beforeDeleteMouseClicked
        jTextArea3.setText("CREATE DEFINER = CURRENT_USER TRIGGER `" + nameSchema + "`.`" + nameTable + "_BEFORE_DELETE` BEFORE DELETE ON `" + nameTable + "` FOR EACH ROW\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_btn_beforeDeleteMouseClicked

    private void btn_afterDeleteMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_afterDeleteMouseClicked
        jTextArea3.setText("CREATE DEFINER = CURRENT_USER TRIGGER `" + nameSchema + "`.`" + nameTable + "_AFTER_DELETE` BEFORE DELETE ON `" + nameTable + "` FOR EACH ROW\n"
                + "BEGIN\n"
                + "\n"
                + "END");
    }//GEN-LAST:event_btn_afterDeleteMouseClicked

    private void jButton22MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton22MouseClicked
        jTextArea3.setText("");
    }//GEN-LAST:event_jButton22MouseClicked

    private void jButton23MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton23MouseClicked
        url = "jdbc:mysql://localhost:3306/" + nameSchema;
        user = "root";
        password = "contra";

        System.out.println(nameSchema);

        try {
            Connection connection = DriverManager.getConnection(url, user, password);

            String createTQuery = jTextArea3.getText();

            Statement statement = connection.createStatement();
            statement.executeUpdate(createTQuery);

            JOptionPane.showMessageDialog(null, "Trigger created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            SchemasTree();
            Triggers.setVisible(false);

            statement.close();
            connection.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "An error occurred: \n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton23MouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Gestor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Gestor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Gestor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Gestor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Gestor().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AlterFunction;
    private javax.swing.JMenuItem AlterProcedure;
    private javax.swing.JMenuItem AlterTable;
    private javax.swing.JMenuItem AlterTrigger;
    private javax.swing.JMenuItem AlterView;
    private javax.swing.JMenuItem Btn_CreateConnection;
    private javax.swing.JMenuItem Btn_DeleteC;
    private javax.swing.JMenuItem Btn_DropSchema;
    private javax.swing.JMenuItem Btn_EditC;
    private javax.swing.JMenuItem Btn_NewSchema;
    private javax.swing.JMenuItem Btn_OpenC;
    private javax.swing.JMenuItem CreateFunction;
    private javax.swing.JMenuItem CreateProcedure;
    private javax.swing.JMenuItem CreateTable;
    private javax.swing.JMenuItem CreateTrigger;
    private javax.swing.JMenuItem CreateView;
    private javax.swing.JTextArea DDL_CreateTable;
    private javax.swing.JMenuItem DropFunction;
    private javax.swing.JMenuItem DropProcedure;
    private javax.swing.JMenuItem DropTable;
    private javax.swing.JMenuItem DropTrigger;
    private javax.swing.JMenuItem DropView;
    private javax.swing.JMenuItem Drop_User;
    private javax.swing.JMenuItem Edit_User;
    private javax.swing.JDialog Function;
    private javax.swing.JDialog Gestor_jDialog;
    private javax.swing.JPasswordField Login_Pass;
    private javax.swing.JTextField Login_User;
    private javax.swing.JDialog NameSchema;
    private javax.swing.JDialog NewTable;
    private javax.swing.JDialog Procedure;
    private javax.swing.JTextField SchemaName;
    private javax.swing.JDialog Triggers;
    private javax.swing.JDialog Users;
    private javax.swing.JPopupMenu Users_crud;
    private javax.swing.JDialog ValidarUser;
    private javax.swing.JDialog View;
    private javax.swing.JButton btn_afterDelete;
    private javax.swing.JButton btn_afterInsert;
    private javax.swing.JButton btn_afterUpdate;
    private javax.swing.JButton btn_beforeDelete;
    private javax.swing.JButton btn_beforeInsert;
    private javax.swing.JButton btn_beforeUpdate;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton22;
    private javax.swing.JButton jButton23;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JButton jButton_Cancel;
    private javax.swing.JButton jButton_Ok;
    private javax.swing.JCheckBox jCheckBox_BaAd;
    private javax.swing.JCheckBox jCheckBox_DBA;
    private javax.swing.JCheckBox jCheckBox_DBD;
    private javax.swing.JCheckBox jCheckBox_DBM;
    private javax.swing.JCheckBox jCheckBox_MaAd;
    private javax.swing.JCheckBox jCheckBox_MoAd;
    private javax.swing.JCheckBox jCheckBox_PrAd;
    private javax.swing.JCheckBox jCheckBox_ReAd;
    private javax.swing.JCheckBox jCheckBox_SeAd;
    private javax.swing.JCheckBox jCheckBox_UsAd;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel_nameSchema;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JPopupMenu jPopupMenu_CCRUD;
    private javax.swing.JPopupMenu jPopupMenu_CreateConnection;
    private javax.swing.JPopupMenu jPopupMenu_DropSchema;
    private javax.swing.JPopupMenu jPopupMenu_Function;
    private javax.swing.JPopupMenu jPopupMenu_NewSchema;
    private javax.swing.JPopupMenu jPopupMenu_Procedure;
    private javax.swing.JPopupMenu jPopupMenu_Table;
    private javax.swing.JPopupMenu jPopupMenu_Triggers;
    private javax.swing.JPopupMenu jPopupMenu_View;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTabbedPane jTabbedPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTable jTableU;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextArea jTextArea_DDL;
    private javax.swing.JTextArea jTextArea_SQLQuery;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jText_CHName;
    private javax.swing.JTextField jText_CName;
    private javax.swing.JPasswordField jText_Pass;
    private javax.swing.JTextField jText_Port;
    private javax.swing.JTextField jText_User;
    private javax.swing.JTree jTree;
    private javax.swing.JTree jTree_C;
    private javax.swing.JTextField newView;
    // End of variables declaration//GEN-END:variables
    DefaultMutableTreeNode raiz;
    DefaultTreeModel treeModel;
    Connection connection;
    Statement statement;
    ResultSet resultSet;
    String url;
    String user;
    String password;
    String user_jText;
    String password_jText;
    String connectionName;
    String userIn;
    String contraIn;
    String operation;
    int id_connection;
    String nameSchema;
    String nameTable;
    String usuarioIngresado;
    String contraIngresado;
    String nameView;
    String nameProcedure;
    String nameFunction;
    String nameTrigger;
}