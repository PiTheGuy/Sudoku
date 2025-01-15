package pitheguy.sudoku.gui;

import pitheguy.sudoku.solver.SolverUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Box extends JPanel {
    private final Sudoku sudoku;
    private final Square[] squares = new Square[9];

    public Box(Sudoku sudoku, int row, int col) {
        this.sudoku = sudoku;
        setLayout(new GridLayout(3, 3));
        setBorder(new LineBorder(Color.BLACK, 2));
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                squares[x * 3 + y] = new Square(sudoku, row * 3 + x, col * 3 + y);
                squares[x * 3 + y].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                add(squares[x * 3 + y]);
            }
        }
    }

    public Square getSquare(int x, int y) {
        return squares[x * 3 + y];
    }

    public Square getSquare(int index) {
        return squares[index];
    }

    public List<Square> getSquares() {
        return Arrays.stream(squares).toList();
    }

    public boolean hasDigitSolved(int digit) {
        return SolverUtils.hasDigitSolved(List.of(squares), digit);
    }
}
