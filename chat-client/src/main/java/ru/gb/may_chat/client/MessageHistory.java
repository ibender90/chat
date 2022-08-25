package ru.gb.may_chat.client;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MessageHistory {
    private static final String filePath = "chat-client/src/main/resources/history";

    public void saveMessage(String inputMessage) {
        try (BufferedWriter writer = new BufferedWriter(new
                FileWriter(filePath, true))) {

            writer.write(inputMessage + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<String> getHistory() {
        List<String> messages = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            if (countLines() > 100) {
                int skip = countLines() - 100;
                for (int i = 0; i < skip; i++) {
                    reader.readLine(); //читаю строки но ничего не делаю с информацией
                }
            }
            while ((line = reader.readLine()) != null) {
                messages.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messages;
    }

    private int countLines() throws IOException {
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new FileReader(filePath));
            while ((reader.readLine()) != null);
            return reader.getLineNumber();
        } catch (Exception ex) {
            return -1;
        } finally {
            if(reader != null)
                reader.close();
        }

    }

}
