package nl.yannickl88.imageview.view.layout;

import java.awt.*;

/**
 * Layout which orders the components in columns based on the available width. A minimum cell size is used. Rows are
 * not spaced but appended right after each other. The first and last column do not have outer padding, everything in
 * between is spaced out evenly.
 *
 * This is similar how Windows 10 explorer tiles folders and items.
 */
public class ColumnLayout implements LayoutManager {
    private final Component component;
    private final int minimalCellSize;

    /**
     * Data class which contains the grid information.
     */
    private static class Grid {
        public final int preferredWidth, columns, rows;

        public Grid(int preferredWidth, int columns, int rows) {
            this.preferredWidth = preferredWidth;
            this.columns = columns;
            this.rows = rows;
        }
    }

    /**
     * Creates a column layout based on a parent component and the minimal cell size. When used in combination with a
     * {@code JScrollPane}, use the Viewport of the pane as the component.
     */
    public ColumnLayout(Component component, int minimalCellSize) {
        this.component = component;
        this.minimalCellSize = minimalCellSize;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {

    }

    @Override
    public void removeLayoutComponent(Component comp) {

    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Grid grid = getGrid(parent);

        return new Dimension(grid.preferredWidth, grid.rows * minimalCellSize);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Grid grid = getGrid(parent);

        return new Dimension(grid.columns * minimalCellSize, grid.rows * minimalCellSize);
    }

    @Override
    public void layoutContainer(Container parent) {
        Grid grid = getGrid(parent);

        synchronized (parent.getTreeLock()) {
            int index = 0;

            for (int i = 0, nComps = parent.getComponentCount(); i < nComps ; i++) {
                Component c = parent.getComponent(i);
                if (c.isVisible()) {
                    c.setBounds(
                            getOffsetInRow(grid, (index % grid.columns)),
                            ((int) Math.floor(index / (double) grid.columns)) * minimalCellSize,
                            minimalCellSize,
                            minimalCellSize
                    );

                    index++;
                }
            }
        }
    }

    public String toString() {
        return getClass().getName() + "[]";
    }

    /**
     * Calculate the grid to be used for the layout.
     */
    private Grid getGrid(Container parent) {
        int parentWidth = this.component.getWidth();

        if (parentWidth == 0) {
            return new Grid(0, 0, 0);
        }

        synchronized (parent.getTreeLock()) {
            int count = 0;
            for (int i = 0, nComps = parent.getComponentCount(); i < nComps ; i++) {
                Component c = parent.getComponent(i);
                if (c.isVisible()) {
                    count++;
                }
            }

            int columns = Math.min(count, (int) Math.floor(parentWidth / (double) minimalCellSize));
            int rows = (int) Math.ceil(count / (double) columns);

            return new Grid(parentWidth, columns, rows);
        }
    }

    /**
     * Calculate the offset needed based on the Grid and for which column.
     */
    private int getOffsetInRow(Grid grid, int column) {
        // First item in the column is aligned to the left side of the container
        if (column == 0) {
            return 0;
        }

        // Last item in the column is aligned to the right side of the container
        if (column == grid.columns - 1) {
            return grid.preferredWidth - minimalCellSize;
        }

        // Middle item in the column are equally spread out over the remaining width container
        int padding = (grid.preferredWidth - (minimalCellSize * grid.columns)) / (grid.columns - 1);

        return (padding + minimalCellSize) * column;
    }
}
