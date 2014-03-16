import edu.northwestern.at.morphadorner.corpuslinguistics.adornedword.AdornedWord;
import edu.northwestern.at.morphadorner.corpuslinguistics.partsofspeech.PartOfSpeechTags;
import edu.northwestern.at.morphadorner.corpuslinguistics.spellingstandardizer.SpellingStandardizer;
import edu.northwestern.at.utils.CharUtils;


public class StandardSpellSetter {
	/** Get standard spelling for a word.
    *
    *  @param  adornedWord     The adorned word.
    *  @param  standardizer        The spelling standardizer.
    *  @param  partOfSpeechTags    The part of speech tags.
    *
    *  On output, sets the standard spelling field of the adorned word
    */
   public static void setStandardSpelling
   (
       AdornedWord adornedWord  ,
       SpellingStandardizer standardizer ,
       PartOfSpeechTags partOfSpeechTags
   )
   {
                               //  Get the spelling.
       String spelling         = adornedWord.getSpelling();
       String standardSpelling = spelling;
       String partOfSpeech     = adornedWord.getPartsOfSpeech();
                               //  Leave proper nouns alone.
       if ( partOfSpeechTags.isProperNounTag( partOfSpeech ) )
       {
       }
                               //  Leave nouns with internal
                               //  capitals alone.
       else if (   partOfSpeechTags.isNounTag( partOfSpeech )  &&
                   CharUtils.hasInternalCaps( spelling ) )
       {
       }
                               //  Leave foreign words alone.
       else if ( partOfSpeechTags.isForeignWordTag( partOfSpeech ) )
       {
       }
                               //  Leave numbers alone.
       else if ( partOfSpeechTags.isNumberTag( partOfSpeech ) )
       {
       }
                               //  Anything else -- call the
                               //  standardizer on the spelling
                               //  to get the standard spelling.
       else
       {
           standardSpelling    =
               standardizer.standardizeSpelling
               (
                   adornedWord.getSpelling() ,
                   partOfSpeechTags.getMajorWordClass
                   (
                       adornedWord.getPartsOfSpeech()
                   )
               );
                               //  If the standard spelling
                               //  is the same as the original
                               //  spelling except for case,
                               //  use the original spelling.
           if ( standardSpelling.equalsIgnoreCase( spelling ) )
           {
               standardSpelling    = spelling;
           }
       }
                               //  Set the standard spelling.
       adornedWord.setStandardSpelling( standardSpelling );
   }
}
