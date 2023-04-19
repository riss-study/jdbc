package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@RequiredArgsConstructor
@Slf4j
public class MemberServiceV2 {

    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer (String fromId, String toId, int money) throws SQLException {

        Connection conn = dataSource.getConnection();

        try {
            // set autocommit false; => 트랜잭션 시작 (수동 커밋 모드로 변경)
            conn.setAutoCommit(false);

            // business logic
            bizLogicAccountTransfer(conn, fromId, money, toId);

            // commit; => 성공 시 커밋
            conn.commit();

        } catch (Exception e) {

            // rollback;    ==> 실패 시 롤백
            conn.rollback();
            throw new IllegalStateException(e);

        } finally {
            releaseConnection(conn);
        }
    }

    private void bizLogicAccountTransfer(Connection conn, String fromId, int money, String toId) throws SQLException {
        Member fromMember = memberRepository.findById(conn, fromId);
        Member toMember = memberRepository.findById(conn, toId);

        memberRepository.update(conn, fromId, fromMember.getMoney()-money);

        validation(toMember);

        memberRepository.update(conn, toId, toMember.getMoney()+money);
    }

    private void releaseConnection(Connection conn) {
        if (null != conn) {
            try {
                // conn.close 하면 커넥션 풀을 쓰기 때문에 풀로 돌아감
                // 그러므로 set autocommit true 로 기본값으로 돌려놔야 함 (자동 커밋 모드로 변경)
                // 그렇지 않으면 해당 세션 커넥션은 계속 false 로 유지되면서 헷갈림
                conn.setAutoCommit(true);
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) throw new IllegalStateException("이체 중 예외 발생");
    }

}
