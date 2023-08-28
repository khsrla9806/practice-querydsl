package com.querydsl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private Integer age;
    private Long teamId;
    private String teamName;
}


