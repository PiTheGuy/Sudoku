package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.ByGroupSolveStrategy;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.util.SquareSet;

import java.util.ArrayList;
import java.util.List;

public class NakedPairsStrategy extends ByGroupSolveStrategy {
    public NakedPairsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    protected boolean solveGroup(SquareSet squares) {
        boolean changed = false;
        List<Square> squaresList = squares.toList();
        for (int i = 0; i < squaresList.size() - 1; i++) {
            Square square = squaresList.get(i);
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.count() != 2) continue;
            List<Square> matches = new ArrayList<>();
            matches.add(square);
            for (int j = i + 1; j < squaresList.size(); j++) {
                Square otherSquare = squaresList.get(j);
                if (otherSquare.isSolved()) continue;
                if (otherSquare.getCandidates().equals(candidates)) matches.add(otherSquare);
                if (matches.size() > 2) break;
            }
            if (matches.size() != 2) continue;
            List<Integer> values = candidates.getAllCandidates();
            for (Square currentSquare : squaresList) {
                if (currentSquare.isSolved()) continue;
                if (matches.contains(currentSquare)) continue;
                for (int value : values) changed |= currentSquare.getCandidates().remove(value);
            }
        }
        return changed;
    }
}
