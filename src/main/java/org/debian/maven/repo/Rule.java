package org.debian.maven.repo;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule {
    private static Pattern generic = Pattern.compile("([\\[\\?\\+\\*\\|])|([^\\\\]\\.)");

    private Pattern pattern;
    private String replace;
    private String rule;

    public Rule(String rule) {
        this.rule = rule;
        if (rule.startsWith("s/")) {
            StringTokenizer st = new StringTokenizer(rule, "/");
            st.nextToken();
            pattern = Pattern.compile(st.nextToken());
            replace = st.nextToken();
        } else {
            String pat = escapeParameters(rule.replace(".", "\\.").replace("*", "(.*)"));
	    pattern = Pattern.compile(pat);
            replace = escapeGroupMatch(rule).replace("*", "$1");
        }
    }

    public boolean match(String s) {
        if (s == null) {
            return isGeneric();
        }
        return pattern.matcher(s).matches();
    }

    public String apply(String s) {
        if (s == null) {
            return null;
        }
        Matcher m = pattern.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            if (m.start() < m.end()) {
                m.appendReplacement(sb, replace);
            }
        }
        return sb.toString();
    }

    public boolean isGeneric() {
        return generic.matcher(pattern.pattern()).find();
    }

    public String getPattern() {
        return pattern.pattern();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rule other = (Rule) obj;
        if (this.pattern != other.pattern && (this.pattern == null || !this.pattern.equals(other.pattern))) {
            return false;
        }
        if ((this.replace == null) ? (other.replace != null) : !this.replace.equals(other.replace)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.pattern != null ? this.pattern.hashCode() : 0);
        hash = 89 * hash + (this.replace != null ? this.replace.hashCode() : 0);
        return hash;
    }

    public String toString() {
        return rule;
    }
    
    /**
     * Escape (ie. preprend \\) characters which can be specials chars for regexp.
     * 
     * @param value Input chars
     * @return Escaped output chars
     */
    private String escapeParameters(String value) {
	return escapeGroupMatch(value).replace("{", "\\{").replace("}", "\\}");
    }
    
    /**
     * Escape (ie. preprend \\) characters which can be group identifiers for replace
     * 
     * @param value Input chars
     * @return Escaped output chars
     */
    private String escapeGroupMatch(String value) {
	return value.replace("$", "\\$");
    }
}
