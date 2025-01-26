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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AlternatingInferenceChainsStrategy extends SolveStrategy {
    private final Map<Node, Set<Node>> strongLinkCache = new HashMap<>();
    private final Map<Node, Set<Node>> weakLinkCache = new HashMap<>();

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
                findCycles(new Node(square, digit), new Cycle(true), continuousCycles, discontinuousCycles, true, maxDepth);
                findCycles(new Node(square, digit), new Cycle(false), continuousCycles, discontinuousCycles, false, maxDepth);
            }
        }
        cycles:
        for (Cycle cycle : continuousCycles) {
            for (Node node : cycle)
                if (!node.square().getCandidates().contains(node.digit())) continue cycles;
            for (int i = 0; i < cycle.size() - 1; i++) {
                if (cycle.get(i).square() == cycle.get(i + 1).square()) {
                    short flags = DigitCandidates.getFlags(cycle.get(i).digit(), cycle.get(i + 1).digit());
                    changed |= cycle.get(i).square().getCandidates().setFlags(flags);
                }
            }
            //System.out.println("Cycle found: " + cycle);
            for (int digit : cycle.getContainedDigits()) {
                for (Square square : sudoku.getAllSquares()) {
                    Node node = new Node(square, digit);
                    if (square.isSolved()) continue;
                    if (cycle.contains(node)) continue;
                    if (!square.getCandidates().contains(digit)) continue;
                    List<Node> visibleNodes = new ArrayList<>();
                    for (Node current : cycle)
                        if (current.digit() == digit && SolverUtils.isConnected(square, current.square()))
                            visibleNodes.add(current);
                    if (visibleNodes.size() < 2) continue;
                    boolean valid = false;
                    for (Pair<Node> nodePair : Util.getAllPairs(visibleNodes)) {
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
            for (Node node : cycle)
                if (!node.square().getCandidates().contains(node.digit())) continue cycles;
            Square square = cycle.getFirst().square();
            int digit = cycle.getFirst().digit();
            if (cycle.isFirstLinkStrong()) changed |= square.getCandidates().setFlags(DigitCandidates.getFlags(digit));
            else changed |= square.getCandidates().remove(digit);
        }
        return changed;
    }

    private void findCycles(Node node, Cycle cycle, Set<Cycle> continuousCycles, Set<Cycle> discontinuousCycles, boolean isStrongLink, int maxDepth) {
        if (cycle.size() > maxDepth) return;
        if (!cycle.isEmpty()) {
            if (cycle.getFirst().equals(node)) {
                if (cycle.size() > 2) {
                    if (cycle.size() % 2 == 0) continuousCycles.add(cycle.copy());
                    else discontinuousCycles.add(cycle.copy());
                }
                return;
            } else if (cycle.contains(node)) return;
        }
        Set<Node> links = isStrongLink ? findStrongLinks(node) : findWeakLinks(node);
        if (links.isEmpty()) return;

        cycle.add(node);
        for (Node link : links) {
            findCycles(link, cycle, continuousCycles, discontinuousCycles, !isStrongLink, maxDepth);
        }
        cycle.removeLast();
    }

    private Set<Node> findStrongLinks(Node node) {
        if (strongLinkCache.containsKey(node)) return strongLinkCache.get(node);
        Square square = node.square();
        int digit = node.digit();
        Set<Node> links = new LinkedHashSet<>();
        if (square.getCandidates().count() == 2) {
            DigitCandidates candidates = square.getCandidates().copy();
            candidates.remove(digit);
            int newDigit = candidates.getFirst();
            links.add(new Node(square, newDigit));
        }
        List<Optional<Square>> externalLinks = new ArrayList<>();
        externalLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingRow(), s -> s != square && s.getCandidates().contains(digit), true));
        externalLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingColumn(), s -> s != square && s.getCandidates().contains(digit), true));
        externalLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingBox(), s -> s != square && s.getCandidates().contains(digit), true));
        links.addAll(externalLinks.stream().filter(Optional::isPresent).map(s -> new Node(s.get(), digit)).toList());
        strongLinkCache.put(node, links);
        return links;
    }

    private Set<Node> findWeakLinks(Node node) {
        if (weakLinkCache.containsKey(node)) return weakLinkCache.get(node);
        Square square = node.square();
        int digit = node.digit();
        Set<Node> weakLinks = new LinkedHashSet<>();
        if (square.getCandidates().count() > 1)
            for (int c : square.getCandidates().getAllCandidates())
                if (c != digit) weakLinks.add(new Node(square, c));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingRow(), digit));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingColumn(), digit));
        weakLinks.addAll(findWeakLinksForGroup(square.getSurroundingBox(), digit));
        weakLinkCache.put(node, weakLinks);
        return weakLinks;
    }

    private List<Node> findWeakLinksForGroup(List<Square> group, int digit) {
        if (SolverUtils.hasDigitSolved(group, digit)) return Collections.emptyList();
        return group.stream()
                .filter(s -> !s.isSolved() && s.getCandidates().contains(digit))
                .map(s -> new Node(s, digit))
                .toList();
    }

    private static class Cycle extends ArrayList<Node> {
        private final boolean firstLinkStrong;
        private final NodeSet containedNodes = new NodeSet();

        public Cycle(boolean firstLinkStrong) {
            this.firstLinkStrong = firstLinkStrong;
        }

        public Cycle(Collection<Node> nodes, boolean firstLinkStrong, NodeSet containedNodes) {
            super(nodes);
            this.firstLinkStrong = firstLinkStrong;
            this.containedNodes.addAll(containedNodes);
        }

        @Override
        public boolean add(Node node) {
            boolean added = super.add(node);
            if (added) containedNodes.add(node);
            return added;
        }

        @Override
        public boolean addAll(Collection<? extends Node> c) {
            boolean modified = super.addAll(c);
            if (modified) c.forEach(containedNodes::add);
            return modified;
        }

        @Override
        public boolean remove(Object o) {
            int index = indexOf(o);
            if (index != -1) remove(index);
            return index != -1;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean modified = false;
            for (Object o : c) modified |= remove(o);
            return modified;
        }

        @Override
        public Node remove(int index) {
            Node removed = super.remove(index);
            containedNodes.remove(removed);
            return removed;
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Node node) return containedNodes.contains(node);
            else return false;
        }

        public List<Integer> getContainedDigits() {
            return stream().map(Node::digit).distinct().toList();
        }

        public boolean isFirstLinkStrong() {
            return firstLinkStrong;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Cycle other)) return false;
            if (this.size() != other.size()) return false;
            List<Node> doubled = new ArrayList<>(this);
            doubled.addAll(this);
            return Collections.indexOfSubList(doubled, other) != -1;
        }

        @Override
        public int hashCode() {
            if (isEmpty()) return 0;
            Node start = stream().min(Node.COMPARATOR).orElseThrow();
            int startIndex = indexOf(start);
            List<Node> normalized = new ArrayList<>(subList(startIndex, size()));
            if (startIndex != 0) normalized.addAll(subList(0, startIndex));
            return normalized.hashCode();
        }

        public Cycle copy() {
            return new Cycle(this, firstLinkStrong, containedNodes);
        }
    }

    private record Node(Square square, int digit) {
        public static final Comparator<Node> COMPARATOR = Comparator.comparingInt((Node node) -> node.square().getRow())
                .thenComparingInt(node -> node.square().getCol())
                .thenComparingInt(Node::digit);
    }

    private static class NodeSet {
        private final BitSet containedNodes = new BitSet();

        public void add(Node node) {
            containedNodes.set(getNodeIndex(node));
        }

        public void addAll(NodeSet nodes) {
            containedNodes.or(nodes.containedNodes);
        }

        public void remove(Node node) {
            containedNodes.clear(getNodeIndex(node));
        }

        public boolean contains(Node node) {
            return containedNodes.get(getNodeIndex(node));
        }

        private static int getNodeIndex(Node node) {
            Square square = node.square();
            int digit = node.digit();
            int squareId = square.getRow() * 9 + square.getCol();
            return squareId * 9 + (digit - 1);
        }
    }
}