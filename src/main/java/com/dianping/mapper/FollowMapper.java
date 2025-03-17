package com.dianping.mapper;

import com.dianping.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author roy
 * 
 */
public interface FollowMapper extends BaseMapper<Follow> {
    /**
     * 删除关注数据
     * @param userId
     * @param followUserId
     */
    @Delete("delete from tb_follow where user_id = #{userId} and follow_user_id = #{followUserId}")
    Integer delete(Long userId, Long followUserId);
}
