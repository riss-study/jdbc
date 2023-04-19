package dev.riss.jdbc.exception.basic;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 모든 호출하는 밖으로 던져야하는 메서드에서 throws SQLException, ConnectException 를 선언해줘야 함 (둘다 체크 에러이기 때문)
 *
 * 체크 예외 2가지 문제
 * 1. 복구 불가능한 예외 - 대부분의 예외는 복구 불가능함 (데이터베이스나 네트워크 통신처럼 시스템 레벨에서 올라온 예외들은 대부분 복구 불가능)
 *  ex. SQLException 은 SQL 문법 오류, 데이터베이스 자체 문제, 데이터베이스 서버 다운 등 복구가 불가능한 문제임
 *      서비스나 컨트롤러에서는 해당 문제 해결 불가능. 이런 문제는 일관성있게 공통 처리해야 함(오류 로그 남기고 개발자의 빠른 인지 후 처리 필요).
 *      서블릿 필터, 스프링 인터셉터, 스프링 ControllerAdvice 를 사용하여 깔끔한 공통 해결 가능
 * 2. 의존관계에 대한 문제 - 컨트롤러나 서비스 입장에서 본인이 처리할 수 없어도 어쩔 수 없이 throws 를 통해 던지는 예외 선언해야 함
 *  why 문제? => ex.서비스, 컨트롤러에서 ex. java.sql.SQLException 을 의존하게 됨!!
 *  => 이건 JDBC 기술임. 고로 JDBC 기술에 의존하게 됨, 향후 다른 기술로 변경하면 ex. JPA 라면 해당 exception 에 의존하도록 수정해야함
 *      (OCP 깨짐 => 클라이언트 코드 변경 없이 구현체 변경이 가능한 DI 의 장점이 사라짐)
 *
 * ** 해당 부분을 throws Exception 으로 해결하기엔 다른 모든 체크 예외(중요하더라도)를 다 밖으로 던지는 문제가 발생 (체크 예외 기능이 무효화됨)
 * ==> AntiPattern!! Exception 을 밖으로 던지지 말자 ====> Unchecked(Runtime) Exception 활용하자!!
  */

public class CheckedAppTest {

    @Test
    void checked () {
        Controller controller = new Controller();
        assertThatThrownBy(() -> controller.request())
                .isInstanceOf(Exception.class);
    }

    static class Controller {
        Service service=new Service();

        public void request () throws SQLException, ConnectException {
            service.logic();
        }
    }

    static class Service {

        Repository repository=new Repository();
        NetworkClient networkClient=new NetworkClient();

        public void logic () throws SQLException, ConnectException {
            repository.call();
            networkClient.call();
        }

    }

    static class NetworkClient {
        public void call () throws ConnectException {
            throw new ConnectException("연결 실패");
        }
    }

    static class Repository {
        public void call () throws SQLException {
            throw new SQLException("ex");
        }
    }
}
