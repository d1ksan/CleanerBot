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
        // SQL для создания таблицы, если её нет
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "user_id INTEGER UNIQUE NOT NULL," +
                     "username TEXT," +
                     "registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ");";
        // try-with-resources автоматически закроет Connection и Statement
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Таблица users проверена/создана.");
        } catch (SQLException e) {
            System.err.println("Ошибка создания таблицы: " + e.getMessage());
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
}