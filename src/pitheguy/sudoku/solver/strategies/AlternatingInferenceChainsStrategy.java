package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.*;
import pitheguy.sudoku.util.*;

import java.util.*;
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
        computeAlmostLockedSets();
        if (solveImpl(8, true)) return true;
        if (solveImpl(10, true)) return true;
        if (solveImpl(15, true)) return true;
        if (solveImpl(15, false)) return true;
        return false;
    }

    private boolean solveImpl(int maxDepth, boolean requireClosed) {
        boolean changed = false;
        Set<Cycle> continuousCycles = new LinkedHashSet<>();
        Set<Cycle> discontinuousCycles = new LinkedHashSet<>();
        Set<Cycle> disconnectedCycles = new LinkedHashSet<>();
        for (Square square : sudoku.getAllSquares()) {
            if (square.isSolved()) continue;
            for (int digit : square.getCandidates().getAllCandidates()) {
                findCycles(new Node(square, digit), new Cycle(true), continuousCycles, discontinuousCycles, disconnectedCycles, true, maxDepth, requireClosed);
                findCycles(new Node(square, digit), new Cycle(false), continuousCycles, discontinuousCycles, disconnectedCycles, false, maxDepth, requireClosed);
            }
        }
        for (Cycle cycle : continuousCycles) {
            if (isCycleInvalid(cycle)) continue;
            //System.out.println("Cycle found: " + cycle);
            for (int i = 0; i < cycle.size() - 1; i++) {
                if (cycle.get(i).isSingle() &&
                    cycle.get(i + 1).isSingle() &&
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
                        if (current.digit() == digit && current.isSingle() &&
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
        for (Cycle cycle : discontinuousCycles) {
            //System.out.println("Discontinuous cycle: " + cycle);
            if (isCycleInvalid(cycle)) continue;
            if (!cycle.getFirst().isSingle()) continue;
            Square square = cycle.getFirst().squares().getFirst();
            int digit = cycle.getFirst().digit();
            //System.out.println((cycle.isFirstLinkStrong() ? "Keeping " : "Removing " ) + digit + " on " + square);
            if (cycle.isFirstLinkStrong()) changed |= square.getCandidates().setFlags(DigitCandidates.getFlags(digit));
            else changed |= square.getCandidates().remove(digit);
        }
        if (changed || requireClosed) return changed;
        if (processDigitForcingChains(disconnectedCycles)) return true;
        Map<Node, List<Cycle>> cyclesByStart = createCyclesByStartMap(disconnectedCycles);
        if (processCellForcingChains(cyclesByStart)) return true;
        if (processUnitForcingChains(cyclesByStart)) return true;
        return false;
    }

    private static boolean processDigitForcingChains(Set<Cycle> disconnectedCycles) {
        boolean changed = false;
        cycles:
        for (Cycle cycle : disconnectedCycles) {
            if (isCycleInvalid(cycle)) continue;
            if (!cycle.isFirstLinkStrong()) continue;
            if (!cycle.getFirst().isSingle()) continue;
            if (cycle.getFirst().digit() != cycle.getLast().digit()) continue;
            if (cycle.size() % 2 != 0) continue;
            //System.out.println("Disconnected cycle: " + cycle);
            Square startSquare = cycle.getFirst().squares().getFirst();
            Square endSquare = cycle.getLast().squares().getFirst();
            if (!SolverUtils.isConnected(startSquare, endSquare)) continue;
            Set<Square> affectedSquares = new HashSet<>();
            if (startSquare.getRow() == endSquare.getRow()) affectedSquares.addAll(startSquare.getSurroundingRow());
            if (startSquare.getCol() == endSquare.getCol()) affectedSquares.addAll(startSquare.getSurroundingColumn());
            if (startSquare.getBox() == endSquare.getBox()) affectedSquares.addAll(startSquare.getSurroundingBox());
            int digit = cycle.getFirst().digit();
            for (int i = 1; i < cycle.size() - 1; i++)
                for (Square square : affectedSquares) if (cycle.get(i).squares().contains(square)) continue cycles;
            for (Square square : affectedSquares) {
                if (square == startSquare || square == endSquare) continue;
                if (square.isSolved()) continue;
                //System.out.println("Removing " + digit + " from " + square);
                changed |= square.getCandidates().remove(digit);
            }
        }
        return changed;
    }

    private static Map<Node, List<Cycle>> createCyclesByStartMap(Set<Cycle> disconnectedCycles) {
        Map<Node, List<Cycle>> cyclesByStart = new HashMap<>();
        for (Cycle cycle : disconnectedCycles) {
            if (isCycleInvalid(cycle)) continue;
            if (!cycle.getFirst().isSingle()) continue;
            cyclesByStart.computeIfAbsent(cycle.getFirst(), k -> new ArrayList<>()).add(cycle);
            Cycle reverse = cycle.copy();
            Collections.reverse(reverse);
            cyclesByStart.computeIfAbsent(cycle.getLast(), k -> new ArrayList<>()).add(reverse);
        }
        return cyclesByStart;
    }

    private boolean processCellForcingChains(Map<Node, List<Cycle>> cyclesByStart) {
        boolean changed = false;
        squares:
        for (Square square : sudoku.getAllSquares()) {
            if (square.isSolved()) continue;
            if (square.getCandidates().count() < 2) continue;
            List<Integer> candidates = square.getCandidates().getAllCandidates();
            for (int digit : candidates) {
                Node node = new Node(square, digit);
                if (!cyclesByStart.containsKey(node)) continue squares;
            }
            List<Map<Node, NodeState>> conclusions = new ArrayList<>();
            for (int digit : candidates) {
                Map<Node, NodeState> map = new HashMap<>();
                List<Cycle> cycles = cyclesByStart.get(new Node(square, digit));
                for (Cycle cycle : cycles) {
                    if (cycle.isFirstLinkStrong()) continue;
                    boolean on = true;
                    for (Node node : cycle) {
                        NodeState newState = on ? NodeState.turnOn(map.get(node)) : NodeState.turnOff(map.get(node));
                        map.put(node, newState);
                        on = !on;
                    }
                }
                conclusions.add(map);
            }
            for (Node node : conclusions.getFirst().keySet()) {
                if (!node.isSingle()) continue;
                if (!conclusions.stream().allMatch(map -> map.containsKey(node))) continue;
                if (conclusions.stream().allMatch(map -> map.get(node).isOn()) && node.squares().getFirst().getCandidates().contains(node.digit()))
                    changed |= node.squares().getFirst().getCandidates().setFlags(DigitCandidates.getFlags(node.digit()));
                else if (conclusions.stream().allMatch(map -> map.get(node).isOff()))
                    changed |= node.squares.getFirst().getCandidates().remove(node.digit());
            }
        }
        return changed;
    }

    private boolean processUnitForcingChains(Map<Node, List<Cycle>> cyclesByStart) {
        boolean changed = false;
        changed |= processUnitForUnitForcingChains(cyclesByStart, sudoku::getRow);
        changed |= processUnitForUnitForcingChains(cyclesByStart, sudoku::getColumn);
        changed |= processUnitForUnitForcingChains(cyclesByStart, sudoku::getBox);
        return changed;
    }

    private boolean processUnitForUnitForcingChains(Map<Node, List<Cycle>> cyclesByStart, Function<Integer, List<Square>> unitGetter) {
        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            List<Square> squares = unitGetter.apply(i);
            squares.removeIf(Square::isSolved);
            digits:
            for (int digit = 1; digit <= 9; digit++) {
                if (SolverUtils.hasDigitSolved(squares, digit)) continue;
                List<Map<Node, NodeState>> conclusions = new ArrayList<>();
                List<Square> containedSquares = new ArrayList<>(squares);
                int finalDigit = digit;
                containedSquares.removeIf(square -> !square.getCandidates().contains(finalDigit));
                if (containedSquares.size() == 1) continue;
                for (Square square : containedSquares) {
                    if (!cyclesByStart.containsKey(new Node(square, digit))) continue digits;
                    Map<Node, NodeState> map = new HashMap<>();
                    List<Cycle> cycles = cyclesByStart.get(new Node(square, digit));
                    for (Cycle cycle : cycles) {
                        boolean on = true;
                        for (Node node : cycle) {
                            NodeState newState = on ? NodeState.turnOn(map.get(node)) : NodeState.turnOff(map.get(node));
                            map.put(node, newState);
                            on = !on;
                        }
                    }
                    conclusions.add(map);
                }
                if (conclusions.isEmpty()) continue;
                for (Node node : conclusions.getFirst().keySet()) {
                    if (!node.isSingle()) continue;
                    if (!conclusions.stream().allMatch(map -> map.containsKey(node))) continue;
                    if (conclusions.stream().allMatch(map -> map.get(node).isOn()) && node.squares().getFirst().getCandidates().contains(node.digit()))
                        changed |= node.squares().getFirst().getCandidates().setFlags(DigitCandidates.getFlags(node.digit()));
                    else if (conclusions.stream().allMatch(map -> map.get(node).isOff()))
                        changed |= node.squares.getFirst().getCandidates().remove(node.digit());
                }
            }
        }
        return changed;
    }

    private static boolean isCycleInvalid(Cycle cycle) {
        for (Node node : cycle) {
            for (Square s : node.squares()) {
                if (!s.getCandidates().contains(node.digit())) {
                    return true;
                }
            }
        }
        return false;
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

    private void findCycles(Node node, Cycle cycle, Set<Cycle> continuousCycles, Set<Cycle> discontinuousCycles, Set<Cycle> disconnectedCycles, boolean isStrongLink, int maxDepth, boolean requireClosed) {
        if (cycle.size() > maxDepth) return;
        if (!cycle.isEmpty()) {
            if (cycle.getFirst().equals(node)) {
                if (cycle.size() > 2) {
                    Cycle copy = cycle.copy();
                    copy.close();
                    if (cycle.size() % 2 == 0) continuousCycles.add(copy);
                    else discontinuousCycles.add(copy);
                }
                return;
            } else if (cycle.contains(node)) return;
        }
        if (!validNextNode(cycle, node)) return;
        Set<Node> links = isStrongLink ? findStrongLinks(node) : findWeakLinks(node);
        if (links.isEmpty() || links.size() == 1 && cycle.contains(links.iterator().next())) {
            if (!requireClosed) {
                Cycle copy = cycle.copy();
                copy.add(node);
                if (cycle.size() > 1 && node.isSingle()) disconnectedCycles.add(copy);
            }
            return;
        }
        cycle.add(node);
        for (Node link : links) {
            findCycles(link, cycle, continuousCycles, discontinuousCycles, disconnectedCycles, !isStrongLink, maxDepth, requireClosed);
        }
        cycle.removeLast();
    }

    private boolean validNextNode(Cycle cycle, Node node) {
        for (Square square : node.squares()) {
            for (Node cycleNode : cycle) {
                if (cycleNode.isSingle()) continue;
                if (cycleNode.digit() == node.digit() && cycleNode.squares().contains(square))
                    return false;
            }
        }
        if (!node.isSingle()) for (Square square : node.squares()) if (cycle.contains(square, node.digit())) return false;
        return true;
    }

    private Set<Node> findStrongLinks(Node node) {
        if (strongLinkCache.containsKey(node)) return strongLinkCache.get(node);
        List<Square> squares = node.squares();
        int digit = node.digit();
        Set<Node> links = new LinkedHashSet<>();
        if (node.isSingle() && squares.getFirst().getCandidates().count() == 2) {
            DigitCandidates candidates = squares.getFirst().getCandidates().copy();
            candidates.remove(digit);
            int newDigit = candidates.getFirst();
            links.add(new Node(squares, newDigit));
        }
        if (node.isSingle() && almostLockedSetCache.containsKey(squares.getFirst())) {
            Square square = squares.getFirst();
            List<AlmostLockedSet> sets = almostLockedSetCache.get(square);
            for (AlmostLockedSet set : sets)
                if (set.getExtraDigit() == node.digit() && set.getExtraDigitSquare() == square)
                    links.addAll(set.getPossibleNodes());
        }
        if (node.getConnectionType() != Node.ConnectionType.COLUMN)
            findStrongLinksForGroup(node, squares.getFirst().getSurroundingRow(), digit, GroupType.ROW).ifPresent(links::add);
        if (node.getConnectionType() != Node.ConnectionType.ROW)
            findStrongLinksForGroup(node, squares.getFirst().getSurroundingColumn(), digit, GroupType.COLUMN).ifPresent(links::add);
        if (node.isSingle() || SolverUtils.allInSameGroup(node.squares(), GroupType.BOX))
            findStrongLinksForGroup(node, squares.getFirst().getSurroundingBox(), digit, GroupType.BOX).ifPresent(links::add);
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
        weakLinks.remove(node);
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
        private final Set<Node> containedNodes = new HashSet<>();
        private boolean closed = false;

        public Cycle(boolean firstLinkStrong) {
            this.firstLinkStrong = firstLinkStrong;
        }

        public Cycle(Collection<Node> nodes, boolean firstLinkStrong, Set<Node> containedNodes) {
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
            if (modified) containedNodes.addAll(c);
            return modified;
        }

        @Override
        public boolean remove(Object o) {
            boolean removed = super.remove(o);
            if (removed) containedNodes.remove(o);
            return removed;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean modified = super.removeAll(c);
            if (modified) containedNodes.removeAll(c);
            return modified;
        }

        @Override
        public Node remove(int index) {
            Node removed = super.remove(index);
            if (removed != null) containedNodes.remove(removed);
            return removed;
        }

        @Override
        public Node removeLast() {
            Node removed = super.removeLast();
            if (removed != null) containedNodes.remove(removed);
            return removed;
        }

        @Override
        public boolean contains(Object o) {
            return containedNodes.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return containedNodes.containsAll(c);
        }

        public boolean contains(Square square, int digit) {
            return containedNodes.contains(new Node(square, digit));
        }

        public List<Integer> getContainedDigits() {
            return stream().map(Node::digit).distinct().toList();
        }

        public boolean isFirstLinkStrong() {
            return firstLinkStrong;
        }

        public void close() {
            closed = true;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Cycle other)) return false;
            if (!closed && super.equals(obj) && this.firstLinkStrong == other.firstLinkStrong) return true;
            if (this.size() != other.size()) return false;
            List<Node> doubled = new ArrayList<>(this);
            doubled.addAll(this);
            return Collections.indexOfSubList(doubled, other) != -1;
        }

        @Override
        public int hashCode() {
            if (!closed) return Objects.hash(super.hashCode(), firstLinkStrong);
            Node start = stream().min(Comparator.naturalOrder()).orElseThrow();
            int startIndex = indexOf(start);
            List<Node> normalized = new ArrayList<>(subList(startIndex, size()));
            if (startIndex != 0) normalized.addAll(subList(0, startIndex));
            return normalized.hashCode();
        }

        public Cycle copy() {
            return new Cycle(this, firstLinkStrong, containedNodes);
        }
    }

    private static final class Node implements Comparable<Node> {
        private final List<Square> squares;
        private final int digit;
        private ConnectionType connectionType;
        private final boolean single;
        private final int hashCode;

        public Node(List<Square> squares, int digit) {
            this.squares = squares;
            this.digit = digit;
            this.single = squares.size() == 1;
            this.hashCode = computeHashCode();
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
            if (single) return ConnectionType.SINGLE;
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

        public boolean isSingle() {
            return single;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            return this.hashCode == obj.hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        private int computeHashCode() {
            if (single) return squares.getFirst().getIndex() * 31 + digit;
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

    private enum NodeState {
        ON,
        OFF,
        BOTH;

        public boolean isOn() {
            return this == ON || this == BOTH;
        }

        public boolean isOff() {
            return this == OFF || this == BOTH;
        }

        public static NodeState turnOn(NodeState state) {
            if (state == null) return ON;
            return switch (state) {
                case ON -> ON;
                case OFF, BOTH -> BOTH;
            };
        }

        public static NodeState turnOff(NodeState state) {
            if (state == null) return OFF;
            return switch (state) {
                case OFF -> OFF;
                case ON, BOTH -> BOTH;
            };
        }
    }
}