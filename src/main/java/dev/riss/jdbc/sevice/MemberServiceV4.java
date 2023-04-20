package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예외 누수 문제 해결
 * SQLException 제거
 *
 * MemberRepository Interface 에 의존
 */
@Slf4j
public class MemberServiceV4 {
    private final MemberRepository memberRepository;

    public MemberServiceV4(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void accountTransfer(String fromId, String toId, int money) {
        bizLogicAccountTransfer(fromId, money, toId);
    }

    private void bizLogicAccountTransfer(String fromId, int money, String toId) {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        // 스프링 데이터 접근 예외를 활용한 복구 예시
        try {
            memberRepository.update(fromId, fromMember.getMoney() - money);
        } catch (DuplicateKeyException e) {     // RepositoryV4_2에서 스프링 데이터 접근 예외로 변환해서 던져주기 때문에 가능
            // 복구 로직
        }
        validation(toMember);

        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) throw new IllegalStateException("이체 중 예외 발생");
    }

}
