package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.ByGroupSolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;

import java.util.List;
import java.util.Optional;

public class HiddenSinglesStrategy extends ByGroupSolveStrategy {
    @Override
    public boolean solveGroup(Sudoku sudoku, List<Square> squares) {
        boolean changed = false;
        for (int digit = 1; digit <= 9; digit++) {
            if (SolverUtils.hasDigitSolved(squares, digit)) continue;
            int finalDigit = digit;
            Optional<Square> hiddenSingle = SolverUtils.getOnlySquareThat(squares,
                    square -> square.getCandidates().contains(finalDigit),
                    true);
            if (hiddenSingle.isPresent()) {
                hiddenSingle.get().setValue(String.valueOf(digit));
                changed = true;
            }
        }
        return changed;
    }
}
