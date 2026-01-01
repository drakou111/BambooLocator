package dev.drakou111.ui;

import dev.drakou111.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    private Bruteforce bruteforce;
    private Thread bruteforceThread;
    private final int SQUARE_SIZE = 4;

    @Override
    public void start(Stage stage) {

        GridPane grid = new GridPane();
        KernelCell[][] cells = new KernelCell[SQUARE_SIZE][SQUARE_SIZE];

        for (int y = 0; y < SQUARE_SIZE; y++) {
            for (int x = 0; x < SQUARE_SIZE; x++) {
                KernelCell cell = new KernelCell();
                cells[y][x] = cell;
                grid.add(cell, x, y);
            }
        }

        Label northLabel = new Label("Z- / North");
        Label southLabel = new Label("Z+ / South");
        Label westLabel  = new Label("X- / West");
        Label eastLabel  = new Label("X+ / East");

        northLabel.setStyle("-fx-font-weight: bold;");
        southLabel.setStyle("-fx-font-weight: bold;");
        westLabel.setStyle("-fx-font-weight: bold;");
        eastLabel.setStyle("-fx-font-weight: bold;");

        BorderPane gridWithDirections = new BorderPane();
        gridWithDirections.setCenter(grid);
        gridWithDirections.setTop(northLabel);
        gridWithDirections.setBottom(southLabel);
        gridWithDirections.setLeft(westLabel);
        gridWithDirections.setRight(eastLabel);

        BorderPane.setAlignment(northLabel, Pos.CENTER);
        BorderPane.setAlignment(southLabel, Pos.CENTER);
        BorderPane.setAlignment(westLabel, Pos.CENTER);
        BorderPane.setAlignment(eastLabel, Pos.CENTER);

        BorderPane.setMargin(northLabel, new Insets(0, 0, 6, 0));
        BorderPane.setMargin(southLabel, new Insets(6, 0, 0, 0));
        BorderPane.setMargin(westLabel,  new Insets(0, 6, 0, 0));
        BorderPane.setMargin(eastLabel,  new Insets(0, 0, 0, 6));

        TextField minX = new TextField("-10000");
        TextField maxX = new TextField("10000");
        TextField minZ = new TextField("-10000");
        TextField maxZ = new TextField("10000");

        minX.setPrefWidth(90);
        maxX.setPrefWidth(90);
        minZ.setPrefWidth(90);
        maxZ.setPrefWidth(90);

        GridPane rangePane = new GridPane();
        rangePane.setHgap(8);
        rangePane.setVgap(6);
        rangePane.addRow(0, new Label("X min"), minX, new Label("X max"), maxX);
        rangePane.addRow(1, new Label("Z min"), minZ, new Label("Z max"), maxZ);

        Button startBtn = new Button("Start Bruteforce");
        Button stopBtn = new Button("Stop");
        stopBtn.setDisable(true);

        TextArea terminal = new TextArea();
        terminal.setEditable(false);
        terminal.setWrapText(true);
        terminal.setPrefRowCount(6);
        terminal.setStyle("""
            -fx-font-family: monospace;
            -fx-font-size: 12px;
        """);

        startBtn.setOnAction(e -> {
            terminal.setText("");

            final double FULL_EDGE = 0.25;
            int minCellX = SQUARE_SIZE;
            int minCellY = SQUARE_SIZE;
            int maxCellX = -1;
            int maxCellY = -1;

            for (int y = 0; y < SQUARE_SIZE; y++) {
                for (int x = 0; x < SQUARE_SIZE; x++) {
                    Bounds r = cells[y][x].getRegion();

                    boolean isFull =
                            r.minX <= -FULL_EDGE &&
                            r.maxX >=  FULL_EDGE &&
                            r.minZ <= -FULL_EDGE &&
                            r.maxZ >=  FULL_EDGE;

                    if (!isFull) {
                        minCellX = Math.min(minCellX, x);
                        minCellY = Math.min(minCellY, y);
                        maxCellX = Math.max(maxCellX, x);
                        maxCellY = Math.max(maxCellY, y);
                    }
                }
            }

            if (maxCellX == -1) {
                terminal.appendText("Region ignored: please specify at least one constraint.\n");
                return;
            }

            int croppedWidth  = maxCellX - minCellX + 1;
            int croppedHeight = maxCellY - minCellY + 1;

            terminal.appendText(String.format(
                    "Region cropped to %dx%d (from %d,%d to %d,%d)\n",
                    croppedWidth, croppedHeight,
                    minCellX, minCellY, maxCellX, maxCellY
                                             ));

            Bounds[][] kernel = new Bounds[croppedHeight][croppedWidth];
            for (int y = 0; y < croppedHeight; y++) {
                for (int x = 0; x < croppedWidth; x++) {
                    kernel[y][x] = cells[minCellY + y][minCellX + x].toBounds();
                }
            }

            Kernel k = new Kernel(kernel, BlockType.BAMBOO0);

            final int x0, x1, z0, z1;
            try {
                x0 = Integer.parseInt(minX.getText().trim());
                x1 = Integer.parseInt(maxX.getText().trim());
                z0 = Integer.parseInt(minZ.getText().trim());
                z1 = Integer.parseInt(maxZ.getText().trim());
            } catch (NumberFormatException ex) {
                terminal.appendText("Invalid numeric range input.\n");
                return;
            }

            Logger logger = message -> Platform.runLater(() -> {
                terminal.appendText(message + "\n");
                terminal.setScrollTop(Double.MAX_VALUE);
            });

            bruteforce = new Bruteforce(k, x0, x1, z0, z1, logger);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    bruteforce.run();
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
            });

            task.setOnCancelled(ev -> {
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
            });

            task.setOnFailed(ev -> {
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
                Throwable ex = task.getException();
                terminal.appendText("Bruteforce failed: " +
                                    (ex != null ? ex.getMessage() : "unknown") + "\n");
            });

            bruteforceThread = new Thread(task, "Bamboo-Thread");
            bruteforceThread.setDaemon(true);
            bruteforceThread.start();

            startBtn.setDisable(true);
            stopBtn.setDisable(false);
        });

        stopBtn.setOnAction(e -> {
            if (bruteforce != null) {
                bruteforce.stop();
                terminal.appendText("Stopped.\n");
            }
            stopBtn.setDisable(true);
        });

        HBox buttons = new HBox(10, startBtn, stopBtn);

        VBox root = new VBox(12, gridWithDirections, rangePane, buttons, terminal);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/dark-theme.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Bamboo Locator");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
