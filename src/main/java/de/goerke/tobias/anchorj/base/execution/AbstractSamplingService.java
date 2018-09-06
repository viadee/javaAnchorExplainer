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
public abstract class AbstractSamplingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSamplingService.class);

    final BiFunction<AnchorCandidate, Integer, Double> sampleFunction;

    /**
     * Instantiated the sampling service
     *
     * @param sampleFunction the function used for sampling
     */
    protected AbstractSamplingService(final BiFunction<AnchorCandidate, Integer, Double> sampleFunction) {
        this.sampleFunction = sampleFunction;
    }

    /**
     * Instantiates and returns an appropriate implementation.
     *
     * @param sampleFunction the sampling function
     * @param threadCount    number of threads to use
     * @param doBalance      if true, the work will be equally split among all threads
     * @return the appropriate service. Either {@link LinearSamplingService}, {@link ParallelSamplingService} or
     * {@link BalancedParallelSamplingService}
     */
    public static AbstractSamplingService createExecution(final BiFunction<AnchorCandidate, Integer, Double> sampleFunction,
                                                          final int threadCount, final boolean doBalance) {
        if (threadCount <= 1)
            return new LinearSamplingService(sampleFunction);
        if (!doBalance)
            return new ParallelSamplingService(sampleFunction, threadCount);
        return new BalancedParallelSamplingService(sampleFunction, threadCount);
    }

    /**
     * Creates a session that has to be used in order to obtain samples.
     *
     * @return an {@link AbstractSession} instance.
     */
    public abstract AbstractSession createSession();

    /**
     * Session object
     */
    public abstract class AbstractSession {
        // Retain order
        final Map<AnchorCandidate, Integer> samplingCountMap = new LinkedHashMap<>();

        /**
         * Creates an instance.
         */
        protected AbstractSession() {
        }

        /**
         * Registers a candidate to be evaluated.
         * <p>
         * If the candidate is already contained, the number of new executions will be added.
         *
         * @param candidate the candidate to evaluate
         * @param count     the number of samples to obtain
         * @return this session object which can be executed
         */
        public AbstractSession registerCandidateEvaluation(final AnchorCandidate candidate, int count) {
            if (samplingCountMap.containsKey(candidate))
                count += samplingCountMap.get(candidate);
            samplingCountMap.put(candidate, count);

            return this;
        }

        /**
         * Delegates a map of sample executions to
         * {@link AbstractSession#registerCandidateEvaluation(AnchorCandidate, int)}.
         *
         * @param map the mapping of candidates to execution counts
         * @return this session object which can be executed
         */
        public AbstractSession registerCandidateEvaluation(final Map<AnchorCandidate, Integer> map) {
            for (Map.Entry<AnchorCandidate, Integer> entry : map.entrySet())
                registerCandidateEvaluation(entry.getKey(), entry.getValue());

            return this;
        }

        /**
         * Executes the requested samples
         */
        public void run() {
            final double start = System.currentTimeMillis();
            execute();
            LOGGER.debug("Evaluated a total of {} samples for {} candidates in {}ms",
                    samplingCountMap.values().stream().mapToInt(i -> i).sum(),
                    samplingCountMap.entrySet().size(), (System.currentTimeMillis() - start));
        }

        /**
         * Executes the session internally
         */
        protected abstract void execute();
    }
}
