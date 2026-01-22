package com.server.smarttransferserver.service;

import com.server.smarttransferserver.dto.LoginDTO;
import com.server.smarttransferserver.dto.RegisterDTO;
import com.server.smarttransferserver.vo.LoginVO;
import com.server.smarttransferserver.vo.UserInfoVO;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);
    
    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);
    
    /**
     * 获取用户信息
     */
    UserInfoVO getUserInfo(Long userId);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 更新用户信息
     */
    void updateUserInfo(Long userId, String nickname, String email, String phone);
    
    /**
     * 获取用户列表（管理员）
     */
    List<UserInfoVO> getUserList();
    
    /**
     * 更新用户状态（管理员）
     */
    void updateUserStatus(Long userId, Integer status);
    
    /**
     * 删除用户（管理员）
     */
    void deleteUser(Long userId);
    
    /**
     * 获取用户存储统计
     */
    Map<String, Object> getStorageStats(Long userId);
    
    /**
     * 获取系统级存储统计（管理员）
     * 统计所有用户的文件存储情况
     */
    Map<String, Object> getSystemStorageStats();
    
    /**
     * 上传头像
     * @param userId 用户ID
     * @param file 头像文件
     * @return 头像相对路径（如：avatars/1/avatar.jpg）
     */
    String uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);
}

