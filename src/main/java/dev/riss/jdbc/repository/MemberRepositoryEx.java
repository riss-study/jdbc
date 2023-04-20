package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;

import java.sql.SQLException;

/** * 특정 기술에 종속되는 인터페이스
 * 인터페이스의 구현체가 체크 예외를 던지려면, 인터페이스 메서드에 체크 예외 던지는 부분이 선언돼있어야 함
 * 구현체에서 던질 수 있는 예외는 인터페이스 메서드에서 정의한 던지는 예외이거나 하위 타입어어야 함
 *
 * 이렇게 인터페이스에 체크 예외를 포함해야하기 때문에 결국 이 인터페이스도 특정 기술에 종속적이다.
 * 여기서는 JDBC 기술에 종속적이다.
 * 이미 인터페이스가 특정 구현 기술에 오염돼었기 때문에, 향후 다른 기술로 변경할 때는 인터페이스 자체를 변경해야 함
 *
 * * 런타임 예외와 인터페이스
 * 런타임 예외는 위의 문제에 있어서 자유롭기 때문에 따로 선언하지 않아도 됨. -> 인터페이스가 특정 기술에 종속적일 필요가 없음!!
 */
public interface MemberRepositoryEx {
    Member save(Member member) throws SQLException;
    Member findById(String memberId) throws SQLException;
    void update(String memberId, int money) throws SQLException;
    void delete(String memberId) throws SQLException;
}
