package de.goerke.tobias.anchorj.base;

/**
 * Interface for models predicting an instance's label
 *
 * @param <T> type of the predicted data instance
 */
public interface ClassificationFunction<T extends DataInstance<?>> {

    /**
     * Labels an instance according to the models prediction
     *
     * @param instance the instance to predict a label for
     * @return the instance's predicted label
     */
    int predict(T instance);

    /**
     * Labels a set of instances according to the models prediction.
     * <p>
     * Overwrite only if the model provides mechanisms to predict multiple instances more efficiently than a single one.
     *
     * @param instances the instances to predict labels for
     * @return the instances' predicted labels
     */
    default int[] predict(T[] instances) {
        int[] result = new int[instances.length];
        for (int i = 0; i < instances.length; i++)
            result[i] = predict(instances[i]);
        return result;
    }
}
