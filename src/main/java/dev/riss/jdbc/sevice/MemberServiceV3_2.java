package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;

/**
 * Transaction - Transaction Template (remove redundant logic like Tx start, commit, rollback in try-catch phrase)
 */
@Slf4j
public class MemberServiceV3_2 {

//    private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;

    // TransactionTemplate 을 주입받으려면 TransactionManager 가 필요. 밖에서 TransactionManager 를 주입받아서 내부에서 생성에 이용
    // (이 방법을 많이 씀. 그러므로 @RequiredArgsConstructor 없앰)
    // why? TransactionTemplate 은 interface 가 아닌 class 이기 때문에 유연성이 없음.
    // => 여러 DB 기술로 바꾸기 위한 TransactionManager 를 유연하게 주입받고 TransactionTemplate 을 내부에서 생성하는 방법을 많이 씀
    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository) {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    public void accountTransfer (String fromId, String toId, int money) {

        // 1. execute 코드 안에서 Tx 가 시작하고 (execute 람다식 안에서 TransactionStatus 를 파라미터로 넘겨줌)
        // 이전에 TransactionManager 에서 status 를 꺼내왔고, TxTemplate 은 TxManager 를 이용해 만드는 것이므로
        // 내부에서 만들어서 파라미터로 넘겨주는 건가봄
        txTemplate.executeWithoutResult((status) -> {
            // 2. 이 콜백 안에서 비즈니스 로직을 시작함
            try {
                bizLogicAccountTransfer(fromId, money, toId);
            } catch (SQLException e) {      // 람다식은 체크 예외를 밖으로 던질 수 없음 (SQLException 은 체크 예외)
                throw new IllegalStateException(e); // 그러므로 try-catch 구문 이용하여 언체크 예외로 바꾸어 던지도록 예외 전환
                // IllegalStateException 은 언체크 예외라
            }
            // 3. 비즈니스 로직이 성공적으로 끝났을 때 commit, 예외(언체크 예외) 발생 시 rollback 동작함, 체크 예외는 커밋함 (나중에 설명)
        });
        // but, 아직 서비스 로직 안에 transaction 처리하는 기술 로직(핵심 기능이 아닌 부가 기능)이 포함돼있음
        // 즉, 두 관심사를 하나의 클래스에서 처리함. -> 트랜잭션 안쓸땐 이걸 뜯어 고쳐야 함 ==> 유지보수 어려움
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
