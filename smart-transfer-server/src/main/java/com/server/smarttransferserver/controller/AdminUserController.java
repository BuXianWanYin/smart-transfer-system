package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.*;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理员用户管理控制器
 * 包含所有需要管理员权限的用户管理接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
@RequireAdmin
public class AdminUserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public Result<List<UserInfoVO>> getUserList() {
        try {
            List<UserInfoVO> userList = userService.getUserList();
            return Result.success(userList);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/status/{userId}")
    public Result<Void> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusDTO dto) {
        try {
            userService.updateUserStatus(userId, dto.getStatus());
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新用户状态失败 - 用户ID: {}", userId, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除用户失败 - 用户ID: {}", userId, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}/info")
    public Result<Void> updateUserInfo(@PathVariable Long userId, @Valid @RequestBody UpdateUserInfoDTO dto) {
        try {
            userService.updateUserInfoByAdmin(userId, dto.getNickname(), dto.getEmail(), dto.getPhone());
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新用户信息失败 - 用户ID: {}", userId, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/{userId}/avatar")
    public Result<String> uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            log.info("管理员上传用户头像 - 目标用户ID: {}, 文件: {}, 大小: {}字节",
                    userId, file.getOriginalFilename(), file.getSize());
            String avatarPath = userService.uploadAvatarByAdmin(userId, file);
            log.info("上传头像成功 - 用户ID: {}, 路径: {}", userId, avatarPath);
            return Result.success(avatarPath);
        } catch (Exception e) {
            log.error("上传头像失败 - 用户ID: {}", userId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "头像上传失败");
        }
    }

    /**
     * 获取系统级存储统计
     */
    @GetMapping("/system-storage")
    public Result<Map<String, Object>> getSystemStorageStats() {
        try {
            Map<String, Object> stats = userService.getSystemStorageStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取系统存储统计失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户详情（存储使用、传输统计）
     */
    @GetMapping("/detail/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        try {
            Map<String, Object> detail = userService.getUserDetail(userId);
            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取用户详情失败 - 用户ID: {}", userId, e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量更新用户状态
     */
    @PutMapping("/batch-status")
    public Result<Void> batchUpdateUserStatus(@Valid @RequestBody BatchUpdateUserStatusDTO dto) {
        try {
            userService.batchUpdateUserStatus(dto.getUserIds(), dto.getStatus());
            return Result.success(null);
        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteUsers(@Valid @RequestBody BatchDeleteUsersDTO dto) {
        try {
            userService.batchDeleteUsers(dto.getUserIds());
            return Result.success(null);
        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }
}
