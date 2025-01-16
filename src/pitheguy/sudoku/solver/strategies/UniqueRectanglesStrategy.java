package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;

import java.util.*;

public class UniqueRectanglesStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        boolean changed = false;
        changed |= Type1.solve(sudoku);
        changed |= Type2.solve(sudoku);
        return changed;
    }

    private static Map<DigitCandidates, List<Square>> getBivalueCellsMap(Sudoku sudoku, int index, boolean isRow) {
        Map<DigitCandidates, List<Square>> map = new HashMap<>();
        for (int pos = 0; pos < 9; pos++) {
            Square square = isRow ? sudoku.getSquare(index, pos) : sudoku.getSquare(pos, index);
            if (square.isSolved()) continue;
            DigitCandidates candidates = square.getCandidates();
            if (candidates.count() != 2) continue;
            map.computeIfAbsent(candidates, k -> new ArrayList<>()).add(square);
        }
        return map;
    }

    private static class Type1 {
        private static boolean solve(Sudoku sudoku) {
            for (int row = 0; row < 9; row++) {
                Map<DigitCandidates, List<Square>> map = getBivalueCellsMap(sudoku, row, true);
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

    private static class Type2 {
        private static boolean solve(Sudoku sudoku) {
            boolean changed = false;
            changed |= solveImpl(sudoku, true);
            changed |= solveImpl(sudoku, false);
            return changed;
        }

        private static boolean solveImpl(Sudoku sudoku, boolean isRow) {
            for (int index = 0; index < 9; index++) {
                Map<DigitCandidates, List<Square>> map = getBivalueCellsMap(sudoku, index, isRow);
                for (Map.Entry<DigitCandidates, List<Square>> entry : map.entrySet()) {
                    DigitCandidates floorCandidates = entry.getKey();
                    List<Square> squares = entry.getValue();
                    if (squares.size() != 2) continue;
                    for (Square square1 : squares) {
                        Square square2 = squares.getFirst() == square1 ? squares.get(1) : squares.get(0);
                        for (int otherIndex = 0; otherIndex < 9; otherIndex++) {
                            if (index == otherIndex) continue;
                            Square square3 = isRow ? sudoku.getSquare(otherIndex, square1.getCol()) : sudoku.getSquare(square1.getRow(), otherIndex);
                            Square square4 = isRow ? sudoku.getSquare(otherIndex, square2.getCol()) : sudoku.getSquare(square2.getRow(), otherIndex);
                            if (square3.isSolved() || !square4.isSolved()) continue;
                            if (!square3.getCandidates().equals(square4.getCandidates())) continue;
                            if (!square3.getCandidates().containsAll(floorCandidates)) continue;
                            if (square1.getBox() != square2.getBox() && square1.getBox() != square3.getBox()) continue;
                            //System.out.println("s1: " + square1 + ", s2: " + square2 + ", s3: " + square3 + ", s4: " + square4);
                            DigitCandidates candidates = square3.getCandidates().copy();
                            candidates.removeAll(floorCandidates);
                            int extraDigit = candidates.getFirst();
                            Set<Square> affectedSquares = new HashSet<>(isRow ? square3.getSurroundingRow() : square3.getSurroundingColumn());
                            if (square3.getBox() == square4.getBox()) affectedSquares.addAll(square3.getSurroundingBox());
                            affectedSquares.remove(square3);
                            affectedSquares.remove(square4);
                            boolean changed = false;
                            for (Square square : affectedSquares) changed |= square.getCandidates().remove(extraDigit);
                            if (changed) return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
