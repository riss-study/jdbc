package dev.riss.jdbc.exception.translator;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.ex.MyDbException;
import dev.riss.jdbc.repository.ex.MyDuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import static dev.riss.jdbc.connection.ConnectionConst.*;

/**
 * SQL ErrorCode 를 이용하여 데이터베이스에 어떤 오류가 있는 지 확인 가능 => 예외 변환 사용
 * 예외 변환을 통해 SQLException 을 특정 기술에 의존하지 않는 직접 만든 예외인 MyDuplicateKeyException 으로 변환
 * Repository layer(계층)에서 예외를 전환(변환)해준 덕분에 서비스 계층은 특정 기술에 의존하지 않는 MyDuplicateKeyException 을 사용하여
 * 문제를 복구하고, 서비스 계층의 순수성 유지 가능(특정 기술에 의존하지 않음)
 *
 * but, SQL ErrorCode 는 DB 마다 다 다름. 결국, DB 가 변경될 때마다 에러 코드도 변경해야 함. 에러코드는 수백가지가 있음
 *      ex. 키 중복 오류 코드는 H2:23505, MySQL:1062 로 다르다.
 * => 스프링이 이러한 각각 다른 DB 들의 예외를 추상화해서 제공해줌
 */
@SpringBootTest
public class ExTranslatorV1Test {

    @Autowired
    private DataSource dataSource;
    Repository repository;
    Service service;

    @BeforeEach
    void init () {
//        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        repository = new Repository(dataSource);
        service=new Service(repository);
    }

    @Test
    void duplicateKeySave () {
        service.create("myId");
        service.create("myId"); // 같은 id 저장 시도
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Service {
        private final Repository repository;

        public void create (String memberId) {
            try {
                repository.save(new Member(memberId, 0));
                log.info("saveId={}", memberId);
            } catch (MyDuplicateKeyException e) {
                log.info("키 중복, 복구 시도");
                String retryId = generateNewId(memberId);
                log.info("retryId={}", retryId);
                repository.save(new Member(retryId, 0));
            } catch (MyDbException e) {     // 어차피 공통 처리 로직에서 처리하고 MyDbException 은 런타임 예외이기 때문에,
                // 따로 catch 잡아서 던져줄 필요 없음. 여기서는 다양하게 예외를 잡아서 처리할 수 있다는 것을 보여주기 위한 코드임
                log.info("데이터 접근 계층 예외", e);
                throw e;
            }
        }

        private String generateNewId (String memberId) {
            return memberId + new Random().nextInt(10000);
        }
    }



    @RequiredArgsConstructor
    static class Repository {
        private final DataSource dataSource;

        public Member save (Member member) {
            String sql = "INSERT INTO member(member_id, money) VALUES(?, ?)";
            Connection conn=null;
            PreparedStatement pstmt=null;

            try {
                conn = dataSource.getConnection();      // 트랜잭션 동기화 x. test 이므로 그냥 사용 (<-> DataSourceUtils)
                pstmt=conn.prepareStatement(sql);
                pstmt.setString(1, member.getMemberId());
                pstmt.setInt(2, member.getMoney());
                pstmt.executeUpdate();
                return member;
            } catch (SQLException e) {
                // h2 db
                if (23505 == e.getErrorCode()) throw new MyDuplicateKeyException(e);
                throw new MyDbException(e);

            } finally {
                JdbcUtils.closeStatement(pstmt);
                JdbcUtils.closeConnection(conn);
            }
        }
    }
}
