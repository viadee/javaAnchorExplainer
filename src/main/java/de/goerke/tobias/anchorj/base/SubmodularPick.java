package de.goerke.tobias.anchorj.base;

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
public class SubmodularPick<T extends DataInstance<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);
    protected final AnchorConstructionBuilder<T> anchorConstructionBuilder;
    protected final ClassificationFunction<T> classificationFunction;
    private final int maxThreads;
    private final ExecutorService executorService;

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param maxThreads                the number of threads to execute in parallel.
     *                                  Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                  thread count multiplies.
     * @param anchorConstructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                                  when running the algorithm.
     */
    public SubmodularPick(final int maxThreads, final AnchorConstructionBuilder<T> anchorConstructionBuilder) {
        if (maxThreads > 1 && anchorConstructionBuilder.getMaxThreadCount() > 1)
            LOGGER.warn("AnchorConstruction threading enables. Threads will multiply to max: {} threads",
                    anchorConstructionBuilder.getMaxThreadCount() * maxThreads);
        this.maxThreads = maxThreads;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
        this.anchorConstructionBuilder = anchorConstructionBuilder;
        this.classificationFunction = anchorConstructionBuilder.getClassificationFunction();
    }

    private static <T> List<List<T>> splitList(List<T> list, int partitionSize) {
        final List<List<T>> partitions = new LinkedList<>();
        for (int i = 0; i < list.size(); i += partitionSize) {
            partitions.add(list.subList(i, Math.min(i + partitionSize, list.size())));
        }
        return partitions;
    }

    private static double multiply(double[] colSums, double[] columnImportance) {
        double result = 0;
        for (int i = 0; i < colSums.length; i++) {
            if (colSums[i] <= 0)
                continue;
            result += columnImportance[i];
        }
        return result;
    }

    private static double[] colSum(double[][] matrix, Collection<Integer> previouslySelectedRows, int additionalIndex) {
        final Set<Integer> indices = new HashSet<>(previouslySelectedRows);
        indices.add(additionalIndex);

        double result[] = new double[matrix[0].length];
        for (int col = 0; col < result.length; col++) {
            for (int row : indices) {
                result[col] += matrix[row][col];
            }
        }
        return result;
    }

    /**
     * Produces explanations for all instances and thus delegates to the {@link AnchorConstruction}
     *
     * @param labeledInstances the labeled instances
     * @return the explanation results
     */
    protected Collection<AnchorResult<T>> execute(List<T> labeledInstances) {
        final List<List<T>> splitLists = splitList(labeledInstances, labeledInstances.size() / maxThreads);
        final Collection<AnchorResult<T>> threadResults = Collections.synchronizedCollection(new ArrayList<>());
        for (final List<T> list : splitLists) {
            executorService.submit(() -> {
                LOGGER.info("Thread started");
                for (T instance : list) {
                    ClassificationFunction<T> classificationFunction = anchorConstructionBuilder.getClassificationFunction();
                    final int label = classificationFunction.predict(instance);
                    AnchorResult<T> result = setupForInstance(anchorConstructionBuilder, instance, label);

                    if (result != null)
                        threadResults.add(result);
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
        return threadResults;
    }

    private AnchorResult<T> setupForInstance(final AnchorConstructionBuilder<T> anchorConstructionBuilder,
                                             final T instance, final int label) {
        try {
            final AnchorResult<T> anchorResult = anchorConstructionBuilder.setupForSP(instance, label).build().constructAnchor();
            if (!anchorResult.isAnchor()) {
                LOGGER.debug("Could not find an anchor for instance {}. Discarding best candidate.",
                        instance);
                return null;
            }
            return anchorResult;
        } catch (NoCandidateFoundException e) {
            LOGGER.warn("Could not find a candidate for instance {}", instance);
            return null;
        }
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
    private double calculateFeatureImportance(AnchorResult<T> anchorResult, int feature) {
        // Searches for the parent in which the features has been added and extracts its added feature value
        AnchorCandidate current = anchorResult;
        do {
            List<Integer> orderedFeatures = current.getOrderedFeatures();
            Integer addedElement = orderedFeatures.get(orderedFeatures.size() - 1);
            if (addedElement.equals(feature))
                // Maximize added precision
                //return current.getAddedPrecision();
                // Maximize coverage
                return Math.min(1, current.getAddedCoverageInPercent());
            current = current.getParentCandidate();
        } while (current != null);

        throw new RuntimeException("Inconsistent candidate inheritance");
    }

    /**
     * Executes the {@link SubmodularPick} algorithm.
     * <p>
     * Produces multiple results that are most important for the global explanation of a model.
     *
     * @param labeledInstances        the instances to obtain explanations for.
     * @param nrOfExplanationsDesired the max result number to generate
     * @return a {@link List} of {@link AnchorResult}s
     */
    public List<AnchorResult<T>> run(final List<T> labeledInstances, final int nrOfExplanationsDesired) {
        final double startTime = System.currentTimeMillis();
        Collection<AnchorResult<T>> threadResults = execute(labeledInstances);
        LOGGER.info("Took {} ms", (System.currentTimeMillis() - startTime));

        @SuppressWarnings("unchecked") final AnchorResult<T>[] anchorResults = threadResults.toArray(new AnchorResult[0]);

        // Build matrix
        // 1. Assign a column index to each feature
        final Map<Integer, Integer> featureToColumnMap = new HashMap<>();
        int index = 0;
        for (AnchorResult anchorResult : anchorResults)
            for (Integer feature : anchorResult.getOrderedFeatures())
                if (!featureToColumnMap.containsKey(feature))
                    featureToColumnMap.put(feature, index++);
        // 2. Calculate importance matrix
        final double[][] importanceMatrix = new double[anchorResults.length][featureToColumnMap.size()];
        for (int i = 0; i < importanceMatrix.length; i++) {
            for (int feature : anchorResults[i].getOrderedFeatures()) {
                importanceMatrix[i][featureToColumnMap.get(feature)] = calculateFeatureImportance(anchorResults[i], feature);
            }
        }
        // 3. Flatten matrix to see how important columns are
        final double[] columnImportance = new double[featureToColumnMap.size()];
        for (int i = 0; i < importanceMatrix.length; i++)
            for (int j = 0; j < importanceMatrix[i].length; j++)
                columnImportance[j] += importanceMatrix[i][j];

        // Greedy SP algorithm
        final Set<Integer> selectedIndices = new LinkedHashSet<>();
        final Set<AnchorResult> remainingResults = new HashSet<>(Arrays.asList(anchorResults));
        for (int i = 0; i < Math.min(nrOfExplanationsDesired, anchorResults.length); i++) {
            double bestCoverage = 0;
            Integer bestIndex = null;
            for (AnchorResult anchorResult : remainingResults) {
                int anchorResultIndex = Arrays.asList(anchorResults).indexOf(anchorResult);
                double[] colSums = colSum(importanceMatrix, selectedIndices, anchorResultIndex);
                double coverage = multiply(colSums, columnImportance);
                if (coverage >= bestCoverage) {
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
        for (Integer idx : selectedIndices)
            result.add(anchorResults[idx]);
        return result.stream().sorted(Comparator.comparingDouble(AnchorCandidate::getCoverage)).collect(Collectors.toList());
    }
}