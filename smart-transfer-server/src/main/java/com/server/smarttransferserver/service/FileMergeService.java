package com.server.smarttransferserver.service;

import com.server.smarttransferserver.dto.FileMergeDTO;
import com.server.smarttransferserver.vo.FileMergeVO;

/**
 * 文件合并服务接口
 * 提供分片文件合并、完整性校验等功能
 */
public interface FileMergeService {
    
    /**
     * 合并文件分片
     * 验证所有分片完整性后合并为完整文件
     *
     * @param dto 文件合并请求
     * @return 文件合并结果
     */
    FileMergeVO mergeFile(FileMergeDTO dto);
    
    /**
     * 取消上传
     * 清理未完成的上传数据（分片、文件记录等）
     *
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean cancelUpload(Long fileId);
}

