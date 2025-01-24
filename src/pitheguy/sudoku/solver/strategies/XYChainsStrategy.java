package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.SquareSet;

import java.util.*;

public class XYChainsStrategy extends SolveStrategy {
    private final Map<Square, List<Square>> connectedBivalueSquaresCache = new HashMap<>();

    public XYChainsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        if (solveImpl(false)) return true;
        if (solveImpl(true)) return true;
        return false;
    }

    private boolean solveImpl(boolean backtrack) {
        List<Square> bivalueSquares = SolverUtils.getAllBivalueSquares(sudoku);
        Set<List<Square>> allChains = new LinkedHashSet<>();
        for (Square square : bivalueSquares) buildChain(square, new ArrayList<>(), allChains, new SquareSet(sudoku), backtrack);
        for (List<Square> chain : allChains) {
            if (chain.size() < 3) continue;
            Square start = chain.getFirst();
            Square end = chain.getLast();
            if (start.getCandidates().equals(end.getCandidates()))
                for (int candidate : start.getCandidates().getAllCandidates())
                    if (processXYChain(chain, candidate)) return true;
            if (start.getCandidates().or(end.getCandidates()).count() == 3) {
                int sharedCandidate = getSharedCandidate(start, end);
                if (sharedCandidate == -1) continue;
                if (processXYChain(chain, sharedCandidate)) return true;
            }
        }
        return false;
    }

    private boolean processXYChain(List<Square> chain, int candidate) {
        if (!isValidChain(chain, candidate)) return false;
        //System.out.println("Chain found: " + chain);
        for (Square square : sudoku.getAllSquares()) {
            if (square.isSolved()) continue;
            if (chain.contains(square)) continue;
            if (SolverUtils.isConnected(square, chain.getFirst()) &&
                SolverUtils.isConnected(square, chain.getLast())) {
                //System.out.println("Removing " + candidate + " from " + square);
                if (square.getCandidates().remove(candidate)) return true;
            }
        }
        return false;
    }

    private boolean isValidChain(List<Square> chain, int targetDigit) {
        if (!chain.getFirst().getCandidates().contains(targetDigit)) return false;
        int target = targetDigit;
        for (Square square : chain) {
            if (!square.getCandidates().contains(target)) return false;
            DigitCandidates candidates = square.getCandidates().copy();
            candidates.remove(target);
            target = candidates.getFirst();
        }
        return target == targetDigit;
    }

    private boolean isValidPartialChain(List<Square> chain) {
        return hasNoRedundantSquares(chain) &&
               isLinkingValid(chain);
    }

    private boolean isLinkingValid(List<Square> chain) {
        if (chain.size() < 2) return true;
        int target = getSharedCandidate(chain.get(0), chain.get(1));
        if (target == -1) return false;
        for (int i = 1; i < chain.size(); i++) {
            Square square = chain.get(i);
            if (!square.getCandidates().contains(target)) return false;
            DigitCandidates candidates = square.getCandidates().copy();
            candidates.remove(target);
            target = candidates.getFirst();
        }
        return true;
    }

    private boolean hasNoRedundantSquares(List<Square> chain) {
        if (chain.size() != 3) return true;
        Square first = chain.get(0);
        Square second = chain.get(1);
        Square third = chain.get(2);
        if (areInSameGroup(first, second, third)) {
            List<Square> newChain = new ArrayList<>(chain);
            newChain.remove(second);
            return !isLinkingValid(newChain);
        }
        return true;
    }

    private boolean areInSameGroup(Square a, Square b, Square c) {
        return (a.getRow() == b.getRow() && b.getRow() == c.getRow()) ||
               (a.getCol() == b.getCol() && b.getCol() == c.getCol()) ||
               (a.getBox() == b.getBox() && b.getBox() == c.getBox());
    }

    private int getSharedCandidate(Square square1, Square square2) {
        return square1.getCandidates().and(square2.getCandidates()).getFirst();
    }

    private void buildChain(Square square, List<Square> chain, Set<List<Square>> allChains, SquareSet visited, boolean backtrack) {
        //System.out.println("buildChain: " + square + " chain: " + chain);
        if (chain.size() > 10) return;
        if (!isValidPartialChain(chain)) return;
        if (visited.contains(square)) {
            allChains.add(chain);
            return;
        }
        visited.add(square);
        chain.add(square);
        List<Square> connectedBivalueSquares = getConnectedBivalueSquares(square);
        if (connectedBivalueSquares.isEmpty()) {
            allChains.add(chain);
            return;
        }
        for (Square next : connectedBivalueSquares) {
            if (square.getCandidates().or(next.getCandidates()).count() == 4) continue;
            buildChain(next, new ArrayList<>(chain), allChains, visited, backtrack);
        }
        if (backtrack) visited.remove(square);
    }

    private List<Square> getConnectedBivalueSquares(Square square) {
        if (connectedBivalueSquaresCache.containsKey(square)) return connectedBivalueSquaresCache.get(square);
        List<Square> connectedSquares = findConnectedBivalueSquares(square);
        connectedBivalueSquaresCache.put(square, connectedSquares);
        return connectedSquares;
    }

    private List<Square> findConnectedBivalueSquares(Square square) {
        List<Square> connectedSquares = new ArrayList<>();
        addConnectedBivalueCells(square.getSurroundingRow(), square, connectedSquares);
        addConnectedBivalueCells(square.getSurroundingColumn(), square, connectedSquares);
        addConnectedBivalueCells(square.getSurroundingBox(), square, connectedSquares);
        return connectedSquares;
    }

    private static void addConnectedBivalueCells(List<Square> group, Square square, List<Square> connectedSquares) {
        for (Square current : group)
            if (current != square && !current.isSolved() && current.getCandidates().count() == 2 &&
                current.getCandidates().or(square.getCandidates()).count() != 4) connectedSquares.add(current);
    }
}
