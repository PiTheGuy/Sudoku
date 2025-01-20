package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.Pair;
import pitheguy.sudoku.util.SquareSet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AlmostLockedSetStrategy implements SolveStrategy {
    private static final boolean DEBUG = false;

    @Override
    public boolean solve(Sudoku sudoku) {
        List<AlmostLockedSet> almostLockedSets = findAlmostLockedSets(sudoku);
        if (almostLockedSets.size() < 2) return false;
        for (int i = 0; i < almostLockedSets.size(); i++) {
            AlmostLockedSet set1 = almostLockedSets.get(i);
            sets:
            for (int j = i + 1; j < almostLockedSets.size(); j++) {
                AlmostLockedSet set2 = almostLockedSets.get(j);
                DigitCandidates commonCandidates = set1.candidates.and(set2.candidates);
                if (commonCandidates.isEmpty()) continue;
                if (set1.candidates.or(set2.candidates).count() == Math.max(set1.candidates.count(),set2.candidates.count())) continue;
                List<Pair<Square>> connectedPairs = new ArrayList<>();
                for (Square square1 : set1.squares) {
                    for (Square square2 : set2.squares) {
                        if (square1 == square2) continue sets;
                        if (SolverUtils.isConnected(square1, square2)) connectedPairs.add(new Pair<>(square1, square2));
                    }
                }
                if (connectedPairs.isEmpty()) continue;
                List<Integer> restrictedCommons = new ArrayList<>();
                for (int x : commonCandidates.getAllCandidates()) {
                    for (Pair<Square> pair : connectedPairs) {
                        if (pair.first().getCandidates().and(pair.second().getCandidates()).contains(x)) {
                            restrictedCommons.add(x);
                            break;
                        }
                    }
                }
                restrictedCommons.removeIf(rc -> {
                   SquareSet set1Contains = new SquareSet(sudoku);
                   SquareSet set2Contains = new SquareSet(sudoku);
                   for (Square square : set1.squares) if (square.getCandidates().contains(rc)) set1Contains.add(square);
                   for (Square square : set2.squares) if (square.getCandidates().contains(rc)) set2Contains.add(square);
                   for (Square square1 : set1Contains) {
                       for (Square square2 : set2Contains) {
                           if (!SolverUtils.isConnected(square1, square2)) return true;
                       }
                   }
                   return false;
                });
                if (restrictedCommons.isEmpty()) continue;
                List<Integer> otherCommons = commonCandidates.getAllCandidates();
                otherCommons.removeAll(restrictedCommons);
                if (otherCommons.isEmpty()) continue;
                boolean changed = false;
                for (int z : otherCommons) {
                    SquareSet bothSets = new SquareSet(sudoku);
                    bothSets.addAll(set1.squares);
                    bothSets.addAll(set2.squares);
                    SquareSet containsZ = new SquareSet(sudoku);
                    boolean logged = false;
                    for (Square square : bothSets) if (square.getCandidates().contains(z)) containsZ.add(square);
                    squares:
                    for (Square square : sudoku.getAllSquares()) {
                        if (square.isSolved()) continue;
                        if (bothSets.contains(square)) continue;
                        for (Square s : containsZ) if (!SolverUtils.isConnected(square, s)) continue squares;
                        if (DEBUG) logged = debugLog(z, square, logged, set1, set2, connectedPairs, restrictedCommons);
                        changed |= square.getCandidates().remove(z);
                    }
                }
                if (changed) return true;
            }
        }
        return false;
    }

    private static boolean debugLog(int z, Square square, boolean logged, AlmostLockedSet set1, AlmostLockedSet set2, List<Pair<Square>> connectedPairs, List<Integer> restrictedCommons) {
        if (!logged) {
            logged = true;
            System.out.println("Removing " + z + " due to " + set1 + " and " + set2);
            System.out.println("Connected Pairs: " + connectedPairs);
            System.out.println("Restricted Commons: " + restrictedCommons);
        }
        System.out.println("Removing from " + square);
        return logged;
    }

    private List<AlmostLockedSet> findAlmostLockedSets(Sudoku sudoku) {
        List<AlmostLockedSet> almostLockedSets = new ArrayList<>();
        findALSForGroup(sudoku, almostLockedSets, sudoku::getRow);
        findALSForGroup(sudoku, almostLockedSets, sudoku::getColumn);
        findALSForGroup(sudoku, almostLockedSets, sudoku::getBox);
        return almostLockedSets;
    }

    private void findALSForGroup(Sudoku sudoku, List<AlmostLockedSet> almostLockedSets, Function<Integer, List<Square>> groupGetter) {
        for (int index = 0; index < 9; index++) {
            List<Square> squares = groupGetter.apply(index);
            squares.removeIf(Square::isSolved);
            squares.removeIf(square -> square.getCandidates().count() == 1);
            for (int size = 2; size <= 5; size++)
                findALSForSize(sudoku, squares, size, 0, new ArrayList<>(), DigitCandidates.EMPTY, almostLockedSets);
        }
    }

    private void findALSForSize(Sudoku sudoku, List<Square> squares, int size, int start, List<Square> current, DigitCandidates candidates, List<AlmostLockedSet> result) {
        if (size > squares.size()) return;
        if (candidates.count() > size + 1) return;
        if (current.size() == size) {
            if (candidates.count() == size + 1)
                result.add(new AlmostLockedSet(new SquareSet(sudoku, current.toArray(new Square[0])), candidates));
            return;
        }
        for (int i = start; i < squares.size(); i++) {
            Square square = squares.get(i);
            if (!current.isEmpty() && current.stream().allMatch(s -> s.getCandidates().and(square.getCandidates()).isEmpty()))
                continue;
            current.add(square);
            DigitCandidates newCandidates = candidates.or(square.getCandidates());
            findALSForSize(sudoku, squares, size, i + 1, current, newCandidates, result);
            current.removeLast();
        }
    }

    private record AlmostLockedSet(SquareSet squares, DigitCandidates candidates) {}
}
