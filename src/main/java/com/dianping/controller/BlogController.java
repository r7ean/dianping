package com.dianping.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.Blog;
import com.dianping.entity.User;
import com.dianping.service.IBlogService;
import com.dianping.service.IUserService;
import com.dianping.utils.SystemConstants;
import com.dianping.utils.UserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author roy
 * 
 */
@RestController
@RequestMapping("/blog")
@Tag(name = "博客管理", description = "新增、查询、点赞、查询点赞列表、查询自己博客、查询他人博客、热门博客、查看关注的人博客")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 新增博客
     * @param blog 博客实体
     * @return 博客ID
     */
    @PostMapping
    @Operation(summary = "新增博客")
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 查看blog
     * @param id 查询博客ID
     * @return 博客
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询博客")
    public Result queryBlogById(@PathVariable("id") Integer id){
        return blogService.queryBlogById(id);
    }

    /**
     * 点赞博客
     * @param id 点赞博客ID
     */
    @PutMapping("/like/{id}")
    @Operation(summary = "点赞博客")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    /**
     * 查询点赞笔记用户列表
     * @param id 博客ID
     * @return 点赞过的用户
     */
    @GetMapping("/likes/{id}")
    @Operation(summary = "查询点赞笔记用户列表")
    public Result queryBlogLikes(@PathVariable("id") Integer id){
        return blogService.queryBlogLikes(id);
    }

    /**
     * 查询自己的博客
     * @param current 当前页
     * @return 当前页博客记录
     */
    @GetMapping("/of/me")
    @Operation(summary = "分页查询自己的博客")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 分页查询他人博客列表
     * @param current 当前也
     * @param id 目标用户ID
     * @return 当前也记录
     */
    @GetMapping("/of/user")
    @Operation(summary = "分页查询他人博客列表")
    public Result queryBlogByUserId(@RequestParam(value = "current", defaultValue = "1") Integer current,
                                    @RequestParam("id") Long id){
        return blogService.queryBlogByUserId(current, id);
    }

    /**
     * 分页查询热门博客
     * @param current 当前也
     * @return 当前页记录
     */
    @GetMapping("/hot")
    @Operation(summary = "分页查询热门博客")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    /**
     * 查看关注的人的笔记
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    @Operation(summary = "查看关注的人的笔记")
    public Result queryBlogFollows(@RequestParam("lastId") Long max,
                                   @RequestParam(value = "offset", defaultValue = "0") Integer offset){
        return blogService.queryBlogFollows(max,offset);
    }
}
