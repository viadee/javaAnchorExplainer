package de.viadee.xai.anchor.algorithm.execution;

import java.util.Map;

import de.viadee.xai.anchor.algorithm.AnchorCandidate;
import de.viadee.xai.anchor.algorithm.ClassificationFunction;
import de.viadee.xai.anchor.algorithm.DataInstance;
import de.viadee.xai.anchor.algorithm.PerturbationFunction;
import de.viadee.xai.anchor.algorithm.execution.sampling.SamplingFunction;

/**
 * Implementation of the {@link AbstractSamplingService} sequentially obtaining all samples.
 *
 * @param <T> Type of the sampled instance
 */
public class LinearSamplingService<T extends DataInstance<?>> extends AbstractSamplingService<T> {
    private static final long serialVersionUID = -2145854454277378170L;

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction Function used to classify any instance of type
     * @param perturbationFunction   Function used to create perturbations of the explained instance
     */
    public LinearSamplingService(ClassificationFunction<T> classificationFunction,
                                 PerturbationFunction<T> perturbationFunction) {
        super(classificationFunction, perturbationFunction);
    }

    /**
     * Creates the sampling service.
     *
     * @param samplingFunction the sampling function to be used
     */
    public LinearSamplingService(SamplingFunction samplingFunction) {
        super(samplingFunction);
    }

    @Override
    public SamplingSession createSession(int explainedInstanceLabel) {
        return new LinearSession(explainedInstanceLabel);
    }

    @Override
    public SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction) {
        return new LinearSamplingService<>(samplingFunction);
    }

    private class LinearSession extends AbstractSamplingSession {
        private static final long serialVersionUID = -5683739179207561142L;

        /**
         * Creates an instance.
         *
         * @param explainedInstanceLabel the explained instance label
         */
        private LinearSession(int explainedInstanceLabel) {
            super(explainedInstanceLabel);
        }

        @Override
        public void execute() {
            for (Map.Entry<AnchorCandidate, Integer> entry : samplingCountMap.entrySet())
                doSample(entry.getKey(), entry.getValue());
        }
    }
}
