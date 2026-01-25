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
     * 管理员更新用户信息
     * @param targetUserId 目标用户ID
     * @param nickname 昵称
     * @param email 邮箱
     * @param phone 手机号
     */
    void updateUserInfoByAdmin(Long targetUserId, String nickname, String email, String phone);
    
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
    
    /**
     * 管理员上传用户头像
     * @param targetUserId 目标用户ID
     * @param file 头像文件
     * @return 头像相对路径（如：avatars/1/avatar.jpg）
     */
    String uploadAvatarByAdmin(Long targetUserId, org.springframework.web.multipart.MultipartFile file);
    
    /**
     * 获取用户详情（存储使用、传输统计）
     * @param userId 用户ID
     * @return 用户详情数据
     */
    Map<String, Object> getUserDetail(Long userId);
    
    /**
     * 批量更新用户状态
     * @param userIds 用户ID列表
     * @param status 状态（1-启用，0-禁用）
     */
    void batchUpdateUserStatus(List<Long> userIds, Integer status);
    
    /**
     * 批量删除用户
     * @param userIds 用户ID列表
     */
    void batchDeleteUsers(List<Long> userIds);
    
    /**
     * 获取头像文件
     * @param avatarPath 头像相对路径（如：avatars/1/avatar.jpg）
     * @return ResponseEntity包含头像文件资源
     */
    org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getAvatar(String avatarPath);
    
    /**
     * 从HttpServletRequest获取头像文件
     * @param request HTTP请求
     * @return ResponseEntity包含头像文件资源
     */
    org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getAvatarFromRequest(javax.servlet.http.HttpServletRequest request);
}

