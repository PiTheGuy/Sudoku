package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SolverUtils {
    public static boolean isConnected(Square square1, Square square2) {
        return square1.getRow() == square2.getRow() ||
               square1.getCol() == square2.getCol() ||
               square1.getBox() == square2.getBox();
    }

    public static boolean hasDigitSolved(List<Square> squares, int digit) {
        for (Square square : squares) {
            String value = square.getValue();
            if (value.isEmpty()) continue;
            if (value.charAt(0) == '0' + digit) return true;
        }
        return false;
    }

    public static Optional<Square> getOnlySquareThat(List<Square> squares, Predicate<Square> condition, boolean excludeSolvedSquares) {
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
}

