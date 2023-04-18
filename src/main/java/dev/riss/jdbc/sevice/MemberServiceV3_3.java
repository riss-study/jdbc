package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * Transaction - @Transactional AOP -> remove tx relative logic in service
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_3 {
    private final MemberRepositoryV3 memberRepository;

    // class level 에 붙이면 해당 클래스에 있는 모든 public 메서드(외부에서 호출가능한) 가 다 해당 기능이 붙음
    @Transactional  // Spring AOP 이용 => Spring Proxy 라는 녀석이 빈으로 등록돼있으며 이 친구 안에서 Tx 관련 로직을 다 수행
    public void accountTransfer (String fromId, String toId, int money) throws SQLException {
        bizLogicAccountTransfer(fromId, money, toId);
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
