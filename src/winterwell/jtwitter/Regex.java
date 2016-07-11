package winterwell.jtwitter;


import java.util.regex.Pattern;
/**
 * This file is copyrighted by Twitter and protected by the Twitter's Apache 2004 user license
 * @author alexn
 *
 */
public class Regex {
  private static final String UNICODE_SPACES = "[" +
    "\\u0009-\\u000d" +     //  # White_Space # Cc   [5] <control-0009>..<control-000D>
    "\\u0020" +             // White_Space # Zs       SPACE
    "\\u0085" +             // White_Space # Cc       <control-0085>
    "\\u00a0" +             // White_Space # Zs       NO-BREAK SPACE
    "\\u1680" +             // White_Space # Zs       OGHAM SPACE MARK
    "\\u180E" +             // White_Space # Zs       MONGOLIAN VOWEL SEPARATOR
    "\\u2000-\\u200a" +     // # White_Space # Zs  [11] EN QUAD..HAIR SPACE
    "\\u2028" +             // White_Space # Zl       LINE SEPARATOR
    "\\u2029" +             // White_Space # Zp       PARAGRAPH SEPARATOR
    "\\u202F" +             // White_Space # Zs       NARROW NO-BREAK SPACE
    "\\u205F" +             // White_Space # Zs       MEDIUM MATHEMATICAL SPACE
    "\\u3000" +              // White_Space # Zs       IDEOGRAPHIC SPACE
  "]";

  private static String LATIN_ACCENTS_CHARS = "\\u00c0-\\u00d6\\u00d8-\\u00f6\\u00f8-\\u00ff" + // Latin-1
                                              "\\u0100-\\u024f" + // Latin Extended A and B
                                              "\\u0253\\u0254\\u0256\\u0257\\u0259\\u025b\\u0263\\u0268\\u026f\\u0272\\u0289\\u028b" + // IPA Extensions
                                              "\\u02bb" + // Hawaiian
                                              "\\u0300-\\u036f" + // Combining diacritics
                                              "\\u1e00-\\u1eff"; // Latin Extended Additional (mostly for Vietnamese)
  private static final String HASHTAG_ALPHA_CHARS = "a-z" + LATIN_ACCENTS_CHARS +
                                                   "\\u0400-\\u04ff\\u0500-\\u0527" +  // Cyrillic
                                                   "\\u2de0-\\u2dff\\ua640-\\ua69f" +  // Cyrillic Extended A/B
                                                   "\\u0591-\\u05bf\\u05c1-\\u05c2\\u05c4-\\u05c5\\u05c7" +
                                                   "\\u05d0-\\u05ea\\u05f0-\\u05f4" + // Hebrew
                                                   "\\ufb1d-\\ufb28\\ufb2a-\\ufb36\\ufb38-\\ufb3c\\ufb3e\\ufb40-\\ufb41" +
                                                   "\\ufb43-\\ufb44\\ufb46-\\ufb4f" + // Hebrew Pres. Forms
                                                   "\\u0610-\\u061a\\u0620-\\u065f\\u066e-\\u06d3\\u06d5-\\u06dc" +
                                                   "\\u06de-\\u06e8\\u06ea-\\u06ef\\u06fa-\\u06fc\\u06ff" + // Arabic
                                                   "\\u0750-\\u077f\\u08a0\\u08a2-\\u08ac\\u08e4-\\u08fe" + // Arabic Supplement and Extended A
                                                   "\\ufb50-\\ufbb1\\ufbd3-\\ufd3d\\ufd50-\\ufd8f\\ufd92-\\ufdc7\\ufdf0-\\ufdfb" + // Pres. Forms A
                                                   "\\ufe70-\\ufe74\\ufe76-\\ufefc" + // Pres. Forms B
                                                   "\\u200c" +                        // Zero-Width Non-Joiner
                                                   "\\u0e01-\\u0e3a\\u0e40-\\u0e4e" + // Thai
                                                   "\\u1100-\\u11ff\\u3130-\\u3185\\uA960-\\uA97F\\uAC00-\\uD7AF\\uD7B0-\\uD7FF" + // Hangul (Korean)
                                                   "\\p{InHiragana}\\p{InKatakana}" +  // Japanese Hiragana and Katakana
                                                   "\\p{InCJKUnifiedIdeographs}" +     // Japanese Kanji / Chinese Han
                                                   "\\u3003\\u3005\\u303b" +           // Kanji/Han iteration marks
                                                   "\\uff21-\\uff3a\\uff41-\\uff5a" +  // full width Alphabet
                                                   "\\uff66-\\uff9f" +                 // half width Katakana
                                                   "\\uffa1-\\uffdc";                  // half width Hangul (Korean)
  private static final String HASHTAG_ALPHA_NUMERIC_CHARS = "0-9\\uff10-\\uff19_" + HASHTAG_ALPHA_CHARS;
  private static final String HASHTAG_ALPHA = "[" + HASHTAG_ALPHA_CHARS +"]";
  private static final String HASHTAG_ALPHA_NUMERIC = "[" + HASHTAG_ALPHA_NUMERIC_CHARS +"]";

