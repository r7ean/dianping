package com.dianping.controller;


import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.UserInfo;
import com.dianping.service.IUserInfoService;
import com.dianping.service.IUserService;
import com.dianping.utils.RegexUtils;
import com.dianping.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author roy
 * 
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "发送验证码、登录、登出、用户信息、签到等功能")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    @Operation(summary = "发送验证码")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpServletRequest request){
        return userService.login(loginForm, request);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result logout(HttpServletRequest request){
        return userService.logout(request);
    }

    @GetMapping("/me")
    @Operation(summary = "查询登录用户信息")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户信息")
    public Result getUserById(@PathVariable("id") Integer id){
        return userService.getUserById(id);
    }
    
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    /**
     * 用户签到
     * @return
     */
    @PostMapping("/sign")
    @Operation(summary = "用户签到")
    public Result sign(){
        return userService.sign();
    }

    /**
     * 用户当月签到统计
     * @return
     */
    @GetMapping("/sign/count")
    @Operation(summary = "统计用户签到次数")
    public Result signCount(){
        return userService.signCount();
    }
}
