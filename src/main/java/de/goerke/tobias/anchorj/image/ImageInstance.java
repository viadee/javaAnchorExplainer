package de.goerke.tobias.anchorj.image;

import de.goerke.tobias.anchorj.base.DataInstance;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Representation of an image {@link DataInstance} using {@link ImageRepresentation}.
 * <p>
 * Saves the labels each pixel is assigned to, thus saving superpixels, enabling image perturbation.
 */
public class ImageInstance implements DataInstance<ImageRepresentation> {
    private final ImageRepresentation originalImage;
    private final int[] labels;
    private final int featureCount;

    /**
     * Creates the instance.
     *
     * @param originalImage the image instance being explained
     * @param labels        array of the labels each pixel is assigned to. Indexed as  {@code x + y * width}
     */
    public ImageInstance(ImageRepresentation originalImage, int[] labels) {
        if (originalImage == null)
            throw new IllegalArgumentException("Original image" + ParameterValidation.NULL_MESSAGE);
        if (labels.length != (originalImage.getHeight() * originalImage.getWidth()))
            throw new IllegalArgumentException("There must be a label assigned to each pixel");

        this.originalImage = originalImage;
        this.labels = labels;
        this.featureCount = Math.toIntExact(IntStream.of(labels).distinct().count());
    }

//    public static ImageInstance createLabelsFromSuperpixels(ImageRepresentation originalImage, int width, int proximityModifier) {
//        return Superpixel.calculateClusters(originalImage, width, proximityModifier);
//    }

    @Override
    public ImageRepresentation getInstance() {
        return originalImage;
    }

    @Override
    public int getFeatureCount() {
        return featureCount;
    }

    /**
     * Offers access to the label for each pixel.
     * <p>
     * A pixel (x,y) is indexed at {@code x + y * width}
     *
     * @return the labels array describing each feature
     */
    public int[] getLabels() {
        return labels;
    }

    /**
     * Returns all pixels belonging to a certain label
     *
     * @param label the label to return pixels for
     * @return a {@link List} of int arrays, each containing the x coordinate at index 0, y at index 1
     */
    List<int[]> getLabelPixels(int label) {
        List<int[]> result = new ArrayList<>();
        int width = originalImage.getWidth();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] != label)
                continue;
            int x = i % width;
            int y = (i - x) / width;
            result.add(new int[]{x, y});
        }
        return result;
    }
}
