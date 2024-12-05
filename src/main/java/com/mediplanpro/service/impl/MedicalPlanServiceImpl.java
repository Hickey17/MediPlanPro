package com.mediplanpro.service.impl;

import org.elasticsearch.action.delete.DeleteRequest; // 删除请求
import org.elasticsearch.action.index.IndexRequest;  // 索引请求
import org.elasticsearch.action.index.IndexResponse;  // 索引响应
import org.elasticsearch.action.search.SearchRequest;  // 搜索请求
import org.elasticsearch.action.search.SearchResponse;  // 搜索响应
import org.elasticsearch.client.RequestOptions;  // 通用请求选项
import org.elasticsearch.client.RestHighLevelClient;  // 高级客户端
import org.elasticsearch.index.query.QueryBuilders;  // 查询构建器
import org.elasticsearch.search.builder.SearchSourceBuilder;  // 搜索源构建器
import org.elasticsearch.search.SearchHit;  // 搜索结果
import org.elasticsearch.xcontent.XContentType;  // 内容类型
import org.elasticsearch.action.bulk.BulkRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mediplanpro.model.*;
import com.mediplanpro.service.JsonSchemaValidatorService;
import com.mediplanpro.service.MedicalPlanService;
import com.mediplanpro.service.RedisService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;



@Service
public class MedicalPlanServiceImpl implements MedicalPlanService {

    @Autowired
    private RedisService redisService;
    @Autowired
    private RestHighLevelClient restHighLevelClient;



    @Autowired
    private JsonSchemaValidatorService jsonSchemaValidatorService;

