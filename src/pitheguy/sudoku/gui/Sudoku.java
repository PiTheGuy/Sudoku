package pitheguy.sudoku.gui;

import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SudokuSolver;
import pitheguy.sudoku.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Sudoku extends JFrame {
    public final Box[] boxes = new Box[9];
    public String[] board = createEmptyBoard();
    private final Square[] cachedSquares = new Square[81];
    private int selectedCell = -1;

    private static final ThreadLocal<RandomAccessFile> threadLocalFile = ThreadLocal.withInitial(() -> {
        try {
            return new RandomAccessFile("sudoku.csv", "r");
        } catch (IOException e) {
            return null;
        }
    });

    public Sudoku(boolean visible) {
        super("Sudoku");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 900);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 3));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                boxes[row * 3 + col] = new Box(this, row, col);
                add(boxes[row * 3 + col]);
            }
        }
        addKeyListener(new SudokuKeyListener());
        initializeSquareCache();
        setVisible(visible);
    }

    private static String[] createEmptyBoard() {
        String[] board = new String[81];
        Arrays.fill(board, "0");
        return board;
    }

    private void initializeSquareCache() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cachedSquares[row * 9 + col] = boxes[(row / 3) * 3 + (col / 3)].getSquare(row % 3, col % 3);;
            }
        }
    }

    public boolean isSelected(int row, int col) {
        return selectedCell == row * 9 + col;
    }

    public void setSelected(int row, int col) {
        selectedCell = row * 9 + col;
        repaint();
    }

    public Square getSquare(int row, int col) {
        return cachedSquares[row * 9 + col];
    }

    public void checkValidity() {
        forEachSquare(square -> square.setInvalid(false));
        checkDuplicates(this::getRow);
        checkDuplicates(this::getColumn);
        checkDuplicates(this::getBox);
    }

    public boolean isSolved() {
        for (String value : board) if (value.isEmpty()) return false;
        return true;
    }

    public List<Square> getRow(int row) {
        List<Square> list = new ArrayList<>(9);
        for (int col = 0; col < 9; col++) list.add(getSquare(row, col));
        return list;
    }

    public List<Square> getColumn(int col) {
        List<Square> list = new ArrayList<>(9);
        for (int row = 0; row < 9; row++) list.add(getSquare(row, col));
        return list;
    }

    public List<Square> getBox(int box) {
        List<Square> list = new ArrayList<>(9);
        for (int cell = 0; cell < 9; cell++) list.add(boxes[box].getSquare(cell));
        return list;
    }

    public List<Square> getAllSquares() {
        return List.of(cachedSquares);
    }

    public void forEachSquare(Consumer<Square> action) {
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) action.accept(getSquare(row, col));
    }

    private void checkDuplicates(Function<Integer, List<Square>> groupExtractor) {
        for (int i = 0; i < 9; i++) {
            Map<String, Square> values = new HashMap<>();
            for (Square square : groupExtractor.apply(i)) {
                String value = square.getValue();
                if (value.isEmpty()) continue;
                if (values.containsKey(value)) {
                    values.get(value).setInvalid(true);
                    square.setInvalid(true);
                } else {
                    values.put(value, square);
                }
            }
        }
    }

    public boolean isPuzzleLoadingAvailable() {
        return threadLocalFile.get() != null;
    }

    public void openLoadPuzzleDialog() {
        if (!isPuzzleLoadingAvailable()) {
            JOptionPane.showMessageDialog(this, "Failed to load puzzles file. Puzzle loading is unavailable", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String response = JOptionPane.showInputDialog("Please enter a puzzle number");
        if (response == null) return;
        try {
            loadPuzzle(Integer.parseInt(response));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid puzzle number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadPuzzle(int puzzleNumber) {
        int bytesPerLine = 164;
        try {
            RandomAccessFile puzzlesFile = threadLocalFile.get();
            puzzlesFile.seek((long) (puzzleNumber - 1) * bytesPerLine);
            String line = puzzlesFile.readLine();
            if (line == null) throw new IOException("Puzzle not found");
            String puzzle = line.substring(0, line.indexOf(","));
            for (int cell = 0; cell < 81; cell++) {
                String value = String.valueOf(puzzle.charAt(cell));
                board[cell] = value.equals("0") ? "" : value;
                Square square = getSquare(cell / 9, cell % 9);
                square.setGiven(!value.equals("0"));
                square.invalidateCachedValue();
            }
            resetCandidates();
            repaint();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load puzzle", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void solvePuzzle() {
        SudokuSolver solver = new SudokuSolver(this);
        solver.solve();
        repaint();
    }

    public void resetCandidates() {
        forEachSquare(square -> square.getCandidates().reset());
    }

    public void toggleCandidate(int digit) {
        DigitCandidates candidates = getSquare(selectedCell / 9, selectedCell % 9).getCandidates();
        if (candidates.contains(digit)) candidates.remove(digit);
        else candidates.add(digit);
    }

    public void copyBoardToClipboard() {
        StringBuilder sb = new StringBuilder();
        for (int cell = 0; cell < 81; cell++) {
            String value = board[cell];
            sb.append(value.isEmpty() ? "0" : value);
        }
        Util.copyToClipboard(sb.toString());
    }

    private class SudokuKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F4) openLoadPuzzleDialog();
            if (e.getKeyCode() == KeyEvent.VK_F5) Util.runInBackground(Sudoku.this::solvePuzzle);
            if (e.getKeyCode() == KeyEvent.VK_F6) resetCandidates();
            if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) copyBoardToClipboard();
            if (selectedCell != -1) {
                int keyCode = e.getKeyCode();
                if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
                    if (e.isShiftDown()) toggleCandidate(keyCode - '0');
                    else cachedSquares[selectedCell].setValue(String.valueOf((char) keyCode));
                } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                    board[selectedCell] = "";
                    getSquare(selectedCell / 9, selectedCell % 9).invalidateCachedValue();
                }
                checkValidity();
                repaint();
            }
        }
    }
}
