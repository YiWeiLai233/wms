package com.yiweilai.wms.file.service.impl;

import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.file.entity.FileRecord;
import com.yiweilai.wms.file.mapper.FileRecordMapper;
import com.yiweilai.wms.file.service.FileService;
import com.yiweilai.wms.file.vo.FileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordMapper fileRecordMapper;

    @Value("${file.upload-path:/app/uploads}")
    private String uploadPath;

    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileVO upload(MultipartFile file, String bizType, Long bizId) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        // 生成存储路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;
        String relativePath = "/" + datePath + "/" + newFilename;
        String fullPath = uploadPath + relativePath;

        // 创建目录并保存文件
        try {
            Path targetPath = Paths.get(fullPath).toAbsolutePath().normalize();
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, file.getBytes());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败");
        }

        // 保存文件记录
        FileRecord fileRecord = new FileRecord();
        fileRecord.setFileName(originalFilename);
        fileRecord.setFilePath(relativePath);
        fileRecord.setFileSize(file.getSize());
        fileRecord.setFileType(file.getContentType());
        fileRecord.setBizType(bizType);
        fileRecord.setBizId(bizId);
        fileRecordMapper.insert(fileRecord);

        // 返回结果
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(fileRecord, vo);
        vo.setUrl(baseUrl + relativePath);
        return vo;
    }

    @Override
    public FileVO getById(Long id) {
        FileRecord fileRecord = fileRecordMapper.findById(id);
        if (fileRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        FileVO vo = new FileVO();
        BeanUtils.copyProperties(fileRecord, vo);
        vo.setUrl(baseUrl + fileRecord.getFilePath());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileRecord fileRecord = fileRecordMapper.findById(id);
        if (fileRecord == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        // 删除物理文件
        File file = new File(uploadPath + fileRecord.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        // 删除记录
        fileRecordMapper.deleteById(id);
    }
}
