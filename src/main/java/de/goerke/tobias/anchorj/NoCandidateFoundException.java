package de.goerke.tobias.anchorj;

/**
 * Exception signalling that no anchor could be constructed as no single candidate evaluated had a precision
 * greater than zero.
 * <p>
 * Due to the algorithm's nature, this must have happened in the first round of the beam search as no feature had a
 * correct prediction. Either try to increase the evaluations for each instance or change the feature
 * (e.g. for images increase the size of superpixels)
 * <p>
 * Based on this, no further rounds could be conducted.
 */
public class NoCandidateFoundException extends RuntimeException {
    private static final String MESSAGE = "No candidate found having a precision greater than 0";

    /**
     * Creates the instance.
     */
    NoCandidateFoundException() {
        super(MESSAGE);
    }
}
