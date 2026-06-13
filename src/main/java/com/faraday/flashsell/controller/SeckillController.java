package com.faraday.flashsell.controller;

import com.faraday.flashsell.common.response.Result;
import com.faraday.flashsell.model.dto.SeckillDTO;
import com.faraday.flashsell.model.vo.SeckillResultVO;
import com.faraday.flashsell.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 秒杀接口
     *
     * 前端调用：
     *   POST /api/seckill/execute
     *   Headers: Authorization: Bearer {token}
     *   Body: { "goodsId": 1 }
     *
     * 返回：
     *   成功 → { code: 200, data: { orderNumber: "xxx", status: 0 } }
     *   失败 → { code: 50004, message: "库存不足" }
     */
    @PostMapping("/execute")
    public Result<SeckillResultVO> execute(@RequestBody SeckillDTO dto, @RequestAttribute("userId") Long userId) {
        return seckillService.execute(dto, userId);
    }
}