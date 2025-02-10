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
            // Modern MySQL driver class:
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ConfigHandler.useSSL()
                    + "&allowPublicKeyRetrieval=" + ConfigHandler.allowPublicKeyRetrieval();
            connection = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            logError("Failed to connect to MySQL: " + e.getMessage(), e);
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Creates or updates the player_transfer table (if it does not exist).
     * Now includes columns for yaw and pitch (both FLOAT).
     */
    public void setupTable() {
        if (connection == null) {
            logError("MySQL connection is null, cannot setup table.", null);
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            // Create the table if it doesn't exist.
            // Includes yaw and pitch columns for storing direction.
            String sql = "CREATE TABLE IF NOT EXISTS player_transfer (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "destServer VARCHAR(50), " +
                    "x INT, " +
                    "y INT, " +
                    "z INT, " +
                    "direction VARCHAR(10), " +  // We keep this for reference (unused).
                    "yaw FLOAT, " +
                    "pitch FLOAT" +
                    ")";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logError("Failed to setup MySQL table: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the player's transfer data, including offset position and yaw/pitch.
     * - direction is kept but not used in the final teleport anymore.
     */
    public void savePlayerTransfer(String uuid, String destServer, int x, int y, int z,
                                   String direction, float yaw, float pitch) {
        if (connection == null) {
            logError("MySQL connection is null, cannot save player transfer.", null);
            return;
        }
        String query = "REPLACE INTO player_transfer (uuid, destServer, x, y, z, direction, yaw, pitch) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, destServer);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setString(6, direction);
            ps.setFloat(7, yaw);
            ps.setFloat(8, pitch);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Failed to save player transfer: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the stored location + yaw/pitch for a player's transfer.
     */
    public TransferData getTransferData(String uuid) {
        if (connection == null) {
            logError("MySQL connection is null, cannot retrieve transfer data.", null);
            return null;
        }
        String query = "SELECT destServer, x, y, z, direction, yaw, pitch FROM player_transfer WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TransferData(
                            rs.getString("destServer"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            rs.getString("direction"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            logError("Failed to get transfer data: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Deletes the stored transfer data for a player once used.
     */
    public void deleteTransferData(String uuid) {
        if (connection == null) {
            logError("MySQL connection is null, cannot delete transfer data.", null);
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM player_transfer WHERE uuid = ?")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Failed to delete transfer data: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logError("Failed to close MySQL connection: " + e.getMessage(), e);
        }
    }

    private void logError(String message, Exception e) {
        System.err.println(message);
        if (e != null) {
            e.printStackTrace();
        }
    }

    /**
     * Holds all data needed to teleport a player on the destination server.
     */
    public static class TransferData {
        public String destServer;
        public int x, y, z;
        public String direction; // Not strictly used for the final teleport, but kept for reference.
        public float yaw, pitch;

        public TransferData(String destServer, int x, int y, int z, String direction, float yaw, float pitch) {
            this.destServer = destServer;
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
