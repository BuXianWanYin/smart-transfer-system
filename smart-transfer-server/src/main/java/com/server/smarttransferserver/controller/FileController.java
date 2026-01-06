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

/**
 * 文件传输Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
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
}

