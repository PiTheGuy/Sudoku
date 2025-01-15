package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.Pair;

import java.util.*;

public class XWingStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        changed |= solveImpl(sudoku, false);
        changed |= solveImpl(sudoku, true);
        return changed;
    }

    private boolean solveImpl(Sudoku sudoku, boolean isRow) {
        boolean changed = false;
        for (int digit = 1; digit <= 9; digit++) {
            List<Square>[] matchesByIndex = new List[9];
            Arrays.setAll(matchesByIndex, i -> new ArrayList<>());
            for (int index = 0; index < 9; index++) {
                List<Square> squares = isRow ? sudoku.getRow(index) : sudoku.getColumn(index);
                if (SolverUtils.hasDigitSolved(squares, digit)) continue;
                for (Square square : squares) {
                    if (square.isSolved()) continue;
                    if (square.getCandidates().contains(digit)) matchesByIndex[index].add(square);
                }
            }
            List<Pair<Integer>> validPairs = new ArrayList<>();
            for (int index1 = 0; index1 < 9; index1++) {
                if (matchesByIndex[index1].size() != 2) continue;
                for (int index2 = index1 + 1; index2 < 9; index2++)
                    if (matchesByIndex[index2].size() == 2) {
                        validPairs.add(new Pair<>(index1, index2));
                    }
            }
            Map<Pair<Integer>, Pair<Integer>> map = new HashMap<>();
            for (Pair<Integer> pair : validPairs) {
                int pos1 = isRow ? matchesByIndex[pair.first()].get(0).getCol() : matchesByIndex[pair.first()].get(0).getRow();
                int pos2 = isRow ? matchesByIndex[pair.first()].get(1).getCol() : matchesByIndex[pair.first()].get(1).getRow();
                if (!isSquareValid(sudoku, isRow, pair.first(), pos1, digit) ||
                    !isSquareValid(sudoku, isRow, pair.first(), pos2, digit) ||
                    !isSquareValid(sudoku, isRow, pair.second(), pos1, digit) ||
                    !isSquareValid(sudoku, isRow, pair.second(), pos2, digit))
                    continue;
                if ((isRow ? matchesByIndex[pair.second()].get(0).getCol() : matchesByIndex[pair.second()].get(0).getRow()) == pos1 ||
                    (isRow ? matchesByIndex[pair.second()].get(1).getCol() : matchesByIndex[pair.second()].get(1).getRow()) == pos2) {
                    map.put(pair, new Pair<>(pos1, pos2));
                }
            }
            for (Map.Entry<Pair<Integer>, Pair<Integer>> entry : map.entrySet()) {
                for (int i : new int[]{entry.getValue().first(), entry.getValue().second()}) {
                    List<Square> squares = isRow ? sudoku.getColumn(i) : sudoku.getRow(i);
                    for (Square square : squares) {
                        int index = isRow ? square.getRow() : square.getCol();
                        if (index == entry.getKey().first() || index == entry.getKey().second()) continue;
                        if (square.isSolved()) continue;
                        changed |= square.getCandidates().remove(digit);
                    }
                }
            }
        }
        return changed;
    }

    private Square getSquare(Sudoku sudoku, boolean isRow, int index, int pos) {
        return isRow ? sudoku.getSquare(index, pos) : sudoku.getSquare(pos, index);
    }

    private boolean isSquareValid(Sudoku sudoku, boolean isRow, int index, int pos, int digit) {
        Square square = getSquare(sudoku, isRow, index, pos);
        return !square.isSolved() && square.getCandidates().contains(digit);
    }
}
