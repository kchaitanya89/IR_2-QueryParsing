/**
 *
 */
package edu.buffalo.cse.irf14.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.buffalo.cse.irf14.util.Constants;

/**
 * @author nikhillo Class that emulates reading data back from a written index
 */
public class IndexReader {
	// Map<String, Posting> inMemIndex = new TreeMap<String, Posting>();
	Index index;
	IndexType type;

	/**
	 * Default constructor
	 * 
	 * @param indexDir
	 *            : The root directory from which the index is to be read. This
	 *            will be exactly the same directory as passed on IndexWriter.
	 *            In case you make subdirectories etc., you will have to handle
	 *            it accordingly.
	 * @param type
	 *            The {@link IndexType} to read from
	 */
	public IndexReader(String indexDir, IndexType type) {
		String indexFolderPath = null;
		this.type = type;
		if (IndexType.TERM == type || type == null) {
			indexFolderPath = indexDir + File.separatorChar
					+ Constants.termIndexFolderName;
		}
		if (IndexType.AUTHOR == type) {
			indexFolderPath = indexDir + File.separatorChar
					+ Constants.authorIndexFolderName;
		}
		if (IndexType.CATEGORY == type) {
			indexFolderPath = indexDir + File.separatorChar
					+ Constants.categoryIndexFolderName;
		}
		if (IndexType.PLACE == type) {
			indexFolderPath = indexDir + File.separatorChar
					+ Constants.placeIndexFolderName;
		}
		index = readIndex(indexFolderPath);
	}

	public Index getIndex() {
		return index;
	}

