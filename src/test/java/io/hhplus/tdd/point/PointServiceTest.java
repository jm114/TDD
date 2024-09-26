package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    private UserPoint userPoint;

    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService sut;

    @BeforeEach
    void setUp(){
        sut = new PointService(userPointTable, pointHistoryTable);
        //MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("사용자의 포인트를 조회한다")
    void getPointTest() {
        //given
        long id = 100;
        userPoint = new UserPoint(100, 1000, System.currentTimeMillis());

        //when
        when(userPointTable.selectById(id)).thenReturn(userPoint);
        UserPoint user = sut.getPoint(id);

        //then
        assertThat(user.point()).isEqualTo(1000);
    }


    @Test
    @DisplayName("포인트를 충전하거나 사용할 때 쌓인 이력을 조회한다.")
    void getHistoryTest() {
        //given
        long id = 100;
        List<PointHistory> pointHistories = Stream.of(
                        new PointHistory(1,100,1000,TransactionType.CHARGE, 10),
                        new PointHistory(2,100,500,TransactionType.USE,10),
                        new PointHistory(3,100,1500,TransactionType.CHARGE,10))
                .toList();

        //when
        when(pointHistoryTable.selectAllByUserId(id)).thenReturn(pointHistories);
        List<PointHistory> historyList = sut.getHistory(id);

        //then
        //1. 이력이 존재하는지
        assertThat(historyList).isNotEmpty();
        //2. test 데이터의 갯수
        assertThat(historyList).hasSize(3);
        //3. test 데이터의 유저id가 맞는지
        assertThat(historyList).allMatch(history -> history.userId() == 100);
    }


    @Test
    @DisplayName("포인트를 충전하면 잔여포인트에 더해진다.")
    void chargeTest() {
        //given
        sut.charge(300, 1000);

        //when
        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> sut.charge(300, 5000)),
                CompletableFuture.supplyAsync(() -> sut.charge(300, 200))
        ).join();

        //then
        assertThat(sut.getPoint(300)).isEqualTo(1000+5000+200);
    }

    @Test
    @DisplayName("포인트를 사용하면 잔여포인트가 줄어든다.")
    void useTest() {
        //given
        sut.charge(300, 1000);

        //when
        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> sut.use(300, 500)),
                CompletableFuture.supplyAsync(() -> sut.use(300, 200))
        ).join();

        //then
        assertThat(sut.getPoint(300)).isEqualTo(1000-500-200);
    }
}