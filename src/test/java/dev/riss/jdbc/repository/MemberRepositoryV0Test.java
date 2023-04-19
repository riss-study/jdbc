package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class MemberRepositoryV0Test {

    MemberRepositoryV0 repository=new MemberRepositoryV0();

    @Test
    void crud() throws SQLException {
        // save
        Member member = new Member("memberV0", 10000);
        repository.save(member);

        // findById
        Member findMember = repository.findById(member.getMemberId());
        log.info("findMember={}", findMember);
        log.info("findMember == member {}", findMember == member);
        log.info("member equals findMember {}", member.equals(findMember));
        // lombok @Data 는 equalsAndHashCode 를 만들어줌. (원래는 equals, hashCode 를 오버라이드해서 만들어야함)
        // 실제로 == 비교는 false => findMember, member 는 각각 따로 생성해준 인스턴스이기 때문. 엄연히 다른 객체임
        // lombok 이 자동으로 만들어줘서 (모든 필드를 비교해서 같으면 equals true 반환), equals 는 true 로 뜸
        assertThat(findMember).isEqualTo(member);

        // update: money 10000 -> 20000
        repository.update(member.getMemberId(), 20000);
        Member updatedMember = repository.findById(member.getMemberId());
        assertThat(updatedMember.getMoney()).isEqualTo(20000);

        // delete
        repository.delete(member.getMemberId());
        assertThatThrownBy(() -> repository.findById(member.getMemberId())).isInstanceOf(NoSuchElementException.class);

    }

}