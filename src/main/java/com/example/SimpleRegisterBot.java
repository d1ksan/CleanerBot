package com.example;

import java.util.ArrayList;
import java.util.List;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SuppressWarnings("deprecation")
public class SimpleRegisterBot extends TelegramLongPollingBot {
  private final DatabaseManager dbManager = new DatabaseManager();

  @Override
  public String getBotUsername() {
    return "";
  }

  @Override
  public String getBotToken() {
    return "";
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      long chatId = update.getMessage().getChatId();
      long userId = update.getMessage().getFrom().getId();
      String userName = update.getMessage().getFrom().getUserName();
      String messageText = update.getMessage().getText();

      // Обработка команд
      if ("/start".equals(messageText)) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет! Я бот для уборки. Выбери действие:");
        message.setReplyMarkup(getInlineKeyboard());
        try {
          execute(message);
        } catch (TelegramApiException e) {
          e.printStackTrace();
        }
        return;
      }

      // ----- НОВЫЙ БЛОК: создание комнаты -----
      if (messageText.startsWith("/createroom")) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
          sendText(chatId, "Пожалуйста, укажите код комнаты. Например: /createroom myroom123");
          return;
        }
        String roomCode = parts[1].trim();

        // Убедимся, что пользователь зарегистрирован
        if (!dbManager.isUserRegistered(userId)) {
          dbManager.registerUser(userId, userName);
        }

        boolean created = dbManager.createRoom(roomCode, userId);
        if (created) {
          sendText(
              chatId, "✅ Комната '" + roomCode + "' успешно создана! Вы стали её участником.");
        } else {
          sendText(chatId, "❌ Комната с таким кодом уже существует. Придумайте другой код.");
        }
        return;
      }

      // ----- НОВЫЙ БЛОК: вход в комнату -----
      if (messageText.startsWith("/entertheroom")) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
          sendText(chatId, "Пожалуйста, укажите код комнаты. Например: /entertheroom myroom123");
          return;
        }
        String roomCode = parts[1].trim();

        // Убедимся, что пользователь зарегистрирован
        if (!dbManager.isUserRegistered(userId)) {
          dbManager.registerUser(userId, userName);
        }

        if (!dbManager.roomExists(roomCode)) {
          sendText(chatId, "❌ Комната с кодом '" + roomCode + "' не найдена.");
          return;
        }

        boolean added = dbManager.addUserToRoom(roomCode, userId);
        if (added) {
          sendText(chatId, "✅ Вы присоединились к комнате '" + roomCode + "'.");
        } else {
          sendText(chatId, "❌ Не удалось присоединиться к комнате.");
        }
        return;
      }

      // Старая логика для обычных сообщений (регистрация/приветствие)
      if (!dbManager.isUserRegistered(userId)) {
        dbManager.registerUser(userId, userName);
        sendText(chatId, "Вы успешно зарегистрированы, @" + userName + "!");
      } else {
        sendText(chatId, "Привет, @" + userName + "! Ты уже в системе.");
      }

    } else if (update.hasCallbackQuery()) {
      // Обработка inline-кнопок (оставляем без изменений)
      String callbackData = update.getCallbackQuery().getData();
      long chatId = update.getCallbackQuery().getMessage().getChatId();
      long userId = update.getCallbackQuery().getFrom().getId();
      String userName = update.getCallbackQuery().getFrom().getUserName();

      switch (callbackData) {
        case "profile":
          if (dbManager.isUserRegistered(userId)) {
            sendText(chatId, "👤 Твой профиль:\nID: " + userId + "\nUsername: @" + userName);
          } else {
            sendText(chatId, "Ты ещё не зарегистрирован.");
          }
          break;
        case "help":
          sendText(chatId, "❓ Это демо-бот. /start — меню");
          break;
      }
    }
  }
  // Вспомогательный метод для отправки текстового сообщения
  private void sendText(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    try {
      execute(message); // отправляем через API бота
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private InlineKeyboardMarkup getInlineKeyboard() {
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    // Первая кнопка
    InlineKeyboardButton profileButton = new InlineKeyboardButton();
    profileButton.setText("👤 Мой профиль");
    profileButton.setCallbackData("profile");

    // Вторая кнопка
    InlineKeyboardButton helpButton = new InlineKeyboardButton();
    helpButton.setText("❓ Помощь");
    helpButton.setCallbackData("help");

    // Добавляем кнопки в ряд (по одной в ряду)
    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(profileButton);
    rows.add(row1);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(helpButton);
    rows.add(row2);

    markup.setKeyboard(rows);
    return markup;
  }
}