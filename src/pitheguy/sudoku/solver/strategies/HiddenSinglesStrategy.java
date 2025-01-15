package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.List;

public class HiddenSinglesStrategy extends ByGroupSolveStrategy {
    @Override
    public boolean solveGroup(Sudoku sudoku, List<Square> squares) {
        boolean changed = false;
        for (int i = 1; i <= 9; i++) {
            if (SolverUtils.hasDigitSolved(squares, i)) continue;
            Square containedSquare = null;
            for (Square square : squares) {
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                if (candidates.contains(i)) {
                    if (containedSquare != null) {
                        containedSquare = null;
                        break;
                    } else containedSquare = square;
                }
            }
            if (containedSquare != null) {
                containedSquare.setValue(String.valueOf(i));
                changed = true;
            }
        }
        return changed;
    }
}
