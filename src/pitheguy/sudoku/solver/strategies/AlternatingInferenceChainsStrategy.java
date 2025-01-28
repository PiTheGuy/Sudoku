package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.DigitCandidates;
import pitheguy.sudoku.solver.GroupType;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.ListComparator;
import pitheguy.sudoku.util.Pair;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlternatingInferenceChainsStrategy extends SolveStrategy {
    private static final List<Pair<Integer>> GROUP_ELIGIBLE_INDEX_PAIRS = Util.getAllPairs(IntStream.range(0, 9).boxed().toList()).stream()
            .filter(pair -> pair.first() / 3 == pair.second() / 3).toList();
    private static final List<List<Integer>> GROUP_ELIGIBLE_INDEX_TRIPLETS = List.of(List.of(0, 1, 2), List.of(3, 4, 5), List.of(6, 7, 8));
    private final Map<Node, Set<Node>> strongLinkCache = new HashMap<>();
    private final Map<Node, Set<Node>> weakLinkCache = new HashMap<>();
    private final Map<Square, List<AlmostLockedSet>> almostLockedSetCache = new HashMap<>();

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
        computeAlmostLockedSets();
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
                if (!node.squares().stream().allMatch(s -> s.getCandidates().contains(node.digit()))) continue cycles;
            //System.out.println("Cycle found: " + cycle);
            for (int i = 0; i < cycle.size() - 1; i++) {
                if (cycle.get(i).getConnectionType() == Node.ConnectionType.SINGLE &&
                    cycle.get(i + 1).getConnectionType() == Node.ConnectionType.SINGLE &&
                    cycle.get(i).squares().getFirst() == cycle.get(i + 1).squares().getFirst()) {
                    short flags = DigitCandidates.getFlags(cycle.get(i).digit(), cycle.get(i + 1).digit());
                    //System.out.println("Removing all but " + cycle.get(i).digit + " and " + cycle.get(i + 1).digit + " from " + cycle.get(i).squares().getFirst());
                    changed |= cycle.get(i).squares().getFirst().getCandidates().setFlags(flags);
                }
            }
            for (int digit : cycle.getContainedDigits()) {
                for (Square square : sudoku.getAllSquares()) {
                    Node node = new Node(square, digit);
                    if (square.isSolved()) continue;
                    if (cycle.contains(node)) continue;
                    if (!square.getCandidates().contains(digit)) continue;
                    List<Node> visibleNodes = new ArrayList<>();
                    for (Node current : cycle)
                        if (current.digit() == digit && current.getConnectionType() == Node.ConnectionType.SINGLE &&
                            SolverUtils.isConnected(square, current.squares().getFirst())) visibleNodes.add(current);
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
            //System.out.println("Discontinuous cycle: " + cycle);
            for (Node node : cycle)
                if (!node.squares().stream().allMatch(s -> s.getCandidates().contains(node.digit()))) continue cycles;
            if (cycle.getFirst().getConnectionType() != Node.ConnectionType.SINGLE) continue;
            Square square = cycle.getFirst().squares().getFirst();
            int digit = cycle.getFirst().digit();
            //System.out.println((cycle.isFirstLinkStrong() ? "Keeping " : "Removing " ) + digit + " on " + square);
            if (cycle.isFirstLinkStrong()) changed |= square.getCandidates().setFlags(DigitCandidates.getFlags(digit));
            else changed |= square.getCandidates().remove(digit);
        }
        return changed;
    }

    private void computeAlmostLockedSets() {
        for (Pair<Square> pair : Util.getAllPairs(sudoku.getAllSquares())) {
            if (!SolverUtils.isConnectedNoBox(pair.first(), pair.second())) continue;
            if (pair.first().isSolved() || pair.second().isSolved()) continue;
            if (pair.first().getCandidates().or(pair.second().getCandidates()).count() != 3) continue;
            DigitCandidates sharedCandidates = pair.first().getCandidates().and(pair.second().getCandidates());
            if (sharedCandidates.count() != 2) continue;
            AlmostLockedSet als = new AlmostLockedSet(pair.first(), pair.second());
            almostLockedSetCache.computeIfAbsent(pair.first(), s -> new ArrayList<>()).add(als);
            almostLockedSetCache.computeIfAbsent(pair.second(), s -> new ArrayList<>()).add(als);
        }
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
        if (!validNextNode(cycle, node)) return;
        Set<Node> links = isStrongLink ? findStrongLinks(node) : findWeakLinks(node);
        if (links.isEmpty()) return;
        cycle.add(node);
        for (Node link : links) {
            findCycles(link, cycle, continuousCycles, discontinuousCycles, !isStrongLink, maxDepth);
        }
        cycle.removeLast();
    }

    private boolean validNextNode(Cycle cycle, Node node) {
        for (Square square : node.squares()) {
            for (Node cycleNode : cycle) {
                if (cycleNode.connectionType == Node.ConnectionType.SINGLE) continue;
                if (cycleNode.digit() == node.digit() && cycleNode.squares().contains(square))
                    return false;
            }
        }
        if (node.getConnectionType() != Node.ConnectionType.SINGLE) for (Square square : node.squares()) if (cycle.contains(square, node.digit())) return false;
        return true;
    }

    private Set<Node> findStrongLinks(Node node) {
        if (strongLinkCache.containsKey(node)) return strongLinkCache.get(node);
        List<Square> squares = node.squares();
        int digit = node.digit();
        Set<Node> links = new LinkedHashSet<>();
        if (node.getConnectionType() == Node.ConnectionType.SINGLE && squares.getFirst().getCandidates().count() == 2) {
            DigitCandidates candidates = squares.getFirst().getCandidates().copy();
            candidates.remove(digit);
            int newDigit = candidates.getFirst();
            links.add(new Node(squares, newDigit));
        }
        if (node.getConnectionType() == Node.ConnectionType.SINGLE && almostLockedSetCache.containsKey(squares.getFirst())) {
            Square square = squares.getFirst();
            List<AlmostLockedSet> sets = almostLockedSetCache.get(square);
            for (AlmostLockedSet set : sets)
                if (set.getExtraDigit() == node.digit() && set.getExtraDigitSquare() == square)
                    links.addAll(set.getPossibleNodes());
        }
        List<Optional<Node>> externalLinks = new ArrayList<>();
        if (node.getConnectionType() != Node.ConnectionType.COLUMN)
            externalLinks.add(findStrongLinksForGroup(node, squares.getFirst().getSurroundingRow(), digit, GroupType.ROW));
        if (node.getConnectionType() != Node.ConnectionType.ROW)
            externalLinks.add(findStrongLinksForGroup(node, squares.getFirst().getSurroundingColumn(), digit, GroupType.COLUMN));
        if (node.getConnectionType() == Node.ConnectionType.SINGLE || SolverUtils.allInSameGroup(node.squares(), GroupType.BOX))
            externalLinks.add(findStrongLinksForGroup(node, squares.getFirst().getSurroundingBox(), digit, GroupType.BOX));
        links.addAll(externalLinks.stream().filter(Optional::isPresent).map(Optional::get).toList());
        strongLinkCache.put(node, links);
        return links;
    }

    private Optional<Node> findStrongLinksForGroup(Node node, List<Square> group, int digit, GroupType groupType) {
        Square square = node.squares().getFirst();
        List<Square> containedSquares = new ArrayList<>();
        for (Square groupSquare : group) {
            if (node.squares().contains(groupSquare)) continue;
            if (groupSquare.isSolved()) continue;
            if (groupSquare.getCandidates().contains(digit)) containedSquares.add(groupSquare);
        }
        if (containedSquares.isEmpty()) return Optional.empty();
        else if (containedSquares.size() == 1) return Optional.of(new Node(containedSquares.getFirst(), digit));
        else if (containedSquares.size() == 2 || containedSquares.size() == 3) {
            if (groupType == GroupType.BOX) {
                if (SolverUtils.allInSameGroup(containedSquares, false))
                    return Optional.of(new Node(containedSquares, digit));
                else return Optional.empty();
            }
            if (!SolverUtils.allInSameGroup(containedSquares, GroupType.BOX)) return Optional.empty();
            Function<Square, Integer> indexExtractor = groupType == GroupType.ROW ? Square::getRow : Square::getCol;
            if (indexExtractor.apply(square).equals(indexExtractor.apply(containedSquares.getFirst())) ||
                square.getBox() == containedSquares.getFirst().getBox()) {
                return Optional.of(new Node(containedSquares, digit));
            }
        }
        return Optional.empty();
    }

    private Set<Node> findWeakLinks(Node node) {
        if (weakLinkCache.containsKey(node)) return weakLinkCache.get(node);
        List<Square> squares = node.squares();
        int digit = node.digit();
        Set<Node> weakLinks = new LinkedHashSet<>();
        if (node.getConnectionType() == Node.ConnectionType.SINGLE && squares.getFirst().getCandidates().count() > 1) {
            Square square = squares.getFirst();
            for (int c : square.getCandidates().getAllCandidates())
                if (c != digit) weakLinks.add(new Node(square, c));
        }
        if (node.getConnectionType() != Node.ConnectionType.COLUMN)
            weakLinks.addAll(findWeakLinksForGroup(squares.getFirst().getSurroundingRow(), digit, GroupType.ROW));
        if (node.getConnectionType() != Node.ConnectionType.ROW)
            weakLinks.addAll(findWeakLinksForGroup(squares.getFirst().getSurroundingColumn(), digit, GroupType.COLUMN));
        if (node.getConnectionType() == Node.ConnectionType.SINGLE || SolverUtils.allInSameGroup(node.squares(), GroupType.BOX))
            weakLinks.addAll(findWeakLinksForGroup(squares.getFirst().getSurroundingBox(), digit, GroupType.BOX));
        weakLinkCache.put(node, weakLinks);
        return weakLinks;
    }

    private List<Node> findWeakLinksForGroup(List<Square> group, int digit, GroupType groupType) {
        if (SolverUtils.hasDigitSolved(group, digit)) return Collections.emptyList();
        List<Node> links = group.stream().filter(s -> !s.isSolved() && s.getCandidates().contains(digit))
                .map(s -> new Node(s, digit)).collect(Collectors.toList());
        if (groupType == GroupType.BOX) return links;
        for (Pair<Integer> pair : GROUP_ELIGIBLE_INDEX_PAIRS) {
            Pair<Square> squarePair = pair.map(group::get);
            if (squarePair.first().isSolved() || squarePair.second().isSolved()) continue;
            if (squarePair.first().getCandidates().contains(digit) &&
                squarePair.second().getCandidates().contains(digit)) {
                links.add(new Node(squarePair.asList(), digit));
            }
        }
        for (List<Integer> triplet : GROUP_ELIGIBLE_INDEX_TRIPLETS) {
            List<Square> squares = triplet.stream().map(group::get).toList();
            if (squares.stream().anyMatch(Square::isSolved)) continue;
            if (squares.stream().allMatch(s -> s.getCandidates().contains(digit)))
                links.add(new Node(squares, digit));
        }
        return links;
    }

    private static class Cycle extends ArrayList<Node> {
        private final boolean firstLinkStrong;
        private final NodeSet containedSingleNodes = new NodeSet();

        public Cycle(boolean firstLinkStrong) {
            this.firstLinkStrong = firstLinkStrong;
        }

        public Cycle(Collection<Node> nodes, boolean firstLinkStrong, NodeSet containedSingleNodes) {
            super(nodes);
            this.firstLinkStrong = firstLinkStrong;
            this.containedSingleNodes.addAll(containedSingleNodes);
        }

        @Override
        public boolean add(Node node) {
            boolean added = super.add(node);
            if (added && node.getConnectionType() == Node.ConnectionType.SINGLE) containedSingleNodes.add(node);
            return added;
        }

        @Override
        public boolean addAll(Collection<? extends Node> c) {
            boolean modified = super.addAll(c);
            if (modified)
                c.stream().filter(node -> node.getConnectionType() == Node.ConnectionType.SINGLE).forEach(containedSingleNodes::add);
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
            if (removed.getConnectionType() == Node.ConnectionType.SINGLE)
                containedSingleNodes.remove(removed);
            return removed;
        }

        @Override
        public Node removeLast() {
            return remove(size() - 1);
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Node node) {
                if (node.getConnectionType() == Node.ConnectionType.SINGLE) return containedSingleNodes.contains(node);
                else return super.contains(o);
            } else return false;
        }

        public boolean contains(Square square, int digit) {
            return containedSingleNodes.contains(square, digit);
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
            Node start = stream().min(Comparator.naturalOrder()).orElseThrow();
            int startIndex = indexOf(start);
            List<Node> normalized = new ArrayList<>(subList(startIndex, size()));
            if (startIndex != 0) normalized.addAll(subList(0, startIndex));
            return normalized.hashCode();
        }

        public Cycle copy() {
            return new Cycle(this, firstLinkStrong, containedSingleNodes);
        }
    }

    private static final class Node implements Comparable<Node> {
        private final List<Square> squares;
        private final int digit;
        private ConnectionType connectionType;

        public Node(List<Square> squares, int digit) {
            this.squares = squares;
            this.digit = digit;
        }

        public Node(Square square, int digit) {
            this(List.of(square), digit);
        }

        @Override
        public int compareTo(Node o) {
            if (o == null) return 1;
            int squaresCompare = new ListComparator<Square>().compare(squares, o.squares);
            if (squaresCompare != 0) return squaresCompare;
            return digit - o.digit;
        }

        public ConnectionType getConnectionType() {
            if (connectionType == null) connectionType = computeConnectionType();
            return connectionType;
        }

        private ConnectionType computeConnectionType() {
            if (squares.size() == 1) return ConnectionType.SINGLE;
            if (squares.get(0).getRow() == squares.get(1).getRow()) return ConnectionType.ROW;
            if (squares.get(0).getCol() == squares.get(1).getCol()) return ConnectionType.COLUMN;
            throw new IllegalStateException("Squares in node are not connected");
        }

        public List<Square> squares() {
            return squares;
        }

        public int digit() {
            return digit;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Node) obj;
            if (this.digit != that.digit) return false;
            if (this.getConnectionType() != that.getConnectionType()) return false;
            if (this.getConnectionType() == ConnectionType.SINGLE) return this.squares.getFirst() == that.squares.getFirst();
            return Objects.equals(this.squares, that.squares);
        }

        @Override
        public int hashCode() {
            if (getConnectionType() == ConnectionType.SINGLE) return squares.getFirst().getIndex() * 31 + digit;
            return squares.hashCode() * 31 + digit;
        }

        @Override
        public String toString() {
            return "Node[" +
                   "squares=" + squares + ", " +
                   "digit=" + digit + ']';
        }


        public enum ConnectionType {
            SINGLE,
            ROW,
            COLUMN
        }
    }

    private static class NodeSet {
        private final BitSet containedNodes = new BitSet();

        public void add(Node node) {
            if (node.getConnectionType() != Node.ConnectionType.SINGLE)
                throw new IllegalArgumentException("Only single nodes are allowed");
            containedNodes.set(getNodeIndex(node));
        }

        public void addAll(NodeSet nodes) {
            containedNodes.or(nodes.containedNodes);
        }

        public void remove(Node node) {
            if (node.getConnectionType() == Node.ConnectionType.SINGLE) containedNodes.clear(getNodeIndex(node));
        }

        public boolean contains(Node node) {
            if (node.getConnectionType() == Node.ConnectionType.SINGLE) return containedNodes.get(getNodeIndex(node));
            else return false;
        }

        public boolean contains(Square square, int digit) {
            int squareId = square.getRow() * 9 + square.getCol();
            int index = squareId * 9 + (digit - 1);
            return containedNodes.get(index);
        }

        private static int getNodeIndex(Node node) {
            Square square = node.squares().getFirst();
            int digit = node.digit();
            int squareId = square.getRow() * 9 + square.getCol();
            return squareId * 9 + (digit - 1);
        }
    }

    private record AlmostLockedSet(Square square1, Square square2) {
        public List<Square> getSquares() {
            return List.of(square1, square2);
        }

        public DigitCandidates getSharedCandidates() {
            return square1.getCandidates().and(square2.getCandidates());
        }

        public int getExtraDigit() {
            DigitCandidates candidates = square1.getCandidates().or(square2.getCandidates());
            candidates.removeAll(getSharedCandidates());
            return candidates.getFirst();
        }

        public Square getExtraDigitSquare() {
            int extraDigit = getExtraDigit();
            if (square1.getCandidates().contains(extraDigit)) return square1;
            else return square2;
        }

        public List<Node> getPossibleNodes() {
            List<Node> possibleNodes = new ArrayList<>();
            for (int digit : getSharedCandidates().getAllCandidates()) possibleNodes.add(new Node(getSquares(), digit));
            return possibleNodes;
        }
    }
}