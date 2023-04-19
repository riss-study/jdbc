package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * JDBC - DataSource, JdbcUtils 사용
 */
@Slf4j
public class MemberRepositoryV1 {

    private final DataSource dataSource;

    public MemberRepositoryV1(DataSource dataSource) {
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

    // close 를 하더라도, 풀에서 꺼낸거면 실제로 히카리풀이라는 게 감싸고 있어서, close 요청이 오면 커넥션 풀의 커넥션을 반환하는 로직이 담겨 있음
    private void close (Connection conn, Statement stmt, ResultSet rs) {

        // SQLException 뿐만 아니라 다른 exception 들도 대비해서 JdbcUtils 에서 잘 만들어놨음
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(conn);
        
    }

    private Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        log.info("get connection={}, class={}", conn, conn.getClass());
        return conn;
    }

}
