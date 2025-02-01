package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;

import java.util.*;

public class FinnedXWingStrategy extends SolveStrategy {
    public FinnedXWingStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        changed |= solveImpl(false);
        changed |= solveImpl(true);
        return changed;
    }

    private boolean solveImpl(boolean isRow) {
        boolean changed = false;
        for (int digit = 1; digit <= 9; digit++) {
            Map<Integer, List<Square>> hasDigit = new HashMap<>();
            for (int index = 0; index < 9; index++) {
                List<Square> squares = isRow ? sudoku.getRow(index) : sudoku.getColumn(index);
                for (Square square : squares)
                    if (!square.isSolved() && square.getCandidates().contains(digit))
                        hasDigit.computeIfAbsent(index, k -> new ArrayList<>()).add(square);
            }
            List<Integer> possibleIndexes = new ArrayList<>();
            for (int index = 0; index < 9; index++) {
                if (!hasDigit.containsKey(index)) continue;
                int size = hasDigit.get(index).size();
                if (size == 2) possibleIndexes.add(index);
            }
            if (possibleIndexes.isEmpty()) continue;
            for (int index1 : possibleIndexes) {
                for (int index2 = 0; index2 < 9; index2++) {
                    if (index1 == index2) continue;
                    if (!hasDigit.containsKey(index2)) continue;
                    if (hasDigit.get(index2).size() < 2) continue;
                    Set<Integer> index1Contained = new HashSet<>();
                    Set<Integer> index2Contained = new HashSet<>();
                    for (Square square : hasDigit.get(index1)) index1Contained.add(isRow ? square.getCol() : square.getRow());
                    for (Square square : hasDigit.get(index2)) index2Contained.add(isRow ? square.getCol() : square.getRow());
                    Set<Integer> shared = new HashSet<>(index1Contained);
                    shared.retainAll(index2Contained);
                    if (shared.size() != 2) continue;
                    Set<Integer> extra = new HashSet<>(index2Contained);
                    extra.removeAll(shared);
                    int finalIndex = index2;
                    List<Square> finSquares = extra.stream().map(i -> getSquare(isRow, finalIndex, i)).toList();

                    for (int reverseIndex : shared) {
                        List<Square> squares = isRow ? sudoku.getColumn(reverseIndex) : sudoku.getRow(reverseIndex);
                        for (Square square : squares) {
                            if (square.isSolved()) continue;
                            int index = isRow ? square.getRow() : square.getCol();
                            if (index == index1 || index == index2) continue;
                            List<Square> checkedSquares = new ArrayList<>(finSquares);
                            checkedSquares.add(square);
                            if (!SolverUtils.allInSameGroup(checkedSquares, true)) continue;
                            changed |= square.getCandidates().remove(digit);
                        }
                    }
                }
            }
        }
        return changed;
    }

    private Square getSquare(boolean isRow, int index, int pos) {
        return isRow ? sudoku.getSquare(index, pos) : sudoku.getSquare(pos, index);
    }
}
