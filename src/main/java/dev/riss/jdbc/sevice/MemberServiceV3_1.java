package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;

/**
 * Transaction - Transaction Manager (커넥션을 파라미터로 넘기지 않아도 됨)
 * JDBC 기술에 의존하지 않는 서비스 로직 (대신 (Platform)TransactionManager interface 에 의존)
 * TransactionManager 구현체 DI(의존관계 주입)만 바꿔주면 다른 데이터 접근 기술(ex. JPA) 사용 가능
 * SQLException 의존성이 있지만, 이건 예외 문제 파트에서 나중에 해결
 */
@RequiredArgsConstructor
@Slf4j
public class MemberServiceV3_1 {

//    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    // JDBC 사용하기 때문에 DataSourceTransactionManager 구현체(JDBC 용)를 주입받아야 함 (우린 Test 에서 사용 시 넣어줌)
    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer (String fromId, String toId, int money) {

//        Connection conn = dataSource.getConnection();
        // 트랜잭션 시작(TransactionManager.getTransaction(transactionDefinition) 이용) -> TransactionStatus 반환
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        // DefaultTransactionDefinition => 기본 설정된 definition (나중에 세팅도 바꿀 수 있음)

        try {
            // business logic
            bizLogicAccountTransfer(fromId, money, toId);
            // commit; => 성공 시 커밋 TransactionManager, TransactionStatus 이용
            transactionManager.commit(status);
        } catch (Exception e) {
            // rollback;    ==> 실패 시 롤백 TransactionManager, TransactionStatus 이용
            transactionManager.rollback(status);
            throw new IllegalStateException(e);
        }
        // transactionManager 가 내부에서 commit, rollback 시 release 를 알아서 해주므로(다 정리해줌)
        // finally 에서 release 더이상 안해줘도 됨 (아래 3개를 자동으로 해줌)
        // 1. 트랜잭션 동기화 매니저를 정리(쓰레드로컬은 사용후 꼭 정리해야함)
        // 2. set autocommit true (커넥션 풀을 고려해야하므로)
        // 3. con.close(커넥션 종료, 풀 사용하는 경우는 커넥션 풀에 반환)
    }

    private void bizLogicAccountTransfer(String fromId, int money, String toId) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney()-money);

        validation(toMember);

        memberRepository.update(toId, toMember.getMoney()+money);
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) throw new IllegalStateException("이체 중 예외 발생");
    }

}
