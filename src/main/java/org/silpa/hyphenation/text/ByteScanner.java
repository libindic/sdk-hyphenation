package org.silpa.hyphenation.text;

import org.silpa.hyphenation.util.ErrorHandler;
import org.silpa.hyphenation.util.Hashtable;
import org.silpa.hyphenation.util.List;

import java.io.IOException;

/* parser for TeX hyphenation tables */
class ByteScanner implements RuleDefinition {
    final static short EOF = 0, LBRAC = 1, RBRAC = 2, PATTERNS = 3, EXCEPTIONS = 4, PATTERN = 5;

    private final ErrorHandler eh;
    private final List[] entrytab;
    private final Hashtable exceptions;

    private java.io.InputStream in;
    private int[] codelist;

    char[] pattern = new char[0];
    int patlen;
    private int cc, cc1, prevlno, lno, cno;


    private static final int cp2i(int c0, int c1) {
        return (c0 << 8) + c1;
    }

    ByteScanner(ErrorHandler eh) {
        exceptions = new Hashtable();
        entrytab = new List[256];
        for (int i = 0; i != 256; ++i) {
            entrytab[i] = new List();
        }
        this.eh = eh;
    }

    void scan(java.io.InputStream in, int[] codelist) {
        this.in = in;
        this.codelist = codelist;
        cc = '\n';
        cc1 = -1;
        prevlno = -1;
        lno = 0;
        cno = 0;
        read();
        final short EXCEPTIONS = 1, PATTERNS = 2, NONE = 0;
        short state = NONE;
        short sym;
        LANGDATA:
        for (; ; ) {
            sym = getSym();
            switch (sym) {
                case ByteScanner.EOF:
                    break LANGDATA;
                case ByteScanner.PATTERNS:
                case ByteScanner.EXCEPTIONS:
                    if (getSym() != ByteScanner.LBRAC) {
                        error("'{' expected");
                    }
                    state = sym == ByteScanner.PATTERNS ? PATTERNS : EXCEPTIONS;
                    continue LANGDATA;
                case ByteScanner.RBRAC:
                    state = NONE;
                    continue LANGDATA;
                case ByteScanner.PATTERN:
                    switch (state) {
                        case PATTERNS:
                            readPattern();
                            break;
                        case EXCEPTIONS:
                            readException();
                            break;
                        case NONE:
                            break;
                    }
                    continue LANGDATA;
                case ByteScanner.LBRAC:
                default:
                    error("problem parsing input");
                    continue LANGDATA;
            }
        }
        try {
            in.close();
        } catch (IOException e) {
        }
    }

    private short getSym() {
        SYM:
        for (; ; ) {
            while (isSpace())
                read(); /* skip space */
            switch (cc) {
                case '%': /* skip comment */
                    do {
                        read();
                    } while (!(isNL() || cc == -1));
                    continue SYM;
                case '\\':
                    read();
                    if (isLetter()) {
                        patlen = 0;
                        for (; ; ) {
                            cc2pat();
                            if (!isLetter()) {
                                String kwd = new String(pattern, 0, patlen);
                                if (kwd.equals("patterns")) return PATTERNS;
                                else if (kwd.equals("hyphenation")) return EXCEPTIONS;
                                else if (kwd.equals("endinput")) return EOF;
                                else {
                                    warning("shamelessly skipping \\" + kwd);
                                    continue SYM;
                                }
                            }
                        }
                    }
                case '{':
                    read();
                    return LBRAC;
                case '}':
                    read();
                    return RBRAC;
                case -1:
                    return EOF;
                default:
                    if (isPatChar()) {
                        patlen = 0;
                        PAT:
                        for (; ; ) {
                            cc2pat();
                            if (!isPatChar()) {
                                if (patlen > 1) return PATTERN;
                                else break PAT;
                            }
                        }
                    } else {
                        read(); /* just skip */
                    }
                    continue SYM;
            }
        }
    }

    public int[] getException(String word) {
        return (int[]) exceptions.get(word);
    }

    public List getPatternTree(int c) {
        return entrytab[c % 256];
    }

