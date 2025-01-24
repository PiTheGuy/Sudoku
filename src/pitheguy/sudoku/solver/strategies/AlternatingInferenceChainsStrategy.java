package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.Pair;
import pitheguy.sudoku.util.UniquePair;
import pitheguy.sudoku.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AlternatingInferenceChainsStrategy extends SolveStrategy {
    private final Map<UniquePair<Square, Integer>, Set<UniquePair<Square, Integer>>> strongLinkCache = new HashMap<>();
    private final Map<UniquePair<Square, Integer>, Set<UniquePair<Square, Integer>>> weakLinkCache = new HashMap<>();

    public AlternatingInferenceChainsStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        if (solveImpl(8)) return true;
        if (solveImpl(10)) return true;
        if (solveImpl(15)) return true;
        return false;
    }

    private boolean solveImpl(int maxDepth) {
        boolean changed = false;
        Set<Cycle> continuousCycles = new LinkedHashSet<>();
        Set<Cycle> discontinuousCycles = new LinkedHashSet<>();
        for (Square square : sudoku.getAllSquares()) {
            if (square.isSolved()) continue;
            for (int digit : square.getCandidates().getAllCandidates()) {
                findCycles(new UniquePair<>(square, digit), new Cycle(true), continuousCycles, discontinuousCycles, true, maxDepth);
                findCycles(new UniquePair<>(square, digit), new Cycle(false), continuousCycles, discontinuousCycles, false, maxDepth);
            }
        }
        cycles:
        for (Cycle cycle : continuousCycles) {
            for (UniquePair<Square, Integer> node : cycle)
                if (!node.first().getCandidates().contains(node.second())) continue cycles;
            for (int i = 0; i < cycle.size() - 1; i++) {
                if (cycle.get(i).first() == cycle.get(i + 1).first()) {
                    short flags = DigitCandidates.getFlags(cycle.get(i).second(), cycle.get(i + 1).second());
                    changed |= cycle.get(i).first().getCandidates().setFlags(flags);
                }
            }
            //System.out.println("Cycle found: " + cycle);
            for (int digit : cycle.getContainedDigits()) {
                for (Square square : sudoku.getAllSquares()) {
                    UniquePair<Square, Integer> pair = new UniquePair<>(square, digit);
                    if (square.isSolved()) continue;
                    if (cycle.contains(pair)) continue;
                    if (!square.getCandidates().contains(digit)) continue;
                    List<UniquePair<Square, Integer>> visibleNodes = new ArrayList<>();
                    for (UniquePair<Square, Integer> current : cycle)
                        if (current.second() == digit && SolverUtils.isConnected(square, current.first()))
                            visibleNodes.add(current);
                    if (visibleNodes.size() < 2) continue;
                    boolean valid = false;
                    for (Pair<UniquePair<Square, Integer>> nodePair : Util.getAllPairs(visibleNodes)) {
                        int index1 = cycle.indexOf(nodePair.first());
                        int index2 = cycle.indexOf(nodePair.second());
                        if (index1 % 2 != index2 % 2) {
                            //System.out.println("Removing " + digit + " from " + square + " because of " + nodePair.first() + " and " + nodePair.second());
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) continue;
                    changed |= square.getCandidates().remove(digit);
                }
            }
        }
        cycles:
        for (Cycle cycle : discontinuousCycles) {
            for (UniquePair<Square, Integer> node : cycle)
                if (!node.first().getCandidates().contains(node.second())) continue cycles;
            Square square = cycle.getFirst().first();
            int digit = cycle.getFirst().second();
            if (cycle.isFirstLinkStrong()) changed |= square.getCandidates().setFlags(DigitCandidates.getFlags(digit));
            else changed |= square.getCandidates().remove(digit);
        }
        return changed;
    }

    private void findCycles(UniquePair<Square, Integer> candidate, Cycle cycle, Set<Cycle> continuousCycles, Set<Cycle> discontinuousCycles, boolean isStrongLink, int maxDepth) {
        if (cycle.size() > maxDepth) return;
        if (!cycle.isEmpty()) {
            if (cycle.getFirst().equals(candidate)) {
                if (cycle.size() > 2) {
                    if (cycle.size() % 2 == 0) continuousCycles.add(cycle.copy());
                    else discontinuousCycles.add(cycle.copy());
                }
                return;
            } else if (cycle.contains(candidate)) return;
        }
        //System.out.println(cycle);
        Set<UniquePair<Square, Integer>> links = isStrongLink ? findStrongLinks(candidate) : findWeakLinks(candidate);
        if (links.isEmpty()) return;

        cycle.add(candidate);
        for (UniquePair<Square, Integer> link : links) {
            findCycles(link, cycle, continuousCycles, discontinuousCycles, !isStrongLink, maxDepth);
        }
        cycle.removeLast();
    }

    private Set<UniquePair<Square, Integer>> findStrongLinks(UniquePair<Square, Integer> candidate) {
        if (strongLinkCache.containsKey(candidate)) return strongLinkCache.get(candidate);
        Square square = candidate.first();
        int digit = candidate.second();
        Set<UniquePair<Square, Integer>> links = new HashSet<>();
        if (square.getCandidates().count() == 2) {
            DigitCandidates candidates = square.getCandidates().copy();
            candidates.remove(digit);
            int newDigit = candidates.getFirst();
            links.add(new UniquePair<>(square, newDigit));
        }
        List<Optional<Square>> externalLinks = new ArrayList<>();
        externalLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingRow(), s -> s != square && s.getCandidates().contains(digit), true));
        externalLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingColumn(), s -> s != square && s.getCandidates().contains(digit), true));
        externalLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingBox(), s -> s != square && s.getCandidates().contains(digit), true));
        links.addAll(externalLinks.stream().filter(Optional::isPresent).map(s -> new UniquePair<>(s.get(), digit)).toList());
        strongLinkCache.put(candidate, links);
        return links;
    }

    private Set<UniquePair<Square, Integer>> findWeakLinks(UniquePair<Square, Integer> candidate) {
        if (weakLinkCache.containsKey(candidate)) return weakLinkCache.get(candidate);
        Square square = candidate.first();
        int digit = candidate.second();
        Set<UniquePair<Square, Integer>> weakLinks = new HashSet<>();
        if (square.getCandidates().count() > 1)
            for (int c : square.getCandidates().getAllCandidates())
                if (c != digit) weakLinks.add(new UniquePair<>(square, c));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingRow(), digit));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingColumn(), digit));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingBox(), digit));
        weakLinkCache.put(candidate, weakLinks);
        return weakLinks;
    }

    private List<UniquePair<Square, Integer>> findWeakLinksForGroup(List<Square> group, int digit) {
        if (SolverUtils.hasDigitSolved(group, digit)) return Collections.emptyList();
        return group.stream()
                .filter(s -> !s.isSolved() && s.getCandidates().contains(digit))
                .map(s -> new UniquePair<>(s, digit))
                .toList();
    }

    private static class Cycle extends ArrayList<UniquePair<Square, Integer>> {
        private final boolean firstLinkStrong;

        public Cycle(boolean firstLinkStrong) {
            this.firstLinkStrong = firstLinkStrong;
        }

        public Cycle(Collection<UniquePair<Square, Integer>> squares, boolean firstLinkStrong) {
            super(squares);
            this.firstLinkStrong = firstLinkStrong;
        }

        public List<Integer> getContainedDigits() {
            return stream().map(UniquePair::second).distinct().toList();
        }

        public boolean isFirstLinkStrong() {
            return firstLinkStrong;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Cycle other)) return false;
            if (this.size() != other.size()) return false;
            List<UniquePair<Square, Integer>> doubled = new ArrayList<>(this);
            doubled.addAll(this);
            return Collections.indexOfSubList(doubled, other) != -1;
        }

        @Override
        public int hashCode() {
            Cycle copy = copy();
            copy.sort(Comparator.comparingInt((UniquePair<Square, Integer> u) -> u.first().getRow())
                    .thenComparingInt(u -> u.first().getCol())
                    .thenComparingInt(UniquePair::second));
            UniquePair<Square, Integer> start = copy.getFirst();
            int startIndex = indexOf(start);
            List<UniquePair<Square, Integer>> normalized = new ArrayList<>(subList(startIndex, size()));
            if (startIndex != 0) normalized.addAll(subList(0, startIndex));
            return normalized.hashCode();
        }

        public Cycle copy() {
            return new Cycle(this, firstLinkStrong);
        }
    }
}