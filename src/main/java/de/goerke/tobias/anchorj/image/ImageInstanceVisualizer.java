package de.goerke.tobias.anchorj.image;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.AnchorResult;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * May be used to visualize an image instance.
 */
public class ImageInstanceVisualizer {

    private final ImageInstance originalInstance;

    /**
     * Creates the instance.
     *
     * @param originalInstance the image instance that is to be explained
     */
    public ImageInstanceVisualizer(ImageInstance originalInstance) {
        if (originalInstance == null)
            throw new IllegalArgumentException("Original instance" + ParameterValidation.NULL_MESSAGE);
        this.originalInstance = originalInstance;
    }

    /**
     * Opens a {@link JFrame} showing a single image for visualization purposes.
     *
     * @param type the {@link ImageRepresentation}
     */
    public static void showOnScreen(ImageRepresentation type) {
        JFrame frame = new JFrame();
        ImageIcon icon = new ImageIcon(type.asByteArray());
        JLabel label = new JLabel(icon);
        frame.add(label);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Takes a result, i.e. a set of features, and draws the pixels assigned to these features only.
     * Thereby, only the result features are to be seen.
     *
     * @param features the features to be shown
     * @return an {@link ImageRepresentation}
     */
    public ImageRepresentation drawResult(Collection<Integer> features) {
        final ImageRepresentation instanceType = originalInstance.getInstance();
        final int[] labels = originalInstance.getLabels();
        final ImageRepresentation result = instanceType.createBlankCanvas();
        for (int x = 0; x < instanceType.getWidth(); x++) {
            for (int y = 0; y < instanceType.getHeight(); y++) {
                final int label = labels[x + y * instanceType.getWidth()];
                if (features.contains(label))
                    result.setPixel(x, y, instanceType.getPixel(x, y));
                else
                    result.setPixel(x, y, 0);
            }
        }

        return result;
    }

    /**
     * Draws the superpixels that were identified to the image
     *
     * @return the image with superpixel borders marked
     */
    public ImageRepresentation drawSuperpixeled() {
        final ImageRepresentation instanceType = originalInstance.getInstance();
        final ImageRepresentation result = instanceType.createBlankCanvas();
        // Create output image with pixel edges
        final int height = instanceType.getHeight();
        for (int y = 1; y < height - 1; y++) {
            final int width = instanceType.getWidth();
            for (int x = 1; x < width - 1; x++) {
                final int id1 = originalInstance.getLabels()[x + y * width];
                final int id2 = originalInstance.getLabels()[(x + 1) + y * width];
                final int id3 = originalInstance.getLabels()[x + (y + 1) * width];
                if (id1 != id2 || id1 != id3) {
                    result.setPixel(x, y, 0x000000);
                } else {
                    result.setPixel(x, y, instanceType.getPixel(x, y));
                }
            }
        }


        return result;
    }


    /**
     * May be used to visualize an anchor result by highlighting the feature importances
     *
     * @param anchorCandidate the anchorCandidate to extract the feature importances from
     * @return the {@link ImageRepresentation} containing the showable image
     */
    public ImageRepresentation drawImportance(AnchorCandidate anchorCandidate) {
        final Map<Integer, Double> importanceMap = new HashMap<>();
        while (anchorCandidate != null) {
            importanceMap.put(anchorCandidate.getAddedFeature(), anchorCandidate.getAddedPrecision());
            anchorCandidate = anchorCandidate.getParentCandidate();
        }
        return drawImportance(importanceMap);
    }

    /**
     * May be used to visualize an anchor result by highlighting the feature importances
     *
     * @param featureImportanceMap the feature importances of each label
     * @return the {@link ImageRepresentation} containing the showable image
     */
    public ImageRepresentation drawImportance(Map<Integer, Double> featureImportanceMap) {
        for (double featureImportance : featureImportanceMap.values())
            if (featureImportance > 1)
                throw new IllegalArgumentException("Feature importance is higher than 1");

        final double highestImportance = featureImportanceMap.values().stream().mapToDouble(i -> i).max().orElse(1D);

        final ImageRepresentation result = originalInstance.getInstance().createBlankCanvas();
        for (int i = 0; i < originalInstance.getFeatureCount(); i++) {
            for (int[] pixels : originalInstance.getLabelPixels(i)) {
                final int x = pixels[0];
                final int y = pixels[1];
                final int initial = originalInstance.getInstance().getPixel(x, y);

                final Double featureImportance = Math.max(0, featureImportanceMap.getOrDefault(i,
                        0D) / highestImportance);

                final int adjustedColor = darken(initial, featureImportance);

                result.setPixel(x, y, adjustedColor);
            }
        }
        return result;
    }

    private static int darken(int color, double factor) {
        return (color & 0xFF000000) |
                (Math.min(255, Math.max(0, ((int) (((color >> 16) & 0xFF) * factor)))) << 16) |
                (Math.min(255, Math.max(0, ((int) (((color >> 8) & 0xFF) * factor)))) << 8) |
                (Math.min(255, Math.max(0, ((int) (((color) & 0xFF) * factor)))));
    }
}
