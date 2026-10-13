// The QR code of a link, drawn as SVG elements rather than markup.

import encodeQR from './vendor/qr-0.7.2/qr.mjs';

const SVG = 'http://www.w3.org/2000/svg';

/**
 * One SVG path for the dark modules of a QR matrix: each run of dark modules
 * in a row is one rectangle, `border` modules in from the edge.
 */
export function qrPath(matrix, border = 0) {
    const parts = [];

    matrix.forEach((row, y) => {
        let x = 0;

        while (x < row.length) {
            if (!row[x]) {
                x += 1;
                continue;
            }

            const start = x;

            while (x < row.length && row[x]) {
                x += 1;
            }

            parts.push(`M${start + border} ${y + border}h${x - start}v1h-${x - start}z`);
        }
    });

    return parts.join('');
}

/**
 * The QR code of `text` as an <svg> element of `document`, with the quiet zone
 * of four modules the standard asks for, scaling to whatever box holds it.
 */
export function qrSvg(document, text) {
    // The encoder's own border is dropped from the matrix it hands back, so
    // the quiet zone is added once, here.
    const matrix = encodeQR(text, 'raw', { ecc: 'medium', border: 1 }).slice(1, -1)
        .map((row) => row.slice(1, -1));
    const border = 4;
    const side = matrix.length + border * 2;
    const svg = document.createElementNS(SVG, 'svg');
    const background = document.createElementNS(SVG, 'rect');
    const modules = document.createElementNS(SVG, 'path');

    svg.setAttribute('viewBox', `0 0 ${side} ${side}`);
    svg.setAttribute('shape-rendering', 'crispEdges');
    svg.setAttribute('role', 'img');
    background.setAttribute('width', side);
    background.setAttribute('height', side);
    background.setAttribute('fill', '#ffffff');
    modules.setAttribute('d', qrPath(matrix, border));
    modules.setAttribute('fill', '#000000');
    svg.append(background, modules);

    return svg;
}
