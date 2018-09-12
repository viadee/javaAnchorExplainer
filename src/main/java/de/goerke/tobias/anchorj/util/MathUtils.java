package de.goerke.tobias.anchorj.util;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Provides some basic math utils so we don't have to depend on third party libraries
 */
public final class MathUtils {

    private MathUtils() {
    }

    /**
     * Argsort function
     * <p>
     * "Which index would belong where if the array was sorted?"
     *
     * @param a the array
     * @return the array sorted by its indices.
     */
    public static int[] argSort(final double[] a) {
        return argSort(a, true);
    }

    /**
     * Argsort function
     * <p>
     * "Which index would belong where if the array was sorted?"
     *
     * @param a         the array
     * @param ascending if true, ascending. Descending otherwise
     * @return the array sorted by its indices
     */
    public static int[] argSort(final double[] a, final boolean ascending) {
        final Integer[] indexes = new Integer[a.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, (i1, i2) -> (ascending ? 1 : -1) * Double.compare(a[i1], a[i2]));
        return Stream.of(indexes).mapToInt(i -> i).toArray();
    }

    /**
     * Argmax function
     *
     * @param array the array
     * @return the index of the highest value in the array
     */
    public static int argMax(final double[] array) {
        return argExtreme(array, true);
    }

    /**
     * Argmin function
     *
     * @param array the array
     * @return the index of the lowest value in the array
     */
    public static int argMin(final double[] array) {
        return argExtreme(array, false);
    }

    private static int argExtreme(final double[] ar, final boolean max) {
        double value = (max) ? Double.MIN_VALUE : Double.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < ar.length; i++) {
            if ((max && ar[i] > value) || (!max && ar[i] < value)) {
                value = ar[i];
                index = i;
            }
        }
        return index;
    }
}
