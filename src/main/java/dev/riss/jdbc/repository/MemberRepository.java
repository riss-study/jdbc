package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;

/**
 * * 런타임 예외와 인터페이스
 * 런타임 예외는 위의 문제에 있어서 자유롭기 때문에 따로 선언하지 않아도 됨. -> 인터페이스가 특정 기술에 종속적일 필요가 없음!!
 */
public interface MemberRepository {
    Member save(Member member);
    Member findById(String memberId);
    void update(String memberId, int money);
    void delete(String memberId);
}