  /* URL related hash regex collection */
  private static final String URL_VALID_PRECEEDING_CHARS = "(?:[^A-Z0-9/@＠$#＃\u202A-\u202E]|^)";

  private static final String URL_VALID_CHARS = "[\\p{Alnum}" + LATIN_ACCENTS_CHARS + "]";
  private static final String URL_VALID_SUBDOMAIN = "(?:(?:" + URL_VALID_CHARS + "[" + URL_VALID_CHARS + "\\-_]*)?" + URL_VALID_CHARS + "\\.)";
  private static final String URL_VALID_DOMAIN_NAME = "(?:(?:" + URL_VALID_CHARS + "[" + URL_VALID_CHARS + "\\-]*)?" + URL_VALID_CHARS + "\\.)";
  /* Any non-space, non-punctuation characters. \p{Z} = any kind of whitespace or invisible separator. */
  private static final String URL_VALID_UNICODE_CHARS = "[.[^\\p{Punct}\\s\\p{Z}\\p{InGeneralPunctuation}]]";

	private static final String URL_VALID_GTLD = "(?:(?:삼성|닷컴|닷넷|香格里拉|餐厅|食品|飞利浦|電訊盈科|集团|购物|谷歌|" +
		"诺基亚|联通|网络|网站|网店|网址|组织机构|移动|珠宝|点看|游戏|淡马锡|机构|書籍|时尚|新闻|政府|政务|手表|手机|我爱你|慈善|微博|" +
		"广东|工行|家電|娱乐|大拿|在线|嘉里大酒店|嘉里|商标|商店|商城|公益|公司|八卦|健康|信息|佛山|企业|中文网|中信|世界|ポイント|" +
		"ファッション|セール|ストア|コム|グーグル|クラウド|みんな|คอม|संगठन|नेट|कॉम|همراه|موقع|موبايلي|كوم|شبكة|بيتك|بازار|" +
		"العليان|ارامكو|ابوظبي|קום|сайт|рус|орг|онлайн|москва|ком|дети|zuerich|zone|zippo|zip|zero|zara|zappos|yun|" +
		"youtube|you|yokohama|yoga|yodobashi|yandex|yamaxun|yahoo|yachts|xyz|xxx|xperia|xin|xihuan|xfinity|xerox|" +
		"xbox|wtf|wtc|world|works|work|woodside|wolterskluwer|wme|wine|windows|win|williamhill|wiki|wien|whoswho|" +
		"weir|weibo|wedding|wed|website|weber|webcam|weatherchannel|weather|watches|watch|warman|wanggou|wang|" +
		"walter|wales|vuelos|voyage|voto|voting|vote|volkswagen|vodka|vlaanderen|viva|vistaprint|vista|vision|" +
		"virgin|vip|vin|villas|viking|vig|video|viajes|vet|versicherung|vermögensberatung|vermögensberater|verisign|" +
		"ventures|vegas|vana|vacations|ups|uol|uno|university|unicom|ubs|tvs|tushu|tunes|tui|tube|trv|trust|" +
		"travelersinsurance|travelers|travelchannel|travel|training|trading|trade|toys|toyota|town|tours|total|" +
		"toshiba|toray|top|tools|tokyo|today|tmall|tirol|tires|tips|tiffany|tienda|tickets|theatre|theater|thd|teva|" +
		"tennis|temasek|telefonica|telecity|tel|technology|tech|team|tdk|tci|taxi|tax|tattoo|tatar|tatamotors|" +
		"taobao|talk|taipei|tab|systems|symantec|sydney|swiss|swatch|suzuki|surgery|surf|support|supply|supplies|" +
		"sucks|style|study|studio|stream|store|storage|stockholm|stcgroup|stc|statoil|statefarm|statebank|starhub|" +
		"star|stada|srl|spreadbetting|spot|spiegel|space|soy|sony|song|solutions|solar|sohu|software|softbank|" +
		"social|soccer|sncf|smile|skype|sky|skin|ski|site|singles|sina|silk|shriram|show|shouji|shopping|" +
		"shop|shoes|shiksha|shia|shell|shaw|sharp|shangrila|sfr|sexy|sex|sew|seven|services|sener|select|" +
		"seek|security|seat|scot|scor|science|schwarz|schule|school|scholarships|schmidt|schaeffler|scb|sca|" +
		"sbs|sbi|saxo|save|sas|sarl|sapo|sap|sanofi|sandvikcoromant|sandvik|samsung|salon|sale|sakura|" +
		"safety|safe|saarland|ryukyu|rwe|run|ruhr|rsvp|room|rodeo|rocks|rocher|rip|rio|ricoh|richardli|rich|" +
		"rexroth|reviews|review|restaurant|rest|republican|report|repair|rentals|rent|ren|reit|reisen|reise|" +
		"rehab|redumbrella|redstone|red|recipes|realty|realtor|realestate|read|racing|quest|quebec|qpon|pwc|" +
		"pub|protection|property|properties|promo|progressive|prof|productions|prod|pro|prime|press|praxi|" +
		"post|porn|politie|poker|pohl|pnc|plus|plumbing|playstation|play|place|pizza|pioneer|pink|ping|pin|" +
		"pid|pictures|pictet|pics|piaget|physio|photos|photography|photo|philips|pharmacy|pet|pccw|" +
		"passagens|party|parts|partners|pars|paris|panerai|pamperedchef|page|ovh|ott|otsuka|osaka|origins|" +
		"orientexpress|organic|org|orange|oracle|ooo|online|onl|ong|one|omega|ollo|olayangroup|olayan|" +
		"okinawa|office|obi|nyc|ntt|nrw|nra|nowtv|nowruz|now|norton|northwesternmutual|nokia|nissay|nissan|" +
		"ninja|nikon|nico|nhk|ngo|nfl|nexus|nextdirect|next|news|new|neustar|network|netflix|netbank|net|" +
		"nec|navy|natura|name|nagoya|nadex|mutuelle|mutual|museum|mtr|mtpc|mtn|movistar|movie|mov|" +
		"motorcycles|moscow|mortgage|mormon|montblanc|money|monash|mom|moi|moe|moda|mobily|mobi|mma|mls|mlb|" +
		"mitsubishi|mit|mini|mil|microsoft|miami|metlife|meo|menu|men|memorial|meme|melbourne|meet|media|" +
		"med|mba|mattel|marriott|markets|marketing|market|mango|management|man|makeup|maison|maif|madrid|" +
		"luxury|luxe|lupin|ltda|ltd|love|lotto|lotte|london|lol|locus|locker|loans|loan|lixil|living|live|" +
		"lipsy|link|linde|lincoln|limo|limited|like|lighting|lifestyle|lifeinsurance|life|lidl|liaison|lgbt|" +
		"lexus|lego|legal|leclerc|lease|lds|lawyer|law|latrobe|lat|lasalle|lanxess|landrover|land|lancaster|" +
		"lamer|lamborghini|lacaixa|kyoto|kuokgroup|kred|krd|kpn|kpmg|kosher|komatsu|koeln|kiwi|kitchen|" +
		"kindle|kinder|kim|kia|kfh|kerryproperties|kerrylogistics|kerryhotels|kddi|kaufen|juegos|jprs|" +
		"jpmorgan|joy|jot|joburg|jobs|jnj|jmp|jll|jlc|jewelry|jetzt|jcp|jcb|java|jaguar|iwc|itv|itau|" +
		"istanbul|ist|ismaili|iselect|irish|ipiranga|investments|international|int|insure|insurance|" +
		"institute|ink|ing|info|infiniti|industries|immobilien|immo|imdb|imamat|ikano|iinet|ifm|icu|ice|" +
		"icbc|ibm|hyundai|htc|hsbc|how|house|hotmail|hoteles|hosting|host|horse|honda|homes|homedepot|" +
		"holiday|holdings|hockey|hkt|hiv|hitachi|hisamitsu|hiphop|hgtv|hermes|here|helsinki|help|healthcare|" +
		"health|hdfcbank|haus|hangout|hamburg|guru|guitars|guide|guge|gucci|guardian|group|gripe|green|" +
		"gratis|graphics|grainger|gov|got|gop|google|goog|goodyear|goo|golf|goldpoint|gold|godaddy|gmx|gmo|" +
		"gmbh|gmail|globo|global|gle|glass|giving|gives|gifts|gift|ggee|genting|gent|gea|gdn|gbiz|garden|" +
		"games|game|gallup|gallo|gallery|gal|fyi|futbol|furniture|fund|fujitsu|ftr|frontier|frontdoor|" +
		"frogans|frl|fresenius|fox|foundation|forum|forsale|forex|ford|football|foodnetwork|foo|fly|" +
		"flsmidth|flowers|florist|flir|flights|flickr|fitness|fit|fishing|fish|firmdale|firestone|fire|" +
		"financial|finance|final|film|ferrero|feedback|fedex|fast|fashion|farmers|farm|fans|fan|family|" +
		"faith|fairwinds|fail|fage|extraspace|express|exposed|expert|exchange|everbank|events|eus|" +
		"eurovision|estate|esq|erni|ericsson|equipment|epson|epost|enterprises|engineering|engineer|energy|" +
		"emerck|email|education|edu|edeka|eat|earth|dvag|durban|dupont|dunlop|dubai|dtv|drive|download|dot|" +
		"doosan|domains|doha|dog|docs|dnp|discount|directory|direct|digital|diet|diamonds|dhl|dev|design|" +
		"desi|dentist|dental|democrat|delta|deloitte|dell|delivery|degree|deals|dealer|deal|dds|dclk|day|" +
		"datsun|dating|date|dance|dad|dabur|cyou|cymru|cuisinella|csc|cruises|crs|crown|cricket|creditunion|" +
		"creditcard|credit|courses|coupons|coupon|country|corsica|coop|cool|cookingchannel|cooking|" +
		"contractors|contact|consulting|construction|condos|comsec|computer|compare|company|community|" +
		"commbank|comcast|com|cologne|college|coffee|codes|coach|clubmed|club|cloud|clothing|clinique|" +
		"clinic|click|cleaning|claims|cityeats|city|citic|cisco|circle|cipriani|church|chrome|christmas|" +
		"chloe|chintai|cheap|chat|chase|channel|chanel|cfd|cfa|cern|ceo|center|ceb|cbre|cbn|cba|catering|" +
		"cat|casino|cash|casa|cartier|cars|careers|career|care|cards|caravan|car|capital|capetown|canon|" +
		"cancerresearch|camp|camera|cam|call|cal|cafe|cab|bzh|buzz|buy|business|builders|build|bugatti|" +
		"budapest|brussels|brother|broker|broadway|bridgestone|bradesco|boutique|bot|bostik|bosch|boots|" +
		"book|boo|bond|bom|boehringer|boats|bnpparibas|bnl|bmw|bms|blue|bloomberg|blog|blanco|blackfriday|" +
		"black|biz|bio|bingo|bing|bike|bid|bible|bharti|bet|best|berlin|bentley|beer|beats|bcn|bcg|bbva|bbc|" +
		"bayern|bauhaus|bargains|barefoot|barclays|barclaycard|barcelona|bar|bank|band|baidu|baby|azure|axa|" +
		"aws|avianca|autos|auto|author|audio|audible|audi|auction|attorney|associates|asia|arte|art|arpa|" +
		"army|archi|aramco|aquarelle|apple|app|apartments|anz|anquan|android|analytics|amsterdam|amica|" +
		"alstom|alsace|ally|allfinanz|alipay|alibaba|akdn|airtel|airforce|airbus|aig|agency|agakhan|afl|" +
		"aetna|aero|aeg|adult|ads|adac|actor|active|aco|accountants|accountant|accenture|academy|abudhabi|" +
		"abogado|able|abbvie|abbott|abb|aarp|aaa|onion)(?=\\P{Alnum}|$))";
	
