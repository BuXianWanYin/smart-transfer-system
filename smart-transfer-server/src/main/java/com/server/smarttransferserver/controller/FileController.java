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
            boolean success = mergeService.cancelUpload(fileId);
            if (success) {
                return Result.success("取消上传成功");
            } else {
                return Result.error("取消上传失败");
            }
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
     * 下载文件（支持断点续传）
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
            FileInfoVO fileInfo = fileInfoService.getFileById(id);
            if (fileInfo == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 获取绝对路径（兼容相对路径和绝对路径）
            java.nio.file.Path filePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
            File file = filePath.toFile();
            if (!file.exists()) {
                log.error("文件不存在 - 路径: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            long fileLength = file.length();
            
            // 编码文件名
            String encodedFileName = URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            // 支持断点续传 - 解析Range头
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                long start = Long.parseLong(ranges[0]);
                long end = ranges.length > 1 && !ranges[1].isEmpty() 
                        ? Long.parseLong(ranges[1]) : fileLength - 1;
                
                // 校验范围
                if (start >= fileLength) {
                    return ResponseEntity.status(416).build(); // Range Not Satisfiable
                }
                if (end >= fileLength) {
                    end = fileLength - 1;
                }
                
                final long contentLength = end - start + 1;
                final long startPos = start;
                
                // 使用InputStreamResource返回部分内容
                org.springframework.core.io.InputStreamResource resource = 
                        new org.springframework.core.io.InputStreamResource(
                                new java.io.FileInputStream(file) {
                                    {
                                        skip(startPos);
                                    }
                                    
                                    private long remaining = contentLength;
                                    
                                    @Override
                                    public int read() throws java.io.IOException {
                                        if (remaining <= 0) return -1;
                                        remaining--;
                                        return super.read();
                                    }
                                    
                                    @Override
                                    public int read(byte[] b, int off, int len) throws java.io.IOException {
                                        if (remaining <= 0) return -1;
                                        len = (int) Math.min(len, remaining);
                                        int read = super.read(b, off, len);
                                        if (read > 0) remaining -= read;
                                        return read;
                                    }
                                });
                
                return ResponseEntity.status(206) // Partial Content
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .contentLength(contentLength)
                        .body(resource);
            }
            
            // 普通下载（无Range请求）
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileLength)
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
            
            // 获取绝对路径（兼容相对路径和绝对路径）
            java.nio.file.Path filePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
            File file = filePath.toFile();
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
            // 设置响应头
            String fileName = "files_" + System.currentTimeMillis() + ".zip";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            
            // 创建ZIP输出流
            try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
                for (Long id : ids) {
                    FileInfoVO fileInfo = fileInfoService.getFileById(id);
                    if (fileInfo == null || fileInfo.getIsDir() == 1) {
                        continue;
                    }
                    
                    // 获取绝对路径（兼容相对路径和绝对路径）
                    java.nio.file.Path filePath = fileStorageService.getAbsoluteFilePath(fileInfo.getFilePath());
                    File file = filePath.toFile();
                    if (!file.exists()) {
                        log.warn("文件不存在 - ID: {}, 路径: {}", id, filePath);
                        continue;
                    }
                    
                    // 添加文件到ZIP
                    String entryName = fileInfo.getFileName();
                    if (fileInfo.getExtendName() != null && !fileInfo.getFileName().contains(".")) {
                        entryName = fileInfo.getFileName() + "." + fileInfo.getExtendName();
                    }
                    
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipOut.putNextEntry(zipEntry);
                    
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            zipOut.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    zipOut.closeEntry();
                }
                
                zipOut.finish();
            }
            
        } catch (Exception e) {
            log.error("批量下载失败", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "批量下载失败");
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
            }
        }
    }
}

