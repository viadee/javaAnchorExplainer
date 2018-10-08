package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.AnchorCandidate;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Implementation of the {@link AbstractSamplingService} sequentially obtaining all samples.
 */
public class LinearSamplingService extends AbstractSamplingService {

    LinearSamplingService(final BiFunction<AnchorCandidate, Integer, Double> sampleFunction) {
        super(sampleFunction);
    }

    @Override
    public AbstractSamplingSession createSession() {
        return new LinearSession();
    }

    private class LinearSession extends AbstractSamplingSession {
        @Override
        public void execute() {
            for (Map.Entry<AnchorCandidate, Integer> entry : samplingCountMap.entrySet())
                sampleFunction.apply(entry.getKey(), entry.getValue());
        }
    }
}
