package de.viadee.anchorj.global;

import de.viadee.anchorj.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Class implementing the Submodular Pick (SP) algorithm proposed by Ribeiro (2016).
 * <p>
 * In the showcase example this class gets extended and enabled to be used in conjunction with Apache Spark.
 * Due to this projects dependency-less design, it is not included in the core project.
 * <p>
 * Algorithm outline:
 * <ol>
 * <li>Obtain explanations for all instances</li>
 * <li>Create a 2D matrix. Store a row for each explanation and map it to the explanation's feature importance</li>
 * <li>Flatten this matrix to obtain feature importance of a whole feature</li>
 * <li>Now select explanations s.t. they optimize these values using a greedy algorithm</li>
 * </ol>
 */
public class SubmodularPick<T extends DataInstance<?>> extends AbstractGlobalExplainer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);

    /**
     * Creates the instance.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies if executed locally.
     */
    public SubmodularPick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads) {
        super(constructionBuilder, maxThreads);
    }

    /**
     * Creates the instance.
     *
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     */
    public SubmodularPick(BatchExplainer<T> batchExplainer, AnchorConstructionBuilder<T> constructionBuilder) {
        super(batchExplainer, constructionBuilder);
    }

    /**
     * Flatten matrix to see how important each column is. Results in importance matrix I
     *
     * @param importanceMatrix the importance matrix
     * @return the column importance I
     */
    private double[] createColumnImportance(double[][] importanceMatrix) {
        final double[] columnImportances = new double[importanceMatrix[0].length];
        // Loop columns
        for (int columnIndex = 0; columnIndex < importanceMatrix[0].length; columnIndex++) {
            double currentImportance = 0;
            // Loop rows
            for (final double[] row : importanceMatrix) {
                // Only count, if feature has any importance in this row
                final double cellValue = row[columnIndex];
                currentImportance += cellValue;
            }
            // ColImportance = average over all rows
            columnImportances[columnIndex] = currentImportance / importanceMatrix.length;
        }

        return columnImportances;
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
     * @return an importance value
     */
    private double computeFeatureImportance(AnchorResult<T> anchorResult, int feature) {
        // Searches for the parent in which the features has been added and extracts its added feature value
        AnchorCandidate current = anchorResult;
        do {
            final List<Integer> orderedFeatures = current.getOrderedFeatures();
            final Integer addedElement = orderedFeatures.get(orderedFeatures.size() - 1);
            if (addedElement.equals(feature))
                return Math.max(0, Math.min(1, current.getAddedPrecision()));
            current = current.getParentCandidate();
        } while (current != null);

        throw new RuntimeException("Should not happen - Inconsistent candidate inheritance!");
    }


    @Override
    List<AnchorResult<T>> pickExplanations(AnchorResult<T>[] explanations, int nrOfExplanationsDesired) {
        // Build matrix
        // 2. Assign a unique column index to each feature
        final CreateFeatureToColumnMapResult createFeatureToColumnMapResult = createFeatureToColumnMap(explanations);
        final Map<Integer, ?> featureToColumnMap = createFeatureToColumnMapResult.map;

        // 3. Calculate cell importance matrix, which later gets transformed to importance matrix
        final double[][] importanceMatrix = new double[explanations.length][createFeatureToColumnMapResult.columnCount];
        for (int i = 0; i < explanations.length; i++) {
            for (final int feature : explanations[i].getOrderedFeatures()) {
                importanceMatrix[i][getCandidateFeatureIndex(featureToColumnMap, explanations[i], feature)] =
                        computeFeatureImportance(explanations[i], feature);
            }
        }

        // 4. Flatten matrix to see how important each column is. Results in importance matrix I
        final double[] columnImportance = createColumnImportance(importanceMatrix);

        return greedyPick(nrOfExplanationsDesired, explanations, importanceMatrix, columnImportance);
    }

    /**
     * Creates a mapping for an explanation's feature value to an id
     *
     * @param anchorResults the explanations
     * @return a mapping of unique ids to objects (e.g. features ids or feature values)
     */
    protected CreateFeatureToColumnMapResult createFeatureToColumnMap(final AnchorResult<T>[] anchorResults) {
        final Map<Integer, Integer> featureToColumnMap = new HashMap<>();
        int index = 0;
        for (final AnchorResult anchorResult : anchorResults)
            for (final Integer feature : anchorResult.getOrderedFeatures())
                if (!featureToColumnMap.containsKey(feature))
                    featureToColumnMap.put(feature, index++);
        return new CreateFeatureToColumnMapResult(featureToColumnMap, featureToColumnMap.size());
    }

    /**
     * Resolves an explanation's feature to a unique id
     *
     * @param featureToColumnMap the featureToColumnMap as computed by {@link #createFeatureToColumnMap(AnchorResult[])}
     * @param anchorResult       the result whose feature to resolve to an id
     * @param feature            the feature to resolve
     * @return the explanation's feature id
     */
    @SuppressWarnings("unchecked")
    protected int getCandidateFeatureIndex(final Map<Integer, ?> featureToColumnMap, AnchorResult anchorResult, int feature) {
        return ((Map<Integer, Integer>) featureToColumnMap).get(feature);
    }

    private List<AnchorResult<T>> greedyPick(final int nrOfExplanationsDesired, final AnchorResult<T>[] anchorResults,
                                             final double[][] importanceMatrix, final double[] columnImportance) {
        // 5. Greedy SP algorithm
        final Set<Integer> selectedIndices = new LinkedHashSet<>();
        final Set<AnchorResult> remainingResults = new HashSet<>(Arrays.asList(anchorResults));
        double previousBestCoverage = 0;
        for (int i = 0; i < Math.min(nrOfExplanationsDesired, anchorResults.length); i++) {
            double bestCoverage = 0;
            Integer bestIndex = null;
            for (AnchorResult anchorResult : remainingResults) {
                final int anchorResultIndex = Arrays.asList(anchorResults).indexOf(anchorResult);
                final double[] colSums = SubmodularPickUtils.colSum(importanceMatrix, selectedIndices, anchorResultIndex);
                final double coverage = SubmodularPickUtils.multiply(colSums, columnImportance);
                if (coverage > bestCoverage) {
                    bestIndex = anchorResultIndex;
                    bestCoverage = coverage;
                }
            }
            if (bestIndex == null)
                break;

            LOGGER.info("Adding candidate {} adding coverage of {}, totalling to {}",
                    anchorResults[bestIndex].getCanonicalFeatures(),
                    (bestCoverage - previousBestCoverage), bestCoverage);
            previousBestCoverage = bestCoverage;
            selectedIndices.add(bestIndex);
            remainingResults.remove(anchorResults[bestIndex]);
        }

        final List<AnchorResult<T>> result = new ArrayList<>();
        for (final Integer idx : selectedIndices)
            result.add(anchorResults[idx]);
        return result;
    }

    class CreateFeatureToColumnMapResult {
        final Map<Integer, ?> map;
        final int columnCount;

        CreateFeatureToColumnMapResult(Map<Integer, ?> map, int columnCount) {
            this.map = map;
            this.columnCount = columnCount;
        }
    }
}