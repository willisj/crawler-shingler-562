package toolset;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

class StringComparator implements Comparator<String> {
	@Override
	public int compare(String s1, String s2) {
		return s1.compareTo(s2);
	}
}

public class Shingler {

	static public String[] shingle(String textToShingle, int ShingleLength) {

		boolean recordingShingle = false;
		int segmentsSoFar = 0;
		String currentShingle = "";
		String[] segments, returnArray;
		HashSet<String> generatedShingles = new HashSet<String>();

		// sorted lowercase alpha
		final String[] stopWords = new String[] { "a", "a's", "able", "about",
				"above", "according", "accordingly", "across", "actually",
				"after", "afterwards", "again", "against", "ain't", "al",
				"all", "allow", "allows", "almost", "alone", "along",
				"already", "also", "although", "always", "am", "among",
				"amongst", "amp", "an", "and", "another", "any", "anybody",
				"anyhow", "anyone", "anything", "anyway", "anyways",
				"anywhere", "are", "aren't", "around", "as", "aside", "at",
				"awfully", "be", "because", "been", "before", "behind",
				"being", "below", "beside", "besides", "between", "br", "but",
				"by", "c'mon", "c's", "came", "can", "can't", "cannot", "cant",
				"cause", "causes", "certainly", "clearly", "co", "com", "come",
				"comes", "could", "couldn't", "course", "currently", "de",
				"despite", "did", "didn't", "different", "do", "does",
				"doesn't", "doing", "don't", "done", "down", "during", "each",
				"ed", "edu", "eg", "either", "el", "else", "elsewhere", "er",
				"es", "et", "etc", "even", "ever", "few", "font", "for",
				"from", "further", "furthermore", "get", "gets", "getting",
				"given", "gives", "go", "goes", "going", "gone", "got",
				"gotten", "had", "hadn't", "happens", "has", "hasn't", "have",
				"haven't", "having", "he", "he's", "hence", "her", "here",
				"here's", "hereafter", "hereby", "herein", "hereupon", "hers",
				"herself", "hi", "him", "himself", "his", "hither",
				"hopefully", "how", "howbeit", "however", "href", "html",
				"i'd", "i'll", "i'm", "i've", "ie", "if", "in", "inasmuch",
				"indeed", "indicate", "indicated", "indicates", "insofar",
				"instead", "into", "inward", "is", "isn't", "it", "it'd",
				"it'll", "it's", "its", "itself", "just", "last", "lately",
				"later", "latter", "latterly", "least", "less", "lest", "let",
				"let's", "li", "like", "liked", "likely", "mainly", "many",
				"may", "maybe", "me", "mean", "merely", "might", "more",
				"moreover", "most", "mostly", "much", "must", "my", "myself",
				"namely", "nbsp", "near", "nearly", "necessary", "need",
				"needs", "neither", "never", "nevertheless", "new", "next",
				"nine", "no", "non", "none", "nor", "not", "novel", "now",
				"of", "off", "often", "oh", "old", "on", "once", "one", "ones",
				"only", "onto", "or", "other", "otherwise", "ought", "our",
				"ours", "ourselves", "out", "over", "overall", "own", "per",
				"quot", "self", "selves", "several", "shall", "she", "should",
				"shouldn't", "since", "so", "some", "soon", "still", "such",
				"t's", "th", "than", "that", "that", "that's", "thats", "the",
				"their", "theirs", "them", "themselves", "then", "thence",
				"there", "there's", "thereafter", "thereby", "therefore",
				"therein", "theres", "thereupon", "these", "they", "they'd",
				"they'll", "they're", "they've", "this", "those", "though",
				"thus", "to", "together", "too", "under", "unless", "unlikely",
				"until", "unto", "up", "upon", "us", "very", "vs", "want",
				"wants", "was", "wasn't", "way", "we", "we'd", "we'll",
				"we're", "we've", "went", "were", "weren't", "what", "what's",
				"whatever", "when", "whence", "whenever", "where", "where's",
				"whereafter", "whereas", "whereby", "wherein", "whereupon",
				"wherever", "whether", "which", "while", "who", "who's",
				"whoever", "whom", "whose", "why", "will", "with", "within",
				"without", "won't", "would", "would", "wouldn't", "yet", "you",
				"you'd", "you'll", "you're", "you've", "your", "yours",
				"yourself", "yourselves" };

		segments = textToShingle.split("\\s");

		// iterate over all space delimited tokens
		for (String s : segments) {

			// if this token is a stop-word, start recording
			if (!recordingShingle && binarySearch(stopWords, s))
				recordingShingle = true;

			// if we're recording a shingle, add the current token
			if (recordingShingle) {
				currentShingle += s;

				// check to see that this new token hasn't put us over the
				// shingle length
				if (++segmentsSoFar >= ShingleLength) {

					// it has, record the total shingle and reset
					generatedShingles.add(currentShingle.toLowerCase());
					segmentsSoFar = 0;
					recordingShingle = false;
					currentShingle = "";
				}
			}
		}
		if (generatedShingles.size() > 0) {

			returnArray = generatedShingles.toArray(new String[generatedShingles.size()]);
			Arrays.sort(returnArray, new StringComparator());
			return returnArray;
		} else
			return null;

	}

	static boolean binarySearch(final String[] haystack, String needle) {
		int high = haystack.length - 1;
		int low = 0;
		int mid = (high + low) / 2;
		int compareResult;

		needle = needle.toLowerCase();

		if (haystack[0].equals(needle) || haystack[high].equals(needle))
			return true;

		while (high > low) {
			compareResult = haystack[mid].compareTo(needle);

			if (compareResult == 0)
				return true;
			else if (compareResult > 0)
				high = mid - 1;
			else
				low = mid + 1;

			mid = (high + low) / 2;
		}

		return false;
	}

}
