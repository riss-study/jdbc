package dev.riss.jdbc.repository;

import dev.riss.jdbc.domain.Member;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class MemberRepositoryV0Test {

    MemberRepositoryV0 repository=new MemberRepositoryV0();

    @Test
    void crud () throws SQLException {
        Member member = new Member("memberV1", 10001);
        repository.save(member);
    }

}