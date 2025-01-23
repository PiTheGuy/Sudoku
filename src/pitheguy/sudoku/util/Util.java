package pitheguy.sudoku.util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Util {
    public static void runInBackground(Runnable task) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }
        };
        worker.execute();
    }

    public static void copyToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public static String readFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return null;
        }
    }


    public static <T> List<Pair<T>> getAllPairs(List<T> list) {
        List<Pair<T>> pairs = new ArrayList<>();
        if (list.size() < 2) return pairs;
        for (int i = 0; i < list.size(); i++)
            for (int j = i + 1; j < list.size(); j++) pairs.add(new Pair<>(list.get(i), list.get(j)));
        return pairs;
    }
}
