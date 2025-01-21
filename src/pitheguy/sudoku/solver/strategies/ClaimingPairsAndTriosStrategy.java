package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.SquareSet;

import java.util.ArrayList;
import java.util.List;

public class ClaimingPairsAndTriosStrategy extends SolveStrategy {
    public ClaimingPairsAndTriosStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            changed |= solveImpl(sudoku.getRow(i));
            changed |= solveImpl(sudoku.getColumn(i));
        }
        return changed;
    }

    private boolean solveImpl(SquareSet squares) {
        boolean changed = false;
        for (int digit = 1; digit <= 9; digit++) {
            if (SolverUtils.hasDigitSolved(squares, digit)) continue;
            List<Square> containedSquares = new ArrayList<>();
            for (Square square : squares) {
                if (square.isSolved()) continue;
                if (square.getCandidates().contains(digit)) containedSquares.add(square);
            }
            if (containedSquares.isEmpty()) continue;
            boolean allSameBox = true;
            Square first = containedSquares.getFirst();
            int boxNumber = first.getBox();
            for (int i = 1; i < containedSquares.size(); i++) {
                Square square = containedSquares.get(i);
                allSameBox &= square.getBox() == boxNumber;
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
