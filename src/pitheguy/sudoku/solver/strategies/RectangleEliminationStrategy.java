package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.SquareSet;

import java.util.*;

public class RectangleEliminationStrategy extends SolveStrategy {
    public RectangleEliminationStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        for (int digit = 1; digit <= 9; digit++) {
            for (int row = 0; row < 9; row++) {
                for (int col = 0; col < 9; col++) {
                    if (solveImpl(sudoku.getSquare(row, col), digit, true)) return true;
                    if (solveImpl(sudoku.getSquare(row, col), digit, false)) return true;
                }
            }
        }
        return false;
    }

    private boolean solveImpl(Square square, int digit, boolean isRow) {
        if (square.isSolved()) return false;
        SquareSet squares = isRow ? square.getSurroundingRow() : square.getSurroundingColumn();
        if (SolverUtils.hasDigitSolved(squares, digit)) return false;
        Optional<Square> optional = SolverUtils.getOnlySquareThat(squares,
                s -> s.getCandidates().contains(digit) && s != square,
                true);
        if (optional.isEmpty()) return false;
        Square wing1 = optional.get();
        List<Square> possibleSquares = new ArrayList<>();
        for (Square current : isRow ? square.getSurroundingColumn() : square.getSurroundingRow())
            if (!current.isSolved() && current != wing1 && current.getCandidates().contains(digit))
                possibleSquares.add(current);
        if (possibleSquares.size() < 2) return false;
        wing2:
        for (Square wing2 : possibleSquares) {
            if (wing2 == square) continue;
            Square forthCorner = isRow ? sudoku.getSquare(wing2.getRow(), wing1.getCol()) : sudoku.getSquare(wing1.getRow(), wing2.getCol());;
            if (forthCorner.getBox() == wing1.getBox() || forthCorner.getBox() == wing2.getBox()) continue;
            SquareSet box = forthCorner.getSurroundingBox();
            if (SolverUtils.hasDigitSolved(box, digit)) continue;
            for (Square current : box) {
                if (current.isSolved()) continue;
                if (!current.getCandidates().contains(digit)) continue;
                if (!SolverUtils.isConnected(current, wing1) &&
                    !SolverUtils.isConnected(current, wing2)) {
                    continue wing2;
                }
            }
            if (wing2.getCandidates().remove(digit)) return true;
        }
        return false;
    }
}
