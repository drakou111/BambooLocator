package dev.drakou111.ui;

import dev.drakou111.*;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

public class App {

    private Bruteforce bruteforce;
    private SwingWorker<Void, Void> worker;

    private KernelCell[][] cells;
    private JPanel gridPanel;
    private int squareSize = 4;

    private static final int MAX_GRID_PIXEL = 800;

    public void open() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {}

        JFrame frame = new JFrame("Bamboo Kernel Bruteforcer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        SpinnerNumberModel gridModel = new SpinnerNumberModel(squareSize, 1, 16, 1);
        JSpinner gridSpinner = new JSpinner(gridModel);
        JLabel spinnerLabel = new JLabel("Grid size:");
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(spinnerLabel);
        topPanel.add(gridSpinner);

        gridPanel = new JPanel();
        rebuildGrid();

        JLabel northLabel = new JLabel("Z- / North", SwingConstants.CENTER);
        JLabel southLabel = new JLabel("Z+ / South", SwingConstants.CENTER);
        JLabel westLabel  = new JLabel("X- / West", SwingConstants.CENTER);
        JLabel eastLabel  = new JLabel("X+ / East", SwingConstants.CENTER);

        northLabel.setFont(northLabel.getFont().deriveFont(Font.BOLD));
        southLabel.setFont(southLabel.getFont().deriveFont(Font.BOLD));
        westLabel.setFont(westLabel.getFont().deriveFont(Font.BOLD));
        eastLabel.setFont(eastLabel.getFont().deriveFont(Font.BOLD));

        JPanel gridWithDirections = new JPanel(new BorderLayout());
        gridWithDirections.add(northLabel, BorderLayout.NORTH);
        gridWithDirections.add(southLabel, BorderLayout.SOUTH);
        gridWithDirections.add(westLabel, BorderLayout.WEST);
        gridWithDirections.add(eastLabel, BorderLayout.EAST);
        gridWithDirections.add(gridPanel, BorderLayout.CENTER);
        gridWithDirections.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JTextField minX = new JTextField("-10000", 8);
        JTextField maxX = new JTextField("10000", 8);
        JTextField minZ = new JTextField("-10000", 8);
        JTextField maxZ = new JTextField("10000", 8);

        JPanel rangePane = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4,4,4,4);
        gbc.gridx = 0; gbc.gridy = 0; rangePane.add(new JLabel("X min"), gbc);
        gbc.gridx = 1; rangePane.add(minX, gbc);
        gbc.gridx = 2; rangePane.add(new JLabel("X max"), gbc);
        gbc.gridx = 3; rangePane.add(maxX, gbc);

        gbc.gridx = 0; gbc.gridy = 1; rangePane.add(new JLabel("Z min"), gbc);
        gbc.gridx = 1; rangePane.add(minZ, gbc);
        gbc.gridx = 2; rangePane.add(new JLabel("Z max"), gbc);
        gbc.gridx = 3; rangePane.add(maxZ, gbc);

        JButton startBtn = new JButton("Start Bruteforce");
        JButton stopBtn  = new JButton("Stop");
        stopBtn.setEnabled(false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(startBtn);
        buttons.add(stopBtn);

        JTextArea terminal = new JTextArea(8,48);
        terminal.setEditable(false);
        terminal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane terminalScroll = new JScrollPane(terminal);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(rangePane);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buttons);

        JPanel bottomPanel = new JPanel(new BorderLayout(6,6));
        bottomPanel.add(controlPanel, BorderLayout.NORTH);
        bottomPanel.add(terminalScroll, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout(12,12));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(gridWithDirections, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        gridSpinner.addChangeListener(e -> {
            squareSize = (int) gridModel.getNumber();
            rebuildGrid();
            gridWithDirections.removeAll();
            gridWithDirections.add(northLabel, BorderLayout.NORTH);
            gridWithDirections.add(southLabel, BorderLayout.SOUTH);
            gridWithDirections.add(westLabel, BorderLayout.WEST);
            gridWithDirections.add(eastLabel, BorderLayout.EAST);
            gridWithDirections.add(gridPanel, BorderLayout.CENTER);
            gridWithDirections.revalidate();
            gridWithDirections.repaint();
            frame.pack();
        });

        startBtn.addActionListener(e -> {
            terminal.setText("");

            final double FULL_EDGE = 0.25;
            int minCellX = squareSize;
            int minCellY = squareSize;
            int maxCellX = -1;
            int maxCellY = -1;

            for (int y = 0; y < squareSize; y++) {
                for (int x = 0; x < squareSize; x++) {
                    Bounds r = cells[y][x].getRegion();
                    boolean isFull =
                            r.minX <= -FULL_EDGE &&
                            r.maxX >= FULL_EDGE &&
                            r.minZ <= -FULL_EDGE &&
                            r.maxZ >= FULL_EDGE;
                    if (!isFull) {
                        minCellX = Math.min(minCellX, x);
                        minCellY = Math.min(minCellY, y);
                        maxCellX = Math.max(maxCellX, x);
                        maxCellY = Math.max(maxCellY, y);
                    }
                }
            }

            if (maxCellX == -1) {
                terminal.append("Region ignored: please specify at least one constraint.\n");
                return;
            }

            int croppedWidth  = maxCellX - minCellX + 1;
            int croppedHeight = maxCellY - minCellY + 1;
            terminal.append(String.format("Region cropped to %dx%d (from %d,%d to %d,%d)\n",
                                          croppedWidth, croppedHeight, minCellX, minCellY, maxCellX, maxCellY));

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
                terminal.append("Invalid numeric range input.\n");
                return;
            }

            Logger logger = message -> SwingUtilities.invokeLater(() -> {
                terminal.append(message+"\n");
                terminal.setCaretPosition(terminal.getDocument().getLength());
            });

            bruteforce = new Bruteforce(k, x0, x1, z0, z1, logger);

            worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() { bruteforce.run(); return null; }
                @Override
                protected void done() {
                    startBtn.setEnabled(true);
                    stopBtn.setEnabled(false);
                    try { get(); } catch (InterruptedException | ExecutionException ex) {
                        terminal.append("Bruteforce failed: "+ex.getMessage()+"\n");
                        ex.printStackTrace();
                    }
                    terminal.append("Bruteforce finished.\n");
                }
            };

            worker.execute();
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
        });

        stopBtn.addActionListener(e -> {
            if (bruteforce != null) bruteforce.stop();
            if (worker != null) worker.cancel(true);
            terminal.append("Stop requested.\n");
            stopBtn.setEnabled(false);
        });
    }

    private void rebuildGrid() {
        gridPanel.removeAll();
        int perCell = Math.max(24, MAX_GRID_PIXEL / Math.max(1, squareSize));
        int totalGridPx = perCell * squareSize;
        gridPanel.setPreferredSize(new Dimension(totalGridPx, totalGridPx));
        gridPanel.setLayout(new GridLayout(squareSize, squareSize, 4, 4));
        cells = new KernelCell[squareSize][squareSize];
        for (int y = 0; y < squareSize; y++) {
            for (int x = 0; x < squareSize; x++) {
                KernelCell cell = new KernelCell(perCell);
                cells[y][x] = cell;
                gridPanel.add(cell);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            new App().open();
        });
    }
}
