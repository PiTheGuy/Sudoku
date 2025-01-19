package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlignedPairExclusionStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        List<Square> allSquares = sudoku.getAllSquares();
        for (Square square1 : allSquares) {
            if (square1.isSolved()) continue;
            for (Square square2 : allSquares) {
                if (square2.isSolved()) continue;
                if (square1 == square2) continue;
                if (!SolverUtils.isConnected(square1, square2)) continue;
                if (square1.getCandidates().count() == 1 || square2.getCandidates().count() == 1) continue;
                List<Integer> square1Candidates = square1.getCandidates().getAllCandidates();
                List<Integer> square2Candidates = square2.getCandidates().getAllCandidates();
                Map<Pair<Integer>, Boolean> validPairs = new HashMap<>();
                for (int digit1 : square1Candidates) {
                    for (int digit2 : square2Candidates) {
                        if (digit1 == digit2) continue;
                        List<UniquePair<DigitCandidates, Square>> connectedSquareCandidateInfo = getConnectedSquareCandidates(square1, square2);
                        List<DigitCandidates> connectedSquareCandidates = connectedSquareCandidateInfo.stream().map(UniquePair::first).toList();
                        if (connectedSquareCandidates.isEmpty()) continue;
                        for (DigitCandidates candidates : connectedSquareCandidates) {
                            candidates.remove(digit1);
                            candidates.remove(digit2);
                        }
                        for (UniquePair<DigitCandidates, Square> candidateInfo : connectedSquareCandidateInfo) {
                            DigitCandidates candidates = candidateInfo.first();
                            Square square = candidateInfo.second();
                            if (candidates.count() != 1) continue;
                            for (UniquePair<DigitCandidates, Square> otherCandidateInfo : connectedSquareCandidateInfo) {
                                if (candidateInfo == otherCandidateInfo) continue;
                                if (SolverUtils.isConnected(square, otherCandidateInfo.second()))
                                    otherCandidateInfo.first().remove(candidates.getFirst());
                            }
                        }
                        boolean valid = connectedSquareCandidates.stream().noneMatch(DigitCandidates::isEmpty);
                        validPairs.put(new Pair<>(digit1, digit2), valid);
                    }
                }
                for (int digit = 1; digit <= 9; digit++) {
                    boolean found = false;
                    boolean valid = true;
                    for (Pair<Integer> pair : validPairs.keySet()) {
                        if (!pair.asList().contains(digit)) continue;
                        found = true;
                        if (validPairs.get(pair)) {
                            valid = false;
                            break;
                        }
                    }
                    if (found && valid) {
                        boolean changed = false;
                        changed |= square1.getCandidates().remove(digit);
                        changed |= square2.getCandidates().remove(digit);
                        if (changed) return true;
                    }
                }

            }
        }
        return false;
    }

    private List<UniquePair<DigitCandidates, Square>> getConnectedSquareCandidates(Square square1, Square square2) {
        Set<Square> squares = new HashSet<>();
        if (square1.getRow() == square2.getRow()) squares.addAll(square1.getSurroundingRow());
        if (square1.getCol() == square2.getCol()) squares.addAll(square1.getSurroundingColumn());
        if (square1.getBox() == square2.getBox()) squares.addAll(square1.getSurroundingBox());
        squares.remove(square1);
        squares.remove(square2);
        squares.removeIf(Square::isSolved);
        List<UniquePair<DigitCandidates, Square>> list = new ArrayList<>();
        for (Square square : squares) {
            DigitCandidates candidates = square.getCandidates();
            if (candidates.count() > 1) {
                DigitCandidates copy = candidates.copy();
                list.add(new UniquePair<>(copy, square));
            }
        }
        return list;
    }
}
