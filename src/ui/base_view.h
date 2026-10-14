#ifndef UI_BASE_VIEW_H
#define UI_BASE_VIEW_H

#include "raylib.h"
#include "ui/color_theme.h"

/**
 * "Abstract base class" for visualization views.
 * Concrete views embed a BaseView as their first member and provide
 * the virtual functions through a BaseViewVTable.
 */
typedef struct BaseView BaseView;

typedef struct {
    void (*draw_content)(BaseView *view);   // Draw the visualization
    void (*draw_grid)(BaseView *view);      // Draw grid lines
    void (*reset)(BaseView *view);          // Reset the view state
} BaseViewVTable;

struct BaseView {
    const BaseViewVTable *vtable;
    Rectangle bounds;               // Area of the window the view draws into
    const ColorTheme *color_theme;
};

static inline void base_view_init(BaseView *view, const BaseViewVTable *vtable) {
    view->vtable = vtable;
    view->bounds = (Rectangle) { 0, 0, 0, 0 };
    view->color_theme = color_theme_get(THEME_DARK_BLUE);
}

static inline void base_view_set_color_theme(BaseView *view, const ColorTheme *theme) {
    view->color_theme = theme;
}

static inline void base_view_set_bounds(BaseView *view, Rectangle bounds) {
    view->bounds = bounds;
}

/** Clear with the theme background, draw the grid, then the content (clipped to bounds). */
static inline void base_view_draw(BaseView *view) {
    Rectangle b = view->bounds;
    if (b.width <= 0 || b.height <= 0) return;

    BeginScissorMode((int) b.x, (int) b.y, (int) b.width, (int) b.height);
    DrawRectangleRec(b, view->color_theme->canvas_background);
    view->vtable->draw_grid(view);
    view->vtable->draw_content(view);
    EndScissorMode();
}

static inline void base_view_reset(BaseView *view) {
    view->vtable->reset(view);
}

#endif
