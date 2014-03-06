/**
 * 
 */
package toolset;

/**
 * @author Jordan
 * 
 */
public class ShingleComparator {

	// Objective: compare takes two sets of shingles and finds their Jaccardian
	// similarity | A.INTERSECT(B) | / | A.UNION(B) |

	// Inputs: An array of strings SORTED ALPHABETICALLY (aa, ab, ac)

	// Outputs: A floating point value between 0 and 1 representing the % of
	// shingles in common

	public static float compare(String[] file1, String[] file2) {
		int a, b, hits, compareVal;
		final int file1Size = file1.length;
		final int file2Size = file2.length;
		a = b = hits = 0;

		while (a < file1Size && b < file2Size) {
			compareVal = file1[a].compareTo(file2[b]);

			if (compareVal > 0) // b < a so move b to catch up
				++b;
			else if (compareVal < 0) // a < b so move a to catch up
				++a;
			else {
				++hits;
				++a;
				++b;
			}
		}

		// subtract hits to account for non-unique elements removed by the UNION
		return ((float) hits) / ((file1Size + file2Size) - hits);

	}
}
