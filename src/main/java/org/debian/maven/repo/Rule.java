package org.debian.maven.repo;

/*
 * Copyright 2009 Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class Rule {
    private static Pattern generic = Pattern.compile("([\\[\\?\\+\\*\\|])|([^\\\\]\\.)");

    private Pattern pattern;
    private String replace;
    private String rule;
    private String description;

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
            return matchesNull();
        }
        return pattern.matcher(s).matches();
    }

    public String apply(String s) {
        if (s == null) {
            if (matchesNull()) {
                if (replace.indexOf("$1") < 0) {
                    return replace;
                }
            }
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
        return matchesNull() || generic.matcher(pattern.pattern()).find();
    }

    public boolean matchesNull() {
        String patternString = pattern.pattern();
        return ".*".equals(patternString) || "(.*)".equals(patternString);
    }

    public String getPattern() {
        return pattern.pattern();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rule other = (Rule) obj;
        if (this.pattern != other.pattern && (this.pattern == null || !this.pattern.pattern().equals(other.pattern.pattern()))) {
            return false;
        }
        return !((this.replace == null) ? (other.replace != null) : !this.replace.equals(other.replace));
    }

    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.pattern != null ? this.pattern.pattern().hashCode() : 0);
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
