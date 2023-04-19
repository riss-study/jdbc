package dev.riss.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 언체크 예외도 throws 선언하면 개발자가 IDE 를 통해 해당 예외가 발생한다는 것을 인지할 수 있음 (딱 그정도 도움)
// 언체크 예외 장점: 신경쓰고 싶지 않은 언체크 예외를 다 무시할 수 있음(생략 가능),
// + 신경쓰고 싶지 않은 언체크 예외의 의존관계를 참조하지 않아도 됨 (서비스 단에서 선언안해도 컨트롤러단에서 처리할 수 있음)
// 언체크 예외 단점: 잡아야하는 언체크 예외임에도 불구하고, 개발자가 실수로 누락할 수 있음. <-> 체크 예외
@Slf4j
public class UncheckedTest {

    @Test
    void uncheckedCatch() {
        Service service=new Service();
        service.callCatch();
    }

    @Test
    void uncheckedThrow () {
        Service service=new Service();
        assertThatThrownBy(() -> service.callThrow()).isInstanceOf(MyUncheckedException.class);
    }

    /**
     * RuntimeException 을 상속받은 예외는 언체크 예외(런타임 예외)가 된다.
     */
    static class MyUncheckedException extends RuntimeException {
        public MyUncheckedException(String message) {
            super(message);
        }
    }

    /**
     * Unchecked 예외는 예외를 잡거나 던지지 않아도 됨
     * 예외를 잡지 않으면 자동으로 밖으로 던짐
     */
    static class Service {
        Repository repository=new Repository();

        /**
         * 필요한 경우 예외를 잡아서 처리하면 됨
         */
        public void callCatch () {
            try {
                repository.call();
            } catch (MyUncheckedException e) {
                // 예외 처리 로직
                log.info("예외 처리 message={}", e.getMessage(), e);
            }
        }

        /**
         * 예외를 잡지 않아도 자연스럽게 상위로 예외 던짐
         * 체크 예외와 다르게 throws 예외 선언하지 않아도 됨
         */
        public void callThrow () {      // 예외 선언하라고 컴파일 오류 안뜸. 선언 해도 됨
            repository.call();
        }
    }

    static class Repository {
        public void call () {       // throws MyUncheckedException 생략 가능
            throw new MyUncheckedException("ex");
        }
    }

}
