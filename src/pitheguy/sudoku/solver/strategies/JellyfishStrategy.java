package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;

import java.util.*;

public class JellyfishStrategy extends SolveStrategy {
    public JellyfishStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        changed |= solveImpl(true);
        changed |= solveImpl(false);
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
                if (size >= 2 && size <= 4) possibleIndexes.add(index);
            }
            if (possibleIndexes.size() < 4) continue;
            for (int i1 = 0; i1 < possibleIndexes.size(); i1++) {
                int index1 = possibleIndexes.get(i1);
                for (int i2 = i1 + 1; i2 < possibleIndexes.size(); i2++) {
                    int index2 = possibleIndexes.get(i2);
                    for (int i3 = i2 + 1; i3 < possibleIndexes.size(); i3++) {
                        int index3 = possibleIndexes.get(i3);
                        for (int i4 = i3 + 1; i4 < possibleIndexes.size(); i4++) {
                            int index4 = possibleIndexes.get(i4);
                            Set<Integer> reverseIndexes = new HashSet<>();
                            for (Square square : hasDigit.get(index1)) reverseIndexes.add(isRow ? square.getCol() : square.getRow());
                            for (Square square : hasDigit.get(index2)) reverseIndexes.add(isRow ? square.getCol() : square.getRow());
                            for (Square square : hasDigit.get(index3)) reverseIndexes.add(isRow ? square.getCol() : square.getRow());
                            for (Square square : hasDigit.get(index4)) reverseIndexes.add(isRow ? square.getCol() : square.getRow());
                            if (reverseIndexes.size() != 4) continue;
                            for (int reverseIndex : reverseIndexes) {
                                List<Square> squares = isRow ? sudoku.getColumn(reverseIndex) : sudoku.getRow(reverseIndex);
                                for (Square square : squares) {
                                    if (square.isSolved()) continue;
                                    int index = isRow ? square.getRow() : square.getCol();
                                    if (index == index1 || index == index2 || index == index3 || index == index4) continue;
                                    changed |= square.getCandidates().remove(digit);
                                }
                            }
                        }
                    }
                }
            }
        }
        return changed;
    }
}
