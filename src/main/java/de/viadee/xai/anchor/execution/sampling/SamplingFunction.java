package de.viadee.xai.anchor.execution.sampling;

import de.viadee.xai.anchor.AnchorCandidate;
import de.viadee.xai.anchor.ClassificationFunction;
import de.viadee.xai.anchor.DataInstance;
import de.viadee.xai.anchor.coverage.CoverageIdentification;
import de.viadee.xai.anchor.coverage.PerturbationBasedCoverageIdentification;

import java.io.Serializable;

/**
 * May be used to define custom sampling functions used by
 * {@link de.viadee.xai.anchor.execution.SamplingService} implementations
 *
 * @param <T> type of the data instance
 */
public interface SamplingFunction<T extends DataInstance<?>> extends Serializable {

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

    /**
     * Gets called when the perturbation base gets changed.
     * <p>
     * Some functions need to be renewed when this happens, see
     * {@link de.viadee.xai.anchor.global.ReconfigurablePerturbationFunction}
     *
     * @param explainedInstance the explained instance
     * @return a new {@link SamplingFunction instance}
     */
    SamplingFunction<T> notifyOriginChange(T explainedInstance) throws UnsupportedOperationException;

    /**
     * Anchors's default method of evaluating anchors' coverage is based on the perturbation space.
     * <p>
     * Thus, each samplingFunction is required to be able to create a default solution based on perturbations.
     *
     * @return a {@link CoverageIdentification} function
     */
    PerturbationBasedCoverageIdentification createPerturbationBasedCoverageIdentification();

    /**
     * Evaluating a candidate involves calling a model. This function is responsible for managing this component
     *
     * @return a {@link ClassificationFunction}
     */
    ClassificationFunction<T> getClassificationFunction();
}
