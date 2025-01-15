package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.ArrayList;
import java.util.List;

public class XYZWingStrategy implements SolveStrategy {
    public boolean solve(Sudoku sudoku) {
        boolean[] changed = {false};
        List<Square> trivalueCells = new ArrayList<>();
        List<Square> bivalueCells = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                Square square = sudoku.getSquare(row, col);
                if (square.isSolved()) continue;
                if (square.getCandidates().count() == 2) bivalueCells.add(square);
                if (square.getCandidates().count() == 3) trivalueCells.add(square);
            }
        }
        List<Match> matches = new ArrayList<>();
        for (Square pivot : trivalueCells) {
            List<Integer> candidates = pivot.getCandidates().getAllCandidates();
            int x = candidates.get(0);
            int y = candidates.get(1);
            int z = candidates.get(2);
            List<Square> containsXY = new ArrayList<>();
            List<Square> containsXZ = new ArrayList<>();
            for (Square wing : bivalueCells) {
                if (!SolverUtils.isConnected(pivot, wing)) continue;
                DigitCandidates wingCandidates = wing.getCandidates();
                if (wingCandidates.contains(x) && wingCandidates.contains(y)) containsXY.add(wing);
                if (wingCandidates.contains(x) && wingCandidates.contains(z)) containsXZ.add(wing);
            }
            for (Square wing1 : containsXY) {
                for (Square wing2 : containsXZ) {
                    if (wing1 == wing2 || SolverUtils.isConnected(wing1, wing2)) continue;
                    matches.add(new Match(pivot, wing1, wing2, x));
                }
            }
        }
        for (Match match : matches) {
            sudoku.forEachSquare(square -> {
                if (square == match.pivot || square == match.wing1 || square == match.wing2) return;
                if (SolverUtils.isConnected(square, match.pivot) &&
                    SolverUtils.isConnected(square, match.wing1) &&
                    SolverUtils.isConnected(square, match.wing2)) {
                    changed[0] |= square.getCandidates().remove(match.x);
                }
            });
        }

        return changed[0];
    }

    private record Match(Square pivot, Square wing1, Square wing2, int x) {
    }
}
