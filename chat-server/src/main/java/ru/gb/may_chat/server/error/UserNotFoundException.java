package ru.gb.may_chat.server.error;

import java.util.InputMismatchException;

public class UserNotFoundException extends IllegalArgumentException {
    public UserNotFoundException(String s) {
        super(s);
    }
}
