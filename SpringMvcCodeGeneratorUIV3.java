package com.test.swing.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class SpringMvcCodeGeneratorUIV3 extends JFrame {
	private JCheckBox controllerCheckBox;
	private JCheckBox daoCheckBox;
	private JCheckBox processorCheckBox;
	private JButton generateButton;
	private JTextField entityFileTextField;
	private File entityFile;
	private JCheckBox selectAllCheckBox;

	public SpringMvcCodeGeneratorUIV3() {
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

		// Create the entity file text field
		entityFileTextField = new JTextField(20);
		entityFileTextField.setEditable(false);

		// Create the browse button
		JButton browseButton = new JButton("Browse");
		browseButton.addActionListener(this::browseButtonActionPerformed);

		// Create the select all checkbox
		selectAllCheckBox = new JCheckBox("Select All");
		selectAllCheckBox.addItemListener(this::selectAllCheckBoxItemStateChanged);

		/*
		 * selectAllCheckBox = new JCheckBox("Select All");
		 * selectAllCheckBox.setBounds(10, 150, 100, 25);
		 * selectAllCheckBox.addItemListener((ItemListener) this);
		 */

		// Create the panel for checkboxes
		JPanel checkboxPanel = new JPanel();
		checkboxPanel.setLayout(new GridLayout(4, 1));
		checkboxPanel.add(controllerCheckBox);
		checkboxPanel.add(daoCheckBox);
		checkboxPanel.add(processorCheckBox);
		checkboxPanel.add(selectAllCheckBox);

		// Create the panel for entity file and browse button
		JPanel entityPanel = new JPanel(new FlowLayout());
		entityPanel.add(entityFileTextField);
		entityPanel.add(browseButton);

		// Create the panel for generate button
		JPanel generatePanel = new JPanel(new FlowLayout());
		generatePanel.add(generateButton);

		// Create the main panel
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		mainPanel.add(entityPanel, BorderLayout.NORTH);
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

		// Check if an entity file is selected
		if (entityFile == null || !entityFile.exists()) {
			JOptionPane.showMessageDialog(this, "Please select an entity file.", "Error", JOptionPane.ERROR_MESSAGE);
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

			// Generate files based on selected options
			if (controllerCheckBox.isSelected()) {
				String controllerFileContent = generateControllerFileContent(getEntityName());
				writeFile(destinationPath, getEntityName() + "Ctrl.java", controllerFileContent);
			}

			if (daoCheckBox.isSelected()) {
				String daoFileContent = generateDAOFileContent(getEntityName());
				writeFile(destinationPath, getEntityName() + "DAO.java", daoFileContent);
			}

			if (processorCheckBox.isSelected()) {
				String processorFileContent = generateProcessorFileContent(getEntityName());
				writeFile(destinationPath, getEntityName() + "Proc.java", processorFileContent);
			}

			JOptionPane.showMessageDialog(this, "Java files generated successfully!");
		}
	}

	private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			entityFile = fileChooser.getSelectedFile();
			entityFileTextField.setText(entityFile.getAbsolutePath());
		}
	}

	private void selectAllCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {
		if (selectAllCheckBox.isSelected()) {
			controllerCheckBox.setSelected(true);
			daoCheckBox.setSelected(true);
			processorCheckBox.setSelected(true);
		} else {
			controllerCheckBox.setSelected(false);
			daoCheckBox.setSelected(false);
			processorCheckBox.setSelected(false);
		}
	}

	private String getEntityName() {
		String entityFileName = entityFile.getName();
		int dotIndex = entityFileName.lastIndexOf('.');
		if (dotIndex > 0) {
			return entityFileName.substring(0, dotIndex);
		}
		return entityFileName;
	}

	private String generateControllerFileContent(String entityName) {
		// Controller file content generation logic
		return ""; // Replace with actual code
	}

	private String generateDAOFileContent(String entityName) {
		// DAO file content generation logic
		return ""; // Replace with actual code
	}

	private String generateProcessorFileContent(String entityName) {
		// Processor file content generation logic
		return ""; // Replace with actual code
	}

	/*
	 * private void writeFile(String destinationPath, String fileName, String
	 * fileContent) { // Write the file to the destination path // Replace with
	 * actual file writing code }
	 */
	private void writeFile(String destinationPath, String fileName, String fileContent) {
		try {
			// Create the destination directory if it doesn't exist
			File directory = new File(destinationPath);
			if (!directory.exists()) {
				directory.mkdirs();
			}

			// Construct the file path
			String filePath = destinationPath + File.separator + fileName;

			// Write the file content to the file
			FileWriter fileWriter = new FileWriter(filePath);
			fileWriter.write(fileContent);
			fileWriter.close();

			System.out.println("File generated successfully: " + filePath);
		} catch (IOException e) {
			System.out.println("Error generating file: " + e.getMessage());
		}
	}

	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(() -> {
			new SpringMvcCodeGeneratorUIV3().setVisible(true);
		});
	}
}
