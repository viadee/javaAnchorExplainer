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
 * <li>Now select explanations s.t. they optimize these values</li>
 * </ol>
 */
public class SubmodularPick<T extends DataInstance<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);

    private final BatchExplainer<T> batchExplainer;
    private final AnchorConstructionBuilder<T> constructionBuilder;

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies.
     */
    public SubmodularPick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads) {
        this(new ThreadedBatchExplainer<>(maxThreads), constructionBuilder);
    }

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     */
    public SubmodularPick(BatchExplainer<T> batchExplainer, AnchorConstructionBuilder<T> constructionBuilder) {
        this.batchExplainer = batchExplainer;
        this.constructionBuilder = constructionBuilder;
    }

    /**
     * Flatten matrix to see how important each column is. Results in importance matrix I
     *
     * @param importanceMatrix the importance matrix
     * @return the column importance I
     */
    protected double[] createColumnImportance(double[][] importanceMatrix) {
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
    protected double computeFeatureImportance(AnchorResult<T> anchorResult, int feature) {
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

    /**
     * Executes the {@link SubmodularPick} algorithm.
     * <p>
     * Produces multiple results that are most important for the global explanation of a model.
     *
     * @param instances               the instances to obtain explanations for.
     * @param nrOfExplanationsDesired the max result number to generate
     * @return a {@link List} of {@link AnchorResult}s
     */
    public List<AnchorResult<T>> run(final List<T> instances, final int nrOfExplanationsDesired) {
        if (instances == null || instances.isEmpty())
            return Collections.emptyList();

        final double startTime = System.currentTimeMillis();
        // 1. Obtain the anchors - explanation matrix W
        final AnchorResult<T>[] anchorResults = batchExplainer.obtainAnchors(constructionBuilder, instances);
        LOGGER.info("Took {} ms for gathering all explanations", (System.currentTimeMillis() - startTime));

        final double[][] importanceMatrix = createImportanceMatrix(anchorResults);

        // 4. Flatten matrix to see how important each column is. Results in importance matrix I
        final double[] columnImportance = createColumnImportance(importanceMatrix);

        return greedyPick(nrOfExplanationsDesired, anchorResults, importanceMatrix, columnImportance);
    }

    protected double[][] createImportanceMatrix(final AnchorResult<T>[] anchorResults) {
        // Build matrix
        // 2. Assign a unique column index to each feature
        final Map<Integer, Integer> featureToColumnMap = new HashMap<>();
        int index = 0;
        for (final AnchorResult anchorResult : anchorResults)
            for (final Integer feature : anchorResult.getOrderedFeatures())
                if (!featureToColumnMap.containsKey(feature))
                    featureToColumnMap.put(feature, index++);

        // 3. Calculate cell importance matrix, which later gets transformed to importance matrix
        final double[][] importanceMatrix = new double[anchorResults.length][featureToColumnMap.size()];
        for (int i = 0; i < anchorResults.length; i++) {
            for (final int feature : anchorResults[i].getOrderedFeatures()) {
                importanceMatrix[i][featureToColumnMap.get(feature)] = computeFeatureImportance(anchorResults[i], feature);
            }
        }
        return importanceMatrix;
    }

    private List<AnchorResult<T>> greedyPick(final int nrOfExplanationsDesired, final AnchorResult<T>[] anchorResults,
                                             final double[][] importanceMatrix, final double[] columnImportance) {
        // 5. Greedy SP algorithm
        final Set<Integer> selectedIndices = new LinkedHashSet<>();
        final Set<AnchorResult> remainingResults = new HashSet<>(Arrays.asList(anchorResults));
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

            selectedIndices.add(bestIndex);
            remainingResults.remove(anchorResults[bestIndex]);
        }

        final List<AnchorResult<T>> result = new ArrayList<>();
        for (final Integer idx : selectedIndices)
            result.add(anchorResults[idx]);
        return result;
    }
}