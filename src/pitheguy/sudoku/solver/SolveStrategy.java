package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Sudoku;

public interface SolveStrategy {
    boolean solve(Sudoku sudoku);
}
