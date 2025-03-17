package com.dianping.service;

import com.dianping.dto.Result;
import com.dianping.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author roy
 * 
 */
public interface IBlogService extends IService<Blog> {

    Result saveBlog(Blog blog);

    Result queryBlogById(Integer id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Integer id);

    Result queryBlogByUserId(Integer current, Long id);

    Result queryBlogFollows(Long max, Integer offset);
}
