package com.mediplanpro.service;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class JsonSchemaValidatorService {

    private Schema schema;

    // 初始化时加载 JSON Schema
    public JsonSchemaValidatorService() {
        InputStream schemaStream = getClass().getResourceAsStream("/schemas/medicalPlanSchema.json");
        JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
        this.schema = SchemaLoader.load(rawSchema);
    }

    public void validate(JSONObject data) throws ValidationException {
        schema.validate(data);
    }
}

