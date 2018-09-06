package de.goerke.tobias.anchorj.text;

import de.goerke.tobias.anchorj.base.LabeledInstanceList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A list of text data instances.
 * <p>
 * Used to extract all instances out of a document
 */
public class TextDataList extends LabeledInstanceList<TextInstance> {

    private TextDataList(final TextInstance[] dataInstances, final int[] labels) {
        super(dataInstances, labels);
    }

    /**
     * Constructs a {@link TextDataList} from a collection of instances (text file)
     *
     * @param documents a {@link List} of Strings, each representing an instance
     * @param labels    an array containing a label for each instance
     * @return the {@link TextDataList}
     */
    public static TextDataList fromDocuments(List<String> documents, int[] labels) {
        final List<String[]> splitDocuments = documents.stream().map(d -> d.split("\\r?\\n")).collect(Collectors.toList());
        final int instanceCount = splitDocuments.stream().flatMapToInt(s -> IntStream.of(s.length)).sum();
        final TextInstance[] textInstances = new TextInstance[instanceCount];
        final int[] explodedLabels = new int[instanceCount];
        for (int i = 0; i < splitDocuments.size(); i++) {
            final String[] document = splitDocuments.get(i);
            for (int j = 0; j < document.length; j++) {
                int index = j + i * document.length;
                textInstances[index] = new TextInstance(document[j]);
                explodedLabels[index] = labels[i];
            }
        }
        return new TextDataList(textInstances, explodedLabels);
    }
}
