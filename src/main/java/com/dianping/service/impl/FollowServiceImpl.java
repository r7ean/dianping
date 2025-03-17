package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Follow;
import com.dianping.mapper.FollowMapper;
import com.dianping.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.service.IUserService;
import com.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.FOLLOWS_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author roy
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;
    
    /**
     * 关注或取关用户
     * @param followUserId 关注用户ID
     * @param isFollow 是否已经关注
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOWS_KEY + userId;
        if(BooleanUtil.isTrue(isFollow)){
            // 关注用户 向follow表插入数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean saveSuccess = save(follow);
            if(saveSuccess){
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        }else {
            // 取关用户 删除follow表中数据
            Integer delete = baseMapper.delete(userId, followUserId);
            if(delete != null && delete > 0){
                stringRedisTemplate.opsForSet().remove(key + userId, followUserId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query()
                .eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count();
        return Result.ok(count > 0);
    }

    /**
     * 查看共同关注
     * @param id 目标用户ID
     */
    @Override
    public Result commonFollows(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key = FOLLOWS_KEY + userId;
        String key2 = FOLLOWS_KEY + id;
        Set<String> commonList = stringRedisTemplate.opsForSet().intersect(key2, key);

        if(commonList == null || commonList.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> usersId = commonList.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        List<UserDTO> users = userService
                .listByIds(usersId)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        
        return Result.ok(users);
    }
}
