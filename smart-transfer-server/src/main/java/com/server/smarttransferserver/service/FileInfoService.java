package com.server.smarttransferserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.server.smarttransferserver.entity.FileInfo;
import com.server.smarttransferserver.vo.FileInfoVO;

/**
 * 文件信息服务接口
 * 提供文件信息的查询、更新等功能
 */
public interface FileInfoService extends IService<FileInfo> {
    
    
    /**
     * 根据文件哈希查询文件信息
     *
     * @param fileHash 文件哈希值
     * @return 文件信息，不存在返回null
     */
    FileInfo getByFileHash(String fileHash);
    
    /**
     * 根据ID获取文件信息（返回VO）
     *
     * @param id 文件ID
     * @return 文件信息VO
     */
    FileInfoVO getFileById(Long id);
    
    /**
     * 分页查询文件列表（返回VO）
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param status 文件状态（可选）
     * @return 分页结果
     */
    IPage<FileInfoVO> getFileList(Integer pageNum, Integer pageSize, String status);
    
    /**
     * 更新文件上传状态
     *
     * @param fileId 文件ID
     * @param status 上传状态
     * @return 是否更新成功
     */
    boolean updateUploadStatus(Long fileId, String status);
    
    /**
     * 删除文件
     *
     * @param id 文件ID
     */
    void deleteFile(Long id);
}

