Usage
=====

### To get string with soft hyphens inserted

```
        Hyphenator hyphenator = new Hyphenator(getActivity());
        hyphenator.loadTable(getActivity().getResources().openRawResource(R.raw.silpa_sdk_hyph_en));
        
        String text = "SILPA is an acronym of Swathanthra (Mukth, " +
                            "Free as in Freedom) Indian Language Processing Applications. " +
                            "Its a web framework written using Flask microframework for hosting " +
                            "various Indian language computing algorithms written in python. " +
                            "It currently provides JSONRPC support which is also used by web framework " +
                            "itself to input data and fetch result." +
                            "The modules work as standalone python packages which will serve their " +
                            "purpose and also they plug into the silpa-flask webframewok so that " +
                            "they can be accessed as web services also, or become just another webapp " +
                            "like the dictionary module.";
                            
        String hyphenatedText = hyphenator.hyphenate(text);

```



### To get string with soft hyphens inserted, with auto detection of language

```
        Hyphenator hyphenator = new Hyphenator(getActivity());
        String text = "ശരിയ്ക്കും അങ്ങനെ ഒരു ലിനക്സ് ഉണ്ടു് എന്നു് മാത്രമല്ല ആളുകള്‍ അതു് ഉപയോഗിയ്ക്കുന്നുമുണ്ടു്, പക്ഷേ അതു് പ്രവര്‍ത്തക സംവിധാനത്തിന്റെ ഒരു ഭാഗം മാത്രമാണു്. ലിനക്സൊരു കെര്‍ണലാണു്: നിങ്ങള്‍ പ്രവര്‍ത്തിപ്പിയ്ക്കുന്ന മറ്റു് പ്രോഗ്രാമുകള്‍ക്കു് സിസ്റ്റത്തിന്റെ വിഭവങ്ങള്‍ വിട്ടുകൊടുക്കുന്ന പ്രോഗ്രാമാണതു്. ഒരു പ്രവര്‍ത്തക സംവിധാനത്തിന്റെ ഒഴിച്ചുകൂടാനാവാത്ത ഭാഗമാണു് കെര്‍ണല്‍, പക്ഷേ അതു് മാത്രം കൊണ്ടു് വലിയ പ്രയോജനമൊന്നുമില്ല; മുഴുവന്‍ പ്രവര്‍ത്തക സംവിധാനത്തിനൊപ്പമേ അതിനു് പ്രവര്‍ത്തിയ്ക്കാനാകൂ. ലിനക്സ് സാധാരണയായി ഗ്നു എന്ന പ്രവര്‍ത്തക സംവിധാനവുമായി ചേര്‍ന്നാണുപയോഗിയ്ക്കുന്നതു്: ലിനക്സ് കെര്‍ണലായി പ്രവര്‍ത്തിയ്ക്കുന്ന മുഴുവന്‍ സിസ്റ്റവും അടിസ്ഥാനപരമായി ഗ്നുവാണു് അഥവാ ഗ്നു/ലിനക്സ് ആണു്. “ലിനക്സ്” എന്നു് പറയപ്പെടുന്ന എല്ലാ വിതരണങ്ങളും ശരിയ്ക്കും, ഗ്നു/ലിനക്സ് വിതരണങ്ങളാണു്.";
        
        String hyphenatedText = hyphenator.hyphenateWithDetectLangauge(text);

```

This automatically loads rules for Indic languages - Assamese, Bengali, Gujarati, Hindi, Kannada, Malayalam, Marathi, Oriya, Panjabi, Tamil, Telugu.



### To specify unbreakable characters

To specify unbreakable characters, use the following function

```
public String hyphenate(String phrase, int leftHyphenMin, int rightHyphenMin);
public String hyphenateWithDetectLangauge(String phrase, int leftHyphenMin, int rightHyphenMin);

```

where 

`leftHyphenMin` -> unbreakable characters at the beginning of each word in the phrase

`rightHyphenMin` -> unbreakable characters at the end of each word in the phrase

Default values are `leftHyphenMin = 1` , `rightHyphenMin = 1`
