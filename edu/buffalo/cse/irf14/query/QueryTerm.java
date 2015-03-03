package edu.buffalo.cse.irf14.query;

public class QueryTerm {

	private int df;
	private int qcnt;
	private double wtq;

	public double getWtq() {
		return wtq;
	}

	public void setWtq(double wtq) {
		this.wtq = wtq;
	}

	public int getDf() {
		return df;
	}

	public void setDf(int df) {
		this.df = df;
	}

	public int getQcnt() {
		return qcnt;
	}

	@Override
	public String toString() {
		return df + "," + qcnt + "," + wtq;
	}

	public void setQcnt(int qcnt) {
		this.qcnt = qcnt;
	}

}
