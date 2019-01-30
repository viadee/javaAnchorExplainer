package de.viadee.xai.anchor.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.viadee.xai.anchor.AnchorCandidate;
import de.viadee.xai.anchor.AnchorConstruction;
import de.viadee.xai.anchor.AnchorConstructionBuilder;
import de.viadee.xai.anchor.AnchorResult;
import de.viadee.xai.anchor.DataInstance;

/**
 * {@link CoveragePick} is a global explainer aiming to maximize the result's coverage.
 * <p>
 * Thereby, it only returns results whose coverage is additive.
 *
 * @param <T> Type of the explained instance
 */
public class CoveragePick<T extends DataInstance<?>> extends AbstractGlobalExplainer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoveragePick.class);

    private final boolean includeTargetValue;

    /**
     * Creates the instance.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies.
     */
    public CoveragePick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads) {
        super(constructionBuilder, maxThreads);
        this.includeTargetValue = false;
    }

    /**
     * Creates the instance.
     *
     * @param includeTargetValue  if set true, the algorithm will consider target value like feature values.
     *                            However, this causes the coverage to be only addable within each target value class
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     */
    public CoveragePick(boolean includeTargetValue, BatchExplainer<T> batchExplainer,
                        AnchorConstructionBuilder<T> constructionBuilder) {
        super(batchExplainer, constructionBuilder);
        this.includeTargetValue = includeTargetValue;
    }

    @Override
    List<AnchorResult<T>> pickExplanations(AnchorResult<T>[] explanations, int nrOfExplanationsDesired) {
        final List<AnchorResult<T>> survivors = new ArrayList<>(Arrays.asList(explanations));
        final List<AnchorResult<T>> result = new ArrayList<>();

        for (int i = 0; i < nrOfExplanationsDesired; i++) {
            // Find element with best coverage
            ListIterator<AnchorResult<T>> iter = survivors.listIterator();
            double bestCoverage = -1;
            int bestIndex = -1;
            while (iter.hasNext()) {
                final AnchorResult<T> current = iter.next();
                if (current.getCoverage() > bestCoverage) {
                    bestCoverage = current.getCoverage();
                    bestIndex = iter.previousIndex();
                }
            }
            if (bestIndex < 0)
                break;

            final AnchorResult<T> bestExplanation = survivors.remove(bestIndex);
            result.add(bestExplanation);

            // Now remove all other explanations from survivors that contain features of the best explanation
            iter = survivors.listIterator();
            while (iter.hasNext()) {
                final AnchorResult<T> current = iter.next();
                final boolean hasSameLabel = !includeTargetValue || bestExplanation.getLabel() == current.getLabel();
                final boolean hasSameFeatureValue = current.getCanonicalFeatures().stream().anyMatch(feature ->
                        bestExplanation.getCanonicalFeatures().contains(feature) && current.getInstance()
                                .getValue(feature).equals(bestExplanation.getInstance().getValue(feature)));
                if (hasSameLabel && hasSameFeatureValue) {
                    iter.remove();
                }
            }
        }

        if (includeTargetValue) {
            result.stream().map(AnchorResult::getLabel).distinct().forEach(label -> {
                final Double resultCoverage = result.stream().filter(a -> a.getLabel() == label)
                        .map(AnchorCandidate::getCoverage).reduce((x, y) -> x + y)
                        .orElse(0D);
                LOGGER.info("The returned {} results for label {} exclusively cover a total of {}% of the mode's input",
                        result.size(), label, resultCoverage);
            });
        } else {
            final Double resultCoverage = result.stream().map(AnchorCandidate::getCoverage).reduce((x, y) -> x + y)
                    .orElse(0D);
            LOGGER.info("The returned {} results exclusively cover a total of {}% of the mode's input",
                    result.size(), resultCoverage);
        }
        return result;
    }
}
