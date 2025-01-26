package pitheguy.sudoku.solver;

import pitheguy.sudoku.gui.Square;

public enum GroupType {
    ROW,
    COLUMN,
    BOX;

    public int get(Square square) {
        return switch (this) {
            case ROW -> square.getRow();
            case COLUMN -> square.getCol();
            case BOX -> square.getBox();
        };
    }
}
