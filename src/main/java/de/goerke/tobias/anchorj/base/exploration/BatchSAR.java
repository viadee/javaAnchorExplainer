package de.goerke.tobias.anchorj.base.exploration;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.execution.AbstractSamplingService;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implementation of the BatchSAR algorithm proposed by Jun et al in
 * "Top Arm Identification in Multi-Armed Bandits with Batch Arm Pulls".
 * <p>
 * See <a href="http://proceedings.mlr.press/v51/jun16.pdf">http://proceedings.mlr.press/v51/jun16.pdf</a>.
 * <p>
 * This algorithm offers native batch pulls that enable efficient parallelization and threading to speed up exploration.
 * Depending on the use-case, this offers significant benefits over the {@link KL_LUCB} algorithm originally proposed
 * by Ribeiro et al.
 * <p>
 * This algorithm is to be used in a fixed budget setting.
 * <p>
 * This algorithm ignores the delta value.
 * If the fixed confidence setting is to be used, {@link BatchRacing} may be used.
 */
public class BatchSAR extends AbstractBRAlgorithm {
    private final int batchBudget;
    private final int nn;

    /**
     * Constructs the algorithm class settings its parameters.
     *
     * @param br          sets batch size and batch restriction equally so that there are no arm pull restrictions
     * @param batchBudget the maximum number of batches to obtain
     */
    public BatchSAR(int br, int batchBudget) {
        this(br, br, batchBudget);
    }

    /**
     * Constructs the algorithm class settings its parameters.
     *
     * @param b           arms are pulled in batches of size b in each round.
     * @param r           any arm can be pulled at most r &lt;= b times in a batch
     * @param batchBudget the maximum number of batches to obtain
     */
    public BatchSAR(int b, int r, int batchBudget) {
        super(b, r);
        if (!ParameterValidation.isUnsigned(batchBudget))
            throw new IllegalArgumentException("Batch budget must not be negative");
        this.batchBudget = batchBudget;
        this.nn = Math.max(ceil(b / (double) r), 2);
    }

    @Override
    public List<AnchorCandidate> identify(final List<AnchorCandidate> candidates,
                                          final AbstractSamplingService samplingService,
                                          final double delta, final int nrOfResults) {
        final int n = candidates.size();
        // We do not have n at instantiation, so this cannot be a class field (immutability / thread safety)
        final int c1 = b + (nn * Math.min(r, ceil(b / (double) 2))) + n;


        final Set<AnchorCandidate> survivingSet = new HashSet<>(candidates);
        final Set<AnchorCandidate> acceptedSet = new HashSet<>();
        for (int s = 1; s <= (n - nn + 1); s++) {
            final int ms = calculateM(n, c1, s);
            // Pull every arm until it has been pulled at least ms times
            // while (survivingSet.stream().min(Comparator.comparingInt(AnchorCandidate::getSampledSize)).orElseThrow()
            //         .getSampledSize() < ms) {
            batchSample(survivingSet, samplingService, ms);
            // }

            final int remainingK = nrOfResults - acceptedSet.size();

            // Choose an arm that is safest to remove. Do this by getting the candidate with the highest empirical gap
            // This arm is empirically either the best or worst arm
            if (s <= (n - nn)) {
                final List<AnchorCandidate> sortedByMean = survivingSet.stream()
                        .sorted(Comparator.comparingDouble(AnchorCandidate::getPrecision).reversed())
                        .collect(Collectors.toList());
                final double deltaP1 = sortedByMean.get(0).getPrecision() - sortedByMean.get(remainingK).getPrecision();
                final double deltaP2 = sortedByMean.get(remainingK - 1).getPrecision() -
                        sortedByMean.get(n - s).getPrecision();
                // Argmax
                final AnchorCandidate removedArm = (deltaP1 >= deltaP2) ? sortedByMean.get(0) : sortedByMean.get(n - s);
                survivingSet.remove(removedArm);
                // If the arm was the best one, add it to accepted
                if (removedArm == sortedByMean.get(0))
                    acceptedSet.add(removedArm);
                // Early exit case 1: the surviving set is of the same size as we are looking for
                if (survivingSet.size() == (nrOfResults - acceptedSet.size())) {
                    acceptedSet.addAll(survivingSet);
                    break;
                }
                // Early exit case 2: acceptedSet contains right amount of candidates
                if (acceptedSet.size() == nrOfResults) {
                    break;
                }
            }
            // Final stage: s = (n - nn + 1)
            // Choose empirical top-k' arms and add them to final accepted set
            else {
                final List<AnchorCandidate> addedCandidates = survivingSet.stream()
                        .sorted(Comparator.comparingDouble(AnchorCandidate::getPrecision).reversed()).limit(remainingK)
                        .collect(Collectors.toList());
                acceptedSet.addAll(addedCandidates);
            }
        }

        return new ArrayList<>(acceptedSet);
    }

    private int calculateM(int n, int c1, int s) {
        // Base term
        final double baseNominator = (b * batchBudget) - IntStream.rangeClosed(nn + 1, n)
                .map(i -> ceil(b / (double) i)).sum() - c1;
        final double baseDenominator = (nn / (double) 2) + IntStream.rangeClosed(nn + 1, n)
                .mapToDouble(i -> 1 / (double) i).sum();

        final double base = baseNominator / baseDenominator;

        final double multiplierDenominator = (s <= (n - nn)) ? (n - s + 1) : 2;
        final double multiplier = 1 / multiplierDenominator;

        return ceil(base * multiplier);
    }

    private static int ceil(double a) {
        return (int) Math.ceil(a);
    }
}
