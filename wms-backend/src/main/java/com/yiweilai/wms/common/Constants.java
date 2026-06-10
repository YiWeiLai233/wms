package com.yiweilai.wms.common;

/**
 * 系统常量
 */
public class Constants {

    private Constants() {}

    /** JWT 请求头 */
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 分页默认值 */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 文件上传路径前缀 */
    public static final String FILE_UPLOAD_PRODUCT = "/product";
    public static final String FILE_UPLOAD_RETURN = "/return";
    public static final String FILE_UPLOAD_OUTBOUND = "/outbound";

    /** 逻辑删除标记 */
    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;
}
