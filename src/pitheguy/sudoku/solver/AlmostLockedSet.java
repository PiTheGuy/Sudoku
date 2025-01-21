package pitheguy.sudoku.solver;

import pitheguy.sudoku.util.SquareSet;

public record AlmostLockedSet(SquareSet squares, DigitCandidates candidates) {
    public DigitCandidates getCommonCandidates(AlmostLockedSet other) {
        return candidates.and(other.candidates);
    }
}
