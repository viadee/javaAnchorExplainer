package de.goerke.tobias.anchorj.base;

import java.io.Serializable;

/**
 * Represents an instance that may be explained by the anchors algorithm.
 *
 * @param <T> Type of a single, predictable and perturbable instance.
 *            E.g. an {@link java.awt.image.BufferedImage} as image, a {@link String} as sentence,
 *            or an array for a table row
 */
public interface DataInstance<T> extends Serializable {
    /**
     * @return the instance being described
     */
    T getInstance();

    /**
     * @return the amount of features contained in the instance
     */
    int getFeatureCount();
}
