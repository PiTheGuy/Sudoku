package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.*;

public class UniqueRectanglesStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        return solveType1(sudoku);
    }

    private boolean solveType1(Sudoku sudoku) {
        for (int row = 0; row < 9; row++) {
            Map<DigitCandidates, List<Square>> map = new HashMap<>();
            for (int col = 0; col < 9; col++) {
                Square square = sudoku.getSquare(row, col);
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                if (candidates.count() != 2) continue;
                map.computeIfAbsent(candidates, k -> new ArrayList<>()).add(square);
            }
            for (Map.Entry<DigitCandidates, List<Square>> entry : map.entrySet()) {
                DigitCandidates candidates = entry.getKey();
                List<Square> squares = entry.getValue();
                if (squares.size() != 2) continue;
                for (Square square1 : squares) {
                    Optional<Square> optional = SolverUtils.getOnlySquareThat(square1.getSurroundingColumn(),
                            square -> square != square1 && square.getCandidates().equals(candidates),
                            true);
                    if (optional.isEmpty()) continue;
                    Square square2 = squares.getFirst() == square1 ? squares.get(1) : squares.get(0);
                    Square square3 = optional.get();
                    if (square1.getBox() != square2.getBox() && square1.getBox() != square3.getBox()) continue;
                    Square forthCorner = sudoku.getSquare(square3.getRow(), square2.getCol());
                    if (forthCorner.isSolved()) continue;
                    if (!forthCorner.getCandidates().containsAll(candidates)) continue;
                    if (forthCorner.getCandidates().removeAll(candidates)) return true;
                }
            }
        }
        return false;
    }
}
