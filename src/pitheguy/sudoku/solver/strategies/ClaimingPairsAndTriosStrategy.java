package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.ArrayList;
import java.util.List;

public class ClaimingPairsAndTriosStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            changed |= solveImpl(sudoku, sudoku.getRow(i));
            changed |= solveImpl(sudoku, sudoku.getColumn(i));
        }
        return changed;
    }

    private boolean solveImpl(Sudoku sudoku, List<Square> squares) {
        boolean changed = false;
        for (int digit = 1; digit <= 9; digit++) {
            if (SolverUtils.hasDigitSolved(squares, digit)) continue;
            List<Square> containedSquares = new ArrayList<>();
            for (Square square : squares) {
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                if (candidates.contains(digit)) containedSquares.add(square);
            }
            if (containedSquares.isEmpty()) continue;
            boolean allSameBox = true;
            Square first = containedSquares.getFirst();
            int boxNumber = (first.getRow() / 3) * 3 + (first.getCol() / 3);
            for (int i = 1; i < containedSquares.size(); i++) {
                Square square = containedSquares.get(i);
                allSameBox &= (square.getRow() / 3) * 3 + (square.getCol() / 3) == boxNumber;
            }
            if (allSameBox) {
                for (Square square : sudoku.getBox(boxNumber)) {
                    if (containedSquares.contains(square)) continue;
                    if (square.isSolved()) continue;
                    changed |= square.getCandidates().remove(digit);
                }
            }
        }
        return changed;
    }
}
