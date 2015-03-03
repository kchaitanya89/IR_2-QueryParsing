/**
 *
 */
package edu.buffalo.cse.irf14.query;

import java.util.ArrayDeque;

/**
 * @author nikhillo Static parser that converts raw text to Query objects
 */
public class QueryParser {
	
	/**
	 * MEthod to parse the given user query into a Query object
	 * 
	 * @param userQuery
	 *            : The query to parse
	 * @param defaultOperator
	 *            : The default operator to use, one amongst (AND|OR)
	 * @return Query object if successfully parsed, null otherwise
	 */
	public static Query parse(String userQuery, String defaultOperator) {

		if(userQuery==null){
			return null;
		}
		int len = userQuery.length();
		if(len==0){
			return null;
		}
		
		int i = 0;
		String index = null;

		StringBuilder query = new StringBuilder();
		StringBuilder token = new StringBuilder();
		ArrayDeque<String> infix = new ArrayDeque<String>();
		boolean operator = true;
		boolean not = false;
		boolean start = true;
		String lastCat = null;
		int inGrp = 0;
		int grpLen = 0;

		while (i < len) {
			char ch = userQuery.charAt(i);
			if (ch == '"') {
				token.append('"');
				if (start) {
					i++;
					start = false;
					continue;
				} else {
					String tok = token.toString();
					if (!operator) {
						infix.push(defaultOperator);
					}
					if (index != null) {
						tok = index + token.toString();
					} else if (token.indexOf(":", 1) == -1) {
						tok = "Term:" + token.toString();
						if (grpLen > 0) {
							grpLen++;
						} else if (lastCat != null && inGrp == 0) {
							grpLen = 1;
						}
						lastCat = null;
					} else {
						lastCat = token.substring(0, token.indexOf(":"));
						if (grpLen > 1) {
							groupThem(infix, grpLen, false);
						}
						grpLen = 0;
					}
					if (not) {
						tok = "<" + tok + ">";
						not = false;
					}
					infix.push(tok);
					operator = false;
					start = true;
					token = new StringBuilder();
				}
			} else if (ch == '(') {
				inGrp++;
				int prev = i - 1;
				if (prev > -1 && userQuery.charAt(prev) == ':') {
					index = token.toString();
					token = new StringBuilder();
				}
				infix.push("[");

			} else if (ch == ')') {
				inGrp--;
				if (token.length() > 0) {
					String tok = token.toString();
					if (!operator) {
						infix.push(defaultOperator);
					}
					if (index != null) {
						tok = index + token.toString();
					} else if (token.indexOf(":", 1) == -1) {
						tok = "Term:" + token.toString();
					}
					if (not) {
						tok = "<" + tok + ">";
						not = false;
					}
					infix.push(tok);
					operator = false;
					token = new StringBuilder();
				}

				infix.push("]");
				index = null;

			} else if (ch == ' ' && start) {
				if (token.length() == 0) {
					i++;
					continue;
				}
				String tok = token.toString();
				if (!operator
						&& (tok.equalsIgnoreCase("AND") || tok
								.equalsIgnoreCase("OR"))) {
					operator = true;
				} else if (!operator && tok.equalsIgnoreCase("NOT")) {
					operator = not = true;
					//tok = "AND";
				} else {
					if (index != null) {
						tok = index + token.toString();
					} else if (token.indexOf(":", 1) == -1) {
						tok = "Term:" + token.toString();
						if (grpLen > 0) {
							grpLen++;
						} else if (lastCat != null && inGrp == 0) {
							grpLen = 1;
						}
						lastCat = null;
					} else {
						lastCat = token.substring(0, token.indexOf(":"));
						if (grpLen > 1) {
							groupThem(infix, grpLen, false);
						}
						grpLen = 0;
					}
					if (!operator) {
						infix.push(defaultOperator);
					}
					operator = false;
					if (not) {
						tok = "<" + tok + ">";
						not = false;
					}
				}
				infix.push(tok);
				token = new StringBuilder();

			} else {

				token.append(ch);

				if (i == len - 1) {
					String tok = token.toString();
					if (!operator) {
						infix.push(defaultOperator);
					}
					if (index != null) {
						tok = index + token.toString();
					} else if (token.indexOf(":", 1) == -1) {
						tok = "Term:" + token.toString();
						if (grpLen > 0) {
							grpLen++;
						} else if (lastCat != null && inGrp == 0) {
							grpLen = 1;
						}
						lastCat = null;
					} else {
						lastCat = token.substring(0, token.indexOf(":"));
						if (grpLen > 1) {
							groupThem(infix, grpLen, false);
						}
						grpLen = 0;
					}
					if (not) {
						tok = "<" + tok + ">";
						not = false;
					}
					infix.push(tok);
					operator = false;
				}

			}
			i++;
		}

		if (grpLen > 1) {
			groupThem(infix, grpLen, true);
		}
		
		ArrayDeque<String> postfix = new ArrayDeque<String>();
		ArrayDeque<String> operators = new ArrayDeque<String>();
		
		query.append('{');

		while (!infix.isEmpty()) {		
			String tok = infix.pollLast();
			
			//prepare postfix sequence
			if(tok.equals("[") || tok.equalsIgnoreCase("AND") || tok.equalsIgnoreCase("OR")){
				operators.push(tok);
			}else if(tok.equalsIgnoreCase("NOT")){
				operators.push(tok);
				tok = "AND";
			}else if(tok.equals("]")){
				while(!operators.isEmpty()){
					String op = operators.pop();
					if(op.equals("[")){
						break;
					}
					postfix.push(op);
				}
			}else{
				postfix.push(tok);
			}
			
			//convert infix to string representation			
			query.append(tok);
			if (!tok.equals("[")) {
				query.append(' ');
			}
			if (tok.equals("]")) {
				query.deleteCharAt(query.length() - 3);
			}
		}
		if(query.charAt(query.length()-1)==' '){
			query.deleteCharAt(query.length()-1);
		}
		query.append('}');
		//push the remaining operators to postfix
		while(!operators.isEmpty()){
			postfix.push(operators.pop());
		}
		
		Query q = new Query();
		q.setQuery(query);
		q.setQueryStack(postfix);
		
		//System.out.println(query.toString());

		return q;
	}

