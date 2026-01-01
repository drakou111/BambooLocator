package dev.drakou111.ui;

import dev.drakou111.Bounds;
import javafx.scene.CacheHint;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Objects;

public class KernelCell extends Pane {

    private enum Bamboo { A, B }
    private Bamboo dragging = null;

    private static final int SCALE = 14;
    private static final int LOGICAL_BLOCK = 16;
    private static final int LOGICAL_BAMBOO = 2;

    private static final int BLOCK_SIZE = LOGICAL_BLOCK * SCALE;
    private static final int BAMBOO_SIZE = LOGICAL_BAMBOO * SCALE;

    private static final double MAX_OFFSET = 0.5;

    private static final double[] OFFSETS = {
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

    private final ImageView bambooA;
    private final ImageView bambooB;

    private final Rectangle regionRect;

    public KernelCell() {
        setPrefSize(BLOCK_SIZE, BLOCK_SIZE);
        setMinSize(BLOCK_SIZE, BLOCK_SIZE);
        setMaxSize(BLOCK_SIZE, BLOCK_SIZE);

        ImageView bgView = new ImageView(loadImage("/cell.png", BLOCK_SIZE));
        bgView.setMouseTransparent(true);

        bambooA = new ImageView(loadImage("/bamboo.png", BAMBOO_SIZE));
        bambooA.setCache(true);
        bambooA.setCacheHint(CacheHint.SPEED);

        bambooB = new ImageView(loadImage("/bamboo.png", BAMBOO_SIZE));
        bambooB.setOpacity(0.85);
        bambooB.setCache(true);
        bambooB.setCacheHint(CacheHint.SPEED);

        regionRect = new Rectangle();
        regionRect.setFill(Color.color(0, 1, 0, 0.25));
        regionRect.setStroke(Color.LIMEGREEN);
        regionRect.getStrokeDashArray().setAll(6.0, 6.0);
        regionRect.setStrokeWidth(2);
        regionRect.setMouseTransparent(true);

        getChildren().addAll(bgView, regionRect, bambooA, bambooB);

        updateVisuals();

        setOnMouseReleased(e -> dragging = null);
        setOnMouseDragged(this::handleDrag);
    }

    private void handleDrag(MouseEvent e) {
        double[] o = mouseToOffset(e);

        if (dragging == null) {
            double distA = distance(ax, az, o[0], o[1]);
            double distB = distance(bx, bz, o[0], o[1]);
            dragging = (distA <= distB) ? Bamboo.A : Bamboo.B;
        }

        if (dragging == Bamboo.A) {
            ax = nearestOffset(o[0]);
            az = nearestOffset(o[1]);
        } else {
            bx = nearestOffset(o[0]);
            bz = nearestOffset(o[1]);
        }

        updateVisuals();
    }

    private double distance(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void updateVisuals() {
        placeBamboo(bambooA, ax, az);
        placeBamboo(bambooB, bx, bz);
        updateRegion();
    }

    private void placeBamboo(ImageView view, double ox, double oz) {
        double center = BLOCK_SIZE / 2.0;
        double px = center + ox * BLOCK_SIZE;
        double py = center + oz * BLOCK_SIZE;

        view.setLayoutX(Math.round(px - BAMBOO_SIZE / 2.0));
        view.setLayoutY(Math.round(py - BAMBOO_SIZE / 2.0));
    }

    private void updateRegion() {
        double minX = Math.min(ax, bx);
        double maxX = Math.max(ax, bx);
        double minZ = Math.min(az, bz);
        double maxZ = Math.max(az, bz);

        double x1 = offsetToPixel(minX);
        double x2 = offsetToPixel(maxX);
        double z1 = offsetToPixel(minZ);
        double z2 = offsetToPixel(maxZ);

        x1 -= BAMBOO_SIZE / 2.0;
        x2 += BAMBOO_SIZE / 2.0;
        z1 -= BAMBOO_SIZE / 2.0;
        z2 += BAMBOO_SIZE / 2.0;

        double w = x2 - x1;
        double h = z2 - z1;

        if (w == 0 && h == 0) {
            regionRect.setVisible(false);
            return;
        }

        regionRect.setVisible(true);
        regionRect.setX(x1);
        regionRect.setY(z1);
        regionRect.setWidth(w);
        regionRect.setHeight(h);
    }

    private double[] mouseToOffset(MouseEvent e) {
        double dx = e.getX() - BLOCK_SIZE / 2.0;
        double dy = e.getY() - BLOCK_SIZE / 2.0;

        double rx = clamp(dx / (BLOCK_SIZE / 2.0));
        double ry = clamp(dy / (BLOCK_SIZE / 2.0));

        return new double[]{rx * MAX_OFFSET, ry * MAX_OFFSET};
    }

    private double offsetToPixel(double o) {
        return Math.round((BLOCK_SIZE / 2.0) + o * BLOCK_SIZE);
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

    private double clamp(double v) {
        return Math.max(-1, Math.min(1, v));
    }

    private Image loadImage(String path, int size) {
        return new Image(
                Objects.requireNonNull(getClass().getResourceAsStream(path)),
                size,
                size,
                false,
                false
        );
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
