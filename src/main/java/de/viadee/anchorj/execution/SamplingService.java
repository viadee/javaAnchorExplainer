package de.viadee.anchorj.execution;

import java.io.Serializable;

/**
 * Interface defining all methods a sampling service is obligated to provide.
 */
public interface SamplingService extends Serializable {

    /**
     * Returns the time spent taking samples
     *
     * @return the time spent taking samples in milliseconds
     */
    double getTimeSpentSampling();

    // TODO make methods default and add total samples taken

    /**
     * Creates a session that has to be used in order to obtain samples.
     *
     * @param explainedInstanceLabel the explained instance label
     * @return an {@link SamplingSession} instance.
     */
    SamplingSession createSession(final int explainedInstanceLabel);
}
