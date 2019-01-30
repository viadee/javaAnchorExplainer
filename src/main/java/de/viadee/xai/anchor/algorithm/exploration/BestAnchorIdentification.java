package de.viadee.xai.anchor.algorithm.exploration;

import java.io.Serializable;
import java.util.List;

import de.viadee.xai.anchor.algorithm.AnchorCandidate;
import de.viadee.xai.anchor.algorithm.execution.SamplingService;

/**
 * Interface defining required methods for identifying the best candidates.
 * <p>
 * Formally, these candidates must satisfy following constraint with high probability:
 * <p>
 * {@code P(prec(A) >= tau) >= 1 - delta}
 * <p>
 * Usually, this is formulated as a pure-exploration Multi-Armed-Bandit problem.
 * <p>
 * Note: the algorithm is not required to only return candidates that achieve the tau level
 * <p>
 * Implementation, such as {@link KL_LUCB} or {@link BatchSAR} may be found in this package, too.
 */
public interface BestAnchorIdentification extends Serializable {

    /**
     * Explores the best candidates by (repeatedly) evaluating them.
     * <p>
     * Equals the execution of the multi-armed-bandit.
     * <p>
     * This returned candidates do not have to be subject to any constraints regarding confidence and/or other params.
     * The anchors algorithm will later enforce all required constraints.
     *
     * @param candidates             the candidates to inspect
     * @param samplingService        an implementation of the {@link SamplingService}, controlling the evaluation of
     *                               samples. Allows for threading.
     * @param explainedInstanceLabel the label that is being explained
     * @param delta                  the probability of identifying the correct result == confidence
     * @param epsilon                the maximum error == tolerance
     * @param nrOfResults            the number of best candidates to return
     * @return the best {@link AnchorCandidate}s out of the specified candidates
     */
    List<AnchorCandidate> identify(final List<AnchorCandidate> candidates,
                                   final SamplingService samplingService,
                                   final int explainedInstanceLabel,
                                   final double delta, final double epsilon, final int nrOfResults);
}
