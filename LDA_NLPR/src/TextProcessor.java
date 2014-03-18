import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.DefaultLemmatizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.Lemmatizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.lexicon.Lexicon;
import edu.northwestern.at.morphadorner.corpuslinguistics.partsofspeech.PartOfSpeechTags;
import edu.northwestern.at.morphadorner.corpuslinguistics.postagger.DefaultPartOfSpeechTagger;
import edu.northwestern.at.morphadorner.corpuslinguistics.postagger.PartOfSpeechTagger;
import edu.northwestern.at.morphadorner.corpuslinguistics.spellingstandardizer.DefaultSpellingStandardizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.spellingstandardizer.SpellingStandardizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.PennTreebankTokenizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.WordTokenizer;
import edu.northwestern.at.utils.StringUtils;


public class TextProcessor {


	String delimer = " | ";
	
	SentenceDetectorME sdetector;
	Tokenizer tokenizer;
	TokenNameFinder perNameFinder;
	TokenNameFinder locNameFinder;
	TokenNameFinder orgNameFinder;
	TokenNameFinder timNameFinder;
	TokenNameFinder datNameFinder;
	TokenNameFinder monNameFinder;
	
	private static final boolean dbg = false;
	PartOfSpeechTagger partOfSpeechTagger;
	Lexicon wordLexicon;
	PartOfSpeechTags partOfSpeechTags;
	
	Lemmatizer lemmatizer;
	SpellingStandardizer standardizer;
	WordTokenizer spellingTokenizer;
	
	
	