	private static final String URL_VALID_CCTLD = "(?:(?:한국|香港|澳門|新加坡|台灣|台湾|中國|中国|გე|ไทย|ලංකා|" +
		"ഭാരതം|ಭಾರತ|భారత్|சிங்கப்பூர்|இலங்கை|இந்தியா|ଭାରତ|ભારત|ਭਾਰਤ|ভাৰত|ভারত|বাংলা|भारत|" +
		"پاکستان|مليسيا|مصر|قطر|فلسطين|عمان|عراق|سورية|سودان|تونس|بھارت|ایران|امارات|المغرب|السعودية|الجزائر|" +
		"الاردن|հայ|қаз|укр|срб|рф|мон|мкд|ею|бел|бг|ελ|zw|zm|za|yt|ye|ws|wf|vu|vn|vi|vg|ve|vc|va|uz|uy|us|" +
		"um|uk|ug|ua|tz|tw|tv|tt|tr|tp|to|tn|tm|tl|tk|tj|th|tg|tf|td|tc|sz|sy|sx|sv|su|st|ss|sr|so|sn|sm|sl|" +
		"sk|sj|si|sh|sg|se|sd|sc|sb|sa|rw|ru|rs|ro|re|qa|py|pw|pt|ps|pr|pn|pm|pl|pk|ph|pg|pf|pe|pa|om|nz|nu|" +
		"nr|np|no|nl|ni|ng|nf|ne|nc|na|mz|my|mx|mw|mv|mu|mt|ms|mr|mq|mp|mo|mn|mm|ml|mk|mh|mg|mf|me|md|mc|ma|" +
		"ly|lv|lu|lt|ls|lr|lk|li|lc|lb|la|kz|ky|kw|kr|kp|kn|km|ki|kh|kg|ke|jp|jo|jm|je|it|is|ir|iq|io|in|im|" +
		"il|ie|id|hu|ht|hr|hn|hm|hk|gy|gw|gu|gt|gs|gr|gq|gp|gn|gm|gl|gi|gh|gg|gf|ge|gd|gb|ga|fr|fo|fm|fk|fj|" +
		"fi|eu|et|es|er|eh|eg|ee|ec|dz|do|dm|dk|dj|de|cz|cy|cx|cw|cv|cu|cr|co|cn|cm|cl|ck|ci|ch|cg|cf|cd|cc|" +
		"ca|bz|by|bw|bv|bt|bs|br|bq|bo|bn|bm|bl|bj|bi|bh|bg|bf|be|bd|bb|ba|az|ax|aw|au|at|as|ar|aq|ao|an|am|" +
		"al|ai|ag|af|ae|ad|ac)(?=\\P{Alnum}|$))";
	
