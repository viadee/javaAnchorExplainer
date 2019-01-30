package de.viadee.xai.anchor.algorithm.global;

import de.viadee.xai.anchor.algorithm.AnchorResult;
import de.viadee.xai.anchor.algorithm.DataInstance;

import java.util.Arrays;
import java.util.List;

/**
 * Depicts the functionality a global explainer is expected to provide
 *
 * @param <T> Type of the DataInstance being explained
 */
public interface GlobalExplainer<T extends DataInstance<?>> {

    /**
     * Executes the global explainer.
     * <p>
     * Produces multiple results that are most important for the global explanation of a model.
     *
     * @param instances               the instances to obtain explanations for.
     * @param nrOfExplanationsDesired the max result number to generate
     * @return a {@link List} of {@link AnchorResult}s
     */
    default List<AnchorResult<T>> run(T[] instances, int nrOfExplanationsDesired) {
        return run(Arrays.asList(instances), nrOfExplanationsDesired);
    }

    /**
     * Executes the global explainer.
     * <p>
     * Produces multiple results that are most important for the global explanation of a model.
     *
     * @param instances               the instances to obtain explanations for.
     * @param nrOfExplanationsDesired the max result number to generate
     * @return a {@link List} of {@link AnchorResult}s
     */
    List<AnchorResult<T>> run(List<T> instances, int nrOfExplanationsDesired);
}
