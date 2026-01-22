package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.LoginDTO;
import com.server.smarttransferserver.dto.RegisterDTO;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.util.UserContextHolder;
import com.server.smarttransferserver.vo.LoginVO;
import com.server.smarttransferserver.vo.UserInfoVO;
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
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        Long userId = UserContextHolder.getUserId();
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
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
    public Result<Void> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Integer> params) {
        try {
            Integer status = params.get("status");
            userService.updateUserStatus(userId, status);
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
    public Result<Void> batchUpdateUserStatus(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) params.get("userIds");
            Integer status = (Integer) params.get("status");
            
            userService.batchUpdateUserStatus(userIds, status);
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
    public Result<Void> batchDeleteUsers(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> userIds = (List<Long>) params.get("userIds");
            
            userService.batchDeleteUsers(userIds);
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
    public ResponseEntity<Resource> getAvatar(HttpServletRequest request) {
        try {
            // 获取请求路径，例如：/user/avatar/avatars/1/avatar_xxx.jpg
            String requestURI = request.getRequestURI();
            
            // 提取 /avatar/ 之后的部分
            int avatarIndex = requestURI.indexOf("/avatar/");
            if (avatarIndex == -1) {
                return ResponseEntity.notFound().build();
            }
            
            String avatarPathFromRequest = requestURI.substring(avatarIndex + "/avatar/".length());
            return userService.getAvatar(avatarPathFromRequest);
        } catch (Exception e) {
            log.error("获取头像失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

