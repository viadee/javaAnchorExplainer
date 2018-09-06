package de.goerke.tobias.anchorj.base;

import java.lang.reflect.Array;

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
    default int predict(T instance) {
        @SuppressWarnings("unchecked") final T[] array = (T[]) Array.newInstance(instance.getClass(), 1);
        array[0] = instance;
        return predict(array)[0];
    }

    /**
     * Labels a set of instances according to the models prediction
     *
     * @param instances the instances to predict labels for
     * @return the instances' predicted labels
     */
    int[] predict(T[] instances);
}
