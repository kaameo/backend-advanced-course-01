package com.beac.todo.exception;

public class TodoNotFoundException extends RuntimeException {

    public TodoNotFoundException(Long id) {
        super("해당 할 일을 찾을 수 없습니다. id = " + id);
    }
}
