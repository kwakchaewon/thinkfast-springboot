package com.example.thinkfast.controller;

import com.example.thinkfast.common.BaseResponse;
import com.example.thinkfast.common.BaseResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@BaseResponseBody
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public BaseResponse<String> test() {
        return BaseResponse.success("성공");
    }
} 