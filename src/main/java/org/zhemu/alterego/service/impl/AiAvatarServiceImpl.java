package org.zhemu.alterego.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qcloud.cos.model.ObjectMetadata;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zhemu.alterego.manager.CosManager;
import org.zhemu.alterego.service.AiAvatarService;

/**
 * Agent 头像生成服务（DashScope qwen-image-plus）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAvatarServiceImpl implements AiAvatarService {

    private final CosManager cosManager;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${agentscope.dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.image.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${dashscope.image.model:qwen-image-plus}")
    private String modelName;

    @Value("${dashscope.image.size:1024*1024}")
    private String imageSize;

    @Value("${dashscope.image.poll-interval-ms:1000}")
    private long pollIntervalMs;

    @Value("${dashscope.image.max-poll-times:30}")
    private int maxPollTimes;

    @Override
    public String generateAvatar(Long agentId, String speciesName, String personality) {
        if (agentId == null || StrUtil.isBlank(speciesName)) {
            return null;
        }
        String prompt = buildPrompt(speciesName, personality);
        String taskId = submitAsyncTask(prompt);
        if (StrUtil.isBlank(taskId)) {
            return null;
        }
        String imageUrl = pollImageUrl(taskId);
        if (StrUtil.isBlank(imageUrl)) {
            return null;
        }
        log.info("DashScope image url: {}", imageUrl);
        return downloadAndUpload(agentId, imageUrl);
    }

    private String buildPrompt(String speciesName, String personality) {
        return String.format(
                "生成一个头像：卡通风格、正面、半身或头部、方形构图、干净纯色背景。物种：%s。性格：%s。要求：可爱、风格统一、无文字水印、高清。",
                speciesName,
                StrUtil.blankToDefault(personality, "温和可爱"));
    }

    private String submitAsyncTask(String prompt) {
        String url = baseUrl + "/services/aigc/text2image/image-synthesis";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("X-DashScope-Async", "enable");

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("size", imageSize);
        parameters.put("n", 1);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("input", input);
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || StrUtil.isBlank(response.getBody())) {
            log.warn("DashScope image task submit failed: status={}", response.getStatusCode());
            return null;
        }

        JSONObject root = JSONUtil.parseObj(response.getBody());
        JSONObject output = root.getJSONObject("output");
        if (output != null) {
            return output.getStr("task_id");
        }
        return root.getStr("task_id");
    }

    private String pollImageUrl(String taskId) {
        String url = baseUrl + "/tasks/" + taskId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int i = 0; i < maxPollTimes; i++) {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StrUtil.isNotBlank(response.getBody())) {
                JSONObject root = JSONUtil.parseObj(response.getBody());
                JSONObject output = root.getJSONObject("output");
                if (output != null) {
                    String status = output.getStr("task_status");
                    if ("SUCCEEDED".equalsIgnoreCase(status)) {
                        String imageUrl = extractImageUrl(output);
                        if (StrUtil.isNotBlank(imageUrl)) {
                            return imageUrl;
                        }
                    } else if ("FAILED".equalsIgnoreCase(status)) {
                        log.warn("DashScope image task failed: taskId={}", taskId);
                        return null;
                    }
                }
            }
            try {
                Thread.sleep(Duration.ofMillis(pollIntervalMs).toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        log.warn("DashScope image task timeout: taskId={}", taskId);
        return null;
    }

    private String extractImageUrl(JSONObject output) {
        JSONArray results = output.getJSONArray("results");
        if (results != null && !results.isEmpty()) {
            JSONObject item = results.getJSONObject(0);
            if (item != null) {
                String url = item.getStr("url");
                if (StrUtil.isNotBlank(url)) {
                    return url;
                }
            }
        }
        JSONArray images = output.getJSONArray("images");
        if (images != null && !images.isEmpty()) {
            JSONObject item = images.getJSONObject(0);
            if (item != null) {
                String url = item.getStr("url");
                if (StrUtil.isNotBlank(url)) {
                    return url;
                }
            }
        }
        return null;
    }

    private String downloadAndUpload(Long agentId, String imageUrl) {
        ResponseEntity<byte[]> response =
                restTemplate.exchange(java.net.URI.create(imageUrl), HttpMethod.GET, null, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("Download image failed: status={}", response.getStatusCode());
            return null;
        }
        byte[] data = response.getBody();
        String key = "agent/avatar/" + agentId + "/" + System.currentTimeMillis() + ".png";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        MediaType contentType = response.getHeaders().getContentType();
        metadata.setContentType(contentType != null ? contentType.toString() : MediaType.IMAGE_PNG_VALUE);
        cosManager.putObject(key, new ByteArrayInputStream(data), metadata);
        return cosManager.getAccessUrl(key);
    }
}
