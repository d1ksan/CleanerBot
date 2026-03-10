package com.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
  public static void main(String[] args) {
    try {
      // Создаём объект TelegramBotsApi для регистрации бота
      TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
      // Регистрируем нашего бота
      botsApi.registerBot(new SimpleRegisterBot());
      System.out.println("✅ Бот успешно запущен!");
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }
}