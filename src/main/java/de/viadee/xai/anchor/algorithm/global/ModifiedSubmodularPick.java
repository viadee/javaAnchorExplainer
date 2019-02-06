package de.viadee.xai.anchor.algorithm.global;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import de.viadee.xai.anchor.algorithm.AnchorConstruction;
import de.viadee.xai.anchor.algorithm.AnchorConstructionBuilder;
import de.viadee.xai.anchor.algorithm.AnchorResult;
import de.viadee.xai.anchor.algorithm.DataInstance;
import de.viadee.xai.anchor.algorithm.execution.ExecutorServiceFunction;
import de.viadee.xai.anchor.algorithm.execution.ExecutorServiceSupplier;

/**
 * Class implementing the Submodular Pick (SP) algorithm proposed by Ribeiro (2016).
 * <p>
 * This class extends the SP algorithm to account not only for feature ids but also for their values
 */
public class ModifiedSubmodularPick<T extends DataInstance<?>> extends SubmodularPick<T> {

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param constructionBuilder     the builder used to create instances of the {@link AnchorConstruction}
     *                                when running the algorithm.
     * @param maxThreads              the number of threads to obtainAnchors in parallel.
     *                                Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                thread count multiplies.
     * @param executorService         Executor to use - if this one is not clustered, this instance will be closed after
     *                                finishing computations
     * @param executorServiceSupplier used when this class is serialized (e. g. clustering)
     */
    public ModifiedSubmodularPick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads,
                                  final ExecutorService executorService,
                                  final ExecutorServiceSupplier executorServiceSupplier) {
        super(constructionBuilder, maxThreads, executorService, executorServiceSupplier);
    }

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param constructionBuilder     the builder used to create instances of the {@link AnchorConstruction}
     *                                when running the algorithm.
     * @param maxThreads              the number of threads to obtainAnchors in parallel.
     *                                Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                                thread count multiplies.
     * @param executorService         Executor to use - if this one is not clustered, this instance will be closed after
     *                                finishing computations
     * @param executorServiceFunction used when this class is serialized (e. g. clustering). maxThreads is used as
     *                                parameter
     */
    public ModifiedSubmodularPick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads,
                                  final ExecutorService executorService,
                                  final ExecutorServiceFunction executorServiceFunction) {
        super(constructionBuilder, maxThreads, executorService, executorServiceFunction);
    }

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     */
    public ModifiedSubmodularPick(BatchExplainer<T> batchExplainer, AnchorConstructionBuilder<T> constructionBuilder) {
        super(batchExplainer, constructionBuilder);
    }

    @Override
    protected CreateFeatureToColumnMapResult createFeatureToColumnMap(AnchorResult<T>[] anchorResults) {
        final AtomicInteger uniqueIndex = new AtomicInteger();
        final Map<Integer, Map<Object, Integer>> featureValueIndexMap = new HashMap<>();

        for (final AnchorResult<T> anchorResult : anchorResults)
            for (final Integer feature : anchorResult.getOrderedFeatures()) {
                Object featureValue = anchorResult.getInstance().getValue(feature);
                featureValueIndexMap
                        .computeIfAbsent(feature, (v) -> new HashMap<>())
                        .computeIfAbsent(featureValue, (v) -> uniqueIndex.getAndIncrement());
            }
        return new CreateFeatureToColumnMapResult(featureValueIndexMap, uniqueIndex.get());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected int getCandidateFeatureIndex(Map<Integer, ?> featureToColumnMap, AnchorResult anchorResult, int feature) {
        return ((Map<Integer, Map<Object, Integer>>) featureToColumnMap)
                .get(feature).get(anchorResult.getInstance().getValue(feature));
    }
}
