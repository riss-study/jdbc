package dev.riss.jdbc.repository;

import dev.riss.jdbc.connection.DBConnectionUtil;
import dev.riss.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

/**
 * JDBC - DriverManager 사용
 */
@Slf4j
public class MemberRepositoryV0 {

    public Member save (Member member) throws SQLException {
        String sql = "INSERT INTO member(member_id, money) VALUES (?, ?)";

        Connection conn=null;
        PreparedStatement pstmt=null;       // SQLInjection 공격 방지하려면 PreparedStatement 써야 함
                                        // ? 를 통해 파라미터를 바인딩하면 단순히 데이터로 취급되기 때문에
                                        // SQLInjection 같이 데이터 부분에 sql 문이 들어오더라도 sql 로 취급되지 않음

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, member.getMember_id());  // sql 의 ? 부분에 순서에 맞춰 파라미터 바인딩 (여긴 첫번째 ?)
            pstmt.setInt(2, member.getMoney());     // 여긴 두번째 ? 에 money 값 지정해줌
            pstmt.executeUpdate();  // 준비한 sql 을 DB 에 전달 (이 sql 에 영향받은 row 숫자를 반환해줌)

            return member;
        } catch (SQLException e) {
            log.error("db error", e);
            throw e;
        } finally {
            // connection 은 실제 외부 리소스(TCP/IP) 를 사용하는 것, 닫지 않으면 해당 리소스가 계속 반환되지 않은 채 존재할 수 있음
            // 리소스 누수 -> 커넥션 부족 장애로 이어질 수 있음
            // close 는 시작과 역순 (conn 먼저 열고 pstmt 를 열었으므로)
            close(conn, pstmt, null);
        }

    }

    private void close (Connection conn, Statement stmt, ResultSet rs) {

        // open: Connection -> (Prepared)Statement -> ResultSet 순
        // close 그 반대순
        if (null != rs) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.info("error", e);   // rs 도 아래와 마찬가지임
            }
        }

        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.info("error", e);   // statement 닫을 때 예외 터지면 사실상 해줄 수 있는게 없음. 그러므로 일단 로그만 찍음
            }
        }

        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.info("error", e);   // 여기도 닫을 때 터지는 건 해줄 수 있는 게 없음.
            }
        }
        // stmt 가 SQLException 이 터지더라도 catch 에서 밖으로 return, throw 를 해주지 않으므로 conn 도 닫을 수 있음
        // 실제로 stmt 가 예외가 터져서 던져지면 stmt.close() 다음에 conn 을 닫아야 하는데 닫지 못하는 상황이 생기면 안됨
        
    }

    private Connection getConnection() {
        return DBConnectionUtil.getConnection();
    }

}