	/**
	 * @param string
	 * @return
	 */
	private Index readIndex(String indexFolder) {
		Index index = new Index();
		boolean isTermIndex = type == IndexType.TERM;
		for (File file : new File(indexFolder).listFiles()) {
			Properties properties = new Properties();
			FileInputStream fileInputStream;
			try {
				fileInputStream = new FileInputStream(file);
				properties.load(fileInputStream);
				fileInputStream.close();

				for (Object term : properties.keySet()) {
					int j = 0;
					String value = (String) properties.get(term);
					String[] split = value.split("@");
					String[] documentIDs = split[j++].split("\\|");
					String[] frequencies = split[j++].split("\\|");
					String[] titleFlags = new String[0];
					if (isTermIndex) {
						titleFlags = split[j++].split("\\|");
					}
					Integer totalTermFreq = Integer.parseInt(split[j++]);
					Integer totalDocumentFreq = Integer.parseInt(split[j++]);
					Map<String, Posting.Entry> individualPostingsMap = new TreeMap<String, Posting.Entry>();
					if (isTermIndex) {
						for (int i = 0; i < documentIDs.length; i++) {
							Posting.Entry pentry = new Posting.Entry();
							pentry.setTf(Integer.parseInt(frequencies[i]));
							pentry.setTitle(titleFlags[i].equals("1"));
							individualPostingsMap.put(documentIDs[i],pentry);
						}
					} else {
						for (int i = 0; i < documentIDs.length; i++) {
							Posting.Entry pentry = new Posting.Entry();
							pentry.setTf(Integer.parseInt(frequencies[i]));
							individualPostingsMap.put(documentIDs[i],pentry);
						}
					}

					Posting posting = new Posting();
					posting.setIndividualPostingsMap(individualPostingsMap);
					posting.setTotalTermFreq(totalTermFreq);
					posting.setTotalDocumentFreq(totalDocumentFreq);

					index.put(term.toString(), posting);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return index;
	}

	/**
	 * Get total number of terms from the "key" dictionary associated with this
	 * index. A postings list is always created against the "key" dictionary
	 * 
	 * @return The total number of terms
	 */
	public int getTotalKeyTerms() {
		return index != null ? index.size() : -1;
	}

	/**
	 * Get total number of terms from the "value" dictionary associated with
	 * this index. A postings list is always created with the "value" dictionary
	 * 
	 * @return The total number of terms
	 */
	public int getTotalValueTerms() {
		return index != null ? index.valueSize() : -1;
	}

	/**
	 * Method to get the postings for a given term. You can assume that the raw
	 * string that is used to query would be passed through the same Analyzer as
	 * the original field would have been.
	 * 
	 * @param term
	 *            : The "analyzed" term to get postings for
	 * @return A Map containing the corresponding fileid as the key and the
	 *         number of occurrences as values if the given term was found, null
	 *         otherwise.
	 */
	public Map<String, Integer> getPostings(String term) {
		Posting posting = index.get(term);
		Map<String, Posting.Entry> individualPostingsMap = null;
		Map<String, Integer> result = null;
		if (posting != null) {
			individualPostingsMap = posting.getIndividualPostingsMap();
			result = new HashMap<String, Integer>();
			for (Map.Entry<String, Posting.Entry> pentry : individualPostingsMap
					.entrySet()) {
				result.put(pentry.getKey(), pentry.getValue().getTf());
			}
		}
		return result;
	}

	/**
	 * Method to get the top k terms from the index in terms of the total number
	 * of occurrences.
	 * 
	 * @param k
	 *            : The number of terms to fetch
	 * @return : An ordered list of results. Must be <=k fr valid k values null
	 *         for invalid k values
	 */
	public List<String> getTopK(int k) {
		SortedSet<Map.Entry<String, Posting>> sortedset = new TreeSet<Map.Entry<String, Posting>>(
				new Comparator<Map.Entry<String, Posting>>() {
					@Override
					public int compare(Map.Entry<String, Posting> e1,
							Map.Entry<String, Posting> e2) {
						return e1.getValue().compareTo(e2.getValue());
					}
				});

		for (Field field : Index.class.getDeclaredFields()) {
			try {
				Map<String, Posting> alphabetIndexMap = (TreeMap<String, Posting>) field
						.get(index);
				sortedset.addAll(alphabetIndexMap.entrySet());
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Iterator<Entry<String, Posting>> iterator = sortedset.iterator();
		int counter = 1;
		List<String> arrayList = new ArrayList<String>();
		while (iterator.hasNext() && counter <= k) {
			Map.Entry<String, Posting> entry = iterator.next();
			arrayList.add(entry.getKey());
			counter++;
		}

		return arrayList.isEmpty() ? null : arrayList;
	}

	/**
	 * Method to implement a simple boolean AND query on the given index
	 * 
	 * @param terms
	 *            The ordered set of terms to AND, similar to getPostings() the
	 *            terms would be passed through the necessary Analyzer.
	 * @return A Map (if all terms are found) containing FileId as the key and
	 *         number of occurrences as the value, the number of occurrences
	 *         would be the sum of occurrences for each participating term.
	 *         return null if the given term list returns no results BONUS ONLY
	 */
	public Map<String, Integer> query(String... terms) {
		Map<String, Posting.Entry> resultMap = null;
		Map<String, Integer> result = null;
		List<Map<String, Posting.Entry>> mapList = new ArrayList<Map<String, Posting.Entry>>();
		for (String term : terms) {
			Posting posting = index.get(term);
			Map<String, Posting.Entry> individualPostingsMap = posting
					.getIndividualPostingsMap();
			mapList.add(individualPostingsMap);
		}

		if (!mapList.isEmpty()) {
			resultMap = new TreeMap<String, Posting.Entry>(mapList.get(0));
			result = new TreeMap<String, Integer>();
			// Intersection of docIDs
			for (Map<String, Posting.Entry> map : mapList) {
				resultMap.keySet().retainAll(map.keySet());
			}

			int freqSum = 0;
			// Update Frequencies
			for (String docID : resultMap.keySet()) {
				for (Map<String, Posting.Entry> map : mapList) {
					freqSum += map.get(docID).getTf();
				}
				result.put(docID, freqSum);
				freqSum = 0;
			}
		}
		return result.isEmpty() ? null : result;
	}
}