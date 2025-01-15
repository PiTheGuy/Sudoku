package pitheguy.sudoku;

import org.apache.commons.cli.*;
import pitheguy.sudoku.gui.Sudoku;

public class Main {
    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(createOptions(), args);
        if (commandLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar Sudoku.jar", createOptions());
            return;
        }
        Sudoku sudoku = new Sudoku(true);
        if (commandLine.hasOption("puzzle")) sudoku.loadPuzzle(Integer.parseInt(commandLine.getOptionValue("puzzle")));
    }

    public static Options createOptions() {
        Options options = new Options();
        options.addOption("help", false, "Print this message");
        options.addOption("puzzle", true, "Import a puzzle on startup");
        return options;
    }
}