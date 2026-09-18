package com.beac.todo.initData;

import com.beac.todo.entity.Todo;
import com.beac.todo.entity.TodoStatus;
import com.beac.todo.service.TodoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import static com.beac.todo.entity.TodoStatus.*;

@Configuration
@Slf4j
public class DataInit {
    private final DataInit self;
    private final TodoService todoService;

    public DataInit(@Lazy DataInit self, TodoService todoService) {
        this.self = self;
        this.todoService = todoService;
    }

    @Bean
    public ApplicationRunner baseInitDataRunner() {
        return args -> {
            self.makeBaseTodos();
        };
    }

    @Transactional
    public void makeBaseTodos() {
        if (todoService.count() > 0) {
            return;
        }

        make("우유 사기", "2L 한 통", TODO);
        make("스프링 공부하기", "JPA 변경 감지와 영속성 컨텍스트 정리", DONE);
        make("헬스장 가기", "하체 루틴 40분", DONE);
        make("치과 예약", "정기 검진, 평일 오후로 잡기", TODO);
        make("전기요금 납부", "자동이체 실패분 수동 결제", DONE);
        make("이력서 업데이트", "최근 프로젝트 2건 추가", TODO);
        make("방 청소", "책상 정리하고 빨래 돌리기", TODO);
        make("알고리즘 문제 풀기", "백준 그래프 탐색 3문제", TODO);
        make("부모님께 전화", null, DONE);
        make("도서관 책 반납", "연체까지 하루 남음", TODO);
        make("노트북 백업", "외장하드에 프로젝트 폴더 복사", DONE);
        make("장보기", "계란, 양파, 두부, 간장", TODO);
        make("포트폴리오 사이트 배포", "도메인 연결까지 마무리", TODO);
        make("건강검진 예약", "회사 지원 대상, 연말까지", TODO);
        make("영어 단어 암기", "하루 30개씩", DONE);
        make("자전거 정비", "체인 기름칠하고 공기압 확인", TODO);
        make("세금 신고 서류 준비", "원천징수영수증 발급받기", TODO);
        make("친구 생일 선물 고르기", "다음 주 토요일 모임 전까지", TODO);
        make("냉장고 정리", "유통기한 지난 것 버리기", DONE);
        make("자격증 원서 접수", "정보처리기사 필기, 접수 기간 확인", TODO);
    }

    private void make(String title, String content, TodoStatus status) {
        Todo todo = Todo.create(title, content);
        todo.changeStatus(status);

        todoService.save(todo);
    }
}
