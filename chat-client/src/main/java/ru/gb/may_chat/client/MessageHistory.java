package ru.gb.may_chat.client;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageHistory {


    public void saveMessage(String inputMessage) {
        try (BufferedWriter writer = new BufferedWriter(new
                FileWriter("chat-client/src/main/resources/history"))) {

            writer.write(inputMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getHistory() {

        return new ArrayList<>();
    }
    //всё, что handler отправляет должно сохраняться в файл resources/history

    //после успешного логина в окно чата нужно вывести последние сообщения
}
