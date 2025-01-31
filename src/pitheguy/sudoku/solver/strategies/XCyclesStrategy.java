
package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.Pair;
import pitheguy.sudoku.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class XCyclesStrategy extends SolveStrategy {

    public XCyclesStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        for (int digit = 1; digit <= 9; digit++) {
            Set<Cycle> allCycles = new LinkedHashSet<>();
            for (Square square : sudoku.getAllSquares()) {
                if (square.isSolved()) continue;
                if (!square.getCandidates().contains(digit)) continue;
                findCycles(square, digit, new Cycle(), allCycles, true);
                findCycles(square, digit, new Cycle(), allCycles, false);
            }
            for (Cycle cycle : allCycles) {
                boolean changed = false;
                //System.out.println("Cycle found for digit " + digit + ": " + cycle);
                for (Square square : sudoku.getAllSquares()) {
                    if (square.isSolved()) continue;
                    if (cycle.contains(square)) continue;
                    if (!square.getCandidates().contains(digit)) continue;
                    List<Square> visibleCycleSquares = new ArrayList<>();
                    for (Square current : cycle)
                        if (SolverUtils.isConnected(square, current)) visibleCycleSquares.add(current);
                    if (visibleCycleSquares.size() < 2) break;
                    boolean valid = false;
                    for (Pair<Square> pair : Util.getAllPairs(visibleCycleSquares)) {
                        int index1 = cycle.indexOf(pair.first());
                        int index2 = cycle.indexOf(pair.second());
                        if (index1 % 2 != index2 % 2) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) continue;
                    //System.out.println("Removing from " + square);
                    changed |= square.getCandidates().remove(digit);
                }
                if (changed) return true;
            }
        }
        return false;
    }

    private void findCycles(Square square, int digit, Cycle cycle, Set<Cycle> allCycles, boolean isStrongLink) {
        if (!cycle.isEmpty() && cycle.getFirst() != square && cycle.contains(square)) return;
        List<Square> links = isStrongLink ? findStrongLinks(square, digit) : findWeakLinks(square, digit);
        if (links.isEmpty()) return;
        if (!cycle.isEmpty() && cycle.getFirst() == square) {
            if (links.contains(square) && cycle.size() > 2 && cycle.size() % 2 == 0) allCycles.add(cycle.copy());
            return;
        }
        cycle.add(square);
        for (Square link : links) findCycles(link, digit, cycle, allCycles, !isStrongLink);
        cycle.remove(square);
    }

    private List<Square> findStrongLinks(Square square, int digit) {
        List<Square> possibleLinks = new ArrayList<>();
        SolverUtils.getOnlySquareThat(square.getSurroundingRow(), s -> s != square && s.getCandidates().contains(digit), true).ifPresent(possibleLinks::add);
        SolverUtils.getOnlySquareThat(square.getSurroundingColumn(), s -> s != square && s.getCandidates().contains(digit), true).ifPresent(possibleLinks::add);
        SolverUtils.getOnlySquareThat(square.getSurroundingBox(), s -> s != square && s.getCandidates().contains(digit), true).ifPresent(possibleLinks::add);
        return possibleLinks;
    }

    private List<Square> findWeakLinks(Square square, int digit) {
        List<Square> weakLinks = new ArrayList<>();
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingRow(), digit));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingColumn(), digit));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingBox(), digit));
        return weakLinks;
    }

    private List<Square> findWeakLinksForGroup(List<Square> group, int digit) {
        if (SolverUtils.hasDigitSolved(group, digit)) return Collections.emptyList();
        group.removeIf(square -> square.isSolved() || square.getCandidates().contains(digit));
        return group;
    }

    private static class Cycle extends ArrayList<Square> {

        public Cycle() {
        }

        public Cycle(Collection<Square> squares) {
            super(squares);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Cycle other)) return false;
            if (this.size() != other.size()) return false;
            List<Square> doubled = new ArrayList<>(this);
            doubled.addAll(this);
            return Collections.indexOfSubList(doubled, other) != -1;
        }

        @Override
        public int hashCode() {
            return stream().map(Square::hashCode).sorted().reduce(0, Integer::sum);
        }

        public Cycle copy() {
            return new Cycle(this);
        }
    }
}