package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Abstract service supervising the evaluation of candidates by sampling.
 * <p>
 * Its subclasses mainly enable different kinds of parallelization.
 */
public abstract class AbstractSamplingService<T extends DataInstance<?>> implements SamplingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSamplingService.class);

    private final ClassificationFunction<T> classificationFunction;
    private final PerturbationFunction<T> perturbationFunction;

    /**
     * Used to record the total time spend sampling.
     * As the service takes multiple samples at once, recording this time should not become a performance issue
     */
    private double timeSpentSampling;

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction Function used to classify any instance of type
     * @param perturbationFunction   Function used to create perturbations of the
     *                               {@link AnchorConstruction#explainedInstance}
     */
    protected AbstractSamplingService(ClassificationFunction<T> classificationFunction,
                                      PerturbationFunction<T> perturbationFunction) {
        this.classificationFunction = classificationFunction;
        this.perturbationFunction = perturbationFunction;
    }

    /**
     * Generate perturbations and evaluates a candidate by fetching the perturbed instances' predictions.
     *
     * @param candidate              the {@link AnchorCandidate} to evaluate
     * @param samplesToEvaluate      the number of samples to take
     * @param explainedInstanceLabel the explained instance label
     * @return the precision computed in this sampling run
     */
    protected double doSample(final AnchorCandidate candidate, final int samplesToEvaluate,
                            final int explainedInstanceLabel) {
        if (samplesToEvaluate < 1)
            return 0;

        final PerturbationFunction.PerturbationResult<T> perturbationResult = perturbationFunction.perturb(
                candidate.getCanonicalFeatures(), samplesToEvaluate);
        final int[] predictions = classificationFunction.predict(perturbationResult.getRawResult());

        final int matchingLabels = Math.toIntExact(IntStream.of(predictions)
                .filter(p -> p == explainedInstanceLabel).count());

        candidate.registerSamples(samplesToEvaluate, matchingLabels);

        double precision = matchingLabels / (double) predictions.length;
        LOGGER.trace("Sampling {} perturbations of {} has resulted in {} correct predictions, thus a precision of {}",
                samplesToEvaluate, candidate.getCanonicalFeatures(), matchingLabels, precision);
        return precision;
    }

    @Override
    public double getTimeSpentSampling() {
        return timeSpentSampling;
    }

    /**
     * Session object
     */
    public abstract class AbstractSamplingSession implements SamplingSession {
        private final int explainedInstanceLabel;

        // Retain order
        protected final Map<AnchorCandidate, Integer> samplingCountMap = new LinkedHashMap<>();

        /**
         * Creates an instance.
         *
         * @param explainedInstanceLabel the instance label being explained
         */
        protected AbstractSamplingSession(int explainedInstanceLabel) {
            this.explainedInstanceLabel = explainedInstanceLabel;
        }

        @Override
        public AbstractSamplingSession registerCandidateEvaluation(final AnchorCandidate candidate, int count) {
            if (samplingCountMap.containsKey(candidate))
                count += samplingCountMap.get(candidate);
            samplingCountMap.put(candidate, count);

            return this;
        }


        @Override
        public void run() {
            double time = System.currentTimeMillis();
            execute();
            time = System.currentTimeMillis() - time;
            timeSpentSampling += time;
            LOGGER.debug("Evaluated a total of {} samples for {} candidates in {}ms",
                    samplingCountMap.values().stream().mapToInt(i -> i).sum(),
                    samplingCountMap.entrySet().size(), time);
        }

        /**
         * Generate perturbations and evaluates a candidate by fetching the perturbed instances' predictions.
         *
         * @param candidate         the {@link AnchorCandidate} to evaluate
         * @param samplesToEvaluate the number of samples to take
         * @return the precision computed in this sampling run
         */
        protected double doSample(final AnchorCandidate candidate, final int samplesToEvaluate) {
            return AbstractSamplingService.this.doSample(candidate, samplesToEvaluate, explainedInstanceLabel);
        }

        /**
         * Executes the session internally
         */
        protected abstract void execute();
    }
}
