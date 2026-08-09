package com.jjwpp.linxr.common.dto;

import lombok.Data;
import java.util.List;

/**
 * 统一词条 DTO — 将12张业务表的数据统一为前端所需的格式
 */
@Data
public class CodexEntryDTO {
    private String id;
    private String name;
    private String subtitle;
    private String summary;
    private String category;
    private List<String> tags;
    private List<String> details;
}
