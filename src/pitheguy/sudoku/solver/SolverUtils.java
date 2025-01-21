package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.util.SquareSet;

import java.util.*;
import java.util.function.Predicate;

public class SolverUtils {
    public static boolean isConnected(Square square1, Square square2) {
        return square1.getRow() == square2.getRow() ||
               square1.getCol() == square2.getCol() ||
               square1.getBox() == square2.getBox();
    }

    public static boolean hasDigitSolved(Iterable<Square> squares, int digit) {
        for (Square square : squares) {
            String value = square.getValue();
            if (value.isEmpty()) continue;
            if (value.charAt(0) == '0' + digit) return true;
        }
        return false;
    }

    public static Optional<Square> getOnlySquareThat(Iterable<Square> squares, Predicate<Square> condition, boolean excludeSolvedSquares) {
        Optional<Square> result = Optional.empty();
        for (Square current : squares) {
            if (excludeSolvedSquares && current.isSolved()) continue;
            if (condition.test(current)) {
                if (result.isPresent()) {
                    return Optional.empty();
                } else result = Optional.of(current);
            }
        }
        return result;
    }

    public static List<Square> getAllBivalueSquares(Sudoku sudoku) {
        List<Square> bivalueSquares = new ArrayList<>();
        sudoku.forEachSquare(square -> {
            if (!square.isSolved() && square.getCandidates().count() == 2) bivalueSquares.add(square);
        });
        return bivalueSquares;
    }
}

