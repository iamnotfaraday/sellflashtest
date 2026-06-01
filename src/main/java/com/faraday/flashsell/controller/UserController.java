package com.faraday.flashsell.controller;

import com.faraday.flashsell.common.response.Result;
import com.faraday.flashsell.model.dto.LoginDTO;
import com.faraday.flashsell.model.vo.LoginVO;
import com.faraday.flashsell.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return userService.login(dto);
    }
}
