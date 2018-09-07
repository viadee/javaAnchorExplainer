package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.AnchorCandidate;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

/**
 * Implementation of the {@link AbstractSamplingService} and extension of {@link ParallelSamplingService}.
 * <p>
 * Equally distributes the amount of samples to be obtained among all threads.
 * <p>
 * Hence, a single thread may obtain predictions for different candidates in one batch.
 */
public class BalancedParallelSamplingService extends ParallelSamplingService {
    BalancedParallelSamplingService(final BiFunction<AnchorCandidate, Integer, Double> sampleFunction,
                                    final int threadCount) {
        super(sampleFunction, threadCount);
    }

    @Override
    public AbstractSession createSession() {
        return new BalancedParallelSession();
    }

    private class BalancedParallelSession extends ParallelSession {
        @Override
        protected Collection<Callable<Object>> createCallables() {
            Collection<Callable<Object>> result = new ArrayList<>();
            // Equally distribute all calls (no matter which candidate) among the different threads
            final int totalCount = samplingCountMap.values().stream().mapToInt(i -> i).sum();
            final int countPerThread = totalCount / threadCount;
            // Division rest needs to be distributed, too
            int leftOver = totalCount % threadCount;

            final Iterator<Map.Entry<AnchorCandidate, Integer>> iterator = samplingCountMap.entrySet().iterator();
            // Check if no samples shall be taken at all
            if (!iterator.hasNext())
                return Collections.emptyList();
            Map.Entry<AnchorCandidate, Integer> current = iterator.next();

            int currentLeft = current.getValue();
            for (int i = 0; i < threadCount; i++) {
                final List<Runnable> subRunnables = new ArrayList<>();
                int threadRemainingCount = countPerThread;
                // Spread division rest over first threads
                if (leftOver > 0) {
                    threadRemainingCount++;
                    leftOver--;
                }
                while (threadRemainingCount > 0) {
                    final int sampleCount = Math.min(threadRemainingCount, currentLeft);
                    final AnchorCandidate candidate = current.getKey();
                    currentLeft -= sampleCount;
                    threadRemainingCount -= sampleCount;
                    subRunnables.add(() -> sampleFunction.apply(candidate, sampleCount));
                    if (currentLeft < 1) {
                        // Check if we reached the end
                        if (!iterator.hasNext())
                            break;
                        current = iterator.next();
                        currentLeft = current.getValue();
                    }
                }
                result.add(Executors.callable(() -> subRunnables.forEach(Runnable::run)));
            }
            return result;
        }
    }
}
