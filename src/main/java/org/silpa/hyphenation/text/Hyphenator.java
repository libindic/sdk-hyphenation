/* $Id: Hyphenator.java,v 1.17 2003/08/25 08:41:28 dvd Exp $ */

package org.silpa.hyphenation.text;

import android.content.Context;

import org.silpa.guesslanguage.GuessLanguage;
import org.silpa.hyphenation.R;
import org.silpa.hyphenation.text.Utf8TexParser.TexParserException;
import org.silpa.hyphenation.util.ErrorHandler;
import org.silpa.hyphenation.util.List;
import org.silpa.hyphenation.util.LoggingErrorHandler;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * insert soft hyphens at all allowed locations uses TeX hyphenation tables
 */
public class Hyphenator {

    //Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
    public static final char HYPHEN = '\u2010';
    public static final char HYPHEN_MINUS = '\u002d';
    public static final char SOFT_HYPHEN = '\u00ad';
    public static final char NON_BREAKING_HYPHEN = '\u2011';
    private static final char ZERO_WIDTH_SPACE = '\u200b';

    private final ForwardingErrorHandler errorHandler;
    private RuleDefinition ruleSet;
    private final ByteScanner b;

    // Guess Language
    private GuessLanguage guessLanguage;
    private Context mContext;
    private static Map<String, String> indicHyphenRules = new HashMap<>();

