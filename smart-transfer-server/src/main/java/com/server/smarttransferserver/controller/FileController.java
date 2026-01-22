package com.server.smarttransferserver.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.FileMergeDTO;
import com.server.smarttransferserver.dto.FileUploadInitDTO;
import com.server.smarttransferserver.dto.TransferTaskQueryDTO;
import com.server.smarttransferserver.service.DownloadCompleteService;
import com.server.smarttransferserver.service.FileDownloadService;
import com.server.smarttransferserver.service.FileInfoService;
import com.server.smarttransferserver.service.FileMergeService;
import com.server.smarttransferserver.service.FileUploadService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.vo.ChunkUploadVO;
import com.server.smarttransferserver.vo.FileDownloadInitVO;
import com.server.smarttransferserver.vo.FileMergeVO;
import com.server.smarttransferserver.vo.FileInfoVO;
import com.server.smarttransferserver.vo.FileUploadInitVO;
import com.server.smarttransferserver.vo.TransferTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件传输Controller
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    
    @Autowired
    private FileUploadService uploadService;
    
    @Autowired
    private FileMergeService mergeService;
    
    @Autowired
    private TransferTaskService taskService;
    
    @Autowired
    private FileInfoService fileInfoService;
    
    @Autowired
    private com.server.smarttransferserver.service.IFileStorageService fileStorageService;
    
    @Autowired
    private FileDownloadService downloadService;
    
    @Autowired
    private DownloadCompleteService downloadCompleteService;
    
    /**
     * 初始化文件上传
     *
     * @param dto 上传初始化DTO
     * @return 初始化结果
     */
    @PostMapping("/upload/init")
    public Result<FileUploadInitVO> initUpload(@Valid @RequestBody FileUploadInitDTO dto) {
        log.info("初始化文件上传 - 文件名: {}, 大小: {}", dto.getFileName(), dto.getFileSize());
        try {
            FileUploadInitVO vo = uploadService.initUpload(dto);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("初始化上传失败", e);
            return Result.error("初始化上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查分片是否已上传（秒传/断点续传）
     * 兼容 vue-simple-uploader GET 请求
     *
     * @param identifier 文件唯一标识（MD5）
     * @param chunkNumber 分片编号
     * @param totalChunks 总分片数
     * @param totalSize 文件总大小
     * @param filename 文件名
     * @return 检查结果
     */
    @GetMapping("/upload/chunk")
    public Result<FileUploadInitVO> checkChunk(
            @RequestParam("identifier") String identifier,
            @RequestParam(value = "chunkNumber", required = false) Integer chunkNumber,
            @RequestParam(value = "totalChunks", required = false) Integer totalChunks,
            @RequestParam(value = "totalSize", required = false) Long totalSize,
            @RequestParam(value = "filename", required = false) String filename) {
        
        log.info("检查分片 - identifier: {}, chunkNumber: {}", identifier, chunkNumber);
        try {
            FileUploadInitVO vo = uploadService.checkChunk(identifier, totalSize, filename, totalChunks);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("检查分片失败", e);
            return Result.error("检查分片失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传分片
     *
     * @param fileId      文件ID
     * @param chunkNumber 分片序号
     * @param chunkHash   分片哈希
     * @param file        分片文件
     * @return 上传结果
     */
    @PostMapping("/upload/chunk")
    public Result<ChunkUploadVO> uploadChunk(
            @RequestParam("fileId") Long fileId,
            @RequestParam("chunkNumber") Integer chunkNumber,
            @RequestParam("chunkHash") String chunkHash,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        log.info("上传分片 - 文件ID: {}, 分片: {}", fileId, chunkNumber);
        
        try {
            ChunkUploadVO vo = uploadService.uploadChunk(fileId, chunkNumber, chunkHash, file);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("分片上传失败", e);
            return Result.error("分片上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 合并文件
     *
     * @param dto 合并请求DTO
     * @return 合并结果
     */
    @PostMapping("/merge")
    public Result<FileMergeVO> mergeFile(@Valid @RequestBody FileMergeDTO dto) {
        log.info("合并文件 - 文件ID: {}", dto.getFileId());
        try {
            FileMergeVO vo = mergeService.mergeFile(dto);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("文件合并失败", e);
            return Result.error("文件合并失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消上传
     * 清理未完成的上传数据
     *
     * @param fileId 文件ID
     * @return 取消结果
     */
    @DeleteMapping("/upload/{fileId}")
    public Result<String> cancelUpload(@PathVariable Long fileId) {
        log.info("取消上传 - 文件ID: {}", fileId);
        try {
            mergeService.cancelUpload(fileId);
            return Result.success("取消上传成功");
        } catch (Exception e) {
            log.error("取消上传失败", e);
            return Result.error("取消上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/task/{taskId}")
    public Result<TransferTaskVO> getTask(@PathVariable String taskId) {
        log.info("查询任务 - 任务ID: {}", taskId);
        try {
            TransferTaskVO vo = taskService.getTaskByTaskId(taskId);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询任务失败", e);
            return Result.error("查询任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询任务列表
     *
     * @param queryDTO 查询条件
     * @return 任务列表
     */
    @PostMapping("/task/list")
    public Result<IPage<TransferTaskVO>> queryTasks(@RequestBody TransferTaskQueryDTO queryDTO) {
        log.info("查询任务列表 - 类型: {}, 状态: {}", queryDTO.getTaskType(), queryDTO.getTransferStatus());
        try {
            IPage<TransferTaskVO> page = taskService.queryTasks(queryDTO);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return Result.error("查询任务列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 删除结果
     */
    @DeleteMapping("/task/{taskId}")
    public Result<Void> deleteTask(@PathVariable String taskId) {
        log.info("删除任务 - 任务ID: {}", taskId);
        try {
            taskService.deleteTask(taskId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return Result.error("删除任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param status   上传状态（可选）
     * @param userId   用户ID（可选，仅管理员可用，用于筛选指定用户的文件）
     * @return 文件列表
     */
    @GetMapping("/list")
    public Result<IPage<FileInfoVO>> getFileList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId) {
        
        log.info("查询文件列表 - 页码: {}, 大小: {}, 状态: {}, 用户ID: {}", pageNum, pageSize, status, userId);
        try {
            IPage<FileInfoVO> page = fileInfoService.getFileList(pageNum, pageSize, status, userId);
            return Result.success(page);
        } catch (Exception e) {
            log.error("查询文件列表失败", e);
            return Result.error("查询文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件详情
     *
     * @param id 文件ID
     * @return 文件详情
     */
    @GetMapping("/{id}")
    public Result<FileInfoVO> getFileInfo(@PathVariable Long id) {
        log.info("获取文件详情 - ID: {}", id);
        try {
            FileInfoVO vo = fileInfoService.getFileById(id);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取文件详情失败", e);
            return Result.error("获取文件详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化文件下载（分块下载）
     *
     * @param id 文件ID
     * @param chunkSize 分块大小（可选，默认5MB）
     * @return 下载初始化结果
     */
    @GetMapping("/download/init/{id}")
    public Result<FileDownloadInitVO> initDownload(
            @PathVariable Long id,
            @RequestParam(value = "chunkSize", required = false) Long chunkSize) {
        log.info("初始化文件下载 - ID: {}, 分块大小: {}", id, chunkSize);
        try {
            FileDownloadInitVO vo = downloadService.initDownload(id, chunkSize);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("初始化下载失败", e);
            return Result.error("初始化下载失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件分块（集成拥塞控制，二进制流传输）
     * **优化：使用二进制流传输（标准做法），元数据通过响应头传输**
     *
     * @param id 文件ID
     * @param chunkNumber 分块编号
     * @param startByte 起始字节位置
     * @param endByte 结束字节位置
     * @return 二进制数据流（元数据在响应头中）
     */
    @GetMapping("/download/chunk/{id}/{chunkNumber}")
    public ResponseEntity<byte[]> downloadChunk(
            @PathVariable Long id,
            @PathVariable Integer chunkNumber,
            @RequestParam(value = "startByte", required = false) Long startByte,
            @RequestParam(value = "endByte", required = false) Long endByte) {
        log.info("下载分块 - 文件ID: {}, 分块: {}, 范围: {}-{}", id, chunkNumber, startByte, endByte);
        try {
            return downloadService.downloadChunk(id, chunkNumber, startByte, endByte);
        } catch (Exception e) {
            log.error("下载分块失败", e);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Success", "false");
            headers.set("X-Error-Message", e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .headers(headers)
                    .body(("下载分块失败: " + e.getMessage()).getBytes());
        }
    }
    
    /**
     * 标记下载任务完成
     *
     * @param taskId 任务ID
     * @return 完成结果
     */
    @PostMapping("/download/complete/{taskId}")
    public Result<String> completeDownload(@PathVariable String taskId) {
        log.info("标记下载任务完成 - 任务ID: {}", taskId);
        try {
            downloadCompleteService.completeDownload(taskId);
            return Result.success("下载任务完成");
        } catch (Exception e) {
            log.error("标记下载任务完成失败", e);
            return Result.error("标记下载任务完成失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消下载任务（修复M1: 任务取消时资源清理）
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/download/{taskId}")
    public Result<String> cancelDownload(@PathVariable String taskId) {
        log.info("取消下载任务 - 任务ID: {}", taskId);
        try {
            downloadCompleteService.cancelDownload(taskId);
            return Result.success("取消下载成功");
        } catch (Exception e) {
            log.error("取消下载任务失败", e);
            return Result.error("取消下载任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件（支持断点续传，兼容旧接口）
     *
     * @param id 文件ID
     * @param rangeHeader Range请求头（用于断点续传）
     * @return 文件流
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        log.info("下载文件 - ID: {}, Range: {}", id, rangeHeader);
        try {
            return fileInfoService.downloadFile(id, rangeHeader);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 删除文件
     *
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteFile(@PathVariable Long id) {
        log.info("删除文件 - ID: {}", id);
        try {
            fileInfoService.deleteFile(id);
            return Result.success("文件删除成功");
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return Result.error("删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 搜索文件
     *
     * @param fileName 文件名关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<?> searchFile(@RequestParam String fileName) {
        log.info("搜索文件 - 关键词: {}", fileName);
        try {
            List<FileInfoVO> list = fileInfoService.searchByFileName(fileName);
            return Result.success(list);
        } catch (Exception e) {
            log.error("搜索文件失败", e);
            return Result.error("搜索文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 重命名文件
     *
     * @param params 包含id和fileName
     * @return 重命名结果
     */
    @PutMapping("/rename")
    public Result<String> renameFile(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String fileName = params.get("fileName").toString();
        log.info("重命名文件 - ID: {}, 新名称: {}", id, fileName);
        try {
            fileInfoService.renameFile(id, fileName);
            return Result.success("重命名成功");
        } catch (Exception e) {
            log.error("重命名文件失败", e);
            return Result.error("重命名文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 移动文件到指定文件夹
     *
     * @param params 包含id和targetFolderId
     * @return 移动结果
     */
    @PutMapping("/move")
    public Result<String> moveFile(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Long targetFolderId = Long.valueOf(params.get("targetFolderId").toString());
        log.info("移动文件 - ID: {}, 目标文件夹: {}", id, targetFolderId);
        try {
            fileInfoService.moveFile(id, targetFolderId);
            return Result.success("移动成功");
        } catch (Exception e) {
            log.error("移动文件失败", e);
            return Result.error("移动文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量移动文件
     *
     * @param params 包含fileIds和targetFolderId
     * @return 移动结果
     */
    @PutMapping("/move/batch")
    public Result<String> batchMoveFiles(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> fileIds = ((List<Integer>) params.get("fileIds"))
                .stream().map(Long::valueOf).collect(Collectors.toList());
        Long targetFolderId = Long.valueOf(params.get("targetFolderId").toString());
        log.info("批量移动文件 - 数量: {}, 目标文件夹: {}", fileIds.size(), targetFolderId);
        try {
            fileInfoService.batchMoveFiles(fileIds, targetFolderId);
            return Result.success("批量移动成功");
        } catch (Exception e) {
            log.error("批量移动文件失败", e);
            return Result.error("批量移动文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除文件（移动到回收站）
     *
     * @param ids 文件ID列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Result<String> batchDeleteFiles(@RequestBody List<Long> ids) {
        log.info("批量删除文件 - 数量: {}", ids.size());
        try {
            fileInfoService.batchDeleteFiles(ids);
            return Result.success("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            return Result.error("批量删除文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 预览文件
     *
     * @param id 文件ID
     * @return 文件流
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id) {
        log.info("预览文件 - ID: {}", id);
        try {
            return fileInfoService.previewFile(id);
        } catch (Exception e) {
            log.error("预览文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 复制文件
     *
     * @param params 包含fileId和targetFolderId
     * @return 复制结果
     */
    @PostMapping("/copy")
    public Result<String> copyFile(@RequestBody Map<String, Object> params) {
        Long fileId = Long.valueOf(params.get("fileId").toString());
        Long targetFolderId = Long.valueOf(params.get("targetFolderId").toString());
        log.info("复制文件 - ID: {}, 目标文件夹: {}", fileId, targetFolderId);
        try {
            fileInfoService.copyFile(fileId, targetFolderId);
            return Result.success("复制成功");
        } catch (Exception e) {
            log.error("复制文件失败", e);
            return Result.error("复制文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量复制文件
     *
     * @param params 包含fileIds和targetFolderId
     * @return 复制结果
     */
    @PostMapping("/copy/batch")
    public Result<String> batchCopyFiles(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> fileIds = ((List<Integer>) params.get("fileIds"))
                .stream().map(Long::valueOf).collect(Collectors.toList());
        Long targetFolderId = Long.valueOf(params.get("targetFolderId").toString());
        log.info("批量复制文件 - 数量: {}, 目标文件夹: {}", fileIds.size(), targetFolderId);
        try {
            fileInfoService.batchCopyFiles(fileIds, targetFolderId);
            return Result.success("批量复制成功");
        } catch (Exception e) {
            log.error("批量复制文件失败", e);
            return Result.error("批量复制文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 解压文件
     *
     * @param params 解压参数
     * @return 解压结果
     */
    @PostMapping("/unzip")
    public Result<String> unzipFile(@RequestBody Map<String, Object> params) {
        Long fileId = Long.valueOf(params.get("fileId").toString());
        Integer unzipMode = Integer.valueOf(params.get("unzipMode").toString());
        String folderName = params.get("folderName") != null ? params.get("folderName").toString() : null;
        Long targetFolderId = params.get("targetFolderId") != null ? 
                Long.valueOf(params.get("targetFolderId").toString()) : null;
        
        log.info("解压文件 - ID: {}, 模式: {}, 文件夹名: {}, 目标: {}", 
                fileId, unzipMode, folderName, targetFolderId);
        try {
            fileInfoService.unzipFile(fileId, unzipMode, folderName, targetFolderId);
            return Result.success("解压成功");
        } catch (Exception e) {
            log.error("解压文件失败", e);
            return Result.error("解压文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量下载文件（打包为ZIP）
     *
     * @param ids 文件ID列表
     * @param response HTTP响应
     */
    @GetMapping("/download/batch")
    public void batchDownload(
            @RequestParam("ids") List<Long> ids,
            HttpServletResponse response) {
        
        log.info("批量下载文件 - 数量: {}", ids.size());
        
        try {
            fileInfoService.batchDownloadFiles(ids, response);
        } catch (Exception e) {
            log.error("批量下载失败", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "批量下载失败: " + e.getMessage());
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }
}

