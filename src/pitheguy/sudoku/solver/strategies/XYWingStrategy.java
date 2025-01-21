package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.SquareSet;

import java.util.ArrayList;
import java.util.List;

public class XYWingStrategy extends SolveStrategy {
    public XYWingStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        List<Square> bivalueCells = new ArrayList<>();
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                Square square = sudoku.getSquare(row, col);
                if (!square.isSolved() && square.getCandidates().count() == 2) bivalueCells.add(square);
            }
        }
        List<Match> matches = new ArrayList<>();
        for (Square square : bivalueCells) {
            List<Integer> candidates = square.getCandidates().getAllCandidates();
            int x = candidates.get(0);
            int y = candidates.get(1);
            SquareSet containsX = new SquareSet(sudoku);
            SquareSet containsY = new SquareSet(sudoku);
            for (Square otherSquare : bivalueCells) {
                if (otherSquare == square) continue;
                if (!SolverUtils.isConnected(square, otherSquare)) continue;
                if (otherSquare.getCandidates().contains(x)) containsX.add(otherSquare);
                if (otherSquare.getCandidates().contains(y)) containsY.add(otherSquare);
            }
            for (Square wing1 : containsX) {
                for (Square wing2 : containsY) {
                    List<Integer> wing1Candidates = wing1.getCandidates().getAllCandidates();
                    List<Integer> wing2Candidates = wing2.getCandidates().getAllCandidates();
                    int z1 = wing1Candidates.get(0) == x ? wing1Candidates.get(1) : wing1Candidates.get(0);
                    int z2 = wing2Candidates.get(0) == y ? wing2Candidates.get(1) : wing2Candidates.get(0);
                    if (z1 == z2 && !SolverUtils.isConnected(wing1, wing2)) matches.add(new Match(square, wing1, wing2, z1));
                }
            }
        }
        for (Match match : matches) {
            for (Square square : sudoku.getAllSquares()) {
                if (square == match.pivot || square == match.wing1 || square == match.wing2) continue;
                if (square.isSolved()) continue;
                if (SolverUtils.isConnected(square, match.wing1) && SolverUtils.isConnected(square, match.wing2))
                    changed |= square.getCandidates().remove(match.z);
            }
        }

        return changed;
    }

    private record Match(Square pivot, Square wing1, Square wing2, int z) {
    }
}
