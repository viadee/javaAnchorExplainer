package de.viadee.anchorj.global;

import de.viadee.anchorj.AnchorConstruction;
import de.viadee.anchorj.AnchorConstructionBuilder;
import de.viadee.anchorj.AnchorResult;
import de.viadee.anchorj.DataInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class implementing the Submodular Pick (SP) algorithm proposed by Ribeiro (2016).
 * <p>
 * This class extends the SP algorithm to account not only for feature ids but also for their values
 */
public class ModifiedSubmodularPick<T extends DataInstance<?>> extends SubmodularPick<T> {

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies.
     */
    public ModifiedSubmodularPick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads) {
        super(constructionBuilder, maxThreads);
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