package de.goerke.tobias.anchorj.base.global;

import de.goerke.tobias.anchorj.base.AnchorConstruction;
import de.goerke.tobias.anchorj.base.AnchorConstructionBuilder;
import de.goerke.tobias.anchorj.base.AnchorResult;
import de.goerke.tobias.anchorj.base.DataInstance;
import de.goerke.tobias.anchorj.tabular.TabularInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Class implementing the Submodular Pick (SP) algorithm proposed by Ribeiro (2016).
 * <p>
 * In the showcase example this class gets extended and enabled to be used in conjunction with Apache Spark.
 * Due to this projects dependency-less design, it is not included in the core project.
 */
public class ModifiedSubmodularPick<T extends DataInstance<?>> extends SubmodularPick<T> {

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param optimizationGoal    the optimization goal
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies.
     */
    public ModifiedSubmodularPick(AnchorConstructionBuilder<T> constructionBuilder, SubmodularPickGoal optimizationGoal,
                                  int maxThreads) {
        super(constructionBuilder, optimizationGoal, maxThreads);
    }

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param optimizationGoal    the optimization goal
     */
    public ModifiedSubmodularPick(BatchExplainer<T> batchExplainer, AnchorConstructionBuilder<T> constructionBuilder,
                                  SubmodularPickGoal optimizationGoal) {
        super(batchExplainer, constructionBuilder, optimizationGoal);
    }

    @Override
    protected double[][] createImportanceMatrix(AnchorResult<T>[] anchorResults) {
        // Build matrix
        // 2. Assign a unique column uniqueIndex to each feature
        final Map<Integer, Map<Object, Integer>> featureValueIndexMap = new HashMap<>();
        final AtomicInteger uniqueIndex = new AtomicInteger();
        for (final AnchorResult<T> anchorResult : anchorResults)
            for (final Integer feature : anchorResult.getOrderedFeatures()) {
                Object featureValue = anchorResult.getInstance().getFeature(feature);
                featureValueIndexMap
                        .computeIfAbsent(feature, (v) -> new HashMap<>())
                        .computeIfAbsent(featureValue, (v) -> uniqueIndex.getAndIncrement());
            }

        // 3. Calculate cell importance matrix, which later gets transformed to importance matrix
        final double[][] importanceMatrix = new double[anchorResults.length][uniqueIndex.get()];
        for (int i = 0; i < importanceMatrix.length; i++) {
            for (final int feature : anchorResults[i].getOrderedFeatures()) {
                Integer colIndex = featureValueIndexMap.get(feature)
                        .get(anchorResults[i].getInstance().getFeature(feature));
                importanceMatrix[i][colIndex] = getOptimizationGoal().computeFeatureImportance(anchorResults[i], feature);
            }
        }
        return importanceMatrix;
    }
}