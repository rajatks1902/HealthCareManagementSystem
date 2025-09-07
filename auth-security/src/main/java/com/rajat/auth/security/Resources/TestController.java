package com.rajat.auth.security.Resources;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rajat")
public class TestController {


    @GetMapping
    public String testApi(){
        return "API is CALLED Successfully";
    }
}
