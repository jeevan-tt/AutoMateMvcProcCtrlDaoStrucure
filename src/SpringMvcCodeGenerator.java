import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SpringMvcCodeGenerator {
	public static void main(String[] args) {
		try {
			// Input name from console
			java.util.Scanner scanner = new java.util.Scanner(System.in);
			System.out.print("Enter TABLE  name: ");
			String name = scanner.nextLine();

			// Convert input name to camelCase
			String pascalCaseName = convertToCamelCase(name);
			pascalCaseName = pascalCaseName.substring(0, pascalCaseName.length() - 2);

			String camelCaseName = pascalCaseName.substring(0, 1).toLowerCase()
					+ pascalCaseName.substring(1, pascalCaseName.length());

			System.out.println(name);
			System.out.println(pascalCaseName);
			System.out.println(camelCaseName);
			/* System.out.println(pascalCaseName); */

			// Generate Controller file
			generateControllerFile(pascalCaseName);

			// Generate Processor file
			generateProcessorFile(pascalCaseName);

			// Generate DAO file
			generateDaoFile(pascalCaseName);

			System.out.println("Java files generated successfully!");

			scanner.close();
		} catch (IOException e) {
			System.out.println("Error generating Java files: " + e.getMessage());
		}
	}

	public static String convertToCamelCase(String name) {
		String[] words = name.split("_");
		StringBuilder pascalCaseName = new StringBuilder();
		for (String word : words) {
			pascalCaseName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
		}
		return pascalCaseName.toString();
	}

	/*
	 * public static String convertToPascalCase(String name) { String[] words =
	 * name.split("_"); StringBuilder pascalCaseName = new StringBuilder(); for
	 * (String word : words) { pascalCaseName.append(word.substring(0,
	 * 1).toLowerCase()).append(word.substring(1).toLowerCase()); } return
	 * pascalCaseName.toString(); }
	 */

	public static void generateControllerFile(String name) throws IOException {
		String fileName = name + "Ctrl.java";
		String fileContent = "import org.springframework.stereotype.Controller;\n"
				+ "import org.springframework.web.bind.annotation.GetMapping;\n"
				+ "import org.springframework.web.bind.annotation.PostMapping;\n"
				+ "import org.springframework.web.bind.annotation.RequestMapping;\n\n" + "@Controller\n"
				+ "@RequestMapping(\"/" + name.toLowerCase() + "\")\n" + "public class " + name + "Ctrl {\n"
				+ "    @GetMapping(\"/fetch\")\n" + "    public String fetch() {\n" + "        // Fetch logic here\n"
				+ "        return \"fetchResult\";\n" + "    }\n\n" + "    @PostMapping(\"/save\")\n"
				+ "    public String save() {\n" + "        // Save logic here\n" + "        return \"saveResult\";\n"
				+ "    }\n" + "}\n";
		generateFile(fileName, fileContent);
	}

	public static void generateProcessorFile(String name) throws IOException {
		String fileName = name + "Proc.java";
		String fileContent = "public class " + name + "Proc {\n" + "    public String processData() {\n"
				+ "        // Process data logic here\n" + "        return \"processedData\";\n" + "    }\n" + "}\n";
		generateFile(fileName, fileContent);
	}

	public static void generateDaoFile(String name) throws IOException {
		String fileName = name + "DAO.java";
		String fileContent = "public class " + name + "DAO {\n" + "    public void fetchFromDB() {\n"
				+ "        // Fetch from DB logic here\n" + "    }\n\n" + "    public void saveToDB() {\n"
				+ "        // Save to DB logic here\n" + "    }\n" + "}\n";
		generateFile(fileName, fileContent);
	}

	public static void generateFile(String fileName, String fileContent) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(fileName));
		writer.println(fileContent);
		writer.close();
	}
}
