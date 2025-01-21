package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.ByGroupSolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.SquareSet;

import java.util.Optional;

public class HiddenSinglesStrategy extends ByGroupSolveStrategy {
    public HiddenSinglesStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solveGroup(SquareSet squares) {
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
