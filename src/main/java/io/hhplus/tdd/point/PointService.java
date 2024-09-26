package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    //유저별로 동시성을 제어하기 위해 lock 유무 상태를 담는다...?ㅎ
    private final ConcurrentHashMap<Long, Lock> locks = new ConcurrentHashMap<>();

    /**
     * 회원별 포인트 조회
     * @param id
     * @return
     */
    public UserPoint getPoint(long id)
    {
        return userPointTable.selectById(id);
    }


    /**
     * 회원별 포인트 충전/사용 내역 조회
     * @param id
     * @return
     */
    public List<PointHistory> getHistory(long id)
    {
        return pointHistoryTable.selectAllByUserId(id);
    }


    /**
     * 포인트 충전
     * @param id
     * @param amount
     * @return
     *
     * - 최대 잔고는 100만원을 초과할 수 없다.
     * - 충전 금액은 0보다 커야한다.
     */
    public UserPoint charge(long id, long amount)  {
        lock(id);
        try{
            UserPoint user = this.getPoint(id);

            //if(user == null) throw new IllegalArgumentException("유효하지 않은 유저입니다");
            //if(amount <= 0) throw new IllegalArgumentException("충전 금액은 0보다 커야합니다.");
            //if(user.point() + amount >= 1000000) throw new IllegalArgumentException("잔여 포인트는 100만원을 초과할 수 없습니다.");

            UserPoint charged = userPointTable.insertOrUpdate(id, user.point() + amount);

            //충전 내역 저장
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return charged;
        }finally {
            unlock(id);
        }
    }


    /**
     * 포인트 사용
     * @param id
     * @param amount
     * @return
     *
     * - 잔고가 부족하면 사용할 수 없다.
     * - 사용 금액은 0보다 커야한다.
     *
     */
    public UserPoint use (long id, long amount){
        lock(id);
        try{
            UserPoint user = this.getPoint(id);

            //if(user == null) throw new IllegalArgumentException("유효하지 않은 유저입니다");
            //if(amount <= 0) throw new IllegalArgumentException("사용 금액은 0보다 커야합니다.");
            //if(user.point() - amount < 0) throw new IllegalArgumentException("잔여 포인트가 부족합니다.");

            UserPoint charged = userPointTable.insertOrUpdate(id, user.point() - amount);

            //충전 내역 저장
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            return charged;
        }finally {
            unlock(id);
        }
    }



    private void lock(long id){
        locks.computeIfAbsent(id, key -> new ReentrantLock()).lock();
    }

    private void unlock(long id){
        Lock locked = locks.get(id);
        if(locked != null) locked.unlock();
    }

}
