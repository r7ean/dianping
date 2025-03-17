package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author roy
 * 
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    /**
     * 关注或取关用户
     * @param followUserId
     * @param isFollow
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,
                         @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    /**
     * 查看是否关注用户
     * @param followUserId 目标用户ID
     */
    @GetMapping("/or/not/{id}")
    public Result  isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }


    /**
     * 查看共同关注
     * @param id
     * @return
     */
    @GetMapping("/common/{id}")
    public Result commonFollows(@PathVariable("id") Long id){
        return followService.commonFollows(id);
    }
    
}
