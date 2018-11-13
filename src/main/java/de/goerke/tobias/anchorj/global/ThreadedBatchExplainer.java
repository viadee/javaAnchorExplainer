package de.goerke.tobias.anchorj.global;

import de.goerke.tobias.anchorj.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Default batch explainer using threads to obtain multiple results
 *
 * @param <T> Type of the explained instance
 */
public class ThreadedBatchExplainer<T extends DataInstance<?>> implements BatchExplainer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedBatchExplainer.class);

    private final int maxThreads;
    private final ExecutorService executorService;

    /**
     * Creates an instance of the {@link ThreadedBatchExplainer}
     *
     * @param maxThreads the number of threads to obtainAnchors in parallel.
     *                   Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                   thread count multiplies.
     */
    public ThreadedBatchExplainer(final int maxThreads) {
        this.maxThreads = maxThreads;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
    }

    /**
     * Explains a specified instance.
     * <p>
     * For this, the builder is modified to setup and reconfigure all required functions. E.g. the perturbation function
     * usually perturbs one fixed instance that needs to be changed beforehand
     *
     * @param anchorConstruction the readied builder
     * @param <T>                type of the explained instance
     * @return the explanation result
     */
    private static <T extends DataInstance<?>> AnchorResult<T> obtainAnchor(final AnchorConstruction<T> anchorConstruction) {
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

    @Override
    public AnchorResult<T>[] obtainAnchors(AnchorConstructionBuilder<T> anchorConstructionBuilder, List<T> instances) {
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
}
