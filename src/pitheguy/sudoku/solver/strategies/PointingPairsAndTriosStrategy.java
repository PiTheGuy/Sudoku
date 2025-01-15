package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.*;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

import java.util.List;

public class PointingPairsAndTriosStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        for (int box = 0; box < 9; box++) changed |= solveImpl(sudoku, box);
        return changed;
    }

    private boolean solveImpl(Sudoku sudoku, int boxNumber) {
        boolean changed = false;
        Box box = sudoku.boxes[boxNumber];
        List<Square> squares = box.getSquares();
        for (int digit = 1; digit <= 9; digit++) {
            if (box.hasDigitSolved(digit)) continue;
            for (int row = 0; row < 3; row++) {
                boolean noOutsideDigits = true;
                boolean hasDigit = false;
                for (Square square : squares) {
                    if (!square.getValue().isEmpty()) continue;
                    DigitCandidates candidates = square.getCandidates();
                    if (square.getRow() % 3 == row) {
                        if (candidates.contains(digit)) hasDigit = true;
                    } else if (candidates.contains(digit)) {
                        noOutsideDigits = false;
                        break;
                    }
                }
                if (hasDigit && noOutsideDigits)
                    for (Square currentSquare : box.getSquare(row, 0).getSurroundingRow())
                        if (!squares.contains(currentSquare)) changed |= currentSquare.getCandidates().remove(digit);
            }
            for (int col = 0; col < 3; col++) {
                boolean noOutsideDigits = true;
                boolean hasDigit = false;
                for (Square square : squares) {
                    if (!square.getValue().isEmpty()) continue;
                    DigitCandidates candidates = square.getCandidates();
                    if (square.getCol() % 3 == col) {
                        if (candidates.contains(digit)) hasDigit = true;
                    } else if (candidates.contains(digit)) {
                        noOutsideDigits = false;
                        break;
                    }
                }
                if (hasDigit && noOutsideDigits)
                    for (Square currentSquare : box.getSquare(0, col).getSurroundingColumn())
                        if (!squares.contains(currentSquare)) changed |= currentSquare.getCandidates().remove(digit);
            }
        }
        return changed;
    }
}
