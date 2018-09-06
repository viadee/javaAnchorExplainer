package de.goerke.tobias.anchorj.text;

import de.goerke.tobias.anchorj.base.DataInstance;

import java.util.function.Function;

/**
 * An instance used to described a text piece (e.g. content)
 */
public class TextInstance implements DataInstance<String[]> {
    private static final Function<String, String[]> DEFAULT_TOKENIZER = s -> s.split(" ");
    private final String content;
    private final String[] tokens;

    /**
     * @param content   the content
     * @param tokenizer the tokenizer used to extract all features
     */
    private TextInstance(final String content, final Function<String, String[]> tokenizer) {
        this(content, tokenizer.apply(content));
    }

    /**
     * Extracts the instances out of a string sentence by using a default tokenizer
     *
     * @param content the text instance
     */
    public TextInstance(final String content) {
        this(content, DEFAULT_TOKENIZER);
    }

    /**
     * Creates the instance by specifying all features of it
     *
     * @param tokens the tokens of the instance
     */
    public TextInstance(final String[] tokens) {
        this(String.join(" ", tokens), tokens);
    }

    /**
     * Creates the instance by specifying all features of it
     *
     * @param content the content
     * @param tokens  the tokens of the instance
     */
    public TextInstance(final String content, final String[] tokens) {
        this.content = content;
        this.tokens = tokens;
    }

    @Override
    public String[] getInstance() {
        return tokens;
    }

    /**
     * @return the instance as a joined string
     */
    public String getJoinedInstance() {
        return String.join(" ", tokens);
    }

    @Override
    public int getFeatureCount() {
        return tokens.length;
    }
}
