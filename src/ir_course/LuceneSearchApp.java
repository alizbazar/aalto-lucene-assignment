package ir_course;
/*
 * Skeleton class for the Lucene search program implementation
 * Created on 2011-12-21
 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 */


import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class LuceneSearchApp {
	
	private IndexWriter m_indexWriter = null;
	private RAMDirectory m_idx;
	
	public LuceneSearchApp() {
		m_idx = new RAMDirectory();
	}
    
    public IndexWriter getIndexWriter(boolean create) throws IOException {
        if (m_indexWriter == null || create) {
        	StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
        	IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);
            m_indexWriter = new IndexWriter(m_idx, config);
        }
        return m_indexWriter;
    }
	
	public void index(List<RssFeedDocument> docs) throws IOException {
		IndexWriter writer = getIndexWriter(true);
		
		for(RssFeedDocument indexable : docs)
		{
			Document doc = new Document();
			// Index fields as text
			doc.add(new TextField("title", indexable.getTitle(), Field.Store.YES));
			doc.add(new TextField("description", indexable.getDescription(), Field.Store.YES));

			// Index date as int
			String date = DateTools.timeToString(indexable.getPubDate().getTime(), DateTools.Resolution.DAY);
			doc.add(new IntField("date", Integer.parseInt(date), Field.Store.YES));

			writer.addDocument(doc);
		}
		
		writer.close();
		// implement the Lucene indexing here
		
	}
	
	public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) throws IOException {
		
		printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

		List<String> results = new LinkedList<String>();

		IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(m_idx));
		
		BooleanQuery query = new BooleanQuery();
		
		query = addTerms(query, inTitle, "title", Occur.MUST);
		query = addTerms(query, notInTitle, "title", Occur.MUST_NOT);
		query = addTerms(query, inDescription, "description", Occur.MUST);
		query = addTerms(query, notInDescription, "description", Occur.MUST_NOT);
		
		Integer sDate = null;
		Integer eDate = null;
		if (startDate != null)
		{
			sDate = Integer.parseInt(startDate.replace("-", ""));
		}
		if (endDate != null)
		{
			eDate = Integer.parseInt(endDate.replace("-", ""));
		}
		if (sDate != null || eDate != null)
		{			
			NumericRangeQuery<Integer> rangeQ = NumericRangeQuery.newIntRange("date", sDate, eDate, true, true);
			query.add(rangeQ, Occur.MUST);
		}
		
		TopDocs found = searcher.search(query, 100);

		for (int i = 0; i < found.scoreDocs.length; i++)
		{
			int docId = found.scoreDocs[i].doc;
			Document d = searcher.doc(docId);
			results.add(d.get("title"));
		}
		
		// implement the Lucene search here

		return results;
	}
	
	private static BooleanQuery addTerms(BooleanQuery query, List<String> terms, String fieldName, Occur occurrence)
	{
		if (terms == null)
		{
			return query;
		}
		for(String term : terms)
		{
			TermQuery termQuery = new TermQuery(new Term(fieldName, term));
			query.add(new BooleanClause(termQuery, occurrence));
		}
		return query;
	}
	
	public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: "+inTitle);
			if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInTitle != null) {
			System.out.print("not in title: "+notInTitle);
			if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (inDescription != null) {
			System.out.print("in description: "+inDescription);
			if (notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInDescription != null) {
			System.out.print("not in description: "+notInDescription);
			if (startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (startDate != null) {
			System.out.print("startDate: "+startDate);
			if (endDate != null)
				System.out.print("; ");
		}
		if (endDate != null)
			System.out.print("endDate: "+endDate);
		System.out.println("):");
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			RssFeedParser parser = new RssFeedParser();
			parser.parse(args[0]);
			List<RssFeedDocument> docs = parser.getDocuments();
			
			try {
				engine.index(docs);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Error while indexing");
			}

			List<String> inTitle;
			List<String> notInTitle;
			List<String> inDescription;
			List<String> notInDescription;
			List<String> results;

			try {
				// 1) search documents with words "kim" and "korea" in the title
				inTitle = new LinkedList<String>();
				inTitle.add("kim");
				inTitle.add("korea");
					results = engine.search(inTitle, null, null, null, null, null);
				engine.printResults(results);
				
				// 2) search documents with word "kim" in the title and no word "korea" in the description
				inTitle = new LinkedList<String>();
				notInDescription = new LinkedList<String>();
				inTitle.add("kim");
				notInDescription.add("korea");
				results = engine.search(inTitle, null, null, notInDescription, null, null);
				engine.printResults(results);
	
				// 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
				inTitle = new LinkedList<String>();
				inTitle.add("us");
				notInTitle = new LinkedList<String>();
				notInTitle.add("dawn");
				inDescription = new LinkedList<String>();
				inDescription.add("american");
				inDescription.add("confession");
				results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
				engine.printResults(results);
				
				// 4) search documents whose publication date is 2011-12-18
				results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
				engine.printResults(results);
				
				// 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
				inTitle = new LinkedList<String>();
				inTitle.add("video");
				results = engine.search(inTitle, null, null, null, "2000-01-01", null);
				engine.printResults(results);
				
				// 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
				notInDescription = new LinkedList<String>();
				notInDescription.add("canada");
				notInDescription.add("iraq");
				notInDescription.add("israel");
				results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
				engine.printResults(results);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Error while reading");
			}
		}
		else
			System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
	}
}
