package de.viadee.xai.anchor.algorithm.global;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.viadee.xai.anchor.algorithm.AnchorConstruction;
import de.viadee.xai.anchor.algorithm.AnchorConstructionBuilder;
import de.viadee.xai.anchor.algorithm.AnchorResult;
import de.viadee.xai.anchor.algorithm.DataInstance;
import de.viadee.xai.anchor.algorithm.NoCandidateFoundException;

/**
 * Default batch explainer using threads to obtain multiple results
 *
 * @param <T> Type of the explained instance
 */
public class ThreadedBatchExplainer<T extends DataInstance<?>> implements BatchExplainer<T> {
    private static final long serialVersionUID = -4681054503306584585L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadedBatchExplainer.class);

    private final int maxThreads;
    private transient ExecutorService executorService;

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
        // TODO may re add changes of branche fix-parallelization
        final List<List<T>> splitLists = SubmodularPickUtils.splitList(instances, instances.size() / maxThreads);
        final Collection<AnchorResult<T>> threadResults = new ArrayList<>();
        List<Callable<List<AnchorResult<T>>>> callables = new ArrayList<>();
        for (final List<T> list : splitLists) {
            callables.add(new AnchorCallable(anchorConstructionBuilder, list));
        }
        try {
            List<Future<List<AnchorResult<T>>>> resultLists = executorService.invokeAll(callables);
            for (Future<List<AnchorResult<T>>> future : resultLists) {
                threadResults.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        }

        //noinspection unchecked
        return threadResults.toArray((AnchorResult<T>[]) new AnchorResult[0]);
    }

    private class AnchorCallable implements Callable<List<AnchorResult<T>>> {
        private final List<T> list;
        private final AnchorConstructionBuilder<T> anchorConstructionBuilder;

        AnchorCallable(AnchorConstructionBuilder<T> anchorConstructionBuilder, List<T> list) {
            this.list = list;
            this.anchorConstructionBuilder = anchorConstructionBuilder;
        }

        @Override
        public List<AnchorResult<T>> call() {
            List<AnchorResult<T>> localResult = new ArrayList<>();
            for (final T instance : list) {
                // This section needs to be synchronized as to prevent racing conditions
                AnchorConstruction<T> anchorConstruction = AnchorConstructionBuilder
                        .buildForSP(anchorConstructionBuilder, instance);
                final AnchorResult<T> result = obtainAnchor(anchorConstruction);
                if (result != null) {
                    localResult.add(result);
                }
            }
            return localResult;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.executorService = Executors.newFixedThreadPool(this.maxThreads);
    }
}
