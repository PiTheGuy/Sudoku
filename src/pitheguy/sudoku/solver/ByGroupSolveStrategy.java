package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;

import java.util.List;

public abstract class ByGroupSolveStrategy implements SolveStrategy {

    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            changed |= solveGroup(sudoku, sudoku.getRow(i));
            changed |= solveGroup(sudoku, sudoku.getColumn(i));
            changed |= solveGroup(sudoku, sudoku.getBox(i));
        }
        return changed;
    }

    protected abstract boolean solveGroup(Sudoku sudoku, List<Square> squares);
}
