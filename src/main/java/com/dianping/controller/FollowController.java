package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.service.IFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户关注管理", description = "关注或取关、查看是否关注、查看共同关注")
public class FollowController {
    @Resource
    private IFollowService followService;

    /**
     * 关注或取关用户
     * @param followUserId 目标用户ID
     * @param isFollow 是否关注
     */
    @PutMapping("/{id}/{isFollow}")
    @Operation(summary = "关注或取关用户")
    public Result follow(@PathVariable("id") Long followUserId,
                         @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    /**
     * 查看是否关注用户
     * @param followUserId 目标用户ID
     */
    @GetMapping("/or/not/{id}")
    @Operation(summary = "查看是否关注用户")
    public Result  isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }


    /**
     * 查看共同关注
     * @param id 目标用户ID
     * @return 共同关注用户列表
     */
    @GetMapping("/common/{id}")
    @Operation(summary = "查看共同关注")
    public Result commonFollows(@PathVariable("id") Long id){
        return followService.commonFollows(id);
    }
    
}
