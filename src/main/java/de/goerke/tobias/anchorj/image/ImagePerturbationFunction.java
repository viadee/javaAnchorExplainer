package de.goerke.tobias.anchorj.image;

import de.goerke.tobias.anchorj.base.PerturbationFunction;
import de.goerke.tobias.anchorj.base.global.ReconfigurablePerturbationFunction;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class may be used to perturb an {@link ImageInstance}.
 * <p>
 * By randomly showing/hiding non-fixed features it will create different images.
 * As proposed by Ribeiro, instead of hiding superpixels, these may be superimposed by other images.
 */
public class ImagePerturbationFunction implements ReconfigurablePerturbationFunction<ImageInstance> {
    private final ImageInstance imageInstance;
    private final List<ImageRepresentation> backgroundImageInstances;
    private final double superpixelChangeChance;
    private final Random random = new Random();

    /**
     * Creates the instance.
     *
     * @param imageInstance            the instance to be perturbed
     * @param backgroundImageInstances the images used to superimpose instead of hiding pixels. May be null or empty
     * @param superpixelChangeChance   the chance a non-fixed pixel gets hidden/swapped
     */
    public ImagePerturbationFunction(final ImageInstance imageInstance, List<ImageRepresentation> backgroundImageInstances,
                                     final double superpixelChangeChance) {
        if (imageInstance == null)
            throw new IllegalArgumentException("Image instance" + ParameterValidation.NULL_MESSAGE);
        if (!ParameterValidation.isPercentage(superpixelChangeChance))
            throw new IllegalArgumentException("Superixel hidden chance" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        backgroundImageInstances = (backgroundImageInstances == null) ? Collections.emptyList() : backgroundImageInstances;
        for (ImageRepresentation backgroundImageInstance : backgroundImageInstances)
            if ((backgroundImageInstance.getHeight() != imageInstance.getInstance().getHeight())
                    || backgroundImageInstance.getWidth() != imageInstance.getInstance().getWidth())
                throw new IllegalArgumentException("Background images must be of same size as the image instance.");

        this.imageInstance = imageInstance;
        this.backgroundImageInstances = backgroundImageInstances;
        this.superpixelChangeChance = superpixelChangeChance;
    }

    /**
     * Creates the instance.
     *
     * @param imageInstance          the instance to be perturbed
     * @param superpixelChangeChance the chance a non-fixed pixel gets hidden
     */
    public ImagePerturbationFunction(final ImageInstance imageInstance, final double superpixelChangeChance) {
        this(imageInstance, null, superpixelChangeChance);
    }

    private static void writePixels(final ImageRepresentation image, final List<int[]> pixels, final int[] rgbs) {
        for (int i = 0; i < pixels.size(); i++) {
            final int[] pixel = pixels.get(i);
            image.setPixel(pixel[0], pixel[1], rgbs[i]);
        }
    }

    private static int[] getPixels(final ImageRepresentation image, final List<int[]> pixels) {
        final int[] rgbs = new int[pixels.size()];
        for (int i = 0; i < pixels.size(); i++) {
            final int[] pixel = pixels.get(i);
            rgbs[i] = image.getPixel(pixel[0], pixel[1]);
        }
        return rgbs;
    }

    @Override
    public PerturbationFunction<ImageInstance> createForInstance(ImageInstance instance) {
        return new ImagePerturbationFunction(imageInstance, backgroundImageInstances, superpixelChangeChance);
    }

    @Override
    public PerturbationResult<ImageInstance> perturb(Set<Integer> immutableFeaturesIdx, int nrPerturbations) {
        final ImageRepresentation[] imageRepresentations = new ImageRepresentation[nrPerturbations];
        final boolean[][] changed = new boolean[nrPerturbations][imageInstance.getFeatureCount()];

        for (int i = 0; i < nrPerturbations; i++) {
            imageRepresentations[i] = imageInstance.getInstance().createBlankCanvas();
        }

        // Use the same background image for this perturbation run, if one should exist
        final ImageRepresentation chosenBackground = (backgroundImageInstances == null || backgroundImageInstances.isEmpty())
                ? null : backgroundImageInstances.get(random.nextInt(backgroundImageInstances.size()));

        for (int i = 0; i < imageInstance.getFeatureCount(); i++) {
            final List<int[]> pixels = imageInstance.getLabelPixels(i);
            final int[] originalRgbs = new int[pixels.size()];
            for (int j = 0; j < originalRgbs.length; j++) {
                final int[] pixel = pixels.get(j);
                originalRgbs[j] = imageInstance.getInstance().getPixel(pixel[0], pixel[1]);
            }
            final int[] backgroundPixels = (chosenBackground == null) ? null : getPixels(chosenBackground, pixels);

            for (int j = 0; j < nrPerturbations; j++) {
                if (immutableFeaturesIdx.contains(i)) {
                    // Skipping feature. Still have to copy pixel anyway
                    writePixels(imageRepresentations[j], pixels, originalRgbs);
                    continue;
                }

                if (random.nextDouble() <= superpixelChangeChance) {
                    // Do something with this superpixel. Hide, swap etc.
                    if (backgroundPixels == null)
                        // No background images to swap, leave these pixels blank
                        continue;
                    // Superimpose the background image over other pixels
                    writePixels(imageRepresentations[j], pixels, backgroundPixels);
                    continue;
                }

                writePixels(imageRepresentations[j], pixels, originalRgbs);
                changed[j][i] = true;
            }
        }

        return new PerturbationResultImpl<>(Stream.of(imageRepresentations)
                .map(b -> new ImageInstance(b, imageInstance.getLabels())).toArray(ImageInstance[]::new), changed);
    }
}
