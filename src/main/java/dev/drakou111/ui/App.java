package dev.drakou111.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import dev.drakou111.BlockType;
import dev.drakou111.Bounds;
import dev.drakou111.Kernel;
import dev.drakou111.bruteforce.BruteforceMode;
import dev.drakou111.bruteforce.Bruteforcer;
import dev.drakou111.bruteforce.cpu.MultiThreadBruteforcer;
import dev.drakou111.bruteforce.cpu.SingleThreadBruteforcer;
import dev.drakou111.bruteforce.gpu.CudaInfo;
import dev.drakou111.bruteforce.gpu.JCudaBruteforcer;
import dev.drakou111.utils.BambooOffsets;
import dev.drakou111.utils.KernelEstimator;

import javax.swing.*;
import java.awt.*;

public class App {

    private Bruteforcer bruteforce;
    private SwingWorker<Void, Void> worker;

    private KernelCell[][] cells;
    private JPanel gridPanel;
    private int squareSize = 4;

    JLabel etaLabel;
    private JProgressBar progressBar;
    private Timer progressTimer;

    private static final int MAX_GRID_PIXEL = 800;

    public void open() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ignored) {}

        JFrame frame = new JFrame("Bamboo Kernel Bruteforcer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        SpinnerNumberModel gridModel = new SpinnerNumberModel(squareSize, 1, 16, 1);
        JSpinner gridSpinner = new JSpinner(gridModel);

        JComboBox<BruteforceMode> modeCombo = new JComboBox<>(BruteforceMode.values());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Grid size:"));
        topPanel.add(gridSpinner);
        topPanel.add(Box.createHorizontalStrut(12));
        topPanel.add(new JLabel("Mode:"));
        topPanel.add(modeCombo);

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

        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        SpinnerNumberModel tileWModel = new SpinnerNumberModel(64, 1, 4096, 1);
        SpinnerNumberModel tileHModel = new SpinnerNumberModel(64, 1, 4096, 1);
        SpinnerNumberModel cudaThreadsModel = new SpinnerNumberModel(256, 32, 1024, 32);
        SpinnerNumberModel cudaBlocksModel = new SpinnerNumberModel(2048, 1, 65536, 1);

        JSpinner tileWSpinner = new JSpinner(tileWModel);
        JSpinner tileHSpinner = new JSpinner(tileHModel);
        JSpinner cudaThreadsSpinner = new JSpinner(cudaThreadsModel);
        JSpinner cudaBlocksSpinner = new JSpinner(cudaBlocksModel);

        int defaultThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        SpinnerNumberModel mtThreadsModel = new SpinnerNumberModel(defaultThreads, 1, 512, 1);
        JSpinner mtThreadsSpinner = new JSpinner(mtThreadsModel);

        paramsPanel.add(new JLabel("Tile W:")); paramsPanel.add(tileWSpinner);
        paramsPanel.add(new JLabel("Tile H:")); paramsPanel.add(tileHSpinner);
        paramsPanel.add(new JLabel("CUDA threads:")); paramsPanel.add(cudaThreadsSpinner);
        paramsPanel.add(new JLabel("CUDA blocks:")); paramsPanel.add(cudaBlocksSpinner);
        paramsPanel.add(Box.createHorizontalStrut(12));
        paramsPanel.add(new JLabel("MT threads:")); paramsPanel.add(mtThreadsSpinner);

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

        etaLabel = new JLabel("ETA: --");
        etaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        etaLabel.setVisible(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);
        progressBar.setVisible(false);

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));

        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        etaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressPanel.add(progressBar);
        progressPanel.add(Box.createVerticalStrut(2));
        progressPanel.add(etaLabel);

        JPanel terminalWithProgress = new JPanel(new BorderLayout(4, 4));
        terminalWithProgress.add(terminalScroll, BorderLayout.CENTER);
        terminalWithProgress.add(progressPanel, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(rangePane);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(paramsPanel);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(buttons);

        JPanel bottomPanel = new JPanel(new BorderLayout(6,6));
        bottomPanel.add(controlPanel, BorderLayout.NORTH);
        bottomPanel.add(terminalWithProgress, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout(12,12));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(gridWithDirections, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        frame.setContentPane(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        terminal.setText("");
        boolean cuda = CudaInfo.isCudaAvailable();
        CudaInfo.DeviceInfo dev;
        if (cuda) {
            dev = CudaInfo.queryDevice();
            if (dev != null) {
                terminal.append("CUDA detected: SMs=" + dev.smCount() + ", maxThreads/block=" + dev.maxThreadsPerBlock() + "\n");

                int suggestedThreads = Math.min(256, dev.maxThreadsPerBlock());
                int suggestedBlocks = Math.max(256, dev.smCount() * 32);
                cudaThreadsModel.setValue(suggestedThreads);
                cudaBlocksModel.setValue(suggestedBlocks);

                tileWModel.setValue(64);
                tileHModel.setValue(64);
            }
            terminal.append("Default mode: JCUDA\n");
        } else {
            int cores = Runtime.getRuntime().availableProcessors();
            terminal.append("CUDA not available\n");
            terminal.append("CPU cores: " + cores + "\n");
            BruteforceMode defaultMode = (cores > 4) ? BruteforceMode.MULTI_THREAD : BruteforceMode.SINGLE_THREAD;
            modeCombo.setSelectedItem(defaultMode);
            terminal.append("Default mode: " + defaultMode + "\n");
            modeCombo.removeItem(BruteforceMode.CUDA);
        }

        if (cuda) modeCombo.setSelectedItem(BruteforceMode.CUDA);

        gridSpinner.addChangeListener(e -> {
            squareSize = (int) gridModel.getNumber();
            rebuildGrid();
            gridWithDirections.revalidate();
            gridWithDirections.repaint();
            frame.pack();
        });

        modeCombo.addActionListener(e -> {
            BruteforceMode m = (BruteforceMode) modeCombo.getSelectedItem();
            boolean isCuda = m == BruteforceMode.CUDA;
            boolean isMulti = m == BruteforceMode.MULTI_THREAD;

            tileWSpinner.setEnabled(isCuda);
            tileHSpinner.setEnabled(isCuda);
            cudaThreadsSpinner.setEnabled(isCuda);
            cudaBlocksSpinner.setEnabled(isCuda);

            mtThreadsSpinner.setEnabled(isMulti);
        });

        startBtn.addActionListener(e -> startBruteforce(
                terminal, startBtn, stopBtn, modeCombo,
                tileWModel, tileHModel, cudaThreadsModel, cudaBlocksModel,
                mtThreadsModel, minX, maxX, minZ, maxZ
                                                       ));

        stopBtn.addActionListener(e -> {
            if (bruteforce != null) bruteforce.stop();
            if (worker != null) worker.cancel(true);
            terminal.append("Stopped.\n");
            stopBtn.setEnabled(false);
            modeCombo.setEnabled(true);
            stopProgressTimer();
            progressBar.setVisible(false);
        });

        modeCombo.getActionListeners()[0].actionPerformed(null);
    }

    private void rebuildGrid() {
        gridPanel.removeAll();
        int perCell = Math.max(24, MAX_GRID_PIXEL / Math.max(1, squareSize));
        gridPanel.setPreferredSize(new Dimension(perCell * squareSize, perCell * squareSize));
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

    private void startBruteforce(JTextArea terminal, JButton startBtn, JButton stopBtn, JComboBox<BruteforceMode> modeCombo,
                                 SpinnerNumberModel tileWModel, SpinnerNumberModel tileHModel,
                                 SpinnerNumberModel cudaThreadsModel, SpinnerNumberModel cudaBlocksModel,
                                 SpinnerNumberModel mtThreadsModel,
                                 JTextField minX, JTextField maxX, JTextField minZ, JTextField maxZ) {

        terminal.setText("");

        final double FULL_EDGE = 0.25;
        int minCellX = squareSize, minCellY = squareSize;
        int maxCellX = -1, maxCellY = -1;
        for (int y = 0; y < squareSize; y++) {
            for (int x = 0; x < squareSize; x++) {
                Bounds r = cells[y][x].getRegion();
                boolean isFull = r.minX <= -FULL_EDGE && r.maxX >= FULL_EDGE && r.minZ <= -FULL_EDGE && r.maxZ >= FULL_EDGE;
                if (!isFull) {
                    minCellX = Math.min(minCellX, x);
                    minCellY = Math.min(minCellY, y);
                    maxCellX = Math.max(maxCellX, x);
                    maxCellY = Math.max(maxCellY, y);
                }
            }
        }

        if (maxCellX == -1) {
            terminal.append("Region ignored: please specify constraints.\n");
            return;
        }

        Bounds[][] kernelArr = new Bounds[maxCellY - minCellY + 1][maxCellX - minCellX + 1];
        for (int y = 0; y < kernelArr.length; y++) {
            for (int x = 0; x < kernelArr[0].length; x++) {
                kernelArr[y][x] = cells[minCellY + y][minCellX + x].toBounds();
            }
        }
        Kernel k = new Kernel(kernelArr, BlockType.BAMBOO0);

        int x0, x1, z0, z1;
        try {
            x0 = Integer.parseInt(minX.getText().trim());
            x1 = Integer.parseInt(maxX.getText().trim());
            z0 = Integer.parseInt(minZ.getText().trim());
            z1 = Integer.parseInt(maxZ.getText().trim());
        } catch (NumberFormatException ex) {
            terminal.append("Invalid numeric range.\n");
            return;
        }

        KernelEstimator.Result est = KernelEstimator.estimate(kernelArr, BambooOffsets.OFFSETS, x0, x1, z0, z1);
        terminal.append("---- Probability estimates ----\n");
        terminal.append("Chance of ≥1 hit: " + String.format("%.6f%%", est.probability() * 100) + "\n");
        terminal.append("Expected occurrences: " + est.expectedCount() + "\n");
        terminal.append("-------------------------------\n");

        Logger logger = msg -> SwingUtilities.invokeLater(() -> {
            terminal.append(msg + "\n");
            terminal.setCaretPosition(terminal.getDocument().getLength());
        });

        BruteforceMode mode = (BruteforceMode) modeCombo.getSelectedItem();
        terminal.append("Mode: " + mode + "\n");

        int tileW = (int) tileWModel.getNumber();
        int tileH = (int) tileHModel.getNumber();
        int cudaThreads = (int) cudaThreadsModel.getNumber();
        int cudaBlocks = (int) cudaBlocksModel.getNumber();
        int mtThreads = (int) mtThreadsModel.getNumber();

        switch (mode) {
            case SINGLE_THREAD -> bruteforce = new SingleThreadBruteforcer(k, x0, x1, z0, z1, logger);
            case MULTI_THREAD -> bruteforce = new MultiThreadBruteforcer(k, x0, x1, z0, z1, logger, mtThreads);
            case CUDA -> bruteforce = new JCudaBruteforcer(k, x0, x1, z0, z1, logger, tileW, tileH, cudaThreads, cudaBlocks);
        }

        long startTimeMs = System.currentTimeMillis();
        bruteforce.setProgressListener(progress ->
            SwingUtilities.invokeLater(() -> {
                int percent = (int) Math.round(progress * 100.0);
                percent = Math.max(0, Math.min(100, percent));
                progressBar.setValue(percent);
                progressBar.setString(percent + "%");

                if (progress > 0.0001) {
                    long now = System.currentTimeMillis();
                    long elapsedMs = now - startTimeMs;
                    long etaMs = (long)(elapsedMs * (1.0 - progress) / progress);
                    etaLabel.setText("ETA: " + formatDuration(etaMs));
                } else {
                    etaLabel.setText("ETA: --");
                }
            })
        );

        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                progressBar.setValue(0);
                progressBar.setVisible(true);
                etaLabel.setText("ETA: --");
                etaLabel.setVisible(true);
                bruteforce.run();
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                etaLabel.setVisible(false);

                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                modeCombo.setEnabled(true);

                try { get(); }
                catch (Exception ex) {
                    //ignore
                }
                terminal.append("Finished.\n");
            }
        };

        worker.execute();
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        modeCombo.setEnabled(false);
    }

    private static String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long s = totalSeconds % 60;
        long m = (totalSeconds / 60) % 60;
        long h = totalSeconds / 3600;

        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }

    private void stopProgressTimer() {
        if (progressTimer != null) {
            progressTimer.stop();
            progressTimer = null;
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new App().open());
    }
}
