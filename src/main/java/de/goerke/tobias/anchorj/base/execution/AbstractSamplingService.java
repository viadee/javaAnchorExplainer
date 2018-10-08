package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Abstract service supervising the evaluation of candidates by sampling.
 * <p>
 * Its subclasses mainly enable different kinds of parallelization.
 */
public abstract class AbstractSamplingService implements SamplingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSamplingService.class);

    final BiFunction<AnchorCandidate, Integer, Double> sampleFunction;

    /**
     * Used to record the total time spend sampling.
     * As the service takes multiple samples at once, recording this time should not become a performance issue
     */
    private double timeSpentSampling;

    /**
     * Instantiated the sampling service
     *
     * @param sampleFunction the function used for sampling
     */
    protected AbstractSamplingService(final BiFunction<AnchorCandidate, Integer, Double> sampleFunction) {
        this.sampleFunction = sampleFunction;
    }


    @Override
    public double getTimeSpentSampling() {
        return timeSpentSampling;
    }

    /**
     * Session object
     */
    public abstract class AbstractSamplingSession implements SamplingSession {
        // Retain order
        final Map<AnchorCandidate, Integer> samplingCountMap = new LinkedHashMap<>();

        /**
         * Creates an instance.
         */
        protected AbstractSamplingSession() {
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
         * Executes the session internally
         */
        protected abstract void execute();
    }
}
