package io.github.jochyoua.offlinecommands.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jochyoua.offlinecommands.OfflineCommands;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class StorageManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String url;
    private Connection connection;

    public StorageManager(OfflineCommands offlineCommands) {
        this.url = "jdbc:sqlite:" + new File(offlineCommands.getDataFolder(), "user_database.db").getAbsolutePath();
    }

    /**
     * Initializes the database by creating the UserStorage table if it does not exist.
     *
     * @throws SQLException if a database access error occurs
     */
    public void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            String createUserStorageTable = "CREATE TABLE IF NOT EXISTS UserStorage (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT, " +
                    "commands TEXT" +
                    ")";
            stmt.execute(createUserStorageTable);
        }
    }

    /**
     * Retrieves a connection to the database. If the current connection is
     * closed or null, a new connection is established.
     *
     * @return a connection to the SQLite database
     * @throws SQLException if a database access error occurs
     */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves a UserStorage object for the specified UUID.
     *
     * @param uuid the UUID of the user
     * @return the UserStorage object, or null if the user is not found
     * @throws SQLException            if a database access error occurs
     * @throws JsonProcessingException if an error occurs while processing JSON
     */
    public UserStorage getUser(UUID uuid) throws SQLException, JsonProcessingException {
        String sql = "SELECT * FROM UserStorage WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                List<CommandStorage> commands = objectMapper.readValue(rs.getString("commands"), new TypeReference<List<CommandStorage>>() {
                });
                return UserStorage.builder()
                        .uuid(UUID.fromString(rs.getString("uuid")))
                        .username(rs.getString("username"))
                        .commands(commands)
                        .build();
            }
        }
        return null;
    }

    /**
     * Adds or updates a user in the database.
     *
     * @param userStorage the UserStorage object to add or update
     * @throws SQLException            if a database access error occurs
     * @throws JsonProcessingException if an error occurs while processing JSON
     */
    public void addOrUpdateUser(UserStorage userStorage) throws SQLException, JsonProcessingException {
        String sql = "INSERT OR REPLACE INTO UserStorage(uuid, username, commands) VALUES(?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userStorage.getUuid().toString());
            pstmt.setString(2, userStorage.getUsername());
            pstmt.setString(3, objectMapper.writeValueAsString(userStorage.getCommands()));
            pstmt.executeUpdate();
        }
    }

    public CommandStorage getCommandFromDatabase(String commandIdentifier) throws SQLException, JsonProcessingException {
        String sql = "SELECT * FROM UserStorage";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                List<CommandStorage> commands = objectMapper.readValue(rs.getString("commands"), new TypeReference<List<CommandStorage>>() {
                });
                for (CommandStorage command : commands) {
                    if (command.getIdentifier().equalsIgnoreCase(commandIdentifier)) {
                        return command;
                    }
                }
            }
        }
        return null;
    }


    /**
     * Removes a user from the database.
     *
     * @param uuid the UUID of the user to remove
     * @throws SQLException if a database access error occurs
     */
    public void removeUser(UUID uuid) throws SQLException {
        String sql = "DELETE FROM UserStorage WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        }
    }

    /**
     * Retrieves a list of all UserStorage objects from the database.
     *
     * @return a list of UserStorage objects
     * @throws SQLException            if a database access error occurs
     * @throws JsonProcessingException if an error occurs while processing JSON
     */
    public List<UserStorage> getUserStorageList() throws SQLException, JsonProcessingException {
        List<UserStorage> userStorageList = new ArrayList<>();
        String sql = "SELECT * FROM UserStorage";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                List<CommandStorage> commands = objectMapper.readValue(rs.getString("commands"), new TypeReference<List<CommandStorage>>() {
                });
                UserStorage userStorage = UserStorage.builder()
                        .uuid(UUID.fromString(rs.getString("uuid")))
                        .username(rs.getString("username"))
                        .commands(commands)
                        .build();
                userStorageList.add(userStorage);
            }
        }
        return userStorageList;
    }
}
