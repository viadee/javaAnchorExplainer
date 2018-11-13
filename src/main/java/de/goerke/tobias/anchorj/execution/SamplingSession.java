package de.goerke.tobias.anchorj.execution;

import de.goerke.tobias.anchorj.AnchorCandidate;

import java.util.Map;

/**
 * Interface defining a session which can be run to sample all registered candidates
 */
public interface SamplingSession {

    /**
     * Registers a candidate to be evaluated.
     * <p>
     * If the candidate is already contained, the number of new executions will be added.
     *
     * @param candidate the candidate to evaluate
     * @param count     the number of samples to obtain
     * @return this session object which can be executed
     */
    SamplingSession registerCandidateEvaluation(AnchorCandidate candidate, int count);


    /**
     * Delegates a map of sample executions to
     * {@link SamplingSession#registerCandidateEvaluation(AnchorCandidate, int)}.
     *
     * @param map the mapping of candidates to execution counts
     * @return this session object which can be executed
     */
    default SamplingSession registerCandidateEvaluation(Map<AnchorCandidate, Integer> map) {
        for (Map.Entry<AnchorCandidate, Integer> entry : map.entrySet())
            registerCandidateEvaluation(entry.getKey(), entry.getValue());

        return this;
    }

    /**
     * Obtains the request samples
     */
    void run();
}
