package com.server.smarttransferserver.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.server.smarttransferserver.common.Result;
import com.server.smarttransferserver.dto.FileMergeDTO;
import com.server.smarttransferserver.dto.FileUploadInitDTO;
import com.server.smarttransferserver.dto.TransferTaskQueryDTO;
import com.server.smarttransferserver.service.FileInfoService;
import com.server.smarttransferserver.service.FileMergeService;
import com.server.smarttransferserver.service.FileUploadService;
import com.server.smarttransferserver.service.TransferTaskService;
import com.server.smarttransferserver.vo.ChunkUploadVO;
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

import javax.validation.Valid;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            if (vo.getSuccess()) {
                return Result.success(vo);
            } else {
                return Result.error(vo.getMessage());
            }
        } catch (Exception e) {
            log.error("文件合并失败", e);
            return Result.error("文件合并失败: " + e.getMessage());
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
            if (vo != null) {
                return Result.success(vo);
            } else {
                return Result.error("任务不存在");
            }
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
     * @return 文件列表
     */
    @GetMapping("/list")
    public Result<IPage<FileInfoVO>> getFileList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        
        log.info("查询文件列表 - 页码: {}, 大小: {}, 状态: {}", pageNum, pageSize, status);
        try {
            IPage<FileInfoVO> page = fileInfoService.getFileList(pageNum, pageSize, status);
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
            if (vo != null) {
                return Result.success(vo);
            } else {
                return Result.error("文件不存在");
            }
        } catch (Exception e) {
            log.error("获取文件详情失败", e);
            return Result.error("获取文件详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     *
     * @param id 文件ID
     * @return 文件流
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        log.info("下载文件 - ID: {}", id);
        try {
            FileInfoVO fileInfo = fileInfoService.getFileById(id);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(fileInfo.getFilePath());
            if (!file.exists()) {
                log.error("文件不存在 - 路径: {}", fileInfo.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 编码文件名
            String encodedFileName = URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(resource);
                    
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
            FileInfoVO fileInfo = fileInfoService.getFileById(id);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(fileInfo.getFilePath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = getContentType(fileInfo.getExtendName());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("预览文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取文件MIME类型
     */
    private String getContentType(String extendName) {
        if (extendName == null) return "application/octet-stream";
        
        switch (extendName.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "pdf":
                return "application/pdf";
            case "txt":
            case "md":
                return "text/plain";
            case "html":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
        }
    }
}

