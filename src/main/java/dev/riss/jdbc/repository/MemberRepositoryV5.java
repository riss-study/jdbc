package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;

/**
 * JdbcTemplate 사용
 *      1. connection 동기화 (트랜잭션 유지)
 *      2. connection release
 *      3. 스프링 예외로 변환
 *      ==> 다 해줌
 */
@Slf4j
public class MemberRepositoryV5 implements MemberRepository {

    private final JdbcTemplate template;

    public MemberRepositoryV5(DataSource dataSource) {
        this.template=new JdbcTemplate(dataSource);
    }

    @Override
    public Member save (Member member) {
        String sql = "INSERT INTO member(member_id, money) VALUES (?, ?)";
        template.update(sql, member.getMemberId(), member.getMoney());      // 해당 쿼리로 인해 업데이트된 row 숫자 반환
        return member;

    }

    @Override
    public Member findById (String memberId) {
        String sql = "SELECT * FROM member WHERE member_id = ?";
        return template.queryForObject(sql, memberRowMapper(), memberId);   // 조회할 때는 RowMapper<T> 를 이용하여 매핑해줌
        // 하나의 데이터 조회할 땐 queryForObject, 여러 데이터 조회할 땐 query ==> 변환할 Object 로 매핑해서 반환함
        // 물론 매핑시킬 RowMapper<T> 는 구현해야 함 (기존처럼 resultSet 의 커서가 가리키는 column 이용하고 여러개면 next 자동으로 해주는 듯)
    }

    @Override
    public void update (String memberId, int money) {
        String sql = "UPDATE member SET money=? WHERE member_id=?";
        template.update(sql, money, memberId);
    }

    @Override
    public void delete (String memberId) {
        String sql = "DELETE FROM member WHERE member_id=?";
        template.update(sql, memberId);
    }

    private RowMapper<Member> memberRowMapper () {
        return (rs, rowNum) -> {
            Member member = new Member();
            member.setMemberId(rs.getString("member_id"));
            member.setMoney(rs.getInt("money"));

            return member;
        };
    }

}
