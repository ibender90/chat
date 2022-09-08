package ru.gb.may_chat.server.service.impl;

import ru.gb.may_chat.server.error.WrongCredentialsException;
import ru.gb.may_chat.server.model.User;
import ru.gb.may_chat.server.service.UserService;

import java.sql.*;

public class InDBUserServiceImpl implements UserService {

    private static Connection connection;
    private static Statement statement;

    @Override
    public void start() {
        try {
            connect();
            createTableIfAbsent();

        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private static void createTableIfAbsent() throws SQLException {
        ResultSet exists = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='users';");
        if (exists.getInt(1) == 0) {
            statement.execute("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "login TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL," +
                    "nick TEXT UNIQUE NOT NULL)");
            fillTable();
        }
    }

    private static void fillTable() throws SQLException {
        for (int i = 1; i<6; i++){
            String login = String.format("log%d", i);
            String password = String.format("pass%d", i);
            String nick = String.format("nick%d", i);

            statement.execute(String.format("INSERT INTO users (login, password, nick) VALUES ('%s', '%s', '%s')",
                    login, password, nick));
        }
    }

    @Override
    public void stop() {
        try {
            disconnect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String authenticate(String login, String password) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT login, password, nick FROM users WHERE login = ? AND password = ?");
        preparedStatement.setString(1, login);
        preparedStatement.setString(2, password);
        ResultSet resultSet = preparedStatement.executeQuery();

        if(resultSet.isClosed()){
            throw new WrongCredentialsException("Wrong login or password");
        }

        return resultSet.getString("nick");
    }

    @Override
    public String changeNick(String login, String newNick) {
        return null;
    }

    @Override
    public User createUser(String login, String password, String nick) {
        return null;
    }

    @Override
    public void deleteUser(String login, String password) {

    }

    @Override
    public void changePassword(String login, String oldPassword, String newPassword) {

    }

    private static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chat_users.db");
        statement = connection.createStatement();
    }

    private static void disconnect() throws SQLException {
        connection.close();

    }
}
