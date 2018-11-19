package de.viadee.anchorj.execution.sampling;

import de.viadee.anchorj.AnchorCandidate;

/**
 * May be used to define custom sampling functions used by
 * {@link de.viadee.anchorj.execution.SamplingService} implementations
 */
public interface SamplingFunction {

    /**
     * Evaluates a candidate using a defined amount of samples to be taken.
     * A candidate evaluates to the fraction of evaluations that equal the explained instance label.
     * <p>
     * All sampling processes need to be registered using the {@link AnchorCandidate#registerSamples(int, int)} function
     * <p>
     * Generate perturbations and evaluates a candidate by fetching the perturbed instances' predictions.
     *
     * @param candidate              the {@link AnchorCandidate} to evaluate
     * @param samplesToEvaluate      the number of samples to take
     * @param explainedInstanceLabel the explained instance label
     * @return the precision computed in this sampling run
     */
    double evaluate(final AnchorCandidate candidate, final int samplesToEvaluate, final int explainedInstanceLabel);
}
