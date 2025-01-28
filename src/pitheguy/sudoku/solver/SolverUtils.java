package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;

import java.util.*;
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

    public static List<Square> getAllBivalueSquares(Sudoku sudoku) {
        List<Square> bivalueSquares = new ArrayList<>();
        sudoku.forEachSquare(square -> {
            if (!square.isSolved() && square.getCandidates().count() == 2) bivalueSquares.add(square);
        });
        return bivalueSquares;
    }

    public static boolean allInSameGroup(List<Square> squares, boolean allowBox) {
        if (squares.size() < 2) return true;
        boolean sameRow = true;
        int row = squares.getFirst().getRow();
        for (int i = 1; i < squares.size(); i++) {
            if (squares.get(i).getRow() != row) {
                sameRow = false;
                break;
            }
        }
        boolean sameCol = true;
        int col = squares.getFirst().getCol();
        for (int i = 1; i < squares.size(); i++) {
            if (squares.get(i).getCol() != col) {
                sameCol = false;
                break;
            }
        }
        if (sameRow || sameCol) return true;
        if (!allowBox) return false;
        int box = squares.getFirst().getBox();
        for (int i = 1; i < squares.size(); i++) if (squares.get(i).getBox() != box) return false;
        return true;
    }

    public static boolean allInSameGroup(List<Square> squares, GroupType groupType) {
        if (squares.size() < 2) return true;
        int index = groupType.get(squares.getFirst());
        for (int i = 1; i < squares.size(); i++) if (groupType.get(squares.get(i)) != index) return false;
        return true;
    }
}