    @Override
    public ResponseEntity add(String planString) {
        // 将 JSON 字符串解析为 MedicalPlan 对象
        JSONObject jsonData = new JSONObject(planString);
        jsonSchemaValidatorService.validate(jsonData); // 校验 JSON 格式
        Gson gson = new Gson();
        MedicalPlan plan = gson.fromJson(planString, MedicalPlan.class);

        String id = plan.getObjectId();

        // 检查 Redis 是否已存在
        if (redisService.hasKey(plan.getObjectId())) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

//        // 深拷贝并移除 plan_join
//        MedicalPlan planForRedis = deepCopyWithoutField(plan, "planJoin");
//
//        // 缓存到 Redis
//        redisService.cacheMedicalPlan(planForRedis);
        // 缓存到 Redis
        redisService.cacheMedicalPlan(plan);
        // 设置父文档的 plan_join 字段
        plan.setPlanJoin("plan"); // 根文档类型为 "plan"


        // 保存到 Elasticsearch（顶级文档不需要 routing）
        Map<String, Object> document = convertToMap(plan);
        document.remove("planCostShares");
        document.remove("linkedPlanServices");
        addDocument("medicalplans", id, document);

        // 保存子文档
        saveChildDocuments(plan);

        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    private Map<String, Object> convertToMap(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(object, Map.class);
    }

//    @Override
//    public ResponseEntity add(String planString) {
//        // 将 MedicalPlan 对象转化为 JSON 字符串
//        JSONObject jsonData = new JSONObject(planString);
//        jsonSchemaValidatorService.validate(jsonData);
//        Gson gson = new Gson();
//        MedicalPlan plan = gson.fromJson(planString,MedicalPlan.class);
//        String id = plan.getObjectId();
//        // 检查是否已经存在objectid
//        if(redisService.hasKey(plan.getObjectId())){
//            return new ResponseEntity<>(HttpStatus.CONFLICT);
//        }
//        redisService.cacheMedicalPlan(plan);
//        // 同步到 ElasticSearch
//        medicalPlanElasticRepository.save(plan);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(id);
//    }
//

    @Override
    public ResponseEntity get(String planId,String ifNoneMatch) {
        if(!redisService.hasKey(planId))
            return ResponseEntity.noContent().build();
        MedicalPlan plan = redisService.getCachedMedicalPlan(planId);
        String currentEtag = generateETag(plan);
        if (ifNoneMatch!=null&&ifNoneMatch.equals(currentEtag)){
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header(HttpHeaders.ETAG, currentEtag)
                    .build();
        }
        System.out.println("current etag: "+currentEtag);
        return ResponseEntity.ok().header(HttpHeaders.ETAG, currentEtag).body(plan);
    }

    @Override
    public ResponseEntity delete(String planId,String ifMatch) {
        if(redisService.hasKey(planId)){
            MedicalPlan plan = redisService.getCachedMedicalPlan(planId);
            String currentEtag = generateETag(plan);
            if (ifMatch != null && !ifMatch.equals(currentEtag)) {
                // ETag 不匹配，返回 412 Precondition Failed
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                        .header(HttpHeaders.ETAG, currentEtag)
                        .build();
            }
            redisService.deleteCachedMedicalPlan(planId);
            System.out.println(plan);
            List<String> ids = new ArrayList<>();
            ids.add(planId);
            ids.add(plan.getPlanCostShares().getObjectId());
            for(LinkedPlanService linkedPlanService:plan.getLinkedPlanServices()){
                ids.add(linkedPlanService.getObjectId());
                ids.add(linkedPlanService.getLinkedService().getObjectId());
                ids.add(linkedPlanService.getPlanServiceCostShares().getObjectId());
            }
            try {
                BulkRequest bulkRequest = new BulkRequest();
                for(String id: ids){
                    DeleteRequest deleteRequest = new DeleteRequest("medicalplans", id);
                    //restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
                    bulkRequest.add(deleteRequest);
                }
                restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

            } catch (IOException e) {
                throw new RuntimeException("Failed to delete document from Elasticsearch", e);
            }


            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity update(String planId,String planString,String ifMatch) {
        // 将 MedicalPlan 对象转化为 JSON 字符串
        JSONObject jsonData = new JSONObject(planString);
        jsonSchemaValidatorService.validate(jsonData);
        Gson gson = new Gson();
        MedicalPlan plan = gson.fromJson(planString,MedicalPlan.class);
        String id = plan.getObjectId();
        if(!redisService.hasKey(planId)){
            return ResponseEntity.notFound().build();
        }
        MedicalPlan prePlan = redisService.getCachedMedicalPlan(planId);
        String currentEtag = generateETag(prePlan);

        // 检查客户端提供的 ETag 是否匹配当前 ETag
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            // ETag 不匹配，返回 412 Precondition Failed
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .header(HttpHeaders.ETAG, currentEtag)
                    .build();
        }
        if(redisService.hasKey(id)){
            redisService.deleteCachedMedicalPlan(id);
        }
        redisService.cacheMedicalPlan(plan);
        //medicalPlanElasticRepository.save(plan);

        String newEtag = generateETag(plan);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, newEtag)
                .body(plan);
    }

    @Override
    public ResponseEntity patch(String id, Map<String, Object> updates,String ifMatch) {
        // 从 Redis 中获取计划对象
        MedicalPlan plan = redisService.getCachedMedicalPlan(id);
        String currentEtag = generateETag(plan);

        // 检查客户端提供的 ETag 是否匹配当前 ETag
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            // ETag 不匹配，返回 412 Precondition Failed
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .header(HttpHeaders.ETAG, currentEtag)
                    .build();
        }
        if (plan == null) {
            return ResponseEntity.notFound().build(); // 如果 Redis 中不存在此计划，返回 404
        }

        // 递归地应用更新到计划对象
        applyUpdates(plan, updates);

        // 将更新后的计划对象保存回 Redis
        redisService.cacheMedicalPlan(plan);
        //medicalPlanElasticRepository.save(plan);
        String newEtag = generateETag(plan);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, newEtag)
                .body(plan);
    }
    private void applyUpdates(Object target, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            if (value instanceof Map) {
                // 如果是嵌套对象，则递归更新
                Object nestedObject = getFieldValue(target, key);
                if (nestedObject != null) {
                    applyUpdates(nestedObject, (Map<String, Object>) value);
                }
            } else if (value instanceof List) {
                // 如果是嵌套的列表，更新每个列表项
                List<?> targetList = (List<?>) getFieldValue(target, key);
                List<?> updatesList = (List<?>) value;

                for (int i = 0; i < targetList.size() && i < updatesList.size(); i++) {
                    Object targetItem = targetList.get(i);
                    Map<String, Object> updateItem = (Map<String, Object>) updatesList.get(i);
                    applyUpdates(targetItem, updateItem);
                }
            } else {
                // 简单字段更新
                setFieldValue(target, key, value);
            }
        });
    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Error accessing field: " + fieldName, e);
        }
    }

    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Error setting field: " + fieldName, e);
        }
    }

    private String generateETag(MedicalPlan plan) {
        try {
            // 将对象序列化为字节流
            ObjectMapper objectMapper = new ObjectMapper();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            objectMapper.writeValue(byteArrayOutputStream, plan);
            byte[] contentBytes = byteArrayOutputStream.toByteArray();

            // 使用 MD5 计算哈希值
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(contentBytes);

            // 将哈希值编码为 Base64 字符串，并加上双引号
            return "\"" + Base64.getEncoder().encodeToString(hash) + "\"";
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to generate ETag", e);
        }
    }

    public String addDocument(String indexName, String id, Map<String, Object> document) {
        try {
            // 提取 routing
            String routing = null;
            if (document.containsKey("plan_join") && document.get("plan_join") instanceof Map) {
                Map<String, String> planJoin = (Map<String, String>) document.get("plan_join");
                routing = planJoin.get("parent");
            }

            // 创建 IndexRequest
            IndexRequest request = new IndexRequest(indexName)
                    .id(id)
                    .source(document, XContentType.JSON);

            // 设置 routing 参数（仅对子文档需要）
            if (routing != null) {
                request.routing(routing);
            }

            // 执行索引操作
            IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);

            // 返回文档 ID
            return response.getId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to index document", e);
        }
    }
    private void saveChildDocuments(MedicalPlan parentPlan) {
        // 保存 PlanCostShares（直接子文档）
        PlanCostShares costShares = parentPlan.getPlanCostShares();
        if (costShares != null) {
            // 设置 plan_join 字段
            costShares.setPlanJoin("planCostShares", parentPlan.getObjectId());
            // 转换为 Map 并保存
            Map<String, Object> costSharesDoc = convertToMap(costShares);
            addDocument("medicalplans", costShares.getObjectId(), costSharesDoc);
        }

        // 保存 LinkedPlanService 列表（直接子文档）
        List<LinkedPlanService> linkedPlanServices = parentPlan.getLinkedPlanServices();
        if (linkedPlanServices != null) {
            for (LinkedPlanService service : linkedPlanServices) {
                // 设置 plan_join 字段
                service.setPlanJoin("linkedPlanService", parentPlan.getObjectId());
                // 转换为 Map 并保存
                Map<String, Object> serviceDoc = convertToMap(service);


                // 保存 LinkedService（LinkedPlanService 的子文档）
                LinkedService linkedService = service.getLinkedService();
                if (linkedService != null) {
                    linkedService.setPlanJoin("linkedService", service.getObjectId());
                    Map<String, Object> linkedServiceDoc = convertToMap(linkedService);
                    addDocument("medicalplans", linkedService.getObjectId(), linkedServiceDoc);
                }

                // 保存 PlanServiceCostShares（LinkedPlanService 的子文档）
                PlanServiceCostShares serviceCostShares = service.getPlanServiceCostShares();
                if (serviceCostShares != null) {
                    serviceCostShares.setPlanJoin("planServiceCostShares", service.getObjectId());
                    Map<String, Object> serviceCostSharesDoc = convertToMap(serviceCostShares);
                    addDocument("medicalplans", serviceCostShares.getObjectId(), serviceCostSharesDoc);
                }
                serviceDoc.remove("linkedService");
                serviceDoc.remove("planServiceCostShares");
                addDocument("medicalplans", service.getObjectId(), serviceDoc);
            }
        }
    }





}
