package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.*;

public class HiddenUniqueRectanglesStrategy extends SolveStrategy {
    public HiddenUniqueRectanglesStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        changed |= Type1.solve(sudoku);
        changed |= Type2.solve(sudoku);
        return changed;
    }

    private static List<Rectangle> findPossibleRectangles(Sudoku sudoku) {
        List<Rectangle> possibleRectangles = new ArrayList<>();
        for (int row1 = 0; row1 < 9; row1++) {
            for (int row2 = row1 + 1; row2 < 9; row2++) {
                for (int col1 = 0; col1 < 9; col1++) {
                    for (int col2 = col1 + 1; col2 < 9; col2++) {
                        Rectangle rectangle = new Rectangle(row1, row2, col1, col2);
                        if (rectangle.getCorners(sudoku).stream().anyMatch(Square::isSolved)) continue;
                        if (rectangle.square1(sudoku).getBox() != rectangle.square2(sudoku).getBox() &&
                            rectangle.square1(sudoku).getBox() != rectangle.square3(sudoku).getBox())
                            continue;
                        if (rectangle.getCandidates(sudoku).count() == 2) possibleRectangles.add(rectangle);
                    }
                }
            }
        }
        return possibleRectangles;
    }

    private static Map<Integer, List<StrongLink>> findStrongLinks(Square square) {
        Map<Integer, List<StrongLink>> strongLinks = new HashMap<>();
        for (int digit : square.getCandidates().getAllCandidates()) {
            strongLinks.putIfAbsent(digit, new ArrayList<>());
            Optional<Square> rowSquare = SolverUtils.getOnlySquareThat(square.getSurroundingRow(),
                    s -> s != square && s.getCandidates().contains(digit), true);
            rowSquare.ifPresent(value -> strongLinks.get(digit).add(new StrongLink(square, value, digit)));
            Optional<Square> columnSquare = SolverUtils.getOnlySquareThat(square.getSurroundingColumn(),
                    s -> s != square && s.getCandidates().contains(digit), true);
            columnSquare.ifPresent(value -> strongLinks.get(digit).add(new StrongLink(square, value, digit)));
        }
        return strongLinks;
    }

    private static class Type1 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findPossibleRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                Optional<Square> optional = SolverUtils.getOnlySquareThat(rectangle.getCorners(sudoku),
                        square -> square.getCandidates().count() == 2, true);
                if (optional.isEmpty()) continue;
                Square bivalueSquare = optional.get();
                Square square = rectangle.getOppositeCorner(sudoku, bivalueSquare);
                Map<Integer, List<StrongLink>> strongLinks = findStrongLinks(square);
                Integer strongLinkDigit = null;
                for (Integer digit : strongLinks.keySet()) {
                    strongLinks.get(digit).removeIf(link -> !rectangle.getCorners(sudoku).contains(link.square2()));
                    if (strongLinks.get(digit).size() == 2) {
                        strongLinkDigit = digit;
                        break;
                    }
                }
                if (strongLinkDigit == null) continue;
                //System.out.println("digit " + strongLinkDigit + " square " + square + " " + rectangle);
                DigitCandidates candidates = rectangle.getCandidates(sudoku);
                candidates.remove(strongLinkDigit);
                int removedDigit = candidates.getFirst();
                if (square.getCandidates().remove(removedDigit)) return true;
            }
            return false;
        }
    }

    private static class Type2 {
        private static boolean solve(Sudoku sudoku) {
            List<Rectangle> rectangles = findPossibleRectangles(sudoku);
            for (Rectangle rectangle : rectangles) {
                List<Square> bivalueCells = new ArrayList<>();
                for (Square square : rectangle.getCorners(sudoku))
                    if (square.getCandidates().count() == 2) bivalueCells.add(square);
                if (bivalueCells.size() != 2) continue;
                if (bivalueCells.get(0).getRow() != bivalueCells.get(1).getRow() &&
                    bivalueCells.get(0).getCol() != bivalueCells.get(1).getCol()) continue;
                for (Square square : rectangle.getCorners(sudoku)) {
                    if (bivalueCells.contains(square)) continue;
                    Map<Integer, List<StrongLink>> strongLinks = findStrongLinks(square);
                    List<Square> list = new ArrayList<>(rectangle.getCorners(sudoku));
                    list.removeAll(bivalueCells);
                    list.remove(square);
                    Square otherSquare = list.getFirst();
                    Integer strongLinkDigit = null;
                    for (int digit : strongLinks.keySet()) {
                        strongLinks.get(digit).removeIf(link -> !rectangle.getCorners(sudoku).contains(link.square2()));
                        strongLinks.get(digit).removeIf(link -> link.square2 == otherSquare);
                        if (!strongLinks.get(digit).isEmpty()) {
                            strongLinkDigit = digit;
                            break;
                        }
                    }
                    if (strongLinkDigit == null) continue;
                    DigitCandidates candidates = rectangle.getCandidates(sudoku);
                    candidates.remove(strongLinkDigit);
                    int removedDigit = candidates.getFirst();
                    if (otherSquare.getCandidates().remove(removedDigit)) return true;
                }
            }
            return false;
        }
    }

    private record Rectangle(int row1, int row2, int col1, int col2) {
        public Square square1(Sudoku sudoku) {
            return sudoku.getSquare(row1, col1);
        }

        public Square square2(Sudoku sudoku) {
            return sudoku.getSquare(row1, col2);
        }

        public Square square3(Sudoku sudoku) {
            return sudoku.getSquare(row2, col1);
        }

        public Square square4(Sudoku sudoku) {
            return sudoku.getSquare(row2, col2);
        }

        public List<Square> getCorners(Sudoku sudoku) {
            return List.of(square1(sudoku), square2(sudoku), square3(sudoku), square4(sudoku));
        }

        public DigitCandidates getCandidates(Sudoku sudoku) {
            DigitCandidates candidates = new DigitCandidates();
            for (Square square : getCorners(sudoku))
                candidates = candidates.and(square.getCandidates());
            return candidates;
        }

        public Square getOppositeCorner(Sudoku sudoku, Square square) {
            int row = square.getRow() == row1 ? row2 : row1;
            int col = square.getCol() == col1 ? col2 : col1;
            return sudoku.getSquare(row, col);
        }
    }

    private record StrongLink(Square square1, Square square2, int digit) {}
}
