package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.dto.LoginFormDTO;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.User;
import com.dianping.mapper.UserMapper;
import com.dianping.service.IUserService;
import com.dianping.utils.RegexUtils;
import com.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dianping.utils.RedisConstants.*;
import static com.dianping.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author roy
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号校验失败，请检查手机号是否正确!");
        }
        String code = RandomUtil.randomNumbers(4);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("发送手机验证码成功: {}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpServletRequest request) {
        // 1. 校验手机号和验证码
        if (loginForm.getPhone() == null || RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            // 1.1 如果不符合 返回错误信息
            return Result.fail("手机号格式不符合，请输入正确格式！");
        }
        // String code = (String) session.getAttribute("code");
        // 2. 从redis获取code
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());

        if (loginForm.getCode() == null || !loginForm.getCode().equals(code)) {
            // 2.1 不一致 报错
            return Result.fail("验证码无效,请重试！");
        }

        // 3. 如果验证码一致 根据手机号查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();

        // 4. 判断用户是否存在
        if (user == null) {
            // 4.1 如果不存在创建新用户
            user = new User();
            user.setCreateTime(LocalDateTime.now());
            user.setPhone(loginForm.getPhone());
            user.setUpdateTime(LocalDateTime.now());
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            save(user);
        }
        // 如果没有在本地登录 就进行以下操作
        // 5. 如果存在保存用户到redis 登录成功
        // 5.1  将user对象封装成userDTO对象 保证数据的安全性
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 5.2 将UserDTO转换成map 这里UserDTO中的id是Long类型 会发生类型转换错误
        // 5.2.1 第一种方法可以new一个Map 自己转换  第二种方式如下
        Map<String, Object> map = BeanUtil.beanToMap(
                userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // 5.3 如果用户已经在本地进行登录了 就不需要再次保存到redis 而是刷新token
        String token = request.getHeader("authorization");
        if (token != null && UserHolder.getUser().getId().equals(userDTO.getId())) {
            // 如果token存在 说明已经在redis中有缓存了
            String tokenKey = LOGIN_USER_KEY + token;
            // 如果redis缓存已经失效那么重新写入
            if (!stringRedisTemplate.hasKey(tokenKey)) {
                stringRedisTemplate.opsForHash().putAll(tokenKey, map);
            }
            // 如果未失效则更新有效期
            stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.SECONDS);
            return Result.ok(token);
        }
        // 5.4 如果用户之前未在本地登录 则创建token，将登录信息存入redis
        token = UUID.randomUUID(true).toString();
        // 5.3 设置tokenKey 存入redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, map);
        // 5.4 设置登录有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.SECONDS);
        return Result.ok(token);
    }

    @Override
    public Result logout(HttpServletRequest request) {
        // 1. 先获取登录用户的token
        String token = request.getHeader("authorization");
        String tokenKey = LOGIN_USER_KEY + token;
        // 2. 如果本地登录之后 redis未过期且浏览器token未过期 则删除token和redis信息
        // 2.1 清除token信息
        if(StrUtil.isNotBlank(token)){
            token = "";
        }
        // 2.2 清除redis缓存
        Boolean hasKey = stringRedisTemplate.hasKey(tokenKey);
        if(BooleanUtil.isTrue(hasKey)){
            stringRedisTemplate.delete(tokenKey);
        }
        return Result.ok(token);
    }

    @Override
    public Result getUserById(Integer id) {
        User user = getById(id);
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }


}
