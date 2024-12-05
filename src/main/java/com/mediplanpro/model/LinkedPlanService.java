package com.mediplanpro.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@Data
public class LinkedPlanService {

    private LinkedService linkedService;

    private PlanServiceCostShares planServiceCostShares;

    private String objectId;

    private String _org;

    private String objectType;

    @Field(type = FieldType.Object, name = "plan_join")
    @JsonProperty("plan_join")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> planJoin;

    // 设置 plan_join 字段
    public void setPlanJoin(String name, String parent) {
        if (parent == null) {
            this.planJoin = Map.of("name", name);
        } else {
            this.planJoin = Map.of("name", name, "parent", parent);
        }
    }
}
