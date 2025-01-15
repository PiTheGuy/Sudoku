package pitheguy.sudoku.util;

import java.util.List;

public record Triplet<T>(T first, T second, T third) {
    public List<T> asList() {
        return List.of(first, second, third);
    }
}
