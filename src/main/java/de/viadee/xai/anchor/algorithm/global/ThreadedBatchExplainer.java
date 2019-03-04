package de.viadee.xai.anchor.algorithm.global;

import de.viadee.xai.anchor.algorithm.*;
import de.viadee.xai.anchor.algorithm.execution.ExecutorServiceFunction;
import de.viadee.xai.anchor.algorithm.execution.ExecutorServiceSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

    private final ExecutorServiceSupplier executorServiceSupplier;

    private final ExecutorServiceFunction executorServiceFunction;

    /**
     * Creates an instance of the {@link ThreadedBatchExplainer}
     *
     * @param executorService         Executor to use - if this one is not clustered, this instance will be closed after
     *                                finishing computations
     * @param executorServiceFunction used when this class is serialized (e. g. clustering). maxThreads is used as
     *                                parameter
     */
    public ThreadedBatchExplainer(final int maxThreads, final ExecutorService executorService,
                                  final ExecutorServiceFunction executorServiceFunction) {
        this.maxThreads = maxThreads;
        this.executorService = executorService;
        this.executorServiceFunction = executorServiceFunction;
        this.executorServiceSupplier = null;
    }

    /**
     * Creates an instance of the {@link ThreadedBatchExplainer}
     *
     * @param executorService         Executor to use - if this one is not clustered, this instance will be closed after
     *                                finishing computations
     * @param executorServiceSupplier used when this class is serialized (e. g. clustering)
     */
    public ThreadedBatchExplainer(final int maxThreads, final ExecutorService executorService,
                                  final ExecutorServiceSupplier executorServiceSupplier) {
        this.maxThreads = maxThreads;
        this.executorService = executorService;
        this.executorServiceFunction = null;
        this.executorServiceSupplier = executorServiceSupplier;
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
            final AnchorResult<T> anchorResult = anchorConstruction.constructAnchor(false);
            if (!anchorResult.isAnchor()) {
                LOGGER.info("Could not find an anchor for instance {}. Discarding best candidate",
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
        final List<List<T>> splitLists = SubmodularPickUtils.splitList(instances,
                instances.size() / this.maxThreads);
        final Collection<AnchorResult<T>> threadResults = new ArrayList<>();
        List<Callable<List<AnchorResult<T>>>> callables = new ArrayList<>();
        for (final List<T> list : splitLists) {
            callables.add(new AnchorCallable(anchorConstructionBuilder, list));
        }

        ExecutorService executorService = null;
        try {
            if (this.executorService != null) {
                executorService = this.executorService;
            } else if (this.executorServiceFunction != null) {
                executorService = this.executorServiceFunction.apply(this.maxThreads);
            } else if (this.executorServiceSupplier != null) {
                executorService = this.executorServiceSupplier.get();
            } else {
                throw new NullPointerException("ExecutorService, Supplier and Function are null");
            }

            List<Future<List<AnchorResult<T>>>> resultLists = executorService.invokeAll(callables);
            for (Future<List<AnchorResult<T>>> future : resultLists) {
                threadResults.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
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
            for (final T instance : this.list) {
                // This section needs to be synchronized as to prevent racing conditions
                AnchorConstruction<T> anchorConstruction = AnchorConstructionBuilder
                        .buildForSP(this.anchorConstructionBuilder, instance);
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
        this.executorService = null;
    }
}
