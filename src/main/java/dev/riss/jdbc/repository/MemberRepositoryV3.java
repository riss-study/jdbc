package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * Transaction - Transaction Manager
 * DataSourceUtils.getConnection()
 * DataSourceUtils.releaseConnection()
 */
@Slf4j
public class MemberRepositoryV3 {

    private final DataSource dataSource;

    public MemberRepositoryV3(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Member save (Member member) throws SQLException {
        String sql = "INSERT INTO member(member_id, money) VALUES (?, ?)";

        Connection conn=null;
        PreparedStatement pstmt=null;

        try {

            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            pstmt.executeUpdate();


            return member;

        } catch (SQLException e) {

            log.error("db error", e);
            throw e;

        } finally {
            close(conn, pstmt, null);
        }

    }

    public Member findById (String memberId) throws SQLException {
        String sql = "SELECT * FROM member WHERE member_id = ?";

        Connection conn=null;
        PreparedStatement pstmt=null;
        ResultSet rs=null;

        try {

            conn=getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();

            if (rs.next()) {

                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;

            } else {
                throw new NoSuchElementException("member not found memberId=" + memberId);
            }

        } catch (SQLException e) {

            log.error("db error", e);
            throw e;

        } finally {
            close(conn, pstmt, rs);
        }
    }

    public void update (String memberId, int money) throws SQLException {
        String sql = "UPDATE member SET money=? WHERE member_id=?";

        Connection conn=null;
        PreparedStatement pstmt=null;

        try {

            conn=getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize (해당 쿼리를 통해 변경된 tuple(row) 수) = {}", resultSize);

        } catch (SQLException e) {

            log.error("db error", e);
            throw e;

        } finally {
            close(conn, pstmt, null);
        }
    }

    public void delete (String memberId) throws SQLException {
        String sql = "DELETE FROM member WHERE member_id=?";

        Connection conn=null;
        PreparedStatement pstmt=null;

        try {

            conn=getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, memberId);
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize (해당 쿼리를 통해 삭제된 tuple(row) 수) = {}", resultSize);

        } catch (SQLException e) {

            log.error("db error", e);
            throw e;

        } finally {
            close(conn, pstmt, null);
        }

    }

    private void close (Connection conn, Statement stmt, ResultSet rs) {

        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        // 주의! 트랜잭션 동기화를 사용하려면 DataSourceUtils 를 사용해야 함
        // 트랜잭션을 사용하기 위해 동기화된 커넥션(트랜잭션 동기화 매니저에서 가져온 커넥션)은 커넥션을 닫지 않고 그대로 유지해준다.
        // 트랜잭션 동기화 매니저가 관리하는 커넥션(그렇지 않은 커넥션)이 아닌 경우 해당 커넥션을 닫는다.
        DataSourceUtils.releaseConnection(conn, dataSource);
//        JdbcUtils.closeConnection(conn);
        
    }

    private Connection getConnection() throws SQLException {
        // 주의! 트랜잭션 동기화를 사용하려면 DataSourceUtils 사용해야 함
        // 트랜잭션 동기화 매니저가 관리하는 커넥션이 있으면, 해당 커넥션을 반환함
        // 없는 경우, 새로운 커넥션을 생성해서 반환함
        Connection conn = DataSourceUtils.getConnection(dataSource);
//        Connection conn = dataSource.getConnection();
        log.info("get connection={}, class={}", conn, conn.getClass());
        return conn;
    }

}
