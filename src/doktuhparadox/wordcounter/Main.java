package doktuhparadox.wordcounter;

import org.apache.commons.cli.*;

import javax.swing.JFileChooser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static doktuhparadox.wordcounter.Main.Flag.*;

public class Main {

    static enum Flag {
        ALL_WORDS("all"),
        SPECIFIC_WORDS("w"),
        FILE_DIRECTORY_INPUTTED("f"),
        FILE_DIRECTORY_SELECTED("fc"),
        CUSTOM_DELIMITING_REGEX("reg");

        private String flagString;

        Flag(String flagString) {
            this.flagString = flagString;
        }

        String flagString() {
            return flagString;
        }
    }

    public static void main(String[] args) {
        final Map<String, Map<Integer, Integer>> wordsAndOccurrences;
        Options options = new Options();
        CommandLineParser parser = new BasicParser();

        options.addOption(ALL_WORDS.flagString(), false, "Search for all words. Ignores -w.");
        options.addOption(SPECIFIC_WORDS.flagString(), true, "List wordsAndOccurrences for which to search. Separated by commas.");
        options.addOption(FILE_DIRECTORY_INPUTTED.flagString(), true, "Designate the input file.");
        options.addOption(FILE_DIRECTORY_SELECTED.flagString(), false, "Use a file chooser to select the input file instead of inputting the directory.");
        options.addOption(CUSTOM_DELIMITING_REGEX.flagString(), true, "Specify a custom stripping regex.");

        try {
            CommandLine cmd = parser.parse(options, args);
            File f = getInputFile(cmd);
            String[] words = readInput(f);

            if (cmd.hasOption(ALL_WORDS.flagString())) {
                wordsAndOccurrences = countWords(words, words);
            } else if (cmd.hasOption(SPECIFIC_WORDS.flagString())) {
                wordsAndOccurrences = countWords(words, cmd.getOptionValue(SPECIFIC_WORDS.flagString()).split(","));
            } else {
                wordsAndOccurrences = null;
            }

            if (wordsAndOccurrences != null) {
                wordsAndOccurrences.forEach((word, countMap) -> {
                    Collection<Integer> countValues = countMap.values();
                    System.out.printf("-> %s:%n", word);
                    OptionalInt minOp = countValues.stream().mapToInt(i -> i).min(),
                            maxOp = countValues.stream().mapToInt(i -> i).max();
                    countMap.forEach((chapter, numOccurrences) -> {
                        System.out.printf("--> Chapter %s: %s occurrences", chapter, numOccurrences);
                        if (minOp.isPresent() && maxOp.isPresent()) {
                            int min = minOp.getAsInt(),
                                max = maxOp.getAsInt();

                            if (min != max) {
                                if (numOccurrences == min) System.out.print(" (-)");
                                if (numOccurrences == max) System.out.print(" (+)");
                            }
                        }
                        System.out.printf("%n");
                    });
                    System.out.printf("-> Total: %s ", countValues.stream().mapToInt(i -> i).sum());
                    countValues.stream().mapToInt(i -> i).average().ifPresent(d -> System.out.printf("(average %s) ", Math.round(d)));
                    System.out.printf("occurrences.%n%n");
                });
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static File getInputFile(CommandLine cmd) {
        File f = null;

        if (cmd.hasOption(FILE_DIRECTORY_INPUTTED.flagString())) {
            f = new File(cmd.getOptionValue(FILE_DIRECTORY_INPUTTED.flagString()));
            if (!f.exists()) System.out.println("Given input file does not exist.");
        } else if (cmd.hasOption(FILE_DIRECTORY_SELECTED.flagString())) {
            JFileChooser jFileChooser = new JFileChooser();
            int confirm = jFileChooser.showDialog(null, "Select");
            if (confirm == JFileChooser.APPROVE_OPTION) {
                f = jFileChooser.getSelectedFile();
            }
        }

        return f;
    }

    private static String[] readInput(File f) throws IOException {
        ArrayList<String> words = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(f));
        String s;
        while ((s = reader.readLine()) != null) {
            Collections.addAll(words, s.replaceAll("[^A-Za-z0-9]+", " ").toLowerCase().split("\\s"));
        }

        String[] wordsArr = new String[words.size()];
        wordsArr = words.toArray(wordsArr);
        return wordsArr;
    }

    public static Map<String, Map<Integer, Integer>> countWords(String[] input, String[] words) {
        TreeMap<String, Map<Integer, Integer>> map = new TreeMap<>();
        for (String word : words) {
            map.put(word, new HashMap<>());
        }

        int chapter = 0;

        for (String word : input) {
            if (word.matches("[0-9]+")) {
                chapter++;
                continue;
            }

            if (map.containsKey(word)) {
                Map<Integer, Integer> wordCount = map.get(word);

                if (wordCount.containsKey(chapter)) {
                    wordCount.put(chapter, wordCount.get(chapter) + 1);
                } else {
                    wordCount.put(chapter, 1);
                }
            }
        }

        return map;
    }
}
