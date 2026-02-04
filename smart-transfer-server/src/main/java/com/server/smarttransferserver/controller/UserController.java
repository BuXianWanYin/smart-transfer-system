package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.*;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.util.UserContextHolder;
import com.server.smarttransferserver.vo.LoginVO;
import com.server.smarttransferserver.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
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
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = UserContextHolder.getUserId();
        try {
            userService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoDTO dto) {
        Long userId = UserContextHolder.getUserId();
        try {
            userService.updateUserInfo(userId, dto.getNickname(), dto.getEmail(), dto.getPhone());
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
        try {
            Map<String, Object> stats = userService.getStorageStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            log.error("上传头像失败：用户未登录");
            return Result.error("请先登录");
        }
        
        try {
            log.info("开始上传头像 - 用户ID: {}, 文件名: {}, 文件大小: {}字节", 
                    userId, file.getOriginalFilename(), file.getSize());
            String avatarPath = userService.uploadAvatar(userId, file);
            log.info("头像上传成功 - 用户ID: {}, 头像路径: {}", userId, avatarPath);
            return Result.success(avatarPath);
        } catch (Exception e) {
            log.error("上传头像失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "头像上传失败，请稍后重试";
            }
            return Result.error(errorMessage);
        }
    }
    
    /**
     * 获取头像（静态资源访问）
     * 路径格式：/user/avatar/avatars/userId/filename
     */
    @GetMapping("/avatar/**")
    public ResponseEntity<org.springframework.core.io.Resource> getAvatar(HttpServletRequest request) {
        try {
            return userService.getAvatarFromRequest(request);
        } catch (Exception e) {
            log.error("获取头像失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

