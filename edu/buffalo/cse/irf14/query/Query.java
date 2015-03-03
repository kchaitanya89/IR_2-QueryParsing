package edu.buffalo.cse.irf14.query;

import java.util.ArrayDeque;

/**
 * Class that represents a parsed query
 * 
 * @author nikhillo
 * 
 */
public class Query {

	private ArrayDeque<String> queryStack;
	private StringBuilder query;

	public StringBuilder getQuery() {
		return query;
	}

	public void setQuery(StringBuilder query) {
		this.query = query;
	}

	/**
	 * Method to convert given parsed query into string
	 */
	public String toString() {
		return query.toString();
	}

	public ArrayDeque<String> getQueryStack() {
		return queryStack;
	}

	public void setQueryStack(ArrayDeque<String> queryStack) {
		this.queryStack = queryStack;
	}
}