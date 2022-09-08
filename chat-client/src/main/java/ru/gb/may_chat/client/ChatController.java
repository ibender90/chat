package ru.gb.may_chat.client;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gb.may_chat.client.net.MessageProcessor;
import ru.gb.may_chat.client.net.NetworkService;
import ru.gb.may_chat.enums.Command;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.*;

public class ChatController implements Initializable, MessageProcessor {

    private static final Logger LOGGER = LogManager.getLogger(ChatController.class);

    @FXML
    private VBox changeNickPanel;

    @FXML
    private TextField newNickField;

    @FXML
    private VBox changePasswordPanel;

    @FXML
    private PasswordField oldPassField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private VBox loginPanel;

    @FXML
    private TextField loginField;

    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox mainPanel;

    @FXML
    private TextArea chatArea;

    @FXML
    private ListView<String> contacts;

    @FXML
    private TextField inputField;

    @FXML
    private Button btnSend;

    private NetworkService networkService;

    private MessageHistory messageHistory;

    private String user;

    public void mockAction(ActionEvent actionEvent) {
        System.out.println("mock");
    }

    public void closeApplication(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void sendMessage(ActionEvent actionEvent) {
        try {
            String text = inputField.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            String recipient = contacts.getSelectionModel().getSelectedItem();
            if (recipient.equals("ALL")) {
                networkService.sendMessage(BROADCAST_MESSAGE.getCommand() + REGEX + text);
            } else {
                networkService.sendMessage(PRIVATE_MESSAGE.getCommand() + REGEX
                        + recipient + REGEX + text);
            }
            inputField.clear();
        } catch (IOException e) {
            showError("Network error");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(
                Alert.AlertType.ERROR,
                message,
                ButtonType.CLOSE
        );
        alert.showAndWait();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        networkService = new NetworkService(this);
        messageHistory = new MessageHistory();
    }

    @Override
    public void processMessage(String message) {
        LOGGER.info("Chat controller is processing incoming message");
        Platform.runLater(() -> parseMessage(message));
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

        switch (command) {
            case AUTH_OK -> authOk(split);
            case ERROR_MESSAGE -> showError(split[1]);
            case LIST_USERS -> parseUsers(split);
            default -> {
                messageHistory.saveMessage(split[1]);
                chatArea.appendText(split[1] + System.lineSeparator());
            }
        }
    }

    private void parseUsers(String[] split) {
        List<String> contact = new ArrayList<>(Arrays.asList(split));
        contact.set(0, "ALL");
        contacts.setItems(FXCollections.observableList(contact));
    }

    private void authOk(String[] split) {
        LOGGER.info("Client authorized");
        user = split[1];
        loginPanel.setVisible(false);
        mainPanel.setVisible(true);
        showMessageHistory();
    }

    private void showMessageHistory(){
        List<String> history = messageHistory.getHistory();

        for (String message :
             history) {
            chatArea.appendText(message + System.lineSeparator());
        }
    }

    public void sendChangeNick(ActionEvent actionEvent) {
//TODO
    }

    public void returnToChat(ActionEvent actionEvent) {
//TODO
    }

    public void sendChangePass(ActionEvent actionEvent) {
//TODO
    }

    public void sendAuth(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login.isBlank() || password.isBlank()) {
            return;
        }
        String msg = AUTH_MESSAGE.getCommand() + REGEX + login + REGEX + password;
        try {
            if (!networkService.isConnected()) {
                networkService.connect();
            }

            networkService.sendMessage(msg);
        } catch (IOException e) {
            showError("Network error");
        }
    }

}
