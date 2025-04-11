package com.example.thinkfast.controller;

import com.example.thinkfast.common.ApiResponse;
import com.example.thinkfast.common.ApiResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiResponseBody
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ApiResponse<String> test() {
        return ApiResponse.success("성공");
    }
} 