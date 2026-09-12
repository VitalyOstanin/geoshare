/**
 * The circle that stands for a member: the picture their account carries, or
 * the first letter of their name when there is none.
 *
 * Shared by the pins on the map and the cards in the panel, so that the same
 * person is drawn the same way wherever they appear.
 */

import { memberInitial, pictureAddress } from './members.js';

/**
 * Set as a background rather than as an image element, so that a portrait of
 * any shape is cropped to the circle instead of being squeezed into it.
 */
export function showFace(face, pictureUrl, name, colour) {
    const picture = pictureAddress(pictureUrl);

    if (picture) {
        // Quoted through the JSON encoder rather than by hand: the address is
        // written by whoever holds the account, and this is what keeps a quote
        // in it from ending the CSS string early.
        face.style.backgroundImage = `url(${JSON.stringify(picture)})`;
        face.textContent = '';

        return;
    }

    face.style.backgroundImage = '';
    face.style.color = colour;
    face.textContent = memberInitial(name);
}
