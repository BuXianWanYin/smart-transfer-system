package com.server.smarttransferserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.server.smarttransferserver.dto.LoginDTO;
import com.server.smarttransferserver.dto.RegisterDTO;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.entity.User;
import com.server.smarttransferserver.mapper.FileInfoMapper;
import com.server.smarttransferserver.mapper.UserMapper;
import com.server.smarttransferserver.service.UserService;
import com.server.smarttransferserver.util.JwtUtil;
import com.server.smarttransferserver.vo.LoginVO;
import com.server.smarttransferserver.vo.UserInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;
    
    @Resource
    private FileInfoMapper fileInfoMapper;
    
    @Resource
    private JwtUtil jwtUtil;

    /**
     * 密码加盐
     */
    private static final String SALT = "smart-transfer-salt";

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证密码
        String encryptedPassword = encryptPassword(loginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 检查状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(new Date());
        userMapper.updateById(user);
        
        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        
        return loginVO;
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        // 验证两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        User existUser = userMapper.selectOne(wrapper);
        
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(encryptPassword(registerDTO.getPassword()));
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setStatus(1); // 启用
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        
        userMapper.insert(user);
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        String encryptedOldPassword = encryptPassword(oldPassword);
        if (!encryptedOldPassword.equals(user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 更新密码
        user.setPassword(encryptPassword(newPassword));
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserInfo(Long userId, String nickname, String email, String phone) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (email != null) {
            user.setEmail(email);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
    }

    @Override
    public List<UserInfoVO> getUserList() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        List<User> users = userMapper.selectList(wrapper);
        
        List<UserInfoVO> result = new ArrayList<>();
        for (User user : users) {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            result.add(vo);
        }
        return result;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(status);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        userMapper.deleteById(userId);
    }

    @Override
    public Map<String, Object> getStorageStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 查询用户所有文件
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileInfo::getUserId, userId);
        wrapper.eq(FileInfo::getDelFlag, 0);
        wrapper.eq(FileInfo::getIsDir, 0); // 只统计文件，不统计文件夹
        
        List<FileInfo> files = fileInfoMapper.selectList(wrapper);
        
        // 统计总存储空间
        long totalSize = 0;
        long imageSize = 0;
        long videoSize = 0;
        long audioSize = 0;
        long docSize = 0;
        long otherSize = 0;
        
        int fileCount = files.size();
        int imageCount = 0;
        int videoCount = 0;
        int audioCount = 0;
        int docCount = 0;
        int otherCount = 0;
        
        for (FileInfo file : files) {
            long size = file.getFileSize() != null ? file.getFileSize() : 0;
            totalSize += size;
            
            String ext = file.getExtendName();
            if (ext != null) {
                ext = ext.toLowerCase();
                if (isImage(ext)) {
                    imageSize += size;
                    imageCount++;
                } else if (isVideo(ext)) {
                    videoSize += size;
                    videoCount++;
                } else if (isAudio(ext)) {
                    audioSize += size;
                    audioCount++;
                } else if (isDocument(ext)) {
                    docSize += size;
                    docCount++;
                } else {
                    otherSize += size;
                    otherCount++;
                }
            } else {
                otherSize += size;
                otherCount++;
            }
        }
        
        stats.put("totalSize", totalSize);
        stats.put("fileCount", fileCount);
        stats.put("imageSize", imageSize);
        stats.put("imageCount", imageCount);
        stats.put("videoSize", videoSize);
        stats.put("videoCount", videoCount);
        stats.put("audioSize", audioSize);
        stats.put("audioCount", audioCount);
        stats.put("docSize", docSize);
        stats.put("docCount", docCount);
        stats.put("otherSize", otherSize);
        stats.put("otherCount", otherCount);
        
        return stats;
    }
    
    private boolean isImage(String ext) {
        return "jpg,jpeg,png,gif,bmp,webp,svg,ico".contains(ext);
    }
    
    private boolean isVideo(String ext) {
        return "mp4,avi,mov,mkv,wmv,flv,webm,m4v".contains(ext);
    }
    
    private boolean isAudio(String ext) {
        return "mp3,wav,flac,aac,ogg,wma,m4a".contains(ext);
    }
    
    private boolean isDocument(String ext) {
        return "doc,docx,xls,xlsx,ppt,pptx,pdf,txt,md".contains(ext);
    }

    /**
     * 加密密码（MD5 + 盐）
     */
    private String encryptPassword(String password) {
        String saltedPassword = SALT + password + SALT;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}

