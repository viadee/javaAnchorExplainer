package de.viadee.xai.anchor.algorithm.exploration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import de.viadee.xai.anchor.algorithm.AnchorCandidate;
import de.viadee.xai.anchor.algorithm.execution.AbstractSamplingService;
import de.viadee.xai.anchor.algorithm.execution.SamplingService;
import de.viadee.xai.anchor.algorithm.execution.SamplingSession;

/**
 * This class implements the Median Elimination algorithm as proposed by Even-Dar et. al in
 * "Action Elimination and Stopping Conditions for the Multi-Armed Bandit and Reinforcement Learning Problems".
 * <p>
 * The algorithm is a 1-best arm selection method that runs in O((n / epsilon) * log (1 / delta))
 * <p>
 * Naturally, this algorithm may be extended to search for K best candidates.
 * <p>
 * Therefore, this bandit is especially useful when clear theoretical and statistical guarantees regarding asymptotic
 * complexity are required
 */
public class MedianElimination implements BestAnchorIdentification {
    private static final long serialVersionUID = 4039788473508865431L;

    /**
     * This method implements the actual algorithm as proposed
     *
     * <pre>
     *     Input: epsilon > 0, delta > 0
     *     Output: An arm
     *
     *     Set
     *          S_1 = A,
     *          epsilon_1 = epsilon / 4,
     *          delta_1 = delta/2,
     *          l = 1
     *     repeat
     *         Sample every arm a in S_l for 1/(epsilon_l / 2)^2 * log(3/delta_l) times
     *         Let p_l^a denote its empirical value
     *
     *         Find the median of p_l^a denoted by m_l
     *
     *         S_{l+1} = S_l \ {a: p_l^a < m_l};
     *         epsilon_{l+1} = 3/4 * epsilon_l
     *         delta_{l+1} = delta_l / 2;
     *         l += 1
     *     until |S_l| = 1
     * </pre>
     *
     * @param candidates      the candidates to inspect
     * @param samplingService an implementation of the {@link AbstractSamplingService}, controlling the evaluation of
     *                        samples. Allows for threading.
     * @param delta           the probability of identifying the correct result == confidence
     * @param epsilon         the maximum error == tolerance
     * @return the best candidate satisfying the specified parameters
     */
    private static AnchorCandidate identifySingle(List<AnchorCandidate> candidates,
                                                  SamplingService samplingService,
                                                  int explainedInstanceLabel, double delta, double epsilon) {
        if (candidates.size() == 1)
            return candidates.get(0);

        final List<AnchorCandidate> s = new ArrayList<>(candidates);
        double epsilon1 = epsilon / 4;
        double delta1 = delta / 2;
        do {
            final int sampleCount = (int) (1D / Math.pow((epsilon1 / 2D), 2) * Math.log(3D / delta1));

            final SamplingSession session = samplingService.createSession(explainedInstanceLabel);
            for (AnchorCandidate candidate : s) {
                session.registerCandidateEvaluation(candidate, sampleCount);
            }
            session.run();

            s.sort(Comparator.comparingDouble(AnchorCandidate::getPrecision));
            final ListIterator<AnchorCandidate> iterator = s.listIterator(s.size() / 2);
            while (iterator.hasPrevious()) {
                iterator.previous();
                iterator.remove();
            }
            epsilon1 = 3D / 4D * epsilon1;
            delta1 = delta1 / 2D;
        } while (s.size() > 1);

        return s.get(0);
    }

    @Override
    public List<AnchorCandidate> identify(List<AnchorCandidate> candidates, SamplingService samplingService,
                                          int explainedInstanceLabel,
                                          double delta, double epsilon, int nrOfResults) {
        final List<AnchorCandidate> remainingCandidates = new ArrayList<>(candidates);
        final List<AnchorCandidate> result = new ArrayList<>();

        while (result.size() != nrOfResults) {
            final AnchorCandidate currentBest = identifySingle(remainingCandidates, samplingService,
                    explainedInstanceLabel, delta, epsilon);
            remainingCandidates.remove(currentBest);
            result.add(currentBest);
        }

        return result;
    }
}
