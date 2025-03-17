package com.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.entity.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author roy
 * 
 */
public interface IUserService extends IService<User> {

    /**
     * 发送手机验证码
     * @param phone 手机号
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    Result login(LoginFormDTO loginForm, HttpServletRequest request);

    /**
     * 登出功能
     * @param request
     * @return
     */
    Result logout(HttpServletRequest request);

    Result getUserById(Integer id);

    Result sign();

    Result signCount();
}
