package dev.riss.jdbc.exception.translator;

import dev.riss.jdbc.connection.ConnectionConst;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static dev.riss.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 스프링 예외 추상화를 이용하여 특정 DB 기술에 종속적인 문제를 해결 (에러코드에 관한 예외)
 * JDBC -> JPA 로 구현 기술 변경되더라도 스프링은 해당 예외를 적절한 스프링 데이터 접근 예외로 변환해줌
 * (물론 스프링에 대한 기술 종속성은 발생. 모든 예외를 직접 정의하고 각 DB 별 에러코드를 예외에 맞게 매핑하여 변환하는 것보단 훨씬 실용적임)
 */
@Slf4j
public class SpringExceptionTranslatorTest {

    DataSource dataSource;

    @BeforeEach
    void init () {
        dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    }

    @Test
    @DisplayName("SQL ErrorCode 직접 확인. DB 마다 예외 코드가 다 다르므로, 스프링이 만들어준 예외로 하나하나 변환하는 것은 현실성이 없음")
    void sqlExceptionErrorCode () {
        String sql="select bad grammar";

        try {
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeQuery();
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            assertThat(errorCode).isEqualTo(42122);      //H2 기준
            log.info("errorCode={}", errorCode);
            log.info("error", e);
        }
    }

    @Test
    @DisplayName("스프링이 제공하는 예외 변환기 사용")
    void exceptionTranslator () {
        String sql = "select bad grammar";

        try {
            Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeQuery();
        } catch (SQLException e) {
            assertThat(e.getErrorCode()).isEqualTo(42122);

            SQLErrorCodeSQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);

            // DataAccessException 의 자식 중 하나가 반환(해당하는 예외)
            // how? => org.springframework.jdbc.support.sql-error-codes.xml 에
            // DB 별 에러코드마다 exception 매핑이 명시돼있어서 이를 읽어서 해결
            // 여기서는 BadSqlGrammarException (문법 오류 예외)
            // translator.translate(읽을 수 있는 설명(아무 스트링), 실행한 SQL, 발생된 Exception)
            DataAccessException resultEx = exTranslator.translate("select", sql, e);
            log.info("resultEx", resultEx);

            assertThat(resultEx.getClass()).isEqualTo(BadSqlGrammarException.class);
        }
    }
}