	public TextProcessor() {
		
		try {
			System.out.println("Loading trained Open NLP models..");
			FileInputStream modelFileSent     = new FileInputStream("models/en-sent.bin");
			FileInputStream modelFileToken    = new FileInputStream("models/en-token.bin");
			FileInputStream modelFilePerNames = new FileInputStream("models/en-ner-person.bin");
			FileInputStream modelFileOrgNames = new FileInputStream("models/en-ner-organization.bin");
			FileInputStream modelFileLocNames = new FileInputStream("models/en-ner-location.bin");
			FileInputStream modelFileTimNames = new FileInputStream("models/en-ner-time.bin");
			FileInputStream modelFileDatNames = new FileInputStream("models/en-ner-date.bin");
			FileInputStream modelFileMonNames = new FileInputStream("models/en-ner-money.bin");

			sdetector = new SentenceDetectorME(new SentenceModel(modelFileSent));
			tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			
			perNameFinder = new NameFinderME(new TokenNameFinderModel(modelFilePerNames));
			locNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileLocNames));
			orgNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileOrgNames));
			
			timNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileTimNames));
			datNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileDatNames));
			monNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileMonNames));	
			
			standardizer = new DefaultSpellingStandardizer();
			
			partOfSpeechTagger = new DefaultPartOfSpeechTagger();
			wordLexicon = partOfSpeechTagger.getLexicon();
			lemmatizer =  new DefaultLemmatizer();
			spellingTokenizer = new PennTreebankTokenizer();
			partOfSpeechTags   =  wordLexicon.getPartOfSpeechTags();
			
			modelFileSent.close();
			modelFileToken.close();
			
			modelFilePerNames.close();
			modelFileOrgNames.close();
			modelFileLocNames.close();
			
			modelFileTimNames.close();
			modelFileDatNames.close();
			modelFileMonNames.close();
			
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	

	//load stop-words from a file
	private HashSet<String> loadStopWords(String stopwordsFile) {
		HashSet<String> stopWords = new HashSet<>();
		 
		try {
			
			BufferedReader br = new BufferedReader(new FileReader("stopwords/" + stopwordsFile));
			String stopword;
			while ((stopword = br.readLine()) != null) {
			   stopWords.add(stopword);
			}
			br.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stopWords;
	}
	
	//remove stop-words from String tokens
	private List<String> removeStopWords(String[] tokens, HashSet<String> stopWords) {
		List<String> newTokens = new ArrayList<String>(Arrays.asList(tokens));
		for (String token : tokens){
			if (stopWords.contains(token)){
				newTokens.remove(token);
			}
		}
		return newTokens;
	}
	
	 
	private List<List<AdornedWord>> stemSentences(List<List<AdornedWord>> taggedSentences) {
		
				List<List<AdornedWord>> stemmedSentences = new ArrayList<>();
				for(List<AdornedWord> taggedSentence : taggedSentences) {
	 				
				List<AdornedWord> stemmedSentence = new ArrayList<>();

				 for ( AdornedWord adornedWord : taggedSentence ) {
		             StandardSpellSetter.setStandardSpelling(adornedWord, standardizer, partOfSpeechTags);
		             LemmaSetter.setLemma(adornedWord, wordLexicon, lemmatizer, partOfSpeechTags, spellingTokenizer);
		             		             
		             stemmedSentence.add(adornedWord); 
		         }

				 stemmedSentences.add(stemmedSentence); // context is now list of adornedWords
			}

	 			return stemmedSentences;	
	 	} 
		 
		 
	
	
	
	// tag a name entity with a specified replacement
	private String tagNames(String[] tokens, String sentence, TokenNameFinder nameFinder, String replacement) {
		Span[] nameSpans = nameFinder.find(tokens);

		if(nameSpans.length > 0) {
			if(dbg)System.out.println("Repace name entities: " + Arrays.toString(Span.spansToStrings(nameSpans, tokens)) + " with: " + replacement);
	    	String[] names = Span.spansToStrings(nameSpans, tokens);	
	    	for(String name : names)
	    		sentence = StringUtils.replaceFirst(sentence, name, replacement);
	    }
		return sentence;
	}
	// tag all name entities
	private String tagNames(String[] tokens, String sentence) {		
			sentence = tagNames(tokens, sentence, perNameFinder,  "Miroslav");
			sentence = tagNames(tokens, sentence, locNameFinder,  "Bulgaria");
			sentence = tagNames(tokens, sentence, orgNameFinder,  "FFA");
			sentence = tagNames(tokens, sentence, timNameFinder,  "morning");
			sentence = tagNames(tokens, sentence, datNameFinder,  "today");
			sentence = tagNames(tokens, sentence, monNameFinder,  "pounds");
			
			return sentence;
	}

	 
	 public List<ContextList> processContextLists(List<ContextList> allContextLists, String stopwordsFile) {
		 List<ContextList>  newContextLists = new ArrayList<>();
		 for(ContextList contextList : allContextLists) {
			 newContextLists.add(processContextList(contextList, stopwordsFile));
		 }
		 return newContextLists;
	 }
	 
	 
	 // process a context list?
	 public ContextList processContextList(ContextList contextList, String stopwordsFile) {
		 System.out.println("Preprocessing documents (Punctuation, Names, Stopwords, Spellcheck, PoS, Lemmatizer)..");
		 ContextList newContextList = new ContextList(contextList.targetWord);
		 
		 for(Object context : contextList.contexts) {	 
			String contextText = (String) context;

				List<List<String>> processedSentences = new ArrayList<>();
				ArrayList<String> sentences = new ArrayList<String>(Arrays.asList(sdetector.sentDetect(contextText))); // split context into sentences using Open NLP
				
				for (String sentence : sentences) {
					if(dbg)System.out.println(sentence);
							String[] tokens = tokenizer.tokenize(sentence);
							sentence = tagNames(tokens, sentence);
						    //sentence = sentence.toLowerCase();
							//sentence = sentence.replaceAll("[\\W]", " "); // replace non-characters and digits with a space
							//sentence = sentence.replaceAll("\\b\\w{1,2}\\b\\s?", ""); //remove words less than 3 characters
							
							tokens = tokenizer.tokenize(sentence);		
							/*
							List<String> newTokens = removeStopWords(tokens, stopWords); //remove stop-words			
							if(newTokens.size() > 4) //save only sentences with length more than 4 words
							processedSentences.add(newTokens);*/
							processedSentences.add(Arrays.asList(tokens));
						} 
				
				List<List<AdornedWord>> taggedSentences = partOfSpeechTagger.tagSentences(processedSentences);	
				List<List<AdornedWord>> stemmedSentences = stemSentences(taggedSentences);
				newContextList.contexts.add(stemmedSentences); // context now is a list of list of AdornedWords
			} 
		 if(dbg) {
			 for(Object context : newContextList.contexts) {
				List<List<AdornedWord>> stemmedContext = (List<List<AdornedWord>>) context;
				 	for(List<AdornedWord> stemmedSentence : stemmedContext) {
				 		for(AdornedWord stemmedWord : stemmedSentence) {
				 			System.out.print(stemmedWord.getLemmata() + "(" + stemmedWord.getPartsOfSpeech() + ")" + " ");	
				 	}
				 	System.out.print(delimer);
				 	}
				 	System.out.println();
			 }
			 System.out.println("\n");
		 }
		 return newContextList;
	 }
		 


	public static void main(String[] args) {
		XMLparser parser = new XMLparser();
		parser.getFileNamesInFolder(new File("SemiEval2010 xml"));
		List<ContextList> allContextLists = parser.parse(parser.files);
		TextProcessor textProcessor = new TextProcessor();
		textProcessor.processContextLists(allContextLists, "stopwords.txt");
	}	
}
