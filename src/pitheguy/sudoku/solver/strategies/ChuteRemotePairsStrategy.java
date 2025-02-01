package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;
import pitheguy.sudoku.util.Pair;
import pitheguy.sudoku.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ChuteRemotePairsStrategy extends SolveStrategy {
    public ChuteRemotePairsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        List<Square> bivalueSquares = SolverUtils.getAllBivalueSquares(sudoku);
        for (Pair<Square> pair : Util.getAllPairs(bivalueSquares)) {
            Square s1 = pair.first();
            Square s2 = pair.second();
            if (SolverUtils.isConnected(s1, s2)) continue;
            if (!isInSameChute(s1, s2)) continue;
            if (!s1.getCandidates().equals(s2.getCandidates())) continue;
            if (s1.getRow() / 3 == s2.getRow() / 3) if (process(s1, s2, true)) return true;
            else if (s1.getCol() / 3 == s2.getCol() / 3) if (process(s1, s2, false)) return true;
        }
        return false;
    }

    private boolean process(Square s1, Square s2, boolean isRow) {
        //System.out.println("Match found: " + s1 + " and " + s2);
        int boxIndex = getIndex(s1, isRow) / 3;
        List<Integer> possibleIndexes = new ArrayList<>(List.of(boxIndex * 3, boxIndex * 3 + 1, boxIndex * 3 + 2));
        possibleIndexes.remove(Integer.valueOf(getIndex(s1, isRow)));
        possibleIndexes.remove(Integer.valueOf(getIndex(s2, isRow)));
        List<Integer> possibleBoxPoses = new ArrayList<>(List.of(0, 1, 2));
        possibleBoxPoses.remove(Integer.valueOf(s1.getBox() % 3));
        possibleBoxPoses.remove(Integer.valueOf(s2.getBox() % 3));
        int index = possibleIndexes.getFirst();
        int boxPos = possibleBoxPoses.getFirst();
        List<Square> squares = new ArrayList<>();
        for (int i = 0; i < 3; i++) squares.add(isRow ? sudoku.getSquare(index, boxPos * 3 + i) : sudoku.getSquare(boxPos * 3 + i, index));
        DigitCandidates totalCandidates = DigitCandidates.EMPTY;
        for (Square square : squares) {
            if (square.isSolved()) continue;
            totalCandidates = totalCandidates.or(square.getCandidates());
        }
        DigitCandidates sharedCandidates = totalCandidates.and(s1.getCandidates());
        if (sharedCandidates.count() != 1) return false;
        int shared = sharedCandidates.getFirst();
        boolean valid = true;
        DigitCandidates c = s1.getCandidates().copy();
        c.remove(shared);
        int other = c.getFirst();
        for (Square square : squares) {
            if (square.getValue() == other) {
                valid = false;
                break;
            }
        }
        if (!valid) return false;
        boolean changed = false;
        for (Square square : sudoku.getAllSquares()) {
            if (square.isSolved()) continue;
            if (SolverUtils.isConnected(square, s1) && SolverUtils.isConnected(square, s2))
                changed |= square.getCandidates().remove(shared);
        }
        return changed;
    }

    private static int getIndex(Square square, boolean isRow) {
        return isRow ? square.getRow() : square.getCol();
    }

    private static boolean isInSameChute(Square s1, Square s2) {
        return s1.getRow() / 3 == s2.getRow() / 3 || s1.getCol() / 3 == s2.getCol() / 3;
    }
}
