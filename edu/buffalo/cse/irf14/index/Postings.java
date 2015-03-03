package edu.buffalo.cse.irf14.index;

public class Postings {
	private TernaryTree docMap;
	private int totalTermFreq;
	private int totalDocumentFreq;
	
	public Postings(){
		docMap = new TernaryTree();
	}
	
	public TernaryTree getDocMap() {
		return docMap;
	}
	public void setDocMap(TernaryTree docPostings) {
		this.docMap = docPostings;
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
	
	public void incrementFrequencies(String docId){
		int freq = 1;
		Object val = docMap.get(docId);
		if(val instanceof Integer){
			freq = (Integer)val + 1;
		}
		docMap.put(docId, freq);
		totalTermFreq++;
		totalDocumentFreq = docMap.size();
	}
	
}
