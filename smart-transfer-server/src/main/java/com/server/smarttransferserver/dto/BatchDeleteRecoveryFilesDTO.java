package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量彻底删除回收站文件DTO
 */
@Data
public class BatchDeleteRecoveryFilesDTO {
    
    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> ids;
}
