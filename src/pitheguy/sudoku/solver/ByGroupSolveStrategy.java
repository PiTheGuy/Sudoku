package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.util.SquareSet;

import java.util.List;

public abstract class ByGroupSolveStrategy extends SolveStrategy {

    public ByGroupSolveStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            changed |= solveGroup(sudoku.getRow(i));
            changed |= solveGroup(sudoku.getColumn(i));
            changed |= solveGroup(sudoku.getBox(i));
        }
        return changed;
    }

    protected abstract boolean solveGroup(SquareSet squares);
}
