package com.server.smarttransferserver.service;

/**
 * 下载完成服务接口
 * 用于在下载完成后清理资源和更新任务状态
 */
public interface DownloadCompleteService {
    
    /**
     * 标记下载任务完成
     * 更新任务状态为COMPLETED，清理算法实例
     *
     * @param taskId 任务ID
     */
    void completeDownload(String taskId);
    
    /**
     * 取消下载任务
     * 清理算法实例和Redis数据
     *
     * @param taskId 任务ID
     */
    void cancelDownload(String taskId);
}
