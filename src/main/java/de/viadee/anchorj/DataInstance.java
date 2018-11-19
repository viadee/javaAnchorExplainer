package de.viadee.anchorj;

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
     * @return the instance being described.
     */
    T getInstance();

    /**
     * Gets a feature contained in the instance.
     * <p>
     * May throw an {@link UnsupportedOperationException} if not supported by the instance, as e.g. for images where
     * it is undefined as to what a feature refers to data-wise
     *
     * @param featureId the featureId
     * @return the feature
     */
    default Object getValue(int featureId) throws UnsupportedOperationException {
        T instance = getInstance();
        if (instance != null && instance.getClass().isArray()) {
            return ((Object[]) instance)[featureId];
        }
        throw new UnsupportedOperationException("Default method cannot auto-extract the feature");
    }

    /**
     * @return the amount of features contained in the instance
     */
    int getFeatureCount();
}
