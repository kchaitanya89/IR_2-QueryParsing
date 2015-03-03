package edu.buffalo.cse.irf14;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.buffalo.cse.irf14.analysis.TermAnalyzer;
import edu.buffalo.cse.irf14.analysis.TokenStream;
import edu.buffalo.cse.irf14.analysis.Tokenizer;
import edu.buffalo.cse.irf14.analysis.TokenizerException;
import edu.buffalo.cse.irf14.document.Parser;
import edu.buffalo.cse.irf14.index.Index;
import edu.buffalo.cse.irf14.index.IndexReader;
import edu.buffalo.cse.irf14.index.IndexType;
import edu.buffalo.cse.irf14.index.Posting;
import edu.buffalo.cse.irf14.query.Query;
import edu.buffalo.cse.irf14.query.QueryParser;
import edu.buffalo.cse.irf14.query.QueryTerm;
import edu.buffalo.cse.irf14.util.Constants;

/**
 * Main class to run the searcher. As before implement all TODO methods unless
 * marked for bonus
 * 
 * @author nikhillo
 * 
 */
public class SearchRunner {
	public enum ScoringModel {
		TFIDF, OKAPI
	};

	private Index termIndex;
	private Index authorIndex;
	private Index placeIndex;
	private Index categoryIndex;
	private static int N;
	private static final TreeMap<String, Posting.Entry> empty = new TreeMap<String, Posting.Entry>();
	private final File corpusDirectory;
	private char mode;
	private PrintStream out;
	private Properties docLengths;
	private int avgDocLength;
	public static final DecimalFormat decFormat = new DecimalFormat("#.#####");

	long start;
	String userQuery;

	private Doc[] scores = new Doc[0];

	// TUNING PARAMETERS
	public static final float K3_OKAPI = 2.0f;
	public static final float K1_OKAPI = 1.2f;
	public static final float B_OKAPI = 0.85f;

	public static final float TITLE_TF = 0.6f;
	public static final float BODY_TF = 0.4f;

	private int maxSnippetLength = 250;

	/**
	 * Default (and only public) constuctor
	 * 
	 * @param indexDir
	 *            : The directory where the index resides
	 * @param corpusDir
	 *            : Directory where the (flattened) corpus resides
	 * @param mode
	 *            : Mode, one of Q or E
	 * @param stream
	 *            : Stream to write output to
	 */
	public SearchRunner(String indexDir, String corpusDir, char mode,
			PrintStream stream) {
		this.mode = mode;
		out = stream;
		loadIndexes(indexDir);
		corpusDirectory = new File(corpusDir);
		setN();
	}

	private void setN() {
		if (corpusDirectory.isDirectory()) {
			N = corpusDirectory.list().length;
		}
	}

