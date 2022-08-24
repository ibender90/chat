package ru.gb.may_chat.server;

import ru.gb.may_chat.enums.Command;
import ru.gb.may_chat.server.error.WrongCredentialsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.AUTH_MESSAGE;
import static ru.gb.may_chat.enums.Command.AUTH_OK;
import static ru.gb.may_chat.enums.Command.ERROR_MESSAGE;

public class Handler {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread handlerThread;
    private Server server;
    private String user;
    private  boolean authorized = false;
    private int timer = 12;

    public Handler(Socket socket, Server server) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Handler created");
        } catch (IOException e) {
            System.err.println("Connection problems with user: " + user);
        }
    }

    public void handle() {
        callTimerToAuthorize();
        handlerThread = new Thread(() -> {
            authorize();
            System.out.println("Auth done");
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {
                    String message = in.readUTF();
                    parseMessage(message);
                } catch (IOException e) {
                    System.out.println("Connection broken with client: " + user);
                    server.removeHandler(this);
                }
            }
        });
        handlerThread.start();
    }

    private void callTimerToAuthorize() {

        Thread timeDaemon = new Thread(()->{
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    Thread.sleep(1000);
                    if (this.authorized) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    this.timer--;
                    if (this.timer == 0) {
                        System.out.println("timeout, handler is dead");
                        this.handlerThread.interrupt(); //? не сработало
                        System.out.println(this.handlerThread.isInterrupted());
                        break;
                    }
                    System.out.println("tick-tack");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        timeDaemon.setDaemon(true);
        timeDaemon.start();
    }



    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

        switch (command) {
            case BROADCAST_MESSAGE -> server.broadcast(user, split[1]);
            case PRIVATE_MESSAGE -> server.sendPrivateMessage(user, message);
            default -> System.out.println("Unknown message " + message);
        }
    }


    private boolean authorize() {
        System.out.println("Authorizing");

        try {
            while (!socket.isClosed()) {
                String msg = in.readUTF();
                if (msg.startsWith(AUTH_MESSAGE.getCommand())) {
                    String[] parsed = msg.split(REGEX);
                    String response = "";
                    String nickname = null;

                    try {
                        nickname = server.getUserService().authenticate(parsed[1], parsed[2]);
                    } catch (WrongCredentialsException e) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + e.getMessage();
                        System.out.println("Wrong credentials: " + parsed[1]);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    if (server.isUserAlreadyOnline(nickname)) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + "This client already connected";
                        System.out.println("Already connected");
                    }
                    
                    if (!response.equals("")) {
                        send(response);
                    } else {
                        System.out.println("Auth ok");
                        this.user = nickname;
                        send(AUTH_OK.getCommand() + REGEX + nickname);
                        server.addHandler(this);
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void send(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread getHandlerThread() {
        return handlerThread;
    }

    public String getUser() {
        return user;
    }
}
