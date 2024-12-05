package com.mediplanpro.controller;


import com.mediplanpro.service.MedicalPlanService;
import com.mediplanpro.service.JsonSchemaValidatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The MedicalPlanController class handles HTTP requests for managing
 * MedicalPlan objects. It supports creating, retrieving, and deleting plans.
 */
@RestController
@RequestMapping("/api/v1/medicalPlans")
public class MedicalPlanController {

    @Autowired
    private MedicalPlanService medicalPlanService;

    @Autowired
    private JsonSchemaValidatorService jsonSchemaValidatorService;

    @PostMapping
    public ResponseEntity add(@RequestBody String planString) {
        return medicalPlanService.add(planString);
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") String planId,@RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        return medicalPlanService.get(planId,ifNoneMatch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") String planId,@RequestHeader(value = "If-Match", required = false) String ifMatch){
        return medicalPlanService.delete(planId,ifMatch);
    }
    @PutMapping("/{id}")
    public ResponseEntity update(@PathVariable("id") String planId,@RequestBody String planString,@RequestHeader(value = "If-Match", required = false) String ifMatch){
        return medicalPlanService.update(planId,planString,ifMatch);
    }

    @PatchMapping("/{id}")
    public ResponseEntity patch(@PathVariable String id, @RequestBody Map<String, Object> updates,@RequestHeader(value = "If-Match", required = false) String ifMatch){
        return medicalPlanService.patch(id,updates,ifMatch);
    }
}