  private static final String URL_PUNYCODE = "(?:xn--[0-9a-z]+)";

  private static final String URL_VALID_DOMAIN =
    "(?:" +                                                   // subdomains + domain + TLD
        URL_VALID_SUBDOMAIN + "+" + URL_VALID_DOMAIN_NAME +   // e.g. www.twitter.com, foo.co.jp, bar.co.uk
        "(?:" + URL_VALID_GTLD + "|" + URL_VALID_CCTLD + "|" + URL_PUNYCODE + ")" +
      ")" +
    "|(?:" +                                                  // domain + gTLD
      URL_VALID_DOMAIN_NAME +                                 // e.g. twitter.com
      "(?:" + URL_VALID_GTLD + "|" + URL_PUNYCODE + ")" +
    ")" +
    "|(?:" + "(?<=https?://)" +
      "(?:" +
        "(?:" + URL_VALID_DOMAIN_NAME + URL_VALID_CCTLD + ")" +  // protocol + domain + ccTLD
        "|(?:" +
          URL_VALID_UNICODE_CHARS + "+\\." +                     // protocol + unicode domain + TLD
          "(?:" + URL_VALID_GTLD + "|" + URL_VALID_CCTLD + ")" +
        ")" +
      ")" +
    ")" +
    "|(?:" +                                                  // domain + ccTLD + '/'
      URL_VALID_DOMAIN_NAME + URL_VALID_CCTLD + "(?=/)" +     // e.g. t.co/
    ")";

