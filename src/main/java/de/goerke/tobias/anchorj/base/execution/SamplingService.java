package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.AnchorCandidate;

import java.util.function.BiFunction;

/**
 * Interface defining all methods a sampling service is obligated to provide.
 */
public interface SamplingService {

    /**
     * Instantiates and returns an appropriate implementation.
     *
     * @param sampleFunction the sampling function
     * @param threadCount    number of threads to use
     * @param doBalance      if true, the work will be equally split among all threads
     * @return the appropriate service. Either {@link LinearSamplingService}, {@link ParallelSamplingService} or
     * {@link BalancedParallelSamplingService}
     */
    static SamplingService createDefaultExecution(BiFunction<AnchorCandidate, Integer, Double> sampleFunction,
                                                  int threadCount, boolean doBalance) {
        if (threadCount <= 1)
            return new LinearSamplingService(sampleFunction);
        if (!doBalance)
            return new ParallelSamplingService(sampleFunction, threadCount);
        return new BalancedParallelSamplingService(sampleFunction, threadCount);
    }

    /**
     * Returns the time spent taking samples
     *
     * @return the time spent taking samples in milliseconds
     */
    double getTimeSpentSampling();

    /**
     * Creates a session that has to be used in order to obtain samples.
     *
     * @return an {@link SamplingSession} instance.
     */
    SamplingSession createSession();
}
