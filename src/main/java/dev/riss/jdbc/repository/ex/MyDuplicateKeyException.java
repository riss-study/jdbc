package dev.riss.jdbc.repository.ex;

// MyDbException 을 상속받아서 DB 관련 예외라는 의미 있는 계층을 형성
// 해당 예외는 데이터 중복인 경우에만 던질 것임
// 직접 만든 예외이므로 JDBC, JPA 등의 특정 기술에 종속적이지 않으므로 서비스 계층의 순수성 유지가 가능함 (기술 변경하더라도 해당 예외 유지 가능)
public class MyDuplicateKeyException extends MyDbException {
    public MyDuplicateKeyException() {
        super();
    }

    public MyDuplicateKeyException(String message) {
        super(message);
    }

    public MyDuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyDuplicateKeyException(Throwable cause) {
        super(cause);
    }
}
