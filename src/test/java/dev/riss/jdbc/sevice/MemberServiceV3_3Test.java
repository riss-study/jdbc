package dev.riss.jdbc.sevice;

import dev.riss.jdbc.domain.Member;
import dev.riss.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;

import static dev.riss.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transaction - @Transactional
 */
@SpringBootTest // 스프링 AOP 를 적용하려면 스프링 컨테이너가 필요. 테스트 시 스프링 부트를 통해 스프링 컨테이너 생성해주는 에노테이션
@Slf4j
class MemberServiceV3_3Test {

    public static final String MEMBER_A="memberA";
    public static final String MEMBER_B="memberB";
    public static final String MEMBER_EX="ex";

    @Autowired
    private MemberRepositoryV3 memberRepository;

    @Autowired
    private MemberServiceV3_3 memberService;

    // @Transactional 은 스프링 빈에 등록돼있고 스프링 컨테이너에서 주입받아서 사용해야하기 때문에,
    // 우리가 이용할 datasource, txManager, service, repository 모두 스프링 빈으로 등록해야함
    // (스프링부트 쓰면 그럴 필요 없긴 함 -> 내가 빈으로 등록안했으면 자동으로 dataSource, transactionManager 리소스 빈으로 등록해줌)
    // => application.yml 에 명시해주면 됨
    @TestConfiguration      // Test 안에서 빈 등록해줌 (Test + @Configuration)
    static class TestConfig {
        @Bean
        DataSource dataSource () {
            return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        }

        // 트랙잭션 프록시도 결국 txManager 를 불러서 사용하는 것이기 때문에 txManager 를 Bean 으로 등록해야함
        @Bean
        PlatformTransactionManager transactionManager () {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        MemberRepositoryV3 memberRepositoryV3 () {
            return new MemberRepositoryV3(dataSource());
        }

        @Bean
        MemberServiceV3_3 memberServiceV3_3 () {
            return new MemberServiceV3_3(memberRepositoryV3());
        }
    }

/*    @BeforeEach
    void before () {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository=new MemberRepositoryV3(dataSource);
        memberService=new MemberServiceV3_3(memberRepository);  // @Transactional 로 인해 트랜잭션 프롲시가 앞에서 (AOP 로)
        // Tx 관련 로직을 다 처리하기 때문에,
        // 더이상 TransactionManager 를 이용할 필요가 없음, but 스프링 컨테이너에 등록해야 함 (위 @TestConfiguration 참조)
    }*/

    @AfterEach
    void after () throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    void AopCheck () {
        log.info("memberService class={}", memberService.getClass()); // MemberServiceV3_3$$SpringCGLIB$$0
        // @Transactional -> 스프링이 해당 서비스 로직을 상속받아서
        // 오버라이드해서 Tx start -> try ~ service.logic ~ commit ~ catch~ rollback 코드를 만들어냄
        // 그 만들어낸 결과물이 트랜잭션 프록시임
        // (=> 내부에 트랜잭션을 처리하는 로직을 갖고 있고, 실제 서비스의 타겟을 호출하는 코드도 내부에서 포함하고 있음)
        // 그러므로 여기서의 memberService 는 실제 멤버서비스가 아닌 트랜잭션 프록시 코드임.
        // => 스프링 컨테이너에 실제로는 이 프록시가 스프링 빈으로 등록되고, 이 프록시(CGLIB)를 의존관계 주입받게 된다.
        log.info("memberRepository class={}", memberRepository.getClass()); // MemberRepositoryV3

        assertThat(AopUtils.isAopProxy(memberService)).isTrue();
        assertThat(AopUtils.isCglibProxy(memberService)).isTrue();
        assertThat(AopUtils.isAopProxy(memberRepository)).isFalse();
    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer () throws SQLException {
        // given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);

        memberRepository.save(memberA);
        memberRepository.save(memberB);

        // when
        log.info("START TX");
        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);
        log.info("END TX");

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체 중 예외 발생")
    void accountTransferEx () throws SQLException {
        // given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);

        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        log.info("START TX");
        // when
        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);
        log.info("END TX");

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberEx = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberEx.getMoney()).isEqualTo(10000);
    }

}