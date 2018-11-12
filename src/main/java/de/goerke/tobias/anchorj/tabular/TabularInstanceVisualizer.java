package de.goerke.tobias.anchorj.tabular;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import de.goerke.tobias.anchorj.base.AnchorResult;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * May be used to visualize an instance of the algorithms result for the user.
 */
public class TabularInstanceVisualizer {
    private final Map<TabularFeature, Map<Integer, Object>> invertedMappings;

    /**
     * Constructs the instance.
     *
     * @param mappings the mappings used for transforming values
     */
    public TabularInstanceVisualizer(Map<TabularFeature, Map<Object, Integer>> mappings) {
        this.invertedMappings = new LinkedHashMap<>();
        for (Map.Entry<TabularFeature, Map<Object, Integer>> entry : mappings.entrySet()) {
            this.invertedMappings.put(entry.getKey(), invertMap(entry.getValue()));
        }
    }

    private static AnchorCandidate getCandidateForFeatureNr(AnchorResult<?> result, Integer featureNr) {
        AnchorCandidate current = result;
        while (current != null) {
            if (current.getAddedFeature().equals(featureNr))
                return current;
            current = current.getParentCandidate();
        }
        throw new RuntimeException("Illegal result hierarchy");
    }

    private static <K, V> Map<V, K> invertMap(Map<K, V> toInvert) {
        final Map<V, K> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : toInvert.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    /**
     * Formats the result readable to the user.
     *
     * @param anchorResult the algorithm's result
     * @return a String visualizing the result
     */
    public String visualizeResult(AnchorResult<TabularInstance> anchorResult) {
        final String[] text = instanceToText(anchorResult.getInstance());
        final String[] explanation = new String[anchorResult.getOrderedFeatures().size()];
        int index = 0;
        final DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        for (Integer featureNr : anchorResult.getOrderedFeatures()) {
            AnchorCandidate candidate = getCandidateForFeatureNr(anchorResult, featureNr);
            explanation[index++] = text[featureNr] + " [" + df.format(candidate.getAddedPrecision()) + ","
                    + df.format(candidate.getAddedCoverage()) + "]";
        }
        return "IF ( " + String.join(" AND " + System.lineSeparator(), explanation) + ")"
                + System.lineSeparator() + "THEN PREDICT " +
                getLabelMapping().getOrDefault(anchorResult.getLabel(), anchorResult.getLabel());
    }

    public String[] getAnchorAsPredicateList(AnchorResult<TabularInstance> anchorResult) {
        final String[] text = instanceToText(anchorResult.getInstance());
        final String[] explanation = new String[anchorResult.getOrderedFeatures().size()];
        int index = 0;

        for (Integer featureNr : anchorResult.getOrderedFeatures()) {
            explanation[index++] = text[featureNr];
        }

        return explanation;
    }

    /**
     * Formats an instance readable to the user.
     *
     * @param explainedInstance      the explained instance
     * @param explainedInstanceLabel the explained instance's label
     * @return a String visualizing the result
     */
    public String visualizeInstance(TabularInstance explainedInstance, int explainedInstanceLabel) {
        return String.join(" AND ", instanceToText(explainedInstance)) +
                " with label " + getLabelMapping().get(explainedInstanceLabel);
    }

    private String[] instanceToText(TabularInstance explainedInstance) {
        final List<String> result = new ArrayList<>();
        int i = 0;
        for (Map.Entry<TabularFeature, Map<Integer, Object>> entry : invertedMappings.entrySet()) {
            if (i >= explainedInstance.getFeatureCount())
                break;
            Object value = entry.getValue().get(explainedInstance.getInstance()[i]);
            String tmp = (value instanceof String) ? (String) value : String.valueOf(value);
            if (value == null)
                tmp = String.valueOf(explainedInstance.getInstance()[i]);
            result.add(entry.getKey().getName() + " = " + tmp);
            i++;
        }
        return result.toArray(new String[0]);
    }

    private Map<Integer, Object> getLabelMapping() {
        return invertedMappings.entrySet().stream().filter(e -> e.getKey().isTargetFeature()).map(Map.Entry::getValue)
                .findFirst().orElse(Collections.emptyMap());
    }
}
