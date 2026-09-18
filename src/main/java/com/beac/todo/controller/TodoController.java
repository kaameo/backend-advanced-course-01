package com.beac.todo.controller;

import com.beac.todo.dto.TodoCreateDto;
import com.beac.todo.dto.TodoResponseDto;
import com.beac.todo.dto.TodoStatusUpdateDto;
import com.beac.todo.dto.TodoUpdateDto;
import com.beac.todo.entity.TodoStatus;
import com.beac.todo.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/todos")
@RequiredArgsConstructor
public class TodoController {
    private final TodoService todoService;

    @GetMapping("/{id}")
    public TodoResponseDto findById(@PathVariable Long id) {
        return TodoResponseDto.from(todoService.findById(id));
    }

    @GetMapping("")
    public PagedModel<TodoResponseDto> findAll(
            @RequestParam(required = false) TodoStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page 는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 는 1 이상이어야 합니다.")
            @Max(value = 100, message = "size 는 100 을 넘을 수 없습니다.") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));

        return new PagedModel<>(todoService.findAll(status, pageable).map(TodoResponseDto::from));
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public TodoResponseDto create(@RequestBody @Valid TodoCreateDto dto) {
        return TodoResponseDto.from(todoService.create(dto));
    }

    @PatchMapping("/{id}")
    public TodoResponseDto update(@PathVariable Long id,
                                  @RequestBody @Valid TodoUpdateDto dto) {
        return TodoResponseDto.from(todoService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public TodoResponseDto changeStatus(@PathVariable Long id,
                                        @RequestBody @Valid TodoStatusUpdateDto dto) {
        return TodoResponseDto.from(todoService.changeStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        todoService.delete(id);
    }
}
