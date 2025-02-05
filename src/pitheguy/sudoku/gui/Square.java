package pitheguy.sudoku.gui;

import pitheguy.sudoku.solver.DigitCandidates;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

public class Square extends JPanel implements Comparable<Square> {
    private final Sudoku sudoku;
    private final DigitCandidates candidates = new DigitCandidates();
    private final int row;
    private final int col;
    private boolean invalid = false;
    private boolean given = false;
    private int cachedValue = 0;

    public Square(Sudoku sudoku, int row, int col) {
        this.sudoku = sudoku;
        this.row = row;
        this.col = col;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!given) sudoku.setSelected(row, col);
            }
        });
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public boolean isGiven() {
        return given;
    }

    public void setGiven(boolean given) {
        this.given = given;
    }

    public int getValue() {
        if (cachedValue == -1) cachedValue = sudoku.board[row * 9 + col];
        return cachedValue;
    }

    public void setValue(int value) {
        sudoku.board[row * 9 + col] = value;
        cachedValue = value;
        if (value != 0) {
            performCandidateElimination(getSurroundingRow(), value);
            performCandidateElimination(getSurroundingColumn(), value);
            performCandidateElimination(getSurroundingBox(), value);
        }
    }

    public void invalidateCachedValue() {
        cachedValue = -1;
    }

    public boolean isSolved() {
        return getValue() != 0;
    }

    private void performCandidateElimination(List<Square> squares, int digit) {
        for (Square square : squares) {
            if (square.isSolved()) continue;
            square.getCandidates().remove(digit);
        }
    }

    public DigitCandidates getCandidates() {
        return candidates;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getBox() {
        return (row / 3) * 3 + (col / 3);
    }

    public int getIndex() {
        return row * 9 + col;
    }

    public List<Square> getSurroundingRow() {
        return sudoku.getRow(row);
    }

    public List<Square> getSurroundingColumn() {
        return sudoku.getColumn(col);
    }

    public List<Square> getSurroundingBox() {
        return sudoku.getBox(getBox());
    }

    public String getLocationString() {
        char rowChar = (char) ('A' + row);
        char colChar = (char) ('1' + col);
        return "" + rowChar + colChar;
    }

    @Override
    public String toString() {
        return getLocationString() + ": " + (isSolved() ? getValue() : getCandidates());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (sudoku.isSelected(row, col)) {
            g2.setColor(new Color(128, 255, 128));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int value = getValue();
        if (value != 0) {
            String text = String.valueOf(value);
            Font font = new Font("Arial", Font.BOLD, getWidth() / 2);
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics(font);
            int textWidth = metrics.stringWidth(text);
            int textHeight = metrics.getAscent();
            int x = (getWidth() - textWidth) / 2;
            int y = (getHeight() + textHeight) / 2 - metrics.getDescent();
            g2.setColor(getTextColor());
            g2.drawString(text, x, y);
        } else if (candidates != null && !candidates.isEmpty()) {
            Font candidateFont = new Font("Arial", Font.PLAIN, getWidth() / 6);
            g2.setFont(candidateFont);
            FontMetrics candidateMetrics = g2.getFontMetrics(candidateFont);
            int cellSize = getWidth() / 3;
            for (int i = 1; i <= 9; i++) {
                if (candidates.contains(i)) {
                    int row = (i - 1) / 3;
                    int col = (i - 1) % 3;
                    String text = String.valueOf(i);
                    int textWidth = candidateMetrics.stringWidth(text);
                    int textHeight = candidateMetrics.getAscent();
                    int x = col * cellSize + (cellSize - textWidth) / 2;
                    int y = row * cellSize + (cellSize + textHeight) / 2 - candidateMetrics.getDescent();
                    g2.setColor(Color.GRAY);
                    g2.drawString(text, x, y);
                }
            }
        }
    }

    private Color getTextColor() {
        if (invalid) return Color.RED;
        if (given) return Color.BLACK;
        else return Color.GRAY;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Square square)) return false;
        return getIndex() == square.getIndex();
    }

    @Override
    public int hashCode() {
        return (row * 73856093) ^ (col * 19349663);
    }

    @Override
    public int compareTo(Square o) {
        return Integer.compare(getIndex(), o.getIndex());
    }
}
