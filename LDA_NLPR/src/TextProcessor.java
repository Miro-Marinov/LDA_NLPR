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
	public static final int CONTEXT_WINDOW = 5;
	
	
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
	//remove stop-words from String sentence
	private String removeStopWords(String sentence, HashSet<String> stopWords) {
		List<String> removedTokens = removeStopWords(tokenizer.tokenize(sentence), stopWords);
		StringBuffer returnSentence = new StringBuffer();
		for (String token : removedTokens){
			returnSentence.append(" " + token);
			}
		
		return returnSentence.toString();
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
	private List<String> getNames(String[] tokens, String sentence, TokenNameFinder nameFinder) {
		Span[] nameSpans = nameFinder.find(tokens);
		if(nameSpans.length > 0) {
	    	String[] names = Span.spansToStrings(nameSpans, tokens);	
	    	List<String> namesList = Arrays.asList(names);
	    	return namesList;	
	    }
		return null;
	}
	
	
	// tag a name entity with a specified replacement
	private String tagNames(String[] tokens, String sentence, TokenNameFinder nameFinder, String replacement) {
		Span[] nameSpans = nameFinder.find(tokens);

		if(nameSpans.length > 0) {
			System.out.println("Repace name entities: " + Arrays.toString(Span.spansToStrings(nameSpans, tokens)) + " with: " + replacement);
	    	String[] names = Span.spansToStrings(nameSpans, tokens);	
	    	for(String name : names)
	    		sentence = StringUtils.replaceFirst(sentence, name, replacement);
	    }
		return sentence;
	}
	// tag all name entities
	private String tagNames(String[] tokens, String sentence) {		
			sentence = tagNames(tokens, sentence, perNameFinder,  "#name");
			sentence = tagNames(tokens, sentence, locNameFinder,  "#location");
			sentence = tagNames(tokens, sentence, orgNameFinder,  "#organization");
			sentence = tagNames(tokens, sentence, timNameFinder,  "#time");
			sentence = tagNames(tokens, sentence, datNameFinder,  "#date");
			sentence = tagNames(tokens, sentence, monNameFinder,  "#money");
			
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
		 HashSet<String> stopWords = loadStopWords(stopwordsFile);
		 try {
			 
			 File outFileSentenceContext = new File("SemiEval2010 sentenceContexts/" + contextList.targetWord + ".txt");		
				if (outFileSentenceContext.exists()) {
					outFileSentenceContext.delete();
	 			}
				
			 outFileSentenceContext.createNewFile();
			 BufferedWriter bwSentenceContext = new BufferedWriter(new FileWriter(outFileSentenceContext));
			 bwSentenceContext.write(contextList.targetWord);
			 bwSentenceContext.newLine();
			 bwSentenceContext.flush();
			 
			 
			 File outFileWindowContext = new File("SemiEval2010 windowContexts/" + contextList.targetWord + ".txt");		
				if (outFileWindowContext.exists()) {
					outFileWindowContext.delete();
	 			}
				
			 outFileWindowContext.createNewFile();
			 BufferedWriter bwWindowContext = new BufferedWriter(new FileWriter(outFileWindowContext));
			 bwWindowContext.write(contextList.targetWord);
			 bwWindowContext.newLine();
			 bwWindowContext.flush();
			 
			 
			 for(String context : contextList.contexts) {	 // for each context
			 ArrayList<String> contextSentences = new ArrayList<String>(Arrays.asList(sdetector.sentDetect(context))); // split context into sentences using Open NLP
				
			 	StringBuffer contextParagraph = new StringBuffer();	
				for (String sentence : contextSentences) { // for each sentence
							//if(dbg)System.out.print(sentence + ".");
							List<List<String>> newContextSentences = new ArrayList<>();
							String[] tokens = tokenizer.tokenize(sentence);
					
							//sentence = sentence.toLowerCase();
							//sentence = sentence.replaceAll("[\\W]", " "); // replace non-characters and digits with a space
							//sentence = sentence.replaceAll("\\b\\w{1,2}\\b\\s?", ""); //remove words less than 3 characters
								
							/*
							List<String> newTokens = removeStopWords(tokens, stopWords); //remove stop-words			
							if(newTokens.size() > 4) //save only sentences with length more than 4 words
							processedSentences.add(newTokens);*/
							
							newContextSentences.add(Arrays.asList(tokens));
							List<List<AdornedWord>> taggedSentences = partOfSpeechTagger.tagSentences(newContextSentences);	
							List<List<AdornedWord>> stemmedSentences = stemSentences(taggedSentences);
							StringBuffer newSentence = new StringBuffer();
							for(List<AdornedWord> stemmedSentence : stemmedSentences) {	
								for(AdornedWord word: stemmedSentence) {
									//System.out.print(word.getToken() + " ");
									//if(dbg)System.out.print(word.getToken() + "|" + word.getLemmata() + "(" + word.getPartsOfSpeech()+ ")" + " ");	
									newSentence.append(word.getLemmata() + " ");	
								}
							} 
							
							String sentenceToWrite = newSentence.toString();
							String[] tokensAgain = tokenizer.tokenize(sentenceToWrite);
							
							sentenceToWrite = tagNames(tokensAgain, sentenceToWrite);
							sentenceToWrite = sentenceToWrite.replaceAll("[^\\w#]", " "); // replace non-(characters or digits) with a space (excluding the #)
							sentenceToWrite = sentenceToWrite.replaceAll("[\\d]", " "); //replace digits with a space
							
							//sentenceToWrite = removeStopWords(sentenceToWrite, stopWords);
							//sentenceToWrite = sentenceToWrite.replaceAll("\\b\\w{1,2}\\b\\s?", ""); //remove words less than 3 characters
							
							sentenceToWrite = sentenceToWrite.replaceAll("\\b\\w{1}\\b\\s?", ""); //remove words less than 2 characters
							sentenceToWrite = sentenceToWrite.toLowerCase();
							
							// Extract sentence as context and save it in the appropriate /folder/file
							if(sentenceToWrite.contains(contextList.targetWord)) {
								bwSentenceContext.write(sentenceToWrite);
								bwSentenceContext.newLine();
								bwSentenceContext.flush();
							}
							
							contextParagraph.append(sentenceToWrite);		
							//System.out.println();	
							if(dbg)System.out.print("...");
				}
							
			//System.out.println(contextParagraph.toString());		
		   
			// Extract window around the word as context and save it in the appropriate /folder/file	
			
			String[] tokensParagraph = tokenizer.tokenize(contextParagraph.toString());

			for(int i = 0 ; i < tokensParagraph.length ; i ++) {
					if(tokensParagraph[i].equals(contextList.targetWord)) {
						StringBuffer contextWindow = new StringBuffer();
						int num = 0;
						for(int l = i - 1, r = i + 1 ; num <= CONTEXT_WINDOW*2 ; ) {
							if(l >= 0 && num <= CONTEXT_WINDOW*2){contextWindow.append(tokensParagraph[l] + " "); num++; l --;}
							if(r < tokensParagraph.length && num <= CONTEXT_WINDOW*2){contextWindow.append(tokensParagraph[r] + " "); num ++; r++;}
							if( (l < 0 && r >= tokensParagraph.length) || num >= CONTEXT_WINDOW*2 ) break;	
						}
						
						bwWindowContext.write(contextWindow.toString());
						bwWindowContext.newLine();
						bwWindowContext.flush();
				}
			}  
			
		 }
		     bwSentenceContext.close();
		     bwWindowContext.close();
		 	} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 return newContextList;
	 }
		 


	public static void main(String[] args) {
		XMLparser parser = new XMLparser();
		parser.getFileNamesInFolder(new File("SemiEval2010 xml"));
		List<ContextList> contextsLists = parser.parse(parser.files);
		TextProcessor textProcessor = new TextProcessor();
		for(ContextList contextList: contextsLists)
			textProcessor.processContextList(contextList, "stopwords.txt");
	}	
}
