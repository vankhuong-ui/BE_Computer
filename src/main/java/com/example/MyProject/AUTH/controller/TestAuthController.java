package com.example.MyProject.AUTH.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/online")
public class TestAuthController {

    @GetMapping
    public String online() {
        return "OK";
    }
}
