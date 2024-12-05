package com.mediplanpro.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;



import org.springframework.data.redis.core.index.Indexed;

import java.util.List;
import java.util.Map;

@Data
@Document(indexName = "medicalplans")
public class MedicalPlan {

    @Id
    @JsonProperty("objectId")
    @Indexed
    private String objectId;

    @JsonProperty("planType")
    private String planType;

    @JsonProperty("creationDate")
    private String creationDate;

    @JsonProperty("planCostShares")
    private PlanCostShares planCostShares;

    private String objectType;

    @JsonProperty("linkedPlanServices")
    private List<LinkedPlanService> linkedPlanServices;

    @JsonProperty("_org")
    private String _org;

    @Field(type = FieldType.Object, name = "plan_join")
    @JsonProperty("plan_join")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> planJoin;

    // 设置 join 字段
    public void setPlanJoin(String name) {
        this.planJoin = Map.of("name", name);
    }
}
