package com.example;

import java.sql.*;

public class DatabaseManager {
  // Путь к файлу базы данных (будет создан в корне проекта)
  private static final String DB_URL = "jdbc:sqlite:bot_users.db";

  public DatabaseManager() {
    // При создании объекта проверяем/создаём таблицу
    createTableIfNotExists();
  }

  private void createTableIfNotExists() {
    String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "user_id INTEGER UNIQUE NOT NULL,"
        + "username TEXT,"
        + "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        + ");";

    String createRoomsTable = "CREATE TABLE IF NOT EXISTS rooms ("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
        + "room_code TEXT UNIQUE NOT NULL,"
        + "created_by INTEGER NOT NULL,"
        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        + ");";

    String createRoomMembersTable = "CREATE TABLE IF NOT EXISTS room_members ("
        + "room_id INTEGER NOT NULL,"
        + "user_id INTEGER NOT NULL,"
        + "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
        + "PRIMARY KEY (room_id, user_id),"
        + "FOREIGN KEY (room_id) REFERENCES rooms(id),"
        + "FOREIGN KEY (user_id) REFERENCES users(user_id)"
        + ");";

    try (Connection conn = DriverManager.getConnection(DB_URL);
        Statement stmt = conn.createStatement()) {
      stmt.execute(createUsersTable);
      stmt.execute(createRoomsTable);
      stmt.execute(createRoomMembersTable);
      System.out.println("✅ Таблицы проверены/созданы.");
    } catch (SQLException e) {
      System.err.println("❌ Ошибка создания таблиц:");
      e.printStackTrace();
    }
  }

  // Проверка, есть ли пользователь в базе
  public boolean isUserRegistered(long userId) {
    String sql = "SELECT 1 FROM users WHERE user_id = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setLong(1, userId);
      ResultSet rs = pstmt.executeQuery();
      return rs.next(); // если есть хоть одна запись, вернёт true
    } catch (SQLException e) {
      System.err.println("Ошибка проверки пользователя: " + e.getMessage());
      return false;
    }
  }

  // Добавление нового пользователя
  public void registerUser(long userId, String username) {
    String sql = "INSERT INTO users (user_id, username) VALUES (?, ?)";
    try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setLong(1, userId);
      pstmt.setString(2, username);
      pstmt.executeUpdate();
      System.out.println("Пользователь " + userId + " зарегистрирован.");
    } catch (SQLException e) {
      System.err.println("Ошибка регистрации пользователя: " + e.getMessage());
    }
  }

  public boolean createRoom(String roomCode, long creatorUserId) {
    String trimmedCode = roomCode.trim();
    System.out.println("🔧 createRoom: попытка создать комнату с кодом '" + trimmedCode + "', пользователь " + creatorUserId);

    String checkSql = "SELECT id FROM rooms WHERE room_code = ?";
    String insertSql = "INSERT INTO rooms (room_code, created_by) VALUES (?, ?)";
    String getLastIdSql = "SELECT last_insert_rowid() AS id";
    String addMemberSql = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";

    try (Connection conn = DriverManager.getConnection(DB_URL)) {
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, trimmedCode);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                int existingId = rs.getInt("id");
                System.out.println("⚠️ Комната с кодом '" + trimmedCode + "' уже существует (id=" + existingId + ").");
                return false;
            }
        }

        // Вставляем комнату
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, trimmedCode);
            insertStmt.setLong(2, creatorUserId);
            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Не удалось вставить комнату.");
            }
        }

        // Получаем ID последней вставленной комнаты
        int roomId;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(getLastIdSql)) {
            if (rs.next()) {
                roomId = rs.getInt("id");
                System.out.println("✅ Комната создана: id=" + roomId + ", код='" + trimmedCode + "'");
            } else {
                throw new SQLException("Не удалось получить ID созданной комнаты.");
            }
        }

        // Добавляем создателя как участника
        try (PreparedStatement addStmt = conn.prepareStatement(addMemberSql)) {
            addStmt.setInt(1, roomId);
            addStmt.setLong(2, creatorUserId);
            addStmt.executeUpdate();
            System.out.println("✅ Пользователь " + creatorUserId + " добавлен в участники комнаты " + roomId);
        }
        return true;

    } catch (SQLException e) {
        System.err.println("❌ Ошибка в createRoom для кода '" + trimmedCode + "':");
        e.printStackTrace();
        return false;
    }
}



  public boolean roomExists(String roomCode) {
    String trimmedCode = roomCode.trim();
    String sql = "SELECT id FROM rooms WHERE room_code = ?";
    try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, trimmedCode);
      ResultSet rs = stmt.executeQuery();
      return rs.next();
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean addUserToRoom(String roomCode, long userId) {
    String getRoomIdSql = "SELECT id FROM rooms WHERE room_code = ?";
    String checkMemberSql = "SELECT 1 FROM room_members WHERE room_id = ? AND user_id = ?";
    String insertMemberSql = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
      // Получаем ID комнаты
      int roomId;
      try (PreparedStatement getStmt = conn.prepareStatement(getRoomIdSql)) {
        getStmt.setString(1, roomCode);
        ResultSet rs = getStmt.executeQuery();
        if (!rs.next()) {
          return false; // комната не найдена
        }
        roomId = rs.getInt("id");
      }

      // Проверяем, не участник ли уже
      try (PreparedStatement checkStmt = conn.prepareStatement(checkMemberSql)) {
        checkStmt.setInt(1, roomId);
        checkStmt.setLong(2, userId);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
          return true; // уже участник (считаем успехом)
        }
      }

      // Добавляем участника
      try (PreparedStatement insertStmt = conn.prepareStatement(insertMemberSql)) {
        insertStmt.setInt(1, roomId);
        insertStmt.setLong(2, userId);
        insertStmt.executeUpdate();
      }
      return true;
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }
}