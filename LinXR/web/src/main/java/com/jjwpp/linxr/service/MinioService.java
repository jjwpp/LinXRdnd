package com.jjwpp.linxr.service;

import com.jjwpp.linxr.common.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * MinIO 文件存储服务
 * <p>
 * 提供文件上传、下载、删除功能。
 * 文件路径规则：{bucket}/{category}/{uuid}.{ext}
 * 例：linxr/monsters/abc123.jpg
 */
@Slf4j
@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @PostConstruct
    public void init() {
        try {
            ensureBucket();
        } catch (Exception e) {
            log.warn("MinIO bucket 初始化失败（MinIO可能未启动）: {}", e.getMessage());
        }
    }

    /**
     * 确保 bucket 存在，不存在则创建，并设置公开读取策略
     */
    public void ensureBucket() throws Exception {
        String bucket = minioConfig.getBucket();
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO bucket 已创建: {}", bucket);
        }
        // 设置 bucket 公开读取策略（允许匿名 GET 请求）
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        log.info("MinIO bucket 公开读取策略已设置: {}", bucket);
    }

    /**
     * 上传文件到 MinIO
     *
     * @param file     Spring MultipartFile
     * @param category 分类目录（如 monsters, classes/male, classes/female）
     * @return 文件在 MinIO 中的对象路径（如 monsters/abc123.jpg）
     */
    public String uploadFile(MultipartFile file, String category) throws Exception {
        ensureBucket();

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = category + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        log.info("[MinIO] 文件上传成功: {}/{}", minioConfig.getBucket(), objectName);
        return objectName;
    }

    /**
     * 上传本地文件到 MinIO（用于批量导入）
     *
     * @param inputStream 文件输入流
     * @param objectName  对象路径（含分类目录）
     * @param contentType MIME 类型
     * @param size        文件大小
     * @return 对象路径
     */
    public String uploadLocalFile(InputStream inputStream, String objectName, String contentType, long size) throws Exception {
        ensureBucket();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build());

        log.info("[MinIO] 本地文件上传成功: {}/{}", minioConfig.getBucket(), objectName);
        return objectName;
    }

    /**
     * 获取文件的完整访问 URL
     *
     * @param objectName 对象路径
     * @return 完整 URL
     */
    public String getFileUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        return minioConfig.getEndpoint() + "/" + minioConfig.getBucket() + "/" + objectName;
    }

    /**
     * 获取文件输入流（下载）
     */
    public InputStream getFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .build());
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(objectName)
                        .build());
        log.info("[MinIO] 文件删除成功: {}/{}", minioConfig.getBucket(), objectName);
    }

    /**
     * 获取 bucket 名称
     */
    public String getBucket() {
        return minioConfig.getBucket();
    }
}
