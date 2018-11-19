package de.viadee.anchorj.global;

import java.util.*;

/**
 * Provides utility functions for the {@link SubmodularPick} algorithm.
 */
final class SubmodularPickUtils {

    /**
     * Splits a list into approx. equal length sublists
     *
     * @param list          the list to be split
     * @param partitionSize the partition size
     * @param <T>           type of the list
     * @return the split list
     */
    static <T> List<List<T>> splitList(List<T> list, int partitionSize) {
        final List<List<T>> partitions = new LinkedList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    /**
     * Multiplied the column importance and the column sum
     *
     * @param colSums          the colSum obtains by {@link SubmodularPickUtils#colSum(double[][], Collection, int)}
     * @param columnImportance the column importance
     * @return the result
     */
    static double multiply(double[] colSums, double[] columnImportance) {
        double result = 0;
        for (int i = 0; i < colSums.length; i++) {
            if (colSums[i] <= 0)
                continue;
            result += columnImportance[i];
        }
        return result;
    }

    /**
     * Calculates the column sum
     *
     * @param matrix                 the explanation matrix
     * @param previouslySelectedRows the already selected rows
     * @param additionalIndex        the index to be considered adding
     * @return the target array to be multiplied with the column importance
     */
    static double[] colSum(double[][] matrix, Collection<Integer> previouslySelectedRows, int additionalIndex) {
        final Set<Integer> indices = new HashSet<>(previouslySelectedRows);
        indices.add(additionalIndex);

        double result[] = new double[matrix[0].length];
        for (int col = 0; col < result.length; col++) {
            for (final int row : indices) {
                result[col] += matrix[row][col];
            }
        }
        return result;
    }
}
