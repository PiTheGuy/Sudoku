package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;
import pitheguy.sudoku.util.SquareSet;

import java.util.ArrayList;
import java.util.List;

public class XYZWingStrategy extends SolveStrategy {
    public XYZWingStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    public boolean solve() {
        boolean changed = false;
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
            for (int z : candidates) {
                List<Integer> otherCandidates = new ArrayList<>(candidates);
                otherCandidates.remove((Integer) z);
                int x = otherCandidates.get(0);
                int y = otherCandidates.get(1);
                SquareSet containsXZ = new SquareSet(sudoku);
                SquareSet containsYZ = new SquareSet(sudoku);
                for (Square wing : bivalueCells) {
                    if (!SolverUtils.isConnected(pivot, wing)) continue;
                    DigitCandidates wingCandidates = wing.getCandidates();
                    if (wingCandidates.contains(x) && wingCandidates.contains(z)) containsXZ.add(wing);
                    if (wingCandidates.contains(y) && wingCandidates.contains(z)) containsYZ.add(wing);
                }
                for (Square wing1 : containsXZ) {
                    for (Square wing2 : containsYZ) {
                        if (wing1 == wing2 || SolverUtils.isConnected(wing1, wing2)) continue;
                        matches.add(new Match(pivot, wing1, wing2, z));
                    }
                }
            }
        }
        for (Match match : matches) {
            for (Square square : sudoku.getAllSquares()) {
                if (square.isSolved()) continue;
                if (square == match.pivot || square == match.wing1 || square == match.wing2) continue;
                if (SolverUtils.isConnected(square, match.pivot) &&
                    SolverUtils.isConnected(square, match.wing1) &&
                    SolverUtils.isConnected(square, match.wing2)) {
                    changed |= square.getCandidates().remove(match.z);
                }
            }
        }

        return changed;
    }

    private record Match(Square pivot, Square wing1, Square wing2, int z) {
    }
}
