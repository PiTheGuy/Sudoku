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
    public static final int BYTES_PER_LINE = 164;
    public final Box[] boxes = new Box[9];
    public int[] board = createEmptyBoard();
    private final Square[] cachedSquares = new Square[81];
    private final Square[][] rows = new Square[9][9];
    private final Square[][] columns = new Square[9][9];
    private final Square[][] boxSquares = new Square[9][9];
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
        initializeCacheArrays();
        setVisible(visible);
    }

    private static int[] createEmptyBoard() {
        int[] board = new int[81];
        Arrays.fill(board, 0);
        return board;
    }

    private void initializeCacheArrays() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                cachedSquares[row * 9 + col] = boxes[(row / 3) * 3 + (col / 3)].getSquare(row % 3, col % 3);;
            }
        }
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                rows[i][j] = getSquare(i, j);
                columns[i][j] = getSquare(j, i);
                boxSquares[i][j] = boxes[i].getSquare(j);
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

    public Square getSquare(String location) {
        char rowChar = location.charAt(0);
        char colChar = location.charAt(1);
        return getSquare(rowChar - 'A', colChar - '1');
    }

    public void checkValidity() {
        forEachSquare(square -> square.setInvalid(false));
        checkDuplicates(this::getRow);
        checkDuplicates(this::getColumn);
        checkDuplicates(this::getBox);
    }

    public boolean isSolved() {
        for (int value : board) if (value == 0) return false;
        return true;
    }

    public List<Square> getRow(int row) {
        return new ArrayList<>(Arrays.asList(rows[row]));
    }

    public List<Square> getColumn(int col) {
        return new ArrayList<>(Arrays.asList(columns[col]));
    }

    public List<Square> getBox(int box) {
        return new ArrayList<>(Arrays.asList(boxSquares[box]));
    }

    public List<Square> getAllSquares() {
        return List.of(cachedSquares);
    }

    public void forEachSquare(Consumer<Square> action) {
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) action.accept(getSquare(row, col));
    }

    private void checkDuplicates(Function<Integer, List<Square>> groupExtractor) {
        for (int i = 0; i < 9; i++) {
            Map<Integer, Square> values = new HashMap<>();
            for (Square square : groupExtractor.apply(i)) {
                int value = square.getValue();
                if (value == 0) continue;
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
        try {
            RandomAccessFile puzzlesFile = threadLocalFile.get();
            puzzlesFile.seek((long) (puzzleNumber - 1) * BYTES_PER_LINE);
            String puzzle = puzzlesFile.readLine();
            if (puzzle == null) throw new IOException("Puzzle not found");
            loadPuzzle(puzzle);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load puzzle", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadPuzzle(String puzzle) {
        if (puzzle.contains(",")) puzzle = puzzle.substring(0, puzzle.indexOf(","));
        for (int cell = 0; cell < 81; cell++) {
            int value = puzzle.charAt(cell) - '0';
            board[cell] = value;
            Square square = cachedSquares[cell];
            square.setGiven(value != 0);
            square.invalidateCachedValue();
        }
        resetCandidates();
        repaint();
    }

    public void solvePuzzle() {
        SudokuSolver solver = new SudokuSolver(this);
        solver.solve();
        repaint();
    }

    public void resetCandidates() {
        getAllSquares().stream().filter(square -> !square.isSolved()).forEach(square -> square.getCandidates().reset());
    }

    public void toggleCandidate(int digit) {
        DigitCandidates candidates = getSquare(selectedCell / 9, selectedCell % 9).getCandidates();
        if (candidates.contains(digit)) candidates.remove(digit);
        else candidates.add(digit);
    }

    public void copyBoardToClipboard(boolean includeCandidates) {
        if (includeCandidates) {
            copyFullBoardToClipboard();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int cell = 0; cell < 81; cell++) sb.append(board[cell]);
        Util.copyToClipboard(sb.toString());
    }

    private void copyFullBoardToClipboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("S9B");
        for (int cell = 0; cell < 81; cell++) {
            Square square = cachedSquares[cell];
            short cellData;
            if (square.isSolved()) {
                cellData = (short) square.getValue();
                if (square.isGiven()) cellData += 9;
            } else {
                cellData = (short) (square.getCandidates().pack() + 18);
            }
            String cellString = Integer.toString(cellData, Character.MAX_RADIX);
            if (cellString.length() == 1) cellString = "0" + cellString;
            sb.append(cellString);
        }
        Util.copyToClipboard(sb.toString());
    }

    public void pasteBoardFromClipboard() {
        String board = Util.readFromClipboard();
        if (board == null) return;
        if (board.startsWith("S9B")) {
            loadFullBoard(board);
            return;
        }
        if (!validateBoard(board)) {
            System.out.println("Warning: attempted to paste invalid or nonexistent board from clipboard");
            return;
        }
        loadPuzzle(board);
    }

    private void loadFullBoard(String board) {
        try {
            board = board.substring("S9B".length());
            if (board.length() != 162) throw new IOException("Unexpected length: expected 162, got " + board.length());
            Arrays.fill(this.board, 0);
            for (int i = 0; i < 81; i++) {
                Square square = cachedSquares[i];
                String cellString = board.substring(i * 2, i * 2 + 2);
                int cellData = Integer.parseInt(cellString, Character.MAX_RADIX);
                if (cellData <= 0) throw new IOException("Invalid cell data: " + cellData);
                else if (cellData <= 9) {
                    square.setValue(cellData);
                    square.setGiven(true);
                } else if (cellData <= 18) {
                    square.setValue(cellData - 9);
                    square.setGiven(false);
                } else if (cellData <= 529) {
                    square.getCandidates().setFlags((short) (cellData - 18));
                    square.setGiven(false);
                } else throw new IOException("Invalid cell data: " + cellData);
                square.invalidateCachedValue();
            }
        } catch (IOException e) {
            System.out.println("Warning: attempted to load malformed full board from clipboard: " + e.getMessage());
        }

    }

    private static boolean validateBoard(String board) {
        if (board == null) return false;
        if (board.length() < 81) return false;
        for (int i = 0; i < board.length(); i++) {
            char c = board.charAt(i);
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private class SudokuKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_F4) openLoadPuzzleDialog();
            if (e.getKeyCode() == KeyEvent.VK_F5) Util.runInBackground(Sudoku.this::solvePuzzle);
            if (e.getKeyCode() == KeyEvent.VK_F6) resetCandidates();
            if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) copyBoardToClipboard(e.isShiftDown());
            if (e.getKeyCode() == KeyEvent.VK_V && e.isControlDown()) pasteBoardFromClipboard();
            if (selectedCell != -1) {
                int keyCode = e.getKeyCode();
                if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
                    if (e.isShiftDown()) toggleCandidate(keyCode - '0');
                    else cachedSquares[selectedCell].setValue(keyCode - '0');
                } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                    board[selectedCell] = 0;
                    getSquare(selectedCell / 9, selectedCell % 9).invalidateCachedValue();
                }
                checkValidity();
                repaint();
            }
        }
    }
}