  private static final String URL_VALID_PORT_NUMBER = "[0-9]++";

  private static final String URL_VALID_GENERAL_PATH_CHARS = "[a-z0-9!\\*';:=\\+,.\\$/%#\\[\\]\\-_~\\|&@" + LATIN_ACCENTS_CHARS + "]";
  /** Allow URL paths to contain balanced parens
   *  1. Used in Wikipedia URLs like /Primer_(film)
   *  2. Used in IIS sessions like /S(dfd346)/
  **/
  private static final String URL_BALANCED_PARENS = "\\(" + URL_VALID_GENERAL_PATH_CHARS + "+\\)";
  /** Valid end-of-path chracters (so /foo. does not gobble the period).
   *   2. Allow =&# for empty URL parameters and other URL-join artifacts
  **/
  private static final String URL_VALID_PATH_ENDING_CHARS = "[a-z0-9=_#/\\-\\+" + LATIN_ACCENTS_CHARS + "]|(?:" + URL_BALANCED_PARENS +")";

  private static final String URL_VALID_PATH = "(?:" +
    "(?:" +
      URL_VALID_GENERAL_PATH_CHARS + "*" +
      "(?:" + URL_BALANCED_PARENS + URL_VALID_GENERAL_PATH_CHARS + "*)*" +
      URL_VALID_PATH_ENDING_CHARS +
    ")|(?:@" + URL_VALID_GENERAL_PATH_CHARS + "+/)" +
  ")";

