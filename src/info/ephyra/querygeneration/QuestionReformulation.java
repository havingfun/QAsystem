package info.ephyra.querygeneration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A data structure consisting of an expression describing a query
 * reformulation and a score that is used by the answer selection module to rank
 * results that follow from that reformulation.</p>
 * 
 * <p>Before the <code>get()</code> method of this class can be called, a
 * <code>Matcher</code> needs to be created that applies the corresponding
 * question pattern to the question and its <code>matches()</code> method needs
 * to be called. This <code>Matcher</code> must be passed to the
 * <code>get()</code> method.
 * 
 * @author Nico Schlaefer
 * @version 2005-09-19
 */
public class QuestionReformulation {
	/**
	 * <p>Expression that describes the reformulation.</p>
	 * 
	 * <p>The expression can contain:<br>
	 * <ul>
	 * <li>Group identifiers that are replaced by groups captured by the
	 * <code>Matcher</code> that applied the corresponding question pattern to
	 * the question string.<br>
	 * Format: <code>[group_id]</code></li>
	 * <li>Combined group identifiers, that are resolved by placing the second
	 * group in between each two words in the first group. One reformulation is
	 * created for each possible position of the second group.<br>
	 * Format: <code>[group_id1]<[group_id2]</code></li>
	 * <li>Other arbitrary strings that are not replaced.</li>
	 * </ul></p>
	 */
	private String expr;
	/**
	 * Score used by the answer selection module to rank results. A more
	 * specific reformulation should be assigned a higher score.
	 */
	private float score;
	
	/**
	 * Creates a <code>QuestionReformulation</code> object and sets the
	 * expression and the score.
	 * 
	 * @param expr expression describing the reformulation
	 * @param score score of the reformulation
	 */
	public QuestionReformulation(String expr, float score) {
		this.expr = expr;
		this.score = score;
	}
	
	/**
	 * Replaces group identifiers of the format <code>[group_no]</code> by the
	 * corresponding group captured by a <code>Matcher</code> that applied the
	 * question pattern to the question string.
	 * 
	 * @param queryString query string containing group identifiers
	 * @param matcher the <code>Matcher</code> that group IDs refer to
	 * @return query string without group identifiers
	 */
	private String evalGroups(String queryString, Matcher matcher) {
		String result = queryString;
		
		Pattern p = Pattern.compile("\\[(\\d*)\\]");
		Matcher m = p.matcher(result);
		
		// replace all group IDs by the corresponding parts of the question
		while (m.find()) {
			int group = Integer.parseInt(m.group(1));
			
			result = result.replace(m.group(), matcher.group(group));
		}
		
		return result;
	}
	
	/**
	 * Replaces combined group identifiers of the format
	 * <code>[group_no1]&lt;[group_no2]</code> by the combinations of the
	 * corresponding groups generated by the <code>combineStrings()<code>
	 * method.
	 * 
	 * @param queryString query string containing combined group identifiers
	 * @param matcher the <code>Matcher</code> that groupd IDs refer to
	 * @return query string without combined group identifiers
	 */
	private String[] evalCombinedGroups(String queryString, Matcher matcher) {
		String[] queryStrings;
		
		Pattern p = Pattern.compile("\\[(\\d*)\\]<\\[(\\d*)\\]");
		Matcher m = p.matcher(queryString);
		
		if (m.find()) {
			// resolve the combined group IDs
			int group1 = Integer.parseInt(m.group(1));
			int group2 = Integer.parseInt(m.group(2));
			String[] combined = combineStrings(matcher.group(group1),
											   matcher.group(group2));
			
			queryStrings = new String[combined.length];
			for (int i = 0; i < combined.length; i++)
				queryStrings[i] = queryString.replace(m.group(), combined[i]);
		} else {
			// create an array of only one string
			queryStrings = new String[1];
			queryStrings[0] = queryString;
		}
		
		return queryStrings;
	}
	
	/**
	 * Combines two strings by inserting the second string in between each two
	 * tokens of the first string. If the first string consist of <code>n</code>
	 * tokens, then this method returns an array of <code>n-1</code> strings.
	 * 
	 * @param s1 first string
	 * @param s2 second string
	 * @return combination of the two strings
	 */
	private String[] combineStrings(String s1, String s2) {
		String[] tokens = s1.split(" ");
		String[] combined = new String[tokens.length - 1];
		
		for (int i = 0; i < combined.length; i++) {
			combined[i] = "";
			
			for (int j = 0; j < combined.length; j++) {
				combined[i] += tokens[j] + " ";
				if (i == j) combined[i] += s2 + " ";
			}
			
			combined[i] += tokens[tokens.length - 1];
		}
		
		return combined;
	}
	
	/**
	 * Returns one or more reformulations of the original question. Requires a
	 * <code>Matcher</code> that applied the corresponding question pattern to
	 * the question string. The method <code>matches()</code> must have been
	 * executed since group identifiers that occur in the expression used
	 * by the reformulator refer to groups that have been captured.
	 * 
	 * @param matcher the <code>Matcher</code> that group IDs refer to
	 * @return reformulations of the original question
	 */
	public String[] get(Matcher matcher) {
		// evaluate combined groups
		String[] queryStrings = evalCombinedGroups(expr, matcher);
		
		for (int i = 0; i < queryStrings.length; i++) {
			// evaluate remaining groups
			queryStrings[i] = evalGroups(queryStrings[i], matcher);
			// add quotation marks
			queryStrings[i] = "\"" + queryStrings[i] + "\"";
		}
		
		return queryStrings;
	}
	
	/**
	 * Returns the score of the reformulation, used by the answer selection
	 * module to rank results that follow from this reformulation.
	 * 
	 * @return score of the reformulation
	 */
	public float getScore() {
		return score;
	}
}
