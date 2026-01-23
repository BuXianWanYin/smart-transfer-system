package com.server.smarttransferserver.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量移动文件DTO
 */
@Data
public class BatchMoveFilesDTO {
    
    @NotEmpty(message = "文件ID列表不能为空")
    private List<Long> fileIds;
    
    @NotNull(message = "目标文件夹ID不能为空")
    private Long targetFolderId;
}
