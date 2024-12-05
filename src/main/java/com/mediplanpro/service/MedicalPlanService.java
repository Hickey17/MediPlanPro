package com.mediplanpro.service;

import com.mediplanpro.model.MedicalPlan;
import org.springframework.http.ResponseEntity;

import java.util.Map;


/**
 * MedicalPlanService interface provides an abstraction for managing
 * MedicalPlan objects. It defines the contract for CRUD operations
 * on MedicalPlan entities.
 */
public interface MedicalPlanService {

    ResponseEntity add(String planString);
    ResponseEntity get(String planId,String ifNoneMatch);
    ResponseEntity delete(String planId,String ifMatch);
    ResponseEntity update(String planId,String planString,String ifMatch);
    ResponseEntity patch(String id, Map<String, Object> updates,String ifMatch);

}
