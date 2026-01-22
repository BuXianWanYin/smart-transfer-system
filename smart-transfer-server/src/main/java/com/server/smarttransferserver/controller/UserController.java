package com.server.smarttransferserver.controller;

import com.server.smarttransferserver.annotation.RequireAdmin;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.LoginDTO;
import com.server.smarttransferserver.dto.RegisterDTO;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.util.UserContextHolder;
import com.server.smarttransferserver.vo.LoginVO;
import com.server.smarttransferserver.vo.UserInfoVO;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            if (status == null) {
                return Result.error("状态参数不能为空");
            }
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
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            String avatarPath = userService.uploadAvatar(userId, file);
            return Result.success(avatarPath);
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
            
            String avatarPath = requestURI.substring(avatarIndex + "/avatar/".length());
            
            // 验证路径格式：avatars/userId/filename
            if (avatarPath == null || avatarPath.isEmpty() || !avatarPath.startsWith("avatars/")) {
                return ResponseEntity.notFound().build();
            }
            
            // 构建完整文件路径
            // avatarPath格式：avatars/userId/filename
            // 需要去掉"avatars/"前缀，因为avatarPath配置已经包含了avatars目录
            String relativePath = avatarPath.substring("avatars/".length());
            String userDir = System.getProperty("user.dir");
            Path avatarFilePath = Paths.get(userDir, "uploads", "avatars", relativePath);
            File avatarFile = avatarFilePath.toFile();
            
            if (!avatarFile.exists() || !avatarFile.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // 根据文件扩展名确定Content-Type
            String fileName = avatarFile.getName().toLowerCase();
            MediaType mediaType = MediaType.IMAGE_JPEG; // 默认
            if (fileName.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (fileName.endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            }
            
            Resource resource = new FileSystemResource(avatarFile);
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // 缓存1年
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

