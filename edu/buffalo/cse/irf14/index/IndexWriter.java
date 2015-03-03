/**
 *
 */
package edu.buffalo.cse.irf14.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import edu.buffalo.cse.irf14.analysis.Analyzer;
import edu.buffalo.cse.irf14.analysis.AnalyzerFactory;
import edu.buffalo.cse.irf14.analysis.Token;
import edu.buffalo.cse.irf14.analysis.TokenStream;
import edu.buffalo.cse.irf14.analysis.Tokenizer;
import edu.buffalo.cse.irf14.analysis.TokenizerException;
import edu.buffalo.cse.irf14.document.Document;
import edu.buffalo.cse.irf14.document.FieldNames;
import edu.buffalo.cse.irf14.index.Posting.Entry;
import edu.buffalo.cse.irf14.util.Constants;

/**
 * @author nikhillo Class responsible for writing indexes to disk
 */
public class IndexWriter {

	// Map<String, Posting> inMemIndex = new TreeMap<String, Posting>();
	Index termIndex = new Index();
	Index authorIndex = new Index();
	Index placeIndex = new Index();
	Index categoryIndex = new Index();
	Tokenizer tokenizer = new Tokenizer();
	AnalyzerFactory analyzerFactory = AnalyzerFactory.getInstance();
	FieldNames fields[];
	File folder;
	Properties documentLengthMap = new Properties();
	Properties documentCatMap = new Properties();
	String currentDocCategory;
	String oldDocCategory;
	long words_in_corpus;

	/**
	 * Default constructor
	 * 
	 * @param indexDir
	 *            : The root directory to be sued for indexing
	 */
	public IndexWriter(String indexDir) {
		folder = new File(indexDir);
		fields = new FieldNames[] { FieldNames.AUTHOR, FieldNames.AUTHORORG,
				FieldNames.CATEGORY, FieldNames.CONTENT, FieldNames.NEWSDATE,
				FieldNames.PLACE };
	}

	/**
	 * Method to add the given Document to the index This method should take
	 * care of reading the filed values, passing them through corresponding
	 * analyzers and then indexing the results for each indexable field within
	 * the document.
	 * 
	 * @param d
	 *            : The Document to be added
	 * @throws IndexerException
	 *             : In case any error occurs
	 */
	public void addDocument(Document document) throws IndexerException {
		String docID = document.getField(FieldNames.FILEID)[0];
		String[] cats = document.getField(FieldNames.CATEGORY);
		currentDocCategory = cats != null ? cats[0] : "";
		oldDocCategory = documentCatMap.getProperty(docID);

		TokenStream tokenStream = null;
		Integer docWordCount = 0;
		String[] title = document.getField(FieldNames.TITLE);
		if (title != null) {
			docWordCount = addTitleToIndex(title[0], docID);
		}

		if (oldDocCategory != null
				&& !oldDocCategory.equals(currentDocCategory)) {
			try {
				tokenStream = tokenizer.consume(currentDocCategory);
				addStreamToIndex(tokenStream, docID, FieldNames.CATEGORY);
			} catch (TokenizerException e) {
				e.printStackTrace();
			}
		} else {
			for (FieldNames fieldName : fields) {

				String[] fieldContent = document.getField(fieldName);

				if (fieldContent != null) {
					for (String fieldContentLine : fieldContent) {
						try {
							tokenStream = tokenizer.consume(fieldContentLine);
							Analyzer analyzerForField = analyzerFactory
									.getAnalyzerForField(fieldName, tokenStream);
							if (analyzerForField == null) {
								continue;
							}

							while (analyzerForField.increment()) {
							}
							tokenStream = analyzerForField.getStream();
							tokenStream.reset();

							docWordCount += addStreamToIndex(tokenStream,
									docID, fieldName);

						} catch (TokenizerException e) {
							e.printStackTrace();
							// throw new IndexerException();
						}

					}
				}
			}
			words_in_corpus += docWordCount;
			documentLengthMap.put(docID, docWordCount.toString());
		}

		if (!currentDocCategory.isEmpty()) {
			documentCatMap.put(docID, cats[0]);
		}
	}

	private int addTitleToIndex(String title, String docID)
			throws IndexerException {

		TokenStream stream = null;
		try {
			stream = tokenizer.consume(title);
			while (stream.hasNext()) {
				stream.next().markAsTitleWord();
			}
			stream.reset();
			Analyzer analyzerForField = analyzerFactory.getAnalyzerForField(
					FieldNames.TITLE, stream);
			while (analyzerForField.increment()) {
			}
			stream = analyzerForField.getStream();
			stream.reset();
			return addStreamToIndex(stream, docID, FieldNames.TITLE);
		} catch (TokenizerException ex) {
			throw new IndexerException();
		}
	}

