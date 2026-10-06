package com.carlos.system.upload.service;

import com.carlos.oss.utils.OssUtil;
import com.carlos.system.upload.pojo.enums.BucketPolicyEnum;
import com.carlos.system.upload.pojo.param.BucketCreateParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bucket业务服务
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileBucketService {

    public void createBuckets(List<BucketCreateParam> buckets) {
        for (BucketCreateParam bucket : buckets) {
            OssUtil.createBucket(bucket.getName());
            applyPolicy(bucket.getName(), bucket.getPolicy());
        }
    }

    public void setPolicies(List<BucketCreateParam> buckets) {
        for (BucketCreateParam bucket : buckets) {
            applyPolicy(bucket.getName(), bucket.getPolicy());
        }
    }

    private void applyPolicy(String bucketName, BucketPolicyEnum policy) {
        if (policy == null) {
            return;
        }
        switch (policy) {
            case PRIVATE -> OssUtil.setBucketPrivate(bucketName);
            case PUBLIC -> OssUtil.setBucketPublic(bucketName);
            default -> throw new IllegalStateException("Unexpected value: " + policy);
        }
    }
}
