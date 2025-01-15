package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;

import java.util.*;

public class XYChainsStrategy implements SolveStrategy {
    @Override
    public boolean solve(Sudoku sudoku) {
        List<Square> bivalueSquares = SolverUtils.getAllBivalueSquares(sudoku);
        Set<List<Square>> allChains = new LinkedHashSet<>();
        for (Square square : bivalueSquares) buildChain(square, new ArrayList<>(), allChains, new HashSet<>());
        for (List<Square> chain : allChains) {
            if (chain.size() < 3) continue;
            Square start = chain.getFirst();
            Square end = chain.getLast();
            if (start.getCandidates().or(end.getCandidates()).count() != 3) continue;
            int sharedCandidate = getSharedCandidate(start, end);
            if (sharedCandidate == -1) continue;
            if (!isValidChain(chain, sharedCandidate)) continue;
            //System.out.println("Chain found: " + chain);
            for (Square square : sudoku.getAllSquares()) {
                if (square.isSolved()) continue;
                if (chain.contains(square)) continue;
                if (SolverUtils.isConnected(square, start) &&
                    SolverUtils.isConnected(square, end) &&
                    square.getCandidates().count() > 1) {
                    //System.out.println("Removing " + sharedCandidate + " from " + square);
                    if (square.getCandidates().remove(sharedCandidate)) return true;
                }
            }
        }
        return false;
    }

    private boolean isValidChain(List<Square> chain, int targetDigit) {
        if (!chain.getFirst().getCandidates().contains(targetDigit)) return false;
        int target = targetDigit;
        for (Square square : chain) {
            List<Integer> candidates = square.getCandidates().getAllCandidates();
            if (!candidates.contains(target)) return false;
            target = candidates.get(0) == target ? candidates.get(1) : candidates.get(0);
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
            List<Integer> candidates = square.getCandidates().getAllCandidates();
            if (!candidates.contains(target)) return false;
            target = candidates.get(0) == target ? candidates.get(1) : candidates.get(0);
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
        for (int c : square1.getCandidates().getAllCandidates())
            if (square2.getCandidates().contains(c)) return c;
        return -1;
    }

    private void buildChain(Square square, List<Square> chain, Set<List<Square>> allChains, Set<Square> visited) {
        //System.out.println("buildChain: " + square + " chain: " + chain);
        if (!isValidPartialChain(chain)) return;
        if (visited.contains(square)) {
            allChains.add(chain);
            return;
        }
        visited.add(square);
        chain.add(square);
        List<Square> connectedBivalueSquares = findConnectedBivalueSquares(square);
        if (connectedBivalueSquares.isEmpty()) {
            allChains.add(chain);
            return;
        }
        for (Square next : connectedBivalueSquares) {
            if (square.getCandidates().or(next.getCandidates()).count() == 4) continue;
            buildChain(next, new ArrayList<>(chain), allChains, visited);
        }
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
