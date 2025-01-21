package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;
import pitheguy.sudoku.util.SquareSet;

import java.util.ArrayList;
import java.util.List;

public class HiddenPairsStrategy extends ByGroupSolveStrategy {
    public HiddenPairsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    protected boolean solveGroup(SquareSet squares) {
        boolean changed = false;
        for (int d1 = 1; d1 <= 9; d1++) {
            if (SolverUtils.hasDigitSolved(squares, d1)) continue;
            for (int d2 = d1 + 1; d2 <= 9; d2++) {
                if (SolverUtils.hasDigitSolved(squares, d2)) continue;
                List<Square> matches = new ArrayList<>();
                for (Square square : squares) {
                    if (square.isSolved()) continue;
                    DigitCandidates candidates = square.getCandidates();
                    if (candidates.contains(d1) && candidates.contains(d2)) matches.add(square);
                    else if (candidates.contains(d1) || candidates.contains(d2)) {
                        matches.clear();
                        break;
                    }
                    if (matches.size() > 2) break;
                }
                if (matches.size() != 2) continue;
                short flags = DigitCandidates.getFlags(d1, d2);
                for (Square match : matches) changed |= match.getCandidates().setFlags(flags);
            }
        }
        return changed;
    }
}
