package pitheguy.sudoku.util;

import java.util.List;
import java.util.function.Function;

public record Pair<T>(T first, T second) {
    public List<T> asList() {
        return List.of(first, second);
    }

    @Override
    public String toString() {
        return "Pair[" + first + ", " + second + "]";
    }

    public <R> Pair<R> map (Function<T, ? extends R> mapper) {
        return new Pair<>(mapper.apply(first), mapper.apply(second));
    }
}
