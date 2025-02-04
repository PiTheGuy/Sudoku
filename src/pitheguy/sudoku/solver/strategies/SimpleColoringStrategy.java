package pitheguy.sudoku.solver.strategies;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;
import pitheguy.sudoku.solver.SolveStrategy;
import pitheguy.sudoku.solver.SolverUtils;
import pitheguy.sudoku.util.Pair;
import pitheguy.sudoku.util.Util;

import java.util.*;
import java.util.List;

public class SimpleColoringStrategy extends SolveStrategy {
    public SimpleColoringStrategy(Sudoku sudoku) {
        super(sudoku);
    }

    @Override
    public boolean solve() {
        for (int digit = 1; digit <= 9; digit++) {
            Set<Square> squares = new HashSet<>();
            for (Square square : sudoku.getAllSquares()) {
                if (square.isSolved()) continue;
                if (square.getCandidates().contains(digit)) squares.add(square);
            }
            NodeGraph graph = new NodeGraph(squares);
            List<Square> squareList = new ArrayList<>(squares);
            for (Pair<Square> pair : Util.getAllPairs(squareList)) {
                if (!SolverUtils.isConnected(pair.first(), pair.second())) continue;
                Node node1 = graph.getNode(pair.first());
                Node node2 = graph.getNode(pair.second());
                if (node1.color() == node2.color() && node1.network() == node2.network()) {
                    //System.out.println("Match found: " + pair.first() + " and " + pair.second() + " are both " + node1.color() + " for digit " + digit);
                    for (Square square : graph.getAllSquaresForColor(node1.color(), node1.network()))
                        square.getCandidates().remove(digit);
                    return true;
                }
            }
        }
        return false;
    }

    public record Node(Square square, NodeColor color, Set<Node> connections, int network) {
        public Node(Square square, NodeColor color, int network) {
            this(square, color, new HashSet<>(), network);
        }

        public void connectTo(Node node) {
            if (node.color() == color) throw new IllegalArgumentException("Can only connect to opposite color");
            connections.add(node);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node[square=");
            sb.append(square);
            sb.append(", color=");
            sb.append(color);
            sb.append(", network=");
            sb.append(network);
            sb.append(", connections=[");
            for (Node node : connections) sb.append(node.square());
            sb.append("]]");
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node node)) return false;
            return network == node.network && Objects.equals(square, node.square) && color == node.color;
        }

        @Override
        public int hashCode() {
            return Objects.hash(square, color, network);
        }
    }

    public static class NodeGraph {
        private final Map<Square, Node> nodes = new HashMap<>();
        private final Set<Square> possibleSquares;

        public NodeGraph(Set<Square> possibleSquares) {
            this.possibleSquares = possibleSquares;
            int network = 0;
            for (Square square : possibleSquares) {
                if (!nodes.containsKey(square)) createGraphFromRoot(square, network++);
            }
        }

        private void createGraphFromRoot(Square root, int network) {
            Node rootNode = new Node(root, NodeColor.BLUE, network);
            createGraphRecursive(rootNode, network);
        }

        private void createGraphRecursive(Node node, int network) {
            Square square = node.square();
            NodeColor color = node.color();
            if (nodes.containsKey(square)) return;
            nodes.put(square, node);
            List<Optional<Square>> possibleLinks = new ArrayList<>();
            possibleLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingRow(), s -> s != square && possibleSquares.contains(s), true));
            possibleLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingColumn(), s -> s != square && possibleSquares.contains(s), true));
            possibleLinks.add(SolverUtils.getOnlySquareThat(square.getSurroundingBox(), s -> s != square && possibleSquares.contains(s), true));
            for (Optional<Square> possibleLink : possibleLinks) {
                if (possibleLink.isEmpty()) continue;
                Square link = possibleLink.get();
                if (nodes.containsKey(link)) {
                    Node linkNode = nodes.get(link);
                    if (linkNode.color() == color) continue;
                    node.connectTo(linkNode);
                    linkNode.connectTo(node);
                } else {
                    Node linkNode = new Node(link, color.getOpposite(), network);
                    node.connectTo(linkNode);
                    createGraphRecursive(linkNode, network);
                }
            }
        }

        public Node getNode(Square square) {
            return nodes.get(square);
        }

        public List<Square> getAllSquaresForColor(NodeColor color, int network) {
            List<Square> squares = new ArrayList<>();
            for (Map.Entry<Square, Node> entry : nodes.entrySet())
                if (entry.getValue().color() == color && entry.getValue().network() == network) squares.add(entry.getKey());
            return squares;
        }
    }

    public enum NodeColor {
        BLUE,
        GREEN;

        public NodeColor getOpposite() {
            return switch (this) {
                case BLUE -> GREEN;
                case GREEN -> BLUE;
            };
        }
    }
}
