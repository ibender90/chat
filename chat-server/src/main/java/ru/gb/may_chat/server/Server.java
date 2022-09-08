package ru.gb.may_chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gb.may_chat.server.error.UserNotFoundException;
import ru.gb.may_chat.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.*;

public class Server {
    private static final int PORT = 8189;
    private List<Handler> handlers;
    protected static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private UserService userService;
    private static final Logger LOGGER = LogManager.getLogger(App.class);


    public Server(UserService userService) {
        this.userService = userService;
        this.handlers = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("SERVER STARTED");
            userService.start();
            while (true) {
                LOGGER.info("Server is waiting for connection");
                Socket socket = serverSocket.accept();
                LOGGER.info("Client connected to server");
                Handler handler = new Handler(socket, this);
                handler.handle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void broadcast(String from, String message) {
        String msg = BROADCAST_MESSAGE.getCommand() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }

    public UserService getUserService() {
        return userService;
    }

    public synchronized boolean isUserAlreadyOnline(String nick) {
        for (Handler handler : handlers) {
            if (handler.getUser().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addHandler(Handler handler) {
        this.handlers.add(handler);
        sendContacts();
    }

    public synchronized void removeHandler(Handler handler) {
        this.handlers.remove(handler);
        sendContacts();
    }

    private void shutdown() {
        userService.stop();
        executorService.shutdown();
    }

    private void sendContacts() {
        String contacts = handlers.stream()
                .map(Handler::getUser)
                .collect(Collectors.joining(REGEX));
        String msg = LIST_USERS.getCommand() + REGEX + contacts;

        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }

    public void sendPrivateMessage(String from, String input) {
        //private message начинается с адресата
        String[] splitInput = input.split(REGEX);
        System.out.println(Arrays.toString(splitInput));
        String to = splitInput[1];
        String message = splitInput[2];
        System.out.println(to);
        String messageShownToTarget = PRIVATE_MESSAGE.getCommand() + REGEX + from + " says: " + message;
        String messageShownToSender = PRIVATE_MESSAGE.getCommand() + REGEX +  "You : " + message;
        System.out.println(message);

        findRecipient(to).send(messageShownToTarget);
        findRecipient(from).send(messageShownToSender);

    }

    private Handler findRecipient(String nick)  {
        for (Handler handler : handlers) {
            System.out.println(handler.getUser());
            if(handler.getUser().equals(nick)){
                return handler;
            }
        } throw new UserNotFoundException("No such user on a server");
    }


}



