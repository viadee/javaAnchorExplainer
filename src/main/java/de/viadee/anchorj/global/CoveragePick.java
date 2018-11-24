package de.viadee.anchorj.global;

import de.viadee.anchorj.AnchorConstruction;
import de.viadee.anchorj.AnchorConstructionBuilder;
import de.viadee.anchorj.AnchorResult;
import de.viadee.anchorj.DataInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link CoveragePick} is a global explainer aiming to maximize the result's coverage.
 * <p>
 * Thereby, it only returns results whose coverage is additive.
 *
 * @param <T> Type of the explained instance
 */
public class CoveragePick<T extends DataInstance<?>> extends AbstractGlobalExplainer<T> {

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     * @param maxThreads          the number of threads to obtainAnchors in parallel.
     *                            Note: if threading is enabled in the anchorConstructionBuilder, the actual
     *                            thread count multiplies.
     */
    public CoveragePick(AnchorConstructionBuilder<T> constructionBuilder, int maxThreads) {
        super(constructionBuilder, maxThreads);
    }

    /**
     * Creates an instance of the {@link SubmodularPick}.
     *
     * @param batchExplainer      the {@link BatchExplainer} to be used to obtain multiple explanations
     * @param constructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                            when running the algorithm.
     */
    public CoveragePick(BatchExplainer<T> batchExplainer, AnchorConstructionBuilder<T> constructionBuilder) {
        super(batchExplainer, constructionBuilder);
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

            AnchorResult<T> bestExplanation = survivors.remove(bestIndex);
            result.add(bestExplanation);

            // Now remove all other explanations from survivors that contain features of the best explanation
            iter = survivors.listIterator();
            while (iter.hasNext()) {
                final AnchorResult<T> current = iter.next();
                if (current.getCanonicalFeatures().stream()
                        .anyMatch(feature -> bestExplanation.getCanonicalFeatures().contains(feature) &&
                                current.getInstance().getValue(feature).equals(bestExplanation.getInstance().getValue(feature)))) {
                    iter.remove();
                }
            }
        }

        return result;
    }
}