	private int addStreamToIndex(TokenStream tokenStream, String docID,
			FieldNames fieldName) {
		Index index = null;
		if (FieldNames.AUTHOR == fieldName || FieldNames.AUTHORORG == fieldName) {
			index = authorIndex;
		} else if (FieldNames.PLACE == fieldName) {
			index = placeIndex;
		} else if (FieldNames.CATEGORY == fieldName) {
			index = categoryIndex;
		} else {
			index = termIndex;
		}
		int streamWC = 0;
		boolean isTitle = fieldName == FieldNames.TITLE;
		while (tokenStream.hasNext()) {
			streamWC++;
			Token nextToken = tokenStream.next();
			String nextTokenString = nextToken.toString();
			nextTokenString = nextTokenString.toLowerCase();
			Posting posting = index.get(nextTokenString);

			Map<String, Posting.Entry> postingMap = null;
			int currentFreq = 1;
			if (posting == null) {
				posting = new Posting();
				postingMap = new TreeMap<String, Posting.Entry>();
				Posting.Entry pentry = new Posting.Entry();
				pentry.setTf(currentFreq);
				if (isTitle) {
					pentry.setTitle(isTitle);
				}
				postingMap.put(docID, pentry);
				posting.setTotalTermFreq(1);
				posting.setTotalDocumentFreq(1);
				posting.setIndividualPostingsMap(postingMap);
				posting.setCategory(currentDocCategory);
			} else {
				Posting.Entry pentry = posting.getIndividualPostingsMap().get(
						docID);
				if (pentry == null) {
					pentry = new Posting.Entry();
				}
				currentFreq = pentry.getTf();
				currentFreq += 1;
				pentry.setTf(currentFreq);
				if (isTitle) {
					pentry.setTitle(isTitle);
				}
				postingMap = posting.getIndividualPostingsMap();
				postingMap.put(docID, pentry);
				posting.setTotalTermFreq(posting.getTotalTermFreq() + 1);
				posting.setTotalDocumentFreq(postingMap.size());
				posting.setIndividualPostingsMap(postingMap);
			}
			index.put(nextTokenString, posting);
		}
		return streamWC;
	}

	/**
	 * Method that indicates that all open resources must be closed and cleaned
	 * and that the entire indexing operation has been completed.
	 * 
	 * @throws IndexerException
	 *             : In case any error occurs
	 */
	public void close() throws IndexerException {
		writeIndex(Constants.termIndexFolderName, termIndex);
		writeIndex(Constants.authorIndexFolderName, authorIndex);
		writeIndex(Constants.categoryIndexFolderName, categoryIndex);
		writeIndex(Constants.placeIndexFolderName, placeIndex);
	}

	/**
	 * @param termIndex2
	 * @throws IndexerException
	 */
	private void writeIndex(String folderName, Index index)
			throws IndexerException {

		// Write document length map to the file
		try {
			documentLengthMap.put("A_D_L", new Long(words_in_corpus
					/ documentLengthMap.size()).toString());

			documentLengthMap.store(
					new FileOutputStream(new File(folder.getAbsolutePath()
							+ File.separatorChar + "DocumentSize.properties")),
					"");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			throw new IndexerException();
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new IndexerException();
		}

		boolean isTermIndex = index == termIndex;
		Field[] fields = Index.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Posting> mapToWrite = (TreeMap<String, Posting>) field
						.get(index);
				Properties properties = new Properties();
				Iterator<String> keys = mapToWrite.keySet().iterator();
				Iterator<Posting> values = mapToWrite.values().iterator();
				while (keys.hasNext()) {
					String term = keys.next();
					Posting posting = values.next();
					Map<String, Posting.Entry> termMap = posting
							.getIndividualPostingsMap();

					StringBuilder docID = new StringBuilder();
					StringBuilder freq = new StringBuilder();
					StringBuilder titleFlag = new StringBuilder();
					for (String keyDocID : termMap.keySet()) {
						Posting.Entry pentry = termMap.get(keyDocID);
						Integer valueFreq = pentry.getTf();
						boolean isTitle = pentry.isTitle();

						docID.append(keyDocID).append("|");
						freq.append(valueFreq).append("|");
						if (isTermIndex) {
							titleFlag.append(isTitle ? "1" : "0").append("|");
						}
					}

					docID.deleteCharAt(docID.length() - 1).append('@')
							.append(freq.deleteCharAt(freq.length() - 1))
							.append('@');

					if (isTermIndex) {
						docID.append(
								titleFlag.deleteCharAt(titleFlag.length() - 1))
								.append('@');
					}

					docID.append(posting.getTotalTermFreq()).append('@')
							.append(posting.getTotalDocumentFreq());

					properties.put(term, docID.toString());

				}
				File indexFolder = new File(folder.getAbsolutePath()
						+ File.separatorChar + folderName);
				indexFolder.mkdir();
				File file = new File(indexFolder.getAbsolutePath()
						+ File.separatorChar + field.getName() + ".properties");
				FileOutputStream fileOut;
				try {
					fileOut = new FileOutputStream(file);
					properties.store(fileOut, null);
					fileOut.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					throw new IndexerException();
				} catch (IOException e) {
					e.printStackTrace();
					throw new IndexerException();
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				throw new IndexerException();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new IndexerException();
			}
		}
	}
}
