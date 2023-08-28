package com.querydsl.repository;

import com.querydsl.dto.MemberSearchCondition;
import com.querydsl.dto.MemberTeamDto;
import com.querydsl.entity.Member;
import com.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {
    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void basicJpaTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> members = memberJpaRepository.findAll();
        assertThat(members).containsExactly(member);

        List<Member> membersByUsername = memberJpaRepository.findAllByUsername("member1");
        assertThat(membersByUsername).containsExactly(member);
    }

    @Test
    void QueryDslTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        List<Member> members = memberJpaRepository.findAllUsingQueryDsl();
        assertThat(members).containsExactly(member);

        List<Member> membersByUsername = memberJpaRepository.findAllByUsernameUsingQueryDsl("member1");
        assertThat(membersByUsername).containsExactly(member);
    }

    @ParameterizedTest
    @MethodSource("searchTestValues")
    void searchByBuilder(MemberSearchCondition condition, int expectedSize) {
        initDataSetting();

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertThat(result).hasSize(expectedSize);
    }

    private void initDataSetting() {
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

    static Stream<Arguments> searchTestValues() {
        return Stream.of(
                Arguments.arguments(
                        new MemberSearchCondition("member1", "teamA", 10, 20), 1
                ),
                Arguments.arguments(
                        new MemberSearchCondition(null, "teamA", 10, 20), 2
                ),
                Arguments.arguments(
                        new MemberSearchCondition("member1", null, 10, 20), 1
                ),
                Arguments.arguments(
                        new MemberSearchCondition(null, null, 10, 30), 3
                ),
                Arguments.arguments(
                        new MemberSearchCondition(null, "teamB", 10, 30), 1
                ),
                Arguments.arguments(
                        new MemberSearchCondition("memberX", "teamB", null, 30), 0
                )
        );
    }
}