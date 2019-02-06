package de.viadee.xai.anchor.algorithm.global;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.viadee.xai.anchor.algorithm.AnchorConstruction;
import de.viadee.xai.anchor.algorithm.AnchorConstructionBuilder;
import de.viadee.xai.anchor.algorithm.AnchorResult;
import de.viadee.xai.anchor.algorithm.DataInstance;
import de.viadee.xai.anchor.algorithm.execution.ExecutorServiceFunction;
import de.viadee.xai.anchor.algorithm.execution.ExecutorServiceSupplier;

/**
 * Provides default functionality for obtaining multiple explanations.
 * <p>
 * May be used for all model-agnostic aggregation based explainers
 *
 * @param <T> Type of the instance
 */
public abstract class AbstractGlobalExplainer<T extends DataInstance<?>> implements GlobalExplainer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);

    protected final BatchExplainer<T> batchExplainer;

    protected final AnchorConstructionBuilder<T> constructionBuilder;

    /**
     * Creates the instance.
     *
     * @param constructionBuilder     the builder used to create instances of the {@link AnchorConstruction}
     *                                when running the algorithm.
     * @param maxThreads              the number of threads to obtainAnchors in parallel.
     *                                Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                thread count multiplies if executed locally.
     * @param executorService         Executor to use - if this one is not clustered, this instance will be closed after
     *                                finishing computations
     * @param executorServiceSupplier used when this class is serialized (e. g. clustering)
     */
    public AbstractGlobalExplainer(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads,
                                   final ExecutorService executorService,
                                   final ExecutorServiceSupplier executorServiceSupplier) {
        this(new ThreadedBatchExplainer<>(maxThreads, executorService, executorServiceSupplier), constructionBuilder);
    }

    /**
     * Creates the instance.
     *
     * @param constructionBuilder     the builder used to create instances of the {@link AnchorConstruction}
     *                                when running the algorithm.
     * @param maxThreads              the number of threads to obtainAnchors in parallel.
     *                                Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                thread count multiplies if executed locally.
     * @param executorService         Executor to use - if this one is not clustered, this instance will be closed after
     *                                finishing computations
     * @param executorServiceFunction used when this class is serialized (e. g. clustering). maxThreads is used as
     *                                parameter
     */
    public AbstractGlobalExplainer(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads,
                                   final ExecutorService executorService,
                                   final ExecutorServiceFunction executorServiceFunction) {
        this(new ThreadedBatchExplainer<>(maxThreads, executorService, executorServiceFunction), constructionBuilder);
    }

    /**
     * Creates the instance.
     *
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     */
    public AbstractGlobalExplainer(BatchExplainer<T> batchExplainer, AnchorConstructionBuilder<T> constructionBuilder) {
        this.batchExplainer = batchExplainer;
        this.constructionBuilder = constructionBuilder;
    }

    @Override
    public List<AnchorResult<T>> run(final List<T> instances, final int nrOfExplanationsDesired) {
        if (instances == null || instances.isEmpty())
            return Collections.emptyList();

        final double startTime = System.currentTimeMillis();
        // 1. Obtain the anchors - explanation matrix W
        final AnchorResult<T>[] anchorResults = batchExplainer.obtainAnchors(this.constructionBuilder, instances);
        LOGGER.info("Took {} ms for gathering all explanations", (System.currentTimeMillis() - startTime));

        return pickExplanations(anchorResults, nrOfExplanationsDesired);
    }

    /**
     * Executes the actual algorithm using the aggregated explanations
     *
     * @param explanations            the explanations
     * @param nrOfExplanationsDesired desired number of explanations to pick
     * @return the AnchorResults
     */
    abstract List<AnchorResult<T>> pickExplanations(final AnchorResult<T>[] explanations, final int nrOfExplanationsDesired);
}
