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
     * 获取用户列表（管理员）
     */
    @RequireAdmin
    @GetMapping("/list")
    public Result<List<UserInfoVO>> getUserList() {
        try {
            List<UserInfoVO> userList = userService.getUserList();
            return Result.success(userList);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 更新用户状态（管理员）
     */
    @RequireAdmin
    @PutMapping("/status/{userId}")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusDTO dto) {
        try {
            userService.updateUserStatus(userId, dto.getStatus());
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除用户（管理员）
     */
    @RequireAdmin
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取系统级存储统计（管理员）
     */
    @RequireAdmin
    @GetMapping("/system-storage")
    public Result<Map<String, Object>> getSystemStorageStats() {
        try {
            Map<String, Object> stats = userService.getSystemStorageStats();
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
        try {
            String avatarPath = userService.uploadAvatar(userId, file);
            return Result.success(avatarPath);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取用户详情（存储使用、传输统计）（管理员）
     */
    @RequireAdmin
    @GetMapping("/detail/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        try {
            Map<String, Object> detail = userService.getUserDetail(userId);
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量更新用户状态（管理员）
     */
    @RequireAdmin
    @PutMapping("/batch-status")
    public Result<Void> batchUpdateUserStatus(@Valid @RequestBody BatchUpdateUserStatusDTO dto) {
        try {
            userService.batchUpdateUserStatus(dto.getUserIds(), dto.getStatus());
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量删除用户（管理员）
     */
    @RequireAdmin
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteUsers(@Valid @RequestBody BatchDeleteUsersDTO dto) {
        try {
            userService.batchDeleteUsers(dto.getUserIds());
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
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

