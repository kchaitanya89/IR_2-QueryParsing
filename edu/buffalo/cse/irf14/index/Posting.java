package edu.buffalo.cse.irf14.index;

import java.util.Map;

public class Posting implements Comparable<Posting> {

	public static class Entry {
		
		int tf;

		public int getTf() {
			return tf;
		}

		public void setTf(int tf) {
			this.tf = tf;
		}

		boolean isTitle;

		public boolean isTitle() {
			return isTitle;
		}

		public void setTitle(boolean isTitle) {
			this.isTitle = isTitle;
		}
		
		@Override
		public String toString() {
			return tf+","+isTitle;
		}
	}

	// private Map<String, Integer> individualPostingsMap;
	private Map<String, Posting.Entry> individualPostingsMap;
	private int totalTermFreq;
	private int totalDocumentFreq;
	private String category;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public int compareTo(Posting posting) {
		int compare = posting.getTotalTermFreq();
		return compare - this.totalTermFreq;
	}

	public Map<String, Posting.Entry> getIndividualPostingsMap() {
		// public Map<String, Integer> getIndividualPostingsMap() {
		return individualPostingsMap;
	}

	public void setIndividualPostingsMap(Map<String, Posting.Entry> individualPostings) {
		// public void setIndividualPostingsMap(Map<String, Integer>
		// individualPostings) {
		this.individualPostingsMap = individualPostings;
	}

	public int getTotalTermFreq() {
		return totalTermFreq;
	}

	public void setTotalTermFreq(int totalTermFreq) {
		this.totalTermFreq = totalTermFreq;
	}

	public int getTotalDocumentFreq() {
		return totalDocumentFreq;
	}

	public void setTotalDocumentFreq(int totalDocumentFreq) {
		this.totalDocumentFreq = totalDocumentFreq;
	}

}