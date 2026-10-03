// Telling a click on a window's backdrop from a click on the window itself.

/**
 * Whether a point falls outside a box.
 *
 * Kept apart from the dialogs so that the arithmetic can be read and tested on
 * its own: a click on the backdrop and a click on the padding of the window
 * are the same event with the same target, and only the coordinates tell them
 * apart. The edge belongs to the box, because a click on the border of a
 * window is a click on that window.
 */
export function outsideTheBox(box, point) {
    return point.clientX < box.left ||
        point.clientX > box.right ||
        point.clientY < box.top ||
        point.clientY > box.bottom;
}
