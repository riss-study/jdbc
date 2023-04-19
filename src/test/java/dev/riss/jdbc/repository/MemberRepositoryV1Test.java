package dev.riss.jdbc.repository;

import com.zaxxer.hikari.HikariDataSource;
import dev.riss.jdbc.connection.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import static dev.riss.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class MemberRepositoryV1Test {

    MemberRepositoryV1 repository;

    @BeforeEach
    void beforeEach () {
        // 기본 DriverManager - 항상 새로운 커넥션을 획득
        // 이걸 쓰면 sql 실행할 때마다 새로운 커넥션을 생성해서 실행하는 걸(0 -> 1 -> 2 -> 3 ...) 볼 수 있음 (Test 에 Log 찍힘)
//        DriverManagerDataSource dataSource=new DriverManagerDataSource(URL, USERNAME, PASSWORD);

        // 커넥션 풀링
        // CRUD 각각 할 때마다 커넥션을 썼다 반환하고, 그다음에 다시 커넥션을 썼다 반환하기 때문에,
        // 순차적으로 로직이 돌아서, getConnection()으로 커넥션 조회할 때마다 계속 첫번째꺼인 conn0 을 가져다 쓰는 것을 볼 수 있음
        // (0 -> 0 -> 0 -> 0 -> ...) => 커넥션 재사용
        // 물론 동시에 여러 요청이 오면, 여러 쓰레드에서 커넥션 풀의 커넥션을 다양하게 가져감
        // ** 참고
        // hikari 커넥션 풀에서 커넥션을 반환(클라이언트가 조회 시)해줄 때, HikariProxyConnection 객체를 생성해서
        // 그 안에 실제 커넥션을 감싸서(wrapping) 반환함(CGLIB 인듯).
        // 그러므로 실제 HikariProxyConnection 객체 주소는 같은 커넥션을 가져오더라도 커넥션 가져올 때마다 다르게 찍힘
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);

        repository=new MemberRepositoryV1(dataSource);      // 만든 dataSource 등록해서 레포지토리 생성

        // 여기서 중요!!
        // DI 관점에서 DataSource 구현체를 바꾸더라도 (DriverManagerDataSource -> HikariDataSource)
        // MemberRepositoryV1 의 코드는 전혀 변경하지 않아도 됨
        // => why? MemberRepositoryV1 은 DataSource 라는 추상화된 인터페이스에만 의존하기 때문임!!
        // (DataSource 를 사용하는 이유이자 장점)
        // => OCP (개방 폐쇄 원칙)
    }

    @Test
    void crud() throws SQLException, InterruptedException {
        // save
        Member member = new Member("memberV0", 10000);
        repository.save(member);

        // findById
        Member findMember = repository.findById(member.getMemberId());
        log.info("findMember={}", findMember);
        log.info("findMember == member {}", findMember == member);
        log.info("member equals findMember {}", member.equals(findMember));

        assertThat(findMember).isEqualTo(member);

        // update: money 10000 -> 20000
        repository.update(member.getMemberId(), 20000);
        Member updatedMember = repository.findById(member.getMemberId());
        assertThat(updatedMember.getMoney()).isEqualTo(20000);

        // delete
        repository.delete(member.getMemberId());
        assertThatThrownBy(() -> repository.findById(member.getMemberId())).isInstanceOf(NoSuchElementException.class);

        Thread.sleep(1000);

    }

}