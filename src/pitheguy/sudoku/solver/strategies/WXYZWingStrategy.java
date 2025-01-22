package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;

import java.util.ArrayList;
import java.util.List;

public class WXYZWingStrategy extends SolveStrategy {
    public WXYZWingStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        List<Square> squares = new ArrayList<>(sudoku.getAllSquares());
        squares.removeIf(Square::isSolved);
        squares.removeIf(square -> square.getCandidates().count() > 4);
        if (squares.size() < 4) return false;
        List<PossibleMatch> possibleMatches = new ArrayList<>();
        getPossibleMatches(squares, new ArrayList<>(), DigitCandidates.EMPTY, possibleMatches, 0);
        if (possibleMatches.isEmpty()) return false;
        for (PossibleMatch possibleMatch : possibleMatches) {
            List<Integer> restrictedCommons = new ArrayList<>();
            for (int digit : possibleMatch.candidates.getAllCandidates()) {
                List<Square> containedSquares = new ArrayList<>();
                for (Square square : possibleMatch.squares)
                    if (square.getCandidates().contains(digit)) containedSquares.add(square);
                boolean areAllInstancesConnected = true;
                pairs:
                for (Square square1 : containedSquares) {
                    for (Square square2 : containedSquares) {
                        if (square1 == square2) continue;
                        if (!SolverUtils.isConnected(square1, square2)) {
                            areAllInstancesConnected = false;
                            break pairs;
                        }
                    }
                }
                if (!areAllInstancesConnected) {
                    restrictedCommons.add(digit);
                    if (restrictedCommons.size() > 1) break;
                }
            }
            if (restrictedCommons.size() != 1) continue;
            int z = restrictedCommons.getFirst();
            List<Square> containsZ = new ArrayList<>();
            for (Square square : possibleMatch.squares) if (square.getCandidates().contains(z)) containsZ.add(square);
            boolean changed = false;
            squares:
            for (Square square : sudoku.getAllSquares()) {
                if (possibleMatch.squares.contains(square)) continue;
                if (square.isSolved()) continue;
                for (Square current : containsZ) if (!SolverUtils.isConnected(square, current)) continue squares;
                changed |= square.getCandidates().remove(z);
            }
            if (changed) return true;
        }
        return false;
    }

    private void getPossibleMatches(
            List<Square> squares,
            List<Square> currentCombination,
            DigitCandidates candidates,
            List<PossibleMatch> possibleMatches,
            int startIndex) {
        if (candidates.count() > 4) return;
        if (currentCombination.size() == 4) {
            if (candidates.count() == 4) possibleMatches.add(new PossibleMatch(new ArrayList<>(currentCombination), candidates));
            return;
        }

        for (int i = startIndex; i < squares.size(); i++) {
            Square square = squares.get(i);
            currentCombination.add(square);
            DigitCandidates newCandidates = candidates.or(square.getCandidates());
            getPossibleMatches(squares, currentCombination, newCandidates, possibleMatches, i + 1);
            currentCombination.removeLast();
        }
    }

    private record PossibleMatch(List<Square> squares, DigitCandidates candidates) {}
}
