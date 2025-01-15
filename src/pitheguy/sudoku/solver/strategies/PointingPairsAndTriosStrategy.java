package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.*;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

import java.util.List;

public class PointingPairsAndTriosStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        for (int box = 0; box < 9; box++) {
            Box box1 = sudoku.boxes[box];
            List<Square> squares = box1.getSquares();
            for (int digit = 1; digit <= 9; digit++) {
                if (box1.hasDigitSolved(digit)) continue;
                for (int row = 0; row < 3; row++) changed |= solveImpl(squares, row, digit, box1, true);
                for (int col = 0; col < 3; col++) changed |= solveImpl(squares, col, digit, box1, false);
            }
        }
        return changed;
    }

    private static boolean solveImpl(List<Square> squares, int index, int digit, Box box, boolean isRow) {
        boolean changed = false;
        boolean hasDigit = false;
        for (Square square : squares) {
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if ((isRow ? square.getRow() : square.getCol()) % 3 == index) {
                if (candidates.contains(digit)) hasDigit = true;
            } else if (candidates.contains(digit)) return false;
        }
        if (hasDigit) {
            List<Square> affectedSquares = isRow ? box.getSquare(index, 0).getSurroundingRow() : box.getSquare(0, index).getSurroundingColumn();
            for (Square currentSquare : affectedSquares)
                if (!squares.contains(currentSquare)) changed |= currentSquare.getCandidates().remove(digit);
        }
        return changed;
    }
}
