package dev.riss.jdbc.connection;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static dev.riss.jdbc.connection.ConnectionConst.*;

@Slf4j
public class ConnectionTest {

    @Test
    void driverManager () throws SQLException {
        Connection conn1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection conn2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        log.info("connection1={}, class={}", conn1, conn1.getClass());
        log.info("connection2={}, class={}", conn2, conn2.getClass());

    }

    @Test
    void dataSourceDriverManager () throws SQLException {
        // DriverManagerDataSource - 항상 새로운 커넥션을 획득 (내부에서 DriverManager 를 쓰기 때문 - DriverManager 는 항상 새로운 커넥션을 획득)
        // Spring 에서 제공
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        userDataSource(dataSource);
        
        // 일반 DriverManager 에서는 gerConnection() 으로 커넥션 조회할 때마다 URL, USERNAME, PASSWORD 같은 정보가 필요했음
        // 하지만, DriverManagerDataSource (DataSource) 를 이용하면, 이미 객체 생성 시점에서 다 세팅을 해놨기 때문에,
        // 커넥션 조회마다 해당 정보를 넘길 필요가 없음
        // ** 설정과 사용의 분리 (RISS) - SRP 의 예 (단일 체계 원칙)
        // - 설정과 관련된 속성들은 한 곳에 세팅돼있어야 변경에 유연
        // - 사용은 설정에 신경쓰지 않고, DataSource 의 getConnection() 만 호출해서 사용하기만 하면 됨
        // => 사용은 설정과 관련된 속성에 의존하지 않아도 되며, 객체만 주입받아서 메서드 호출하면 됨
        // => Repository 는 DataSource 에만 의존하고 위와 같은 속성은 몰라도 됨
        // => 애플리케이션 개발하면 보통 설정은 한 곳에서 하지만, 사용은 수 많은 곳에서 함 (이렇게 제약함)
        // => 이런 제약 덕분에 객체를 설정하는 부분과 사용하는 부분을 명확히 분리할 수 있음, 실수할 일도 줄어듦
    }

    @Test
    void dataSourceConnectionPool () throws SQLException, InterruptedException {
        // 커넥션 풀링 (HikariCP 이용)
        HikariDataSource dataSource = new HikariDataSource();       // 스프링에서 JDBC 쓰면 자동으로 히카리 라이브러리 임포트
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10);      //default 10개, 서버 스펙에 따라 최적화하면서 결정
        dataSource.setPoolName("RissPool");     // 굳이 설정 안해도 댐

        userDataSource(dataSource);
        Thread.sleep(1000);       // sleep 안해주면 우리가 만든 userDataSource() 실행이 굉장히 빠르기 때문에 호출하고 끝나버려서
                            // 커넥션 풀이 10개가 다 풀에 추가되는 로그가 보여지기 전에, 테스트가 끝나버리기 때문에 1초 정도 슬립 걸어줌
        // why? 커넥션 풀 생성 작업은 애플리케이션 작업(실행 시간, ex. 애플리케이션 뜨는 시간)에 영향을 주지 않기 위해서, 별도의 쓰레드에서 작업하기 때문임
        // (poolName connection adder 라는 쓰레드. 여기서는 "RissPool connection adder" 라는 이름의 쓰레드에서 따로 생성)
        // 실제로는 초반에 커넥션 조회하는 시간이 일부 지연될 수 있어서 초반에는 메인 쓰레드에서 바로 커넥션을 생성하게 된다고 함
        // 나도 1개는 메인쓰레드에서 생성됨


        // 로그 중 RissPool - Pool stats (total=7, active=2, idle=5, waiting=0) 를 볼 수 있음
        // test 끝나기 전에 커넥션 close 를 안해줬기 때문에 active 가 2개임 (userDataSource 메서드에서 커넥션 2개 조회했기 때문)
        // 그러므로, 작업이 끝나면 꼭 연결을 닫아줘야 함 (여기서는 반환 -> 커넥션 풀의 커넥션이라 히카리풀이 감싸고 있어서 반환 처리 해줌)
    }

    private void userDataSource (DataSource dataSource) throws  SQLException {
        Connection conn1 = dataSource.getConnection();  // 이게 너무 빨리 동작해서 만약 커넥션 풀에 커넥션이 없는데 이게 실행된다면,
                                                        // 이때는 커넥션이 생성될 때 까지 기다릴 수는 있음
        Connection conn2 = dataSource.getConnection();
        /*Connection conn3 = dataSource.getConnection();
        Connection conn4 = dataSource.getConnection();
        Connection conn5 = dataSource.getConnection();
        Connection conn6 = dataSource.getConnection();
        Connection conn7 = dataSource.getConnection();
        Connection conn8 = dataSource.getConnection();
        Connection conn9 = dataSource.getConnection();
        Connection conn10 = dataSource.getConnection();
        Connection conn11 = dataSource.getConnection();*/
        // 풀에 커넥션이 남아있지 않은데 커넥션 조회하면, 계속 대기
        // 히카리 CP 는 30초 정도 지나면 예외 터트림 (이런 시간은 세팅 가능. 30초 너무 김. 짧게 해서 예외처리해주는 게 나음)
        // java.sql.SQLTransientConnectionException: RissPool - Connection is not available, request timed out after 30009ms.

        // DriverManagerDataSource 객체를 파라미터로 넘기면 서로 다른 커넥션임을 알 수 있음 -> 항상 새로운 커넥션 생성
        log.info("connection1={}, class={}", conn1, conn1.getClass());
        log.info("connection2={}, class={}", conn2, conn2.getClass());
    }

}
