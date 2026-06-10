package com.yiweilai.wms.file.service;

import com.yiweilai.wms.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件 Service
 */
public interface FileService {

    /**
     * 上传文件
     */
    FileVO upload(MultipartFile file, String bizType, Long bizId);

    /**
     * 根据ID获取文件信息
     */
    FileVO getById(Long id);

    /**
     * 删除文件
     */
    void delete(Long id);
}
