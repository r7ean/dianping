package com.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.dto.ScrollResult;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Blog;
import com.dianping.entity.Follow;
import com.dianping.entity.User;
import com.dianping.mapper.BlogMapper;
import com.dianping.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.service.IFollowService;
import com.dianping.service.IUserService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dianping.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.dianping.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author roy
 * 
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Resource
    private IUserService userService;


    @Resource
    private IFollowService followService;
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean saveSuccess = save(blog);
        if(!saveSuccess){
            return Result.fail("发布笔记失败!");
        }
        List<Follow> followUsers = followService.query()
                .eq("follow_user_id", user.getId())
                .list();
        String key = FEED_KEY;
        for(Follow follows : followUsers){
            Long userId = follows.getUserId();
            
            stringRedisTemplate.opsForZSet()
                    .add(key + userId, blog.getId().toString(), System.currentTimeMillis());

        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogById(Integer id) {
        Blog blog = getById(id);
        if(blog == null){
            return Result.fail("笔记已不存在哦!");
        }
        queryBlogUser(blog);
        setBolgLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach((blog)->{
            this.queryBlogUser(blog);
            this.setBolgLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        UserDTO user = UserHolder.getUser();
        if(user == null){
            return Result.fail("请先登录!");
        }
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + id;
        Blog blog = getById(id);

        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score == null){
            // 修改点赞数量
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if(BooleanUtil.isTrue(isSuccess)){
                blog.setIsLike(true);
                stringRedisTemplate.opsForZSet().add(key, userId.toString(),System.currentTimeMillis());
            }
        }else{
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if(BooleanUtil.isTrue(isSuccess)){
                blog.setIsLike(false);
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Integer id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> likedUsers = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(likedUsers == null || likedUsers.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> usersId = likedUsers
                .stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        String idsStr = StrUtil.join(",", usersId);

        List<UserDTO> users = userService.query()
                .in("id", usersId).last("order by  field(id," + idsStr + ")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

    @Override
    public Result queryBlogByUserId(Integer current, Long id) {
        Page<Blog> page = query()
                .eq("user_id", id)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
    /**
     * 查看关注的人的笔记
     * @param max 
     * @param offset 偏移量
     * @return
     */
    @Override
    public Result queryBlogFollows(Long max, Integer offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 查询收件箱  ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        // 3. 解析数据 blogId minTime offset
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
        int os = 1;
        Long minStamp = 0L;
        List<Long> blogIds  = new ArrayList<>(typedTuples.size());
        for(ZSetOperations.TypedTuple<String> tuple :typedTuples){
            // 获取笔记id 存入集合
            String idStr = tuple.getValue();
            blogIds.add(Long.valueOf(idStr));
            // 获取最小时间戳
            long timeStamp = tuple.getScore().longValue();
            if(timeStamp == minStamp){
                os++;
            }else {
                os = 1;
                minStamp = timeStamp;
            }
        }
        // 4. 根据id查询blog
        String strBlogIds = StrUtil.join(",", blogIds);
        List<Blog> blogs = query()
                .in("id", blogIds)
                .last("order by field(id, " + strBlogIds + ")")
                .list();

        blogs = blogs.stream().map(blog -> {
            queryBlogUser(blog);
            setBolgLiked(blog);
            return blog;
        }).collect(Collectors.toList());

        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minStamp);
        // 5. 封装返回
        return Result.ok(scrollResult);
    }

    private void queryBlogUser(Blog blog){
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 设置登录用户点赞笔记
     * @param blog
     */
    private void setBolgLiked(Blog blog){

        UserDTO user = UserHolder.getUser();
        if(user == null){
            return;
        }
        Long userId = user.getId();

        String key = BLOG_LIKED_KEY + blog.getId();

        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }
}
