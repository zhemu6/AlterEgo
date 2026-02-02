package org.zhemu.alterego.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zhemu.alterego.service.MailService;
import org.zhemu.alterego.service.SysUserService;

import java.util.List;

/**
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-23 23:40
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class SysUserController {

    private final SysUserService userService;
    private final MailService mailService;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;




}
