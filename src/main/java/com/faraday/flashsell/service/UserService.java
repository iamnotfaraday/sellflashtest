package com.faraday.flashsell.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faraday.flashsell.common.response.Result;
import com.faraday.flashsell.common.response.ResultCode;
import com.faraday.flashsell.common.utils.JwtUtils;
import com.faraday.flashsell.common.utils.MD5Utils;
import com.faraday.flashsell.dao.UserMapper;
import com.faraday.flashsell.model.dto.LoginDTO;
import com.faraday.flashsell.model.entity.User;
import com.faraday.flashsell.model.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    public Result<LoginVO> login(LoginDTO dto) {
        // 查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())
        );

        // 用户不存在
        if (user == null) {
            return Result.fail(ResultCode.LOGIN_FAIL);
        }

        // 验密码
        String inputMd5 = MD5Utils.md5(dto.getPassword());
        if (!inputMd5.equals(user.getPassword())) {
            return Result.fail(ResultCode.LOGIN_FAIL);
        }

        // 签发 JWT
        String token = jwtUtils.generate(user.getId(), user.getPhone());

        LoginVO vo = new LoginVO(token, user.getNickname(), user.getPhone());
        return Result.success(vo);
    }
}
