package com.mediplanpro.service.impl;

import com.mediplanpro.model.MedicalPlan;
import com.mediplanpro.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * RedisServiceImpl is the implementation of the RedisService interface.
 * It provides concrete methods for interacting with Redis to cache
 * and retrieve MedicalPlan objects.
 */
@Service
public class RedisServiceImpl implements RedisService {

    private final String MEDICAL_PLAN_KEY_PREFIX = "medicalplan:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void cacheMedicalPlan(MedicalPlan plan) {
        String redisKey = MEDICAL_PLAN_KEY_PREFIX + plan.getObjectId();
        System.out.println("Storing key: " + redisKey);
        System.out.println("Medical Plan: " + plan); // 确认 plan 对象的内容
        redisTemplate.opsForValue().set(MEDICAL_PLAN_KEY_PREFIX + plan.getObjectId(), plan);
    }

    @Override
    public MedicalPlan getCachedMedicalPlan(String planId) {
        return (MedicalPlan) redisTemplate.opsForValue().get(MEDICAL_PLAN_KEY_PREFIX + planId);
    }

    @Override
    public void deleteCachedMedicalPlan(String planId) {
        redisTemplate.delete(MEDICAL_PLAN_KEY_PREFIX + planId);
    }

    @Override
    public boolean hasKey(String key) {
        String redisKey = MEDICAL_PLAN_KEY_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }
}
