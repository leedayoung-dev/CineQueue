
package com.cinequeue.backend.user.dto;

import com.cinequeue.backend.user.entity.AgeGroup;
import com.cinequeue.backend.user.entity.PreferredSeat;
import com.cinequeue.backend.user.entity.Region;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotNull(message = "지역을 선택해 주세요.")
        Region region,

        @NotBlank(message = "시·군·구를 선택해 주세요.")
        @Size(max = 30, message = "시·군·구는 30자 이하여야 합니다.")
        String district,

        @NotNull(message = "선호 좌석을 선택해 주세요.")
        PreferredSeat preferredSeat,

        @NotNull(message = "나이대를 선택해 주세요.")
        AgeGroup ageGroup

) {
}