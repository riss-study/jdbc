package dev.riss.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 지저분한 throws 도 사라지고, 레포지토리를 제외한 서비스, 컨트롤러에서의 특정 기술(JDBC 등)에 대한 의존관계가 사라짐!
 * (신경쓰지 않아도 됨, DI 장점 & OCP 지킴)
 * => 물론 이런 복구 불가능한 예외는 일관성있게 공통 처리를 해야함 (ex. filter, interceptor, ControllerAdvice)
 *
 * 구현 기술이 변경되는 경우, 예외를 공통으로 처리하는 곳에서는 예외에 따른 다른 처리가 필요할 수 있지만, 여기 한곳만 변경하면 된다.
 * 공통 처리 부분에서는 필요한 경우 특정 런타임 예외에 대한 의존관계 존재 가능 (ex. RuntimeSQLException -> RuntimeJPAException)
 * 
 * ** 초기 자바 설계 당시, 체크 예외가 더 낫다고 생각하여 대부분의 라이브러리들이 체크 예외를 제공하였으나, 해당 문제로 인해
 *      최근 라이브러리들은 대부분 런타임(언체크) 예외를 기본으로 제공 (JPA 도 런타임 예외 제공)
 *      런타임 예외도 필요 시 체크 예외처럼 잡아서 처리하고, 그렇지 않으면 자연스럽게 던지도록 두고
 *      예외 공통 처리 부분을 앞에서 구현하여 처리하면 됨
 *      ** 런타임 예외는 놓칠 수 있기에 문서화가 중요!! or throws 런타임예외 를 코드로 명시하여 인지
 */
@Slf4j
public class UncheckedAppTest {

    @Test
    void unchecked () {
        Controller controller = new Controller();
        assertThatThrownBy(() -> controller.request())
                .isInstanceOf(Exception.class);
    }

    static class Controller {
        Service service=new Service();

        public void request () {
            service.logic();
        }
    }

    static class Service {

        Repository repository=new Repository();
        NetworkClient networkClient=new NetworkClient();

        public void logic () {
            repository.call();
            networkClient.call();
        }

    }

    static class NetworkClient {
        public void call () {
            try {
                runNetwork();
            } catch (ConnectException e) {
                throw new RuntimeConnectException(e.getMessage());
            }
        }

        public void runNetwork () throws ConnectException {
            throw new ConnectException("연결 실패");
        }
    }

    static class Repository {
        public void call () {
            try {
                runSQL();
            } catch (SQLException e) {      // 레포지토리 안에서는 SQLException 을 잡지만
                                            // 던질때는 만들어놓은 언체크 에러인 RuntimeSQLException 로 전환하여 던짐
                throw new RuntimeSQLException(e);
            }
        }

        public void runSQL () throws SQLException {
            throw new SQLException("ex");
        }
    }

    static class RuntimeConnectException extends RuntimeException {
        public RuntimeConnectException(String message) {
            super(message);
        }
    }

    static class RuntimeSQLException extends RuntimeException {
        public RuntimeSQLException(Throwable cause) {
            super(cause);
        }
    }

}
