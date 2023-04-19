package dev.riss.jdbc.sevice;

import dev.riss.jdbc.connection.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV1;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

@RequiredArgsConstructor
public class MemberServiceV1 {

    private final MemberRepositoryV1 memberRepository;

    public void accountTransfer (String fromId, String toId, int money) throws SQLException {
        // tx 시작
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney()-money);

        validation(toMember);

        memberRepository.update(toId, toMember.getMoney()+money);
        // tx 커밋, 롤백
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) throw new IllegalStateException("이체 중 예외 발생");
    }

}
