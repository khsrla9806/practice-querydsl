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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void basicJpaTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> members = memberRepository.findAll();
        assertThat(members).containsExactly(member);

        List<Member> membersByUsername = memberRepository.findAllByUsername("member1");
        assertThat(membersByUsername).containsExactly(member);
    }

    @ParameterizedTest
    @MethodSource("searchTestValues")
    void searchByWhereParameter(MemberSearchCondition condition, int expectedSize) {
        initDataSetting();

        List<MemberTeamDto> result = memberRepository.searchByWhereParameter(condition);
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

    @ParameterizedTest
    @MethodSource("pageTestValues")
    void searchSimpleTest(
            MemberSearchCondition condition, Pageable pageable, int expectedSize, List<String> expectedUsernames) {
        initDataSetting();

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition, pageable);
        assertThat(result.getContent()).hasSize(expectedSize);

        List<String> usernames = result.getContent().stream()
                .map(MemberTeamDto::getUsername)
                .collect(Collectors.toList());
        assertThat(usernames).isEqualTo(expectedUsernames);
    }

    @ParameterizedTest
    @MethodSource("pageTestValues")
    void searchComplexTest(
            MemberSearchCondition condition, Pageable pageable, int expectedSize, List<String> expectedUsernames) {
        initDataSetting();

        Page<MemberTeamDto> result = memberRepository.searchPageComplex(condition, pageable);
        assertThat(result.getContent()).hasSize(expectedSize);

        List<String> usernames = result.getContent().stream()
                .map(MemberTeamDto::getUsername)
                .collect(Collectors.toList());
        assertThat(usernames).isEqualTo(expectedUsernames);
    }

    static Stream<Arguments> pageTestValues() {
        return Stream.of(
                Arguments.arguments(
                        new MemberSearchCondition("member1", "teamA", 10, 20),
                        PageRequest.of(0, 1),
                        1,
                        List.of("member1")
                ),
                Arguments.arguments(
                        new MemberSearchCondition(null, "teamA", 10, 20),
                        PageRequest.of(0, 2),
                        2,
                        List.of("member1", "member2")
                ),
                Arguments.arguments(
                        new MemberSearchCondition(null, null, 10, 40),
                        PageRequest.of(1, 2),
                        2,
                        List.of("member3", "member4")
                ),
                Arguments.arguments(
                        new MemberSearchCondition(null, null, null, null),
                        PageRequest.of(0, 3),
                        3,
                        List.of("member1", "member2", "member3")
                )
        );
    }
}