package dev.riss.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exception 을 상속받으면 체크 예외 - 예외를 잡아서 처리하지 않아도 throws 생략 가능
 * RuntimeException 을 상속받으면 언체크 예외(런타임 예외) - 예외를 잡아서 처리하지 않으면 throws 에 던지는 예외를 선언 필수
 * RuntimeException 을 제외한 모든 Exception 을 상속받은 예외는 체크 예외
 * 체크 예외 장점: 개발자 실수로 누락되지 않도록 컴파일단에서 오류를 잡아줌
 * 체크 예외 단점: 모든 체크 예외를 받즈시 잡거나 던지도록 처리해야하므로 번거로움
 * + 의존관계에 따른 단점: ex. 서비스에서 처리하기 힘들고 컨트롤러에 넘기는 예외인데 서비스 단에서도 다 throws 로 명시해줘야 함 (뒤에 설명)
 * 비즈니스적으로 의도적으로 반드시 후처리해야하는 중요한 문제일 때 체크 예외를 사용하고 대부분은 언체크 예외를 사용함
 * 뭐... 후처리를 메뉴얼에 잘 작성해두고 런타임 예외로 해도 됨. 비즈니스상 너무 크리티컬해서 체크를 해야하는 경우는 체크 예외 써도 됨
**/
 @Slf4j
public class CheckedTest {

    @Test
    void checkedCatch () {
        Service service=new Service();
        service.callCatch();        // 정상 흐름으로 리턴되므로 성공
    }

    @Test
    void checkedThrow () {
        Service service=new Service();
        assertThatThrownBy(() -> service.callThrow()).isInstanceOf(MyCheckedException.class);
        // MyCheckedException 에러가 던져졌으므로 테스트 성공
    }

    /**
     * Exception 을 상속받은 예외는 체크 예외가 됨
     */
    static class MyCheckedException extends Exception {
        public MyCheckedException(String message) {     // 오류 메시지를 보관하는 생성자 생성
            super(message);
        }
    }

    /**
     * Checked 예외는 예외를 잡아서 처리하거나, 던지거나 둘중 하나를 필수로 선택해야 한다.
     */
    static class Service {
        Repository repository=new Repository();

        /**
         * 예외를 잡아서 처리하는 코드
         */
        public void callCatch () {      // 얘도 받는 예외가 있으므로 해당 예외를 메서드에 선언하거나 try-catch 로 잡아야 함 (없으면 컴파일 오류)
            try {
                repository.call();
            } catch (MyCheckedException e) {       // Exception e 로 바꿔도 Exception 의 하위 예외이기 때문에 잡힘
                // 예외 처리 로직
                log.info("예외처리, message={}", e.getMessage(), e);
                // 예외를 잡아서 로그로 보여주는 정상 흐름으로 반환(void return)되어 끝남 => 테스트도 성공으로 뜸
            }
        }

        /**
         * 체크 예외를 밖으로 던지는 코드
         * 체크 예외는 예외를 잡지 않고 밖으로 던지려면 throws 예외를 메서드에 필수로 선언해야 함 (없으면 컴파일 오류)
         * @throws MyCheckedException
         */
        public void callThrow () throws MyCheckedException {    // throws Exception 로 선언해도, 하위 예외이기 때문에 던져짐. 대신 모든 예외를 다 던지는 것이기 때문에 좋지 못함
            repository.call();
        }
    }

    static class Repository {
        public void call () throws MyCheckedException {     // 체크 예외는 throws Exception 으로 잡거나 밖으로 던지는 예외를 메서드에 선언해줘야 함
            throw new MyCheckedException("ex"); // 예외를 명시적으로 던지므로 메서드에 선언해줘야 함 (없으면 컴파일 오류)
        }
    }

}
