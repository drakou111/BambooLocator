package dev.drakou111.ui;

import dev.drakou111.Bounds;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class KernelCell extends JPanel {

    private enum Bamboo { A, B }
    private Bamboo dragging = null;

    private static final int LOGICAL_BLOCK = 16;
    private static final int LOGICAL_BAMBOO = 2;
    private static final double MAX_OFFSET = 0.5;

    private final double[] OFFSETS = {
            -0.25,
            -0.21666666865348816,
            -0.18333333730697632,
            -0.15000000596046448,
            -0.11666667461395264,
            -0.0833333432674408,
            -0.050000011920928955,
            -0.016666680574417114,
            0.016666680574417114,
            0.050000011920928955,
            0.0833333432674408,
            0.11666667461395264,
            0.15000000596046448,
            0.18333333730697632,
            0.21666666865348816,
            0.25
    };

    private double ax = OFFSETS[0], az = OFFSETS[0];
    private double bx = OFFSETS[OFFSETS.length - 1], bz = OFFSETS[OFFSETS.length - 1];

    // pixel sizes (per-instance)
    private final int blockSizePx;
    private final int bambooSizePx;

    private final BufferedImage bgImg;
    private final BufferedImage bambooImg;

    public KernelCell(int blockSizePx) {
        this.blockSizePx = Math.max(24, blockSizePx);
        this.bambooSizePx = Math.max(4, (this.blockSizePx * LOGICAL_BAMBOO) / LOGICAL_BLOCK);

        setPreferredSize(new Dimension(this.blockSizePx, this.blockSizePx));
        setMinimumSize(new Dimension(this.blockSizePx, this.blockSizePx));
        setLayout(null);

        bgImg = loadImage("/cell.png", this.blockSizePx);
        bambooImg = loadImage("/bamboo.png", this.bambooSizePx);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                double[] o = mouseToOffset(e.getX(), e.getY());
                double distA = distance(ax, az, o[0], o[1]);
                double distB = distance(bx, bz, o[0], o[1]);
                dragging = (distA <= distB) ? Bamboo.A : Bamboo.B;

                if (dragging == Bamboo.A) {
                    ax = nearestOffset(o[0]);
                    az = nearestOffset(o[1]);
                } else {
                    bx = nearestOffset(o[0]);
                    bz = nearestOffset(o[1]);
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging == null) return;
                double[] o = mouseToOffset(e.getX(), e.getY());
                if (dragging == Bamboo.A) {
                    ax = nearestOffset(o[0]);
                    az = nearestOffset(o[1]);
                } else {
                    bx = nearestOffset(o[0]);
                    bz = nearestOffset(o[1]);
                }
                repaint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private BufferedImage loadImage(String path, int size) {
        try {
            var is = getClass().getResourceAsStream(path);
            if (is == null) throw new IOException("Resource not found: " + path);
            BufferedImage orig = ImageIO.read(is);
            Image scaled = orig.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buf.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();
            return buf;
        } catch (IOException ex) {
            BufferedImage buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buf.createGraphics();
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, size, size);
            g.setColor(Color.RED);
            g.drawString("NO IMG", 4, 12);
            g.dispose();
            return buf;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();

        g.drawImage(bgImg, 0, 0, this.blockSizePx, this.blockSizePx, null);

        Rectangle region = computeRegionRect();
        if (region != null) {
            g.setColor(new Color(0, 255, 0, 100));
            g.fill(region);
            Stroke old = g.getStroke();
            g.setColor(new Color(50, 205, 50));
            float[] dash = {6f, 6f};
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g.draw(region);
            g.setStroke(old);
        }

        Point pa = offsetToPixelPoint(ax, az);
        Point pb = offsetToPixelPoint(bx, bz);

        g.drawImage(bambooImg, pa.x - bambooSizePx / 2, pa.y - bambooSizePx / 2, null);
        Composite oldComp = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g.drawImage(bambooImg, pb.x - bambooSizePx / 2, pb.y - bambooSizePx / 2, null);
        g.setComposite(oldComp);

        g.dispose();
    }

    private Rectangle computeRegionRect() {
        double minX = Math.min(ax, bx);
        double maxX = Math.max(ax, bx);
        double minZ = Math.min(az, bz);
        double maxZ = Math.max(az, bz);

        double x1 = offsetToPixel(minX) - bambooSizePx / 2.0;
        double x2 = offsetToPixel(maxX) + bambooSizePx / 2.0;
        double z1 = offsetToPixel(minZ) - bambooSizePx / 2.0;
        double z2 = offsetToPixel(maxZ) + bambooSizePx / 2.0;

        double w = x2 - x1;
        double h = z2 - z1;

        if (w == 0 && h == 0) return null;
        return new Rectangle((int) Math.round(x1), (int) Math.round(z1),
                             (int) Math.round(w), (int) Math.round(h));
    }

    private Point offsetToPixelPoint(double ox, double oz) {
        double center = blockSizePx / 2.0;
        double px = center + ox * blockSizePx;
        double py = center + oz * blockSizePx;
        return new Point((int) Math.round(px), (int) Math.round(py));
    }

    private double offsetToPixel(double o) {
        return (blockSizePx / 2.0) + o * blockSizePx;
    }

    private double[] mouseToOffset(int mouseX, int mouseY) {
        double dx = mouseX - blockSizePx / 2.0;
        double dy = mouseY - blockSizePx / 2.0;

        double rx = clamp(dx / (blockSizePx / 2.0));
        double ry = clamp(dy / (blockSizePx / 2.0));

        return new double[]{rx * MAX_OFFSET, ry * MAX_OFFSET};
    }

    private double nearestOffset(double v) {
        double best = OFFSETS[0];
        double bestDist = Math.abs(v - best);
        for (double o : OFFSETS) {
            double d = Math.abs(v - o);
            if (d < bestDist) {
                bestDist = d;
                best = o;
            }
        }
        return best;
    }

    private double distance(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }

    public Bounds getRegion() {
        return new Bounds(
                Math.min(ax, bx),
                Math.max(ax, bx),
                Math.min(az, bz),
                Math.max(az, bz)
        );
    }

    public Bounds toBounds() {
        return new Bounds(
                Math.min(ax, bx),
                Math.max(ax, bx),
                Math.min(az, bz),
                Math.max(az, bz)
        );
    }
}
