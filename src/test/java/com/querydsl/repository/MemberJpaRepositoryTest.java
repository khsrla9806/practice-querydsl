package com.querydsl.repository;

import com.querydsl.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

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
}