    static {
        indicHyphenRules.put("as", "\\patterns{\n2‍2\n1‌1\nঅ1\nআ1\nই1\nঈ1\nউ1\nঊ1\nঋ1\nৠ1\nঌ1\nৡ1\nএ1\nঐ1\nও1\nঔ1\nা1\nি1\nী1\nু1\nূ1\nৃ1\nৄ1\nৢ1\nৣ1\nে1\nৈ1\nো1\nৌ1\n2়2\nৗ1\n1ক\n1খ\n1গ\n1ঘ\n1ঙ\n1চ\n1ছ\n1জ\n1ঝ\n1ঞ\n1ট\n1ঠ\n1ড\n1ড়\n1ঢ\n1ঢ়\n1ণ\n1ত\n1থ\n1দ\n1ধ\n1ন\n1প\n1ফ\n1ব\n1ভ\n1ম\n1য\n1য়\n1র\n1ল\n1শ\n1ষ\n1স\n1হ\nৎ1\n2ঃ1\n2ং1\n2ঁ1\n2ঽ1\n2্2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("bn", "\\patterns{\n2‍2\n1‌1\nঅ1\nআ1\nই1\nঈ1\nউ1\nঊ1\nঋ1\nৠ1\nঌ1\nৡ1\nএ1\nঐ1\nও1\nঔ1\nা1\nি1\nী1\nু1\nূ1\nৃ1\nৄ1\nৢ1\nৣ1\nে1\nৈ1\nো1\nৌ1\n2়2\nৗ1\n1ক\n1খ\n1গ\n1ঘ\n1ঙ\n1চ\n1ছ\n1জ\n1ঝ\n1ঞ\n1ট\n1ঠ\n1ড\n1ড়\n1ঢ\n1ঢ়\n1ণ\n1ত\n1থ\n1দ\n1ধ\n1ন\n1প\n1ফ\n1ব\n1ভ\n1ম\n1য\n1য়\n1র\n1ল\n1শ\n1ষ\n1স\n1হ\nৎ1\n2ঃ1\n2ং1\n2ঁ1\n2ঽ1\n2্2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("gu", "\\patterns{\n2‍2\n1‌1\nઅ1\nઆ1\nઇ1\nઈ1\nઉ1\nઊ1\nઋ1\nૠ1\nએ1\nઐ1\nઓ1\nઔ1\nા1\nિ1\nી1\nુ1\nૂ1\nૃ1\nૄ1\nૢ1\nૣ1\nે1\nૈ1\nો1\nૌ1\n1ક\n1ખ\n1ગ\n1ઘ\n1ઙ\n1ચ\n1છ\n1જ\n1ઝ\n1ઞ\n1ટ\n1ઠ\n1ડ\n1ઢ\n1ણ\n1ત\n1થ\n1દ\n1ધ\n1ન\n1પ\n1ફ\n1બ\n1ભ\n1મ\n1ય\n1ર\n1લ\n1ળ\n1વ\n1શ\n1ષ\n1સ\n1હ\n2ઁ1\n2ઃ1\n2ઽ1\n2્2\n2ં2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("hi", "\\patterns{\n2‍2\n1‌1\nअ1\nआ1\nइ1\nई1\nउ1\nऊ1\nऋ1\nॠ1\nऌ1\nॡ1\nए1\nऐ1\nओ1\nऔ1\nा1\nि1\nी1\nु1\nू1\nृ1\nॄ1\nॢ1\nॣ1\nे1\nै1\nो1\nौ1\n1क\n1ख\n1ग\n1घ\n1ङ\n1च\n1छ\n1ज\n1झ\n1ञ\n1ट\n1ठ\n1ड\n1ढ\n1ण\n1त\n1थ\n1द\n1ध\n1न\n1प\n1फ\n1ब\n1भ\n1म\n1य\n1र\n1ल\n1ळ\n1व\n1श\n1ष\n1स\n1ह\n2ँ1\n2ं1\n2ः1\n2ऽ1\n2॑1\n2॒1\n2्2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("kn", "\\patterns{\n2‍2\n1‌1\nಅ1\nಆ1\nಇ1\nಈ1\nಉ1\nಊ1\nಋ1\nೠ1\nಌ1\nೡ1\nಎ1\nಏ1\nಐ1\nಒ1\nಓ1\nಔ1\nಾ1\nಿ1\nೀ1\nು1\nೂ1\nೃ1\nೄ1\nೆ1\nೇ1\nೈ1\nೊ1\nೋ1\nೌ1\n1ಕ\n1ಖ\n1ಗ\n1ಘ\n1ಙ\n1ಚ\n1ಛ\n1ಜ\n1ಝ\n1ಞ\n1ಟ\n1ಠ\n1ಡ\n1ಢ\n1ಣ\n1ತ\n1ಥ\n1ದ\n1ಧ\n1ನ\n1ಪ\n1ಫ\n1ಬ\n1ಭ\n1ಮ\n1ಯ\n1ರ\n1ಱ\n1ಲ\n1ಳ\n1ೞ\n1ವ\n1ಶ\n1ಷ\n1ಸ\n1ಹ\n2ಂ1\n2ಃ1\n2ಽ1\n2ೕ1\n2ೖ1\n2್2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("ml", "\\patterns{\n2‍2\n1‌1\n1അ1\n1ആ1\n1ഇ1\n1ഈ1\n1ഉ1\n1ഊ1\n1ഋ1\n1ൠ1\n1ഌ1\n1ൡ1\n1എ1\n1ഏ1\n1ഐ1\n1ഒ1\n1ഓ1\n1ഔ1\nാ1\nി1\nീ1\nു1\nൂ1\nൃ1\nെ1\nേ1\nൈ1\nൊ1\nോ1\nൌ1\nൗ1\n1ക\n1ഖ\n1ഗ\n1ഘ\n1ങ\n1ച\n1ഛ\n1ജ\n1ഝ\n1ഞ\n1ട\n1ഠ\n1ഡ\n1ഢ\n1ണ\n1ത\n1ഥ\n1ദ\n1ധ\n1ന\n1പ\n1ഫ\n1ബ\n1ഭ\n1മ\n1യ\n1ര\n1റ\n1ല\n1ള\n1ഴ\n1വ\n1ശ\n1ഷ\n1സ\n1ഹ\n2ഃ1\n2ം1\n2്2\nന്2\nര്2\nള്2\nല്2\nക്2\nണ്2\n2ന്‍\n2ല്‍\n2ള്‍\n2ണ്‍\n2ര്‍\n2ക്‍\n2ൺ\n2ൻ\n2ർ\n2ൽ\n2ൾ\n2ൿ\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("mr", "\\patterns{\n2‍2\n1‌1\nअ1\nआ1\nइ1\nई1\nउ1\nऊ1\nऋ1\nॠ1\nऌ1\nॡ1\nए1\nऐ1\nओ1\nऔ1\nा1\nि1\nी1\nु1\nू1\nृ1\nॄ1\nॢ1\nॣ1\nे1\nै1\nो1\nौ1\n1क\n1ख\n1ग\n1घ\n1ङ\n1च\n1छ\n1ज\n1झ\n1ञ\n1ट\n1ठ\n1ड\n1ढ\n1ण\n1त\n1थ\n1द\n1ध\n1न\n1प\n1फ\n1ब\n1भ\n1म\n1य\n1र\n1ल\n1ळ\n1व\n1श\n1ष\n1स\n1ह\n2ँ1\n2ं1\n2ः1\n2ऽ1\n2॑1\n2॒1\n2्2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("or", "\\patterns{\n2‍2\n1‌1\nଅ1\nଆ1\nଇ1\nଈ1\nଉ1\nଊ1\nଋ1\nୠ1\nଌ1\nୡ1\nଏ1\nଐ1\nଓ1\nଔ1\nା1\nି1\nୀ1\nୁ1\nୂ1\nୃ1\nେ1\nୈ1\nୋ1\nୌ1\n1କ\n1ଖ\n1ଗ\n1ଘ\n1ଙ\n1ଚ\n1ଛ\n1ଜ\n1ଝ\n1ଞ\n1ଟ\n1ଠ\n1ଡ\n1ଢ\n1ଣ\n1ତ\n1ଥ\n1ଦ\n1ଧ\n1ନ\n1ପ\n1ଫ\n1ବ\n1ଭ\n1ମ\n1ଯ\n1ର\n1ଲ\n1ଳ\n1ଵ\n1ଶ\n1ଷ\n1ସ\n1ହ\n2ଂ1\n2ଃ1\n2ୗ1\n2ଁ1\n2୍2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("pa", "\\patterns{\n2‍2\n1‌1\nਅ1\nਆ1\nਇ1\nਈ1\nਉ1\nਊ1\nਏ1\nਐ1\nਓ1\nਔ1\nਾ1\nਿ1\nੀ1\nੁ1\nੂ1\nੇ1\nੈ1\nੋ1\nੌ1\n1ਕ\n1ਖ\n1ਗ\n1ਘ\n1ਙ\n1ਚ\n1ਛ\n1ਜ\n1ਝ\n1ਞ\n1ਟ\n1ਠ\n1ਡ\n1ਢ\n1ਣ\n1ਤ\n1ਥ\n1ਦ\n1ਧ\n1ਨ\n1ਪ\n1ਫ\n1ਬ\n1ਭ\n1ਮ\n1ਯ\n1ਰ\n1ਲ\n1ਲ਼\n1ਵ\n1ਸ਼\n1ਸ\n1ਹ\n2ਁ1\n2ਂ1\n2ਃ1\n2੍2\n2ੰ2\n2ੱ2\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("ta", "\\patterns{\n2‍2\n1‌1\n1அ1\n1ஆ1\n1இ1\n1ஈ1\n1உ1\n1ஊ1\n1எ1\n1ஏ1\n1ஐ1\n1ஒ1\n1ஓ1\n1ஔ1\nா1\nி1\nீ1\nு1\nூ1\nெ1\nே1\nை1\nொ1\nோ1\nௌ1\n1க\n1ங\n1ச\n1ஜ\n1ஞ\n1ட\n1ண\n1த\n1ந\n1ப\n1ம\n1ய\n1ர\n1ற\n1ல\n1ள\n1ழ\n1வ\n1ஷ\n1ஸ\n1ஹ\n2க்1\n2ங்1\n2ச்1\n2ஞ்1\n2ட்1\n2ண்1\n2த்1\n2ன்1\n2ந்1\n2ப்1\n2ம்1\n2ய்1\n2ர்1\n2ற்1\n2ல்1\n2ள்1\n2ழ்1\n2வ்1\n2ஷ்1\n2ஸ்1\n2ஹ்1\n2ஂ1\n2ஃ1\n2ௗ1\n2்1\n}\n\\hyphenation{\n}");
        indicHyphenRules.put("te", "\\patterns{\n2‍2\n1‌1\nఅ1\nఆ1\nఇ1\nఈ1\nఉ1\nఊ1\nఋ1\nౠ1\nఌ1\nౡ1\nఎ1\nఏ1\nఐ1\nఒ1\nఓ1\nఔ1\nా1\nి1\nీ1\nు1\nూ1\nృ1\nౄ1\nె1\nే1\nై1\nొ1\nో1\nౌ1\n1క\n1ఖ\n1గ\n1ఘ\n1ఙ\n1చ\n1ఛ\n1జ\n1ఝ\n1ఞ\n1ట\n1ఠ\n1డ\n1ఢ\n1ణ\n1త\n1థ\n1ద\n1ధ\n1న\n1ప\n1ఫ\n1బ\n1భ\n1మ\n1య\n1ర\n1ఱ\n1ల\n1ళ\n1వ\n1శ\n1ష\n1స\n1హ\n2ఁ1\n2ం1\n2ః1\n2ౕ1\n2ౖ1\n2్2\n}\n\\hyphenation{\n}");
    }

    /**
     * Constructor
     * Creates an uninitialized instance of Hyphenator. The same instance can be
     * reused for different hyphenation tables.
     *
     * @param context context of application
     */
    public Hyphenator(Context context) {
        errorHandler = new ForwardingErrorHandler(new LoggingErrorHandler(Logger.getLogger(this.getClass().getCanonicalName())));
        b = new ByteScanner(errorHandler);
        this.mContext = context;
        this.guessLanguage = new GuessLanguage(this.mContext);
    }

    public RuleDefinition getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(RuleDefinition scanner) {
        this.ruleSet = scanner;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler.getTarget();
    }

    /**
     * installs error handler.
     *
     * @param eh ErrorHandler used while parsing and hyphenating
     * @see org.silpa.hyphenation.util.ErrorHandler
     */
    public void setErrorHandler(ErrorHandler eh) {
        errorHandler.setTarget(eh);
    }

    /**
     * <p>Loads a hyphenation table with a reader. This enables the use of UTF-8 pattern files.
     * Note that escape codes in the original tex-files are not supported, e.g. ^^f6.
     * This method also differs in that multiple calls to loadTable are not joined, only the
     * most recent pattern file is used.</p>
     * <p/>
     * <p>Only "\pattern{" and "\hyphenation{" groups are supported.</p>
     *
     * @param reader a reader containing hyphenation patterns (most likely a file)
     * @throws TexParserException if there are problems reading the input
     */
    public void loadTable(Reader reader) throws TexParserException {
        Utf8TexParser parser = new Utf8TexParser();
        ruleSet = parser.parse(reader);
    }

    /**
     * loads hyphenation table
     *
     * @param in hyphenation table
     * @throws java.io.IOException
     */
    public void loadTable(java.io.InputStream in) throws java.io.IOException {
        int[] codelist = new int[256];
        {
            for (int i = 0; i != 256; ++i)
                codelist[i] = i;
        }
        loadTable(in, codelist);
    }

    /**
     * loads hyphenation table and code list for non-ucs encoding
     *
     * @param in       hyphenation table
     * @param codelist an array of 256 elements. maps one-byte codes to UTF codes
     * @throws java.io.IOException
     */
    public void loadTable(java.io.InputStream in, int[] codelist)
            throws java.io.IOException {
        b.scan(in, codelist);
        ruleSet = b;
    }

    /**
     * performs hyphenation
     *
     * @param phrase string to hyphenate
     * @return the string with soft hyphens inserted
     */
    public String hyphenate(String phrase) {
        return hyphenate(phrase, 1, 1);
    }

    /**
     * performs hyphenation
     *
     * @param phrase         string to hyphenate
     * @param leftHyphenMin  unbreakable characters at the beginning of each word in the
     *                       phrase
     * @param rightHyphenMin unbreakable characters at the end of each word in the phrase
     * @return the string with soft hyphens inserted
     */
    public String hyphenate(String phrase, int leftHyphenMin, int rightHyphenMin) {

        // Check input
        leftHyphenMin = Math.max(leftHyphenMin, 1);
        rightHyphenMin = Math.max(rightHyphenMin, 1);

        // Ignore short phrases (early out)
        if (phrase.length() < rightHyphenMin + leftHyphenMin) {
            return phrase;
        }

        int processedOffset = Integer.MIN_VALUE;
        int ich = 0;
        char[] sourcePhraseChars = new char[phrase.length() + 1];
        sourcePhraseChars[sourcePhraseChars.length - 1] = (char) 0;
        phrase.getChars(0, phrase.length(), sourcePhraseChars, 0);


        char[] hyphenatedPhraseChars = new char[sourcePhraseChars.length * 2 - 1];
        int ihy = 0;

        boolean inword = false;
        while (true) {
            if (inword) {
                if (Character.isLetter(sourcePhraseChars[ich])) {
                    ich++;
                } else { // last character will be reprocessed in the other
                    // state
                    int length = ich - processedOffset;
                    String word = new String(sourcePhraseChars, processedOffset, length).toLowerCase();
                    int[] hyphenQualificationPoints = ruleSet
                            .getException(word);

                    if (hyphenQualificationPoints == null) {
                        char[] wordChars = extractWord(sourcePhraseChars, processedOffset, length);
                        hyphenQualificationPoints = applyHyphenationRules(
                                wordChars, length);
                    }

                    // now inserting soft hyphens
                    if (leftHyphenMin + rightHyphenMin <= length) {
                        for (int i = 0; i < leftHyphenMin - 1; i++) {
                            hyphenatedPhraseChars[ihy++] = sourcePhraseChars[processedOffset++];
                        }

                        for (int i = leftHyphenMin - 1; i < length
                                - rightHyphenMin; i++) {
                            hyphenatedPhraseChars[ihy++] = sourcePhraseChars[processedOffset++];
                            if (hyphenQualificationPoints[i] % 2 == 1)
                                hyphenatedPhraseChars[ihy++] = SOFT_HYPHEN;
                        }

                        for (int i = length - rightHyphenMin; i < length; i++) {
                            hyphenatedPhraseChars[ihy++] = sourcePhraseChars[processedOffset++];
                        }
                    } else {
                        //Word is to short to hyphenate, so just copy
                        for (int i = 0; i != length; ++i) {
                            hyphenatedPhraseChars[ihy++] = sourcePhraseChars[processedOffset++];
                        }
                    }
                    inword = false;
                }
            } else {
                if (Character.isLetter(sourcePhraseChars[ich])) {
                    processedOffset = ich;
                    inword = true; // processedOffset remembers the start of the word
                } else {
                    if (sourcePhraseChars[ich] == (char) 0)
                        break; // zero is a guard inserted earlier
                    hyphenatedPhraseChars[ihy++] = sourcePhraseChars[ich];
                    if (sourcePhraseChars[ich] == HYPHEN_MINUS || sourcePhraseChars[ich] == HYPHEN) {
                        hyphenatedPhraseChars[ihy++] = ZERO_WIDTH_SPACE;
                    }
                }
                ich++;
            }
        }
        return new String(hyphenatedPhraseChars, 0, ihy);
    }

    /**
     * performs hyphenation with auto detection of language.
     * Object must be created with Hyphenator(Context context)
     *
     * @param phrase string to hyphenate
     * @return hyphenated string
     */
    public String hyphenateWithDetectLangauge(String phrase) {
        return hyphenateWithDetectLangauge(phrase, 1, 1);
    }

    /**
     * performs hyphenation with auto detection of language.
     * Object must be created with Hyphenator(Context context)
     *
     * @param phrase         string to hyphenate
     * @param leftHyphenMin  unbreakable characters at the beginning of each word in the
     *                       phrase
     * @param rightHyphenMin unbreakable characters at the end of each word in the phrase
     * @return the string with soft hyphens inserted
     */
    public String hyphenateWithDetectLangauge(String phrase, int leftHyphenMin, int rightHyphenMin) {
        if (guessLanguage == null) {
            return null;
        }
        String lang = guessLanguage.guessLanguage(phrase);
        if (indicHyphenRules.get(lang) == null) {
            return phrase;
        }

        try {
            if (lang.equals("en")) {
                this.loadTable(this.mContext.getResources().openRawResource(R.raw.silpa_sdk_hyph_en));
            } else {
                RuleDefinition rules = new Utf8TexParser().parse(indicHyphenRules.get(lang));
                this.setRuleSet(rules);
            }
        } catch (TexParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hyphenate(phrase, leftHyphenMin, rightHyphenMin);
    }

    /**
     * Extract a word from a char array. The word is converted to lower case and
     * a '.' character is appended to the beginning and end of the new array.
     *
     * @param chars      The character array to extract a smaller section from
     * @param wordStart  First character to include from the source array <b>chars</b>.
     * @param wordLength Number of characters to include from the source array
     *                   <b>chars</b>
     * @return Word converted so lower case and surrounded by '.'
     */
    private char[] extractWord(char[] chars, int wordStart, int wordLength) {
        char[] echars = new char[wordLength + 2];
        echars[0] = echars[echars.length - 1] = '.';
        for (int i = 0; i < wordLength; i++) {
            echars[1 + i] = Character.toLowerCase(chars[wordStart + i]);
        }
        return echars;
    }

    /**
     * Generate a hyphen qualification points for a word by applying rules.
     *
     * @param wordChars Word surrounded by '.' characters
     * @param length    Length of the word (excluding '.' characters)
     * @return hyphen qualification points for the word
     */
    @SuppressWarnings("rawtypes")
    private int[] applyHyphenationRules(final char[] wordChars, final int length) {
        int[] hyphenQualificationPoints = new int[wordChars.length + 1];

        for (int istart = 0; istart < length; istart++) {
            List rules = ruleSet.getPatternTree((int) wordChars[istart]);
            int i = istart;

            java.util.Enumeration rulesEnumeration = rules.elements();
            while (rulesEnumeration.hasMoreElements()) {
                rules = (List) rulesEnumeration.nextElement();

                if (((Character) rules.head()).charValue() == wordChars[i]) {
                    rules = rules.longTail(); // values
                    int[] nodevalues = (int[]) rules.head();
                    for (int inv = 0; inv < nodevalues.length; inv++) {
                        if (nodevalues[inv] > hyphenQualificationPoints[istart
                                + inv]) {
                            hyphenQualificationPoints[istart + inv] = nodevalues[inv];
                        }
                    }
                    i++;

                    if (i == wordChars.length) {
                        break;
                    }
                    rulesEnumeration = rules.longTail().elements(); // child
                    // nodes
                }
            }
        }

        int[] newvalues = new int[length];
        System.arraycopy(hyphenQualificationPoints, 2, newvalues, 0, length); // save
        // 12
        // bytes;
        // senseless
        hyphenQualificationPoints = newvalues;
        return hyphenQualificationPoints;
    }

    private class ForwardingErrorHandler implements ErrorHandler {
        private ErrorHandler target;

        public ForwardingErrorHandler(ErrorHandler target) {
            this.target = target;
        }

        public ErrorHandler getTarget() {
            return target;
        }

        public void setTarget(ErrorHandler target) {
            this.target = target;
        }

        public void debug(String domain, String message) {
            target.debug(domain, message);
        }

        public void info(String s) {
            target.info(s);
        }

        public void warning(String s) {
            target.warning(s);
        }

        public void error(String s) {
            target.error(s);
        }

        public void exception(String s, Exception e) {
            target.exception(s, e);
        }
    }

}