package de.goerke.tobias.anchorj.base.global;

import de.goerke.tobias.anchorj.base.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Class implementing the Submodular Pick (SP) algorithm proposed by Ribeiro (2016).
 * <p>
 * In the showcase example this class gets extended and enabled to be used in conjunction with Apache Spark.
 * Due to this projects dependency-less design, it is not included in the core project.
 */
public class SubmodularPick<T extends DataInstance<?>> extends AbstractAnchorsAggregator<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);
    final SubmodularPickGoal optimizationGoal;

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param maxThreads                the number of threads to obtainAnchors in parallel.
     *                                  Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                  thread count multiplies.
     * @param anchorConstructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                                  when running the algorithm.
     * @param optimizationGoal          the optimization goal
     */
    public SubmodularPick(int maxThreads, AnchorConstructionBuilder<T> anchorConstructionBuilder,
                          SubmodularPickGoal optimizationGoal) {
        super(maxThreads, anchorConstructionBuilder);
        this.optimizationGoal = optimizationGoal;
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
        final AnchorResult<T>[] anchorResults = obtainAnchors(instances);
        LOGGER.info("Took {} ms for gathering all explanations", (System.currentTimeMillis() - startTime));

        final double[][] importanceMatrix = createImportanceMatrix(anchorResults);

        // 4. Flatten matrix to see how important each column is. Results in importance matrix I
        final double[] columnImportance = optimizationGoal.computeColumnImportance(importanceMatrix);

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
        for (int i = 0; i < importanceMatrix.length; i++) {
            for (final int feature : anchorResults[i].getOrderedFeatures()) {
                importanceMatrix[i][featureToColumnMap.get(feature)] = optimizationGoal
                        .computeFeatureImportance(anchorResults[i], feature);
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