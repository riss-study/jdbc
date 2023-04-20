package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.ex.MyDbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * SQLExceptionTranslator 추가
 *
 * 스프링 예외 추상화 -> 서비스 계층은 특정 리포지토리의 구현 기술과 예외에 종속적이지 않게 됨. 구현 기술이 변경돼도 그대류 유지
 * ==> 다시 DI 제대로 활용 가능
 *
 * 서비스 계층에서 예외를 잡아서 복구해야 하는 경우, 예외가 스프링이 제공하는 데이터 접근 예외로 변경돼서 넘어오기 때문에
 * 필요한 경우 예외를 잡아서 복구하면 됨 (스프링 기술에만 종속적)
 *
 * but, 아직 각 CRUD 메소드 안에서의 conn, pstmt, try-catch 가 중복임
 */
@Slf4j
public class MemberRepositoryV4_2 implements MemberRepository {

    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator;  //SQLErrorCodeExceptionTranslator 는 이 인터페이스의 구현체 중 하나

    public MemberRepositoryV4_2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
        // error code 기반으로 스프링 데이터 접근 예외 찾는 변환기를 등록한 거임. 다른 변환기 구현체도 존재
    }

    @Override
    public Member save (Member member) {
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
            throw exTranslator.translate("save", sql, e);       // 스프링 예외변환기 사용해서 스프링예외로 변환해서 던짐
        } finally {
            close(conn, pstmt, null);
        }

    }

    @Override
    public Member findById (String memberId) {
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
            throw exTranslator.translate("findById", sql, e);
        } finally {
            close(conn, pstmt, rs);
        }
    }

    @Override
    public void update (String memberId, int money) {
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
            throw exTranslator.translate("update", sql, e);
        } finally {
            close(conn, pstmt, null);
        }
    }

    @Override
    public void delete (String memberId) {
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
            throw exTranslator.translate("delete", sql, e);
        } finally {
            close(conn, pstmt, null);
        }

    }

    private void close (Connection conn, Statement stmt, ResultSet rs) {

        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(conn, dataSource);
        
    }

    private Connection getConnection() {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        log.info("get connection={}, class={}", conn, conn.getClass());
        return conn;
    }

}
