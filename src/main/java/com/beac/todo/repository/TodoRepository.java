package com.beac.todo.repository;

import com.beac.todo.entity.Todo;
import com.beac.todo.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    Page<Todo> findByStatus(TodoStatus status, Pageable pageable);
}
