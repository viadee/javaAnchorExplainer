package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.ClassificationFunction;
import de.goerke.tobias.anchorj.base.DataInstance;
import de.goerke.tobias.anchorj.base.PerturbationFunction;
import de.goerke.tobias.anchorj.base.execution.sampling.SamplingFunction;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Implementation of the {@link AbstractSamplingService} sequentially obtaining all samples.
 *
 * @param <T> Type of the sampled instance
 */
public class LinearSamplingService<T extends DataInstance<?>> extends AbstractSamplingService<T> {

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

    private class LinearSession extends AbstractSamplingSession {
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
