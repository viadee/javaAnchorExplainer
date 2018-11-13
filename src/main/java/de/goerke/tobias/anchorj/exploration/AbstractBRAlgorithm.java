package de.goerke.tobias.anchorj.exploration;

import de.goerke.tobias.anchorj.AnchorCandidate;
import de.goerke.tobias.anchorj.execution.SamplingService;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.*;

/**
 * Base class for the algorithms {@link BatchSAR} and {@link BatchRacing} that use (b,r) parameters.
 * <p>
 * Enables efficient threading using thread pools.
 * <p>
 * These algorithms are specified by by Jun et al in
 * "Top Arm Identification in Multi-Armed Bandits with Batch Arm Pulls".
 * <p>
 * See <a href="http://proceedings.mlr.press/v51/jun16.pdf">http://proceedings.mlr.press/v51/jun16.pdf</a>.
 * <p>
 * These algorithm offers native batch pulls enabling efficient parallelization and threading to speed up exploration.
 * Depending on the use-case, this offers significant benefits over the {@link KL_LUCB} algorithm originally proposed
 * by Ribeiro et al.
 */
abstract class AbstractBRAlgorithm implements BestAnchorIdentification {
    /**
     * Arms must be pulled in batches of size b each round
     */
    final int b;

    /**
     * One arm can be pulled at most r <= b times in a batch (If r = b then there is no constraint).
     */
    final int r;

    /**
     * Constructs the algorithm class settings its parameters.
     *
     * @param b arms are pulled in batches of size b in each round.
     * @param r any arm can be pulled at most r <= b times in a batch
     */
    AbstractBRAlgorithm(final int b, final int r) {
        if (!ParameterValidation.isUnsigned(b))
            throw new IllegalArgumentException("b budget must not be negative");
        if (!ParameterValidation.isUnsigned(r))
            throw new IllegalArgumentException("r budget must not be negative");
        if (r > b)
            throw new IllegalArgumentException("r must not be greater than b");
        this.b = b;
        this.r = r;
    }

    /**
     * Implementation of RoundRobin evaluating the specified candidates.
     *
     * @param candidates             the candidates to evaluate
     * @param samplingService        the service evaluation samples
     * @param explainedInstanceLabel the label that is being explained
     */
    void batchSample(final Set<AnchorCandidate> candidates,
                     final SamplingService samplingService,
                     final int explainedInstanceLabel) {
        final Map<AnchorCandidate, Integer> pullCountMap = new HashMap<>();
        List<AnchorCandidate> sortedBySampleSize = new ArrayList<>(candidates);
        sortedBySampleSize.sort(Comparator.comparingInt(AnchorCandidate::getSampledSize));
        Iterator<AnchorCandidate> iterator = new CyclicIterator<>(sortedBySampleSize).iterator();

        for (int i = 0; i < Math.min(b, candidates.size() * r); i++) {
            AnchorCandidate next = iterator.next();
            int currentCount = pullCountMap.getOrDefault(next, 0);
            pullCountMap.put(next, currentCount + 1);
        }
        samplingService.createSession(explainedInstanceLabel).registerCandidateEvaluation(pullCountMap).run();
    }

    /**
     * Implementation of RoundRobin evaluating the specified candidates.
     * <p>
     * Assigns pulls repeatedly until the required min count per arm is reached
     *
     * @param candidates             the candidates to evaluate
     * @param samplingService        the service evaluation samples
     * @param explainedInstanceLabel the label that is being explained
     * @param minCountEach           the minimum count every arm has to be pulled
     */
    void batchSample(final Set<AnchorCandidate> candidates,
                     final SamplingService samplingService, final int explainedInstanceLabel,
                     final int minCountEach) {
        final LinkedHashMap<AnchorCandidate, Integer> pullCountMap = new LinkedHashMap<>();
        candidates.stream().sorted(Comparator.comparingInt(AnchorCandidate::getSampledSize))
                .forEach(c -> pullCountMap.put(c, 0));

        Iterator<AnchorCandidate> iterator = new CyclicIterator<>(new ArrayList<>(pullCountMap.keySet())).iterator();

        // The first element always has fewest
        while (pullCountMap.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow(IllegalArgumentException::new).getValue() < minCountEach) {
            for (int i = 0; i < Math.min(b, candidates.size() * r); i++) {
                AnchorCandidate next = iterator.next();
                int currentCount = pullCountMap.getOrDefault(next, 0);
                pullCountMap.put(next, currentCount + 1);
            }
        }
        samplingService.createSession(explainedInstanceLabel).registerCandidateEvaluation(pullCountMap).run();
    }


    private static class CyclicIterator<T> implements Iterable<T> {
        private final List<T> coll;

        private CyclicIterator(final List<T> coll) {
            this.coll = coll;
        }

        @Override
        // @NonNull would do the trick, however, we have not imported javax.*
        @SuppressWarnings({"NullableProblems", "unchecked"})
        public Iterator<T> iterator() {
            return new Iterator() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public T next() {
                    // Added to comply with code checks.
                    // Contract of next-method required NoSuchElementException to be thrown when there are no more elems
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    final T res = coll.get(index);
                    index = (index + 1) % coll.size();
                    return res;
                }
            };
        }
    }
}
