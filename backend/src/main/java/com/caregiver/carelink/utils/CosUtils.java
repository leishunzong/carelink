package com.caregiver.carelink.utils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 腾讯云COS文件上传工具类
 *
 * @author CareLink
 * @since 2026-01-29
 */
@Component
public class CosUtils {

    private static final Logger log = LoggerFactory.getLogger(CosUtils.class);

    @Resource
    private COSClient cosClient;

    @Value("${cos.bucket-name}")
    private String bucketName;

    @Value("${cos.base-url}")
    private String baseUrl;

    /**
     * 上传文件
     *
     * @param file 文件
     * @param folder 文件夹路径（如: avatar/, images/）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        try {
            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 生成唯一文件名
            String fileName = folder + UUID.randomUUID().toString().replace("-", "") + extension;

            // 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);

            // 上传文件
            PutObjectResult result = cosClient.putObject(putObjectRequest);
            log.info("文件上传成功: {}", fileName);

            // 返回文件访问URL
            return baseUrl + "/" + fileName;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // 从URL中提取文件key
            String fileKey = fileUrl.replace(baseUrl + "/", "");
            
            // 删除文件
            cosClient.deleteObject(bucketName, fileKey);
            log.info("文件删除成功: {}", fileKey);
            
        } catch (Exception e) {
            log.error("文件删除失败", e);
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param fileUrl 文件URL
     * @return 是否存在
     */
    public boolean doesFileExist(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        try {
            String fileKey = fileUrl.replace(baseUrl + "/", "");
            return cosClient.doesObjectExist(bucketName, fileKey);
        } catch (Exception e) {
            log.error("检查文件存在性失败", e);
            return false;
        }
    }
}
