package de.goerke.tobias.anchorj.base;


/**
 * Exception signalling that no anchor could be constructed.
 * Other than {@link NoCandidateFoundException}, a candidate with a precision greater than zero was found, it just did
 * not match the threshold.
 * <p>
 * The best candidate is attached to this exception.
 *
 */
public class NoAnchorFoundException extends Exception {
    private static final String MESSAGE = "No anchor found matching the specified parameters. " +
            "However, candidates with a precision greater than zero were found. " +
            "This object is attached to this exception";

    private final AnchorResult bestCandidate;

    /**
     * Instantiates the exception
     *
     * @param bestCandidate the best result found instead
     */
    NoAnchorFoundException(AnchorResult bestCandidate) {
        super(MESSAGE);
        this.bestCandidate = bestCandidate;
    }

    /**
     * Returns the alternative with the best precision instead of the not desired but not found Anchor.
     *
     * @param <T> The expected return type, as throwables must not be generic
     * @return the best candidate
     */
    @SuppressWarnings("unchecked")
    public <T extends DataInstance<?>> AnchorResult<T> getBestCandidate() {
        return bestCandidate;
    }
}
