package com.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SuppressWarnings("deprecation")
public class SimpleRegisterBot extends TelegramLongPollingBot {

    // Экземпляр менеджера базы данных
    private final DatabaseManager dbManager = new DatabaseManager();

    @Override
    public String getBotUsername() {
        // Замени на имя твоего бота (которое дал BotFather)
        return "RUDN_cleaning_schedule";
    }

    @Override
    public String getBotToken() {
        // Замени на токен, полученный от BotFather
        return "8616700916:AAH2dxjtFdUr2vETgfI1fDc1DHieRp8jXLo";
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, есть ли сообщение с текстом
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();   // ID чата для ответа
            long userId = update.getMessage().getFrom().getId();  // ID пользователя
            String userName = update.getMessage().getFrom().getUserName(); // username

            // Проверяем регистрацию через базу данных
            if (!dbManager.isUserRegistered(userId)) {
                // Если не зарегистрирован — добавляем
                dbManager.registerUser(userId, userName);
                sendText(chatId, "Вы успешно зарегистрированы, @" + userName + "!");
            } else {
                // Если уже есть — приветствуем
                sendText(chatId, "Привет, @" + userName + "! Ты уже в системе.");
            }
        }
    }

    // Вспомогательный метод для отправки текстового сообщения
    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);  // отправляем через API бота
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}