package ru.gb.may_chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gb.may_chat.server.service.impl.InDBUserServiceImpl;

public class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);
    public static void main(String[] args) {
        LOGGER.info("STARTING SERVER");
        new Server(new InDBUserServiceImpl()).start();
    }

}
