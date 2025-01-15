package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.ByGroupSolveStrategy;
import pitheguy.sudoku.solver.DigitCandidates;

import java.util.ArrayList;
import java.util.List;

public class NakedPairsStrategy extends ByGroupSolveStrategy {
    @Override
    protected boolean solveGroup(Sudoku sudoku, List<Square> squares) {
        boolean changed = false;
        for (int i = 0; i < squares.size() - 1; i++) {
            Square square = squares.get(i);
            if (!square.getValue().isEmpty()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.count() != 2) continue;
            List<Square> matches = new ArrayList<>();
            matches.add(square);
            for (int j = i + 1; j < squares.size(); j++) {
                Square otherSquare = squares.get(j);
                if (!otherSquare.getValue().isEmpty()) continue;
                if (otherSquare.getCandidates().equals(candidates)) matches.add(otherSquare);
                if (matches.size() > 2) break;
            }
            if (matches.size() != 2) continue;
            List<Integer> values = candidates.getAllCandidates();
            for (Square currentSquare : squares) {
                if (!currentSquare.getValue().isEmpty()) continue;
                if (matches.contains(currentSquare)) continue;
                for (int value : values) changed |= currentSquare.getCandidates().remove(value);
            }
        }
        return changed;
    }
}