    private void read() {
        if (cc != -1) {
            if (isNL()) {
                ++lno;
                cno = 0;
            }
            try {
                if (cc1 != -1) {
                    cc = cc1;
                    cc1 = -1;
                } else {
                    cc = in.read();
                }
                switch (cc) {
                    case '^': {
                        cc1 = in.read();
                        if (cc1 == '^') { /* ^^... */
                            cc1 = -1;
                            int cc2 = in.read();
                            if ((cc = hexval(cc2)) == -1) { /*
                                                             * not a lowercase
															 * hexadecimal digit
															 */
                                cc = (cc2 + 64) & 127; /* crazy tex rule */
                            } else { /* is a lowercase hexadecimal digit */
                                cc1 = in.read();
                                if ((cc2 = hexval(cc1)) != -1) { /*
                                                                 * is a
																 * two-digit
																 * hexadecimal
																 * value
																 */
                                    cc1 = -1;
                                    cc *= 16;
                                    cc += cc2;
                                }
                            }
                        }
                        cc = codelist[cc];
                    }
                    break;
					/*
					 * many hyphenation patterns contain accents patterned after
					 * \rm font's macros
					 * the simplest approach is to adopt it -- and when I look
					 * at this code I am really not
					 * sure whether it does the right thing or not
					 */
                    case '\\': {
                        cc1 = in.read();
                        switch (cc1) {
                            case '^':
                            case '\'':
                            case '`':
                            case '"':
                            case '~':
                            case '=':
                            case '.': // accents
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'H':
                            case 'k':
                            case 'r':
                            case 'u':
                            case 'v': // accents
                            case 'a':
                            case 'i':
                            case 'l':
                            case 'o':
                            case 's': { // special characters
                                int key = cc1 << 8;  // accented character code:
                                // (accent<<8)+base
                                // character
                                int sep = -1; // separator: nothing, space,
                                // curly bracket, backslash for
                                // dotless i and j
                                int cc0 = in.read(); // base character
                                if ((cc0 == ' ' && !(cc1 == 'l' || cc1 == 'o' || cc1 == 'i')) // \c
                                        // o,
                                        // but
                                        // not
                                        // \l
                                        // ,
                                        // \o
                                        // ,
                                        // \i
                                        // ,
                                        || cc0 == '{' || cc0 == '\\') {
                                    sep = cc0;
                                    cc0 = in.read();
                                    if (sep == '{' && cc0 == '}') sep = -1;
                                    cc0 = ' ';
                                }
                                cc1 = -1;
                                key += cc0;
                                if (sep == '{') in.read();
                                Object chrval = acctab.get(Integer.valueOf(key));
                                cc = chrval != null ? ((Integer) chrval)
                                        .intValue() : cc0; // unless the code
                                // for the accented
                                // character is
                                // known, use
                                // unmodified one
                            }
                            break;
                            default:
                                break;
                        }
                    }
                    break;
                    default:
                        if (cc != -1) cc = codelist[cc];
                        break;
                }
            } catch (IOException e) {
                error(e.toString());
                cc = -1;
            }
            cno++;
        }
    }

    private void cc2pat() {
        if (patlen == pattern.length) {
            char[] newpattern = new char[patlen * 2 + 1];
            System.arraycopy(pattern, 0, newpattern, 0, patlen);
            pattern = newpattern;
        }
        pattern[patlen++] = Character.toLowerCase((char) cc);
        read();
    }

    private boolean isLetter() {
        if (cc == -1) return false;
        else return Character.isLetter((char) this.cc);
    }

    private boolean isSpace() {
        if (cc == -1) return false;
        else {
            return Character.isSpaceChar((char) cc) || isNL();
        }
    }

    private boolean isNL() {
        if (cc == '\n') return true;
        if (cc == '\r') {
            if (cc1 == -1) {
                try {
                    cc = in.read();
                } catch (IOException e) {
                    error(e.toString());
                    cc = -1;
                }
            } else {
                cc = cc1;
                cc1 = -1;
            }
            if (cc != '\n') cc1 = cc;
            return true;
        }
        return false;
    }

    private boolean isPatChar() {
        if (cc == -1) return false;
        else {
            char cc = (char) this.cc;
            return Character.isLetterOrDigit(cc) || cc == '.' || cc == '-';
        }
    }

