package studio.magemonkey.database;

import studio.magemonkey.handlers.ConfigHandler;
import java.sql.*;

public class MySQLManager {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public MySQLManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=" + ConfigHandler.useSSL() +
                    "&allowPublicKeyRetrieval=" + ConfigHandler.allowPublicKeyRetrieval();
            connection = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            logError("Failed to connect to MySQL: " + e.getMessage(), e);
        }
    }

    // New method to check if the connection is established.
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void setupTable() {
        if (connection == null) {
            logError("MySQL connection is null, cannot setup table.", null);
            return;
        }
        try {
            Statement stmt = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS player_transfer (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "destServer VARCHAR(50), " +
                    "x INT, " +
                    "y INT, " +
                    "z INT, " +
                    "direction VARCHAR(10)" +
                    ")";
            stmt.executeUpdate(sql);
            stmt.close();
        } catch (SQLException e) {
            logError("Failed to setup MySQL table: " + e.getMessage(), e);
        }
    }

    public void savePlayerTransfer(String uuid, String destServer, int x, int y, int z, String direction) {
        if (connection == null) {
            logError("MySQL connection is null, cannot save player transfer.", null);
            return;
        }
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "REPLACE INTO player_transfer (uuid, destServer, x, y, z, direction) VALUES (?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, uuid);
            ps.setString(2, destServer);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setString(6, direction);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logError("Failed to save player transfer: " + e.getMessage(), e);
        }
    }

    public TransferData getTransferData(String uuid) {
        if (connection == null) {
            logError("MySQL connection is null, cannot retrieve transfer data.", null);
            return null;
        }
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT destServer, x, y, z, direction FROM player_transfer WHERE uuid = ?"
            );
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TransferData data = new TransferData(
                        rs.getString("destServer"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getString("direction")
                );
                rs.close();
                ps.close();
                return data;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            logError("Failed to get transfer data: " + e.getMessage(), e);
        }
        return null;
    }

    public void deleteTransferData(String uuid) {
        if (connection == null) {
            logError("MySQL connection is null, cannot delete transfer data.", null);
            return;
        }
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM player_transfer WHERE uuid = ?");
            ps.setString(1, uuid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logError("Failed to delete transfer data: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            logError("Failed to close MySQL connection: " + e.getMessage(), e);
        }
    }

    private void logError(String message, Exception e) {
        // Use System.err for logging errors here. In a real plugin, you might use the plugin's logger.
        System.err.println(message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    public static class TransferData {
        public String destServer;
        public int x, y, z;
        public String direction;

        public TransferData(String destServer, int x, int y, int z, String direction) {
            this.destServer = destServer;
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
        }
    }
}
