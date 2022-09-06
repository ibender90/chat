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
    private Runnable handlerThread;
    private Server server;
    private String user;
    private  boolean authorized = false;
    private boolean threadIsInterrupted = false;
    private int timeToAuthorize = 60;

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

    public Runnable handle() {
        //callTimerToAuthorize();
        handlerThread = new Runnable() {
            @Override
            public void run() {
                authorize();
                System.out.println("Auth process is finished");
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        String message = in.readUTF();
                        parseMessage(message);
                    } catch (IOException e) {
                        System.out.println("Connection broken with client: " + user);
                        server.removeHandler(Handler.this);
                    }
                }
            }
        };
//            authorize();
//            System.out.println("Auth process is finished");
//            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
//                try {
//                    String message = in.readUTF();
//                    parseMessage(message);
//                } catch (IOException e) {
//                    System.out.println("Connection broken with client: " + user);
//                    server.removeHandler(this);
//                }
//            }
//        });
        //handlerThread.start();
        return handlerThread;
    }

    private void callTimerToAuthorize() {
        Thread timer = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Timer started");
                for (int i =0; i < timeToAuthorize; i ++) {
                    if (authorized) {
                        break;
                    }
                    System.out.println("tick-tack");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println("Timeout");
                send(ERROR_MESSAGE.getCommand() + REGEX + "Timeout");
                try {
                    killHandler();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        timer.setDaemon(true);
        timer.start();
    }

    private void killHandler() throws IOException {
        //handlerThread.interrupt();
        threadIsInterrupted = true;
    }

    private void authorize() {
        System.out.println("Authorizing");

        try {
            while (!socket.isClosed() && !threadIsInterrupted) {

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
                        this.authorized = true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public void send(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread getHandlerThread() {
        return (Thread) handlerThread;
    }

    public String getUser() {
        return user;
    }
}
