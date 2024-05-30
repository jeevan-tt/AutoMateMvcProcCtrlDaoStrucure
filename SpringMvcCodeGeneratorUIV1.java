import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SpringMvcCodeGeneratorUIV1 extends JFrame {
    private JCheckBox controllerCheckBox;
    private JCheckBox daoCheckBox;
    private JCheckBox processorCheckBox;
    private JButton generateButton;
    private JTextField tableNameTextField;
    private JCheckBox selectAllCheckBox;

    // Database connection details
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "system";
    private static final String PASSWORD = "root";

    public SpringMvcCodeGeneratorUIV1() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Spring MVC Code Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the checkboxes
        controllerCheckBox = new JCheckBox("Controller");
        daoCheckBox = new JCheckBox("DAO");
        processorCheckBox = new JCheckBox("Processor");

        // Create the generate button
        generateButton = new JButton("Generate");
        generateButton.addActionListener(this::generateButtonActionPerformed);

        // Create the table name text field
        tableNameTextField = new JTextField(20);

        // Create the select all checkbox
        selectAllCheckBox = new JCheckBox("Select All");
        selectAllCheckBox.addItemListener(this::selectAllCheckBoxItemStateChanged);

        // Create the panel for checkboxes
        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new GridLayout(4, 1));
        checkboxPanel.add(controllerCheckBox);
        checkboxPanel.add(daoCheckBox);
        checkboxPanel.add(processorCheckBox);
        checkboxPanel.add(selectAllCheckBox);

        // Create the panel for table name input
        JPanel tablePanel = new JPanel(new FlowLayout());
        tablePanel.add(tableNameTextField);

        // Create the panel for generate button
        JPanel generatePanel = new JPanel(new FlowLayout());
        generatePanel.add(generateButton);

        // Create the main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(tablePanel, BorderLayout.NORTH);
        mainPanel.add(checkboxPanel, BorderLayout.CENTER);
        mainPanel.add(generatePanel, BorderLayout.SOUTH);

        // Add the main panel to the frame
        setContentPane(mainPanel);
        pack();
    }

    private void generateButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // Check if any option is selected
        if (!controllerCheckBox.isSelected() && !daoCheckBox.isSelected() && !processorCheckBox.isSelected()) {
            JOptionPane.showMessageDialog(this, "Please select at least one option to generate.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if a table name is entered
        String tableName = tableNameTextField.getText().trim();
        if (tableName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a table name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Destination Path");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Show the file chooser dialog
        int result = fileChooser.showOpenDialog(this);

        // Check if a directory is selected
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            String destinationPath = selectedDir.getAbsolutePath();

            try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String entityClassContent = generateEntityClassContent(connection, tableName, destinationPath);

                // Write the entity class to the selected directory
                writeFile(destinationPath, toClassName(tableName) + "DO.java", entityClassContent);

                // Generate files based on selected options
                if (controllerCheckBox.isSelected()) {
                    String controllerFileContent = generateControllerFileContent(tableName);
                    writeFile(destinationPath, toClassName(tableName) + "Ctrl.java", controllerFileContent);
                }

                if (daoCheckBox.isSelected()) {
                    String daoFileContent = generateDAOFileContent(tableName);
                    writeFile(destinationPath, toClassName(tableName) + "DAO.java", daoFileContent);
                }

                if (processorCheckBox.isSelected()) {
                    String processorFileContent = generateProcessorFileContent(tableName);
                    writeFile(destinationPath, toClassName(tableName) + "Proc.java", processorFileContent);
                }

                JOptionPane.showMessageDialog(this, "Java files generated successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void selectAllCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {
        boolean selected = selectAllCheckBox.isSelected();
        controllerCheckBox.setSelected(selected);
        daoCheckBox.setSelected(selected);
        processorCheckBox.setSelected(selected);
    }

    private String generateEntityClassContent(Connection connection, String tableName, String destinationPath) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null);

        StringBuilder entityClass = new StringBuilder();
        String className = toClassName(tableName) + "DO";
        entityClass.append("import javax.persistence.*;\n");
        entityClass.append("import java.math.BigDecimal;\n");
        entityClass.append("import java.sql.Date;\n");
        entityClass.append("import java.sql.Timestamp;\n");
        entityClass.append("import java.util.List;\n");
        entityClass.append("\n");
        entityClass.append("@Entity\n");
        entityClass.append("@Table(name = \"").append(tableName).append("\")\n");
        entityClass.append("public class ").append(className).append(" {\n");

        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String columnType = columns.getString("TYPE_NAME");

            String javaType = mapToJavaType(columnType);
            entityClass.append("    @Column(name = \"").append(columnName).append("\")\n");
            entityClass.append("    private ").append(javaType).append(" ").append(toCamelCase(columnName)).append(";\n");
        }

        // Retrieve and process foreign key relationships
        ResultSet foreignKeys = metaData.getImportedKeys(null, null, tableName.toUpperCase());
        while (foreignKeys.next()) {
            String pkTableName = foreignKeys.getString("PKTABLE_NAME");
            String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
            String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");

            // Generate child entity class if not already generated
            String childEntityClassContent = generateEntityClassContent(connection, pkTableName, destinationPath);
            writeFile(destinationPath, toClassName(pkTableName) + "DO.java", childEntityClassContent);

            // Add association mapping in the parent entity class
            entityClass.append("    @ManyToOne(fetch = FetchType.LAZY)\n");
            entityClass.append("    @JoinColumn(name = \"").append(fkColumnName).append("\", referencedColumnName = \"").append(pkColumnName).append("\")\n");
            entityClass.append("    private ").append(toClassName(pkTableName)).append("DO ").append(toCamelCase(pkTableName)).append(";\n");
        }

        // Add getters and setters
        addGettersAndSetters(entityClass, tableName);

        entityClass.append("}\n");

        // Generate corresponding DTO class
        String dtoClassContent = generateDTOClassContent(connection, tableName);
        writeFile(destinationPath, toClassName(tableName) + "DTO.java", dtoClassContent);

        return entityClass.toString();
    }

    private String toClassName(String tableName) {
        tableName = tableName.toLowerCase(Locale.ROOT);
        StringBuilder className = new StringBuilder();
        boolean toUpperCase = true;
        for (char ch : tableName.toCharArray()) {
            if (ch == '_') {
                toUpperCase = true;
            } else if (toUpperCase) {
                className.append(Character.toUpperCase(ch));
                toUpperCase = false;
            } else {
                className.append(ch);
            }
        }
        if (className.toString().endsWith("Tx")) {
            className.setLength(className.length() - 2);
        }
        return className.toString();
    }

    private String toCamelCase(String name) {
        StringBuilder camelCase = new StringBuilder();
        boolean toUpperCase = false;
        for (char ch : name.toCharArray()) {
            if (ch == '_') {
                toUpperCase = true;
            } else if (toUpperCase) {
                camelCase.append(Character.toUpperCase(ch));
                toUpperCase = false;
            } else {
                camelCase.append(Character.toLowerCase(ch));
            }
        }
        return camelCase.toString();
    }

    private String mapToJavaType(String sqlType) {
        switch (sqlType.toUpperCase()) {
            case "VARCHAR":
            case "VARCHAR2":
            case "CHAR":
            case "NVARCHAR2":
            case "LONG":
                return "String";
            case "NUMBER":
                return "BigDecimal";
            case "DATE":
                return "Date";
            case "TIMESTAMP":
                return "Timestamp";
            case "BLOB":
                return "byte[]";
            default:
                throw new IllegalArgumentException("Unsupported SQL type: " + sqlType);
        }
    }

    private void addGettersAndSetters(StringBuilder entityClass, String tableName) {
        entityClass.append("\n");
        entityClass.append("    // Getters and Setters\n");

        String className = toClassName(tableName) + "DO";
        entityClass.append("    public ").append(className).append("() {}\n");

        // Retrieve all fields
        int startIndex = entityClass.indexOf("private ");
        while (startIndex != -1) {
            int endIndex = entityClass.indexOf(";", startIndex);
            String fieldDeclaration = entityClass.substring(startIndex, endIndex);
            String[] parts = fieldDeclaration.split(" ");
            String fieldType = parts[1];
            String fieldName = parts[2];

            // Generate getter
            entityClass.append("    public ").append(fieldType).append(" get")
                .append(Character.toUpperCase(fieldName.charAt(0))).append(fieldName.substring(1)).append("() {\n")
                .append("        return ").append(fieldName).append(";\n")
                .append("    }\n");

            // Generate setter
            entityClass.append("    public void set").append(Character.toUpperCase(fieldName.charAt(0)))
                .append(fieldName.substring(1)).append("(").append(fieldType).append(" ").append(fieldName)
                .append(") {\n")
                .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                .append("    }\n");

            startIndex = entityClass.indexOf("private ", endIndex);
        }
    }

    private String generateDTOClassContent(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null);

        StringBuilder dtoClass = new StringBuilder();
        String className = toClassName(tableName) + "DTO";
        dtoClass.append("import java.math.BigDecimal;\n");
        dtoClass.append("import java.sql.Date;\n");
        dtoClass.append("import java.sql.Timestamp;\n");
        dtoClass.append("\n");
        dtoClass.append("public class ").append(className).append(" {\n");

        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String columnType = columns.getString("TYPE_NAME");

            String javaType = mapToJavaType(columnType);
            dtoClass.append("    private ").append(javaType).append(" ").append(toCamelCase(columnName)).append(";\n");
        }

        // Add getters and setters
        addGettersAndSetters(dtoClass, tableName);

        dtoClass.append("}\n");

        return dtoClass.toString();
    }

    private void writeFile(String destinationPath, String fileName, String content) {
        try (FileWriter writer = new FileWriter(new File(destinationPath, fileName))) {
            writer.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateControllerFileContent(String tableName) {
        String className = toClassName(tableName) + "Ctrl";
        return "import org.springframework.web.bind.annotation.*;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import java.util.List;\n" +
                "\n" +
                "@RestController\n" +
                "@RequestMapping(\"/" + toCamelCase(tableName) + "s\")\n" +
                "public class " + className + " {\n" +
                "\n" +
                "    @Autowired\n" +
                "    private " + toClassName(tableName) + "Service service;\n" +
                "\n" +
                "    @GetMapping\n" +
                "    public List<" + toClassName(tableName) + "DTO> getAll() {\n" +
                "        return service.getAll();\n" +
                "    }\n" +
                "\n" +
                "    @GetMapping(\"/{id}\")\n" +
                "    public " + toClassName(tableName) + "DTO getById(@PathVariable Long id) {\n" +
                "        return service.getById(id);\n" +
                "    }\n" +
                "\n" +
                "    @PostMapping\n" +
                "    public " + toClassName(tableName) + "DTO create(@RequestBody " + toClassName(tableName) + "DTO dto) {\n" +
                "        return service.create(dto);\n" +
                "    }\n" +
                "\n" +
                "    @PutMapping(\"/{id}\")\n" +
                "    public " + toClassName(tableName) + "DTO update(@PathVariable Long id, @RequestBody " + toClassName(tableName) + "DTO dto) {\n" +
                "        return service.update(id, dto);\n" +
                "    }\n" +
                "\n" +
                "    @DeleteMapping(\"/{id}\")\n" +
                "    public ResponseEntity<?> delete(@PathVariable Long id) {\n" +
                "        service.delete(id);\n" +
                "        return ResponseEntity.ok().build();\n" +
                "    }\n" +
                "}\n";
    }

    private String generateDAOFileContent(String tableName) {
        String className = toClassName(tableName) + "DAO";
        return "import org.springframework.data.jpa.repository.JpaRepository;\n" +
                "import org.springframework.stereotype.Repository;\n" +
                "\n" +
                "@Repository\n" +
                "public interface " + className + " extends JpaRepository<" + toClassName(tableName) + "DO, Long> {\n" +
                "}\n";
    }

    private String generateProcessorFileContent(String tableName) {
        String className = toClassName(tableName) + "Proc";
        return "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "import java.util.List;\n" +
                "import java.util.stream.Collectors;\n" +
                "\n" +
                "@Service\n" +
                "public class " + className + " {\n" +
                "\n" +
                "    @Autowired\n" +
                "    private " + toClassName(tableName) + "DAO dao;\n" +
                "\n" +
                "    public List<" + toClassName(tableName) + "DTO> getAll() {\n" +
                "        return dao.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());\n" +
                "    }\n" +
                "\n" +
                "    public " + toClassName(tableName) + "DTO getById(Long id) {\n" +
                "        return dao.findById(id).map(this::convertToDTO).orElse(null);\n" +
                "    }\n" +
                "\n" +
                "    public " + toClassName(tableName) + "DTO create(" + toClassName(tableName) + "DTO dto) {\n" +
                "        " + toClassName(tableName) + "DO entity = convertToEntity(dto);\n" +
                "        entity = dao.save(entity);\n" +
                "        return convertToDTO(entity);\n" +
                "    }\n" +
                "\n" +
                "    public " + toClassName(tableName) + "DTO update(Long id, " + toClassName(tableName) + "DTO dto) {\n" +
                "        " + toClassName(tableName) + "DO entity = convertToEntity(dto);\n" +
                "        entity.setId(id);\n" +
                "        entity = dao.save(entity);\n" +
                "        return convertToDTO(entity);\n" +
                "    }\n" +
                "\n" +
                "    public void delete(Long id) {\n" +
                "        dao.deleteById(id);\n" +
                "    }\n" +
                "\n" +
                "    private " + toClassName(tableName) + "DTO convertToDTO(" + toClassName(tableName) + "DO entity) {\n" +
                "        // Conversion logic\n" +
                "        return new " + toClassName(tableName) + "DTO();\n" +
                "    }\n" +
                "\n" +
                "    private " + toClassName(tableName) + "DO convertToEntity(" + toClassName(tableName) + "DTO dto) {\n" +
                "        // Conversion logic\n" +
                "        return new " + toClassName(tableName) + "DO();\n" +
                "    }\n" +
                "}\n";
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            new SpringMvcCodeGeneratorUIV1().setVisible(true);
        });
    }
}
