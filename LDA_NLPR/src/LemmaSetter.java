import java.util.List;

import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;
import edu.northwestern.at.morphadorner.corpuslinguistics.lemmatizer.Lemmatizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.lexicon.Lexicon;
import edu.northwestern.at.morphadorner.corpuslinguistics.partsofspeech.PartOfSpeechTags;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.WordTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringUtil;

public class LemmaSetter {
	/** Get lemma for a word.
    *
    *  @param  adornedWord         The adorned word.
    *  @param  lexicon             The word lexicon.
    *  @param  lemmatizer          The lemmatizer.
    *  @param  partOfSpeechTags    The part of speech tags.
    *  @param  spellingTokenizer   Tokenizer for spelling.
    *
    *  On output, sets the lemma field of the adorned word
    *  We look in the word lexicon first for the lemma.
    *  If the lexicon does not contain the lemma, we
    *  use the lemmatizer.
    */
   
	
	public static void setLemma
   (
       AdornedWord adornedWord  ,
       Lexicon lexicon ,
       Lemmatizer lemmatizer ,
       PartOfSpeechTags partOfSpeechTags ,
       WordTokenizer spellingTokenizer
   )
   {
       String spelling     = adornedWord.getSpelling();
       String partOfSpeech = adornedWord.getPartsOfSpeech();
       String lemmata      = spelling;
                               //  Get lemmatization word class
                               //  for part of speech.
       String lemmaClass   =
           partOfSpeechTags.getLemmaWordClass( partOfSpeech );
                               //  Do not lemmatize words which
                               //  should not be lemmatized,
                               //  including proper names.
       if  (   lemmatizer.cantLemmatize( spelling ) ||
               lemmaClass.equals( "none" )
           )
       {
       }
       else
       {
                               //  Try compound word exceptions
                               //  list first.
           lemmata = lemmatizer.lemmatize( spelling , "compound" );
                               //  If lemma not found, keep trying.
           if ( lemmata.equals( spelling ) )
           {
                               //  Extract individual word parts.
                               //  May be more than one for a
                               //  contraction.
               List<String> wordList   =
                   spellingTokenizer.extractWords( spelling );
                               //  If just one word part,
                               //  get its lemma.
               if  (   !partOfSpeechTags.isCompoundTag( partOfSpeech ) ||
                       ( wordList.size() == 1 )
                   )
               {
                   if ( lemmaClass.length() == 0 )
                   {
                       lemmata = lemmatizer.lemmatize( spelling );
                   }
                   else
                   {
                       lemmata =
                           lemmatizer.lemmatize( spelling , lemmaClass );
                   }
               }
                               //  More than one word part.
                               //  Get lemma for each part and
                               //  concatenate them with the
                               //  lemma separator to form a
                               //  compound lemma.
               else
               {
                   lemmata             = "";
                   String lemmaPiece   = "";
                   String[] posTags    = partOfSpeechTags.splitTag( partOfSpeech );
                   String lemmaSeparator = "|";
                   if ( posTags.length == wordList.size() )
                   {
                       for ( int i = 0 ; i < wordList.size() ; i++ )
                       {
                           String wordPiece    = (String)wordList.get( i );
                           if ( i > 0 )
                           {
                               lemmata = lemmata + lemmaSeparator;
                           }
                           lemmaClass  =
                               partOfSpeechTags.getLemmaWordClass
                               (
                                   posTags[ i ]
                               );
                           lemmaPiece  =
                               lemmatizer.lemmatize
                               (
                                   wordPiece ,
                                   lemmaClass
                               );
                           lemmata = lemmata + lemmaPiece;
                       }
                   }
               }
           }
       }
       adornedWord.setLemmata( lemmata );
   }
}