	private void loadIndexes(String indexDir) {
		authorIndex = new IndexReader(indexDir, IndexType.AUTHOR).getIndex();
		placeIndex = new IndexReader(indexDir, IndexType.PLACE).getIndex();
		termIndex = new IndexReader(indexDir, IndexType.TERM).getIndex();
		categoryIndex = new IndexReader(indexDir, IndexType.CATEGORY)
				.getIndex();
		docLengths = new Properties();
		try {
			docLengths.load(new FileInputStream(new File(indexDir
					+ File.separatorChar + Constants.docLengthsFileName)));
			avgDocLength = Integer.parseInt((String) docLengths.get("A_D_L"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class Doc implements Comparable<Doc> {
		private String name;
		private double score;
		private String formattedScore;

		public String getFormattedScore() {
			return formattedScore;
		}

		public void setFormattedScore(String formattedScore) {
			this.formattedScore = formattedScore;
		}

		public Doc(String docID, double prod) {
			name = docID;
			score = prod;
			formattedScore = decFormat.format(prod);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
			formattedScore = decFormat.format(score);
		}

		@Override
		public String toString() {
			return name + " - " + formattedScore;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Doc) {
				Doc d1 = (Doc) obj;
				return name.equals(d1.name);
			}
			return false;
		}

		@Override
		public int compareTo(Doc o) {
			return Double.compare(o.getScore(), score);
		}
	}

	/**
	 * Method to execute given query in the Q mode
	 * 
	 * @param userQuery
	 *            : Query to be parsed and executed
	 * @param model
	 *            : Scoring Model to use for ranking results
	 */
	public void query(String userQuery, ScoringModel model) {
		if (userQuery == null || userQuery.isEmpty()) {
			return;
		}
		this.userQuery = userQuery;
		start = System.currentTimeMillis();
		Query query = QueryParser.parse(userQuery, "OR");
		HashMap<String, QueryTerm> queryTerms = new HashMap<String, QueryTerm>();
		HashMap<String, HashMap<String, Posting.Entry>> forwardIndex = new HashMap<String, HashMap<String, Posting.Entry>>();
		TreeSet<String> resultDocs = evaluate(query, forwardIndex, queryTerms);

		if (model == null) {
			if (queryTerms.size() < 3) {
				model = ScoringModel.TFIDF;
			} else {
				model = ScoringModel.OKAPI;
			}
		}

		if (model == ScoringModel.TFIDF) {
			if (queryTerms.size() == 1) {
				tfidfScoringForOneTermQueries(resultDocs, forwardIndex,
						queryTerms);
			} else {
				tfidfScoring(resultDocs, forwardIndex, queryTerms);
			}
		} else {
			okapiScoring(resultDocs, forwardIndex, queryTerms);
		}
		if (mode == 'Q') {
			generateSnippets(scores, queryTerms);
		}
	}

	/**
	 * @param scores
	 * @param queryTerms
	 */
	private void generateSnippets(Doc[] scores,
			HashMap<String, QueryTerm> queryTerms) {
		Snippet[] snippets = new Snippet[scores.length];

		int size = scores.length > 10 ? 10 : scores.length;
		// Retrieve the title
		for (int i = 0; i < size; i++) {
			Scanner scanner = null;
			try {

				StringBuilder fileContentBuilder = new StringBuilder();
				scanner = new Scanner(new File(
						corpusDirectory.getAbsolutePath() + File.separatorChar
								+ scores[i].name));
				String title = Parser.getNextNonEmptyLine(scanner);

				Snippet snippet = new Snippet(title);
				snippets[i] = snippet;

				while (scanner.hasNext()) {
					String nextLine = scanner.nextLine();
					fileContentBuilder.append(nextLine);
				}
				scanner.close();

				String fileContent = fileContentBuilder.toString();

				BreakIterator breakIterator = BreakIterator
						.getSentenceInstance();
				breakIterator.setText(fileContent);
				int start = breakIterator.first();

				StringBuilder snippetBuilder = new StringBuilder();
				for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator
						.next()) {
					String sentence = fileContent.substring(start, end).trim();
					for (String queryTerm : queryTerms.keySet()) {
						// if a term occurs
						int position = sentence.toLowerCase().indexOf(
								queryTerm.toLowerCase());
						if (position != -1) {
							String substring = null;
							// If query term is the last word of sentence
							if (position < sentence.length()
									- (queryTerm.length() + 1)) {
								substring = sentence.substring(0,
										sentence.length() - 3)
										+ "...";
							} else {
								substring = sentence.substring(0,
										sentence.length());
							}
							substring = substring
									.replaceAll("(?i)(" + queryTerm + ")",
											"<b>" + "$1" + "<\\\\b>");
							snippetBuilder.append(substring);
							break;
						}
					}
				}

				String snippetString = snippetBuilder.toString();
				if (snippetString.length() > maxSnippetLength) {
					snippetString = snippetString.substring(0,
							maxSnippetLength - 3) + "...";
				}

				snippet.setSummary(snippetString);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		long end = System.currentTimeMillis();
		out.append(userQuery+"\r\n");
		System.out.println("Query Time: " + (end - start)+"\r\n");
		// System.out.println("Snippets: ----------");
		for (int j = 0; j < size; j++) {
			out.append("Document Name: " + scores[j].getName()+"\r\n");
			out.append("Rank: " + (j + 1)+"\r\n");
			out.append("Title: " + snippets[j].title+"\r\n");
			out.append("Summary: " + snippets[j].summary+"\r\n");
			out.append("Relevancy score: " + scores[j].formattedScore+"\r\n");
			out.append("\r\n");
			out.close();
		}
		
	}

	private void okapiScoring(TreeSet<String> docs,
			HashMap<String, HashMap<String, Posting.Entry>> forwardIndex,
			HashMap<String, QueryTerm> queryTerms) {
		ArrayList<Doc> scoresList = new ArrayList<Doc>();

		for (String docID : docs) {
			HashMap<String, Posting.Entry> termMap = forwardIndex.get(docID);

			double prod = 0.0;
			for (String term : termMap.keySet()) {
				QueryTerm queryTerm = queryTerms.get(term);
				double idf = Math.log10((N + 1) / (1 + queryTerm.getDf()));
				Posting.Entry pentry = termMap.get(term);
				int tf = pentry.getTf();
				if (pentry.isTitle()) {
					tf += 2;
				}

				int len = Integer.parseInt((String) docLengths.get(docID));
				double den = K1_OKAPI
						* ((1 - B_OKAPI) + B_OKAPI * (len / avgDocLength)) + tf;
				double num = (K1_OKAPI + 1) * tf;

				double wtq = (K3_OKAPI + 1) * queryTerm.getQcnt()
						/ (K3_OKAPI + queryTerm.getQcnt());

				prod += idf * (num / den) * wtq;
			}

			prod *= 0.5 * (double) termMap.size() / queryTerms.size();

			if (prod > 0.0) {
				scoresList.add(new Doc(docID, prod));
			}
		}

		Object[] scoresObj = scoresList.toArray();
		scores = new Doc[scoresObj.length];
		Arrays.sort(scoresObj);
		double highScore = scoresObj != null && scoresObj.length > 0 ? ((Doc) scoresObj[0])
				.getScore() : 0.0;
		for (int j = 0; j < scoresObj.length; j++) {
			((Doc) scoresObj[j]).setScore(((Doc) scoresObj[j]).getScore()
					/ highScore);
			scores[j] = (Doc) scoresObj[j];
			// System.out.println(scoresObj[j]);
		}
	}

	private void tfidfScoringForOneTermQueries(TreeSet<String> docs,
			HashMap<String, HashMap<String, Posting.Entry>> forwardIndex,
			HashMap<String, QueryTerm> queryTerms) {

		ArrayList<Doc> scoresList = new ArrayList<Doc>();
		for (String docID : docs) {
			HashMap<String, Posting.Entry> termMap = forwardIndex.get(docID);

			double tfidf = 0;
			for (String term : termMap.keySet()) {
				QueryTerm queryTerm = queryTerms.get(term);
				double idf = Math.log10(N / queryTerm.getDf());
				Posting.Entry pentry = termMap.get(term);
				double tf = pentry.getTf();
				if (pentry.isTitle()) {
					tf = TITLE_TF + BODY_TF * Math.log10(tf);
				} else {
					tf = BODY_TF * (1 + Math.log10(tf));
				}
				tfidf += idf * tf;
			}
			int parseInt = Integer.parseInt((String) docLengths.get(docID));
			// System.out.println(docID + " " + parseInt);
			tfidf /= parseInt;

			if (tfidf > 0.0) {
				scoresList.add(new Doc(docID, tfidf));
			}
		}

		Object[] scoresObj = scoresList.toArray();
		scores = new Doc[scoresObj.length];
		Arrays.sort(scoresObj);
		double highScore = scoresObj != null && scoresObj.length > 0 ? ((Doc) scoresObj[0])
				.getScore() : 0.0;
		for (int j = 0; j < scoresObj.length; j++) {
			((Doc) scoresObj[j]).setScore(((Doc) scoresObj[j]).getScore()
					/ highScore);
			scores[j] = (Doc) scoresObj[j];
			// System.out.println(scoresObj[j]);
		}
	}

	private void tfidfScoring(TreeSet<String> docs,
			HashMap<String, HashMap<String, Posting.Entry>> forwardIndex,
			HashMap<String, QueryTerm> queryTerms) {

		ArrayList<Doc> scoresList = new ArrayList<Doc>();

		calculateWtq(queryTerms);
		// System.out.println(queryTerms);
		for (String docID : docs) {
			HashMap<String, Posting.Entry> termMap = forwardIndex.get(docID);

			double sum = 0;
			// Calculate the denominator
			for (Posting.Entry pentry : termMap.values()) {
				double tf = pentry.getTf();
				if (pentry.isTitle()) {
					tf = TITLE_TF + BODY_TF * Math.log10(tf);
				} else {
					tf = BODY_TF * (1 + Math.log10(tf));
				}
				sum += tf * tf;
			}

			double sqrt = Math.sqrt(sum);
			double prod = 0;

			for (String term : termMap.keySet()) {
				QueryTerm queryTerm = queryTerms.get(term);
				Posting.Entry pentry = termMap.get(term);
				double tf = pentry.getTf();
				if (pentry.isTitle()) {
					tf = TITLE_TF + BODY_TF * (tf - 1);
				} else {
					tf = BODY_TF * tf;
				}
				prod += queryTerm.getWtq() * (tf / sqrt);

			}

			// normalization with respect to lenght of the document
			int parseInt = Integer.parseInt((String) docLengths.get(docID));
			prod = prod / parseInt;

			// weightage corresponding to how many query terms are present in
			// the document
			prod *= (double) termMap.size() / queryTerms.size();

			if (prod > 0.0) {
				scoresList.add(new Doc(docID, prod));
			}
		}
		Object[] scoresObj = scoresList.toArray();
		scores = new Doc[scoresObj.length];
		Arrays.sort(scoresObj);
		double highScore = scoresObj != null && scoresObj.length > 0 ? ((Doc) scoresObj[0])
				.getScore() : 0.0;
		for (int j = 0; j < scoresObj.length; j++) {
			((Doc) scoresObj[j]).setScore(((Doc) scoresObj[j]).getScore()
					/ highScore);
			scores[j] = (Doc) scoresObj[j];
			// System.out.println(scoresObj[j]);
		}
	}

	private void calculateWtq(HashMap<String, QueryTerm> queryTerms) {
		for (String term : queryTerms.keySet()) {
			QueryTerm qterm = queryTerms.get(term);
			double tf = 1 + Math.log10(qterm.getQcnt());
			double idf = Math.log10((1 + N) / (1 + qterm.getDf()));
			qterm.setWtq(idf * tf);
		}
	}

	private TreeSet<String> evaluate(Query query,
			HashMap<String, HashMap<String, Posting.Entry>> forwardIndex,
			HashMap<String, QueryTerm> queryterms) {

		ArrayDeque<String> qstack = query.getQueryStack();
		ArrayDeque<TreeSet<String>> results = new ArrayDeque<TreeSet<String>>();
		while (!qstack.isEmpty()) {
			String token = qstack.pollLast();
			if (token.equalsIgnoreCase("OR")) {
				handleOR(results);
			} else if (token.equalsIgnoreCase("AND")) {
				handleAND(results);
			} else if (token.equalsIgnoreCase("NOT")) {
				handleNOT(results);
			} else {
				TreeMap<String, Posting.Entry> invIndex = null;
				if (token.charAt(0) == '<') {
					token = token.substring(1, token.length() - 1);
				}
				int i = token.indexOf(':');
				String indxString = token.substring(0, i);
				String term = token.substring(i + 1);
				term = format(term);
				// System.out.println(term);
				invIndex = queryIndex(indxString, term, queryterms);
				TreeSet<String> result = transform(term, invIndex, forwardIndex);
				results.push(result);
			}
		}
		// System.out.println("FI");
		// System.out.println(forwardIndex);

		TreeSet<String> finalResult = results.pop();// final result
		// System.out.println(finalResult);

		return finalResult;
		// display the results in the required format
	}

	private String format(String term) {
		try {
			TokenStream stream = new Tokenizer().consume(term);
			TermAnalyzer ta = new TermAnalyzer(stream);
			while (ta.increment()) {
			}
			stream.reset();

			term = stream.hasNext() ? stream.next().toString() : null;
		} catch (TokenizerException e) {
			e.printStackTrace();
		}
		return term.toLowerCase();
	}

	private TreeSet<String> transform(String term,
			TreeMap<String, Posting.Entry> invIndex,
			HashMap<String, HashMap<String, Posting.Entry>> forwardIndex) {

		Set<Map.Entry<String, Posting.Entry>> entries = invIndex.entrySet();
		TreeSet<String> docs = new TreeSet<String>();
		for (Map.Entry<String, Posting.Entry> entry : entries) {
			String key = entry.getKey();
			docs.add(key);
			Posting.Entry pentry = entry.getValue();
			HashMap<String, Posting.Entry> hashMap = forwardIndex.get(key);
			if (hashMap == null) {
				hashMap = new HashMap<String, Posting.Entry>();
				forwardIndex.put(key, hashMap);
			}
			hashMap.put(term, pentry);
		}
		return docs;
	}

	private void handleNOT(ArrayDeque<TreeSet<String>> results) {
		TreeSet<String> two = results.pop();// second operand
		TreeSet<String> one = results.pop();// first operand
		TreeSet<String> result = new TreeSet<String>();

		if (!one.isEmpty()) {

			String p1 = one.first();

			if (!two.isEmpty()) {
				String p2 = two.first();
				// loop and then remove two's documents from one's list
				while (p1 != null && p2 != null) {
					int cmp = p1.compareTo(p2);
					if (cmp == 0) {
						p1 = one.higher(p1);
						p2 = two.higher(p2);
					} else if (cmp < 0) {
						result.add(p1);
						p1 = one.higher(p1);
					} else {
						p2 = two.higher(p2);
					}
				}
			}

			// push the remaining elements of one into result
			while (p1 != null) {
				result.add(p1);
				p1 = one.higher(p1);
			}
		}

		// push the resulting list to the results stack
		results.push(result);
	}

	private TreeMap<String, Posting.Entry> queryIndex(String indxString,
			String term, HashMap<String, QueryTerm> queryTerms) {
		Index index = findIndex(indxString);
		Posting p = index.get(term);
		QueryTerm qterm = queryTerms.get(term);
		if (qterm == null) {
			qterm = new QueryTerm();
			queryTerms.put(term, qterm);
		}
		qterm.setQcnt(qterm.getQcnt() + 1);

		if (p != null) {
			qterm.setDf(p.getTotalDocumentFreq());
			return (TreeMap<String, Posting.Entry>) p
					.getIndividualPostingsMap();
		}
		return empty;
	}

	private Index findIndex(String indxString) {
		if (indxString.equalsIgnoreCase("Author")) {
			return authorIndex;
		} else if (indxString.equalsIgnoreCase("Place")) {
			return placeIndex;
		} else if (indxString.equalsIgnoreCase("Category")) {
			return categoryIndex;
		} else {
			return termIndex;
		}
	}

	private void handleAND(ArrayDeque<TreeSet<String>> results) {
		TreeSet<String> two = results.pop();// second operand
		TreeSet<String> one = results.pop();// first operand
		TreeSet<String> result = new TreeSet<String>();

		if (!one.isEmpty() && !two.isEmpty()) {
			String p1 = one.first();
			String p2 = two.first();
			// loop and then take the common documents from both the lists
			while (p1 != null && p2 != null) {
				int cmp = p1.compareTo(p2);
				if (cmp == 0) {
					result.add(p1);
					p1 = one.higher(p1);
					p2 = two.higher(p2);
				} else if (cmp < 0) {
					p1 = one.higher(p1);
				} else {
					p2 = two.higher(p2);
				}
			}
		}

		// push the resulting list to the results stack
		results.push(result);
	}

	private void handleOR(ArrayDeque<TreeSet<String>> results) {
		TreeSet<String> two = results.pop();// second operand
		TreeSet<String> one = results.pop();// first operand
		TreeSet<String> result = new TreeSet<String>();

		String p1 = null;
		String p2 = null;

		if (!one.isEmpty()) {
			p1 = one.first();
		}

		if (!two.isEmpty()) {
			p2 = two.first();
		}

		// loop and then take the union of documents from both the lists
		while (p1 != null && p2 != null) {
			int cmp = p1.compareTo(p2);
			if (cmp == 0) {
				result.add(p1);
				p1 = one.higher(p1);
				p2 = two.higher(p2);
			} else if (cmp < 0) {
				result.add(p1);
				p1 = one.higher(p1);
			} else {
				result.add(p2);
				p2 = two.higher(p2);
			}
		}

		// push the remaining elements of one and two into the result
		while (p1 != null) {
			result.add(p1);
			p1 = one.higher(p1);
		}
		while (p2 != null) {
			result.add(p2);
			p2 = one.higher(p2);
		}

		// push the resulting list to the results stack
		results.push(result);
	}

	/**
	 * Method to execute queries in E mode
	 * 
	 * @param queryFile
	 *            : The file from which queries are to be read and executed
	 */
	public void query(File queryFile) {
		try {
			FileReader in = new FileReader(queryFile);
			BufferedReader bufferedReader = new BufferedReader(in);

			String readLine = bufferedReader.readLine();
			StringBuilder queryFileBuilder = new StringBuilder();

			while (readLine != null) {
				queryFileBuilder.append(readLine + "\r\n");
				readLine = bufferedReader.readLine();
			}
			bufferedReader.close();

			Pattern numberOfQueriesPattern = Pattern.compile(
					"numQueries=([0-9]+)", Pattern.CASE_INSENSITIVE);
			Pattern queriesPattern = Pattern.compile("(.*):\\{(.*)\\}",
					Pattern.CASE_INSENSITIVE);

			String fileContent = queryFileBuilder.toString();
			Matcher queryMatcher = queriesPattern.matcher(fileContent);
			Matcher numberOfQueriesMatcher = numberOfQueriesPattern
					.matcher(fileContent);

			numberOfQueriesMatcher.find();
			int numberOfInputQueries = Integer.parseInt(numberOfQueriesMatcher
					.group(1).trim());

			TreeMap<String, String> queryMap = new TreeMap<String, String>();

			while (queryMatcher.find()) {
				String grp1 = queryMatcher.group(1);
				String grp2 = queryMatcher.group(2);

				queryMap.put(grp1, grp2);
			}

			StringBuilder outputFileBuilder = new StringBuilder();
			int resultCounter = 0;
			for (String queryID : queryMap.keySet()) {
				query(queryMap.get(queryID), null);
				resultCounter++;
				outputFileBuilder.append(queryID);
				outputFileBuilder.append(":{");
				int k = Math.min(10, scores.length);
				for (int i = 0; i < k; i++) {
					outputFileBuilder.append(scores[i].getName());
					outputFileBuilder.append("#");
					outputFileBuilder.append(scores[i].getScore());
					outputFileBuilder.append(", ");
				}
				outputFileBuilder.delete(outputFileBuilder.length() - 2,
						outputFileBuilder.length() - 1);
				outputFileBuilder.append("}");
				outputFileBuilder.append("\r\n");
			}

			outputFileBuilder.insert(0, "numResults=" + resultCounter + "\r\n");

			out.append(outputFileBuilder);
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * General cleanup method
	 */
	public void close() {
		out.close();
	}

	/**
	 * Method to indicate if wildcard queries are supported
	 * 
	 * @return true if supported, false otherwise
	 */
	public static boolean wildcardSupported() {
		// TODO: CHANGE THIS TO TRUE ONLY IF WILDCARD BONUS ATTEMPTED
		return false;
	}

	/**
	 * Method to get substituted query terms for a given term with wildcards
	 * 
	 * @return A Map containing the original query term as key and list of
	 *         possible expansions as values if exist, null otherwise
	 */
	public Map<String, List<String>> getQueryTerms() {
		// TODO:IMPLEMENT THIS METHOD IFF WILDCARD BONUS ATTEMPTED
		return null;
	}

	/**
	 * Method to indicate if speel correct queries are supported
	 * 
	 * @return true if supported, false otherwise
	 */
	public static boolean spellCorrectSupported() {
		// TODO: CHANGE THIS TO TRUE ONLY IF SPELLCHECK BONUS ATTEMPTED
		return false;
	}

	/**
	 * Method to get ordered "full query" substitutions for a given misspelt
	 * query
	 * 
	 * @return : Ordered list of full corrections (null if none present) for the
	 *         given query
	 */
	public List<String> getCorrections() {
		// TODO: IMPLEMENT THIS METHOD IFF SPELLCHECK EXECUTED
		return null;
	}

	class Snippet {
		/**
		 * @param title
		 */
		public Snippet(String title) {
			this.title = title;
		}

		private String title;
		private String summary;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getSummary() {
			return summary;
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "\nTitle : " + title + "\n" + "Summary: " + summary + "\n";
		}
	}

//	public static void main(String[] args) {
//		// long start = System.currentTimeMillis();
//		SearchRunner goog = new SearchRunner(
//				"C:\\Users\\Ashok\\Downloads\\test",
//				"C:\\Users\\Ashok\\Downloads\\training_flat", 'E', System.out);
//		// goog.query("\"Adobe\"",null);
//		goog.query(new File("C:\\Users\\Ashok\\Desktop\\QF.txt"));
//		// long end = System.currentTimeMillis();
//		// System.out.println(end - start);
//	}
}