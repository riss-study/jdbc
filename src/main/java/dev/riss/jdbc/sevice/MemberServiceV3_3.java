package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * Transaction - @Transactional AOP -> remove tx relative logic in service
 */
@Slf4j
public class MemberServiceV3_3 {
    private final MemberRepositoryV3 memberRepository;

    public MemberServiceV3_3(MemberRepositoryV3 memberRepository) {
        this.memberRepository = memberRepository;
    }

    // class level 에 붙이면 해당 클래스에 있는 모든 public 메서드(외부에서 호출가능한) 가 다 해당 기능이 붙음
    @Transactional  // Spring AOP 이용 => Spring Proxy 라는 녀석이 빈으로 등록돼있으며 이 친구 안에서 Tx 관련 로직을 다 수행
    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        bizLogicAccountTransfer(fromId, money, toId);
    }

    /** 트랜잭션 AOP 적용 전체 흐름
     * 1. 클라이언트(컨트롤러, 테스트케이스 등) 에서 AOP 프록시 호출
     *
     * 2. AOP 프록시 내부에서 트랜잭션을 시작 (스프링 컨테이너를 통해 트랜잭션 매니저 획득)
     * => 미리 빈으로 등록된 트랜잭션 매니저 (ex. DataSourceTransactionManageR)
     * 3. transactionManager.getTransaction() 으로 트랜잭션을 시작!!
     * 4. 해당 메소드에 의해 데이터 소스로 커넥션 생성 (ex. dataSource.getConnection() )
     * 5. 해당 메소드에 의해 conn.setAutoCommit(false) 로 실제로 진짜 트랜잭션 시작 (수동 커밋 모드 활성화)
     *
     * 6. 해당 커넥션을 트랜잭션동기화매니저(TransactionSynchronizationManager) 에 보관 (동기화해놈)
     * 7. 커넥션이 보관됨
     *
     * 8. AOP 프록시에서 실제 서비스 로직을 호출 -> 서비스에서 비즈니스 로직 수행 -> 리포지토리 데이터 접근 로직 수행
     * 9. 리포지토리에서 동기화된 커넥션을 꺼내서 데이터(DB) 처리
     * (DataSourceUtils.getConnection(dataSource) 수행하면 내부에서 TxSyncManager 를 통해 동기화된 커넥션을 획득)
     *
     * 10. 다 끝나면 return -> ... -> return
     * 11. return 하면서 성공이면 commit, 예외가 발생하면 rollback 수행 후 최종 커넥션이 커넥션 풀로 반환됨 (풀이 없으면 커넥션 종료)
     *
     * 선언적 트랜잭션 관린 vs 프로그래밍 방식 트랜잭션 관리
     * - 선언적 트랜잭션 관리 => @Transactional 애노테이션 하나만 선언해서 편리하게 Tx 을 적용하는 것 (과거에는 XML 로 설정하기도 했음)
     * - 프로그래밍 방식 트랜잭션 관리 => 트랜잭션 매니저 또는 트랜잭션 템플릿 등을 사용해서 트랜잭션 관련 코드를 직접 작성하는 방식
     * (우리가 @Transactional 이전에 계속 했던 방식 - ~~ MemberServiceV3_2까지)
     */
    private void bizLogicAccountTransfer(String fromId, int money, String toId) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);

        validation(toMember);

        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) throw new IllegalStateException("이체 중 예외 발생");
    }

}
