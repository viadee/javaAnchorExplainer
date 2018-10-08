package de.goerke.tobias.anchorj.base.exploration;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.execution.SamplingService;

import java.util.*;

/**
 * Implementation of the BatchRacing algorithm proposed by Jun et al in
 * "Top Arm Identification in Multi-Armed Bandits with Batch Arm Pulls".
 * <p>
 * See <a href="http://proceedings.mlr.press/v51/jun16.pdf">http://proceedings.mlr.press/v51/jun16.pdf</a>.
 * <p>
 * This algorithm offers native batch pulls that enable efficient parallelization and threading to speed up exploration.
 * Depending on the use-case, this offers significant benefits over the {@link KL_LUCB} algorithm originally proposed
 * by Ribeiro et al.
 * <p>
 * This algorithm is to be used in a fixed confidence setting.
 * <p>
 * This algorithm ignores the epsilon value. Thus using it negates some of Anchors' properties!
 * <p>
 * If the fixed budget setting is to be used, {@link BatchSAR} may be used.
 */
public class BatchRacing extends AbstractBRAlgorithm {

    /**
     * Constructs the algorithm class settings its parameters.
     * <p>
     * Algorithm will be executed sequentially.
     *
     * @param br sets batch size and batch restriction equally so that there are no arm pull restrictions
     */
    public BatchRacing(int br) {
        this(br, br);
    }

    /**
     * Constructs the algorithm class settings its parameters.
     * <p>
     * Algorithm will be executed sequentially.
     *
     * @param b arms are pulled in batches of size b in each round.
     * @param r any arm can be pulled at most r &lt;= b times in a batch
     */
    public BatchRacing(int b, int r) {
        super(b, r);
    }

    private static double UCB(AnchorCandidate candidate, double delta, int n) {
        return candidate.getPrecision() + deviation(candidate.getSampledSize(), delta, n);
    }

    private static double LCB(AnchorCandidate candidate, double delta, int n) {
        return candidate.getPrecision() - deviation(candidate.getSampledSize(), delta, n);
    }

    private static double deviation(int tau, double delta, int n) {
        double omega = Math.sqrt(delta / (6 * n));
        return deviation(tau, omega);
    }

    private static double deviation(int tau, double omega) {
        final double numerator = 4 * Math.log(log2(2 * tau) / omega);
        return Math.sqrt(numerator / tau);
    }

    private static double log2(int n) {
        return (Math.log(n) / Math.log(2));
    }

    @Override
    public List<AnchorCandidate> identify(final List<AnchorCandidate> candidates,
                                          final SamplingService samplingService,
                                          final double delta, final double epsilon, final int nrOfResults) {

        // The algorithm maintains a set of surviving arms that is initialized as S_1 = [n]
        final Set<AnchorCandidate> survivorSet = new HashSet<>(candidates);
        // Let A be the set of accepted arms
        final Set<AnchorCandidate> acceptedSet = new HashSet<>();
        // Let R be the set of rejected arms
        final Set<AnchorCandidate> rejectedSet = new HashSet<>();


        while (!survivorSet.isEmpty() && acceptedSet.size() < nrOfResults) {
            // At round t, the algorithm calls RoundRobin to choose b arm pulls that keeps the pull count of each arm in
            // the surviving set S_t as uniform as possible
            batchSample(survivorSet, samplingService);

            // Caching the UCB/LCBs to avoid recalculations
            final Map<AnchorCandidate, Double> UCBMap = new HashMap<>();
            final Map<AnchorCandidate, Double> LCBMap = new HashMap<>();
            // Calculate UCB/LCB only once for every element in S_t
            for (AnchorCandidate candidate : survivorSet) {
                UCBMap.put(candidate, UCB(candidate, delta, candidates.size()));
                LCBMap.put(candidate, LCB(candidate, delta, candidates.size()));
            }

            // Then, the algorithm checks if there is any arm that is confidently top-k or confidently not top-k using
            // the LCB and UCB as follows.

            for (AnchorCandidate candidate : survivorSet) {
                // Let k_t = k - |A_t|, the remaining number of top arms to identify
                final int remainingNumberOfResults = nrOfResults - acceptedSet.size();
                final double currentUCB = UCBMap.get(candidate);
                final double currentLCB = LCBMap.get(candidate);
                final long nrGreater = UCBMap.values().stream().filter(ucb -> currentLCB > ucb).count();
                final long nrLesser = LCBMap.values().stream().filter(lcb -> currentUCB < lcb).count();
                // Any arm i whose LCB is greater than the UCB of |S_t| - k_t arms is moved to the accept set
                if (nrGreater > remainingNumberOfResults)
                    acceptedSet.add(candidate);
                    // Symmetrically, any arm i whose UCB is smaller than the LCB of k_t arms is moved to the reject set
                else if (nrLesser >= remainingNumberOfResults)
                    rejectedSet.add(candidate);

                if (acceptedSet.size() == nrOfResults)
                    break;
            }

            survivorSet.removeAll(acceptedSet);
            survivorSet.removeAll(rejectedSet);
        }

        return new ArrayList<>(acceptedSet);
    }
}
