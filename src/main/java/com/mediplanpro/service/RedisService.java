package com.mediplanpro.service;

import com.mediplanpro.model.MedicalPlan;

/**
 * RedisService interface provides an abstraction for caching operations.
 * It defines the contract for storing and retrieving MedicalPlan objects from Redis.
 */
public interface RedisService {

    void cacheMedicalPlan(MedicalPlan plan);

    MedicalPlan getCachedMedicalPlan(String planId);

    void deleteCachedMedicalPlan(String planId);

    boolean hasKey(String key);
}
