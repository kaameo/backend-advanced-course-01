package com.beac.todo.service;

import com.beac.todo.dto.TodoCreateDto;
import com.beac.todo.dto.TodoStatusUpdateDto;
import com.beac.todo.dto.TodoUpdateDto;
import com.beac.todo.entity.Todo;
import com.beac.todo.entity.TodoStatus;
import com.beac.todo.exception.TodoNotFoundException;
import com.beac.todo.repository.TodoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TodoService {
    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional(readOnly = true)
    public Todo findById(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Todo> findAll(TodoStatus status, Pageable pageable) {
        return status == null
                ? todoRepository.findAll(pageable)
                : todoRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public long count() {
        return todoRepository.count();
    }

    @Transactional
    public Todo create(TodoCreateDto dto) {
        return todoRepository.save(Todo.create(dto.title(), dto.content()));
    }

    @Transactional
    public Todo save(Todo todo) {
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo update(Long id, TodoUpdateDto dto) {
        Todo todo = findById(id);

        todo.update(dto.title(), dto.content());
        return todo;
    }

    @Transactional
    public Todo changeStatus(Long id, TodoStatusUpdateDto dto) {
        Todo todo = findById(id);

        todo.changeStatus(dto.status());
        return todo;
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.delete(findById(id));
    }
}
