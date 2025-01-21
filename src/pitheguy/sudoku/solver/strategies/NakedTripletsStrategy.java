package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.ByGroupSolveStrategy;
import pitheguy.sudoku.solver.DigitCandidates;

import java.util.ArrayList;
import java.util.List;

public class NakedTripletsStrategy extends ByGroupSolveStrategy {
    public NakedTripletsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    protected boolean solveGroup(List<Square> squares) {
        boolean changed = false;
        for (int i = 0; i < 7; i++) {
            Square square = squares.get(i);
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.count() < 2 || candidates.count() > 3) continue;
            List<Match> matches = new ArrayList<>();
            for (int j = i + 1; j < 8; j++) {
                Square otherSquare = squares.get(j);
                if (otherSquare.isSolved()) continue;
                for (int k = j + 1; k < 9; k++) {
                    Square otherSquare2 = squares.get(k);
                    if (otherSquare2.isSolved()) continue;
                    DigitCandidates totalCandidates = candidates.or(otherSquare.getCandidates()).or(otherSquare2.getCandidates());
                    if (totalCandidates.count() != 3) continue;
                    matches.add(new Match(List.of(square, otherSquare, otherSquare2), totalCandidates));
                }
            }
            for (Match match : matches) {
                List<Integer> values = match.totalCandidates.getAllCandidates();
                for (Square currentSquare : squares) {
                    if (currentSquare.isSolved()) continue;
                    if (match.squares.contains(currentSquare)) continue;
                    for (int value : values) changed |= currentSquare.getCandidates().remove(value);
                }
            }
        }
        return changed;
    }
    private record Match(List<Square> squares, DigitCandidates totalCandidates) {}
}
