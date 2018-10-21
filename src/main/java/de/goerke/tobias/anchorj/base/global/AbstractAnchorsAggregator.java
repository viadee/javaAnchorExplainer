package de.goerke.tobias.anchorj.base.global;

import de.goerke.tobias.anchorj.base.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

abstract class AbstractAnchorsAggregator<T extends DataInstance<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAnchorsAggregator.class);

    private final int maxThreads;
    private final ExecutorService executorService;
    private final AnchorConstructionBuilder<T> anchorConstructionBuilder;

    /**
     * Creates an instance of the {@link AbstractAnchorsAggregator}
     *
     * @param maxThreads                the number of threads to obtainAnchors in parallel.
     *                                  Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                  thread count multiplies.
     * @param anchorConstructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                                  when running the algorithm.
     */
    protected AbstractAnchorsAggregator(final int maxThreads, final AnchorConstructionBuilder<T> anchorConstructionBuilder) {
        if (maxThreads > 1 && anchorConstructionBuilder.getMaxThreadCount() > 1)
            LOGGER.warn("AnchorConstruction threading enables. Threads will multiply to max: {} threads",
                    anchorConstructionBuilder.getMaxThreadCount() * maxThreads);
        this.maxThreads = maxThreads;
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
                for (final T instance : list) {
                    final ClassificationFunction<T> classifier = anchorConstructionBuilder.getClassificationFunction();
                    final int label = classifier.predict(instance);
                    // This section needs to be synchronized as to prevent racing conditions
                    AnchorConstruction<T> anchorConstruction;
                    synchronized (this) {
                        anchorConstruction = anchorConstructionBuilder.setupForSP(instance, label).build();
                    }
                    final AnchorResult<T> result = obtainAnchor(anchorConstruction);
                    if (result != null)
                        threadResults.add(result);
                }
            });
        }
        // TODO use invokeAll so can be reused
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
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
     * @param anchorConstruction the readied builder
     * @return the explanation result
     */
    private AnchorResult<T> obtainAnchor(final AnchorConstruction<T> anchorConstruction) {
        try {
            final AnchorResult<T> anchorResult = anchorConstruction.constructAnchor();
            if (!anchorResult.isAnchor()) {
                LOGGER.debug("Could not find an anchor for instance {}. Discarding best candidate.",
                        anchorResult.getInstance());
                return null;
            }
            return anchorResult;
        } catch (NoCandidateFoundException e) {
            LOGGER.warn("Could not find a candidate");
            return null;
        }
    }

    /**
     * @return the {@link AnchorConstructionBuilder} for use in arbitrary subclasses
     */
    protected AnchorConstructionBuilder<T> getAnchorConstructionBuilder() {
        return anchorConstructionBuilder;
    }
}
