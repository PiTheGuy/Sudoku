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
            if (s1.getRow() / 3 == s2.getRow() / 3) {
                //System.out.println("Match found: " + s1 + " and " + s2);
                int boxRow = s1.getRow() / 3;
                List<Integer> possibleRows = new ArrayList<>(List.of(boxRow * 3, boxRow * 3 + 1, boxRow * 3 + 2));
                possibleRows.remove(Integer.valueOf(s1.getRow()));
                possibleRows.remove(Integer.valueOf(s2.getRow()));
                List<Integer> possibleBoxCols = new ArrayList<>(List.of(0, 1, 2));
                possibleBoxCols.remove(Integer.valueOf(s1.getBox() % 3));
                possibleBoxCols.remove(Integer.valueOf(s2.getBox() % 3));
                int row = possibleRows.getFirst();
                int boxCol = possibleBoxCols.getFirst();
                List<Square> squares = new ArrayList<>();
                for (int i = 0; i < 3; i++) squares.add(sudoku.getSquare(row, boxCol * 3 + i));
                DigitCandidates totalCandidates = DigitCandidates.EMPTY;
                for (Square square : squares) {
                    if (square.isSolved()) continue;
                    totalCandidates = totalCandidates.or(square.getCandidates());
                }
                DigitCandidates sharedCandidates = totalCandidates.and(s1.getCandidates());
                if (sharedCandidates.count() != 1) continue;
                int shared = sharedCandidates.getFirst();
                boolean valid = true;
                DigitCandidates c = s1.getCandidates().copy();
                c.remove(shared);
                int other = c.getFirst();
                for (Square square : squares) {
                    if (square.getValue().equals(String.valueOf(other))) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) continue;
                boolean changed = false;
                for (Square square : sudoku.getAllSquares()) {
                    if (square.isSolved()) continue;
                    if (SolverUtils.isConnected(square, s1) && SolverUtils.isConnected(square, s2))
                        changed |= square.getCandidates().remove(shared);
                }
                if (changed) return true;
            }
        }
        return false;
    }

    private static boolean isInSameChute(Square s1, Square s2) {
        return s1.getRow() / 3 == s2.getRow() / 3 || s1.getCol() / 3 == s2.getCol() / 3;
    }
}
