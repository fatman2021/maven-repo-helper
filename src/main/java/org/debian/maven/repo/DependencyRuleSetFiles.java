package org.debian.maven.repo;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;

import org.debian.maven.cliargs.ArgumentsMap;

import static org.debian.maven.repo.DependencyRuleSet.*;

public class DependencyRuleSetFiles {

    private static final Logger log = Logger.getLogger(DependencyRuleSetFiles.class.getName());
    //         publishedRules.setDontDuplicate(rules);
    private final EnumMap<RulesType, DependencyRuleSet> files;

    public void addDefaultRules() {
        files.get(RulesType.RULES).add(TO_DEBIAN_VERSION_RULE);
        files.get(RulesType.RULES).add(MAVEN_PLUGINS_KEEP_VERSION_RULE);
    }

    public DependencyRuleSetFiles(EnumMap<RulesType, DependencyRuleSet> files) {
        this.files = files;
    }

    public DependencyRuleSetFiles() {
        this(new EnumMap<RulesType, DependencyRuleSet>(RulesType.class));
        for (RulesType type : RulesType.values()) {
            files.put(type, new DependencyRuleSet());
        }
    }

    public DependencyRuleSet get(RulesType type) {
        return files.get(type);
    }

    public static DependencyRuleSetFiles fromCLIArguments(ArgumentsMap argsMap, boolean verbose) {
        DependencyRuleSetFiles depFiles = new DependencyRuleSetFiles();
        for (RulesType type : RulesType.values()) {
            CLIArgs args = type.cliArgs;
            if (args == null) {
                continue;
            }
            File rulesFile = argsMap.getFile(args.fileLong, args.fileShort, null);
            List<String> rulesExtra = argsMap.getValueList(args.extraLong, args.extraShort);

            DependencyRuleSet ruleSet = DependencyRuleSet.readRules(rulesFile, "", verbose, false);
            ruleSet.addAll(rulesExtra);
            depFiles.get(type).addAll(ruleSet);
        }

        return depFiles;
    }

    public void save(File outputDirectory) {
        for (RulesType type : RulesType.values()) {
            save(outputDirectory, type);
        }
    }

    public void save(File outputDirectory, RulesType type) {
        if(type.filename == null) return;
        File file = new File(outputDirectory, type.filename);
        switch(type) {
            case CLEAN:
                files.get(type).saveToFile(file, files.get(RulesType.IGNORE));
                break;
            default:
                files.get(type).saveToFile(file, null);
                break;
        }
    }

    public enum RulesType {
        RULES("Rules", "rules", new CLIArgs("rules", "r", "extra-rule", "R")),
        AUTOMATIC("Automatic rules", null, null),
        PUBLISHED("Published rules", "publishedRules", new CLIArgs("published-rules", null, "extra-published-rule", "U")),
        IGNORE("Ignore rules", "ignoreRules", new CLIArgs("ignore-rules", "i", "extra-ignore-rule", "I")),
        CLEAN("cleanIgnore", "cleanIgnoreRules", null);

        public final String name;
        public final String filename;
        public final String descriptionResource;
        public final CLIArgs cliArgs;

        RulesType(String name, String identifier, CLIArgs cliArgs) {
            this.name = name;
            this.filename = identifier != null ? "maven." + identifier : null;
            this.descriptionResource = identifier != null ? filename + ".description" : null;
            this.cliArgs = cliArgs;
        }
    }

    public static class CLIArgs {
        public final String fileLong;
        public final String fileShort;
        public final String extraLong;
        public final String extraShort;

        public CLIArgs(String fileLong, String fileShort, String extraLong, String extraShort) {
            this.fileLong = fileLong;
            this.fileShort = fileShort;
            this.extraLong = extraLong;
            this.extraShort = extraShort;
        }
    }
}
