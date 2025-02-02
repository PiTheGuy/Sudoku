package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;

import java.util.*;

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
        changed |= Type5.solve(sudoku);
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
                        Rectangle rectangle = new Rectangle(square1, square2, otherIndex);
                        if (rectangle.square3(sudoku).isSolved() || rectangle.square4(sudoku).isSolved()) continue;
                        if (square1.getBox() != square2.getBox() && square1.getBox() != rectangle.square3(sudoku).getBox())
                            continue;
                        if (rectangle.square3(sudoku).getCandidates().containsAll(floorCandidates) &&
                            rectangle.square4(sudoku).getCandidates().containsAll(floorCandidates)) rectangles.add(rectangle);
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
                if (!rectangle.square3(sudoku).getCandidates().equals(candidates)) continue;
                if (rectangle.square4(sudoku).isSolved()) continue;
                if (!rectangle.square4(sudoku).getCandidates().containsAll(candidates)) continue;
                if (rectangle.square4(sudoku).getCandidates().removeAll(candidates)) return true;
            }
            return false;
        }
    }

    private static class Type2 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findUniqueRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                if (!rectangle.square3(sudoku).getCandidates().equals(rectangle.square4(sudoku).getCandidates())) continue;
                if (rectangle.square3(sudoku).getCandidates().count() != 3) continue;
                DigitCandidates candidates = rectangle.square3(sudoku).getCandidates().copy();
                candidates.removeAll(rectangle.floorCandidates());
                int extraDigit = candidates.getFirst();
                List<Square> surroundingUnit = rectangle.isRowWise() ? rectangle.square3(sudoku).getSurroundingRow() : rectangle.square3(sudoku).getSurroundingColumn();
                Set<Square> affectedSquares = new HashSet<>(surroundingUnit);
                if (rectangle.square3(sudoku).getBox() == rectangle.square4(sudoku).getBox())
                    affectedSquares.addAll(rectangle.square3(sudoku).getSurroundingBox());
                affectedSquares.remove(rectangle.square3(sudoku));
                affectedSquares.remove(rectangle.square4(sudoku));
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
                DigitCandidates candidates = rectangle.square3(sudoku).getCandidates().or(rectangle.square4(sudoku).getCandidates());
                if (candidates.count() != 4) continue;
                candidates.removeAll(rectangle.floorCandidates());
                List<Square> surroundingUnit = rectangle.isRowWise() ? rectangle.square3(sudoku).getSurroundingRow() : rectangle.square3(sudoku).getSurroundingColumn();
                Square otherSquare = null;
                for (Square square : surroundingUnit) {
                    if (square == rectangle.square3(sudoku) || square == rectangle.square4(sudoku)) continue;
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
                    if (square == rectangle.square3(sudoku) || square == rectangle.square4(sudoku) || square == otherSquare)
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
                    List<Square> surroundingUnit = rectangle.isRowWise() ? rectangle.square3(sudoku).getSurroundingRow() : rectangle.square3(sudoku).getSurroundingColumn();
                    boolean hasNoDigit = true;
                    for (Square square : surroundingUnit) {
                        if (square.isSolved()) continue;
                        if (square == rectangle.square3(sudoku) || square == rectangle.square4(sudoku)) continue;
                        if (square.getCandidates().contains(digit)) {
                            hasNoDigit = false;
                            break;
                        }
                    }
                    if (rectangle.square3(sudoku).getBox() == rectangle.square4(sudoku).getBox()) {
                        boolean hasNoDigitInBox = true;
                        for (Square square : rectangle.square3(sudoku).getSurroundingBox()) {
                            if (square.isSolved()) continue;
                            if (square == rectangle.square3(sudoku) || square == rectangle.square4(sudoku)) continue;
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
                        changed |= rectangle.square3(sudoku).getCandidates().remove(otherDigit);
                        changed |= rectangle.square4(sudoku).getCandidates().remove(otherDigit);
                        if (changed) return true;
                    }
                }
            }
            return false;
        }
    }

    private static class Type5 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = new ArrayList<>();
            for (int row1 = 0; row1 < 9; row1++) {
                for (int row2 = row1 + 1; row2 < 9; row2++) {
                    for (int col1 = 0; col1 < 9; col1++) {
                        for (int col2 = col1 + 1; col2 < 9; col2++) {
                            Square square1 = sudoku.getSquare(row1, col1);
                            Square square2 = sudoku.getSquare(row1, col2);
                            Rectangle rectangle = new Rectangle(square1, square2, row2);
                            if (rectangle.getCorners(sudoku).stream().anyMatch(Square::isSolved)) continue;
                            if (square1.getBox() != square2.getBox() && square1.getBox() != rectangle.square3(sudoku).getBox()) continue;
                            if (isValidRectangle(sudoku, rectangle)) rectangles.add(rectangle);
                        }
                    }
                }
            }
            for (Rectangle rectangle : rectangles) {
                boolean isLeftDiagonal = rectangle.square1().getCandidates().count() == 2;
                Square floor1;
                Square floor2;
                if (isLeftDiagonal) {
                    floor1 = rectangle.square1();
                    floor2 = rectangle.square4(sudoku);
                } else {
                    floor1 = rectangle.square2();
                    floor2 = rectangle.square3(sudoku);
                }
                boolean changed = false;
                changed |= processSquare(sudoku, rectangle, floor1);
                changed |= processSquare(sudoku, rectangle, floor2);
                if (changed) return true;
            }
            return false;
        }

        private static boolean isValidRectangle(Sudoku sudoku, Rectangle rectangle) {
            DigitCandidates sharedCandidates = rectangle.getCorners(sudoku).stream().map(Square::getCandidates).reduce(new DigitCandidates(), DigitCandidates::and);
            if (sharedCandidates.count() != 2) return false;
            if (rectangle.square1().getCandidates().equals(rectangle.square4(sudoku).getCandidates()) &&
                rectangle.square1().getCandidates().equals(sharedCandidates) &&
                rectangle.square2().getCandidates().count() > 2 &&
                rectangle.square3(sudoku).getCandidates().count() > 2)
                return true;
            else if (rectangle.square2().getCandidates().equals(rectangle.square3(sudoku).getCandidates()) &&
                     rectangle.square2().getCandidates().equals(sharedCandidates) &&
                     rectangle.square1().getCandidates().count() > 2 &&
                     rectangle.square4(sudoku).getCandidates().count() > 2)
                return true;
            return false;
        }

        private static boolean processSquare(Sudoku sudoku, Rectangle rectangle, Square square) {
            Set<Square> squaresToCheck = new HashSet<>();
            squaresToCheck.addAll(square.getSurroundingRow());
            squaresToCheck.addAll(square.getSurroundingColumn());
            rectangle.getCorners(sudoku).forEach(squaresToCheck::remove);
            squaresToCheck.removeIf(Square::isSolved);
            Optional<Integer> strongLinkDigit = Optional.empty();
            for (int digit : square.getCandidates().getAllCandidates()) {
                boolean containsDigit = false;
                for (Square current : squaresToCheck) {
                    if (current.getCandidates().contains(digit)) {
                        containsDigit = true;
                        break;
                    }
                }
                if (!containsDigit) {
                    if (strongLinkDigit.isPresent()) strongLinkDigit = Optional.empty();
                    else strongLinkDigit = Optional.of(digit);
                }
            }
            if (strongLinkDigit.isEmpty()) return false;
            DigitCandidates copy = square.getCandidates().copy();
            copy.remove(strongLinkDigit.get());
            int targetDigit = copy.getFirst();
            return square.getCandidates().remove(targetDigit);
        }
    }

    private record Rectangle(Square square1, Square square2, int otherIndex) {
        public boolean isRowWise() {
            return square1.getRow() == square2.getRow();
        }

        public Square square3(Sudoku sudoku) {
            return isRowWise() ? sudoku.getSquare(otherIndex, square1.getCol()) : sudoku.getSquare(square1.getRow(), otherIndex);
        }

        public Square square4(Sudoku sudoku) {
            return isRowWise() ? sudoku.getSquare(otherIndex, square2.getCol()) : sudoku.getSquare(square2.getRow(), otherIndex);
        }

        public DigitCandidates floorCandidates() {
            return square1.getCandidates();
        }

        public List<Square> getCorners(Sudoku sudoku) {
            return List.of(square1, square2, square3(sudoku), square4(sudoku));
        }

        @Override
        public String toString() {
            if (isRowWise())
                return "[R" + square1.getRow() + otherIndex + "C" + square1.getCol() + square2.getCol() + "]";
            else
                return "[" + square1.getRow() + square2.getRow() + "C" + square1.getCol() + otherIndex + "]";
        }
    }
}
