package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

public class NakedSinglesStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                Square square = sudoku.getSquare(row, col);
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                if (candidates.count() == 1) {
                    square.setValue(String.valueOf(candidates.getFirst()));
                    changed = true;
                }
            }
        }
        return changed;
    }
}
