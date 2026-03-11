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
    return "RUDN_cleaning_schedule";
  }

  @Override
  public String getBotToken() {
    return ""; // ваш токен
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      long chatId = update.getMessage().getChatId();
      long userId = update.getMessage().getFrom().getId();
      String userName = update.getMessage().getFrom().getUserName();
      String messageText = update.getMessage().getText();

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

      // Справка по командам
      if ("/help".equals(messageText)) {
        sendText(chatId, "🤖 *Бот для уборки*\n/start — меню\n/createroom *код* - создать новую комнату\n/entertheroom *код* - присоединиться к комнате");
        return;
      }

      if (messageText.startsWith("/createroom")) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
          sendText(chatId, "Пожалуйста, укажите код комнаты. Например: /createroom myroom123");
          return;
        }
        String roomCode = parts[1].trim();

        if (!dbManager.isUserRegistered(userId)) {
          dbManager.registerUser(userId, userName);
        }

        boolean created = dbManager.createRoom(roomCode, userId);
        if (created) {
          sendText(chatId, "✅ Комната '" + roomCode + "' успешно создана! Вы стали её участником.");
        } else {
          sendText(chatId, "❌ Комната с таким кодом уже существует. Придумайте другой код.");
        }
        return;
      }

      if (messageText.startsWith("/entertheroom")) {
        String[] parts = messageText.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
          sendText(chatId, "Пожалуйста, укажите код комнаты. Например: /entertheroom myroom123");
          return;
        }
        String roomCode = parts[1].trim();

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

      // Обычное сообщение — регистрация/приветствие
      if (!dbManager.isUserRegistered(userId)) {
        dbManager.registerUser(userId, userName);
        sendText(chatId, "Вы успешно зарегистрированы, @" + userName + "!");
      } else {
        sendText(chatId, "Привет, @" + userName + "! Ты уже в системе.");
      }

    } else if (update.hasCallbackQuery()) {
      String callbackData = update.getCallbackQuery().getData();
      long chatId = update.getCallbackQuery().getMessage().getChatId();
      long userId = update.getCallbackQuery().getFrom().getId();
      String userName = update.getCallbackQuery().getFrom().getUserName();

      // Обработка нажатий на кнопки комнат (начинаются с "room_")
      if (callbackData.startsWith("room_")) {
        String roomCode = callbackData.substring(5);
        sendText(chatId, "🚪 Комната *" + roomCode + "*\nФункционал управления задачами пока в разработке.");
      } else {
        // Старые callback'и: profile, help
        switch (callbackData) {
          case "profile":
            if (dbManager.isUserRegistered(userId)) {
              String profileText = "👤 *Твой профиль:*\n" +
                                   "ID: `" + userId + "`\n" +
                                   "Username: @" + userName + "\n\n" +
                                   "*Твои комнаты:*";

              List<String> userRooms = dbManager.getUserRooms(userId);

              InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
              List<List<InlineKeyboardButton>> rows = new ArrayList<>();

              if (userRooms.isEmpty()) {
                profileText += "\nУ тебя пока нет комнат. Создай первую командой /createroom";
              } else {
                for (String roomCode : userRooms) {
                  InlineKeyboardButton roomButton = new InlineKeyboardButton();
                  roomButton.setText("🚪 " + roomCode);
                  roomButton.setCallbackData("room_" + roomCode);
                  List<InlineKeyboardButton> row = new ArrayList<>();
                  row.add(roomButton);
                  rows.add(row);
                }
              }

              markup.setKeyboard(rows);

              SendMessage message = new SendMessage();
              message.setChatId(String.valueOf(chatId));
              message.setText(profileText);
              message.setParseMode("Markdown");
              message.setReplyMarkup(markup);
              try {
                execute(message);
              } catch (TelegramApiException e) {
                e.printStackTrace();
              }
            } else {
              sendText(chatId, "Ты ещё не зарегистрирован. Напиши любое сообщение для регистрации.");
            }
            break;
          case "help":
            sendText(chatId, "🤖 *Бот для уборки*\n/start — меню\n/createroom *код* - создать новую комнату\n/entertheroom *код* - присоединиться к комнате");
            break;
        }
      }
    }
  }

  private void sendText(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.setParseMode("Markdown");
    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  private InlineKeyboardMarkup getInlineKeyboard() {
    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    InlineKeyboardButton profileButton = new InlineKeyboardButton();
    profileButton.setText("👤 Мой профиль");
    profileButton.setCallbackData("profile");

    InlineKeyboardButton helpButton = new InlineKeyboardButton();
    helpButton.setText("❓ Помощь");
    helpButton.setCallbackData("help");

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