package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Sudoku;

public abstract class SolveStrategy {
    protected final Sudoku sudoku;

    public SolveStrategy(Sudoku sudoku) {
        this.sudoku = sudoku;
    }

    public abstract boolean solve();
}
