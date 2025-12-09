package com.example.thinkfast.controller;

import com.example.thinkfast.common.aop.BaseResponse;
import com.example.thinkfast.common.aop.BaseResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@BaseResponseBody
@RestController
@RequestMapping("/test")
@Tag(name = "테스트", description = "API 서버 상태 확인용 테스트 API")
public class TestController {

    @Operation(summary = "서버 상태 확인", description = "API 서버가 정상적으로 동작하는지 확인합니다.")
    @GetMapping
    public BaseResponse<String> test() {
        return BaseResponse.success("성공");
    }
} 