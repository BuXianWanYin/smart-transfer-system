package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.LoginDTO;
import com.server.smarttransferserver.dto.RegisterDTO;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.util.UserContextHolder;
import com.server.smarttransferserver.vo.LoginVO;
import com.server.smarttransferserver.vo.UserInfoVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            LoginVO loginVO = userService.login(loginDTO);
            return Result.success(loginVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            userService.register(registerDTO);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            UserInfoVO userInfo = userService.getUserInfo(userId);
            return Result.success(userInfo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        
        try {
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody Map<String, String> params) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            userService.updateUserInfo(
                userId,
                params.get("nickname"),
                params.get("email"),
                params.get("phone")
            );
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证 Token 是否有效
     */
    @GetMapping("/check-token")
    public Result<Boolean> checkToken() {
        Long userId = UserContextHolder.getUserId();
        return Result.success(userId != null);
    }

    /**
     * 获取用户存储统计
     */
    @GetMapping("/storage")
    public Result<Map<String, Object>> getStorageStats() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            Map<String, Object> stats = userService.getStorageStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

