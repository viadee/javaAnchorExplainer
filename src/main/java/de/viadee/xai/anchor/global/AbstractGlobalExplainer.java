package de.viadee.xai.anchor.global;

import de.viadee.xai.anchor.AnchorConstruction;
import de.viadee.xai.anchor.AnchorConstructionBuilder;
import de.viadee.xai.anchor.AnchorResult;
import de.viadee.xai.anchor.DataInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Provides default functionality for obtaining multiple explanations.
 * <p>
 * May be used for all model-agnostic aggregation based explainers
 *
 * @param <T> Type of the instance
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractGlobalExplainer<T extends DataInstance<?>> implements GlobalExplainer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmodularPick.class);

    protected final BatchExplainer<T> batchExplainer;

    protected final AnchorConstructionBuilder<T> constructionBuilder;

    /**
     * Creates the instance.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies if executed locally.
     */
    public AbstractGlobalExplainer(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads) {
        this(new ThreadedBatchExplainer<>(maxThreads), constructionBuilder);
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
        final AnchorResult<T>[] anchorResults = batchExplainer.obtainAnchors(constructionBuilder, instances);
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
