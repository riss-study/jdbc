package dev.riss.jdbc.sevice;

import dev.riss.jdbc.connection.domain.Member;
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

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transaction - DataSource, TransactionManager 자동 등록
 */
@SpringBootTest // 스프링 AOP 를 적용하려면 스프링 컨테이너가 필요. 테스트 시 스프링 부트를 통해 스프링 컨테이너 생성해주는 에노테이션
@Slf4j
class MemberServiceV3_4Test {

    public static final String MEMBER_A="memberA";
    public static final String MEMBER_B="memberB";
    public static final String MEMBER_EX="ex";

    @Autowired
    private MemberRepositoryV3 memberRepository;

    @Autowired
    private MemberServiceV3_3 memberService;

    // 스프링 부트의 리소스 자동 등록 (DataSource, TransactionManager)
    // application.properties(.yml) 에 지정된 속성을 참조하여
    // DataSource 는 dataSource 라는 이름으로, PlatformTransactionManager 는 transactionManager 라는 이름으로 빈 자동 등록해줌
    // 어떤 TxManager 를 설정할지는 현재 등록된 라이브러리를 보고 판단 (JDBC -> DatsSourceTxManager, JPA -> JPATxManager)
    // 둘다 사용하면 JPATxManager 등록 (DataSourceTxManager 가 제공하는 기능 대부분 지원하기 때문)
    // ** Tx -> 필자가 그냥 Transaction 을 줄여서 적었음 (실제 클래스는 (DataSource OR JPA)TransactionManager 가 맞음)
    @TestConfiguration
    static class TestConfig {

        private final DataSource dataSource;    // 스프링 컨테이너에 자동으로 등록된 빈 (RepositoryV3 생성때문에 주입받음)

        public TestConfig(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Bean
        MemberRepositoryV3 memberRepositoryV3 () {
            return new MemberRepositoryV3(dataSource);
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