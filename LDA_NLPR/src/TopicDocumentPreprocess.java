import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
 

public class TopicDocumentPreprocess {

	private static final boolean dbg = false;
	public static final int CONTEXT_WINDOW = 5;
	String delimer = " | ";
	
	// OPEN NLP 
	SentenceDetectorME sdetector;
	Tokenizer tokenizer;
	TokenNameFinder perNameFinder;
	TokenNameFinder locNameFinder;
	TokenNameFinder orgNameFinder;
	TokenNameFinder timNameFinder;
	TokenNameFinder datNameFinder;
	TokenNameFinder monNameFinder;
	
	// MorphAdorner
	PartOfSpeechTagger partOfSpeechTagger;
	Lexicon wordLexicon;
	PartOfSpeechTags partOfSpeechTags;
	
	Lemmatizer lemmatizer;
	SpellingStandardizer standardizer;
	WordTokenizer spellingTokenizer;
	
	//Stanford parser
	LexicalizedParser lp;
	WordStemmer ls; 
	TreebankLanguagePack tlp;
    GrammaticalStructureFactory gsf;
	
	HashSet<String> stopWords;
	List<File> files = new ArrayList<>();
	public TopicDocumentPreprocess() {
		
		try {
			if(dbg)System.out.println("Loading trained Open NLP models..");
			
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
			
			
			lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
			ls = new WordStemmer(); // stemmer/lemmatizer object
			
			tlp = new PennTreebankLanguagePack();
		    gsf = tlp.grammaticalStructureFactory();
			
			stopWords = loadStopWords("stopwords.txt");
			
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
			sentence = tagNames(tokens, sentence, perNameFinder,  "#name");
			sentence = tagNames(tokens, sentence, locNameFinder,  "#location");
			sentence = tagNames(tokens, sentence, orgNameFinder,  "#organization");
			sentence = tagNames(tokens, sentence, timNameFinder,  "#time");
			sentence = tagNames(tokens, sentence, datNameFinder,  "#date");
			sentence = tagNames(tokens, sentence, monNameFinder,  "#money");
			
			return sentence;
	}
	
	// tag all name entities
		private String replaceNames(String[] tokens, String sentence) {		
				sentence = tagNames(tokens, sentence, perNameFinder,  "George");
				sentence = tagNames(tokens, sentence, locNameFinder,  "Bulgaria");
				sentence = tagNames(tokens, sentence, orgNameFinder,  "FFA");
				sentence = tagNames(tokens, sentence, timNameFinder,  "morning");
				sentence = tagNames(tokens, sentence, datNameFinder,  "Friday");
				sentence = tagNames(tokens, sentence, monNameFinder,  "$");
				
				return sentence;
		}
	
		//
		public void extractDependencyContexts(File file) {
		    // This option shows loading, sentence-segmenting and tokenizing
		    // a file using DocumentPreprocessor.
			String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			
			try {
				File outFileDependencyContext = new File("SemiEval2010 dependencyContexts/" + targetWord + ".txt");		
				File tempFile = new File("SemiEval2010 dependencyContexts/" + targetWord + "Tmp.txt");	
				
				if (outFileDependencyContext.exists())
					outFileDependencyContext.delete();		
				outFileDependencyContext.createNewFile();
				
			    BufferedWriter bwDependencyContext = new BufferedWriter(new FileWriter(outFileDependencyContext));
			   
		    BufferedReader fileReader = new BufferedReader(new FileReader(file));
		    
		    String line;
			while((line = fileReader.readLine()) != null) {
				System.out.println("New context");
				if (tempFile.exists())
					tempFile.delete();
				tempFile.createNewFile();
				BufferedWriter tmpDependencyContext = new BufferedWriter(new FileWriter(tempFile));
				tmpDependencyContext.append(line);
				tmpDependencyContext.newLine();
				tmpDependencyContext.flush();
			
				for (List<HasWord> sentence : new DocumentPreprocessor(tempFile.getPath())){	
					Tree parse = lp.apply(sentence);
				     
				      //parse.pennPrint();
				      if(dbg)System.out.println();
				      
				      ArrayList<String> words = new ArrayList<>();
					  ArrayList<String> stems = new ArrayList<>();
					  ArrayList<String> tags  = new ArrayList<>();
				      
				      		// Get words and Tags
				   			for (TaggedWord tw : parse.taggedYield()){
				   				words.add(tw.word());
				   				tags.add(tw.tag());
				   			}
				   		 	
				   			// Get stems
				   		    ls.visitTree(parse); // apply the stemmer to the tree
				   			for (TaggedWord tw : parse.taggedYield()){
				   				stems.add(tw.word());
				   			}

				      String dependencyContext = stems.toString();
				      if(dbg)System.out.println(dependencyContext);
				      if(dbg)System.out.println("stems: " + stems);
				      if(dbg)System.out.println("tags: " + tags);
				      dependencyContext = dependencyContext.replaceAll("[-][\\d]+", "");
				      dependencyContext = StringUtils.replaceFirst(dependencyContext, "[", "");
				      dependencyContext = StringUtils.replaceFirst(dependencyContext, "]", "");
				      dependencyContext = dependencyContext.replaceAll(",", "  ");
				      if(dbg)System.out.println(dependencyContext);
				      if(dbg)System.out.println(stems);
				      if(dbg)System.out.println(tags);
				      for(int i = 0 ; i < stems.size() ; i++) {
				    	  if(tags.get(i).equals("SYM")) {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#symbol");
				    	  }
				    	  else if (tags.get(i).equals("UH")) {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#interjection");
				    	  }
				    	  else if (tags.get(i).equals("PR")){
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#particle");
				    	  }
				    	  else if (tags.get(i).equals("DT") || tags.get(i).equals("WDT"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#determiner");
				    	  }
				    	  else if (tags.get(i).equals("PDT")){
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#predeterminer");
				    	  }
				    	  else if (tags.get(i).equals("IN"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#prep");
				    	  }
				    	  else if (tags.get(i).equals("PRP") || tags.get(i).equals("PRP$"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#pronoun");
				    	  }
				    	  else if (tags.get(i).equals("WP") || tags.get(i).equals("WP$"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#whpronoun");
				    	  }
				    	  else if (tags.get(i).equals("JJR") )
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#adjComp");
				    	  }
				    	  else if (tags.get(i).equals("JJS"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#adjSupr");
				    	  }
				    	  else if (tags.get(i).equals("EX"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#existent");
				    	  }
				    	  
				    	  else if (tags.get(i).equals("POS"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#possesEnd");
				    	  }
				    	  
				    	  else if (tags.get(i).equals("PPR"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#personPron");
				    	  }
				    	  else if (tags.get(i).equals("PRP$"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#possesPron");
				    	  }
				    	  else if (tags.get(i).equals("CC$"))
				    	  {
				    		  dependencyContext = dependencyContext.replaceAll(" " + stems.get(i) + " ", "#conjunction");
				    	  }
				      }
				      if(dbg)System.out.println(dependencyContext);
				      dependencyContext = dependencyContext.replaceAll("Bulgaria", "#location");
		    		  
		    		  dependencyContext = dependencyContext.replaceAll("George", "#person");
		    		  
		    		  dependencyContext = dependencyContext.replaceAll("FFA", "#organization");
		    		  
		    		  dependencyContext = dependencyContext.replaceAll("morning", "#time");
		    		  
		    		  dependencyContext = dependencyContext.replaceAll("Friday", "#date");
		    		  
		    		  dependencyContext = dependencyContext.replaceAll("dollars", "#currency");
		    		  if(dbg)System.out.println(dependencyContext);
		    		  dependencyContext = dependencyContext.replaceAll("[^\\w#]", " "); // replace non-(characters or digits) with a space (excluding the #)
		    		  dependencyContext = dependencyContext.replaceAll("[\\d]", " "); //replace digits with a space
		    		  dependencyContext = dependencyContext.replaceAll("\\s{2,}", " "); //replace digits with a space
				      
				      if(dbg)System.out.println(dependencyContext);
				      
				      bwDependencyContext.write(dependencyContext);
				      bwDependencyContext.write(" ");
				      bwDependencyContext.flush();
			}
				if(dbg)System.out.println("New Document!\n");
				bwDependencyContext.newLine();        
				bwDependencyContext.flush();
				tmpDependencyContext.close();
		  }
			bwDependencyContext.close();
			
			fileReader.close();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	 	 
	 // process all paragraphs in a file to extract processed classical contexts (sentences contexts and window contexts comprising of just words)
	 public void extractClassicalContexts(File file) {
		 if(dbg)System.out.println("Preprocessing documents (Punctuation, Names, Stopwords, Spellcheck, PoS, Lemmatizer)..");
		 try {
			 
			 
			 String targetWord = file.getName().substring(0, file.getName().indexOf("."));
			 BufferedReader brParagraphs = new BufferedReader(new FileReader(file));
			 	 
			 File outFileSentenceContext = new File("SemiEval2010 sentenceContexts/" + targetWord + ".txt");		
				if (outFileSentenceContext.exists()) {
					outFileSentenceContext.delete();
	 			}
				
			 outFileSentenceContext.createNewFile();
			 BufferedWriter bwSentenceContext = new BufferedWriter(new FileWriter(outFileSentenceContext));
			 
			 File outFileSentenceTagged = new File("SemiEval2010 rawSentencesTagged/" + targetWord + ".txt");		
				if (outFileSentenceTagged.exists()) {
					outFileSentenceTagged.delete();
	 			}
				
			 outFileSentenceTagged.createNewFile();
			 BufferedWriter bwSentenceTagged = new BufferedWriter(new FileWriter(outFileSentenceTagged));
			 
	 
			 File outFileWindowContext = new File("SemiEval2010 windowContexts/" + targetWord + ".txt");		
				if (outFileWindowContext.exists()) {
					outFileWindowContext.delete();
	 			}
				
			 outFileWindowContext.createNewFile();
			 BufferedWriter bwWindowContext = new BufferedWriter(new FileWriter(outFileWindowContext));

			 
			 String paragraph;
			 while((paragraph = brParagraphs.readLine()) != null) {	// for each context
			 ArrayList<String> contextSentences = new ArrayList<String>(Arrays.asList(sdetector.sentDetect(paragraph))); // split context into sentences using Open NLP
			 	
			 	
			    StringBuffer contextParagraph = new StringBuffer();	
				for (String sentence : contextSentences) { // for each sentence
							
					
					List<List<String>> newContextSentences = new ArrayList<>();
					String[] tokens = tokenizer.tokenize(sentence);
					
					/* tag name entities */
					String replacedNamesSentence = replaceNames(tokens, sentence);
					
					
					bwSentenceTagged.write(replacedNamesSentence);
					bwSentenceTagged.write(" ");
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
					
						String[] tokensSent = tokenizer.tokenize(sentenceToWrite);
						for(String token : tokensSent)
							bwSentenceContext.write(token + " ");
						bwSentenceContext.write(" ");
										
					contextParagraph.append(sentenceToWrite);		
					//if(dbg)System.out.println();	
					if(dbg)System.out.print("...");
				}
				bwSentenceTagged.newLine();
				bwSentenceTagged.flush();
				
				bwSentenceContext.newLine();
				bwSentenceContext.flush();			
			//if(dbg)System.out.println(contextParagraph.toString());		
		   
			// Extract window around the word as context and save it in the appropriate /folder/file	
			String[] tokensParagraph = tokenizer.tokenize(contextParagraph.toString());

			for(int i = 0 ; i < tokensParagraph.length ; i ++) {
					if(tokensParagraph[i].equals(targetWord)) {
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
		     brParagraphs.close();
		     bwSentenceTagged.close();
		     

		     
		 	} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	 }

		public void getFileNamesInFolder(final File folder) {
		    for (final File fileEntry : folder.listFiles()) {
		        if (fileEntry.isDirectory()) {
		        	getFileNamesInFolder(fileEntry);
		        } else {
		            files.add(fileEntry);
		        }
		    }
		}	 
	 


	public static void main(String[] args) {
		TopicDocumentPreprocess textProcessor = new TopicDocumentPreprocess();
		textProcessor.extractClassicalContexts(new File("SemiEval2010 txt/testLDABig.txt"));
		textProcessor.extractDependencyContexts(new File("SemiEval2010 rawSentencesTagged/testLDABig.txt"));

		
	}	
	
	
}