    private void readPattern() {
        List entry = null, level = entrytab[(int) pattern[Character.isDigit(pattern[0]) ? 1 : 0] % 256];
        int[] nodevalues = new int[patlen + 1];
        int ich = 0, inv = 0;
        java.util.Enumeration eentry = level.elements();
        for (; ; ) {
            if (Character.isDigit(pattern[ich])) {
                nodevalues[inv++] = ((int) pattern[ich++] - (int) '0');
                if (ich == patlen) break;
            } else {
                nodevalues[inv++] = 0;
            }
            for (; ; ) {
                if (!eentry.hasMoreElements()) {
                    int[] newnodevalues = new int[inv + 1];
                    for (int jnv = 0; jnv != newnodevalues.length; ++jnv)
                        newnodevalues[jnv] = 0;
                    entry = new List().snoc(Character.valueOf(pattern[ich]))
                            .snoc(newnodevalues);
                    level.snoc(entry);
                    level = entry;
                    eentry = level.elements();
                    eentry.nextElement();
                    eentry.nextElement();
                    break;
                }
                entry = (List) eentry.nextElement();
                if (((Character) entry.head()).charValue() == pattern[ich]) {
                    level = entry;
                    eentry = level.elements();
                    eentry.nextElement();
                    eentry.nextElement();
                    break;
                }
            }
            if (++ich == patlen) {
                nodevalues[inv++] = 0;
                break;
            }
        }
        System.arraycopy(nodevalues, 0, ((int[]) entry.longTail().head()), 0, inv);
    }

    private void readException() {
        int i0 = 0, in = patlen;
        for (; ; ) {
            if (in == i0) return;
            else if (pattern[i0] == '-') ++i0;
            else if (pattern[in - 1] == '-') --in;
            else break;
        }
        int[] values = new int[in - i0];
        int jch = 0;
        for (int ich = i0; ich != in; ++ich) {
            if (pattern[ich] == '-') {
                values[jch - 1] = 1;
            } else {
                pattern[jch] = pattern[ich];
                values[jch] = 0;
                ++jch;
            }
        }
        exceptions.put(new String(pattern, 0, jch), values);
    }

    private void error(String msg) {
        if (prevlno != lno) {
            prevlno = lno; /* one message per line at most */
            eh.error("(" + lno + "," + cno + "): " + msg);
        }
    }

    private void warning(String msg) {
        if (prevlno != lno) {
            prevlno = lno; /* one message per line at most */
            eh.warning("(" + lno + "," + cno + "): " + msg);
        }
    }

