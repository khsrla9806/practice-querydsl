package com.querydsl.entity;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static com.querydsl.entity.QMember.member;
import static com.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
class MemberTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void init() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void testJPQL() {
        String query = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(query, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void testQueryDsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void AndQuery1() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("and 쿼리는 ,로 구분해서 넣으면 된다.")
    void AndQuery2() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"), member.age.eq(10))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void fetch() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        assertThat(members).hasSize(4);
    }

    @Test
    @DisplayName("fetchOne 결과가 1개가 아니면 NonUniqueResultException 반환")
    void fetchOne1() {
        assertThatThrownBy(() -> queryFactory.selectFrom(member).fetchOne())
                .isInstanceOf(NonUniqueResultException.class);
    }

    @Test
    @DisplayName("fetchOne 결과가 0개이면 Null 반환")
    void fetchOne2() {
        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("none"))
                .fetchOne();

        assertThat(member).isNull();
    }

    @Test
    @DisplayName("fetchFirst = limit(1) + fetchOne")
    void fetchFirst() {
        Member fetchFistMember = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();

        Member limitOneMember = queryFactory
                .selectFrom(member)
                .limit(1)
                .fetchOne();

        assertThat(fetchFistMember.getUsername()).isEqualTo(limitOneMember.getUsername());
    }

    @Test
    @Deprecated
    @DisplayName("deprecated 되었다고 한다. | count 쿼리까지 발생하는 메서드")
    void fetchResults() {
        QueryResults<Member> memberResults = queryFactory
                .selectFrom(member)
                .fetchResults();

        List<Member> content = memberResults.getResults();
        long total = memberResults.getTotal();

        assertThat(content).hasSize(4);
        assertThat(total).isEqualTo(4L);
    }

    @Test
    @Deprecated
    @DisplayName("deprecated 되었다고 한다. | count 쿼리를 발생시키는 메서드")
    void fetchCount() {
        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

        assertThat(total).isEqualTo(4);
    }

    @Test
    @DisplayName(
            "1. 회원 나이를 내림차순 정렬" +
            "2. 회원 이름을 오름차순 정렬" +
            "3. 회원 이름이 null 이면 가장 마지막에 출력"
    )
    void sort() {
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member(null, 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    @DisplayName("offset, limit 이용하여 페이징 쿼리")
    void paging() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .offset(1)
                .limit(2)
                .orderBy(member.username.desc())
                .fetch();

        Member member3 = members.get(0);
        Member member2 = members.get(1);

        assertThat(member3.getUsername()).isEqualTo("member3");
        assertThat(member2.getUsername()).isEqualTo("member2");
    }

    @Test
    @DisplayName("집합 함수")
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.avg(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    void groupBy() {
        List<Tuple> result = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

}