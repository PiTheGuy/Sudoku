package pitheguy.sudoku.solver;

import java.util.*;

public class DigitCandidates {
    private short flags;

    public DigitCandidates() {
        flags = (short) 0b111_111_111;
    }

    private DigitCandidates(short flags) {
        this.flags = flags;
    }

    public boolean contains(int digit) {
        checkDigit(digit);
        return (flags & (1 << (digit - 1))) != 0;
    }

    public boolean containsAll(DigitCandidates candidates) {
        return (this.flags & candidates.flags) == candidates.flags;
    }

    public boolean remove(int digit) {
        checkDigit(digit);
        boolean contains = contains(digit);
        flags &= (short) ~(1 << (digit - 1));
        return contains;
    }

    public boolean removeAll(DigitCandidates candidates) {
        short oldFlags = this.flags;
        this.flags &= (short) ~candidates.flags;
        return oldFlags != this.flags;
    }

    public void add(int digit) {
        checkDigit(digit);
        flags |= (short) (1 << (digit - 1));
    }

    public boolean setFlags(short flags) {
        short oldFlags = this.flags;
        this.flags = flags;
        return oldFlags != flags;
    }

    public static short getFlags(int... digits) {
        short flags = 0;
        for (int digit : digits) {
            checkDigit(digit);
            flags |= (short) (1 << (digit - 1));
        }
        return flags;
    }


    private static void checkDigit(int digit) {
        if (digit < 1 || digit > 9) throw new IllegalArgumentException("Digit must be between 1 and 9");
    }

    public DigitCandidates or(DigitCandidates other) {
        return new DigitCandidates((short) (flags | other.flags));
    }

    public int count() {
        return Integer.bitCount(flags);
    }

    public int getFirst() {
        if (flags == 0) return -1;
        return Integer.numberOfTrailingZeros(flags) + 1;
    }

    public List<Integer> getAllCandidates() {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= 9; i++) if (contains(i)) candidates.add(i);
        return candidates;
    }

    public boolean isEmpty() {
        return flags == 0;
    }

    public void reset() {
        flags = (short) 0b111_111_111;
    }

    public DigitCandidates copy() {
        return new DigitCandidates(flags);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 1; i <= 9; i++) if (contains(i)) sb.append(i);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DigitCandidates that)) return false;
        return flags == that.flags;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(flags);
    }
}