    private int hexval(int cc) {
        switch (cc) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return cc - (int) '0';
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return cc - (int) 'a' + 10;
            default:
                return -1;
        }
    }

    private static final Hashtable acctab = new Hashtable();

    static {
		/* grave */
        acctab.put(Integer.valueOf(cp2i('`', 'a')), Integer.valueOf(0xe0));
        acctab.put(Integer.valueOf(cp2i('`', 'e')), Integer.valueOf(0xe8));
        acctab.put(Integer.valueOf(cp2i('`', 'i')), Integer.valueOf(0xec));
        acctab.put(Integer.valueOf(cp2i('`', 'o')), Integer.valueOf(0xf2));
        acctab.put(Integer.valueOf(cp2i('`', 'u')), Integer.valueOf(0xf9));
        acctab.put(Integer.valueOf(cp2i('`', 'w')), Integer.valueOf(0x1E81));
        acctab.put(Integer.valueOf(cp2i('`', 'y')), Integer.valueOf(0x1EF3));

		/* acute */
        acctab.put(Integer.valueOf(cp2i('\'', 'a')), Integer.valueOf(0xe1));
        acctab.put(Integer.valueOf(cp2i('\'', 'c')), Integer.valueOf(0x0107));
        acctab.put(Integer.valueOf(cp2i('\'', 'e')), Integer.valueOf(0xe9));
        acctab.put(Integer.valueOf(cp2i('\'', 'g')), Integer.valueOf(0x1F5));
        acctab.put(Integer.valueOf(cp2i('\'', 'i')), Integer.valueOf(0xed));
        acctab.put(Integer.valueOf(cp2i('\'', 'k')), Integer.valueOf(0x1E31));
        acctab.put(Integer.valueOf(cp2i('\'', 'l')), Integer.valueOf(0x13A));
        acctab.put(Integer.valueOf(cp2i('\'', 'm')), Integer.valueOf(0x1E3F));
        acctab.put(Integer.valueOf(cp2i('\'', 'n')), Integer.valueOf(0x144));
        acctab.put(Integer.valueOf(cp2i('\'', 'o')), Integer.valueOf(0xf3));
        acctab.put(Integer.valueOf(cp2i('\'', 'p')), Integer.valueOf(0x1E55));
        acctab.put(Integer.valueOf(cp2i('\'', 'r')), Integer.valueOf(0x155));
        acctab.put(Integer.valueOf(cp2i('\'', 's')), Integer.valueOf(0x15B));
        acctab.put(Integer.valueOf(cp2i('\'', 'u')), Integer.valueOf(0xfa));
        acctab.put(Integer.valueOf(cp2i('\'', 'w')), Integer.valueOf(0x1E83));
        acctab.put(Integer.valueOf(cp2i('\'', 'y')), Integer.valueOf(0xfd));
        acctab.put(Integer.valueOf(cp2i('\'', 'z')), Integer.valueOf(0x17A));

		/* circuflex */
        acctab.put(Integer.valueOf(cp2i('^', 'a')), Integer.valueOf(0xe2));
        acctab.put(Integer.valueOf(cp2i('^', 'c')), Integer.valueOf(0x0109));
        acctab.put(Integer.valueOf(cp2i('^', 'e')), Integer.valueOf(0xea));
        acctab.put(Integer.valueOf(cp2i('^', 'g')), Integer.valueOf(0x011D));
        acctab.put(Integer.valueOf(cp2i('^', 'h')), Integer.valueOf(0x0125));
        acctab.put(Integer.valueOf(cp2i('^', 'i')), Integer.valueOf(0xee));
        acctab.put(Integer.valueOf(cp2i('^', 'j')), Integer.valueOf(0x0135));
        acctab.put(Integer.valueOf(cp2i('^', 'o')), Integer.valueOf(0xf4));
        acctab.put(Integer.valueOf(cp2i('^', 's')), Integer.valueOf(0x015D));
        acctab.put(Integer.valueOf(cp2i('^', 'u')), Integer.valueOf(0xfb));
        acctab.put(Integer.valueOf(cp2i('^', 'w')), Integer.valueOf(0x0175));
        acctab.put(Integer.valueOf(cp2i('^', 'y')), Integer.valueOf(0x0177));
        acctab.put(Integer.valueOf(cp2i('^', 'z')), Integer.valueOf(0x1E91));

		/* dieresis */
        acctab.put(Integer.valueOf(cp2i('"', 'a')), Integer.valueOf(0xe4));
        acctab.put(Integer.valueOf(cp2i('"', 'e')), Integer.valueOf(0xeb));
        acctab.put(Integer.valueOf(cp2i('"', 'h')), Integer.valueOf(0x1E27));
        acctab.put(Integer.valueOf(cp2i('"', 'i')), Integer.valueOf(0xef));
        acctab.put(Integer.valueOf(cp2i('"', 'o')), Integer.valueOf(0xf6));
        acctab.put(Integer.valueOf(cp2i('"', 't')), Integer.valueOf(0x1E97));
        acctab.put(Integer.valueOf(cp2i('"', 'u')), Integer.valueOf(0xfc));
        acctab.put(Integer.valueOf(cp2i('"', 'w')), Integer.valueOf(0x1E85));
        acctab.put(Integer.valueOf(cp2i('"', 'x')), Integer.valueOf(0x1E8D));
        acctab.put(Integer.valueOf(cp2i('"', 'y')), Integer.valueOf(0xff));

		/* Hungarian umlaut */
        acctab.put(Integer.valueOf(cp2i('H', 'o')), Integer.valueOf(0x151));
        acctab.put(Integer.valueOf(cp2i('H', 'u')), Integer.valueOf(0x171));

		/* tilde */
        acctab.put(Integer.valueOf(cp2i('~', 'a')), Integer.valueOf(0xE3));
        acctab.put(Integer.valueOf(cp2i('~', 'e')), Integer.valueOf(0x1EBD));
        acctab.put(Integer.valueOf(cp2i('~', 'i')), Integer.valueOf(0x0129));
        acctab.put(Integer.valueOf(cp2i('~', 'n')), Integer.valueOf(0x00F1));
        acctab.put(Integer.valueOf(cp2i('~', 'o')), Integer.valueOf(0x00F5));
        acctab.put(Integer.valueOf(cp2i('~', 'u')), Integer.valueOf(0x0169));
        acctab.put(Integer.valueOf(cp2i('~', 'v')), Integer.valueOf(0x1E7D));
        acctab.put(Integer.valueOf(cp2i('~', 'y')), Integer.valueOf(0x1EF9));

		/* breve */
        acctab.put(Integer.valueOf(cp2i('u', 'a')), Integer.valueOf(0x103));
        acctab.put(Integer.valueOf(cp2i('u', 'e')), Integer.valueOf(0x115));
        acctab.put(Integer.valueOf(cp2i('u', 'g')), Integer.valueOf(0x11F));
        acctab.put(Integer.valueOf(cp2i('u', 'i')), Integer.valueOf(0x12D));
        acctab.put(Integer.valueOf(cp2i('u', 'o')), Integer.valueOf(0x14F));
        acctab.put(Integer.valueOf(cp2i('u', 'u')), Integer.valueOf(0x16D));

		/* caron */
        acctab.put(Integer.valueOf(cp2i('v', 'a')), Integer.valueOf(0x1CE));
        acctab.put(Integer.valueOf(cp2i('v', ' ')), Integer.valueOf(0x2C7));
        acctab.put(Integer.valueOf(cp2i('v', 'c')), Integer.valueOf(0x10D));
        acctab.put(Integer.valueOf(cp2i('v', 'd')), Integer.valueOf(0x10F));
        acctab.put(Integer.valueOf(cp2i('v', 'e')), Integer.valueOf(0x11B));
        acctab.put(Integer.valueOf(cp2i('v', 'g')), Integer.valueOf(0x1E7));
        acctab.put(Integer.valueOf(cp2i('v', 'i')), Integer.valueOf(0x1D0));
        acctab.put(Integer.valueOf(cp2i('v', 'j')), Integer.valueOf(0x1F0));
        acctab.put(Integer.valueOf(cp2i('v', 'k')), Integer.valueOf(0x1E9));
        acctab.put(Integer.valueOf(cp2i('v', 'l')), Integer.valueOf(0x13E));
        acctab.put(Integer.valueOf(cp2i('v', 'n')), Integer.valueOf(0x148));
        acctab.put(Integer.valueOf(cp2i('v', 'o')), Integer.valueOf(0x1D2));
        acctab.put(Integer.valueOf(cp2i('v', 'r')), Integer.valueOf(0x159));
        acctab.put(Integer.valueOf(cp2i('v', 's')), Integer.valueOf(0x161));
        acctab.put(Integer.valueOf(cp2i('v', 't')), Integer.valueOf(0x165));
        acctab.put(Integer.valueOf(cp2i('v', 'u')), Integer.valueOf(0x1D4));
        acctab.put(Integer.valueOf(cp2i('v', 'z')), Integer.valueOf(0x17E));

		/* cedilla */
        acctab.put(Integer.valueOf(cp2i('c', 'c')), Integer.valueOf(0xe7));
        acctab.put(Integer.valueOf(cp2i('c', 'd')), Integer.valueOf(0x1E11));
        acctab.put(Integer.valueOf(cp2i('c', 'g')), Integer.valueOf(0x123));
        acctab.put(Integer.valueOf(cp2i('c', 'h')), Integer.valueOf(0x1E29));
        acctab.put(Integer.valueOf(cp2i('c', 'k')), Integer.valueOf(0x137));
        acctab.put(Integer.valueOf(cp2i('c', 'l')), Integer.valueOf(0x13C));
        acctab.put(Integer.valueOf(cp2i('c', 'n')), Integer.valueOf(0x146));
        acctab.put(Integer.valueOf(cp2i('c', 'r')), Integer.valueOf(0x157));
        acctab.put(Integer.valueOf(cp2i('c', 's')), Integer.valueOf(0x15F));
        acctab.put(Integer.valueOf(cp2i('c', 't')), Integer.valueOf(0x163));

		/* dot below */
        acctab.put(Integer.valueOf(cp2i('d', 'a')), Integer.valueOf(0x1EA1));
        acctab.put(Integer.valueOf(cp2i('d', 'b')), Integer.valueOf(0x1E05));
        acctab.put(Integer.valueOf(cp2i('d', 'd')), Integer.valueOf(0x1E0D));
        acctab.put(Integer.valueOf(cp2i('d', 'e')), Integer.valueOf(0x1EB9));
        acctab.put(Integer.valueOf(cp2i('d', 'h')), Integer.valueOf(0x1E25));
        acctab.put(Integer.valueOf(cp2i('d', 'i')), Integer.valueOf(0x1ECB));
        acctab.put(Integer.valueOf(cp2i('d', 'k')), Integer.valueOf(0x1E33));
        acctab.put(Integer.valueOf(cp2i('d', 'l')), Integer.valueOf(0x1E37));
        acctab.put(Integer.valueOf(cp2i('d', 'm')), Integer.valueOf(0x1E43));
        acctab.put(Integer.valueOf(cp2i('d', 'n')), Integer.valueOf(0x1E47));
        acctab.put(Integer.valueOf(cp2i('d', 'o')), Integer.valueOf(0x1ECD));
        acctab.put(Integer.valueOf(cp2i('d', 'r')), Integer.valueOf(0x1E5B));
        acctab.put(Integer.valueOf(cp2i('d', 's')), Integer.valueOf(0x1E63));
        acctab.put(Integer.valueOf(cp2i('d', 't')), Integer.valueOf(0x1E6D));
        acctab.put(Integer.valueOf(cp2i('d', 'u')), Integer.valueOf(0x1EE5));
        acctab.put(Integer.valueOf(cp2i('d', 'v')), Integer.valueOf(0x1E7F));
        acctab.put(Integer.valueOf(cp2i('d', 'w')), Integer.valueOf(0x1E89));
        acctab.put(Integer.valueOf(cp2i('d', 'y')), Integer.valueOf(0x1EF5));
        acctab.put(Integer.valueOf(cp2i('d', 'z')), Integer.valueOf(0x1E93));

		/* dot above */
        acctab.put(Integer.valueOf(cp2i('.', 'c')), Integer.valueOf(0x10B));
        acctab.put(Integer.valueOf(cp2i('.', 'e')), Integer.valueOf(0x117));
        acctab.put(Integer.valueOf(cp2i('.', 'g')), Integer.valueOf(0x121));
        acctab.put(Integer.valueOf(cp2i('.', 'l')), Integer.valueOf(0x140));
        acctab.put(Integer.valueOf(cp2i('.', 'z')), Integer.valueOf(0x17C));

		/* ring */
        acctab.put(Integer.valueOf(cp2i('r', 'a')), Integer.valueOf(0xE5));
        acctab.put(Integer.valueOf(cp2i('r', 'u')), Integer.valueOf(0x16F));
        acctab.put(Integer.valueOf(cp2i('r', 'w')), Integer.valueOf(0x1E98));
        acctab.put(Integer.valueOf(cp2i('r', 'y')), Integer.valueOf(0x1E99));

		/* ogonek */
        acctab.put(Integer.valueOf(cp2i('k', 'a')), Integer.valueOf(0x105));
        acctab.put(Integer.valueOf(cp2i('k', 'e')), Integer.valueOf(0x119));
        acctab.put(Integer.valueOf(cp2i('k', 'i')), Integer.valueOf(0x12F));
        acctab.put(Integer.valueOf(cp2i('k', 'o')), Integer.valueOf(0x1EB));
        acctab.put(Integer.valueOf(cp2i('k', 'u')), Integer.valueOf(0x173));

		/* special codes */
        acctab.put(Integer.valueOf(cp2i('a', 'a')), Integer.valueOf(0xe5));
        acctab.put(Integer.valueOf(cp2i('a', 'e')), Integer.valueOf(0xe6));
        acctab.put(Integer.valueOf(cp2i('i', ' ')), Integer.valueOf(0x131));
        acctab.put(Integer.valueOf(cp2i('l', ' ')), Integer.valueOf(0x142));
        acctab.put(Integer.valueOf(cp2i('o', ' ')), Integer.valueOf(0xf8));
        acctab.put(Integer.valueOf(cp2i('o', 'e')), Integer.valueOf(0x153));
        acctab.put(Integer.valueOf(cp2i('s', 's')), Integer.valueOf(0xdf));
    }

}
