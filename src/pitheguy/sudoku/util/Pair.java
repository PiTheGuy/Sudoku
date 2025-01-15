package pitheguy.sudoku.util;

import java.util.List;

public record Pair<T>(T first, T second) {
    public List<T> asList() {
        return List.of(first, second);
    }
}
