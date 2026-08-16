package com.codegen.codegenerator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class SpringBootCodeGenerator {

    // Target configuration
    private static final String BASE_PACKAGE = "com.app.generated";
    private static final String OUTPUT_DIR = "C:\\Users\\GANA\\Downloads\\CodeBase\\" + BASE_PACKAGE.replace('.', '/');
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Data structure for API specifications
    public record ApiDefinition(
            String feature,
            String subFeature,
            String httpMethod,
            String apiUrl,
            String requestJson,
            String responseJson
    ) {}

    public static void main(String[] args) {
        List<ApiDefinition> apiList = List.of(


                new ApiDefinition(
                        "DEV",
                        "Dev2",
                        "GET",
                        "/api/getlist",
                        "{}",
                        """
                        {
                            "totalRecords": 45,
                            "pageNumber": 1,
                            "pageSize": 10,
                            "applications": [
                                {
                                    "app": "",
                                    "applicant": "",
                                    "amount": 1,
                                    "status": ""
                                }
                            ]
                        }
                        """
                )
        );

        generateSourceCode(apiList);
    }

    public static void generateSourceCode(List<ApiDefinition> apiList) {
        System.out.println("Starting code generation...");

        // Group APIs by Controller Name (based on Feature)
        Map<String, List<ApiDefinition>> controllers = new LinkedHashMap<>();

        for (ApiDefinition api : apiList) {
            String controllerName = toPascalCase(api.feature()) + "Controller";
            controllers.computeIfAbsent(controllerName, k -> new ArrayList<>()).add(api);

            // 1. Process Request & Response DTOs
            String baseDtoName = getBaseName(api);
            processJsonToDto(api.requestJson(), baseDtoName + "RequestDto");
            processJsonToDto(api.responseJson(), baseDtoName + "ResponseDto");
        }

        // 2. Generate Controllers & Services
        for (Map.Entry<String, List<ApiDefinition>> entry : controllers.entrySet()) {
            String controllerName = entry.getKey();
            String featureName = controllerName.replace("Controller", "");
            String serviceName = featureName + "Service";

            generateServiceFile(serviceName, entry.getValue());
            generateControllerFile(controllerName, serviceName, entry.getValue());
        }

        System.out.println("Code generation finished successfully at: " + Paths.get(OUTPUT_DIR).toAbsolutePath());
    }

    // ==========================================
    // JSON to DTO Engine (Recursion & Types)
    // ==========================================

    private static void processJsonToDto(String json, String rootClassName) {
        if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) {
            return; // Empty body, no DTO generation required
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root.isObject()) {
                generateDtoClass(rootClassName, root);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse JSON for " + rootClassName + ": " + e.getMessage());
        }
    }

    private static void generateDtoClass(String className, JsonNode node) {
        StringBuilder fields = new StringBuilder();
        Map<String, JsonNode> nestedClasses = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String fieldName = entry.getKey();
            JsonNode fieldVal = entry.getValue();

            String javaType = inferJavaType(fieldName, fieldVal, nestedClasses);

            fields.append("    @JsonProperty(\"").append(fieldName).append("\")\n");
            if (className.endsWith("RequestDto") && !javaType.equals("Boolean") && !javaType.equals("boolean")) {
                fields.append("    @NotNull\n");
            }
            fields.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n\n");
        }

        StringBuilder classContent = new StringBuilder();
        classContent.append("package ").append(BASE_PACKAGE).append(".dto;\n\n")
                .append("import com.fasterxml.jackson.annotation.JsonProperty;\n")
                .append("import jakarta.validation.constraints.NotNull;\n")
                .append("import lombok.Data;\n")
                .append("import lombok.Builder;\n")
                .append("import lombok.NoArgsConstructor;\n")
                .append("import lombok.AllArgsConstructor;\n")
                .append("import java.util.List;\n\n")
                .append("@Data\n")
                .append("@Builder\n")
                .append("@NoArgsConstructor\n")
                .append("@AllArgsConstructor\n")
                .append("public class ").append(className).append(" {\n\n")
                .append(fields)
                .append("}\n");

        writeFile("dto", className + ".java", classContent.toString());

        // Recursively generate child nested DTO classes
        for (Map.Entry<String, JsonNode> nested : nestedClasses.entrySet()) {
            generateDtoClass(nested.getKey(), nested.getValue());
        }
    }

    private static String inferJavaType(String fieldName, JsonNode node, Map<String, JsonNode> nestedClasses) {
        JsonNodeType type = node.getNodeType();
        switch (type) {
            case STRING:
                return "String";
            case BOOLEAN:
                return "Boolean";
            case NUMBER:
                if (node.isIntegralNumber()) {
                    return node.canConvertToInt() ? "Integer" : "Long";
                } else {
                    return "Double";
                }
            case OBJECT:
                String nestedClassName = toPascalCase(fieldName) + "Dto";
                nestedClasses.put(nestedClassName, node);
                return nestedClassName;
            case ARRAY:
                if (node.size() > 0) {
                    JsonNode firstElement = node.get(0);
                    if (firstElement.isObject()) {
                        String itemClassName = toPascalCase(fieldName.replaceAll("s$", "")) + "Dto";
                        nestedClasses.put(itemClassName, firstElement);
                        return "List<" + itemClassName + ">";
                    } else {
                        return "List<" + inferJavaType(fieldName, firstElement, nestedClasses) + ">";
                    }
                }
                return "List<Object>";
            default:
                return "Object";
        }
    }

    // ==========================================
    // Controller & Service Skeleton Generators
    // ==========================================

    private static void generateControllerFile(String controllerName, String serviceName, List<ApiDefinition> apis) {
        StringBuilder methods = new StringBuilder();
        String serviceVar = toCamelCase(serviceName);

        for (ApiDefinition api : apis) {
            String baseName = getBaseName(api);
            String methodName = toCamelCase(baseName);
            String requestDto = hasBody(api.requestJson()) ? baseName + "RequestDto" : null;
            String responseDto = hasBody(api.responseJson()) ? baseName + "ResponseDto" : "Void";

            String mappingAnnotation = switch (api.httpMethod().toUpperCase()) {
                case "GET" -> "@GetMapping(\"" + api.apiUrl() + "\")";
                case "POST" -> "@PostMapping(\"" + api.apiUrl() + "\")";
                case "PUT" -> "@PutMapping(\"" + api.apiUrl() + "\")";
                case "DELETE" -> "@DeleteMapping(\"" + api.apiUrl() + "\")";
                default -> "@RequestMapping(value = \"" + api.apiUrl() + "\", method = RequestMethod." + api.httpMethod().toUpperCase() + ")";
            };

            methods.append("    ").append(mappingAnnotation).append("\n");
            methods.append("    public ResponseEntity<").append(responseDto).append("> ").append(methodName).append("(");

            if (requestDto != null) {
                methods.append("@Valid @RequestBody ").append(requestDto).append(" request");
            }

            methods.append(") {\n");
            methods.append("        ").append(responseDto).append(" response = ").append(serviceVar).append(".").append(methodName).append("(");
            if (requestDto != null) {
                methods.append("request");
            }
            methods.append(");\n");
            methods.append("        return ResponseEntity.ok(response);\n");
            methods.append("    }\n\n");
        }

        String content = "package " + BASE_PACKAGE + ".controller;\n\n"
                + "import " + BASE_PACKAGE + ".dto.*;\n"
                + "import " + BASE_PACKAGE + ".service." + serviceName + ";\n"
                + "import jakarta.validation.Valid;\n"
                + "import lombok.RequiredArgsConstructor;\n"
                + "import org.springframework.http.ResponseEntity;\n"
                + "import org.springframework.web.bind.annotation.*;\n\n"
                + "@RestController\n"
                + "@RequiredArgsConstructor\n"
                + "public class " + controllerName + " {\n\n"
                + "    private final " + serviceName + " " + serviceVar + ";\n\n"
                + methods
                + "}\n";

        writeFile("controller", controllerName + ".java", content);
    }

    private static void generateServiceFile(String serviceName, List<ApiDefinition> apis) {
        StringBuilder methods = new StringBuilder();

        for (ApiDefinition api : apis) {
            String baseName = getBaseName(api);
            String methodName = toCamelCase(baseName);
            String requestDto = hasBody(api.requestJson()) ? baseName + "RequestDto" : null;
            String responseDto = hasBody(api.responseJson()) ? baseName + "ResponseDto" : "Void";

            methods.append("    ").append(responseDto).append(" ").append(methodName).append("(");
            if (requestDto != null) {
                methods.append(requestDto).append(" request");
            }
            methods.append(");\n\n");
        }

        String content = "package " + BASE_PACKAGE + ".service;\n\n"
                + "import " + BASE_PACKAGE + ".dto.*;\n\n"
                + "public interface " + serviceName + " {\n\n"
                + methods
                + "}\n";

        writeFile("service", serviceName + ".java", content);
    }

    // ==========================================
    // String & Utility Helpers
    // ==========================================

    private static boolean hasBody(String json) {
        return json != null && !json.trim().isEmpty() && !json.trim().equals("{}");
    }

    private static String getBaseName(ApiDefinition api) {
        String sub = (api.subFeature() == null || api.subFeature().trim().isEmpty())
                ? ""
                : api.subFeature().trim();

        if (sub.isEmpty() || sub.equalsIgnoreCase(api.feature())) {
            return toPascalCase(api.feature());
        }
        return toPascalCase(api.feature()) + toPascalCase(sub);
    }

    private static String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String word : input.replaceAll("[^a-zA-Z0-9]", " ").split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1));
                }
            }
        }
        return sb.toString();
    }

    private static String toCamelCase(String input) {
        String pascal = toPascalCase(input);
        if (pascal.isEmpty()) return "";
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    private static void writeFile(String subFolder, String fileName, String content) {
        try {
            File dir = new File(OUTPUT_DIR + "/" + subFolder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            System.out.println("Generated: " + subFolder + "/" + fileName);
        } catch (IOException e) {
            System.err.println("Error writing " + fileName + ": " + e.getMessage());
        }
    }
}