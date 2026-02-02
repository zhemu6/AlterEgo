package org.zhemu.alterego.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * COS 对象存储管理
 *
 * @author lushihao
 */
@Component
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosManager {

    /**
     * Access Key
     */
    private String secretId;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * Region
     */
    private String region;

    /**
     * Bucket Name
     */
    private String bucket;

    /**
     * Base URL
     */
    private String baseUrl;

    private COSClient cosClient;

    /**
     * 初始化 COS 客户端
     */
    @PostConstruct
    public void init() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        cosClient = new COSClient(cred, clientConfig);
    }

    /**
     * 上传对象
     *
     * @param key           唯一键
     * @param localFilePath 本地文件路径
     * @return Uplaod result
     */
    public PutObjectResult putObject(String key, String localFilePath) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key,
                new File(localFilePath));
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件对象
     * @return Upload result
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(bucket, key);
    }

    /**
     * 上传对象
     * 
     * @param key         唯一键
     * @param inputStream 文件流
     * @param metadata    元数据
     */
    public PutObjectResult putObject(String key, java.io.InputStream inputStream,
            com.qcloud.cos.model.ObjectMetadata metadata) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, inputStream, metadata);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 获取访问地址 (Alias for getFileUrl)
     * 
     * @param key 文件Key
     * @return 完整URL
     */
    public String getAccessUrl(String key) {
        return getFileUrl(key);
    }

    /**
     * 获取文件访问地址
     *
     * @param key 文件Key
     * @return 完整URL
     */
    public String getFileUrl(String key) {
        return baseUrl + "/" + key;
    }
}
