package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

public class NakedSinglesStrategy extends SolveStrategy {
    public NakedSinglesStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        for (Square square : sudoku.getAllSquares()) {
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.count() == 1) {
                square.setValue(String.valueOf(candidates.getFirst()));
                changed = true;
            }
        }
        return changed;
    }
}
