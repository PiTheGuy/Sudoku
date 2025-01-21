package pitheguy.sudoku.util;

import pitheguy.sudoku.gui.Square;
import pitheguy.sudoku.gui.Sudoku;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SquareSet implements Iterable<Square> {
    private final Sudoku sudoku;
    private final BitSet contained;

    public SquareSet(Sudoku sudoku) {
        this(sudoku, new BitSet(81));
    }

    public SquareSet(Sudoku sudoku, Collection<Square> squares) {
        this(sudoku, new BitSet(81));
        addAll(squares);
    }

    private SquareSet(Sudoku sudoku, BitSet contained) {
        this.sudoku = sudoku;
        this.contained = contained;
    }

    public int size() {
        return contained.cardinality();
    }

    public boolean isEmpty() {
        return contained.isEmpty();
    }

    public void add(Square square) {
        contained.set(square.getRow() * 9 + square.getCol());
    }

    public void addAll(SquareSet squareSet) {
        contained.or(squareSet.contained);
    }

    public void addAll(Collection<Square> squares) {
        for (Square square : squares) add(square);
    }

    public void remove(Square square) {
        contained.clear(square.getRow() * 9 + square.getCol());
    }

    public void removeAll(SquareSet squareSet) {
        contained.andNot(squareSet.contained);
    }

    public void removeAll(Collection<Square> c) {
        removeAll(new SquareSet(sudoku, c));
    }

    public boolean contains(Square square) {
        return contained.get(square.getRow() * 9 + square.getCol());
    }

    public boolean containsAny(SquareSet squareSet) {
        return contained.intersects(squareSet.contained);
    }

    public SquareSet copy() {
        return new SquareSet(sudoku, contained);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Square square : this) sb.append(square).append(",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Iterator<Square> iterator() {
        return new Iterator<>() {
            private int nextIndex = contained.nextSetBit(0);

            @Override
            public boolean hasNext() {
                return nextIndex != -1;
            }

            @Override
            public Square next() {
                if (nextIndex == -1) throw new NoSuchElementException();
                int currentIndex = nextIndex;
                nextIndex = contained.nextSetBit(nextIndex + 1);
                int row = currentIndex / 9;
                int col = currentIndex % 9;
                return sudoku.getSquare(row, col);
            }
        };
    }
}