	private static void groupThem(ArrayDeque<String> stack, int grpLen,
			boolean last) {
		ArrayDeque<String> temp = new ArrayDeque<String>();
		if (last) {
			temp.push("]");
		} else {
			temp.push(stack.pop());// operator
			temp.push("]");
		}
		temp.push(stack.pop());// first term
		grpLen--;
		// close the new group
		while (grpLen > 0) {
			temp.push(stack.pop());// operator
			temp.push(stack.pop());// term
			grpLen--;
		}

		stack.push("[");// open the new group
		while (!temp.isEmpty()) {
			stack.push(temp.pop());
		}
	}

	public static void main(String[] args) {
		parse("Category:War AND prisoners detainees rebels AND Author:Dutt AND Place:Baghdad","OR");
		parse("Category:War AND Author:Dutt AND Place:Baghdad AND prisoners detainees rebels",
				"OR");
		parse("A B C D", "OR");
		parse("(A OR B OR C OR D) AND ((E AND F) OR (G AND H)) AND ((I OR J OR K) AND (L OR M OR N OR O)) AND (P OR (Q OR (R OR (S AND T))))",
				"OR");
		String[] queries = { "hello", "hello world", "\"hello world\"",
				"orange AND yellow", "(black OR blue) AND bruises",
				"Author:rushdie NOT jihad",
				"(Love NOT War) AND Category:(movies NOT crime)",
				"Category:War AND Author:Dutt AND Place:Baghdad AND prisoners detainees rebels" };
		for (String query : queries) {
			parse(query, "OR");
		}
	}
}