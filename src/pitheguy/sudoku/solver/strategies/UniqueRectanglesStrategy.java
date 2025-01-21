package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UniqueRectanglesStrategy extends SolveStrategy {
    public UniqueRectanglesStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        changed |= Type1.solve(sudoku);
        changed |= Type2.solve(sudoku);
        changed |= Type3.solve(sudoku);
        changed |= Type4.solve(sudoku);
        return changed;
    }

    private static List<Rectangle> findUniqueRectangles(Sudoku sudoku) {
        List<Rectangle> rectangles = new ArrayList<>();
        findUniqueRectanglesImpl(sudoku, rectangles, true);
        findUniqueRectanglesImpl(sudoku, rectangles, false);
        return rectangles;
    }

    private static void findUniqueRectanglesImpl(Sudoku sudoku, List<Rectangle> rectangles, boolean isRow) {
        for (int index = 0; index < 9; index++) {
            Map<DigitCandidates, List<Square>> map = new HashMap<>();
            for (int pos = 0; pos < 9; pos++) {
                Square square = isRow ? sudoku.getSquare(index, pos) : sudoku.getSquare(pos, index);
                if (square.isSolved()) continue;
                DigitCandidates candidates = square.getCandidates();
                if (candidates.count() != 2) continue;
                map.computeIfAbsent(candidates, k -> new ArrayList<>()).add(square);
            }
            for (Map.Entry<DigitCandidates, List<Square>> entry : map.entrySet()) {
                DigitCandidates floorCandidates = entry.getKey();
                List<Square> squares = entry.getValue();
                if (squares.size() != 2) continue;
                for (Square square1 : squares) {
                    Square square2 = squares.getFirst() == square1 ? squares.get(1) : squares.get(0);
                    for (int otherIndex = 0; otherIndex < 9; otherIndex++) {
                        if (index == otherIndex) continue;
                        Rectangle rectangle = new Rectangle(sudoku, square1, square2, otherIndex);
                        if (rectangle.square3().isSolved() || rectangle.square4().isSolved()) continue;
                        if (square1.getBox() != square2.getBox() && square1.getBox() != rectangle.square3().getBox())
                            continue;
                        if (rectangle.square3().getCandidates().containsAll(floorCandidates) &&
                            rectangle.square4().getCandidates().containsAll(floorCandidates)) rectangles.add(rectangle);
                    }
                }
            }
        }
    }

    private static class Type1 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findUniqueRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                DigitCandidates candidates = rectangle.square1().getCandidates();
                if (!rectangle.square3().getCandidates().equals(candidates)) continue;
                if (rectangle.square4().isSolved()) continue;
                if (!rectangle.square4().getCandidates().containsAll(candidates)) continue;
                if (rectangle.square4().getCandidates().removeAll(candidates)) return true;
            }
            return false;
        }
    }

    private static class Type2 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findUniqueRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                if (!rectangle.square3().getCandidates().equals(rectangle.square4().getCandidates())) continue;
                if (rectangle.square3().getCandidates().count() != 3) continue;
                DigitCandidates candidates = rectangle.square3().getCandidates().copy();
                candidates.removeAll(rectangle.floorCandidates());
                int extraDigit = candidates.getFirst();
                List<Square> surroundingUnit = rectangle.isRowWise() ? rectangle.square3().getSurroundingRow() : rectangle.square3().getSurroundingColumn();
                Set<Square> affectedSquares = new HashSet<>(surroundingUnit);
                if (rectangle.square3().getBox() == rectangle.square4().getBox())
                    affectedSquares.addAll(rectangle.square3().getSurroundingBox());
                affectedSquares.remove(rectangle.square3());
                affectedSquares.remove(rectangle.square4());
                affectedSquares.removeIf(Square::isSolved);
                boolean changed = false;
                for (Square square : affectedSquares) changed |= square.getCandidates().remove(extraDigit);
                if (changed) return true;
            }
            return false;
        }
    }

    private static class Type3 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findUniqueRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                DigitCandidates candidates = rectangle.square3().getCandidates().or(rectangle.square4().getCandidates());
                if (candidates.count() != 4) continue;
                candidates.removeAll(rectangle.floorCandidates());
                List<Square> surroundingUnit = rectangle.isRowWise() ? rectangle.square3().getSurroundingRow() : rectangle.square3().getSurroundingColumn();
                Square otherSquare = null;
                for (Square square : surroundingUnit) {
                    if (square == rectangle.square3() || square == rectangle.square4()) continue;
                    if (square.isSolved()) continue;
                    if (square.getCandidates().equals(candidates)) {
                        otherSquare = square;
                        break;
                    }
                }
                if (otherSquare == null) continue;
                boolean changed = false;
                for (Square square : surroundingUnit) {
                    if (square.isSolved()) continue;
                    if (square == rectangle.square3() || square == rectangle.square4() || square == otherSquare)
                        continue;
                    changed |= square.getCandidates().removeAll(candidates);
                }
                if (changed) return true;
            }
            return false;
        }
    }

    private static class Type4 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findUniqueRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                List<Integer> candidates = rectangle.floorCandidates().getAllCandidates();
                for (int digit : candidates) {
                    List<Square> surroundingUnit = rectangle.isRowWise() ? rectangle.square3().getSurroundingRow() : rectangle.square3().getSurroundingColumn();
                    boolean hasNoDigit = true;
                    for (Square square : surroundingUnit) {
                        if (square.isSolved()) continue;
                        if (square == rectangle.square3() || square == rectangle.square4()) continue;
                        if (square.getCandidates().contains(digit)) {
                            hasNoDigit = false;
                            break;
                        }
                    }
                    if (rectangle.square3().getBox() == rectangle.square4().getBox()) {
                        boolean hasNoDigitInBox = true;
                        for (Square square : rectangle.square3().getSurroundingBox()) {
                            if (square.isSolved()) continue;
                            if (square == rectangle.square3() || square == rectangle.square4()) continue;
                            if (square.getCandidates().contains(digit)) {
                                hasNoDigitInBox = false;
                                break;
                            }
                        }
                        hasNoDigit |= hasNoDigitInBox;
                    }
                    if (hasNoDigit) {
                        int otherDigit = candidates.getFirst() == digit ? candidates.getLast() : candidates.getFirst();
                        boolean changed = false;
                        changed |= rectangle.square3().getCandidates().remove(otherDigit);
                        changed |= rectangle.square4().getCandidates().remove(otherDigit);
                        if (changed) return true;
                    }
                }
            }
            return false;
        }
    }

    private record Rectangle(Sudoku sudoku, Square square1, Square square2, int otherIndex) {
        public boolean isRowWise() {
            return square1.getRow() == square2.getRow();
        }

        public Square square3() {
            return isRowWise() ? sudoku.getSquare(otherIndex, square1.getCol()) : sudoku.getSquare(square1.getRow(), otherIndex);
        }

        public Square square4() {
            return isRowWise() ? sudoku.getSquare(otherIndex, square2.getCol()) : sudoku.getSquare(square2.getRow(), otherIndex);
        }

        public DigitCandidates floorCandidates() {
            return square1.getCandidates();
        }

        @Override
        public String toString() {
            return "[" + square1 + ", " + square2 + ", " + square3() + ", " + square4() + "]";
        }
    }
}
