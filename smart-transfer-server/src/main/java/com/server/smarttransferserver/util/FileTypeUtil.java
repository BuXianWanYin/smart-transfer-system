package com.server.smarttransferserver.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件类型工具类
 * 根据文件扩展名判断文件类型
 */
public class FileTypeUtil {

    /**
     * 文件类型常量
     */
    public static final int TYPE_ALL = 0;      // 全部
    public static final int TYPE_IMAGE = 1;    // 图片
    public static final int TYPE_DOCUMENT = 2; // 文档
    public static final int TYPE_VIDEO = 3;    // 视频
    public static final int TYPE_AUDIO = 4;    // 音乐
    public static final int TYPE_OTHER = 5;    // 其他

    /**
     * 图片扩展名
     */
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "tif", "raw", "psd"
    ));

    /**
     * 文档扩展名
     */
    private static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "md", "rtf",
            "odt", "ods", "odp", "csv", "json", "xml", "html", "htm", "css", "js",
            "java", "py", "c", "cpp", "h", "sql", "vue", "ts", "tsx", "jsx"
    ));

    /**
     * 视频扩展名
     */
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpeg", "mpg", "3gp", "rm", "rmvb"
    ));

    /**
     * 音频扩展名
     */
    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "ape", "aiff", "mid", "midi"
    ));

    /**
     * 根据扩展名获取文件类型
     * @param extendName 文件扩展名（不含点号）
     * @return 文件类型代码
     */
    public static int getFileType(String extendName) {
        if (extendName == null || extendName.isEmpty()) {
            return TYPE_OTHER;
        }
        
        String ext = extendName.toLowerCase().trim();
        
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return TYPE_IMAGE;
        }
        if (DOCUMENT_EXTENSIONS.contains(ext)) {
            return TYPE_DOCUMENT;
        }
        if (VIDEO_EXTENSIONS.contains(ext)) {
            return TYPE_VIDEO;
        }
        if (AUDIO_EXTENSIONS.contains(ext)) {
            return TYPE_AUDIO;
        }
        
        return TYPE_OTHER;
    }

    /**
     * 获取指定类型的所有扩展名
     * @param fileType 文件类型
     * @return 扩展名集合
     */
    public static Set<String> getExtensionsByType(int fileType) {
        switch (fileType) {
            case TYPE_IMAGE:
                return IMAGE_EXTENSIONS;
            case TYPE_DOCUMENT:
                return DOCUMENT_EXTENSIONS;
            case TYPE_VIDEO:
                return VIDEO_EXTENSIONS;
            case TYPE_AUDIO:
                return AUDIO_EXTENSIONS;
            default:
                return new HashSet<>();
        }
    }

    /**
     * 判断扩展名是否属于指定类型
     * @param extendName 文件扩展名
     * @param fileType 文件类型
     * @return 是否匹配
     */
    public static boolean matchesType(String extendName, int fileType) {
        if (fileType == TYPE_ALL) {
            return true;
        }
        if (fileType == TYPE_OTHER) {
            int actualType = getFileType(extendName);
            return actualType == TYPE_OTHER;
        }
        return getFileType(extendName) == fileType;
    }
}

