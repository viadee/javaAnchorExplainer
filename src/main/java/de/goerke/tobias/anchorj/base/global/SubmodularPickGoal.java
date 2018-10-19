package de.goerke.tobias.anchorj.base.global;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.AnchorResult;
import de.goerke.tobias.anchorj.base.DataInstance;
import de.goerke.tobias.anchorj.util.ArrayUtils;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Defines the optimization goal of the {@link SubmodularPick} algorithm
 */
public enum SubmodularPickGoal {

    FEATURE_PRECISION(
            (anchorResult, feature) -> {
                // Searches for the parent in which the features has been added and extracts its added feature value
                AnchorCandidate current = anchorResult;
                do {
                    List<Integer> orderedFeatures = current.getOrderedFeatures();
                    Integer addedElement = orderedFeatures.get(orderedFeatures.size() - 1);
                    if (addedElement.equals(feature))
                        return Math.max(0, Math.min(1, current.getAddedPrecision()));
                    current = current.getParentCandidate();
                } while (current != null);

                throw new RuntimeException("Inconsistent candidate inheritance");
            },
            importanceMatrix -> {
                final Double[] columnImportances = new Double[importanceMatrix[0].length];
                for (final Double[] row : importanceMatrix)
                    for (int j = 0; j < row.length; j++)
                        columnImportances[j] = (columnImportances[j] == null) ? row[j] : (columnImportances[j] + row[j]);
                return columnImportances;
            }
    ),

    FEATURE_COVERAGE(
            (anchorResult, feature) -> {
                // Searches for the parent in which the features has been added and extracts its added feature value
                AnchorCandidate current = anchorResult;
                do {
                    List<Integer> orderedFeatures = current.getOrderedFeatures();
                    Integer addedElement = orderedFeatures.get(orderedFeatures.size() - 1);
                    if (addedElement.equals(feature))
                        return 1 - Math.abs(Math.max(-1, Math.min(0, current.getAddedCoverageInPercent())));
                    current = current.getParentCandidate();
                } while (current != null);

                throw new RuntimeException("Inconsistent candidate inheritance");
            },
            importanceMatrix -> {
                final Double[] columnImportances = new Double[importanceMatrix[0].length];
                for (final Double[] row : importanceMatrix)
                    for (int j = 0; j < row.length; j++)
                        columnImportances[j] = (columnImportances[j] == null) ? row[j] : (columnImportances[j] + row[j]);
                return columnImportances;
            }
    ),

    /**
     * The mode initially proposed in "Why Should I Trust You" by Ribeiro et al.
     */
    FEATURE_APPEARANCE(
            (anchorResult, feature) -> anchorResult.getCanonicalFeatures().contains(feature) ? 1D : 0D,
            importanceMatrix -> {
                final Double[] columnImportances = new Double[importanceMatrix[0].length];
                for (final Double[] row : importanceMatrix) {
                    for (int j = 0; j < row.length; j++)
                        columnImportances[j] = (columnImportances[j] == null) ? row[j] : (columnImportances[j] + row[j]);
                }
                for (int i = 0; i < columnImportances.length; i++) {
                    columnImportances[i] = Math.sqrt(columnImportances[i]);
                }
                return columnImportances;
            }
    );

    private final BiFunction<AnchorResult<? extends DataInstance<?>>, Integer, Double> featureImportance;
    private final Function<Double[][], Double[]> columnImportance;

    SubmodularPickGoal(BiFunction<AnchorResult<? extends DataInstance<?>>, Integer, Double> featureImportance,
                       Function<Double[][], Double[]> columnImportance) {
        this.featureImportance = featureImportance;
        this.columnImportance = columnImportance;
    }

    /**
     * This method returns an importance value for a specific feature of an {@link AnchorResult}.
     * <p>
     * This value gets used to build the importance matrix.
     * <p>
     * This works by retrieving the added feature importance value of the specific
     *
     * @param anchorResult the {@link AnchorResult} the feature is taken out of
     * @param feature      the feature being examined
     * @param <T>          the type of the data instance
     * @return an importance value
     */
    public <T extends DataInstance<?>> double computeFeatureImportance(AnchorResult<T> anchorResult, int feature) {
        return featureImportance.apply(anchorResult, feature);
    }

    /**
     * Flatten matrix to see how important each column is. Results in importance matrix I
     *
     * @param importanceMatrix the importance matrix
     * @return the comlumn importance I
     */
    public double[] computeColumnImportance(double[][] importanceMatrix) {
        return ArrayUtils.toPrimitiveArray(columnImportance.apply(ArrayUtils.toBoxedArray(importanceMatrix)));
    }
}