  private static final String URL_VALID_URL_QUERY_CHARS = "[a-z0-9!?\\*'\\(\\);:&=\\+\\$/%#\\[\\]\\-_\\.,~\\|@]";
  private static final String URL_VALID_URL_QUERY_ENDING_CHARS = "[a-z0-9_&=#/]";
  private static final String VALID_URL_PATTERN_STRING =
  "(" +                                                            //  $1 total match
    "(" + URL_VALID_PRECEEDING_CHARS + ")" +                       //  $2 Preceeding chracter
    "(" +                                                          //  $3 URL
      "(https?://)?" +                                             //  $4 Protocol (optional)
      "(" + URL_VALID_DOMAIN + ")" +                               //  $5 Domain(s)
      "(?::(" + URL_VALID_PORT_NUMBER +"))?" +                     //  $6 Port number (optional)
      "(/" +
        URL_VALID_PATH + "*+" +
      ")?" +                                                       //  $7 URL Path and anchor
      "(\\?" + URL_VALID_URL_QUERY_CHARS + "*" +                   //  $8 Query String
              URL_VALID_URL_QUERY_ENDING_CHARS + ")?" +
    ")" +
  ")";

  private static String AT_SIGNS_CHARS = "@\uFF20";

  private static final String DOLLAR_SIGN_CHAR = "\\$";
  private static final String CASHTAG = "[a-z]{1,6}(?:[._][a-z]{1,2})?";

