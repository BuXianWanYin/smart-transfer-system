package com.server.smarttransferserver.service;

import com.server.smarttransferserver.dto.FileUploadInitDTO;
import com.server.smarttransferserver.vo.ChunkUploadVO;
import com.server.smarttransferserver.vo.FileUploadInitVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 * 提供文件上传初始化、分片上传等功能
 */
public interface FileUploadService {
    
    /**
     * 初始化文件上传
     * 检查文件是否已存在，支持秒传和断点续传
     *
     * @param dto 上传初始化请求
     * @return 上传初始化结果
     */
    FileUploadInitVO initUpload(FileUploadInitDTO dto);
    
    /**
     * 上传文件分片
     *
     * @param fileId 文件ID
     * @param chunkNumber 分片编号
     * @param chunkHash 分片哈希
     * @param file 分片文件
     * @return 分片上传结果
     */
    ChunkUploadVO uploadChunk(Long fileId, Integer chunkNumber, String chunkHash, MultipartFile file);
}

