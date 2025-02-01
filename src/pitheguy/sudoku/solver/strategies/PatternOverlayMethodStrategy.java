package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.SquareSet;

import java.util.*;
import java.util.function.Function;

public class PatternOverlayMethodStrategy extends SolveStrategy {
    public PatternOverlayMethodStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        boolean changed = false;
        for (int digit = 1; digit <= 9; digit++) {
            SquareSet containedCells = new SquareSet(sudoku);
            for (Square square : sudoku.getAllSquares())
                if (!square.isSolved() && square.getCandidates().contains(digit)) containedCells.add(square);
            if (containedCells.size() > 40) continue; // Too many patterns to search through
            Set<SquareSet> patterns = findPatterns(containedCells);
            if (patterns.isEmpty()) continue;
            int finalDigit = digit;
            patterns.removeIf(pattern -> {
                boolean valid = true;
                valid &= checkGroup(sudoku::getRow, pattern, finalDigit);
                valid &= checkGroup(sudoku::getColumn, pattern, finalDigit);
                valid &= checkGroup(sudoku::getBox, pattern, finalDigit);
                return !valid;
            });
            SquareSet coveredCells = new SquareSet(sudoku);
            for (SquareSet pattern : patterns) coveredCells.addAll(pattern);
            containedCells.removeAll(coveredCells);
            for (Square square : containedCells) changed |= square.getCandidates().remove(digit);
        }
        return changed;
    }

    private boolean checkGroup(Function<Integer, List<Square>> groupGetter, SquareSet pattern, int digit) {
        boolean valid = true;
        for (int i = 0; i < 9; i++) {
            List<Square> squares = groupGetter.apply(i);
            valid &= SolverUtils.hasDigitSolved(squares, digit) || pattern.containsAny(new SquareSet(sudoku, squares));
        }
        return valid;
    }

    private Set<SquareSet> findPatterns(SquareSet squares) {
        Set<SquareSet> patterns = new HashSet<>();
        SquareSet remainingSquares = squares.copy();
        SquareSet pattern = new SquareSet(sudoku);
        findPatternsRecursive(null, remainingSquares, pattern, patterns);
        return patterns;
    }

    private void findPatternsRecursive(Square square, SquareSet remainingSquares, SquareSet pattern, Set<SquareSet> patterns) {
        if (square != null) {
            remainingSquares.removeAll(square.getSurroundingRow());
            remainingSquares.removeAll(square.getSurroundingColumn());
            remainingSquares.removeAll(square.getSurroundingBox());
            pattern.add(square);
        }
        if (remainingSquares.isEmpty()) {
            patterns.add(pattern);
            return;
        }
        for (Square current : remainingSquares) findPatternsRecursive(current, remainingSquares.copy(), pattern, patterns);
        if (square != null) pattern.remove(square);
    }


}
