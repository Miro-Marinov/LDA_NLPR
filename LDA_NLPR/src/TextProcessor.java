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
	
	
	PartOfSpeechTagger partOfSpeechTagger;
	Lexicon wordLexicon;
	PartOfSpeechTags partOfSpeechTags;
	
	Lemmatizer lemmatizer;
	SpellingStandardizer standardizer;
	WordTokenizer spellingTokenizer;
	
	
	
	public TextProcessor() {
		
		try {
			System.out.println("Loading trained Open NLP models..");
			FileInputStream modelFileSent = new FileInputStream("models/en-sent.bin");
			FileInputStream modelFileToken = new FileInputStream("models/en-token.bin");
			FileInputStream modelFilePerNames = new FileInputStream("models/en-ner-person.bin");
			FileInputStream modelFileOrgNames = new FileInputStream("models/en-ner-organization.bin");
			FileInputStream modelFileLocNames = new FileInputStream("models/en-ner-location.bin");

			sdetector = new SentenceDetectorME(new SentenceModel(modelFileSent));
			tokenizer = new TokenizerME(new TokenizerModel(modelFileToken));
			
			perNameFinder = new NameFinderME(new TokenNameFinderModel(modelFilePerNames));
			locNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileLocNames));
			orgNameFinder = new NameFinderME(new TokenNameFinderModel(modelFileOrgNames));	
			standardizer = new DefaultSpellingStandardizer();
			
			partOfSpeechTagger = new DefaultPartOfSpeechTagger();
			wordLexicon = partOfSpeechTagger.getLexicon();
			lemmatizer =  new DefaultLemmatizer();
			spellingTokenizer = new PennTreebankTokenizer();
			partOfSpeechTags   =  wordLexicon.getPartOfSpeechTags();
			
			
			
			modelFileSent.close();
			modelFileToken.close();
						
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

	private String[] sentenceDetect(String text)
			throws InvalidFormatException, IOException {

		String sentences[] = sdetector.sentDetect(text);
		return sentences;
	}
	
	private String[] tokenSentence(String sentence)
			throws InvalidFormatException, IOException {
		
		String[] tokens = tokenizer.tokenize(sentence);
		return tokens;
	}
		
	private List<String> removeStopWords(String[] tokens, HashSet<String> stopWords){
		List<String> newTokens = new ArrayList<String>(Arrays.asList(tokens));
		for (String token : tokens){
			if (stopWords.contains(token)){
				newTokens.remove(token);
			}
		}
		return newTokens;
	}
	
	private List<List<AdornedWord>> tagSentences(List<List<String>> sentences){
		return  partOfSpeechTagger.tagSentences( sentences );
	 }
	 
	private void printAndSaveTaggedSentences(List<List<AdornedWord>> taggedSentences, String outputFile){
		StringBuffer strBuff = new StringBuffer();
		File file = new File("output/" + outputFile);

		try {
  
	 			// if file doesn't exists, then create it
	 			if (file.exists()) {
	 				file.delete();
	 			}
	 			file.createNewFile();
	 			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
	 			BufferedWriter bw = new BufferedWriter(fw);
	 			
			for(int i = 0 ; i < taggedSentences.size() ; i++) {
				 List<AdornedWord> sentence = taggedSentences.get(i);
				 strBuff.append(i + ": ");
				 System.out.print(i + ": ");
				 for ( AdornedWord adornedWord : sentence )
		         {
		             StandardSpellSetter.setStandardSpelling(adornedWord, standardizer, partOfSpeechTags);
		             //  Set the lemma.
		             LemmaSetter.setLemma(adornedWord, wordLexicon, lemmatizer, partOfSpeechTags, spellingTokenizer);
		             //  Display the adornments.
		             String adornment = adornedWord.getLemmata() + "(" + adornedWord.getPartsOfSpeech() + ")" + delimer;
		             System.out.print(adornment);
		             strBuff.append(adornment);
		         }
		 			bw.write(strBuff.toString());
		 			
		 			bw.newLine();
		 			System.out.println();
		 			
		 			bw.flush();
		 			strBuff.setLength(0);			
			}
			bw.close();  
	 	} catch (IOException e) {
	 		e.printStackTrace();
	 		}    
		 }

	 public List<List<AdornedWord>> processDocuments(String[] documentFiles, String outputFile, String stopwordsFile) {
		 System.out.println("Preprocessing documents (Punctuation, Names, Stopwords, Spellcheck, PoS, Lemmatizer)..");
		 List<List<AdornedWord>> allTaggedProcessedSentences = new ArrayList<>();
		 
		 for(String documentFile : documentFiles) {	 
			 
			 List<List<String>> processedSentences = new ArrayList<>();
			 ArrayList<String> sentences = new ArrayList<>();
			 HashSet<String> stopWords = new HashSet<>();
			 
			try {
				String documentText = new String(Files.readAllBytes(Paths.get("docs/" + documentFile)), StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(new FileReader("stopwords/" + stopwordsFile));
				String stopword;
				while ((stopword = br.readLine()) != null) {
				   stopWords.add(stopword);
				}
				br.close();
				sentences = new ArrayList<String>(Arrays.asList(sentenceDetect(documentText))); // divide the sentences using openNLP
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				
			for (String sentence : sentences) {
				System.out.println(sentence);
				String[] tokens;
					try {
						tokens = tokenSentence(sentence);
						Span[] nameSpans = perNameFinder.find(tokens);
						Span[] locSpans = locNameFinder.find(tokens);
						Span[] orgSpans = orgNameFinder.find(tokens);
					
						// if person names found - remove
						if(nameSpans.length > 0) {
					    	System.out.println("Found person entities: " + Arrays.toString(Span.spansToStrings(nameSpans, tokens)));
					    	String[] names = Span.spansToStrings(nameSpans, tokens);			    	
					    	for(String name : names)
					    		sentence = StringUtils.replaceFirst(sentence, name, "");
					    }
						
						// if location names found - remove
						if(locSpans.length > 0) {
					    	System.out.println("Found location entities: " + Arrays.toString(Span.spansToStrings(locSpans, tokens)));
					    	String[] names = Span.spansToStrings(locSpans, tokens);			    	
					    	for(String name : names)
					    		sentence = StringUtils.replaceFirst(sentence, name, "");
					    }
						
						// if person names found - remove
						if(orgSpans.length > 0) {
					    	System.out.println("Found organization entities: " + Arrays.toString(Span.spansToStrings(orgSpans, tokens)));
					    	String[] names = Span.spansToStrings(orgSpans, tokens);			    	
					    	for(String name : names)
					    		sentence = StringUtils.replaceFirst(sentence, name, "");
					    }
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				    sentence = sentence.toLowerCase();
					sentence = sentence.replaceAll("[\\W\\d]", " "); // replace non-characters and digits with a space
					sentence = sentence.replaceAll("\\b\\w{1,2}\\b\\s?", ""); //remove words less than 3 characters
					
					try {
						tokens = tokenSentence(sentence);
						List<String> newTokens = removeStopWords(tokens, stopWords); //remove stop-words			
						if(newTokens.size() > 4) //save only sentences with length more than 4 words
							processedSentences.add(newTokens); 		
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			List<List<AdornedWord>> taggedProcessedSentences = tagSentences(processedSentences);	
			allTaggedProcessedSentences.addAll(taggedProcessedSentences);
			
			
		 }
		 System.out.println("\n----------------- Tagged Processed Sentences ----------------\n");
		 printAndSaveTaggedSentences(allTaggedProcessedSentences, outputFile);
		 System.out.println("\n\n");
		 
		 System.out.println("\n\nAll tagged processed sentences written in output/" + outputFile);
		 return allTaggedProcessedSentences;
	}	

	public static void main(String[] args) {

		//String[] parts = args[0].split("<");
		
		TextProcessor textProcessor = new TextProcessor();
		String[] documentFiles = new String[] {"Church Murder.txt", "Starcraft Drama.txt", "Missing Plane.txt", "Five whole fingers.txt"};
		String outputFile = "taggedProcessedSentences.txt";
		textProcessor.processDocuments(documentFiles, outputFile, "stopwords.txt");
	}	
}
