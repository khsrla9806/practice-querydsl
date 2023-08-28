package com.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.dto.MemberDto;
import com.querydsl.dto.QMemberDto;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.querydsl.entity.QMember.member;
import static com.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
class MemberTest {

    static final List<String> usernames = List.of("member1", "member2", "member3", "member4");
    static final List<Integer> ages = List.of(10, 20, 30, 40);

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

    @Test
    @DisplayName("내부 조인")
    void innerJoin() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("외부 조인")
    void leftOuterJoin() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamB"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member3", "member4");
    }

    @Test
    @DisplayName("세타 조인 (막 조인, Cross Join)")
    void theTaJoin() {
        em.persist(new Member("teamA", 10));
        em.persist(new Member("teamB", 10));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @Test
    @DisplayName("Join On (연관관계가 있는 경우 - Join 대상 필터링)")
    void joinOn1() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        assertThat(result).hasSize(4);
        result.forEach(System.out::println);
    }

    @Test
    @DisplayName("Join On (연관관계가 없는 경우 - 막 조인의 조건)")
    void joinOn2() {
        em.persist(new Member("teamA", 100));
        em.persist(new Member("teamB", 100));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(team.name.eq(member.username))
                .fetch();

        long nullCount = result.stream()
                .map(tuple -> tuple.get(1, Team.class))
                .filter(Objects::isNull)
                .count();
        assertThat(result).hasSize(6);
        assertThat(nullCount).isEqualTo(4L);
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("fetch join 미적용")
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        /* 영속성 컨텍스트에 로드 되었는지 검증할 수 있는 방법 */
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(isLoaded).isFalse();
    }

    @Test
    @DisplayName("fetch join 적용")
    void fetchJoinUse() {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        /* 영속성 컨텍스트에 로드 되었는지 검증할 수 있는 방법 */
        boolean isLoaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(isLoaded).isTrue();
    }

    @Test
    @DisplayName("서브쿼리 - where 안에 사용")
    void subQueryWhere() {
        /* 나이가 가장 많은 사람을 얻어오는 쿼리 */
        QMember subMember = new QMember("subMember");

        Member result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(subMember.age.max())
                                .from(subMember)
                )).fetchOne();

        assertThat(result.getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("서브쿼리 - select 안에 사용")
    void subQuerySelect() {
        /* 프로젝션 결과 안에 나이 평균을 함께 얻어옴 */
        QMember subMember = new QMember("subMember");
        List<Tuple> result = queryFactory
                .select(member, JPAExpressions.select(subMember.age.avg()).from(subMember))
                .from(member)
                .fetch();

        result.forEach(tuple -> assertThat(tuple.get(1, Double.class)).isEqualTo(25.0));
    }

    @Test
    @DisplayName("간단한 case-when-then")
    void simpleCase() {
        List<String> result = queryFactory
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타"))
                .from(member)
                .fetch();

        List<String> expect = List.of("열살", "스무살", "기타", "기타");
        assertThat(result).isEqualTo(expect);
    }

    @Test
    @DisplayName("복잡한 case-when-then")
    void moreCase() {
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(10, 19)).then("10~19")
                                .when(member.age.between(20, 29)).then("20~29")
                                .when(member.age.between(30, 39)).then("30~39")
                                .when(member.age.between(40, 49)).then("40~49")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        List<String> expect = List.of("10~19", "20~29", "30~39", "40~49");
        assertThat(result).isEqualTo(expect);
    }

    public enum Enum {
        A, B;
    }

    @Test
    @DisplayName("상수")
    void constant() {
        Tuple result = queryFactory
                .select(member.username, Expressions.constant(Enum.A))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Enum a = result.get(1, Enum.class);
        assertThat(a).isEqualTo(Enum.A);
    }

    @Test
    @DisplayName("문자 더하기 - Enum 다룰 때 필요하게 될 stringValue")
    void concat() {
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(result).isEqualTo("member1_10");
    }

    @Test
    @DisplayName("Projection - 단일 컬럼")
    void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        assertThat(result).containsExactly("member1", "member2", "member3", "member4");
    }

    @Test
    @DisplayName("Projection - 튜플")
    void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        assertThat(result).hasSize(4);

        for (int index = 0; index < result.size(); index++) {
            Tuple tuple = result.get(index);
            assertThat(tuple.get(member.username)).isEqualTo(usernames.get(index));
            assertThat(tuple.get(member.age)).isEqualTo(ages.get(index));
        }
    }

    @Test
    @DisplayName("DTO Projection - 프로퍼티 접근 방식 (빈생성자 + Setter), setter 없으면 null 들어감 주의!")
    void DtoProjectionBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        assertThat(result).hasSize(4);

        for (int index = 0; index < result.size(); index++) {
            MemberDto dto = result.get(index);
            assertThat(dto.getUsername()).isEqualTo(usernames.get(index));
            assertThat(dto.getAge()).isEqualTo(ages.get(index));
        }
    }

    @Test
    @DisplayName("DTO Projection - 필드 접근 방식 (필드에 그냥 꽂아줌, 빈 생성자는 반드시 필요)")
    void DtoProjectionByFields() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        assertThat(result).hasSize(4);

        for (int index = 0; index < result.size(); index++) {
            MemberDto dto = result.get(index);
            assertThat(dto.getUsername()).isEqualTo(usernames.get(index));
            assertThat(dto.getAge()).isEqualTo(ages.get(index));
        }
    }

    @Test
    @DisplayName("DTO Projection - 생성자 접근 방식 (모든 필드를 파라미터로 갖는 생성자 호출)")
    void DtoProjectionByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        assertThat(result).hasSize(4);

        for (int index = 0; index < result.size(); index++) {
            MemberDto dto = result.get(index);
            assertThat(dto.getUsername()).isEqualTo(usernames.get(index));
            assertThat(dto.getAge()).isEqualTo(ages.get(index));
        }
    }

    @Test
    @DisplayName("DTO Projection - @QueryProjections 이용하는 방법 (아키텍처 설계 관점에서 논의 필요)")
    void DtoProjectionByQueryProjection() {
        /*
            컴파일 단계에서 에러를 작아주는 장점은 있지만, DTO 가 QueryDsl 에 의존하고 있다는 것과
            DTO 관련된 Q 파일을 만들어야 한다는 단점은 존재한다.
        */
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        assertThat(result).hasSize(4);

        for (int index = 0; index < result.size(); index++) {
            MemberDto dto = result.get(index);
            assertThat(dto.getUsername()).isEqualTo(usernames.get(index));
            assertThat(dto.getAge()).isEqualTo(ages.get(index));
        }
    }

    @ParameterizedTest
    @MethodSource("dynamicQueryArguments")
    @DisplayName("동적쿼리 - Boolean Builder 사용")
    void dynamicQueryByBooleanBuilder(String username, Integer age, int expectedSize) {
        BooleanBuilder builder = new BooleanBuilder();
        if (username != null) {
            builder.and(member.username.eq(username));
        }
        if (age != null) {
            builder.and(member.age.eq(age));
        }

        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();

        assertThat(result).hasSize(expectedSize);
    }

    static Stream<Arguments> dynamicQueryArguments() {
        return Stream.of(
                Arguments.arguments("member1", 10, 1),
                Arguments.arguments(null, 10, 1),
                Arguments.arguments("member1", null, 1),
                Arguments.arguments("member100", 10, 0),
                Arguments.arguments(null, null, 4)
        );
    }

    @ParameterizedTest
    @MethodSource("dynamicQueryArguments")
    @DisplayName("동적쿼리 - Where 다중 파라미터 사용")
    void dynamicQueryByParameters(String username, Integer age, int expectedSize) {
        /*
            where 안에 null 들어가면 해당 조건절은 없는 것이 된다.
            해당 방법을 사용하면 동적쿼리를 메서드화 해서 재사용도 가능하고, 여러 조건을 조립이 가능해진다.
        */
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(usernameEqual(username), ageEqual(age))
                .fetch();

        assertThat(result).hasSize(expectedSize);
    }

    private BooleanExpression usernameEqual(String username) {
        return username != null ? member.username.eq(username) : null;
    }
    private BooleanExpression ageEqual(Integer age) {
        return age != null ? member.age.eq(age) : null;
    }


}