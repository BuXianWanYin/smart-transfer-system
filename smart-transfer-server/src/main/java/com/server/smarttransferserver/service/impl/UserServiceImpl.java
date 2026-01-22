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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     * 头像存储路径
     */
    @Value("${transfer.avatar-path:./uploads/avatars}")
    private String avatarPath;

    /**
     * 密码加盐
     */
    private static final String SALT = "smart-transfer-salt";
    
    /**
     * 初始化头像存储目录
     */
    @PostConstruct
    public void initAvatarPath() {
        try {
            // 获取项目根目录
            String userDir = System.getProperty("user.dir");
            
            // 转换为绝对路径
            if (avatarPath.startsWith("./") || avatarPath.startsWith(".\\")) {
                avatarPath = Paths.get(userDir, avatarPath.substring(2)).toString();
            } else if (!Paths.get(avatarPath).isAbsolute()) {
                avatarPath = Paths.get(userDir, avatarPath).toString();
            }
            
            // 创建目录
            Files.createDirectories(Paths.get(avatarPath));
        } catch (IOException e) {
            throw new RuntimeException("创建头像存储目录失败", e);
        }
    }

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
        loginVO.setRole(user.getRole() != null ? user.getRole() : "USER");
        
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
        user.setRole("USER"); // 默认普通用户
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
        // 确保role字段被正确设置
        if (userInfoVO.getRole() == null) {
            userInfoVO.setRole(user.getRole() != null ? user.getRole() : "USER");
        }
        return userInfoVO;
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证新密码长度
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度必须在6-20个字符之间");
        }
        
        // 验证新密码不能和旧密码相同
        String encryptedOldPassword = encryptPassword(oldPassword);
        String encryptedNewPassword = encryptPassword(newPassword);
        if (encryptedNewPassword.equals(user.getPassword())) {
            throw new RuntimeException("新密码不能和原密码相同");
        }
        
        // 验证旧密码
        if (!encryptedOldPassword.equals(user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        
        // 更新密码
        user.setPassword(encryptedNewPassword);
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
        if (email != null && !email.isEmpty()) {
            // 验证邮箱格式
            if (!isValidEmail(email)) {
                throw new RuntimeException("邮箱格式不正确");
            }
            user.setEmail(email);
        }
        if (phone != null && !phone.isEmpty()) {
            // 验证手机号格式（中国手机号：11位数字，以1开头）
            if (!isValidPhone(phone)) {
                throw new RuntimeException("手机号格式不正确，请输入11位数字且以1开头");
            }
            user.setPhone(phone);
        }
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
    }
    
    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // 简单的邮箱格式验证：包含@和.，且@前后都有字符
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }
    
    /**
     * 验证手机号格式（中国手机号：11位数字，以1开头）
     */
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        // 中国手机号格式：11位数字，以1开头
        String phonePattern = "^1[3-9]\\d{9}$";
        return phone.matches(phonePattern);
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
        
        // 如果要禁用管理员，检查是否还有其他的启用状态的管理员
        if ("ADMIN".equals(user.getRole()) && status == 0) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getRole, "ADMIN");
            wrapper.eq(User::getStatus, 1);
            wrapper.ne(User::getId, userId); // 排除当前用户
            long adminCount = userMapper.selectCount(wrapper);
            if (adminCount == 0) {
                throw new RuntimeException("不能禁用最后一个管理员");
            }
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
        
        // 不能删除管理员
        if ("ADMIN".equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员用户");
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
    
    @Override
    public Map<String, Object> getSystemStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 查询所有用户的文件（未删除）
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
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

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("头像文件不能为空");
        }
        
        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        
        // 提取文件扩展名
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            throw new RuntimeException("文件名必须包含有效的扩展名");
        }
        
        String ext = originalFilename.substring(lastDotIndex + 1).toLowerCase();
        if (!"jpg".equals(ext) && !"jpeg".equals(ext) && !"png".equals(ext) && !"gif".equals(ext)) {
            throw new RuntimeException("头像只支持 jpg、jpeg、png、gif 格式");
        }
        
        // 验证文件大小（最大5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("头像大小不能超过5MB");
        }
        
        try {
            // 创建用户头像目录
            Path userAvatarDir = Paths.get(avatarPath, userId.toString());
            Files.createDirectories(userAvatarDir);
            
            // 生成唯一文件名
            String fileName = "avatar_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path targetPath = userAvatarDir.resolve(fileName);
            
            // 保存文件
            file.transferTo(targetPath.toFile());
            
            // 删除旧头像（如果存在）
            User user = userMapper.selectById(userId);
            if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                // 旧头像路径格式：avatars/userId/filename
                String oldAvatar = user.getAvatar();
                if (oldAvatar.startsWith("avatars/")) {
                    Path oldPath = Paths.get(avatarPath, oldAvatar.substring("avatars/".length()));
                    try {
                        Files.deleteIfExists(oldPath);
                    } catch (IOException e) {
                        // 忽略删除旧头像失败的错误
                    }
                }
            }
            
            // 返回相对路径（格式：avatars/userId/filename）
            String relativePath = "avatars/" + userId + "/" + fileName;
            
            // 更新数据库
            if (user != null) {
                user.setAvatar(relativePath);
                user.setUpdateTime(new Date());
                userMapper.updateById(user);
            }
            
            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 加密密码（MD5 + 盐）
     */
    private String encryptPassword(String password) {
        String saltedPassword = SALT + password + SALT;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}

