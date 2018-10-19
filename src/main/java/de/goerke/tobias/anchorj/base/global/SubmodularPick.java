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
public class SubmodularPick<T extends DataInstance<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);
    private final AnchorConstructionBuilder<T> anchorConstructionBuilder;
    private final int maxThreads;
    private final ExecutorService executorService;
    private final SubmodularPickGoal optimizationGoal;

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param maxThreads                the number of threads to obtainAnchors in parallel.
     *                                  Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                  thread count multiplies.
     * @param goal                      the optimization goal
     * @param anchorConstructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                                  when running the algorithm.
     */
    public SubmodularPick(final int maxThreads, final SubmodularPickGoal goal,
                          final AnchorConstructionBuilder<T> anchorConstructionBuilder) {
        if (maxThreads > 1 && anchorConstructionBuilder.getMaxThreadCount() > 1)
            LOGGER.warn("AnchorConstruction threading enables. Threads will multiply to max: {} threads",
                    anchorConstructionBuilder.getMaxThreadCount() * maxThreads);
        this.maxThreads = maxThreads;
        this.optimizationGoal = goal;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
        this.anchorConstructionBuilder = anchorConstructionBuilder;
    }

    /**
     * Produces explanations for all instances and thus delegates to the {@link AnchorConstruction}.
     * <p>
     * Is protected to be overriden by distributing services, e.g. Spark
     *
     * @param instances the instances
     * @return the explanation results
     */
    protected AnchorResult<T>[] obtainAnchors(List<T> instances) {
        final List<List<T>> splitLists = SubmodularPickUtils.splitList(instances, instances.size() / maxThreads);
        final Collection<AnchorResult<T>> threadResults = Collections.synchronizedCollection(new ArrayList<>());
        for (final List<T> list : splitLists) {
            executorService.submit(() -> {
                LOGGER.info("Thread started");
                for (final T instance : list) {
                    ClassificationFunction<T> classificationFunction = anchorConstructionBuilder.getClassificationFunction();
                    final int label = classificationFunction.predict(instance);
                    AnchorResult<T> result = obtainAnchor(anchorConstructionBuilder, instance, label);
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
        @SuppressWarnings("unchecked")
        AnchorResult<T>[] result = threadResults.toArray((AnchorResult<T>[]) new AnchorResult[0]);
        return result;
    }

    /**
     * Explains a specified instance.
     * <p>
     * For this, the builder is modified to setup and reconfigure all required functions. E.g. the perturbation function
     * usually perturbs one fixed instance that needs to be changed beforehand
     *
     * @param anchorConstructionBuilder the builder
     * @param instance                  the instance to be explained
     * @param label                     the explained instance's label
     * @return the explanation result
     */
    private AnchorResult<T> obtainAnchor(final AnchorConstructionBuilder<T> anchorConstructionBuilder,
                                         final T instance, final int label) {
        try {
            final AnchorResult<T> anchorResult = anchorConstructionBuilder.setupForSP(instance, label)
                    .build().constructAnchor();
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
     * Executes the {@link SubmodularPick} algorithm.
     * <p>
     * Produces multiple results that are most important for the global explanation of a model.
     *
     * @param instances               the instances to obtain explanations for.
     * @param nrOfExplanationsDesired the max result number to generate
     * @return a {@link List} of {@link AnchorResult}s
     */
    public List<AnchorResult<T>> run(final List<T> instances, final int nrOfExplanationsDesired) {
        final double startTime = System.currentTimeMillis();
        // 1. Obtain the anchors - explanation matrix W
        final AnchorResult<T>[] anchorResults = obtainAnchors(instances);
        LOGGER.info("Took {} ms for gathering all explanations", (System.currentTimeMillis() - startTime));

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

        // 4. Flatten matrix to see how important each column is. Results in importance matrix I
        final double[] columnImportance = optimizationGoal.computeColumnImportance(importanceMatrix);

        // 5. Greedy SP algorithm
        final Set<Integer> selectedIndices = new LinkedHashSet<>();
        final Set<AnchorResult> remainingResults = new HashSet<>(Arrays.asList(anchorResults));
        for (int i = 0; i < Math.min(nrOfExplanationsDesired, anchorResults.length); i++) {
            double bestCoverage = 0;
            Integer bestIndex = null;
            for (AnchorResult anchorResult : remainingResults) {
                int anchorResultIndex = Arrays.asList(anchorResults).indexOf(anchorResult);
                double[] colSums = SubmodularPickUtils.colSum(importanceMatrix, selectedIndices, anchorResultIndex);
                double coverage = SubmodularPickUtils.multiply(colSums, columnImportance);
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
        return result.stream()
                //.sorted(Comparator.comparingDouble(AnchorCandidate::getCoverage).reversed())
                .collect(Collectors.toList());
    }

    /**
     * @return the construction builder for subclasses to extract info
     */
    protected AnchorConstructionBuilder<T> getAnchorConstructionBuilder() {
        return anchorConstructionBuilder;
    }
}