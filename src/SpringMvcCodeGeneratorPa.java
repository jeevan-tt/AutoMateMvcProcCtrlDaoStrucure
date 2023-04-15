import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SpringMvcCodeGeneratorPa {
    public static void main(String[] args) {
        try {
            // Input name from console
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.print("Enter name: ");
            String name = scanner.nextLine();
            
            // Generate Controller file
            generateControllerFile(name);
            
            // Generate Processor file
            generateProcessorFile(name);
            
            // Generate DAO file
            generateDaoFile(name);
            
            System.out.println("Java files generated successfully!");
            
            scanner.close();
        } catch (IOException e) {
            System.out.println("Error generating Java files: " + e.getMessage());
        }
    }

    public static void generateControllerFile(String name) throws IOException {
        String fileName = name + "Ctrl.java";
        String fileContent = "import org.springframework.stereotype.Controller;\n"
                + "import org.springframework.web.bind.annotation.GetMapping;\n"
                + "import org.springframework.web.bind.annotation.PostMapping;\n"
                + "import org.springframework.web.bind.annotation.RequestMapping;\n\n"
                + "@Controller\n"
                + "@RequestMapping(\"/" + name.toLowerCase() + "\")\n"
                + "public class " + name + "Ctrl {\n"
                + "    @GetMapping(\"/fetch\")\n"
                + "    public String fetch() {\n"
                + "        // Fetch logic here\n"
                + "        return \"fetchResult\";\n"
                + "    }\n\n"
                + "    @PostMapping(\"/save\")\n"
                + "    public String save() {\n"
                + "        // Save logic here\n"
                + "        return \"saveResult\";\n"
                + "    }\n"
                + "}\n";
        generateFile(fileName, fileContent);
    }

    public static void generateProcessorFile(String name) throws IOException {
        String fileName = name + "Proc.java";
        String fileContent = "public class " + name + "Proc {\n"
                + "    public String processData() {\n"
                + "        // Process data logic here\n"
                + "        return \"processedData\";\n"
                + "    }\n"
                + "}\n";
        generateFile(fileName, fileContent);
    }

    public static void generateDaoFile(String name) throws IOException {
        String fileName = name + "DAO.java";
        String fileContent = "public class " + name + "DAO {\n"
                + "    public void fetchFromDB() {\n"
                + "        // Fetch from DB logic here\n"
                + "    }\n\n"
                + "    public void saveToDB() {\n"
                + "        // Save to DB logic here\n"
                + "    }\n"
                + "}\n";
        generateFile(fileName, fileContent);
    }

    public static void generateFile(String fileName, String fileContent) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(fileName));
        writer.println(fileContent);
        writer.close();
    }
}
