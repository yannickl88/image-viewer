package nl.yannickl88.imageview.view.layout;

import java.awt.*;

public class ColumnLayout implements LayoutManager {
    private final Component component;
    private final int minimalCellSize;

    private static class Grid {
        public final int preferredWidth, columns, rows;

        public Grid(int preferredWidth, int columns, int rows) {
            this.preferredWidth = preferredWidth;
            this.columns = columns;
            this.rows = rows;
        }
    }

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

    private int getOffsetInRow(Grid grid, int index) {
        // First image in the column is aligned to the left side of the container
        if (index == 0) {
            return 0;
        }

        // Last image in the column is aligned to the right side of the container
        if (index == grid.columns - 1) {
            return grid.preferredWidth - minimalCellSize;
        }

        // Middle images in the column are equally spread out over the remaining width container
        int padding = (grid.preferredWidth - (minimalCellSize * grid.columns)) / (grid.columns - 1);

        return (padding + minimalCellSize) * index;
    }
}
