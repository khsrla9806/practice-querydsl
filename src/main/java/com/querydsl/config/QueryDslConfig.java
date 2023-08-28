package com.querydsl.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;

@Configuration
public class QueryDslConfig {

    /* Repository 에서 직접 주입해줘도 상관없고, 이렇게 Bean 으로 등록해도 된다. */
    /*
        Q. 동시성 문제는 상관이 없는가?
        : 스프링이 주입해주는 엔티티 매니저는 실제 동작 시점에 실제 엔티티 매니저를 찾기 위한
          프록시 객체를 넣어주기 때문에, 실제 동작에서는 Transaction 단위로 동작 되기 때문에
          동시성 문제는 걱정하지 않아도 된다.
    */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
