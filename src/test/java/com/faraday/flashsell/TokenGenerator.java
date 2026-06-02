package com.faraday.flashsell;

import com.faraday.flashsell.model.entity.User;
import com.faraday.flashsell.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.faraday.flashsell.common.utils.JwtUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootTest
class TokenGenerator {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtil;

    @Test
    void generateTokens() throws IOException {
        // 1. 查出所有用户
        List<User> users = userService.list();
        System.out.println("查到用户数: " + users.size());

        // 2. 生成 token，写两个文件
        StringBuilder tokenOnly = new StringBuilder();       // 只有 token，每行一个（给 JMeter 用）
        StringBuilder tokenWithUser = new StringBuilder();   // userId,phone,token（方便对照）

        int count = 0;
        for (User user : users) {
            String token = jwtUtil.generate(user.getId(), user.getPhone());

            tokenOnly.append(token).append("\r\n");
            tokenWithUser.append(user.getId())
                    .append(",")
                    .append(user.getPhone())
                    .append(",")
                    .append(token)
                    .append("\r\n");

            // 前 3 条打印到控制台，方便确认每个用户的 token 不同
            if (count < 3) {
                System.out.println("[" + count + "] userId=" + user.getId()
                        + " phone=" + user.getPhone()
                        + " token前20位=" + token.substring(0, Math.min(20, token.length())) + "...");
            }
            count++;
        }

        // 验证：解析第 1 个和最后 1 个 token，确认 userId 不同
        if (!users.isEmpty()) {
            User first = users.get(0);
            User last = users.get(users.size() - 1);
            String firstToken = jwtUtil.generate(first.getId(), first.getPhone());
            String lastToken = jwtUtil.generate(last.getId(), last.getPhone());
            System.out.println("\n--- 验证解析 ---");
            System.out.println("第1个token解析出userId = " + jwtUtil.getUserId(firstToken));
            System.out.println("最后1个token解析出userId = " + jwtUtil.getUserId(lastToken));
            System.out.println("两个userId" + (first.getId().equals(last.getId()) ? " 相同 ⚠️" : " 不同 ✓"));
        }

        // 3. 写出文件
        Files.write(Paths.get("E:/tokens.txt"), tokenOnly.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get("E:/tokens_with_user.csv"), tokenWithUser.toString().getBytes(StandardCharsets.UTF_8));

        System.out.println("\n====== " + count + " 个 Token 已写入 ======");
        System.out.println("  E:/tokens.txt            — 仅 token，供 JMeter CSV读取");
        System.out.println("  E:/tokens_with_user.csv  — userId,phone,token 对照表");
    }
}
