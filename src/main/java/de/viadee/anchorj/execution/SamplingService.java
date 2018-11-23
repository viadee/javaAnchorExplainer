package de.viadee.anchorj.execution;

import de.viadee.anchorj.execution.sampling.SamplingFunction;

import java.io.Serializable;

/**
 * Interface defining all methods a sampling service is obligated to provide.
 */
public interface SamplingService extends Serializable {


    /**
     * Creates a session that has to be used in order to obtain samples.
     *
     * @param explainedInstanceLabel the explained instance label
     * @return an {@link SamplingSession} instance.
     */
    SamplingSession createSession(final int explainedInstanceLabel);

    /**
     * Sets a new samplingFunction.
     * <p>
     * Only required to implement when using the SP-algorithm
     *
     * @param samplingFunction the samplingFunction
     * @return a new SamplingService instance
     */
    SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction);


    /**
     * Returns the time spent taking samples
     * <p>
     * Serves only informative purposes, no correct implementation is required
     *
     * @return the time spent taking samples in milliseconds
     */
    double getTimeSpentSampling();

    /**
     * Returns the amount of samples taken by the service
     * <p>
     * Serves only informative purposes, no correct implementation is required
     *
     * @return the amount of samples taken
     */
    int getSamplesTakenCount();
}
