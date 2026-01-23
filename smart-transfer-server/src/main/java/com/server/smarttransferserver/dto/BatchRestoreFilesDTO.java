package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量还原文件DTO
 */
@Data
public class BatchRestoreFilesDTO {
    
    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> ids;
}
