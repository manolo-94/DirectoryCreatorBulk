package com.lpcode.direcotrycreator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * JavaFX App
 */
public class App extends Application {
    private Label folderPathLabel, filePathLabel, fileCountLabel;
    private ProgressBar progressBar;
    private String selectedFolderPath = "", selectedFilePath = "";

    @Override
    public void start(Stage primaryStage) {
        VBox vbox = new VBox(10);
        vbox.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        Label instructionLabel = new Label("Seleccione la carpeta raíz y su archivo CSV");
        
        Button selectFolderButton = new Button("Seleccionar Carpeta Raíz");
        folderPathLabel = new Label("Carpeta no seleccionada");
        selectFolderButton.setOnAction(e -> selectFolder(primaryStage));
        
        Button selectFileButton = new Button("Seleccionar Archivo CSV");
        filePathLabel = new Label("Archivo no seleccionado");
        fileCountLabel = new Label("");
        selectFileButton.setOnAction(e -> selectFile(primaryStage));
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        
        Button generateButton = new Button("Generar Directorios");
        generateButton.setOnAction(e -> {
            try {
                generateDirectories();
            } catch (CsvValidationException ex) {
                ex.printStackTrace();
            }
        });
        
        vbox.getChildren().addAll(instructionLabel, selectFolderButton, folderPathLabel, selectFileButton, filePathLabel, fileCountLabel, progressBar, generateButton);
        
        Scene scene = new Scene(vbox, 400, 350);
        primaryStage.setTitle("Generador de Directorios");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void selectFolder(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            selectedFolderPath = selectedDirectory.getAbsolutePath();
            folderPathLabel.setText("Carpeta: " + selectedFolderPath);
        }
    }

    private void selectFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedFilePath = file.getAbsolutePath();
            filePathLabel.setText("Archivo: " + selectedFilePath);
            int count = getCSVRowCount(selectedFilePath);
            fileCountLabel.setText("Elementos en CSV: " + count);
        }
    }

    private int getCSVRowCount(String csvFile) {
        int count = 0;
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            while (reader.readNext() != null) count++;
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return count - 1;
    }

    private void generateDirectories() throws CsvValidationException {
        if (selectedFolderPath.isEmpty() || selectedFilePath.isEmpty()) {
            showAlert("Error", "Seleccione una carpeta y un archivo CSV.");
            return;
        }
        
        List<String> subdirectories = readCSV(selectedFilePath);
        if (subdirectories.isEmpty()) {
            showAlert("Error", "El CSV no contiene datos.");
            return;
        }
        
        int total = subdirectories.size(), created = 0;
        StringBuilder log = new StringBuilder();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        
        for (int i = 0; i < total; i++) {
        String dirName = subdirectories.get(i);
        File directory = new File(selectedFolderPath, dirName);
        if (!directory.exists() && directory.mkdir()) {
            log.append("Éxito: ").append(directory.getAbsolutePath()).append("\n");
            created++;
        } else {
            log.append("Error: ").append(directory.getAbsolutePath()).append("\n");
        }
        
        // Actualizar la barra de progreso después de cada 10 directorios creados
        if (i % 10 == 0 || i == total - 1) {
            final int progress = created;
            Platform.runLater(() -> progressBar.setProgress((double) progress / total));
        }
    }
        
        writeLog(log.toString(), selectedFolderPath);
        showAlert("Proceso Finalizado", "Directorios creados: " + created);
        clearForm();
    }

    private List<String> readCSV(String csvFile) throws CsvValidationException {
        List<String> subdirectories = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] nextLine;
            boolean firstLine = true;
            while ((nextLine = reader.readNext()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                if (nextLine.length > 0) subdirectories.add(nextLine[0]);
            }
        } catch (IOException e) {
            showAlert("Error", "Error al leer el CSV.");
        }
        return subdirectories;
    }

    private void writeLog(String log, String folderPath) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File logFile = new File(folderPath, "log_directorios_creados_" + timestamp + ".txt");
        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            out.println(log);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        Platform.runLater(() -> {
            folderPathLabel.setText("Carpeta no seleccionada");
            filePathLabel.setText("Archivo no seleccionado");
            fileCountLabel.setText("");
            progressBar.setProgress(0);
            selectedFolderPath = "";
            selectedFilePath = "";
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}