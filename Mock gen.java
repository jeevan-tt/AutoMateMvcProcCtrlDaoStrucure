package com.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SpringBootCodeGenerator {

    private static final String BASE_PACKAGE = "com.app.generated";
    private static final String OUTPUT_DIR = "src/main/java/" + BASE_PACKAGE.replace('.', '/');
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            new ApiDefinition(  );

        generateSourceCode(apiList);
    }

    public static void generateSourceCode(List<ApiDefinition> apiList) {
        System.out.println("Starting code generation...");

        for (ApiDefinition api : apiList) {
            String baseDtoName = getBaseName(api);
            processJsonToDto(api.requestJson(), baseDtoName + "RequestDto");
            processJsonToDto(api.responseJson(), baseDtoName + "ResponseDto");
        }

        generateMockController(apiList);

        System.out.println("Mock Controller & DTOs successfully generated at: " + Paths.get(OUTPUT_DIR).toAbsolutePath());
    }

    // ==========================================
    // Single Mock Controller Generator
    // ==========================================

    private static void generateMockController(List<ApiDefinition> apis) {
        StringBuilder methods = new StringBuilder();

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

            if (responseDto.equals("Void")) {
                methods.append("        return ResponseEntity.ok().build();\n");
            } else {
                String builderCode = generateLombokBuilderCode(responseDto, api.responseJson(), "        ");
                methods.append("        ").append(responseDto).append(" mockResponse = ").append(builderCode).append(";\n");
                methods.append("        return ResponseEntity.ok(mockResponse);\n");
            }

            methods.append("    }\n\n");
        }

        String content = "package " + BASE_PACKAGE + ".controller;\n\n"
                + "import " + BASE_PACKAGE + ".dto.*;\n"
                + "import jakarta.validation.Valid;\n"
                + "import org.springframework.http.ResponseEntity;\n"
                + "import org.springframework.web.bind.annotation.*;\n"
                + "import java.util.List;\n\n"
                + "@RestController\n"
                + "public class MockApiController {\n\n"
                + methods
                + "}\n";

        writeFile("controller", "MockApiController.java", content);
    }

    // ==========================================
    // JSON to Builder Code Converter
    // ==========================================

    private static String generateLombokBuilderCode(String targetClassName, String json, String indent) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            return buildObjectNode(targetClassName, root, indent);
        } catch (Exception e) {
            return "new " + targetClassName + "()";
        }
    }

    private static String buildObjectNode(String className, JsonNode node, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(".builder()\n");

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode fieldVal = entry.getValue();

            sb.append(indent).append("    .").append(fieldName).append("(")
              .append(buildFieldValue(fieldName, fieldVal, indent + "    "))
              .append(")\n");
        }

        sb.append(indent).append("    .build()");
        return sb.toString();
    }

    private static String buildFieldValue(String fieldName, JsonNode node, String indent) {
        switch (node.getNodeType()) {
            case STRING:
                return "\"" + node.asText().replace("\"", "\\\"") + "\"";
            case BOOLEAN:
                return String.valueOf(node.asBoolean());
            case NUMBER:
                if (node.isIntegralNumber()) {
                    return node.asText();
                } else {
                    return node.asText() + "d";
                }
            case OBJECT:
                String nestedClassName = toPascalCase(fieldName) + "Dto";
                return buildObjectNode(nestedClassName, node, indent);
            case ARRAY:
                if (node.isEmpty()) {
                    return "List.of()";
                }
                StringBuilder listSb = new StringBuilder("List.of(\n");
                for (int i = 0; i < node.size(); i++) {
                    JsonNode elem = node.get(i);
                    listSb.append(indent).append("    ");
                    if (elem.isObject()) {
                        String itemClassName = toPascalCase(fieldName.replaceAll("s$", "")) + "Dto";
                        listSb.append(buildObjectNode(itemClassName, elem, indent + "    "));
                    } else {
                        listSb.append(buildFieldValue(fieldName, elem, indent));
                    }
                    if (i < node.size() - 1) listSb.append(",");
                    listSb.append("\n");
                }
                listSb.append(indent).append(")");
                return listSb.toString();
            default:
                return "null";
        }
    }

    // ==========================================
    // DTO Generator
    // ==========================================

    private static void processJsonToDto(String json, String rootClassName) {
        if (!hasBody(json)) return;

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
            if (className.endsWith("RequestDto") && !javaType.equalsIgnoreCase("Boolean")) {
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

        for (Map.Entry<String, JsonNode> nested : nestedClasses.entrySet()) {
            generateDtoClass(nested.getKey(), nested.getValue());
        }
    }

    private static String inferJavaType(String fieldName, JsonNode node, Map<String, JsonNode> nestedClasses) {
        switch (node.getNodeType()) {
            case STRING:
                return "String";
            case BOOLEAN:
                return "Boolean";
            case NUMBER:
                return node.isIntegralNumber() ? (node.canConvertToInt() ? "Integer" : "Long") : "Double";
            case OBJECT:
                String nestedClassName = toPascalCase(fieldName) + "Dto";
                nestedClasses.put(nestedClassName, node);
                return nestedClassName;
            case ARRAY:
                if (!node.isEmpty()) {
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
    // String & File Helpers
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
            Path targetDir = Paths.get(OUTPUT_DIR, subFolder);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(fileName);
            Files.writeString(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Generated: " + subFolder + "/" + fileName);
        } catch (IOException e) {
            System.err.println("Error writing " + fileName + ": " + e.getMessage());
        }
    }
}
