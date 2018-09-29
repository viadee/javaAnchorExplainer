package de.goerke.tobias.anchorj.base.exploration;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.execution.AbstractSamplingService;
import de.goerke.tobias.anchorj.util.KLBernoulliUtils;
import de.goerke.tobias.anchorj.util.MathUtils;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of the KL LUCB algorithm by Kaufmann and Kalyanakrishnan in their publication
 * "Information Complexity in Bandit Subset Selection"
 * <p>
 * See <a href="http://proceedings.mlr.press/v30/Kaufmann13.pdf">http://proceedings.mlr.press/v30/Kaufmann13.pdf</a>
 */
public class KL_LUCB implements BestAnchorIdentification {
    private final int batchSize;

    /**
     * Sets the algorithm's parameters
     * <p>
     * If A* is the arm with the highest true precision, the following holds for the true precision of the chosen rule A
     * <p>
     * {@code P(prec(A) >= prec(A*) - epsilon) >= 1 - delta}
     *
     * @param batchSize the amount of evaluations to obtain each round
     */
    public KL_LUCB(int batchSize) {
        if (!ParameterValidation.isUnsigned(batchSize))
            throw new IllegalArgumentException("Batch size must not be negative");

        this.batchSize = batchSize;
    }

    /*
     * Applying this KL-LUCB algorithm optimized regret in pure-exploration bandit-problems.
     * <p>
     * However, is a greedy algorithm, thus is best extended by a BeamSearch.
     *
     * <p>
     * See paper <b>Information Complexity in Bandit Subset Selection</b>.
     * <p>
     * LUCB algorithm outline (Kaufmann & Kalyanakrishnan):
     * <pre>
     *      Require: tau > 0 (tolerance level), U, L (confidence bounds)
     *      t = 1 (number of stage of the algorithm), B(1) = infinity (stopping index)
     *      for a=1..K do
     *          Sample arm a, compute confidence bounds U_a(1), L_a(1)
     *      end for
     *      while B(t) > tau do
     *          Draw an arm u_t and l_t. t = t + 1
     *          Update confidence bounds, set J(t) and arms u_t, l_t
     *          B(t) = U_ut(t) - L_l_t(t)
     *      end while
     *      return J(t)
     *  </pre>
     * <p>
     * Outline by Ribeiro:
     * <pre>
     * function BestCand(A, D, e, δ)
     *      initialize prec, prec_ub , prec_lb estimates ∀A ∈ A
     *      A ← arg max_A prec(A)
     *      A' ← arg max_{A' != A} prec_ub(A', δ) {δ implicit below}
     *      while prec_ub (A') − prec_lb(A) > e do
     *          sample z ∼ D(z|A), z' ∼ D(z'|A') {Sample more}
     *          update prec, prec_ub, prec_lb for A and A'
     *          A ← arg max_A prec(A)
     *          A' ← arg max_{A' != A} prec_ub (A')
     *      return A
     * </pre>
     *
     */
    @Override
    public List<AnchorCandidate> identify(final List<AnchorCandidate> candidates,
                                          final AbstractSamplingService samplingService,
                                          final double delta, final double epsilon, final int nrOfResults) {
        final double[] ub = new double[candidates.size()];
        final double[] lb = new double[candidates.size()];

        int t = 1;

        int[] bounds = updateBounds(t, candidates, delta, nrOfResults, ub, lb);
        int ut = bounds[0];
        int lt = bounds[1];
        double b = ub[ut] - lb[lt];

        while (b > epsilon) {
            samplingService.createSession()
                    .registerCandidateEvaluation(candidates.get(ut), batchSize)
                    .registerCandidateEvaluation(candidates.get(lt), batchSize)
                    .run();
            t++;
            bounds = updateBounds(t, candidates, delta, nrOfResults, ub, lb);
            ut = bounds[0];
            lt = bounds[1];
            b = ub[ut] - lb[lt];
        }
        final double[] means = getMultipleMeans(candidates);
        final int[] sortedMeans = MathUtils.argSort(means);
        final int[] topCandidateIndices = Arrays.copyOfRange(sortedMeans, means.length - nrOfResults, means.length);
        return Arrays.stream(topCandidateIndices).mapToObj(candidates::get).collect(Collectors.toList());
    }

    /**
     * Part of the KL-LUCB algorithm updating the bounds
     *
     * @param t          iteration number
     * @param candidates current candidates
     * @param delta      delta value
     * @param topN       number of candidates to choose
     * @param ub         current upper bounds
     * @param lb         current lower bounds
     * @return an array with the new upper and lower bounds
     */
    static int[] updateBounds(int t, final List<AnchorCandidate> candidates, final double delta, final int topN,
                              final double[] ub, final double[] lb) {
        final double[] means = getMultipleMeans(candidates);
        final int[] sortedMeans = MathUtils.argSort(means);
        final double beta = KLBernoulliUtils.computeBeta(candidates.size(), t, delta);
        final int[] j = Arrays.copyOfRange(sortedMeans, means.length - topN, means.length);
        final int[] not_j = Arrays.copyOfRange(sortedMeans, 0, means.length - topN);
        // FIXME dupBernoulli introduces MASSIVE overhead, up to 2/3 of TOTAL Anchors runtime
        for (int f : not_j) {
            ub[f] = KLBernoulliUtils.dupBernoulli(means[f], beta / candidates.get(f).getSampledSize());
        }
        for (int f : j) {
            lb[f] = KLBernoulliUtils.dlowBernoulli(means[f], beta / candidates.get(f).getSampledSize());
        }

        final int ut = (not_j.length == 0) ? 0 : not_j[MathUtils.argMax(IntStream.of(not_j)
                .mapToDouble(localnotj -> ub[localnotj]).toArray())];
        final int lt = j[MathUtils.argMin(IntStream.of(j).mapToDouble(localj -> lb[localj]).toArray())];

        return new int[]{ut, lt};
    }

    private static double[] getMultipleMeans(final List<AnchorCandidate> anchorCandidates) {
        final double[] means = new double[anchorCandidates.size()];
        for (int i = 0; i < means.length; i++)
            means[i] = anchorCandidates.get(i).getPrecision();
        return means;
    }
}