  /* Begin public constants */

  public static final Pattern VALID_HASHTAG = Pattern.compile("(^|[^&" + HASHTAG_ALPHA_NUMERIC_CHARS + "])(#|\uFF03)(" + HASHTAG_ALPHA_NUMERIC + "*" + HASHTAG_ALPHA + HASHTAG_ALPHA_NUMERIC + "*)", Pattern.CASE_INSENSITIVE);
  public static final int VALID_HASHTAG_GROUP_BEFORE = 1;
  public static final int VALID_HASHTAG_GROUP_HASH = 2;
  public static final int VALID_HASHTAG_GROUP_TAG = 3;
  public static final Pattern INVALID_HASHTAG_MATCH_END = Pattern.compile("^(?:[#＃]|://)");
  public static final Pattern RTL_CHARACTERS = Pattern.compile("[\u0600-\u06FF\u0750-\u077F\u0590-\u05FF\uFE70-\uFEFF]");

  public static final Pattern AT_SIGNS = Pattern.compile("[" + AT_SIGNS_CHARS + "]");
  public static final Pattern VALID_MENTION_OR_LIST = Pattern.compile("([^a-z0-9_!#$%&*" + AT_SIGNS_CHARS + "]|^|RT:?)(" + AT_SIGNS + "+)([a-z0-9_]{1,20})(/[a-z][a-z0-9_\\-]{0,24})?", Pattern.CASE_INSENSITIVE);
  public static final int VALID_MENTION_OR_LIST_GROUP_BEFORE = 1;
  public static final int VALID_MENTION_OR_LIST_GROUP_AT = 2;
  public static final int VALID_MENTION_OR_LIST_GROUP_USERNAME = 3;
  public static final int VALID_MENTION_OR_LIST_GROUP_LIST = 4;

  public static final Pattern VALID_REPLY = Pattern.compile("^(?:" + UNICODE_SPACES + ")*" + AT_SIGNS + "([a-z0-9_]{1,20})", Pattern.CASE_INSENSITIVE);
  public static final int VALID_REPLY_GROUP_USERNAME = 1;

  public static final Pattern INVALID_MENTION_MATCH_END = Pattern.compile("^(?:[" + AT_SIGNS_CHARS + LATIN_ACCENTS_CHARS + "]|://)");

  public static final Pattern VALID_URL = Pattern.compile(VALID_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
  public static final int VALID_URL_GROUP_ALL          = 1;
  public static final int VALID_URL_GROUP_BEFORE       = 2;
  public static final int VALID_URL_GROUP_URL          = 3;
  public static final int VALID_URL_GROUP_PROTOCOL     = 4;
  public static final int VALID_URL_GROUP_DOMAIN       = 5;
  public static final int VALID_URL_GROUP_PORT         = 6;
  public static final int VALID_URL_GROUP_PATH         = 7;
  public static final int VALID_URL_GROUP_QUERY_STRING = 8;

  public static final Pattern VALID_TCO_URL = Pattern.compile("^https?:\\/\\/t\\.co\\/[a-z0-9]+", Pattern.CASE_INSENSITIVE);
  public static final Pattern INVALID_URL_WITHOUT_PROTOCOL_MATCH_BEGIN = Pattern.compile("[-_./]$");

  public static final Pattern VALID_CASHTAG = Pattern.compile("(^|" + UNICODE_SPACES + ")(" + DOLLAR_SIGN_CHAR + ")(" + CASHTAG + ")" +"(?=$|\\s|\\p{Punct})", Pattern.CASE_INSENSITIVE);
  public static final int VALID_CASHTAG_GROUP_BEFORE = 1;
  public static final int VALID_CASHTAG_GROUP_DOLLAR = 2;
  public static final int VALID_CASHTAG_GROUP_CASHTAG = 3;
}