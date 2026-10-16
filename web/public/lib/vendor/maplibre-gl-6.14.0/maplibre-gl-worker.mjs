// Built-ins MapLibre 6 calls that browsers before Chrome 93 lack, put at the
// head of the library and of its worker by tools/vendor-web.sh: Object.hasOwn
// (Chrome 93) and at() on arrays, strings and typed arrays (Chrome 92). Each is
// defined only where the browser has none of its own.
(() => {
    const define = (target, name, value) => {
        if (!(name in target)) {
            Object.defineProperty(target, name, { value, configurable: true, writable: true });
        }
    };
    define(Object, 'hasOwn', (object, key) => Object.prototype.hasOwnProperty.call(object, key));
    function at(index) {
        const n = Math.trunc(index) || 0;
        const i = n < 0 ? n + this.length : n;
        return i < 0 || i >= this.length ? undefined : this[i];
    }
    for (const type of [Array, String, Object.getPrototypeOf(Int8Array)]) {
        define(type.prototype, 'at', at);
    }
})();
/**
* MapLibre GL JS
* @license 3-Clause BSD. Full text of license: https://github.com/maplibre/maplibre-gl-js/blob/v6.14.0/LICENSE.txt
*/
var _a, _b, _c2;
var e = Object.create, t = Object.defineProperty, n = Object.getOwnPropertyDescriptor, r = Object.getOwnPropertyNames, i = Object.getPrototypeOf, a = Object.prototype.hasOwnProperty, o = (e49, t2) => () => (t2 || (e49((t2 = { exports: {} }).exports, t2), e49 = null), t2.exports), s = (e49, i2, o2, s2) => {
  if (i2 && typeof i2 == `object` || typeof i2 == `function`) for (var c2 = r(i2), l2 = 0, u2 = c2.length, d2; l2 < u2; l2++) d2 = c2[l2], !a.call(e49, d2) && d2 !== o2 && t(e49, d2, { get: ((e50) => i2[e50]).bind(null, d2), enumerable: !(s2 = n(i2, d2)) || s2.enumerable });
  return e49;
}, c = (n2, r2, o2) => (o2 = n2 == null ? {} : e(i(n2)), s(r2 || !n2 || !n2.__esModule || !a.call(n2, `default`) ? t(o2, `default`, { value: n2, enumerable: true }) : o2, n2));
function l(e49, t2) {
  this.x = e49, this.y = t2;
}
l.prototype = { clone() {
  return new l(this.x, this.y);
}, add(e49) {
  return this.clone()._add(e49);
}, sub(e49) {
  return this.clone()._sub(e49);
}, multByPoint(e49) {
  return this.clone()._multByPoint(e49);
}, divByPoint(e49) {
  return this.clone()._divByPoint(e49);
}, mult(e49) {
  return this.clone()._mult(e49);
}, div(e49) {
  return this.clone()._div(e49);
}, rotate(e49) {
  return this.clone()._rotate(e49);
}, rotateAround(e49, t2) {
  return this.clone()._rotateAround(e49, t2);
}, matMult(e49) {
  return this.clone()._matMult(e49);
}, unit() {
  return this.clone()._unit();
}, perp() {
  return this.clone()._perp();
}, round() {
  return this.clone()._round();
}, mag() {
  return Math.sqrt(this.x * this.x + this.y * this.y);
}, equals(e49) {
  return this.x === e49.x && this.y === e49.y;
}, dist(e49) {
  return Math.sqrt(this.distSqr(e49));
}, distSqr(e49) {
  let t2 = e49.x - this.x, n2 = e49.y - this.y;
  return t2 * t2 + n2 * n2;
}, angle() {
  return Math.atan2(this.y, this.x);
}, angleTo(e49) {
  return Math.atan2(this.y - e49.y, this.x - e49.x);
}, angleWith(e49) {
  return this.angleWithSep(e49.x, e49.y);
}, angleWithSep(e49, t2) {
  return Math.atan2(this.x * t2 - this.y * e49, this.x * e49 + this.y * t2);
}, _matMult(e49) {
  let t2 = e49[0] * this.x + e49[1] * this.y, n2 = e49[2] * this.x + e49[3] * this.y;
  return this.x = t2, this.y = n2, this;
}, _add(e49) {
  return this.x += e49.x, this.y += e49.y, this;
}, _sub(e49) {
  return this.x -= e49.x, this.y -= e49.y, this;
}, _mult(e49) {
  return this.x *= e49, this.y *= e49, this;
}, _div(e49) {
  return this.x /= e49, this.y /= e49, this;
}, _multByPoint(e49) {
  return this.x *= e49.x, this.y *= e49.y, this;
}, _divByPoint(e49) {
  return this.x /= e49.x, this.y /= e49.y, this;
}, _unit() {
  return this._div(this.mag()), this;
}, _perp() {
  let e49 = this.y;
  return this.y = this.x, this.x = -e49, this;
}, _rotate(e49) {
  let t2 = Math.cos(e49), n2 = Math.sin(e49), r2 = t2 * this.x - n2 * this.y, i2 = n2 * this.x + t2 * this.y;
  return this.x = r2, this.y = i2, this;
}, _rotateAround(e49, t2) {
  let n2 = Math.cos(e49), r2 = Math.sin(e49), i2 = t2.x + n2 * (this.x - t2.x) - r2 * (this.y - t2.y), a2 = t2.y + r2 * (this.x - t2.x) + n2 * (this.y - t2.y);
  return this.x = i2, this.y = a2, this;
}, _round() {
  return this.x = Math.round(this.x), this.y = Math.round(this.y), this;
}, constructor: l }, l.convert = function(e49) {
  if (e49 instanceof l) return e49;
  if (Array.isArray(e49)) return new l(+e49[0], +e49[1]);
  if (e49.x !== void 0 && e49.y !== void 0) return new l(+e49.x, +e49.y);
  throw Error(`Expected [x, y] or {x, y} point format`);
};
function u(e49, t2, n2, r2) {
  let i2 = 3 * e49, a2 = 3 * (n2 - e49) - i2, o2 = 1 - i2 - a2, s2 = 3 * t2, c2 = 3 * (r2 - t2) - s2, l2 = 1 - s2 - c2;
  return function(e50, t3 = 1e-6) {
    if (e50 <= 0) return 0;
    if (e50 >= 1) return 1;
    let n3 = e50;
    for (let r4 = 0; r4 < 8; r4++) {
      let r5 = ((o2 * n3 + a2) * n3 + i2) * n3 - e50;
      if (Math.abs(r5) < t3) return ((l2 * n3 + c2) * n3 + s2) * n3;
      let u3 = (3 * o2 * n3 + 2 * a2) * n3 + i2;
      if (Math.abs(u3) < 1e-6) break;
      n3 -= r5 / u3;
    }
    let r3 = 0, u2 = 1;
    n3 = e50;
    for (let s3 = 0; s3 < 20; s3++) {
      let s4 = ((o2 * n3 + a2) * n3 + i2) * n3;
      if (Math.abs(s4 - e50) < t3) break;
      e50 > s4 ? r3 = n3 : u2 = n3, n3 = (r3 + u2) * 0.5;
    }
    return ((l2 * n3 + c2) * n3 + s2) * n3;
  };
}
let d;
function f() {
  return d ??= typeof OffscreenCanvas < `u` && !!new OffscreenCanvas(1, 1).getContext(`2d`) && typeof createImageBitmap == `function`, d;
}
let p;
function m() {
  if (p == null && (p = false, f())) {
    let e49 = new OffscreenCanvas(5, 5).getContext(`2d`, { willReadFrequently: true });
    if (e49) {
      for (let t3 = 0; t3 < 25; t3++) {
        let n2 = t3 * 4;
        e49.fillStyle = `rgb(${n2},${n2 + 1},${n2 + 2})`, e49.fillRect(t3 % 5, Math.floor(t3 / 5), 1, 1);
      }
      let t2 = e49.getImageData(0, 0, 5, 5).data;
      for (let e50 = 0; e50 < 100; e50++) if (e50 % 4 != 3 && t2[e50] !== e50) {
        p = true;
        break;
      }
    }
  }
  return p || false;
}
var h = typeof Float32Array < `u` ? Float32Array : Array;
function g() {
  var e49 = new h(4);
  return h != Float32Array && (e49[0] = 0, e49[1] = 0, e49[2] = 0, e49[3] = 0), e49;
}
function _(e49, t2, n2) {
  var r2 = t2[0], i2 = t2[1], a2 = t2[2], o2 = t2[3];
  return e49[0] = n2[0] * r2 + n2[4] * i2 + n2[8] * a2 + n2[12] * o2, e49[1] = n2[1] * r2 + n2[5] * i2 + n2[9] * a2 + n2[13] * o2, e49[2] = n2[2] * r2 + n2[6] * i2 + n2[10] * a2 + n2[14] * o2, e49[3] = n2[3] * r2 + n2[7] * i2 + n2[11] * a2 + n2[15] * o2, e49;
}
(function() {
  var e49 = g();
  return function(t2, n2, r2, i2, a2, o2) {
    var s2, c2;
    for (n2 ||= 4, r2 ||= 0, c2 = i2 ? Math.min(i2 * n2 + r2, t2.length) : t2.length, s2 = r2; s2 < c2; s2 += n2) e49[0] = t2[s2], e49[1] = t2[s2 + 1], e49[2] = t2[s2 + 2], e49[3] = t2[s2 + 3], a2(e49, e49, o2), t2[s2] = e49[0], t2[s2 + 1] = e49[1], t2[s2 + 2] = e49[2], t2[s2 + 3] = e49[3];
    return t2;
  };
})();
const v = 8192;
function y(e49) {
  return e49 instanceof Error ? e49 : Error(typeof e49 == `string` ? e49 : String(e49));
}
function b(e49) {
  if (e49 <= 0) return 0;
  if (e49 >= 1) return 1;
  let t2 = e49 * e49, n2 = t2 * e49;
  return 4 * (e49 < 0.5 ? n2 : 3 * (e49 - t2) + n2 - 0.75);
}
function x(e49, t2, n2, r2) {
  return u(e49, t2, n2, r2);
}
x(0.25, 0.1, 0.25, 1);
function S(e49, t2, n2) {
  return Math.min(n2, Math.max(t2, e49));
}
function C(e49, t2, n2) {
  let r2 = n2 - t2, i2 = ((e49 - t2) % r2 + r2) % r2 + t2;
  return i2 === t2 ? n2 : i2;
}
function w(e49, ...t2) {
  for (let n2 of t2) for (let t3 in n2) e49[t3] = n2[t3];
  return e49;
}
function T(e49) {
  return Math.log(e49) / Math.LN2 % 1 == 0;
}
function E(e49, t2, n2) {
  let r2 = {};
  for (let i2 in e49) r2[i2] = t2.call(n2 || this, e49[i2], i2, e49);
  return r2;
}
function D(e49, t2, n2) {
  let r2 = {};
  for (let i2 in e49) t2.call(n2 || this, e49[i2], i2, e49) && (r2[i2] = e49[i2]);
  return r2;
}
function ee(e49, t2) {
  if (Array.isArray(e49)) {
    if (!Array.isArray(t2) || e49.length !== t2.length) return false;
    for (let n2 = 0; n2 < e49.length; n2++) if (!ee(e49[n2], t2[n2])) return false;
    return true;
  }
  if (typeof e49 == `object` && e49 && t2 !== null) {
    if (typeof t2 != `object` || Object.keys(e49).length !== Object.keys(t2).length) return false;
    for (let n2 in e49) if (!ee(e49[n2], t2[n2])) return false;
    return true;
  }
  return e49 === t2;
}
function O(e49) {
  return Array.isArray(e49) ? e49.map(O) : typeof e49 == `object` && e49 ? E(e49, O) : e49;
}
const k = {};
function A(e49) {
  k[e49] || (typeof console < `u` && console.warn(e49), k[e49] = true);
}
function te(e49, t2, n2) {
  return (n2.y - e49.y) * (t2.x - e49.x) > (t2.y - e49.y) * (n2.x - e49.x);
}
function ne(e49) {
  return typeof WorkerGlobalScope < `u` && e49 !== void 0 && e49 instanceof WorkerGlobalScope;
}
function re(e49) {
  return typeof ImageBitmap < `u` && e49 instanceof ImageBitmap;
}
function ie(e49, t2, n2, r2, i2) {
  let a2 = Math.max(-t2, 0) * 4, o2 = (Math.max(0, n2) - n2) * r2 * 4 + a2, s2 = r2 * 4, c2 = Math.max(0, t2), l2 = Math.max(0, n2), u2 = Math.min(e49.width, t2 + r2), d2 = Math.min(e49.height, n2 + i2);
  return { rect: { x: c2, y: l2, width: u2 - c2, height: d2 - l2 }, layout: [{ offset: o2, stride: s2 }] };
}
async function ae(e49, t2, n2, r2, i2) {
  if (typeof VideoFrame > `u`) throw Error(`VideoFrame not supported`);
  let a2 = new VideoFrame(e49, { timestamp: 0 });
  try {
    let o2 = a2 == null ? void 0 : a2.format;
    if (!o2 || !(o2.startsWith(`BGR`) || o2.startsWith(`RGB`))) throw Error(`Unrecognized format ${o2}`);
    let s2 = o2.startsWith(`BGR`), c2 = new Uint8ClampedArray(r2 * i2 * 4);
    if (await a2.copyTo(c2, ie(e49, t2, n2, r2, i2)), s2) for (let e50 = 0; e50 < c2.length; e50 += 4) {
      let t3 = c2[e50];
      c2[e50] = c2[e50 + 2], c2[e50 + 2] = t3;
    }
    return c2;
  } finally {
    a2.close();
  }
}
let oe, se;
function ce(e49, t2, n2, r2, i2) {
  let a2 = e49.width, o2 = e49.height;
  (!oe || !se) && (oe = new OffscreenCanvas(a2, o2), se = oe.getContext(`2d`, { willReadFrequently: true })), oe.width = a2, oe.height = o2, se.drawImage(e49, 0, 0, a2, o2);
  let s2 = se.getImageData(t2, n2, r2, i2);
  return se.clearRect(0, 0, a2, o2), s2.data;
}
async function le(e49, t2, n2, r2, i2) {
  if (m()) try {
    return await ae(e49, t2, n2, r2, i2);
  } catch {
  }
  return ce(e49, t2, n2, r2, i2);
}
function ue(e49, t2, n2, r2) {
  return e49.addEventListener(t2, n2, r2), { unsubscribe: () => {
    e49.removeEventListener(t2, n2, r2);
  } };
}
function de(e49) {
  return e49 * Math.PI / 180;
}
const fe = `AbortError`;
var pe = class extends Error {
  constructor(e49 = fe) {
    super(e49 instanceof Error ? e49.message : e49), this.name = fe, e49 instanceof Error && e49.stack && (this.stack = e49.stack);
  }
};
function me(e49) {
  return e49 instanceof Error && e49.name === `AbortError`;
}
function he(e49) {
  if (e49.aborted) throw new pe(e49.reason);
}
var ge = class e2 {
  constructor(e49, t2, n2) {
    let r2 = this.cells = [];
    if (e49 instanceof ArrayBuffer) {
      this.arrayBuffer = e49;
      let i3 = new Int32Array(this.arrayBuffer);
      e49 = i3[0], t2 = i3[1], n2 = i3[2], this.d = t2 + 2 * n2;
      for (let e50 = 0; e50 < this.d * this.d; e50++) {
        let t3 = i3[3 + e50], n3 = i3[3 + e50 + 1];
        r2.push(t3 === n3 ? null : i3.subarray(t3, n3));
      }
      let a2 = i3[3 + r2.length], o2 = i3[3 + r2.length + 1];
      this.keys = i3.subarray(a2, o2), this.bboxes = i3.subarray(o2), this.insert = this._insertReadonly;
    } else {
      this.d = t2 + 2 * n2;
      for (let e50 = 0; e50 < this.d * this.d; e50++) r2.push([]);
      this.keys = [], this.bboxes = [];
    }
    this.n = t2, this.extent = e49, this.padding = n2, this.scale = t2 / e49, this.uid = 0;
    let i2 = n2 / t2 * e49;
    this.min = -i2, this.max = e49 + i2;
  }
  insert(e49, t2, n2, r2, i2) {
    this._forEachCell(t2, n2, r2, i2, this._insertCell, this.uid++, void 0, void 0), this.keys.push(e49), this.bboxes.push(t2), this.bboxes.push(n2), this.bboxes.push(r2), this.bboxes.push(i2);
  }
  _insertReadonly() {
    throw Error(`Cannot insert into a GridIndex created from an ArrayBuffer.`);
  }
  _insertCell(e49, t2, n2, r2, i2, a2) {
    this.cells[i2].push(a2);
  }
  query(e49, t2, n2, r2, i2) {
    let a2 = this.min, o2 = this.max;
    if (e49 <= a2 && t2 <= a2 && o2 <= n2 && o2 <= r2 && !i2) return [...this.keys];
    {
      let a3 = [];
      return this._forEachCell(e49, t2, n2, r2, this._queryCell, a3, {}, i2), a3;
    }
  }
  _queryCell(e49, t2, n2, r2, i2, a2, o2, s2) {
    let c2 = this.cells[i2];
    if (c2 !== null) {
      let i3 = this.keys, l2 = this.bboxes;
      for (let u2 of c2) if (o2[u2] === void 0) {
        let c3 = u2 * 4;
        (s2 ? s2(l2[c3 + 0], l2[c3 + 1], l2[c3 + 2], l2[c3 + 3]) : e49 <= l2[c3 + 2] && t2 <= l2[c3 + 3] && n2 >= l2[c3 + 0] && r2 >= l2[c3 + 1]) ? (o2[u2] = true, a2.push(i3[u2])) : o2[u2] = false;
      }
    }
  }
  _forEachCell(e49, t2, n2, r2, i2, a2, o2, s2) {
    let c2 = this._convertToCellCoord(e49), l2 = this._convertToCellCoord(t2), u2 = this._convertToCellCoord(n2), d2 = this._convertToCellCoord(r2);
    for (let f2 = c2; f2 <= u2; f2++) for (let c3 = l2; c3 <= d2; c3++) {
      let l3 = this.d * c3 + f2;
      if ((!s2 || s2(this._convertFromCellCoord(f2), this._convertFromCellCoord(c3), this._convertFromCellCoord(f2 + 1), this._convertFromCellCoord(c3 + 1))) && i2.call(this, e49, t2, n2, r2, l3, a2, o2, s2)) return;
    }
  }
  _convertFromCellCoord(e49) {
    return (e49 - this.padding) / this.scale;
  }
  _convertToCellCoord(e49) {
    return Math.max(0, Math.min(this.d - 1, Math.floor(e49 * this.scale) + this.padding));
  }
  toArrayBuffer() {
    if (this.arrayBuffer) return this.arrayBuffer;
    let e49 = this.cells, t2 = 3 + this.cells.length + 1 + 1, n2 = 0;
    for (let e50 of this.cells) n2 += e50.length;
    let r2 = new Int32Array(t2 + n2 + this.keys.length + this.bboxes.length);
    r2[0] = this.extent, r2[1] = this.n, r2[2] = this.padding;
    let i2 = t2;
    for (let t3 = 0; t3 < e49.length; t3++) {
      let n3 = e49[t3];
      r2[3 + t3] = i2, r2.set(n3, i2), i2 += n3.length;
    }
    return r2[3 + e49.length] = i2, r2.set(this.keys, i2), i2 += this.keys.length, r2[3 + e49.length + 1] = i2, r2.set(this.bboxes, i2), i2 += this.bboxes.length, r2.buffer;
  }
  static serialize(e49, t2) {
    let n2 = e49.toArrayBuffer();
    return t2 && t2.push(n2), { buffer: n2 };
  }
  static deserialize(t2) {
    return new e2(t2.buffer);
  }
}, j = { $version: 8, $root: { version: { required: true, type: `enum`, values: [8] }, name: { type: `string` }, metadata: { type: `*` }, center: { type: `array`, value: `number`, length: 2 }, centerAltitude: { type: `number` }, zoom: { type: `number` }, bearing: { type: `number`, default: 0, period: 360, units: `degrees` }, pitch: { type: `number`, default: 0, units: `degrees` }, roll: { type: `number`, default: 0, units: `degrees` }, state: { type: `state`, default: {} }, light: { type: `light` }, sky: { type: `sky` }, projection: { type: `projection` }, terrain: { type: `terrain` }, sources: { required: true, type: `sources` }, sprite: { type: `sprite` }, glyphs: { type: `string` }, "font-faces": { type: `fontFaces` }, transition: { type: `transition` }, layers: { required: true, type: `array`, value: `layer` } }, sources: { "*": { type: `source` } }, source: [`source_vector`, `source_raster`, `source_raster_dem`, `source_geojson`, `source_video`, `source_image`], source_vector: { type: { required: true, type: `enum`, values: { vector: {} } }, url: { type: `string` }, tiles: { type: `array`, value: `string` }, bounds: { type: `array`, value: `number`, length: 4, default: [-180, -85.051129, 180, 85.051129] }, scheme: { type: `enum`, values: { xyz: {}, tms: {} }, default: `xyz` }, minzoom: { type: `number`, default: 0 }, maxzoom: { type: `number`, default: 22 }, attribution: { type: `string` }, promoteId: { type: `promoteId` }, volatile: { type: `boolean`, default: false }, encoding: { type: `enum`, values: { mvt: {}, mlt: {} }, default: `mvt` }, "*": { type: `*` } }, source_raster: { type: { required: true, type: `enum`, values: { raster: {} } }, url: { type: `string` }, tiles: { type: `array`, value: `string` }, bounds: { type: `array`, value: `number`, length: 4, default: [-180, -85.051129, 180, 85.051129] }, minzoom: { type: `number`, default: 0 }, maxzoom: { type: `number`, default: 22 }, tileSize: { type: `number`, default: 512, units: `pixels` }, scheme: { type: `enum`, values: { xyz: {}, tms: {} }, default: `xyz` }, attribution: { type: `string` }, volatile: { type: `boolean`, default: false }, "*": { type: `*` } }, source_raster_dem: { type: { required: true, type: `enum`, values: { "raster-dem": {} } }, url: { type: `string` }, tiles: { type: `array`, value: `string` }, bounds: { type: `array`, value: `number`, length: 4, default: [-180, -85.051129, 180, 85.051129] }, minzoom: { type: `number`, default: 0 }, maxzoom: { type: `number`, default: 22 }, tileSize: { type: `number`, default: 512, units: `pixels` }, attribution: { type: `string` }, encoding: { type: `enum`, values: { terrarium: {}, mapbox: {}, custom: {} }, default: `mapbox` }, redFactor: { type: `number`, default: 1 }, blueFactor: { type: `number`, default: 1 }, greenFactor: { type: `number`, default: 1 }, baseShift: { type: `number`, default: 0 }, volatile: { type: `boolean`, default: false }, "*": { type: `*` } }, source_geojson: { type: { required: true, type: `enum`, values: { geojson: {} } }, data: { required: true, type: `*` }, maxzoom: { type: `number`, default: 18 }, attribution: { type: `string` }, buffer: { type: `number`, default: 128, maximum: 512, minimum: 0 }, filter: { type: `filter` }, tolerance: { type: `number`, default: 0.375 }, cluster: { type: `boolean`, default: false }, clusterRadius: { type: `number`, default: 50, minimum: 0 }, clusterMaxZoom: { type: `number` }, clusterMinPoints: { type: `number` }, clusterProperties: { type: `*` }, lineMetrics: { type: `boolean`, default: false }, generateId: { type: `boolean`, default: false }, promoteId: { type: `promoteId` } }, source_video: { type: { required: true, type: `enum`, values: { video: {} } }, urls: { required: true, type: `array`, value: `string` }, coordinates: { required: true, type: `array`, length: 4, value: { type: `array`, length: 2, value: `number` } } }, source_image: { type: { required: true, type: `enum`, values: { image: {} } }, url: { type: `string` }, coordinates: { required: true, type: `array`, length: 4, value: { type: `array`, length: 2, value: `number` } } }, layer: { id: { type: `string`, required: true }, type: { type: `enum`, values: { fill: {}, line: {}, symbol: {}, circle: {}, heatmap: {}, "fill-extrusion": {}, raster: {}, hillshade: {}, "color-relief": {}, background: {} }, required: true }, metadata: { type: `*` }, source: { type: `string` }, "source-layer": { type: `string` }, minzoom: { type: `number`, minimum: 0, maximum: 24 }, maxzoom: { type: `number`, minimum: 0, maximum: 24 }, filter: { type: `filter` }, layout: { type: `layout` }, paint: { type: `paint` } }, layout: [`layout_fill`, `layout_line`, `layout_circle`, `layout_heatmap`, `layout_fill-extrusion`, `layout_symbol`, `layout_raster`, `layout_hillshade`, `layout_color-relief`, `layout_background`], layout_background: { visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, layout_fill: { "fill-sort-key": { type: `number`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, layout_circle: { "circle-sort-key": { type: `number`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, layout_heatmap: { visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, "layout_fill-extrusion": { visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` }, "fill-extrusion-rounded-corner-distance": { type: `number`, default: 0, minimum: 0, units: `meters`, "property-type": `constant` } }, layout_line: { "line-cap": { type: `enum`, values: { butt: {}, round: {}, square: {} }, default: `butt`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "line-join": { type: `enum`, values: { bevel: {}, round: {}, miter: {} }, default: `miter`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "line-miter-limit": { type: `number`, default: 2, requires: [{ "line-join": `miter` }], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "line-round-limit": { type: `number`, default: 1.05, requires: [{ "line-join": `round` }], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "line-sort-key": { type: `number`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, layout_symbol: { "symbol-placement": { type: `enum`, values: { point: {}, line: {}, "line-center": {} }, default: `point`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "symbol-spacing": { type: `number`, default: 250, minimum: 1, units: `pixels`, requires: [{ "symbol-placement": `line` }], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "symbol-avoid-edges": { type: `boolean`, default: false, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "symbol-sort-key": { type: `number`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "symbol-z-order": { type: `enum`, values: { auto: {}, "viewport-y": {}, source: {} }, default: `auto`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-allow-overlap": { type: `boolean`, default: false, requires: [`icon-image`, { "!": `icon-overlap` }], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-overlap": { type: `enum`, values: { never: {}, always: {}, cooperative: {} }, requires: [`icon-image`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-ignore-placement": { type: `boolean`, default: false, requires: [`icon-image`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-optional": { type: `boolean`, default: false, requires: [`icon-image`, `text-field`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-rotation-alignment": { type: `enum`, values: { map: {}, viewport: {}, auto: {} }, default: `auto`, requires: [`icon-image`], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-size": { type: `number`, default: 1, minimum: 0, units: `factor of the original icon size`, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-text-fit": { type: `enum`, values: { none: {}, width: {}, height: {}, both: {} }, default: `none`, requires: [`icon-image`, `text-field`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-text-fit-padding": { type: `array`, value: `number`, length: 4, default: [0, 0, 0, 0], units: `pixels`, requires: [`icon-image`, `text-field`, { "icon-text-fit": [`both`, `width`, `height`] }], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-image": { type: `resolvedImage`, tokens: true, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-rotate": { type: `number`, default: 0, period: 360, units: `degrees`, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-padding": { type: `padding`, default: [2], units: `pixels`, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-keep-upright": { type: `boolean`, default: false, requires: [`icon-image`, { "icon-rotation-alignment": `map` }, { "symbol-placement": [`line`, `line-center`] }], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-offset": { type: `array`, value: `number`, length: 2, default: [0, 0], requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-anchor": { type: `enum`, values: { center: {}, left: {}, right: {}, top: {}, bottom: {}, "top-left": {}, "top-right": {}, "bottom-left": {}, "bottom-right": {} }, default: `center`, requires: [`icon-image`], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "icon-pitch-alignment": { type: `enum`, values: { map: {}, viewport: {}, auto: {} }, default: `auto`, requires: [`icon-image`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-pitch-alignment": { type: `enum`, values: { map: {}, viewport: {}, auto: {} }, default: `auto`, requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-rotation-alignment": { type: `enum`, values: { map: {}, viewport: {}, "viewport-glyph": {}, auto: {} }, default: `auto`, requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-field": { type: `formatted`, default: ``, tokens: true, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-font": { type: `array`, value: `string`, default: [`Open Sans Regular`, `Arial Unicode MS Regular`], requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-size": { type: `number`, default: 16, minimum: 0, units: `pixels`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-max-width": { type: `number`, default: 10, minimum: 0, units: `ems`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-line-height": { type: `number`, default: 1.2, units: `ems`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-letter-spacing": { type: `number`, default: 0, units: `ems`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-justify": { type: `enum`, values: { auto: {}, left: {}, center: {}, right: {} }, default: `center`, requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-radial-offset": { type: `number`, units: `ems`, default: 0, requires: [`text-field`], "property-type": `data-driven`, expression: { interpolated: true, parameters: [`zoom`, `feature`] } }, "text-variable-anchor": { type: `array`, value: `enum`, values: { center: {}, left: {}, right: {}, top: {}, bottom: {}, "top-left": {}, "top-right": {}, "bottom-left": {}, "bottom-right": {} }, requires: [`text-field`, { "symbol-placement": [`point`] }], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-variable-anchor-offset": { type: `variableAnchorOffsetCollection`, requires: [`text-field`, { "symbol-placement": [`point`] }], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-anchor": { type: `enum`, values: { center: {}, left: {}, right: {}, top: {}, bottom: {}, "top-left": {}, "top-right": {}, "bottom-left": {}, "bottom-right": {} }, default: `center`, requires: [`text-field`, { "!": `text-variable-anchor` }], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-max-angle": { type: `number`, default: 45, units: `degrees`, requires: [`text-field`, { "symbol-placement": [`line`, `line-center`] }], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-writing-mode": { type: `array`, value: `enum`, values: { horizontal: {}, vertical: {} }, requires: [`text-field`, { "symbol-placement": [`point`] }], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-rotate": { type: `number`, default: 0, period: 360, units: `degrees`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-padding": { type: `number`, default: 2, minimum: 0, units: `pixels`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-keep-upright": { type: `boolean`, default: true, requires: [`text-field`, { "text-rotation-alignment": `map` }, { "symbol-placement": [`line`, `line-center`] }], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-transform": { type: `enum`, values: { none: {}, uppercase: {}, lowercase: {} }, default: `none`, requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-offset": { type: `array`, value: `number`, units: `ems`, length: 2, default: [0, 0], requires: [`text-field`, { "!": `text-radial-offset` }], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "text-allow-overlap": { type: `boolean`, default: false, requires: [`text-field`, { "!": `text-overlap` }], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-overlap": { type: `enum`, values: { never: {}, always: {}, cooperative: {} }, requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-ignore-placement": { type: `boolean`, default: false, requires: [`text-field`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-optional": { type: `boolean`, default: false, requires: [`text-field`, `icon-image`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "symbol-height-offset": { type: `number`, default: 0, units: `meters`, requires: [{ "symbol-placement": [`point`] }], expression: { interpolated: true, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, "symbol-height-anchor": { type: `enum`, values: { ground: {}, absolute: {} }, default: `ground`, requires: [`symbol-height-offset`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, layout_raster: { visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, layout_hillshade: { visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, "layout_color-relief": { visibility: { type: `enum`, values: { visible: {}, none: {} }, default: `visible`, expression: { interpolated: false, parameters: [`global-state`] }, "property-type": `data-constant` } }, filter: { type: `boolean`, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `data-driven` }, filter_operator: { type: `enum`, values: { "==": {}, "!=": {}, ">": {}, ">=": {}, "<": {}, "<=": {}, in: {}, "!in": {}, all: {}, any: {}, none: {}, has: {}, "!has": {} } }, geometry_type: { type: `enum`, values: { Point: {}, LineString: {}, Polygon: {} } }, function: { expression: { type: `expression` }, stops: { type: `array`, value: `function_stop` }, base: { type: `number`, default: 1, minimum: 0 }, property: { type: `string`, default: `$zoom` }, type: { type: `enum`, values: { identity: {}, exponential: {}, interval: {}, categorical: {} }, default: `exponential` }, colorSpace: { type: `enum`, values: { rgb: {}, lab: {}, hcl: {} }, default: `rgb` }, default: { type: `*`, required: false } }, function_stop: { type: `array`, minimum: 0, maximum: 24, value: [`number`, `color`], length: 2 }, expression: { type: `array`, value: `expression_name`, minimum: 1 }, light: { anchor: { type: `enum`, default: `viewport`, values: { map: {}, viewport: {} }, "property-type": `data-constant`, transition: false, expression: { interpolated: false, parameters: [`zoom`] } }, position: { type: `array`, default: [1.15, 210, 30], length: 3, value: `number`, "property-type": `data-constant`, transition: true, expression: { interpolated: true, parameters: [`zoom`] } }, color: { type: `color`, "property-type": `data-constant`, default: `#ffffff`, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, intensity: { type: `number`, "property-type": `data-constant`, default: 0.5, minimum: 0, maximum: 1, expression: { interpolated: true, parameters: [`zoom`] }, transition: true } }, sky: { "sky-color": { type: `color`, "property-type": `data-constant`, default: `#88C6FC`, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, "horizon-color": { type: `color`, "property-type": `data-constant`, default: `#ffffff`, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, "fog-color": { type: `color`, "property-type": `data-constant`, default: `#ffffff`, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, "fog-ground-blend": { type: `number`, "property-type": `data-constant`, default: 0.5, minimum: 0, maximum: 1, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, "horizon-fog-blend": { type: `number`, "property-type": `data-constant`, default: 0.8, minimum: 0, maximum: 1, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, "sky-horizon-blend": { type: `number`, "property-type": `data-constant`, default: 0.8, minimum: 0, maximum: 1, expression: { interpolated: true, parameters: [`zoom`] }, transition: true }, "atmosphere-blend": { type: `number`, "property-type": `data-constant`, default: 0.8, minimum: 0, maximum: 1, expression: { interpolated: true, parameters: [`zoom`] }, transition: true } }, terrain: { source: { type: `string`, required: true }, exaggeration: { type: `number`, minimum: 0, default: 1 } }, projection: { type: { type: `projectionDefinition`, default: `mercator`, "property-type": `data-constant`, transition: false, expression: { interpolated: true, parameters: [`zoom`] } } }, paint: [`paint_fill`, `paint_line`, `paint_circle`, `paint_heatmap`, `paint_fill-extrusion`, `paint_symbol`, `paint_raster`, `paint_hillshade`, `paint_color-relief`, `paint_background`], paint_fill: { "fill-antialias": { type: `boolean`, default: true, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "fill-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "fill-layer-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`, `global-state`] }, "property-type": `data-constant` }, "fill-color": { type: `color`, default: `#000000`, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "fill-outline-color": { type: `color`, transition: true, requires: [{ "!": `fill-pattern` }, { "fill-antialias": true }], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "fill-translate": { type: `array`, value: `number`, length: 2, default: [0, 0], transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "fill-translate-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, requires: [`fill-translate`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "fill-pattern": { type: `resolvedImage`, transition: true, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `cross-faded-data-driven` } }, "paint_fill-extrusion": { "fill-extrusion-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "fill-extrusion-color": { type: `color`, default: `#000000`, transition: true, requires: [{ "!": `fill-extrusion-pattern` }], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "fill-extrusion-translate": { type: `array`, value: `number`, length: 2, default: [0, 0], transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "fill-extrusion-translate-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, requires: [`fill-extrusion-translate`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "fill-extrusion-pattern": { type: `resolvedImage`, transition: true, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `cross-faded-data-driven` }, "fill-extrusion-height": { type: `number`, default: 0, units: `meters`, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "fill-extrusion-base": { type: `number`, default: 0, units: `meters`, transition: true, requires: [`fill-extrusion-height`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "fill-extrusion-vertical-gradient": { type: `boolean`, default: true, transition: false, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` } }, paint_line: { "line-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "line-layer-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`, `global-state`] }, "property-type": `data-constant` }, "line-color": { type: `color`, default: `#000000`, transition: true, requires: [{ "!": `line-pattern` }], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "line-translate": { type: `array`, value: `number`, length: 2, default: [0, 0], transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "line-translate-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, requires: [`line-translate`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "line-width": { type: `number`, default: 1, minimum: 0, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "line-gap-width": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "line-offset": { type: `number`, default: 0, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "line-blur": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "line-dasharray": { type: `array`, value: `number`, minimum: 0, transition: true, units: `line widths`, requires: [{ "!": `line-pattern` }], expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `cross-faded-data-driven` }, "line-pattern": { type: `resolvedImage`, transition: true, expression: { interpolated: false, parameters: [`zoom`, `feature`] }, "property-type": `cross-faded-data-driven` }, "line-gradient": { type: `color`, transition: false, requires: [{ "!": `line-dasharray` }, { "!": `line-pattern` }, { source: `geojson`, has: { lineMetrics: true } }], expression: { interpolated: true, parameters: [`line-progress`] }, "property-type": `color-ramp` } }, paint_circle: { "circle-radius": { type: `number`, default: 5, minimum: 0, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "circle-color": { type: `color`, default: `#000000`, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "circle-blur": { type: `number`, default: 0, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "circle-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "circle-translate": { type: `array`, value: `number`, length: 2, default: [0, 0], transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "circle-translate-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, requires: [`circle-translate`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "circle-pitch-scale": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "circle-pitch-alignment": { type: `enum`, values: { map: {}, viewport: {} }, default: `viewport`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "circle-stroke-width": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "circle-stroke-color": { type: `color`, default: `#000000`, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "circle-stroke-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` } }, paint_heatmap: { "heatmap-radius": { type: `number`, default: 30, minimum: 1, transition: true, units: `pixels`, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "heatmap-weight": { type: `number`, default: 1, minimum: 0, transition: false, expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "heatmap-intensity": { type: `number`, default: 1, minimum: 0, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "heatmap-color": { type: `color`, default: [`interpolate`, [`linear`], [`heatmap-density`], 0, `rgba(0, 0, 255, 0)`, 0.1, `royalblue`, 0.3, `cyan`, 0.5, `lime`, 0.7, `yellow`, 1, `red`], transition: false, expression: { interpolated: true, parameters: [`heatmap-density`] }, "property-type": `color-ramp` }, "heatmap-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` } }, paint_symbol: { "icon-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "icon-color": { type: `color`, default: `#000000`, transition: true, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "icon-halo-color": { type: `color`, default: `rgba(0, 0, 0, 0)`, transition: true, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "icon-halo-width": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "icon-halo-blur": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "icon-translate": { type: `array`, value: `number`, length: 2, default: [0, 0], transition: true, units: `pixels`, requires: [`icon-image`], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "icon-translate-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, requires: [`icon-image`, `icon-translate`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "text-color": { type: `color`, default: `#000000`, transition: true, overridable: true, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "text-halo-color": { type: `color`, default: `rgba(0, 0, 0, 0)`, transition: true, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "text-halo-width": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "text-halo-blur": { type: `number`, default: 0, minimum: 0, transition: true, units: `pixels`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`, `feature`, `feature-state`] }, "property-type": `data-driven` }, "text-translate": { type: `array`, value: `number`, length: 2, default: [0, 0], transition: true, units: `pixels`, requires: [`text-field`], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "text-translate-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `map`, requires: [`text-field`, `text-translate`], expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` } }, paint_raster: { "raster-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-hue-rotate": { type: `number`, default: 0, period: 360, transition: true, units: `degrees`, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-brightness-min": { type: `number`, default: 0, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-brightness-max": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-saturation": { type: `number`, default: 0, minimum: -1, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-contrast": { type: `number`, default: 0, minimum: -1, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, resampling: { type: `enum`, values: { linear: {}, nearest: {} }, default: `linear`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-resampling": { type: `enum`, values: { linear: {}, nearest: {} }, default: `linear`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "raster-fade-duration": { type: `number`, default: 300, minimum: 0, transition: false, units: `milliseconds`, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` } }, paint_hillshade: { "hillshade-illumination-direction": { type: `numberArray`, default: 335, minimum: 0, maximum: 359, transition: false, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-illumination-altitude": { type: `numberArray`, default: 45, minimum: 0, maximum: 90, transition: false, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-illumination-anchor": { type: `enum`, values: { map: {}, viewport: {} }, default: `viewport`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-exaggeration": { type: `number`, default: 0.5, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-shadow-color": { type: `colorArray`, default: `#000000`, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-highlight-color": { type: `colorArray`, default: `#FFFFFF`, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-accent-color": { type: `color`, default: `#000000`, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "hillshade-method": { type: `enum`, values: { standard: {}, basic: {}, combined: {}, igor: {}, multidirectional: {} }, default: `standard`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` }, resampling: { type: `enum`, values: { linear: {}, nearest: {} }, default: `linear`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` } }, "paint_color-relief": { "color-relief-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "color-relief-color": { type: `color`, transition: false, expression: { interpolated: true, parameters: [`elevation`] }, "property-type": `color-ramp` }, resampling: { type: `enum`, values: { linear: {}, nearest: {} }, default: `linear`, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `data-constant` } }, paint_background: { "background-color": { type: `color`, default: `#000000`, transition: true, requires: [{ "!": `background-pattern` }], expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` }, "background-pattern": { type: `resolvedImage`, transition: true, expression: { interpolated: false, parameters: [`zoom`] }, "property-type": `cross-faded` }, "background-opacity": { type: `number`, default: 1, minimum: 0, maximum: 1, transition: true, expression: { interpolated: true, parameters: [`zoom`] }, "property-type": `data-constant` } }, transition: { duration: { type: `number`, default: 300, minimum: 0, units: `milliseconds` }, delay: { type: `number`, default: 0, minimum: 0, units: `milliseconds` } }, "property-type": { "data-driven": { type: `property-type` }, "cross-faded": { type: `property-type` }, "cross-faded-data-driven": { type: `property-type` }, "color-ramp": { type: `property-type` }, "data-constant": { type: `property-type` }, constant: { type: `property-type` } }, promoteId: { "*": { type: `string` } }, interpolation: { type: `array`, value: `interpolation_name`, minimum: 1 }, interpolation_name: { type: `enum`, values: { linear: { syntax: { overloads: [{ parameters: [], "output-type": `interpolation` }], parameters: [] } }, exponential: { syntax: { overloads: [{ parameters: [`base`], "output-type": `interpolation` }], parameters: [{ name: `base`, type: `number literal` }] } }, "cubic-bezier": { syntax: { overloads: [{ parameters: [`x1`, `y1`, `x2`, `y2`], "output-type": `interpolation` }], parameters: [{ name: `x1`, type: `number literal` }, { name: `y1`, type: `number literal` }, { name: `x2`, type: `number literal` }, { name: `y2`, type: `number literal` }] } } } } };
const M = j, _e = [`type`, `source`, `source-layer`, `minzoom`, `maxzoom`, `filter`, `layout`];
var N = class {
  constructor(e49, t2, n2, r2, i2 = `error`) {
    this.message = (e49 ? `${e49}: ` : ``) + n2, r2 && (this.identifier = r2), this.severity = i2, t2 != null && t2.__line__ && (this.line = t2.__line__);
  }
};
const P = { kind: `null` }, F = { kind: `number` }, I = { kind: `string` }, L = { kind: `boolean` }, ve = { kind: `color` }, ye = { kind: `projectionDefinition` }, be = { kind: `object` }, R = { kind: `value` }, xe = { kind: `error` }, Se = { kind: `collator` }, Ce = { kind: `formatted` }, we = { kind: `padding` }, Te = { kind: `colorArray` }, Ee = { kind: `numberArray` }, De = { kind: `resolvedImage` }, Oe = { kind: `variableAnchorOffsetCollection` };
function ke(e49, t2) {
  return { kind: `array`, itemType: e49, N: t2 };
}
function z(e49) {
  if (e49.kind === `array`) {
    let t2 = z(e49.itemType);
    return typeof e49.N == `number` ? `array<${t2}, ${e49.N}>` : e49.itemType.kind === `value` ? `array` : `array<${t2}>`;
  }
  return e49.kind;
}
const Ae = [P, F, I, L, ve, ye, Ce, be, ke(R), we, Ee, Te, De, Oe];
function je(e49, t2) {
  if (t2.kind === `error`) return null;
  if (e49.kind === `array`) {
    if (t2.kind === `array` && (t2.N === 0 && t2.itemType.kind === `value` || !je(e49.itemType, t2.itemType)) && (typeof e49.N != `number` || e49.N === t2.N)) return null;
  } else if (e49.kind === t2.kind) return null;
  else if (e49.kind === `value`) {
    for (let e50 of Ae) if (!je(e50, t2)) return null;
  }
  return `Expected ${z(e49)} but found ${z(t2)} instead.`;
}
function Me(e49, t2) {
  return t2.some((t3) => t3.kind === e49.kind);
}
function Ne(e49, t2) {
  return t2.some((t3) => t3 === `null` ? e49 === null : t3 === `array` ? Array.isArray(e49) : t3 === `object` ? e49 && !Array.isArray(e49) && typeof e49 == `object` : t3 === typeof e49);
}
function Pe(e49, t2) {
  return e49.kind === `array` && t2.kind === `array` ? e49.itemType.kind === t2.itemType.kind && typeof e49.N == `number` : e49.kind === t2.kind;
}
const Fe = 0.96422, Ie = 0.82521, Le = 4 / 29, Re = 6 / 29, ze = 3 * Re * Re, Be = Math.PI / 180, Ve = 180 / Math.PI;
function He(e49) {
  return e49 %= 360, e49 < 0 && (e49 += 360), e49;
}
function Ue([e49, t2, n2, r2]) {
  e49 = B(e49), t2 = B(t2), n2 = B(n2);
  let i2, a2, o2 = We((0.2225045 * e49 + 0.7168786 * t2 + 0.0606169 * n2) / 1);
  e49 === t2 && t2 === n2 ? i2 = a2 = o2 : (i2 = We((0.4360747 * e49 + 0.3850649 * t2 + 0.1430804 * n2) / Fe), a2 = We((0.0139322 * e49 + 0.0971045 * t2 + 0.7141733 * n2) / Ie));
  let s2 = 116 * o2 - 16;
  return [s2 < 0 ? 0 : s2, 500 * (i2 - o2), 200 * (o2 - a2), r2];
}
function B(e49) {
  return e49 <= 0.04045 ? e49 / 12.92 : ((e49 + 0.055) / 1.055) ** 2.4;
}
function We(e49) {
  return e49 > 0.008856451679035631 ? e49 ** (1 / 3) : e49 / ze + Le;
}
function Ge([e49, t2, n2, r2]) {
  let i2 = (e49 + 16) / 116, a2 = isNaN(t2) ? i2 : i2 + t2 / 500, o2 = isNaN(n2) ? i2 : i2 - n2 / 200;
  return i2 = 1 * qe(i2), a2 = Fe * qe(a2), o2 = Ie * qe(o2), [Ke(3.1338561 * a2 - 1.6168667 * i2 - 0.4906146 * o2), Ke(-0.9787684 * a2 + 1.9161415 * i2 + 0.033454 * o2), Ke(0.0719453 * a2 - 0.2289914 * i2 + 1.4052427 * o2), r2];
}
function Ke(e49) {
  return e49 = e49 <= 304e-5 ? 12.92 * e49 : 1.055 * e49 ** (1 / 2.4) - 0.055, e49 < 0 ? 0 : e49 > 1 ? 1 : e49;
}
function qe(e49) {
  return e49 > Re ? e49 * e49 * e49 : ze * (e49 - Le);
}
function Je(e49) {
  let [t2, n2, r2, i2] = Ue(e49), a2 = Math.sqrt(n2 * n2 + r2 * r2);
  return [Math.round(a2 * 1e4) ? He(Math.atan2(r2, n2) * Ve) : NaN, a2, t2, i2];
}
function Ye([e49, t2, n2, r2]) {
  return e49 = isNaN(e49) ? 0 : e49 * Be, Ge([n2, Math.cos(e49) * t2, Math.sin(e49) * t2, r2]);
}
function Xe([e49, t2, n2, r2]) {
  e49 = He(e49), t2 /= 100, n2 /= 100;
  function i2(r3) {
    let i3 = (r3 + e49 / 30) % 12, a2 = t2 * Math.min(n2, 1 - n2);
    return n2 - a2 * Math.max(-1, Math.min(i3 - 3, 9 - i3, 1));
  }
  return [i2(0), i2(8), i2(4), r2];
}
const Ze = Object.hasOwn || function(e49, t2) {
  return Object.prototype.hasOwnProperty.call(e49, t2);
};
function Qe(e49, t2) {
  return Ze(e49, t2) ? e49[t2] : void 0;
}
function $e(e49) {
  if (e49 = e49.toLowerCase().trim(), e49 === `transparent`) return [0, 0, 0, 0];
  let t2 = Qe(it, e49);
  if (t2) {
    let [e50, n3, r2] = t2;
    return [e50 / 255, n3 / 255, r2 / 255, 1];
  }
  if (e49.startsWith(`#`) && /^#(?:[0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/.test(e49)) {
    let t3 = e49.length < 6 ? 1 : 2, n3 = 1;
    return [et(e49.slice(n3, n3 += t3)), et(e49.slice(n3, n3 += t3)), et(e49.slice(n3, n3 += t3)), et(e49.slice(n3, n3 + t3) || `ff`)];
  }
  if (e49.startsWith(`rgb`)) {
    let t3 = e49.match(/^rgba?\(\s*([\de.+-]+)(%)?(?:\s+|\s*(,)\s*)([\de.+-]+)(%)?(?:\s+|\s*(,)\s*)([\de.+-]+)(%)?(?:\s*([,\/])\s*([\de.+-]+)(%)?)?\s*\)$/);
    if (t3) {
      let [e50, n3, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2] = t3, p2 = [i2 || ` `, s2 || ` `, u2].join(``);
      if (p2 === `  ` || p2 === `  /` || p2 === `,,` || p2 === `,,,`) {
        let e51 = [r2, o2, l2].join(``), t4 = e51 === `%%%` ? 100 : e51 === `` ? 255 : 0;
        if (t4) {
          let e52 = [nt(+n3 / t4, 0, 1), nt(+a2 / t4, 0, 1), nt(+c2 / t4, 0, 1), d2 ? tt(+d2, f2) : 1];
          if (rt(e52)) return e52;
        }
      }
      return;
    }
  }
  let n2 = e49.match(/^hsla?\(\s*([\de.+-]+)(?:deg)?(?:\s+|\s*(,)\s*)([\de.+-]+)%(?:\s+|\s*(,)\s*)([\de.+-]+)%(?:\s*([,\/])\s*([\de.+-]+)(%)?)?\s*\)$/);
  if (n2) {
    let [e50, t3, r2, i2, a2, o2, s2, c2, l2] = n2, u2 = [r2 || ` `, a2 || ` `, s2].join(``);
    if (u2 === `  ` || u2 === `  /` || u2 === `,,` || u2 === `,,,`) {
      let e51 = [+t3, nt(+i2, 0, 100), nt(+o2, 0, 100), c2 ? tt(+c2, l2) : 1];
      if (rt(e51)) return Xe(e51);
    }
  }
}
function et(e49) {
  return parseInt(e49.padEnd(2, e49), 16) / 255;
}
function tt(e49, t2) {
  return nt(t2 ? e49 / 100 : e49, 0, 1);
}
function nt(e49, t2, n2) {
  return Math.min(Math.max(t2, e49), n2);
}
function rt(e49) {
  return !e49.some(Number.isNaN);
}
const it = { aliceblue: [240, 248, 255], antiquewhite: [250, 235, 215], aqua: [0, 255, 255], aquamarine: [127, 255, 212], azure: [240, 255, 255], beige: [245, 245, 220], bisque: [255, 228, 196], black: [0, 0, 0], blanchedalmond: [255, 235, 205], blue: [0, 0, 255], blueviolet: [138, 43, 226], brown: [165, 42, 42], burlywood: [222, 184, 135], cadetblue: [95, 158, 160], chartreuse: [127, 255, 0], chocolate: [210, 105, 30], coral: [255, 127, 80], cornflowerblue: [100, 149, 237], cornsilk: [255, 248, 220], crimson: [220, 20, 60], cyan: [0, 255, 255], darkblue: [0, 0, 139], darkcyan: [0, 139, 139], darkgoldenrod: [184, 134, 11], darkgray: [169, 169, 169], darkgreen: [0, 100, 0], darkgrey: [169, 169, 169], darkkhaki: [189, 183, 107], darkmagenta: [139, 0, 139], darkolivegreen: [85, 107, 47], darkorange: [255, 140, 0], darkorchid: [153, 50, 204], darkred: [139, 0, 0], darksalmon: [233, 150, 122], darkseagreen: [143, 188, 143], darkslateblue: [72, 61, 139], darkslategray: [47, 79, 79], darkslategrey: [47, 79, 79], darkturquoise: [0, 206, 209], darkviolet: [148, 0, 211], deeppink: [255, 20, 147], deepskyblue: [0, 191, 255], dimgray: [105, 105, 105], dimgrey: [105, 105, 105], dodgerblue: [30, 144, 255], firebrick: [178, 34, 34], floralwhite: [255, 250, 240], forestgreen: [34, 139, 34], fuchsia: [255, 0, 255], gainsboro: [220, 220, 220], ghostwhite: [248, 248, 255], gold: [255, 215, 0], goldenrod: [218, 165, 32], gray: [128, 128, 128], green: [0, 128, 0], greenyellow: [173, 255, 47], grey: [128, 128, 128], honeydew: [240, 255, 240], hotpink: [255, 105, 180], indianred: [205, 92, 92], indigo: [75, 0, 130], ivory: [255, 255, 240], khaki: [240, 230, 140], lavender: [230, 230, 250], lavenderblush: [255, 240, 245], lawngreen: [124, 252, 0], lemonchiffon: [255, 250, 205], lightblue: [173, 216, 230], lightcoral: [240, 128, 128], lightcyan: [224, 255, 255], lightgoldenrodyellow: [250, 250, 210], lightgray: [211, 211, 211], lightgreen: [144, 238, 144], lightgrey: [211, 211, 211], lightpink: [255, 182, 193], lightsalmon: [255, 160, 122], lightseagreen: [32, 178, 170], lightskyblue: [135, 206, 250], lightslategray: [119, 136, 153], lightslategrey: [119, 136, 153], lightsteelblue: [176, 196, 222], lightyellow: [255, 255, 224], lime: [0, 255, 0], limegreen: [50, 205, 50], linen: [250, 240, 230], magenta: [255, 0, 255], maroon: [128, 0, 0], mediumaquamarine: [102, 205, 170], mediumblue: [0, 0, 205], mediumorchid: [186, 85, 211], mediumpurple: [147, 112, 219], mediumseagreen: [60, 179, 113], mediumslateblue: [123, 104, 238], mediumspringgreen: [0, 250, 154], mediumturquoise: [72, 209, 204], mediumvioletred: [199, 21, 133], midnightblue: [25, 25, 112], mintcream: [245, 255, 250], mistyrose: [255, 228, 225], moccasin: [255, 228, 181], navajowhite: [255, 222, 173], navy: [0, 0, 128], oldlace: [253, 245, 230], olive: [128, 128, 0], olivedrab: [107, 142, 35], orange: [255, 165, 0], orangered: [255, 69, 0], orchid: [218, 112, 214], palegoldenrod: [238, 232, 170], palegreen: [152, 251, 152], paleturquoise: [175, 238, 238], palevioletred: [219, 112, 147], papayawhip: [255, 239, 213], peachpuff: [255, 218, 185], peru: [205, 133, 63], pink: [255, 192, 203], plum: [221, 160, 221], powderblue: [176, 224, 230], purple: [128, 0, 128], rebeccapurple: [102, 51, 153], red: [255, 0, 0], rosybrown: [188, 143, 143], royalblue: [65, 105, 225], saddlebrown: [139, 69, 19], salmon: [250, 128, 114], sandybrown: [244, 164, 96], seagreen: [46, 139, 87], seashell: [255, 245, 238], sienna: [160, 82, 45], silver: [192, 192, 192], skyblue: [135, 206, 235], slateblue: [106, 90, 205], slategray: [112, 128, 144], slategrey: [112, 128, 144], snow: [255, 250, 250], springgreen: [0, 255, 127], steelblue: [70, 130, 180], tan: [210, 180, 140], teal: [0, 128, 128], thistle: [216, 191, 216], tomato: [255, 99, 71], turquoise: [64, 224, 208], violet: [238, 130, 238], wheat: [245, 222, 179], white: [255, 255, 255], whitesmoke: [245, 245, 245], yellow: [255, 255, 0], yellowgreen: [154, 205, 50] };
function at(e49, t2, n2) {
  return e49 + n2 * (t2 - e49);
}
function ot(e49, t2, n2) {
  return e49.map((e50, r2) => at(e50, t2[r2], n2));
}
function st(e49) {
  return e49 === `rgb` || e49 === `hcl` || e49 === `lab`;
}
var V = (_a = class {
  constructor(e49, t2, n2, r2 = 1, i2 = true) {
    this.r = e49, this.g = t2, this.b = n2, this.a = r2, i2 || (this.r *= r2, this.g *= r2, this.b *= r2, r2 || this.overwriteGetter(`rgb`, [e49, t2, n2, r2]));
  }
  static parse(t2) {
    if (t2 instanceof _a) return t2;
    if (typeof t2 != `string`) return;
    let n2 = $e(t2);
    if (n2) return new _a(...n2, false);
  }
  get rgb() {
    let { r: e49, g: t2, b: n2, a: r2 } = this, i2 = r2 || 1 / 0;
    return this.overwriteGetter(`rgb`, [e49 / i2, t2 / i2, n2 / i2, r2]);
  }
  get hcl() {
    return this.overwriteGetter(`hcl`, Je(this.rgb));
  }
  get lab() {
    return this.overwriteGetter(`lab`, Ue(this.rgb));
  }
  overwriteGetter(e49, t2) {
    return Object.defineProperty(this, e49, { value: t2 }), t2;
  }
  toString() {
    let [e49, t2, n2, r2] = this.rgb;
    return `rgba(${[e49, t2, n2].map((e50) => Math.round(e50 * 255)).join(`,`)},${r2})`;
  }
  static interpolate(t2, n2, r2, i2 = `rgb`) {
    switch (i2) {
      case `rgb`: {
        let [i3, a2, o2, s2] = ot(t2.rgb, n2.rgb, r2);
        return new _a(i3, a2, o2, s2, false);
      }
      case `hcl`: {
        let [i3, a2, o2, s2] = t2.hcl, [c2, l2, u2, d2] = n2.hcl, f2, p2;
        if (!isNaN(i3) && !isNaN(c2)) {
          let e49 = c2 - i3;
          c2 > i3 && e49 > 180 ? e49 -= 360 : c2 < i3 && i3 - c2 > 180 && (e49 += 360), f2 = i3 + r2 * e49;
        } else isNaN(i3) ? isNaN(c2) ? f2 = NaN : (f2 = c2, (o2 === 1 || o2 === 0) && (p2 = l2)) : (f2 = i3, (u2 === 1 || u2 === 0) && (p2 = a2));
        let [m2, h2, g2, _2] = Ye([f2, p2 ?? at(a2, l2, r2), at(o2, u2, r2), at(s2, d2, r2)]);
        return new _a(m2, h2, g2, _2, false);
      }
      case `lab`: {
        let [i3, a2, o2, s2] = Ge(ot(t2.lab, n2.lab, r2));
        return new _a(i3, a2, o2, s2, false);
      }
    }
  }
}, _a.black = new _a(0, 0, 0, 1), _a.white = new _a(1, 1, 1, 1), _a.transparent = new _a(0, 0, 0, 0), _a.red = new _a(1, 0, 0, 1), _a);
const ct = [`bottom`, `center`, `top`];
var lt = class {
  constructor(e49, t2, n2, r2, i2, a2) {
    this.text = e49, this.image = t2, this.scale = n2, this.fontStack = r2, this.textColor = i2, this.verticalAlign = a2;
  }
}, ut = class e3 {
  constructor(e49) {
    this.sections = e49;
  }
  static fromString(t2) {
    return new e3([new lt(t2, null, null, null, null, null)]);
  }
  isEmpty() {
    return this.sections.length === 0 || !this.sections.some((e49) => e49.text.length !== 0 || e49.image && e49.image.name.length !== 0);
  }
  static factory(t2) {
    return t2 instanceof e3 ? t2 : e3.fromString(t2);
  }
  toString() {
    return this.sections.length === 0 ? `` : this.sections.map((e49) => e49.text).join(``);
  }
}, dt = class e4 {
  constructor(e49) {
    this.values = e49.slice();
  }
  static parse(t2) {
    if (t2 instanceof e4) return t2;
    if (typeof t2 == `number`) return new e4([t2, t2, t2, t2]);
    if (Array.isArray(t2) && !(t2.length < 1 || t2.length > 4)) {
      for (let e49 of t2) if (typeof e49 != `number`) return;
      switch (t2.length) {
        case 1:
          t2 = [t2[0], t2[0], t2[0], t2[0]];
          break;
        case 2:
          t2 = [t2[0], t2[1], t2[0], t2[1]];
          break;
        case 3:
          t2 = [t2[0], t2[1], t2[2], t2[1]];
      }
      return new e4(t2);
    }
  }
  toString() {
    return JSON.stringify(this.values);
  }
  static interpolate(t2, n2, r2) {
    return new e4(ot(t2.values, n2.values, r2));
  }
}, ft = class e5 {
  constructor(e49) {
    this.values = e49.slice();
  }
  static parse(t2) {
    if (t2 instanceof e5) return t2;
    if (typeof t2 == `number`) return new e5([t2]);
    if (Array.isArray(t2)) {
      for (let e49 of t2) if (typeof e49 != `number`) return;
      return new e5(t2);
    }
  }
  toString() {
    return JSON.stringify(this.values);
  }
  static interpolate(t2, n2, r2) {
    return new e5(ot(t2.values, n2.values, r2));
  }
}, pt = class e6 {
  constructor(e49) {
    this.values = e49.slice();
  }
  static parse(t2) {
    if (t2 instanceof e6) return t2;
    if (typeof t2 == `string`) {
      let n3 = V.parse(t2);
      return n3 ? new e6([n3]) : void 0;
    }
    if (!Array.isArray(t2)) return;
    let n2 = [];
    for (let e49 of t2) {
      if (typeof e49 != `string`) return;
      let t3 = V.parse(e49);
      if (!t3) return;
      n2.push(t3);
    }
    return new e6(n2);
  }
  toString() {
    return JSON.stringify(this.values);
  }
  static interpolate(t2, n2, r2, i2 = `rgb`) {
    let a2 = [];
    if (t2.values.length != n2.values.length) throw Error(`colorArray: Arrays have mismatched length (${t2.values.length} vs. ${n2.values.length}), cannot interpolate.`);
    for (let e49 = 0; e49 < t2.values.length; e49++) a2.push(V.interpolate(t2.values[e49], n2.values[e49], r2, i2));
    return new e6(a2);
  }
}, H = class extends Error {
  constructor(e49, t2) {
    super(e49), this.name = `RuntimeError`, this.path = t2;
  }
  toJSON() {
    return this.message;
  }
};
const mt = /* @__PURE__ */ new Set([`center`, `left`, `right`, `top`, `bottom`, `top-left`, `top-right`, `bottom-left`, `bottom-right`]);
var ht = class e7 {
  constructor(e49) {
    this.values = e49.slice();
  }
  static parse(t2) {
    if (t2 instanceof e7) return t2;
    if (!(!Array.isArray(t2) || t2.length < 1 || t2.length % 2 != 0)) {
      for (let e49 = 0; e49 < t2.length; e49 += 2) {
        let n2 = t2[e49], r2 = t2[e49 + 1];
        if (typeof n2 != `string` || !mt.has(n2) || !Array.isArray(r2) || r2.length !== 2 || typeof r2[0] != `number` || typeof r2[1] != `number`) return;
      }
      return new e7(t2);
    }
  }
  toString() {
    return JSON.stringify(this.values);
  }
  static interpolate(t2, n2, r2, i2) {
    let a2 = t2.values, o2 = n2.values;
    if (a2.length !== o2.length) throw new H(`Cannot interpolate values of different length. from: ${t2.toString()}, to: ${n2.toString()}`, i2);
    let s2 = [];
    for (let e49 = 0; e49 < a2.length; e49 += 2) {
      if (a2[e49] !== o2[e49]) throw new H(`Cannot interpolate values containing mismatched anchors. from[${e49}]: ${a2[e49]}, to[${e49}]: ${o2[e49]}`, i2);
      s2.push(a2[e49]);
      let [t3, n3] = a2[e49 + 1], [c2, l2] = o2[e49 + 1];
      s2.push([at(t3, c2, r2), at(n3, l2, r2)]);
    }
    return new e7(s2);
  }
}, gt = class e8 {
  constructor(e49) {
    this.name = e49.name, this.available = e49.available;
  }
  toString() {
    return this.name;
  }
  static fromString(t2) {
    return t2 ? new e8({ name: t2, available: false }) : null;
  }
}, _t = class e9 {
  constructor(e49, t2, n2) {
    this.from = e49, this.to = t2, this.transition = n2;
  }
  toString() {
    return this.from === this.to && this.transition === 1 ? this.from : JSON.stringify([this.from, this.to, this.transition]);
  }
  static interpolate(t2, n2, r2) {
    return new e9(t2, n2, r2);
  }
  static parse(t2) {
    if (t2 instanceof e9) return t2;
    if (Array.isArray(t2) && t2.length === 3 && typeof t2[0] == `string` && typeof t2[1] == `string` && typeof t2[2] == `number`) return new e9(t2[0], t2[1], t2[2]);
    if (typeof t2 == `object` && typeof t2.from == `string` && typeof t2.to == `string` && typeof t2.transition == `number`) return new e9(t2.from, t2.to, t2.transition);
    if (typeof t2 == `string`) return new e9(t2, t2, 1);
  }
}, vt = class {
  constructor(e49, t2, n2) {
    this.sensitivity = e49 ? t2 ? `variant` : `case` : t2 ? `accent` : `base`, this.locale = n2, this.collator = new Intl.Collator(this.locale ? this.locale : [], { sensitivity: this.sensitivity, usage: `search` });
  }
  compare(e49, t2) {
    return this.collator.compare(e49, t2);
  }
  resolvedLocale() {
    return new Intl.Collator(this.locale ? this.locale : []).resolvedOptions().locale;
  }
};
function yt(e49, t2, n2, r2) {
  return typeof e49 == `number` && e49 >= 0 && e49 <= 255 && typeof t2 == `number` && t2 >= 0 && t2 <= 255 && typeof n2 == `number` && n2 >= 0 && n2 <= 255 ? r2 === void 0 || typeof r2 == `number` && r2 >= 0 && r2 <= 1 ? null : `Invalid rgba value [${[e49, t2, n2, r2].join(`, `)}]: 'a' must be between 0 and 1.` : `Invalid rgba value [${(typeof r2 == `number` ? [e49, t2, n2, r2] : [e49, t2, n2]).join(`, `)}]: 'r', 'g', and 'b' must be between 0 and 255.`;
}
function bt(e49) {
  if (e49 === null || typeof e49 == `string` || typeof e49 == `boolean` || typeof e49 == `number` || e49 instanceof _t || e49 instanceof V || e49 instanceof vt || e49 instanceof ut || e49 instanceof dt || e49 instanceof ft || e49 instanceof pt || e49 instanceof ht || e49 instanceof gt) return true;
  if (Array.isArray(e49)) {
    for (let t2 of e49) if (!bt(t2)) return false;
    return true;
  }
  if (typeof e49 == `object`) {
    for (let t2 in e49) if (!bt(e49[t2])) return false;
    return true;
  }
  return false;
}
function U(e49) {
  if (e49 === null) return P;
  if (typeof e49 == `string`) return I;
  if (typeof e49 == `boolean`) return L;
  if (typeof e49 == `number`) return F;
  if (e49 instanceof V) return ve;
  if (e49 instanceof _t) return ye;
  if (e49 instanceof vt) return Se;
  if (e49 instanceof ut) return Ce;
  if (e49 instanceof dt) return we;
  if (e49 instanceof ft) return Ee;
  if (e49 instanceof pt) return Te;
  if (e49 instanceof ht) return Oe;
  if (e49 instanceof gt) return De;
  if (Array.isArray(e49)) {
    let t2 = e49.length, n2;
    for (let t3 of e49) {
      let e50 = U(t3);
      if (!n2) n2 = e50;
      else if (n2 === e50) continue;
      else {
        n2 = R;
        break;
      }
    }
    return ke(n2 || R, t2);
  }
  return be;
}
function xt(e49) {
  let t2 = typeof e49;
  return e49 === null ? `` : t2 === `string` || t2 === `number` || t2 === `boolean` ? String(e49) : e49 instanceof V || e49 instanceof _t || e49 instanceof ut || e49 instanceof dt || e49 instanceof ft || e49 instanceof pt || e49 instanceof ht || e49 instanceof gt ? e49.toString() : JSON.stringify(e49);
}
var St = class e10 {
  constructor(e49, t2) {
    this.type = e49, this.value = t2;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`'literal' expression requires exactly one argument, but found ${t2.length - 1} instead.`);
    if (!bt(t2[1])) return n2.error(`invalid value of type "${typeof t2[1]}"`);
    let r2 = t2[1], i2 = U(r2), a2 = n2.expectedType;
    return i2.kind === `array` && i2.N === 0 && a2 && a2.kind === `array` && (typeof a2.N != `number` || a2.N === 0) && (i2 = a2), new e10(i2, r2);
  }
  evaluate() {
    return this.value;
  }
  eachChild() {
  }
  outputDefined() {
    return true;
  }
};
const Ct = [`Unknown`, `Point`, `LineString`, `Polygon`];
var wt = class {
  constructor() {
    this.globals = null, this.feature = null, this.featureState = null, this.formattedSection = null, this._parseColorCache = /* @__PURE__ */ new Map(), this.availableImages = null, this.canonical = null;
  }
  id() {
    return this.feature && `id` in this.feature ? this.feature.id : null;
  }
  geometryType() {
    return this.feature ? typeof this.feature.type == `number` ? Ct[this.feature.type] : this.feature.type : null;
  }
  geometry() {
    return this.feature && `geometry` in this.feature ? this.feature.geometry : null;
  }
  canonicalID() {
    return this.canonical;
  }
  properties() {
    return this.feature && this.feature.properties || {};
  }
  parseColor(e49) {
    let t2 = this._parseColorCache.get(e49);
    return t2 || (t2 = V.parse(e49), this._parseColorCache.set(e49, t2)), t2;
  }
};
function Tt(e49, t2, n2) {
  let r2 = e49.length - 1, i2 = 0, a2 = r2, o2 = 0, s2, c2;
  for (; i2 <= a2; ) if (o2 = Math.floor((i2 + a2) / 2), s2 = e49[o2], c2 = e49[o2 + 1], s2 <= t2) {
    if (o2 === r2 || t2 < c2) return o2;
    i2 = o2 + 1;
  } else if (s2 > t2) a2 = o2 - 1;
  else throw new H(`Input is not a number.`, n2);
  return 0;
}
var Et = class e11 {
  constructor(e49, t2, n2, r2) {
    this.type = e49, this.input = t2, this.key = r2, this.labels = [], this.outputs = [];
    for (let [e50, t3] of n2) this.labels.push(e50), this.outputs.push(t3);
  }
  static parse(t2, n2) {
    if (t2.length - 1 < 4) return n2.error(`Expected at least 4 arguments, but found only ${t2.length - 1}.`);
    if ((t2.length - 1) % 2 != 0) return n2.error(`Expected an even number of arguments.`);
    let r2 = n2.parse(t2[1], 1, F);
    if (!r2) return null;
    let i2 = [], a2 = null;
    n2.expectedType && n2.expectedType.kind !== `value` && (a2 = n2.expectedType);
    for (let e49 = 1; e49 < t2.length; e49 += 2) {
      let r3 = e49 === 1 ? -1 / 0 : t2[e49], o2 = t2[e49 + 1], s2 = e49, c2 = e49 + 1;
      if (typeof r3 != `number`) return n2.error(`Input/output pairs for "step" expressions must be defined using literal numeric values (not computed expressions) for the input values.`, s2);
      if (i2.length && i2[i2.length - 1][0] >= r3) return n2.error(`Input/output pairs for "step" expressions must be arranged with input values in strictly ascending order.`, s2);
      let l2 = n2.parse(o2, c2, a2);
      if (!l2) return null;
      a2 ||= l2.type, i2.push([r3, l2]);
    }
    return new e11(a2, r2, i2, n2.key);
  }
  evaluate(e49) {
    let t2 = this.labels, n2 = this.outputs;
    if (t2.length === 1) return n2[0].evaluate(e49);
    let r2 = this.input.evaluate(e49);
    if (r2 <= t2[0]) return n2[0].evaluate(e49);
    let i2 = t2.length;
    return r2 >= t2[i2 - 1] ? n2[i2 - 1].evaluate(e49) : n2[Tt(t2, r2, this.key)].evaluate(e49);
  }
  eachChild(e49) {
    e49(this.input);
    for (let t2 of this.outputs) e49(t2);
  }
  outputDefined() {
    return this.outputs.every((e49) => e49.outputDefined());
  }
};
function Dt(e49, t2, n2, r2) {
  let i2 = 3 * e49, a2 = 3 * (n2 - e49) - i2, o2 = 1 - i2 - a2, s2 = 3 * t2, c2 = 3 * (r2 - t2) - s2, l2 = 1 - s2 - c2;
  return function(e50, t3 = 1e-6) {
    if (e50 <= 0) return 0;
    if (e50 >= 1) return 1;
    let n3 = e50;
    for (let r4 = 0; r4 < 8; r4++) {
      let r5 = ((o2 * n3 + a2) * n3 + i2) * n3 - e50;
      if (Math.abs(r5) < t3) return ((l2 * n3 + c2) * n3 + s2) * n3;
      let u3 = (3 * o2 * n3 + 2 * a2) * n3 + i2;
      if (Math.abs(u3) < 1e-6) break;
      n3 -= r5 / u3;
    }
    let r3 = 0, u2 = 1;
    n3 = e50;
    for (let s3 = 0; s3 < 20; s3++) {
      let s4 = ((o2 * n3 + a2) * n3 + i2) * n3;
      if (Math.abs(s4 - e50) < t3) break;
      e50 > s4 ? r3 = n3 : u2 = n3, n3 = (r3 + u2) * 0.5;
    }
    return ((l2 * n3 + c2) * n3 + s2) * n3;
  };
}
var Ot = class e12 {
  constructor(e49, t2, n2, r2, i2, a2) {
    this.type = e49, this.operator = t2, this.interpolation = n2, this.input = r2, this.key = a2, this.labels = [], this.outputs = [];
    for (let [e50, t3] of i2) this.labels.push(e50), this.outputs.push(t3);
  }
  static interpolationFactor(e49, t2, n2, r2) {
    let i2 = 0;
    if (e49.name === `exponential`) i2 = kt(t2, e49.base, n2, r2);
    else if (e49.name === `linear`) i2 = kt(t2, 1, n2, r2);
    else if (e49.name === `cubic-bezier`) {
      let a2 = e49.controlPoints;
      i2 = Dt(a2[0], a2[1], a2[2], a2[3])(kt(t2, 1, n2, r2));
    }
    return i2;
  }
  static parse(t2, n2) {
    let [r2, i2, a2, ...o2] = t2;
    if (!Array.isArray(i2) || i2.length === 0) return n2.error(`Expected an interpolation type expression.`, 1);
    if (i2[0] === `linear`) i2 = { name: `linear` };
    else if (i2[0] === `exponential`) {
      let e49 = i2[1];
      if (typeof e49 != `number`) return n2.error(`Exponential interpolation requires a numeric base.`, 1, 1);
      i2 = { name: `exponential`, base: e49 };
    } else if (i2[0] === `cubic-bezier`) {
      let e49 = i2.slice(1);
      if (e49.length !== 4 || e49.some((e50) => typeof e50 != `number` || e50 < 0 || e50 > 1)) return n2.error(`Cubic bezier interpolation requires four numeric arguments with values between 0 and 1.`, 1);
      i2 = { name: `cubic-bezier`, controlPoints: e49 };
    } else return n2.error(`Unknown interpolation type ${String(i2[0])}`, 1, 0);
    if (t2.length - 1 < 4) return n2.error(`Expected at least 4 arguments, but found only ${t2.length - 1}.`);
    if ((t2.length - 1) % 2 != 0) return n2.error(`Expected an even number of arguments.`);
    if (a2 = n2.parse(a2, 2, F), !a2) return null;
    let s2 = [], c2 = null;
    (r2 === `interpolate-hcl` || r2 === `interpolate-lab`) && n2.expectedType != Te ? c2 = ve : n2.expectedType && n2.expectedType.kind !== `value` && (c2 = n2.expectedType);
    for (let e49 = 0; e49 < o2.length; e49 += 2) {
      let t3 = o2[e49], r3 = o2[e49 + 1], i3 = e49 + 3, a3 = e49 + 4;
      if (typeof t3 != `number`) return n2.error(`Input/output pairs for "interpolate" expressions must be defined using literal numeric values (not computed expressions) for the input values.`, i3);
      if (s2.length && s2[s2.length - 1][0] >= t3) return n2.error(`Input/output pairs for "interpolate" expressions must be arranged with input values in strictly ascending order.`, i3);
      let l2 = n2.parse(r3, a3, c2);
      if (!l2) return null;
      c2 ||= l2.type, s2.push([t3, l2]);
    }
    return !Pe(c2, F) && !Pe(c2, ye) && !Pe(c2, ve) && !Pe(c2, we) && !Pe(c2, Ee) && !Pe(c2, Te) && !Pe(c2, Oe) && !Pe(c2, ke(F)) ? n2.error(`Type ${z(c2)} is not interpolatable.`) : new e12(c2, r2, i2, a2, s2, n2.key);
  }
  evaluate(t2) {
    let n2 = this.labels, r2 = this.outputs;
    if (n2.length === 1) return r2[0].evaluate(t2);
    let i2 = this.input.evaluate(t2);
    if (i2 <= n2[0]) return r2[0].evaluate(t2);
    let a2 = n2.length;
    if (i2 >= n2[a2 - 1]) return r2[a2 - 1].evaluate(t2);
    let o2 = Tt(n2, i2, this.key), s2 = n2[o2], c2 = n2[o2 + 1], l2 = e12.interpolationFactor(this.interpolation, i2, s2, c2), u2 = r2[o2].evaluate(t2), d2 = r2[o2 + 1].evaluate(t2);
    switch (this.operator) {
      case `interpolate`:
        switch (this.type.kind) {
          case `number`:
            return at(u2, d2, l2);
          case `color`:
            return V.interpolate(u2, d2, l2);
          case `padding`:
            return dt.interpolate(u2, d2, l2);
          case `colorArray`:
            return pt.interpolate(u2, d2, l2);
          case `numberArray`:
            return ft.interpolate(u2, d2, l2);
          case `variableAnchorOffsetCollection`:
            return ht.interpolate(u2, d2, l2, this.key);
          case `array`:
            return ot(u2, d2, l2);
          case `projectionDefinition`:
            return _t.interpolate(u2, d2, l2);
        }
      case `interpolate-hcl`:
        switch (this.type.kind) {
          case `color`:
            return V.interpolate(u2, d2, l2, `hcl`);
          case `colorArray`:
            return pt.interpolate(u2, d2, l2, `hcl`);
        }
      case `interpolate-lab`:
        switch (this.type.kind) {
          case `color`:
            return V.interpolate(u2, d2, l2, `lab`);
          case `colorArray`:
            return pt.interpolate(u2, d2, l2, `lab`);
        }
    }
  }
  eachChild(e49) {
    e49(this.input);
    for (let t2 of this.outputs) e49(t2);
  }
  outputDefined() {
    return this.outputs.every((e49) => e49.outputDefined());
  }
};
function kt(e49, t2, n2, r2) {
  let i2 = r2 - n2, a2 = e49 - n2;
  return i2 === 0 ? 0 : t2 === 1 ? a2 / i2 : (t2 ** +a2 - 1) / (t2 ** +i2 - 1);
}
const At = { color: V.interpolate, number: at, padding: dt.interpolate, numberArray: ft.interpolate, colorArray: pt.interpolate, variableAnchorOffsetCollection: ht.interpolate, array: ot };
var jt = class e13 {
  constructor(e49) {
    this.type = Ce, this.sections = e49;
  }
  static parse(t2, n2) {
    if (t2.length < 2) return n2.error(`Expected at least one argument.`);
    let r2 = t2[1];
    if (!Array.isArray(r2) && typeof r2 == `object`) return n2.error(`First argument must be an image or text section.`);
    let i2 = [], a2 = false;
    for (let e49 = 1; e49 <= t2.length - 1; ++e49) {
      let r3 = t2[e49];
      if (a2 && typeof r3 == `object` && !Array.isArray(r3)) {
        a2 = false;
        let e50 = null;
        if (r3[`font-scale`] && (e50 = n2.parse(r3[`font-scale`], 1, F), !e50)) return null;
        let t3 = null;
        if (r3[`text-font`] && (t3 = n2.parse(r3[`text-font`], 1, ke(I)), !t3)) return null;
        let o2 = null;
        if (r3[`text-color`] && (o2 = n2.parse(r3[`text-color`], 1, ve), !o2)) return null;
        let s2 = null;
        if (r3[`vertical-align`]) {
          if (typeof r3[`vertical-align`] == `string` && !ct.includes(r3[`vertical-align`])) return n2.error(`'vertical-align' must be one of: 'bottom', 'center', 'top' but found '${r3[`vertical-align`]}' instead.`);
          if (s2 = n2.parse(r3[`vertical-align`], 1, I), !s2) return null;
        }
        let c2 = i2[i2.length - 1];
        c2.scale = e50, c2.font = t3, c2.textColor = o2, c2.verticalAlign = s2;
      } else {
        let r4 = n2.parse(t2[e49], 1, R);
        if (!r4) return null;
        let o2 = r4.type.kind;
        if (o2 !== `string` && o2 !== `value` && o2 !== `null` && o2 !== `resolvedImage`) return n2.error(`Formatted text type must be 'string', 'value', 'image' or 'null'.`);
        a2 = true, i2.push({ content: r4, scale: null, font: null, textColor: null, verticalAlign: null });
      }
    }
    return new e13(i2);
  }
  evaluate(e49) {
    return new ut(this.sections.map((t2) => {
      let n2 = t2.content.evaluate(e49);
      return U(n2) === De ? new lt(``, n2, null, null, null, t2.verticalAlign ? t2.verticalAlign.evaluate(e49) : null) : new lt(xt(n2), null, t2.scale ? t2.scale.evaluate(e49) : null, t2.font ? t2.font.evaluate(e49).join(`,`) : null, t2.textColor ? t2.textColor.evaluate(e49) : null, t2.verticalAlign ? t2.verticalAlign.evaluate(e49) : null);
    }));
  }
  eachChild(e49) {
    for (let t2 of this.sections) e49(t2.content), t2.scale && e49(t2.scale), t2.font && e49(t2.font), t2.textColor && e49(t2.textColor), t2.verticalAlign && e49(t2.verticalAlign);
  }
  outputDefined() {
    return false;
  }
};
function Mt(e49, t2, n2 = 0, r2 = e49.length - 1, i2 = Pt) {
  for (; r2 > n2; ) {
    if (r2 - n2 > 600) {
      let a3 = r2 - n2 + 1, o3 = t2 - n2 + 1, s3 = Math.log(a3), c2 = 0.5 * Math.exp(2 * s3 / 3), l2 = 0.5 * Math.sqrt(s3 * c2 * (a3 - c2) / a3) * (o3 - a3 / 2 < 0 ? -1 : 1);
      Mt(e49, t2, Math.max(n2, Math.floor(t2 - o3 * c2 / a3 + l2)), Math.min(r2, Math.floor(t2 + (a3 - o3) * c2 / a3 + l2)), i2);
    }
    let a2 = e49[t2], o2 = n2, s2 = r2;
    for (Nt(e49, n2, t2), i2(e49[r2], a2) > 0 && Nt(e49, n2, r2); o2 < s2; ) {
      for (Nt(e49, o2, s2), o2++, s2--; i2(e49[o2], a2) < 0; ) o2++;
      for (; i2(e49[s2], a2) > 0; ) s2--;
    }
    i2(e49[n2], a2) === 0 ? Nt(e49, n2, s2) : (s2++, Nt(e49, s2, r2)), s2 <= t2 && (n2 = s2 + 1), t2 <= s2 && (r2 = s2 - 1);
  }
}
function Nt(e49, t2, n2) {
  let r2 = e49[t2];
  e49[t2] = e49[n2], e49[n2] = r2;
}
function Pt(e49, t2) {
  return e49 < t2 ? -1 : +(e49 > t2);
}
function Ft(e49, t2) {
  if (e49.length <= 1) return [e49];
  let n2 = [], r2, i2;
  for (let t3 of e49) {
    let e50 = Lt(t3);
    e50 !== 0 && (t3.area = Math.abs(e50), i2 === void 0 && (i2 = e50 < 0), i2 === e50 < 0 ? (r2 && n2.push(r2), r2 = [t3]) : r2.push(t3));
  }
  if (r2 && n2.push(r2), t2 > 1) for (let e50 = 0; e50 < n2.length; e50++) n2[e50].length <= t2 || (Mt(n2[e50], t2, 1, n2[e50].length - 1, It), n2[e50] = n2[e50].slice(0, t2));
  return n2;
}
function It(e49, t2) {
  return t2.area - e49.area;
}
function Lt(e49) {
  let t2 = 0;
  for (let n2 = 0, r2 = e49.length, i2 = r2 - 1, a2, o2; n2 < r2; i2 = n2++) a2 = e49[n2], o2 = e49[i2], t2 += (o2.x - a2.x) * (a2.y + o2.y);
  return t2;
}
const Rt = { string: I, number: F, boolean: L, object: be };
var zt = class e14 {
  constructor(e49, t2, n2) {
    this.type = e49, this.args = t2, this.key = n2;
  }
  static parse(t2, n2) {
    if (t2.length < 2) return n2.error(`Expected at least one argument.`);
    let r2 = 1, i2, a2 = t2[0];
    if (a2 === `array`) {
      let e49;
      if (t2.length > 2) {
        let i3 = t2[1];
        if (typeof i3 != `string` || !(i3 in Rt) || i3 === `object`) return n2.error(`The item type argument of "array" must be one of string, number, boolean`, 1);
        e49 = Rt[i3], r2++;
      } else e49 = R;
      let a3;
      if (t2.length > 3) {
        if (t2[2] !== null && (typeof t2[2] != `number` || t2[2] < 0 || t2[2] !== Math.floor(t2[2]))) return n2.error(`The length argument to "array" must be a positive integer literal`, 2);
        a3 = t2[2], r2++;
      }
      i2 = ke(e49, a3);
    } else {
      if (!Rt[a2]) throw Error(`Types doesn't contain name = ${a2}`);
      i2 = Rt[a2];
    }
    let o2 = [];
    for (; r2 < t2.length; r2++) {
      let e49 = n2.parse(t2[r2], r2, R);
      if (!e49) return null;
      o2.push(e49);
    }
    return new e14(i2, o2, n2.key);
  }
  evaluate(e49) {
    for (let t2 = 0; t2 < this.args.length; t2++) {
      let n2 = this.args[t2].evaluate(e49);
      if (!je(this.type, U(n2))) return n2;
      if (t2 === this.args.length - 1) throw new H(`Expected value to be of type ${z(this.type)}, but found ${z(U(n2))} instead.`, this.key);
    }
    throw Error();
  }
  eachChild(e49) {
    this.args.forEach(e49);
  }
  outputDefined() {
    return this.args.every((e49) => e49.outputDefined());
  }
};
const Bt = { "to-boolean": L, "to-color": ve, "to-number": F, "to-string": I };
var Vt = class e15 {
  constructor(e49, t2, n2) {
    this.type = e49, this.args = t2, this.key = n2;
  }
  static parse(t2, n2) {
    if (t2.length < 2) return n2.error(`Expected at least one argument.`);
    let r2 = t2[0];
    if (!Bt[r2]) throw Error(`Can't parse ${r2} as it is not part of the known types`);
    if ((r2 === `to-boolean` || r2 === `to-string`) && t2.length !== 2) return n2.error(`Expected one argument.`);
    let i2 = Bt[r2], a2 = [];
    for (let e49 = 1; e49 < t2.length; e49++) {
      let r3 = n2.parse(t2[e49], e49, R);
      if (!r3) return null;
      a2.push(r3);
    }
    return new e15(i2, a2, n2.key);
  }
  evaluate(e49) {
    switch (this.type.kind) {
      case `boolean`:
        return !!this.args[0].evaluate(e49);
      case `color`: {
        let t2, n2;
        for (let r2 of this.args) {
          if (t2 = r2.evaluate(e49), n2 = null, t2 instanceof V) return t2;
          if (typeof t2 == `string`) {
            let n3 = e49.parseColor(t2);
            if (n3) return n3;
          } else if (Array.isArray(t2) && (n2 = t2.length < 3 || t2.length > 4 ? `Invalid rgba value ${JSON.stringify(t2)}: expected an array containing either three or four numeric values.` : yt(t2[0], t2[1], t2[2], t2[3]), !n2)) return new V(t2[0] / 255, t2[1] / 255, t2[2] / 255, t2[3]);
        }
        throw new H(n2 || `Could not parse color from value '${typeof t2 == `string` ? t2 : JSON.stringify(t2)}'`, this.key);
      }
      case `padding`: {
        let t2;
        for (let n2 of this.args) {
          t2 = n2.evaluate(e49);
          let r2 = dt.parse(t2);
          if (r2) return r2;
        }
        throw new H(`Could not parse padding from value '${typeof t2 == `string` ? t2 : JSON.stringify(t2)}'`, this.key);
      }
      case `numberArray`: {
        let t2;
        for (let n2 of this.args) {
          t2 = n2.evaluate(e49);
          let r2 = ft.parse(t2);
          if (r2) return r2;
        }
        throw new H(`Could not parse numberArray from value '${typeof t2 == `string` ? t2 : JSON.stringify(t2)}'`, this.key);
      }
      case `colorArray`: {
        let t2;
        for (let n2 of this.args) {
          t2 = n2.evaluate(e49);
          let r2 = pt.parse(t2);
          if (r2) return r2;
        }
        throw new H(`Could not parse colorArray from value '${typeof t2 == `string` ? t2 : JSON.stringify(t2)}'`, this.key);
      }
      case `variableAnchorOffsetCollection`: {
        let t2;
        for (let n2 of this.args) {
          t2 = n2.evaluate(e49);
          let r2 = ht.parse(t2);
          if (r2) return r2;
        }
        throw new H(`Could not parse variableAnchorOffsetCollection from value '${typeof t2 == `string` ? t2 : JSON.stringify(t2)}'`, this.key);
      }
      case `number`: {
        let t2 = null;
        for (let n2 of this.args) {
          if (t2 = n2.evaluate(e49), t2 === null) return 0;
          let r2 = Number(t2);
          if (!isNaN(r2)) return r2;
        }
        throw new H(`Could not convert ${JSON.stringify(t2)} to number.`, this.key);
      }
      case `formatted`:
        return ut.fromString(xt(this.args[0].evaluate(e49)));
      case `resolvedImage`:
        return gt.fromString(xt(this.args[0].evaluate(e49)));
      case `projectionDefinition`: {
        let t2 = this.args[0].evaluate(e49);
        if (_t.parse(t2)) return t2;
        throw new H(`Could not parse projectionDefinition from value '${typeof t2 == `string` ? t2 : JSON.stringify(t2)}'`, this.key);
      }
      default:
        return xt(this.args[0].evaluate(e49));
    }
  }
  eachChild(e49) {
    this.args.forEach(e49);
  }
  outputDefined() {
    return this.args.every((e49) => e49.outputDefined());
  }
}, Ht = class e16 {
  constructor(e49, t2) {
    this.type = t2.type, this.bindings = [].concat(e49), this.result = t2;
  }
  evaluate(e49) {
    return this.result.evaluate(e49);
  }
  eachChild(e49) {
    for (let t2 of this.bindings) e49(t2[1]);
    e49(this.result);
  }
  static parse(t2, n2) {
    if (t2.length < 4) return n2.error(`Expected at least 3 arguments, but found ${t2.length - 1} instead.`);
    let r2 = [];
    for (let e49 = 1; e49 < t2.length - 1; e49 += 2) {
      let i3 = t2[e49];
      if (typeof i3 != `string`) return n2.error(`Expected string, but found ${typeof i3} instead.`, e49);
      if (/[^a-zA-Z0-9_]/.test(i3)) return n2.error(`Variable names must contain only alphanumeric characters or '_'.`, e49);
      let a2 = n2.parse(t2[e49 + 1], e49 + 1);
      if (!a2) return null;
      r2.push([i3, a2]);
    }
    let i2 = n2.parse(t2[t2.length - 1], t2.length - 1, n2.expectedType, r2);
    return i2 ? new e16(r2, i2) : null;
  }
  outputDefined() {
    return this.result.outputDefined();
  }
}, Ut = class e17 {
  constructor(e49, t2) {
    this.type = t2.type, this.name = e49, this.boundExpression = t2;
  }
  static parse(t2, n2) {
    if (t2.length !== 2 || typeof t2[1] != `string`) return n2.error(`'var' expression requires exactly one string literal argument.`);
    let r2 = t2[1];
    return n2.scope.has(r2) ? new e17(r2, n2.scope.get(r2)) : n2.error(`Unknown variable "${r2}". Make sure "${r2}" has been bound in an enclosing "let" expression before using it.`, 1);
  }
  evaluate(e49) {
    return this.boundExpression.evaluate(e49);
  }
  eachChild() {
  }
  outputDefined() {
    return false;
  }
}, Wt = class e18 {
  constructor(e49, t2, n2, r2) {
    this.type = e49, this.index = t2, this.input = n2, this.key = r2;
  }
  static parse(t2, n2) {
    if (t2.length !== 3) return n2.error(`Expected 2 arguments, but found ${t2.length - 1} instead.`);
    let r2 = n2.parse(t2[1], 1, F), i2 = n2.parse(t2[2], 2, ke(n2.expectedType || R));
    if (!r2 || !i2) return null;
    let a2 = i2.type;
    return new e18(a2.itemType, r2, i2, n2.key);
  }
  evaluate(e49) {
    let t2 = this.index.evaluate(e49), n2 = this.input.evaluate(e49);
    if (t2 < 0) throw new H(`Array index out of bounds: ${t2} < 0.`, this.key);
    if (t2 >= n2.length) throw new H(`Array index out of bounds: ${t2} > ${n2.length - 1}.`, this.key);
    if (t2 !== Math.floor(t2)) throw new H(`Array index must be an integer, but found ${t2} instead.`, this.key);
    return n2[t2];
  }
  eachChild(e49) {
    e49(this.index), e49(this.input);
  }
  outputDefined() {
    return false;
  }
}, Gt = class e19 {
  constructor(e49, t2, n2) {
    this.needle = e49, this.haystack = t2, this.key = n2, this.type = L;
  }
  static parse(t2, n2) {
    if (t2.length !== 3) return n2.error(`Expected 2 arguments, but found ${t2.length - 1} instead.`);
    let r2 = n2.parse(t2[1], 1, R), i2 = n2.parse(t2[2], 2, R);
    return !r2 || !i2 ? null : Me(r2.type, [L, I, F, P, R]) ? new e19(r2, i2, n2.key) : n2.error(`Expected first argument to be of type boolean, string, number or null, but found ${z(r2.type)} instead`);
  }
  evaluate(e49) {
    let t2 = this.needle.evaluate(e49), n2 = this.haystack.evaluate(e49);
    if (!n2) return false;
    if (!Ne(t2, [`boolean`, `string`, `number`, `null`])) throw new H(`Expected first argument to be of type boolean, string, number or null, but found ${z(U(t2))} instead.`, this.key);
    if (!Ne(n2, [`string`, `array`])) throw new H(`Expected second argument to be of type array or string, but found ${z(U(n2))} instead.`, this.key);
    return n2.indexOf(t2) >= 0;
  }
  eachChild(e49) {
    e49(this.needle), e49(this.haystack);
  }
  outputDefined() {
    return true;
  }
}, Kt = class e20 {
  constructor(e49, t2, n2, r2) {
    this.needle = e49, this.haystack = t2, this.key = n2, this.fromIndex = r2, this.type = F;
  }
  static parse(t2, n2) {
    if (t2.length <= 2 || t2.length >= 5) return n2.error(`Expected 2 or 3 arguments, but found ${t2.length - 1} instead.`);
    let r2 = n2.parse(t2[1], 1, R), i2 = n2.parse(t2[2], 2, R);
    if (!r2 || !i2) return null;
    if (!Me(r2.type, [L, I, F, P, R])) return n2.error(`Expected first argument to be of type boolean, string, number or null, but found ${z(r2.type)} instead`);
    if (t2.length === 4) {
      let a2 = n2.parse(t2[3], 3, F);
      return a2 ? new e20(r2, i2, n2.key, a2) : null;
    }
    return new e20(r2, i2, n2.key);
  }
  evaluate(e49) {
    let t2 = this.needle.evaluate(e49), n2 = this.haystack.evaluate(e49);
    if (!Ne(t2, [`boolean`, `string`, `number`, `null`])) throw new H(`Expected first argument to be of type boolean, string, number or null, but found ${z(U(t2))} instead.`, this.key);
    let r2;
    if (this.fromIndex && (r2 = this.fromIndex.evaluate(e49)), Ne(n2, [`string`])) {
      let e50 = n2.indexOf(t2, r2);
      return e50 === -1 ? -1 : [...n2.slice(0, e50)].length;
    }
    if (Ne(n2, [`array`])) return n2.indexOf(t2, r2);
    throw new H(`Expected second argument to be of type array or string, but found ${z(U(n2))} instead.`, this.key);
  }
  eachChild(e49) {
    e49(this.needle), e49(this.haystack), this.fromIndex && e49(this.fromIndex);
  }
  outputDefined() {
    return false;
  }
}, qt = class e21 {
  constructor(e49, t2, n2, r2, i2, a2) {
    this.inputType = e49, this.type = t2, this.input = n2, this.cases = r2, this.outputs = i2, this.otherwise = a2;
  }
  static parse(t2, n2) {
    if (t2.length < 5) return n2.error(`Expected at least 4 arguments, but found only ${t2.length - 1}.`);
    if (t2.length % 2 != 1) return n2.error(`Expected an even number of arguments.`);
    let r2, i2;
    n2.expectedType && n2.expectedType.kind !== `value` && (i2 = n2.expectedType);
    let a2 = {}, o2 = [];
    for (let e49 = 2; e49 < t2.length - 1; e49 += 2) {
      let s3 = t2[e49], c3 = t2[e49 + 1];
      Array.isArray(s3) || (s3 = [s3]);
      let l2 = n2.concat(e49);
      if (s3.length === 0) return l2.error(`Expected at least one branch label.`);
      for (let e50 of s3) {
        if (typeof e50 != `number` && typeof e50 != `string`) return l2.error(`Branch labels must be numbers or strings.`);
        if (typeof e50 == `number` && Math.abs(e50) > 2 ** 53 - 1) return l2.error(`Branch labels must be integers no larger than ${2 ** 53 - 1}.`);
        if (typeof e50 == `number` && Math.floor(e50) !== e50) return l2.error(`Numeric branch labels must be integer values.`);
        if (!r2) r2 = U(e50);
        else if (l2.checkSubtype(r2, U(e50))) return null;
        if (a2[String(e50)] !== void 0) return l2.error(`Branch labels must be unique.`);
        a2[String(e50)] = o2.length;
      }
      let u2 = n2.parse(c3, e49, i2);
      if (!u2) return null;
      i2 ||= u2.type, o2.push(u2);
    }
    let s2 = n2.parse(t2[1], 1, R);
    if (!s2) return null;
    let c2 = n2.parse(t2[t2.length - 1], t2.length - 1, i2);
    return !c2 || s2.type.kind !== `value` && n2.concat(1).checkSubtype(r2, s2.type) ? null : new e21(r2, i2, s2, a2, o2, c2);
  }
  evaluate(e49) {
    let t2 = this.input.evaluate(e49);
    return (U(t2) === this.inputType && this.outputs[this.cases[t2]] || this.otherwise).evaluate(e49);
  }
  eachChild(e49) {
    e49(this.input), this.outputs.forEach(e49), e49(this.otherwise);
  }
  outputDefined() {
    return this.outputs.every((e49) => e49.outputDefined()) && this.otherwise.outputDefined();
  }
}, Jt = class e22 {
  constructor(e49, t2, n2) {
    this.type = e49, this.branches = t2, this.otherwise = n2;
  }
  static parse(t2, n2) {
    if (t2.length < 4) return n2.error(`Expected at least 3 arguments, but found only ${t2.length - 1}.`);
    if (t2.length % 2 != 0) return n2.error(`Expected an odd number of arguments.`);
    let r2;
    n2.expectedType && n2.expectedType.kind !== `value` && (r2 = n2.expectedType);
    let i2 = [];
    for (let e49 = 1; e49 < t2.length - 1; e49 += 2) {
      let a3 = n2.parse(t2[e49], e49, L);
      if (!a3) return null;
      let o2 = n2.parse(t2[e49 + 1], e49 + 1, r2);
      if (!o2) return null;
      i2.push([a3, o2]), r2 ||= o2.type;
    }
    let a2 = n2.parse(t2[t2.length - 1], t2.length - 1, r2);
    if (!a2) return null;
    if (!r2) throw Error(`Can't infer output type`);
    return new e22(r2, i2, a2);
  }
  evaluate(e49) {
    for (let [t2, n2] of this.branches) if (t2.evaluate(e49)) return n2.evaluate(e49);
    return this.otherwise.evaluate(e49);
  }
  eachChild(e49) {
    for (let [t2, n2] of this.branches) e49(t2), e49(n2);
    e49(this.otherwise);
  }
  outputDefined() {
    return this.branches.every(([e49, t2]) => t2.outputDefined()) && this.otherwise.outputDefined();
  }
}, Yt = class e23 {
  constructor(e49, t2, n2, r2, i2) {
    this.type = e49, this.input = t2, this.beginIndex = n2, this.key = r2, this.endIndex = i2;
  }
  static parse(t2, n2) {
    if (t2.length <= 2 || t2.length >= 5) return n2.error(`Expected 2 or 3 arguments, but found ${t2.length - 1} instead.`);
    let r2 = n2.parse(t2[1], 1, R), i2 = n2.parse(t2[2], 2, F);
    if (!r2 || !i2) return null;
    if (!Me(r2.type, [ke(R), I, R])) return n2.error(`Expected first argument to be of type array or string, but found ${z(r2.type)} instead`);
    if (t2.length === 4) {
      let a2 = n2.parse(t2[3], 3, F);
      return a2 ? new e23(r2.type, r2, i2, n2.key, a2) : null;
    }
    return new e23(r2.type, r2, i2, n2.key);
  }
  evaluate(e49) {
    let t2 = this.input.evaluate(e49), n2 = this.beginIndex.evaluate(e49), r2;
    if (this.endIndex && (r2 = this.endIndex.evaluate(e49)), Ne(t2, [`string`])) return [...t2].slice(n2, r2).join(``);
    if (Ne(t2, [`array`])) return t2.slice(n2, r2);
    throw new H(`Expected first argument to be of type array or string, but found ${z(U(t2))} instead.`, this.key);
  }
  eachChild(e49) {
    e49(this.input), e49(this.beginIndex), this.endIndex && e49(this.endIndex);
  }
  outputDefined() {
    return false;
  }
}, Xt = class e24 {
  constructor(e49, t2) {
    this.type = e49, this.args = t2;
  }
  static parse(t2, n2) {
    if (t2.length < 2) return n2.error(`Expected at least one argument.`);
    let r2 = null, i2 = n2.expectedType;
    i2 && i2.kind !== `value` && (r2 = i2);
    let a2 = [];
    for (let e49 of t2.slice(1)) {
      let t3 = n2.parse(e49, 1 + a2.length, r2, void 0, { typeAnnotation: `omit` });
      if (!t3) return null;
      r2 ||= t3.type, a2.push(t3);
    }
    if (!r2) throw Error(`No output type`);
    return i2 && a2.some((e49) => je(i2, e49.type)) ? new e24(R, a2) : new e24(r2, a2);
  }
  evaluate(e49) {
    let t2 = null, n2 = 0, r2;
    for (let i2 of this.args) if (n2++, t2 = i2.evaluate(e49), t2 && t2 instanceof gt && !t2.available && (r2 ||= t2.name, t2 = null, n2 === this.args.length && (t2 = r2)), t2 !== null) break;
    return t2;
  }
  eachChild(e49) {
    this.args.forEach(e49);
  }
  outputDefined() {
    return this.args.every((e49) => e49.outputDefined());
  }
};
function Zt(e49, t2) {
  return e49 === `==` || e49 === `!=` ? t2.kind === `boolean` || t2.kind === `string` || t2.kind === `number` || t2.kind === `null` || t2.kind === `value` : t2.kind === `string` || t2.kind === `number` || t2.kind === `value`;
}
function Qt(e49, t2, n2) {
  return t2 === n2;
}
function $t(e49, t2, n2) {
  return t2 !== n2;
}
function en(e49, t2, n2) {
  return t2 < n2;
}
function tn(e49, t2, n2) {
  return t2 > n2;
}
function nn(e49, t2, n2) {
  return t2 <= n2;
}
function rn(e49, t2, n2) {
  return t2 >= n2;
}
function an(e49, t2, n2, r2) {
  return r2.compare(t2, n2) === 0;
}
function on(e49, t2, n2, r2) {
  return !an(e49, t2, n2, r2);
}
function sn(e49, t2, n2, r2) {
  return r2.compare(t2, n2) < 0;
}
function cn(e49, t2, n2, r2) {
  return r2.compare(t2, n2) > 0;
}
function ln(e49, t2, n2, r2) {
  return r2.compare(t2, n2) <= 0;
}
function un(e49, t2, n2, r2) {
  return r2.compare(t2, n2) >= 0;
}
function dn(e49, t2, n2) {
  let r2 = e49 !== `==` && e49 !== `!=`;
  return class i2 {
    constructor(e50, t3, n3, r3) {
      this.lhs = e50, this.rhs = t3, this.key = n3, this.collator = r3, this.type = L, this.hasUntypedArgument = e50.type.kind === `value` || t3.type.kind === `value`;
    }
    static parse(e50, t3) {
      if (e50.length !== 3 && e50.length !== 4) return t3.error(`Expected two or three arguments.`);
      let n3 = e50[0], a2 = t3.parse(e50[1], 1, R);
      if (!a2) return null;
      if (!Zt(n3, a2.type)) return t3.concat(1).error(`"${n3}" comparisons are not supported for type '${z(a2.type)}'.`);
      let o2 = t3.parse(e50[2], 2, R);
      if (!o2) return null;
      if (!Zt(n3, o2.type)) return t3.concat(2).error(`"${n3}" comparisons are not supported for type '${z(o2.type)}'.`);
      if (a2.type.kind !== o2.type.kind && a2.type.kind !== `value` && o2.type.kind !== `value`) return t3.error(`Cannot compare types '${z(a2.type)}' and '${z(o2.type)}'.`);
      r2 && (a2.type.kind === `value` && o2.type.kind !== `value` ? a2 = new zt(o2.type, [a2], t3.key) : a2.type.kind !== `value` && o2.type.kind === `value` && (o2 = new zt(a2.type, [o2], t3.key)));
      let s2 = null;
      if (e50.length === 4) {
        if (a2.type.kind !== `string` && o2.type.kind !== `string` && a2.type.kind !== `value` && o2.type.kind !== `value`) return t3.error(`Cannot use collator to compare non-string types.`);
        if (s2 = t3.parse(e50[3], 3, Se), !s2) return null;
      }
      return new i2(a2, o2, t3.key, s2);
    }
    evaluate(i3) {
      let a2 = this.lhs.evaluate(i3), o2 = this.rhs.evaluate(i3);
      if (r2 && this.hasUntypedArgument) {
        let t3 = U(a2), n3 = U(o2);
        if (t3.kind !== n3.kind || t3.kind !== `string` && t3.kind !== `number`) throw new H(`Expected arguments for "${e49}" to be (string, string) or (number, number), but found (${t3.kind}, ${n3.kind}) instead.`, this.key);
      }
      if (this.collator && !r2 && this.hasUntypedArgument) {
        let e50 = U(a2), n3 = U(o2);
        if (e50.kind !== `string` || n3.kind !== `string`) return t2(i3, a2, o2);
      }
      return this.collator ? n2(i3, a2, o2, this.collator.evaluate(i3)) : t2(i3, a2, o2);
    }
    eachChild(e50) {
      e50(this.lhs), e50(this.rhs), this.collator && e50(this.collator);
    }
    outputDefined() {
      return true;
    }
  };
}
const fn = dn(`==`, Qt, an), pn = dn(`!=`, $t, on), mn = dn(`<`, en, sn), hn = dn(`>`, tn, cn), gn = dn(`<=`, nn, ln), _n = dn(`>=`, rn, un);
var vn = class e25 {
  constructor(e49, t2, n2) {
    this.type = Se, this.locale = n2, this.caseSensitive = e49, this.diacriticSensitive = t2;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`Expected one argument.`);
    let r2 = t2[1];
    if (typeof r2 != `object` || Array.isArray(r2)) return n2.error(`Collator options argument must be an object.`);
    let i2 = n2.parse(r2[`case-sensitive`] !== void 0 && r2[`case-sensitive`], 1, L);
    if (!i2) return null;
    let a2 = n2.parse(r2[`diacritic-sensitive`] !== void 0 && r2[`diacritic-sensitive`], 1, L);
    if (!a2) return null;
    let o2 = null;
    return r2.locale && (o2 = n2.parse(r2.locale, 1, I), !o2) ? null : new e25(i2, a2, o2);
  }
  evaluate(e49) {
    return new vt(this.caseSensitive.evaluate(e49), this.diacriticSensitive.evaluate(e49), this.locale ? this.locale.evaluate(e49) : null);
  }
  eachChild(e49) {
    e49(this.caseSensitive), e49(this.diacriticSensitive), this.locale && e49(this.locale);
  }
  outputDefined() {
    return false;
  }
}, yn = class e26 {
  constructor(e49, t2, n2, r2, i2, a2) {
    this.type = I, this.number = e49, this.locale = t2, this.currency = n2, this.unit = r2, this.minFractionDigits = i2, this.maxFractionDigits = a2;
  }
  static parse(t2, n2) {
    if (t2.length !== 3) return n2.error(`Expected two arguments.`);
    let r2 = n2.parse(t2[1], 1, F);
    if (!r2) return null;
    let i2 = t2[2];
    if (typeof i2 != `object` || Array.isArray(i2)) return n2.error(`NumberFormat options argument must be an object.`);
    let a2 = null;
    if (i2.locale && (a2 = n2.parse(i2.locale, 1, I), !a2)) return null;
    let o2 = null;
    if (i2.currency && (o2 = n2.parse(i2.currency, 1, I), !o2)) return null;
    let s2 = null;
    if (i2.unit && (s2 = n2.parse(i2.unit, 1, I), !s2)) return null;
    if (o2 && s2) return n2.error("NumberFormat options `currency` and `unit` are mutually exclusive");
    let c2 = null;
    if (i2[`min-fraction-digits`] && (c2 = n2.parse(i2[`min-fraction-digits`], 1, F), !c2)) return null;
    let l2 = null;
    return i2[`max-fraction-digits`] && (l2 = n2.parse(i2[`max-fraction-digits`], 1, F), !l2) ? null : new e26(r2, a2, o2, s2, c2, l2);
  }
  evaluate(e49) {
    return new Intl.NumberFormat(this.locale ? this.locale.evaluate(e49) : [], { style: this.currency ? `currency` : this.unit ? `unit` : `decimal`, currency: this.currency ? this.currency.evaluate(e49) : void 0, unit: this.unit ? this.unit.evaluate(e49) : void 0, minimumFractionDigits: this.minFractionDigits ? this.minFractionDigits.evaluate(e49) : void 0, maximumFractionDigits: this.maxFractionDigits ? this.maxFractionDigits.evaluate(e49) : void 0 }).format(this.number.evaluate(e49));
  }
  eachChild(e49) {
    e49(this.number), this.locale && e49(this.locale), this.currency && e49(this.currency), this.unit && e49(this.unit), this.minFractionDigits && e49(this.minFractionDigits), this.maxFractionDigits && e49(this.maxFractionDigits);
  }
  outputDefined() {
    return false;
  }
}, bn = class e27 {
  constructor(e49) {
    this.type = De, this.input = e49;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`Expected two arguments.`);
    let r2 = n2.parse(t2[1], 1, I);
    return r2 ? new e27(r2) : n2.error(`No image name provided.`);
  }
  evaluate(e49) {
    let t2 = this.input.evaluate(e49), n2 = gt.fromString(t2);
    return n2 && e49.availableImages && (n2.available = e49.availableImages.indexOf(t2) > -1), n2;
  }
  eachChild(e49) {
    e49(this.input);
  }
  outputDefined() {
    return false;
  }
}, xn = class e28 {
  constructor(e49, t2) {
    this.input = e49, this.key = t2, this.type = F;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`Expected 1 argument, but found ${t2.length - 1} instead.`);
    let r2 = n2.parse(t2[1], 1);
    return r2 ? r2.type.kind !== `array` && r2.type.kind !== `string` && r2.type.kind !== `value` ? n2.error(`Expected argument of type string or array, but found ${z(r2.type)} instead.`) : new e28(r2, n2.key) : null;
  }
  evaluate(e49) {
    let t2 = this.input.evaluate(e49);
    if (typeof t2 == `string`) return [...t2].length;
    if (Array.isArray(t2)) return t2.length;
    throw new H(`Expected value to be of type string or array, but found ${z(U(t2))} instead.`, this.key);
  }
  eachChild(e49) {
    e49(this.input);
  }
  outputDefined() {
    return false;
  }
};
const Sn = 8192;
function Cn(e49, t2) {
  let n2 = Tn(e49[0]), r2 = Dn(e49[1]), i2 = 2 ** t2.z;
  return [Math.round(n2 * i2 * Sn), Math.round(r2 * i2 * Sn)];
}
function wn(e49, t2) {
  let n2 = 2 ** t2.z, r2 = (e49[0] / Sn + t2.x) / n2, i2 = (e49[1] / Sn + t2.y) / n2;
  return [En(r2), On(i2)];
}
function Tn(e49) {
  return (180 + e49) / 360;
}
function En(e49) {
  return e49 * 360 - 180;
}
function Dn(e49) {
  return (180 - 180 / Math.PI * Math.log(Math.tan(Math.PI / 4 + e49 * Math.PI / 360))) / 360;
}
function On(e49) {
  return 360 / Math.PI * Math.atan(Math.exp((180 - e49 * 360) * Math.PI / 180)) - 90;
}
function kn(e49, t2) {
  e49[0] = Math.min(e49[0], t2[0]), e49[1] = Math.min(e49[1], t2[1]), e49[2] = Math.max(e49[2], t2[0]), e49[3] = Math.max(e49[3], t2[1]);
}
function An(e49, t2) {
  return !(e49[0] <= t2[0] || e49[2] >= t2[2] || e49[1] <= t2[1] || e49[3] >= t2[3]);
}
function jn(e49, t2, n2) {
  return t2[1] > e49[1] != n2[1] > e49[1] && e49[0] < (n2[0] - t2[0]) * (e49[1] - t2[1]) / (n2[1] - t2[1]) + t2[0];
}
function Mn(e49, t2, n2) {
  let r2 = e49[0] - t2[0], i2 = e49[1] - t2[1], a2 = e49[0] - n2[0], o2 = e49[1] - n2[1];
  return r2 * o2 - a2 * i2 === 0 && r2 * a2 <= 0 && i2 * o2 <= 0;
}
function Nn(e49, t2, n2, r2) {
  let i2 = [t2[0] - e49[0], t2[1] - e49[1]];
  return zn([r2[0] - n2[0], r2[1] - n2[1]], i2) !== 0 && !!(Bn(e49, t2, n2, r2) && Bn(n2, r2, e49, t2));
}
function Pn(e49, t2, n2) {
  for (let r2 of n2) for (let n3 = 0; n3 < r2.length - 1; ++n3) if (Nn(e49, t2, r2[n3], r2[n3 + 1])) return true;
  return false;
}
function Fn(e49, t2, n2 = false) {
  let r2 = false;
  for (let i2 of t2) for (let t3 = 0; t3 < i2.length - 1; t3++) {
    if (Mn(e49, i2[t3], i2[t3 + 1])) return n2;
    jn(e49, i2[t3], i2[t3 + 1]) && (r2 = !r2);
  }
  return r2;
}
function In(e49, t2) {
  for (let n2 of t2) if (Fn(e49, n2)) return true;
  return false;
}
function Ln(e49, t2) {
  for (let n2 of e49) if (!Fn(n2, t2)) return false;
  for (let n2 = 0; n2 < e49.length - 1; ++n2) if (Pn(e49[n2], e49[n2 + 1], t2)) return false;
  return true;
}
function Rn(e49, t2) {
  for (let n2 of t2) if (Ln(e49, n2)) return true;
  return false;
}
function zn(e49, t2) {
  return e49[0] * t2[1] - e49[1] * t2[0];
}
function Bn(e49, t2, n2, r2) {
  let i2 = e49[0] - n2[0], a2 = e49[1] - n2[1], o2 = t2[0] - n2[0], s2 = t2[1] - n2[1], c2 = r2[0] - n2[0], l2 = r2[1] - n2[1], u2 = i2 * l2 - c2 * a2, d2 = o2 * l2 - c2 * s2;
  return u2 > 0 && d2 < 0 || u2 < 0 && d2 > 0;
}
function Vn(e49, t2, n2) {
  let r2 = [];
  for (let i2 = 0; i2 < e49.length; i2++) {
    let a2 = [];
    for (let r3 = 0; r3 < e49[i2].length; r3++) {
      let o2 = Cn(e49[i2][r3], n2);
      kn(t2, o2), a2.push(o2);
    }
    r2.push(a2);
  }
  return r2;
}
function Hn(e49, t2, n2) {
  let r2 = [];
  for (let i2 = 0; i2 < e49.length; i2++) {
    let a2 = Vn(e49[i2], t2, n2);
    r2.push(a2);
  }
  return r2;
}
function Un(e49, t2, n2, r2) {
  if (e49[0] < n2[0] || e49[0] > n2[2]) {
    let t3 = r2 * 0.5, i2 = e49[0] - n2[0] > t3 ? -r2 : n2[0] - e49[0] > t3 ? r2 : 0;
    i2 === 0 && (i2 = e49[0] - n2[2] > t3 ? -r2 : n2[2] - e49[0] > t3 ? r2 : 0), e49[0] += i2;
  }
  kn(t2, e49);
}
function Wn(e49) {
  e49[0] = e49[1] = 1 / 0, e49[2] = e49[3] = -1 / 0;
}
function Gn(e49, t2, n2, r2) {
  let i2 = 2 ** r2.z * Sn, a2 = [r2.x * Sn, r2.y * Sn], o2 = [];
  for (let r3 of e49) for (let e50 of r3) {
    let r4 = [e50.x + a2[0], e50.y + a2[1]];
    Un(r4, t2, n2, i2), o2.push(r4);
  }
  return o2;
}
function Kn(e49, t2, n2, r2) {
  let i2 = 2 ** r2.z * Sn, a2 = [r2.x * Sn, r2.y * Sn], o2 = [];
  for (let n3 of e49) {
    let e50 = [];
    for (let r3 of n3) {
      let n4 = [r3.x + a2[0], r3.y + a2[1]];
      kn(t2, n4), e50.push(n4);
    }
    o2.push(e50);
  }
  if (t2[2] - t2[0] <= i2 / 2) {
    Wn(t2);
    for (let e50 of o2) for (let r3 of e50) Un(r3, t2, n2, i2);
  }
  return o2;
}
function qn(e49, t2) {
  let n2 = [1 / 0, 1 / 0, -1 / 0, -1 / 0], r2 = [1 / 0, 1 / 0, -1 / 0, -1 / 0], i2 = e49.canonicalID();
  if (t2.type === `Polygon`) {
    let a2 = Vn(t2.coordinates, r2, i2), o2 = Gn(e49.geometry(), n2, r2, i2);
    if (!An(n2, r2)) return false;
    for (let e50 of o2) if (!Fn(e50, a2)) return false;
  }
  if (t2.type === `MultiPolygon`) {
    let a2 = Hn(t2.coordinates, r2, i2), o2 = Gn(e49.geometry(), n2, r2, i2);
    if (!An(n2, r2)) return false;
    for (let e50 of o2) if (!In(e50, a2)) return false;
  }
  return true;
}
function Jn(e49, t2) {
  let n2 = [1 / 0, 1 / 0, -1 / 0, -1 / 0], r2 = [1 / 0, 1 / 0, -1 / 0, -1 / 0], i2 = e49.canonicalID();
  if (t2.type === `Polygon`) {
    let a2 = Vn(t2.coordinates, r2, i2), o2 = Kn(e49.geometry(), n2, r2, i2);
    if (!An(n2, r2)) return false;
    for (let e50 of o2) if (!Ln(e50, a2)) return false;
  }
  if (t2.type === `MultiPolygon`) {
    let a2 = Hn(t2.coordinates, r2, i2), o2 = Kn(e49.geometry(), n2, r2, i2);
    if (!An(n2, r2)) return false;
    for (let e50 of o2) if (!Rn(e50, a2)) return false;
  }
  return true;
}
var Yn = class e29 {
  constructor(e49, t2) {
    this.type = L, this.geojson = e49, this.geometries = t2;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`'within' expression requires exactly one argument, but found ${t2.length - 1} instead.`);
    if (bt(t2[1])) {
      let n3 = t2[1];
      if (n3.type === `FeatureCollection`) {
        let t3 = [];
        for (let e49 of n3.features) {
          let { type: n4, coordinates: r2 } = e49.geometry;
          n4 === `Polygon` && t3.push(r2), n4 === `MultiPolygon` && t3.push(...r2);
        }
        if (t3.length) return new e29(n3, { type: `MultiPolygon`, coordinates: t3 });
      } else if (n3.type === `Feature`) {
        let t3 = n3.geometry.type;
        if (t3 === `Polygon` || t3 === `MultiPolygon`) return new e29(n3, n3.geometry);
      } else if (n3.type === `Polygon` || n3.type === `MultiPolygon`) return new e29(n3, n3);
    }
    return n2.error(`'within' expression requires valid geojson object that contains polygon geometry type.`);
  }
  evaluate(e49) {
    if (e49.geometry() != null && e49.canonicalID() != null) {
      if (e49.geometryType() === `Point`) return qn(e49, this.geometries);
      if (e49.geometryType() === `LineString`) return Jn(e49, this.geometries);
    }
    return false;
  }
  eachChild() {
  }
  outputDefined() {
    return true;
  }
}, Xn = class {
  constructor(e49 = [], t2 = (e50, t3) => e50 < t3 ? -1 : +(e50 > t3)) {
    if (this.data = e49, this.length = this.data.length, this.compare = t2, this.length > 0) for (let e50 = (this.length >> 1) - 1; e50 >= 0; e50--) this._down(e50);
  }
  push(e49) {
    this.data.push(e49), this._up(this.length++);
  }
  pop() {
    if (this.length === 0) return;
    let e49 = this.data[0], t2 = this.data.pop();
    return --this.length > 0 && (this.data[0] = t2, this._down(0)), e49;
  }
  peek() {
    return this.data[0];
  }
  _up(e49) {
    let { data: t2, compare: n2 } = this, r2 = t2[e49];
    for (; e49 > 0; ) {
      let i2 = e49 - 1 >> 1, a2 = t2[i2];
      if (n2(r2, a2) >= 0) break;
      t2[e49] = a2, e49 = i2;
    }
    t2[e49] = r2;
  }
  _down(e49) {
    let { data: t2, compare: n2 } = this, r2 = this.length >> 1, i2 = t2[e49];
    for (; e49 < r2; ) {
      let r3 = (e49 << 1) + 1, a2 = r3 + 1;
      if (a2 < this.length && n2(t2[a2], t2[r3]) < 0 && (r3 = a2), n2(t2[r3], i2) >= 0) break;
      t2[e49] = t2[r3], e49 = r3;
    }
    t2[e49] = i2;
  }
};
const Zn = Math.PI / 180;
var Qn = class {
  constructor(e49) {
    let t2 = Zn * 6378.137 * 1e3, n2 = Math.cos(e49 * Zn), r2 = 1 / (1 - 0.0066943799901413165 * (1 - n2 * n2)), i2 = Math.sqrt(r2);
    this.kx = t2 * i2 * n2, this.ky = t2 * i2 * r2 * 0.9933056200098587;
  }
  distance(e49, t2) {
    let n2 = this.wrap(e49[0] - t2[0]) * this.kx, r2 = (e49[1] - t2[1]) * this.ky;
    return Math.sqrt(n2 * n2 + r2 * r2);
  }
  pointOnLine(e49, t2) {
    let n2 = 1 / 0, r2, i2, a2, o2;
    for (let s2 = 0; s2 < e49.length - 1; s2++) {
      let c2 = e49[s2][0], l2 = e49[s2][1], u2 = this.wrap(e49[s2 + 1][0] - c2) * this.kx, d2 = (e49[s2 + 1][1] - l2) * this.ky, f2 = 0;
      (u2 !== 0 || d2 !== 0) && (f2 = (this.wrap(t2[0] - c2) * this.kx * u2 + (t2[1] - l2) * this.ky * d2) / (u2 * u2 + d2 * d2), f2 > 1 ? (c2 = e49[s2 + 1][0], l2 = e49[s2 + 1][1]) : f2 > 0 && (c2 += u2 / this.kx * f2, l2 += d2 / this.ky * f2)), u2 = this.wrap(t2[0] - c2) * this.kx, d2 = (t2[1] - l2) * this.ky;
      let p2 = u2 * u2 + d2 * d2;
      p2 < n2 && (n2 = p2, r2 = c2, i2 = l2, a2 = s2, o2 = f2);
    }
    return { point: [r2, i2], index: a2, t: Math.max(0, Math.min(1, o2)) };
  }
  wrap(e49) {
    for (; e49 < -180; ) e49 += 360;
    for (; e49 > 180; ) e49 -= 360;
    return e49;
  }
};
function $n(e49, t2) {
  return t2[0] - e49[0];
}
function er(e49) {
  return e49[1] - e49[0] + 1;
}
function tr(e49, t2) {
  return e49[1] >= e49[0] && e49[1] < t2;
}
function nr(e49, t2) {
  if (e49[0] > e49[1]) return [null, null];
  let n2 = er(e49);
  if (t2) {
    if (n2 === 2) return [e49, null];
    let t3 = Math.floor(n2 / 2);
    return [[e49[0], e49[0] + t3], [e49[0] + t3, e49[1]]];
  }
  if (n2 === 1) return [e49, null];
  let r2 = Math.floor(n2 / 2) - 1;
  return [[e49[0], e49[0] + r2], [e49[0] + r2 + 1, e49[1]]];
}
function rr(e49, t2) {
  if (!tr(t2, e49.length)) return [1 / 0, 1 / 0, -1 / 0, -1 / 0];
  let n2 = [1 / 0, 1 / 0, -1 / 0, -1 / 0];
  for (let r2 = t2[0]; r2 <= t2[1]; ++r2) kn(n2, e49[r2]);
  return n2;
}
function ir(e49) {
  let t2 = [1 / 0, 1 / 0, -1 / 0, -1 / 0];
  for (let n2 of e49) for (let e50 of n2) kn(t2, e50);
  return t2;
}
function ar(e49) {
  return e49[0] !== -1 / 0 && e49[1] !== -1 / 0 && e49[2] !== 1 / 0 && e49[3] !== 1 / 0;
}
function or(e49, t2, n2) {
  if (!ar(e49) || !ar(t2)) return NaN;
  let r2 = 0, i2 = 0;
  return e49[2] < t2[0] && (r2 = t2[0] - e49[2]), e49[0] > t2[2] && (r2 = e49[0] - t2[2]), e49[1] > t2[3] && (i2 = e49[1] - t2[3]), e49[3] < t2[1] && (i2 = t2[1] - e49[3]), n2.distance([0, 0], [r2, i2]);
}
function sr(e49, t2, n2) {
  let r2 = n2.pointOnLine(t2, e49);
  return n2.distance(e49, r2.point);
}
function cr(e49, t2, n2, r2, i2) {
  let a2 = Math.min(sr(e49, [n2, r2], i2), sr(t2, [n2, r2], i2)), o2 = Math.min(sr(n2, [e49, t2], i2), sr(r2, [e49, t2], i2));
  return Math.min(a2, o2);
}
function lr(e49, t2, n2, r2, i2) {
  if (!(tr(t2, e49.length) && tr(r2, n2.length))) return 1 / 0;
  let a2 = 1 / 0;
  for (let o2 = t2[0]; o2 < t2[1]; ++o2) {
    let t3 = e49[o2], s2 = e49[o2 + 1];
    for (let e50 = r2[0]; e50 < r2[1]; ++e50) {
      let r3 = n2[e50], o3 = n2[e50 + 1];
      if (Nn(t3, s2, r3, o3)) return 0;
      a2 = Math.min(a2, cr(t3, s2, r3, o3, i2));
    }
  }
  return a2;
}
function ur(e49, t2, n2, r2, i2) {
  if (!(tr(t2, e49.length) && tr(r2, n2.length))) return NaN;
  let a2 = 1 / 0;
  for (let o2 = t2[0]; o2 <= t2[1]; ++o2) for (let t3 = r2[0]; t3 <= r2[1]; ++t3) if (a2 = Math.min(a2, i2.distance(e49[o2], n2[t3])), a2 === 0) return a2;
  return a2;
}
function dr(e49, t2, n2) {
  if (Fn(e49, t2, true)) return 0;
  let r2 = 1 / 0;
  for (let i2 of t2) {
    let t3 = i2[0], a2 = i2[i2.length - 1];
    if (t3 !== a2 && (r2 = Math.min(r2, sr(e49, [a2, t3], n2)), r2 === 0)) return r2;
    let o2 = n2.pointOnLine(i2, e49);
    if (r2 = Math.min(r2, n2.distance(e49, o2.point)), r2 === 0) return r2;
  }
  return r2;
}
function fr(e49, t2, n2, r2) {
  if (!tr(t2, e49.length)) return NaN;
  for (let r3 = t2[0]; r3 <= t2[1]; ++r3) if (Fn(e49[r3], n2, true)) return 0;
  let i2 = 1 / 0;
  for (let a2 = t2[0]; a2 < t2[1]; ++a2) {
    let t3 = e49[a2], o2 = e49[a2 + 1];
    for (let e50 of n2) for (let n3 = 0, a3 = e50.length, s2 = a3 - 1; n3 < a3; s2 = n3++) {
      let a4 = e50[s2], c2 = e50[n3];
      if (Nn(t3, o2, a4, c2)) return 0;
      i2 = Math.min(i2, cr(t3, o2, a4, c2, r2));
    }
  }
  return i2;
}
function pr(e49, t2) {
  for (let n2 of e49) for (let e50 of n2) if (Fn(e50, t2, true)) return true;
  return false;
}
function mr(e49, t2, n2, r2 = 1 / 0) {
  let i2 = ir(e49), a2 = ir(t2);
  if (r2 !== 1 / 0 && or(i2, a2, n2) >= r2) return r2;
  if (An(i2, a2)) {
    if (pr(e49, t2)) return 0;
  } else if (pr(t2, e49)) return 0;
  let o2 = 1 / 0;
  for (let r3 of e49) for (let e50 = 0, i3 = r3.length, a3 = i3 - 1; e50 < i3; a3 = e50++) {
    let i4 = r3[a3], s2 = r3[e50];
    for (let e51 of t2) for (let t3 = 0, r4 = e51.length, a4 = r4 - 1; t3 < r4; a4 = t3++) {
      let r5 = e51[a4], c2 = e51[t3];
      if (Nn(i4, s2, r5, c2)) return 0;
      o2 = Math.min(o2, cr(i4, s2, r5, c2, n2));
    }
  }
  return o2;
}
function hr(e49, t2, n2, r2, i2, a2) {
  if (!a2) return;
  let o2 = or(rr(r2, a2), i2, n2);
  o2 < t2 && e49.push([o2, a2, [0, 0]]);
}
function gr(e49, t2, n2, r2, i2, a2, o2) {
  if (!a2 || !o2) return;
  let s2 = or(rr(r2, a2), rr(i2, o2), n2);
  s2 < t2 && e49.push([s2, a2, o2]);
}
function _r(e49, t2, n2, r2, i2 = 1 / 0) {
  let a2 = Math.min(r2.distance(e49[0], n2[0][0]), i2);
  if (a2 === 0) return a2;
  let o2 = new Xn([[0, [0, e49.length - 1], [0, 0]]], $n), s2 = ir(n2);
  for (; o2.length > 0; ) {
    let i3 = o2.pop();
    if (i3[0] >= a2) continue;
    let c2 = i3[1], l2 = t2 ? 50 : 100;
    if (er(c2) <= l2) {
      if (!tr(c2, e49.length)) return NaN;
      if (t2) {
        let t3 = fr(e49, c2, n2, r2);
        if (isNaN(t3) || t3 === 0) return t3;
        a2 = Math.min(a2, t3);
      } else for (let t3 = c2[0]; t3 <= c2[1]; ++t3) {
        let i4 = dr(e49[t3], n2, r2);
        if (a2 = Math.min(a2, i4), a2 === 0) return 0;
      }
    } else {
      let n3 = nr(c2, t2);
      hr(o2, a2, r2, e49, s2, n3[0]), hr(o2, a2, r2, e49, s2, n3[1]);
    }
  }
  return a2;
}
function vr(e49, t2, n2, r2, i2, a2 = 1 / 0) {
  let o2 = Math.min(a2, i2.distance(e49[0], n2[0]));
  if (o2 === 0) return o2;
  let s2 = new Xn([[0, [0, e49.length - 1], [0, n2.length - 1]]], $n);
  for (; s2.length > 0; ) {
    let a3 = s2.pop();
    if (a3[0] >= o2) continue;
    let c2 = a3[1], l2 = a3[2], u2 = t2 ? 50 : 100, d2 = r2 ? 50 : 100;
    if (er(c2) <= u2 && er(l2) <= d2) {
      if (!tr(c2, e49.length) && tr(l2, n2.length)) return NaN;
      let a4;
      if (t2 && r2) a4 = lr(e49, c2, n2, l2, i2), o2 = Math.min(o2, a4);
      else if (t2 && !r2) {
        let t3 = e49.slice(c2[0], c2[1] + 1);
        for (let e50 = l2[0]; e50 <= l2[1]; ++e50) if (a4 = sr(n2[e50], t3, i2), o2 = Math.min(o2, a4), o2 === 0) return o2;
      } else if (!t2 && r2) {
        let t3 = n2.slice(l2[0], l2[1] + 1);
        for (let n3 = c2[0]; n3 <= c2[1]; ++n3) if (a4 = sr(e49[n3], t3, i2), o2 = Math.min(o2, a4), o2 === 0) return o2;
      } else a4 = ur(e49, c2, n2, l2, i2), o2 = Math.min(o2, a4);
    } else {
      let a4 = nr(c2, t2), u3 = nr(l2, r2);
      gr(s2, o2, i2, e49, n2, a4[0], u3[0]), gr(s2, o2, i2, e49, n2, a4[0], u3[1]), gr(s2, o2, i2, e49, n2, a4[1], u3[0]), gr(s2, o2, i2, e49, n2, a4[1], u3[1]);
    }
  }
  return o2;
}
function yr(e49, t2) {
  let n2 = e49.geometry(), r2 = n2.flat().map((t3) => wn([t3.x, t3.y], e49.canonical));
  if (n2.length === 0) return NaN;
  let i2 = new Qn(r2[0][1]), a2 = 1 / 0;
  for (let e50 of t2) {
    switch (e50.type) {
      case `Point`:
        a2 = Math.min(a2, vr(r2, false, [e50.coordinates], false, i2, a2));
        break;
      case `LineString`:
        a2 = Math.min(a2, vr(r2, false, e50.coordinates, true, i2, a2));
        break;
      case `Polygon`:
        a2 = Math.min(a2, _r(r2, false, e50.coordinates, i2, a2));
    }
    if (a2 === 0) return a2;
  }
  return a2;
}
function br(e49, t2) {
  let n2 = e49.geometry(), r2 = n2.flat().map((t3) => wn([t3.x, t3.y], e49.canonical));
  if (n2.length === 0) return NaN;
  let i2 = new Qn(r2[0][1]), a2 = 1 / 0;
  for (let e50 of t2) {
    switch (e50.type) {
      case `Point`:
        a2 = Math.min(a2, vr(r2, true, [e50.coordinates], false, i2, a2));
        break;
      case `LineString`:
        a2 = Math.min(a2, vr(r2, true, e50.coordinates, true, i2, a2));
        break;
      case `Polygon`:
        a2 = Math.min(a2, _r(r2, true, e50.coordinates, i2, a2));
    }
    if (a2 === 0) return a2;
  }
  return a2;
}
function xr(e49, t2) {
  let n2 = e49.geometry();
  if (n2.length === 0 || n2[0].length === 0) return NaN;
  let r2 = Ft(n2, 0).map((t3) => t3.map((t4) => t4.map((t5) => wn([t5.x, t5.y], e49.canonical)))), i2 = new Qn(r2[0][0][0][1]), a2 = 1 / 0;
  for (let e50 of t2) for (let t3 of r2) {
    switch (e50.type) {
      case `Point`:
        a2 = Math.min(a2, _r([e50.coordinates], false, t3, i2, a2));
        break;
      case `LineString`:
        a2 = Math.min(a2, _r(e50.coordinates, true, t3, i2, a2));
        break;
      case `Polygon`:
        a2 = Math.min(a2, mr(t3, e50.coordinates, i2, a2));
    }
    if (a2 === 0) return a2;
  }
  return a2;
}
function Sr(e49) {
  return e49.type === `MultiPolygon` ? e49.coordinates.map((e50) => ({ type: `Polygon`, coordinates: e50 })) : e49.type === `MultiLineString` ? e49.coordinates.map((e50) => ({ type: `LineString`, coordinates: e50 })) : e49.type === `MultiPoint` ? e49.coordinates.map((e50) => ({ type: `Point`, coordinates: e50 })) : [e49];
}
var Cr = class e30 {
  constructor(e49, t2) {
    this.type = F, this.geojson = e49, this.geometries = t2;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`'distance' expression requires exactly one argument, but found ${t2.length - 1} instead.`);
    if (bt(t2[1])) {
      let n3 = t2[1];
      if (n3.type === `FeatureCollection`) return new e30(n3, n3.features.map((e49) => Sr(e49.geometry)).flat());
      if (n3.type === `Feature`) return new e30(n3, Sr(n3.geometry));
      if (`type` in n3 && `coordinates` in n3) return new e30(n3, Sr(n3));
    }
    return n2.error(`'distance' expression requires valid geojson object that contains polygon geometry type.`);
  }
  evaluate(e49) {
    if (e49.geometry() != null && e49.canonicalID() != null) {
      if (e49.geometryType() === `Point`) return yr(e49, this.geometries);
      if (e49.geometryType() === `LineString`) return br(e49, this.geometries);
      if (e49.geometryType() === `Polygon`) return xr(e49, this.geometries);
    }
    return NaN;
  }
  eachChild() {
  }
  outputDefined() {
    return true;
  }
}, wr = class e31 {
  constructor(e49) {
    let t2 = null;
    for (let n2 of e49) if (!t2) t2 = n2.type;
    else if (t2 === n2.type) continue;
    else {
      t2 = R;
      break;
    }
    this.type = ke(t2 ?? R, e49.length), this.arr = e49;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`'semiliteral' expression requires exactly one argument, but found ${t2.length - 1} instead.`);
    if (!bt(t2[1])) return n2.error(`invalid value of type "${typeof t2[1]}"`);
    let r2 = t2[1], i2 = U(r2);
    if (i2.kind === `array`) {
      let t3 = r2, i3 = n2.concat(1), a2 = [];
      for (let e49 = 0; e49 < t3.length; e49++) {
        let n3 = i3.parse(t3[e49], e49, R);
        if (!n3) return null;
        a2.push(n3);
      }
      return new e31(a2);
    }
    return new St(i2, r2);
  }
  evaluate(e49) {
    return this.arr.map((t2) => t2.evaluate(e49));
  }
  eachChild(e49) {
    this.arr.forEach(e49);
  }
  outputDefined() {
    return this.arr.every((e49) => e49.outputDefined());
  }
}, Tr = class e32 {
  constructor(e49) {
    this.key = e49, this.type = R;
  }
  static parse(t2, n2) {
    if (t2.length !== 2) return n2.error(`Expected 1 argument, but found ${t2.length - 1} instead.`);
    let r2 = t2[1];
    return r2 == null ? n2.error(`Global state property must be defined.`) : typeof r2 == `string` ? new e32(r2) : n2.error(`Global state property must be string, but found ${typeof t2[1]} instead.`);
  }
  evaluate(e49) {
    var _a3;
    let t2 = (_a3 = e49.globals) == null ? void 0 : _a3.globalState;
    return !t2 || Object.keys(t2).length === 0 ? null : Qe(t2, this.key) ?? null;
  }
  eachChild() {
  }
  outputDefined() {
    return false;
  }
};
const Er = { "==": fn, "!=": pn, ">": hn, "<": mn, ">=": _n, "<=": gn, array: zt, at: Wt, boolean: zt, case: Jt, coalesce: Xt, collator: vn, format: jt, image: bn, in: Gt, "index-of": Kt, interpolate: Ot, "interpolate-hcl": Ot, "interpolate-lab": Ot, length: xn, let: Ht, literal: St, match: qt, number: zt, "number-format": yn, object: zt, semiliteral: wr, slice: Yt, step: Et, string: zt, "to-boolean": Vt, "to-color": Vt, "to-number": Vt, "to-string": Vt, var: Ut, within: Yn, distance: Cr, "global-state": Tr };
var Dr = class extends Error {
  constructor(e49, t2) {
    super(t2), this.message = t2, this.key = e49;
  }
}, Or = class e33 {
  constructor(e49, t2 = []) {
    this.parent = e49, this.bindings = {};
    for (let [e50, n2] of t2) this.bindings[e50] = n2;
  }
  concat(t2) {
    return new e33(this, t2);
  }
  get(e49) {
    if (this.bindings[e49]) return this.bindings[e49];
    if (this.parent) return this.parent.get(e49);
    throw Error(`${e49} not found in scope.`);
  }
  has(e49) {
    return this.bindings[e49] ? true : this.parent ? this.parent.has(e49) : false;
  }
}, kr = class e34 {
  constructor(e49, t2, n2 = [], r2, i2 = new Or(), a2 = []) {
    this.registry = e49, this.path = n2, this.key = n2.map((e50) => `[${e50}]`).join(``), this.scope = i2, this.errors = a2, this.expectedType = r2, this._isConstant = t2;
  }
  parse(e49, t2, n2, r2, i2 = {}) {
    return t2 == null ? this._parse(e49, i2) : this.concat(t2, n2, r2)._parse(e49, i2);
  }
  _parse(e49, t2) {
    (e49 === null || typeof e49 == `string` || typeof e49 == `boolean` || typeof e49 == `number`) && (e49 = [`literal`, e49]);
    let n2 = this.key;
    function r2(e50, t3, r3) {
      return r3 === `assert` ? new zt(t3, [e50], n2) : r3 === `coerce` ? new Vt(t3, [e50], n2) : e50;
    }
    if (Array.isArray(e49)) {
      if (e49.length === 0) return this.error(`Expected an array with at least one element. If you wanted a literal array, use ["literal", []].`);
      let n3 = e49[0];
      if (typeof n3 != `string`) return this.error(`Expression name must be a string, but found ${typeof n3} instead. If you wanted a literal array, use ["literal", [...]].`, 0), null;
      let i2 = this.registry[n3];
      if (i2) {
        let n4 = i2.parse(e49, this);
        if (!n4) return null;
        if (this.expectedType) {
          let e50 = this.expectedType, i3 = n4.type;
          if ((e50.kind === `string` || e50.kind === `number` || e50.kind === `boolean` || e50.kind === `object` || e50.kind === `array`) && i3.kind === `value`) n4 = r2(n4, e50, t2.typeAnnotation || `assert`);
          else if (e50.kind === `projectionDefinition` && [`string`, `array`, `value`].includes(i3.kind) || [`color`, `formatted`, `resolvedImage`].includes(e50.kind) && [`value`, `string`].includes(i3.kind) || [`padding`, `numberArray`].includes(e50.kind) && [`value`, `number`, `array`].includes(i3.kind) || e50.kind === `colorArray` && [`value`, `string`, `array`].includes(i3.kind) || e50.kind === `variableAnchorOffsetCollection` && [`value`, `array`].includes(i3.kind)) n4 = r2(n4, e50, t2.typeAnnotation || `coerce`);
          else if (this.checkSubtype(e50, i3)) return null;
        }
        if (!(n4 instanceof St) && n4.type.kind !== `resolvedImage` && this._isConstant(n4)) {
          let e50 = new wt();
          try {
            n4 = new St(n4.type, n4.evaluate(e50));
          } catch (e51) {
            return this.error(e51.message), null;
          }
        }
        return n4;
      }
      return this.error(`Unknown expression "${n3}". If you wanted a literal array, use ["literal", [...]].`, 0);
    }
    return e49 === void 0 ? this.error(`'undefined' value invalid. Use null instead.`) : typeof e49 == `object` ? this.error(`Bare objects invalid. Use ["literal", {...}] instead.`) : this.error(`Expected an array, but found ${typeof e49} instead.`);
  }
  concat(t2, n2, r2) {
    let i2 = typeof t2 == `number` ? this.path.concat(t2) : this.path, a2 = r2 ? this.scope.concat(r2) : this.scope;
    return new e34(this.registry, this._isConstant, i2, n2 || null, a2, this.errors);
  }
  error(e49, ...t2) {
    let n2 = `${this.key}${t2.map((e50) => `[${e50}]`).join(``)}`;
    this.errors.push(new Dr(n2, e49));
  }
  checkSubtype(e49, t2) {
    let n2 = je(e49, t2);
    return n2 && this.error(n2), n2;
  }
}, Ar = class e35 {
  constructor(e49, t2, n2, r2, i2) {
    this.name = e49, this.type = t2, this._evaluate = n2, this.args = r2, this.key = i2;
  }
  evaluate(e49) {
    return this._evaluate(e49, this.args, this.key);
  }
  eachChild(e49) {
    this.args.forEach(e49);
  }
  outputDefined() {
    return false;
  }
  static parse(t2, n2) {
    let r2 = t2[0], i2 = e35.definitions[r2];
    if (!i2) return n2.error(`Unknown expression "${r2}". If you wanted a literal array, use ["literal", [...]].`, 0);
    let a2 = Array.isArray(i2) ? i2[0] : i2.type, o2 = Array.isArray(i2) ? [[i2[1], i2[2]]] : i2.overloads, s2 = o2.filter(([e49]) => !Array.isArray(e49) || e49.length === t2.length - 1), c2 = null;
    for (let [i3, o3] of s2) {
      c2 = new kr(n2.registry, Lr, n2.path, null, n2.scope);
      let s3 = [], l2 = false;
      for (let e49 = 1; e49 < t2.length; e49++) {
        let n3 = t2[e49], r3 = Array.isArray(i3) ? i3[e49 - 1] : i3.type, a3 = c2.parse(n3, 1 + s3.length, r3);
        if (!a3) {
          l2 = true;
          break;
        }
        s3.push(a3);
      }
      if (!l2) {
        if (Array.isArray(i3) && i3.length !== s3.length) {
          c2.error(`Expected ${i3.length} arguments, but found ${s3.length} instead.`);
          continue;
        }
        for (let e49 = 0; e49 < s3.length; e49++) {
          let t3 = Array.isArray(i3) ? i3[e49] : i3.type, n3 = s3[e49];
          c2.concat(e49 + 1).checkSubtype(t3, n3.type);
        }
        if (c2.errors.length === 0) return new e35(r2, a2, o3, s3, n2.key);
      }
    }
    if (s2.length === 1) n2.errors.push(...c2.errors);
    else {
      let e49 = (s2.length ? s2 : o2).map(([e50]) => Ir(e50)).join(` | `), r3 = [];
      for (let e50 = 1; e50 < t2.length; e50++) {
        let i3 = n2.parse(t2[e50], 1 + r3.length);
        if (!i3) return null;
        r3.push(z(i3.type));
      }
      n2.error(`Expected arguments of type ${e49}, but found (${r3.join(`, `)}) instead.`);
    }
    return null;
  }
  static register(t2, n2) {
    e35.definitions = n2;
    for (let r2 in n2) t2[r2] = e35;
  }
};
function jr(e49, [t2, n2, r2, i2], a2) {
  t2 = t2.evaluate(e49), n2 = n2.evaluate(e49), r2 = r2.evaluate(e49);
  let o2 = i2 ? i2.evaluate(e49) : 1, s2 = yt(t2, n2, r2, o2);
  if (s2) throw new H(s2, a2);
  return new V(t2 / 255, n2 / 255, r2 / 255, o2, false);
}
function Mr(e49, t2) {
  return e49 in t2 && t2[e49] !== void 0;
}
function Nr(e49, t2) {
  let n2 = t2[e49];
  return n2 === void 0 ? null : n2;
}
function Pr(e49, t2, n2, r2) {
  for (; n2 <= r2; ) {
    let i2 = n2 + r2 >> 1;
    if (t2[i2] === e49) return true;
    t2[i2] > e49 ? r2 = i2 - 1 : n2 = i2 + 1;
  }
  return false;
}
function Fr(e49) {
  return { type: e49 };
}
Ar.register(Er, { error: [xe, [I], (e49, [t2], n2) => {
  throw new H(t2.evaluate(e49), n2);
}], typeof: [I, [R], (e49, [t2]) => z(U(t2.evaluate(e49)))], "to-rgba": [ke(F, 4), [ve], (e49, [t2]) => {
  let [n2, r2, i2, a2] = t2.evaluate(e49).rgb;
  return [n2 * 255, r2 * 255, i2 * 255, a2];
}], rgb: [ve, [F, F, F], jr], rgba: [ve, [F, F, F, F], jr], has: { type: L, overloads: [[[I], (e49, [t2]) => Mr(t2.evaluate(e49), e49.properties())], [[I, be], (e49, [t2, n2]) => Mr(t2.evaluate(e49), n2.evaluate(e49))]] }, get: { type: R, overloads: [[[I], (e49, [t2]) => Nr(t2.evaluate(e49), e49.properties())], [[I, be], (e49, [t2, n2]) => Nr(t2.evaluate(e49), n2.evaluate(e49))]] }, "feature-state": [R, [I], (e49, [t2]) => Nr(t2.evaluate(e49), e49.featureState || {})], properties: [be, [], (e49) => e49.properties()], "geometry-type": [I, [], (e49) => e49.geometryType()], id: [R, [], (e49) => e49.id()], zoom: [F, [], (e49) => e49.globals.zoom], "heatmap-density": [F, [], (e49) => e49.globals.heatmapDensity || 0], elevation: [F, [], (e49) => e49.globals.elevation || 0], "line-progress": [F, [], (e49) => e49.globals.lineProgress || 0], accumulated: [R, [], (e49) => e49.globals.accumulated === void 0 ? null : e49.globals.accumulated], "+": [F, Fr(F), (e49, t2) => {
  let n2 = 0;
  for (let r2 of t2) n2 += r2.evaluate(e49);
  return n2;
}], "*": [F, Fr(F), (e49, t2) => {
  let n2 = 1;
  for (let r2 of t2) n2 *= r2.evaluate(e49);
  return n2;
}], "-": { type: F, overloads: [[[F, F], (e49, [t2, n2]) => t2.evaluate(e49) - n2.evaluate(e49)], [[F], (e49, [t2]) => -t2.evaluate(e49)]] }, "/": [F, [F, F], (e49, [t2, n2]) => t2.evaluate(e49) / n2.evaluate(e49)], "%": [F, [F, F], (e49, [t2, n2]) => t2.evaluate(e49) % n2.evaluate(e49)], ln2: [F, [], () => Math.LN2], pi: [F, [], () => Math.PI], e: [F, [], () => Math.E], "^": [F, [F, F], (e49, [t2, n2]) => t2.evaluate(e49) ** +n2.evaluate(e49)], sqrt: [F, [F], (e49, [t2]) => Math.sqrt(t2.evaluate(e49))], log10: [F, [F], (e49, [t2]) => Math.log(t2.evaluate(e49)) / Math.LN10], ln: [F, [F], (e49, [t2]) => Math.log(t2.evaluate(e49))], log2: [F, [F], (e49, [t2]) => Math.log(t2.evaluate(e49)) / Math.LN2], sin: [F, [F], (e49, [t2]) => Math.sin(t2.evaluate(e49))], cos: [F, [F], (e49, [t2]) => Math.cos(t2.evaluate(e49))], tan: [F, [F], (e49, [t2]) => Math.tan(t2.evaluate(e49))], asin: [F, [F], (e49, [t2]) => Math.asin(t2.evaluate(e49))], acos: [F, [F], (e49, [t2]) => Math.acos(t2.evaluate(e49))], atan: [F, [F], (e49, [t2]) => Math.atan(t2.evaluate(e49))], min: [F, Fr(F), (e49, t2) => Math.min(...t2.map((t3) => t3.evaluate(e49)))], max: [F, Fr(F), (e49, t2) => Math.max(...t2.map((t3) => t3.evaluate(e49)))], abs: [F, [F], (e49, [t2]) => Math.abs(t2.evaluate(e49))], round: [F, [F], (e49, [t2]) => {
  let n2 = t2.evaluate(e49);
  return n2 < 0 ? -Math.round(-n2) : Math.round(n2);
}], floor: [F, [F], (e49, [t2]) => Math.floor(t2.evaluate(e49))], ceil: [F, [F], (e49, [t2]) => Math.ceil(t2.evaluate(e49))], "filter-==": [L, [I, R], (e49, [t2, n2]) => e49.properties()[t2.value] === n2.value], "filter-id-==": [L, [R], (e49, [t2]) => e49.id() === t2.value], "filter-type-==": [L, [I], (e49, [t2]) => e49.geometryType() === t2.value], "filter-<": [L, [I, R], (e49, [t2, n2]) => {
  let r2 = e49.properties()[t2.value], i2 = n2.value;
  return typeof r2 == typeof i2 && r2 < i2;
}], "filter-id-<": [L, [R], (e49, [t2]) => {
  let n2 = e49.id(), r2 = t2.value;
  return typeof n2 == typeof r2 && n2 < r2;
}], "filter->": [L, [I, R], (e49, [t2, n2]) => {
  let r2 = e49.properties()[t2.value], i2 = n2.value;
  return typeof r2 == typeof i2 && r2 > i2;
}], "filter-id->": [L, [R], (e49, [t2]) => {
  let n2 = e49.id(), r2 = t2.value;
  return typeof n2 == typeof r2 && n2 > r2;
}], "filter-<=": [L, [I, R], (e49, [t2, n2]) => {
  let r2 = e49.properties()[t2.value], i2 = n2.value;
  return typeof r2 == typeof i2 && r2 <= i2;
}], "filter-id-<=": [L, [R], (e49, [t2]) => {
  let n2 = e49.id(), r2 = t2.value;
  return typeof n2 == typeof r2 && n2 <= r2;
}], "filter->=": [L, [I, R], (e49, [t2, n2]) => {
  let r2 = e49.properties()[t2.value], i2 = n2.value;
  return typeof r2 == typeof i2 && r2 >= i2;
}], "filter-id->=": [L, [R], (e49, [t2]) => {
  let n2 = e49.id(), r2 = t2.value;
  return typeof n2 == typeof r2 && n2 >= r2;
}], "filter-has": [L, [R], (e49, [t2]) => {
  let n2 = t2.value, r2 = e49.properties();
  return n2 in r2 && r2[n2] !== void 0;
}], "filter-has-id": [L, [], (e49) => e49.id() !== null && e49.id() !== void 0], "filter-type-in": [L, [ke(I)], (e49, [t2]) => t2.value.indexOf(e49.geometryType()) >= 0], "filter-id-in": [L, [ke(R)], (e49, [t2]) => t2.value.indexOf(e49.id()) >= 0], "filter-in-small": [L, [I, ke(R)], (e49, [t2, n2]) => n2.value.indexOf(e49.properties()[t2.value]) >= 0], "filter-in-large": [L, [I, ke(R)], (e49, [t2, n2]) => Pr(e49.properties()[t2.value], n2.value, 0, n2.value.length - 1)], all: { type: L, overloads: [[[L, L], (e49, [t2, n2]) => t2.evaluate(e49) && n2.evaluate(e49)], [Fr(L), (e49, t2) => {
  for (let n2 of t2) if (!n2.evaluate(e49)) return false;
  return true;
}]] }, any: { type: L, overloads: [[[L, L], (e49, [t2, n2]) => t2.evaluate(e49) || n2.evaluate(e49)], [Fr(L), (e49, t2) => {
  for (let n2 of t2) if (n2.evaluate(e49)) return true;
  return false;
}]] }, "!": [L, [L], (e49, [t2]) => !t2.evaluate(e49)], "is-supported-script": [L, [I], (e49, [t2]) => {
  let n2 = e49.globals && e49.globals.isSupportedScript;
  return !n2 || n2(t2.evaluate(e49));
}], upcase: [I, [I], (e49, [t2]) => t2.evaluate(e49).toUpperCase()], downcase: [I, [I], (e49, [t2]) => t2.evaluate(e49).toLowerCase()], concat: [I, Fr(R), (e49, t2) => t2.map((t3) => xt(t3.evaluate(e49))).join(``)], split: [ke(I), [I, I], (e49, [t2, n2]) => t2.evaluate(e49).split(n2.evaluate(e49))], join: [I, [ke(I), I], (e49, [t2, n2]) => t2.evaluate(e49).join(n2.evaluate(e49))], "resolved-locale": [I, [Se], (e49, [t2]) => t2.evaluate(e49).resolvedLocale()] });
function Ir(e49) {
  return Array.isArray(e49) ? `(${e49.map(z).join(`, `)})` : `(${z(e49.type)}...)`;
}
function Lr(e49) {
  if (e49 instanceof Ut) return Lr(e49.boundExpression);
  if (e49 instanceof Ar && e49.name === `error` || e49 instanceof vn || e49 instanceof Yn || e49 instanceof Cr || e49 instanceof Tr) return false;
  let t2 = e49 instanceof Vt || e49 instanceof zt, n2 = true;
  return e49.eachChild((e50) => {
    n2 &&= t2 ? Lr(e50) : e50 instanceof St;
  }), n2 ? Rr(e49) && Br(e49, [`zoom`, `heatmap-density`, `elevation`, `line-progress`, `accumulated`, `is-supported-script`]) : false;
}
function Rr(e49) {
  if (e49 instanceof Ar && (e49.name === `get` && e49.args.length === 1 || e49.name === `feature-state` || e49.name === `has` && e49.args.length === 1 || e49.name === `properties` || e49.name === `geometry-type` || e49.name === `id` || /^filter-/.test(e49.name)) || e49 instanceof Yn || e49 instanceof Cr) return false;
  let t2 = true;
  return e49.eachChild((e50) => {
    t2 && !Rr(e50) && (t2 = false);
  }), t2;
}
function zr(e49) {
  if (e49 instanceof Ar && e49.name === `feature-state`) return false;
  let t2 = true;
  return e49.eachChild((e50) => {
    t2 && !zr(e50) && (t2 = false);
  }), t2;
}
function Br(e49, t2) {
  if (e49 instanceof Ar && t2.indexOf(e49.name) >= 0) return false;
  let n2 = true;
  return e49.eachChild((e50) => {
    n2 && !Br(e50, t2) && (n2 = false);
  }), n2;
}
function Vr(e49) {
  return e49[`property-type`] === `data-driven` || e49[`property-type`] === `cross-faded-data-driven`;
}
function Hr(e49) {
  return !!e49.expression && e49.expression.parameters.indexOf(`zoom`) > -1;
}
function Ur(e49) {
  return !!e49.expression && e49.expression.interpolated;
}
const Wr = /^(.*)-transition$/;
function Gr(e49, ...t2) {
  for (let n2 of t2) for (let t3 in n2) e49[t3] = n2[t3];
  return e49;
}
function W(e49) {
  return e49 instanceof Number ? `number` : e49 instanceof String ? `string` : e49 instanceof Boolean ? `boolean` : Array.isArray(e49) ? `array` : e49 === null ? `null` : typeof e49;
}
function Kr(e49) {
  return typeof e49 == `object` && !!e49 && !Array.isArray(e49) && U(e49) === be;
}
function qr(e49) {
  return e49;
}
function Jr(e49) {
  switch (e49.type) {
    case `color`:
      return V.parse;
    case `padding`:
      return dt.parse;
    case `numberArray`:
      return ft.parse;
    case `colorArray`:
      return pt.parse;
    default:
      return null;
  }
}
function Yr(e49) {
  switch (e49) {
    case `exponential`:
      return ei;
    case `interval`:
      return $r;
    case `categorical`:
      return Qr;
    case `identity`:
      return ti;
    default:
      throw Error(`Unknown function type "${e49}"`);
  }
}
function Xr(e49, t2) {
  let n2 = e49.stops && typeof e49.stops[0][0] == `object`, r2 = n2 || e49.property !== void 0, i2 = n2 || !r2, a2 = e49.type || (Ur(t2) ? `exponential` : `interval`), o2 = Jr(t2);
  if (o2 && (e49 = Gr({}, e49), e49.stops && (e49.stops = e49.stops.map((e50) => [e50[0], o2(e50[1])])), e49.default ? e49.default = o2(e49.default) : e49.default = o2(t2.default)), e49.colorSpace && !st(e49.colorSpace)) throw Error(`Unknown color space: "${e49.colorSpace}"`);
  let s2 = Yr(a2), c2, l2;
  if (a2 === `categorical`) {
    c2 = /* @__PURE__ */ Object.create(null);
    for (let t3 of e49.stops) c2[t3[0]] = t3[1];
    l2 = typeof e49.stops[0][0];
  }
  if (n2) {
    let n3 = {}, r3 = [];
    for (let t3 = 0; t3 < e49.stops.length; t3++) {
      let i4 = e49.stops[t3], a4 = i4[0].zoom;
      n3[a4] === void 0 && (n3[a4] = { zoom: a4, type: e49.type, property: e49.property, default: e49.default, stops: [] }, r3.push(a4)), n3[a4].stops.push([i4[0].value, i4[1]]);
    }
    let i3 = [];
    for (let e50 of r3) i3.push([n3[e50].zoom, Xr(n3[e50], t2)]);
    let a3 = { name: `linear` };
    return { kind: `composite`, interpolationType: a3, interpolationFactor: Ot.interpolationFactor.bind(void 0, a3), zoomStops: i3.map((e50) => e50[0]), evaluate({ zoom: n4 }, r4) {
      return ei({ stops: i3, base: e49.base }, t2, n4).evaluate(n4, r4);
    } };
  }
  if (i2) {
    let n3 = a2 === `exponential` ? { name: `exponential`, base: e49.base === void 0 ? 1 : e49.base } : null;
    return { kind: `camera`, interpolationType: n3, interpolationFactor: Ot.interpolationFactor.bind(void 0, n3), zoomStops: e49.stops.map((e50) => e50[0]), evaluate: ({ zoom: n4 }) => s2(e49, t2, n4, c2, l2) };
  }
  return { kind: `source`, evaluate(n3, r3) {
    let i3 = r3 && r3.properties ? r3.properties[e49.property] : void 0;
    return i3 === void 0 ? Zr(e49.default, t2.default) : s2(e49, t2, i3, c2, l2);
  } };
}
function Zr(e49, t2, n2) {
  if (e49 !== void 0) return e49;
  if (t2 !== void 0) return t2;
  if (n2 !== void 0) return n2;
}
function Qr(e49, t2, n2, r2, i2) {
  return Zr(typeof n2 === i2 ? r2[n2] : void 0, e49.default, t2.default);
}
function $r(e49, t2, n2) {
  if (W(n2) !== `number`) return Zr(e49.default, t2.default);
  let r2 = e49.stops.length;
  if (r2 === 1 || n2 <= e49.stops[0][0]) return e49.stops[0][1];
  if (n2 >= e49.stops[r2 - 1][0]) return e49.stops[r2 - 1][1];
  let i2 = Tt(e49.stops.map((e50) => e50[0]), n2, ``);
  return e49.stops[i2][1];
}
function ei(e49, t2, n2) {
  let r2 = e49.base === void 0 ? 1 : e49.base;
  if (W(n2) !== `number`) return Zr(e49.default, t2.default);
  let i2 = e49.stops.length;
  if (i2 === 1 || n2 <= e49.stops[0][0]) return e49.stops[0][1];
  if (n2 >= e49.stops[i2 - 1][0]) return e49.stops[i2 - 1][1];
  let a2 = Tt(e49.stops.map((e50) => e50[0]), n2, ``), o2 = ni(n2, r2, e49.stops[a2][0], e49.stops[a2 + 1][0]), s2 = e49.stops[a2][1], c2 = e49.stops[a2 + 1][1], l2 = At[t2.type] || qr;
  return typeof s2.evaluate == `function` ? { evaluate(...t3) {
    let n3 = s2.evaluate.apply(void 0, t3), r3 = c2.evaluate.apply(void 0, t3);
    if (n3 !== void 0 && r3 !== void 0) return l2(n3, r3, o2, e49.colorSpace);
  } } : l2(s2, c2, o2, e49.colorSpace);
}
function ti(e49, t2, n2) {
  switch (t2.type) {
    case `color`:
      n2 = V.parse(n2);
      break;
    case `formatted`:
      n2 = ut.fromString(n2.toString());
      break;
    case `resolvedImage`:
      n2 = gt.fromString(n2.toString());
      break;
    case `padding`:
      n2 = dt.parse(n2);
      break;
    case `colorArray`:
      n2 = pt.parse(n2);
      break;
    case `numberArray`:
      n2 = ft.parse(n2);
      break;
    default:
      W(n2) !== t2.type && (t2.type !== `enum` || !t2.values[n2]) && (n2 = void 0);
  }
  return Zr(n2, e49.default, t2.default);
}
function ni(e49, t2, n2, r2) {
  let i2 = r2 - n2, a2 = e49 - n2;
  return i2 === 0 ? 0 : t2 === 1 ? a2 / i2 : (t2 ** +a2 - 1) / (t2 ** +i2 - 1);
}
function ri(e49) {
  return { result: `success`, value: e49 };
}
function ii(e49) {
  return { result: `error`, value: e49 };
}
var ai = class {
  constructor(e49, t2, n2, r2) {
    this.expression = e49, this._warningHistory = {}, this._evaluator = new wt(), this._defaultValue = n2 ? yi(n2) : null, this._enumValues = n2 && n2.type === `enum` ? n2.values : null, this._globalState = r2, this._rootKey = t2;
  }
  evaluateWithoutErrorHandling(e49, t2, n2, r2, i2, a2) {
    return this._globalState && (e49 = bi(e49, this._globalState)), this._evaluator.globals = e49, this._evaluator.feature = t2, this._evaluator.featureState = n2, this._evaluator.canonical = r2, this._evaluator.availableImages = i2 || null, this._evaluator.formattedSection = a2, this.expression.evaluate(this._evaluator);
  }
  evaluate(e49, t2, n2, r2, i2, a2) {
    this._globalState && (e49 = bi(e49, this._globalState)), this._evaluator.globals = e49, this._evaluator.feature = t2 || null, this._evaluator.featureState = n2 || null, this._evaluator.canonical = r2, this._evaluator.availableImages = i2 || null, this._evaluator.formattedSection = a2 || null;
    try {
      let e50 = this.expression.evaluate(this._evaluator);
      if (e50 == null || typeof e50 == `number` && e50 !== e50) return this._defaultValue;
      if (this._enumValues && !(e50 in this._enumValues)) throw new H(`Expected value to be one of ${Object.keys(this._enumValues).map((e51) => JSON.stringify(e51)).join(`, `)}, but found ${JSON.stringify(e50)} instead.`, ``);
      return e50;
    } catch (e50) {
      let t3 = e50 instanceof H ? e50.path : ``, n3 = `${t3}|${e50.message}`;
      return this._warningHistory[n3] || (this._warningHistory[n3] = true, typeof console < `u` && console.warn(oi(this._rootKey, t3, e50.message, this._defaultValue))), this._defaultValue;
    }
  }
};
function oi(e49, t2, n2, r2) {
  return `${e49}${t2}: ${n2}${r2 == null ? `` : ` Falling back to ${String(r2)}.`}`;
}
function si(e49) {
  if (!e49) throw Error(`rootKey must identify the location of the expression in the style JSON, e.g. "layers[3].paint.line-width".`);
}
function ci(e49) {
  return Array.isArray(e49) && e49.length > 0 && typeof e49[0] == `string` && e49[0] in Er;
}
function li(e49, t2, n2, r2) {
  si(t2);
  let i2 = new kr(Er, Lr, [], n2 ? vi(n2) : void 0), a2 = i2.parse(e49, void 0, void 0, void 0, n2 && n2.type === `string` ? { typeAnnotation: `coerce` } : void 0);
  return a2 ? ri(new ai(a2, t2, n2, r2)) : ii(i2.errors);
}
var ui = class {
  constructor(e49, t2, n2) {
    this.kind = e49, this._styleExpression = t2, this.isStateDependent = e49 !== `constant` && !zr(t2.expression), this.globalStateRefs = _i(t2.expression), this._globalState = n2;
  }
  evaluateWithoutErrorHandling(e49, t2, n2, r2, i2, a2) {
    return this._globalState && (e49 = bi(e49, this._globalState)), this._styleExpression.evaluateWithoutErrorHandling(e49, t2, n2, r2, i2, a2);
  }
  evaluate(e49, t2, n2, r2, i2, a2) {
    return this._globalState && (e49 = bi(e49, this._globalState)), this._styleExpression.evaluate(e49, t2, n2, r2, i2, a2);
  }
}, di = class {
  constructor(e49, t2, n2, r2, i2) {
    this.kind = e49, this.zoomStops = n2, this._styleExpression = t2, this.isStateDependent = e49 !== `camera` && !zr(t2.expression), this.globalStateRefs = _i(t2.expression), this.interpolationType = r2, this._globalState = i2;
  }
  evaluateWithoutErrorHandling(e49, t2, n2, r2, i2, a2) {
    return this._globalState && (e49 = bi(e49, this._globalState)), this._styleExpression.evaluateWithoutErrorHandling(e49, t2, n2, r2, i2, a2);
  }
  evaluate(e49, t2, n2, r2, i2, a2) {
    return this._globalState && (e49 = bi(e49, this._globalState)), this._styleExpression.evaluate(e49, t2, n2, r2, i2, a2);
  }
  interpolationFactor(e49, t2, n2) {
    return this.interpolationType ? Ot.interpolationFactor(this.interpolationType, e49, t2, n2) : 0;
  }
};
function fi(e49) {
  return e49._styleExpression !== void 0;
}
function pi(e49, t2, n2, r2) {
  let i2 = li(e49, t2, n2, r2);
  if (i2.result === `error`) return i2;
  let a2 = i2.value.expression, o2 = Rr(a2);
  if (!o2 && !Vr(n2)) return ii([new Dr(``, `data expressions not supported`)]);
  let s2 = Br(a2, [`zoom`]);
  if (!s2 && !Hr(n2)) return ii([new Dr(``, `zoom expressions not supported`)]);
  let c2 = gi(a2);
  if (!c2 && !s2) return ii([new Dr(``, `"zoom" expression may only be used as input to a top-level "step" or "interpolate" expression.`)]);
  if (c2 instanceof Dr) return ii([c2]);
  if (c2 instanceof Ot && !Ur(n2)) return ii([new Dr(``, `"interpolate" expressions cannot be used with this property`)]);
  if (!c2) return ri(o2 ? new ui(`constant`, i2.value, r2) : new ui(`source`, i2.value, r2));
  let l2 = c2 instanceof Ot ? c2.interpolation : void 0;
  return ri(o2 ? new di(`camera`, i2.value, c2.labels, l2, r2) : new di(`composite`, i2.value, c2.labels, l2, r2));
}
var mi = class e36 {
  constructor(e49, t2, n2) {
    this.isStateDependent = false, this.globalStateRefs = /* @__PURE__ */ new Set(), this._globalState = null, si(t2), this._parameters = e49, this._specification = n2, this._rootKey = t2, this._defaultValue = yi(n2), this._warningHistory = {};
    let r2 = Xr(this._parameters, this._specification);
    this.kind = r2.kind, this.interpolationFactor = r2.interpolationFactor, this.zoomStops = r2.zoomStops, this.interpolationType = r2.interpolationType, this._innerEvaluate = r2.evaluate;
  }
  evaluate(e49, t2) {
    try {
      return this._innerEvaluate(e49, t2);
    } catch (e50) {
      let t3 = e50 instanceof Error ? e50.message : String(e50), n2 = `|${t3}`;
      return this._warningHistory[n2] || (this._warningHistory[n2] = true, typeof console < `u` && console.warn(oi(this._rootKey, ``, t3, this._defaultValue))), this._defaultValue;
    }
  }
  static deserialize(t2) {
    return new e36(t2._parameters, t2._rootKey, t2._specification);
  }
  static serialize(e49) {
    return { _parameters: e49._parameters, _specification: e49._specification, _rootKey: e49._rootKey };
  }
};
function hi(e49, t2, n2, r2) {
  if (Kr(e49)) return new mi(e49, t2, n2);
  if (ci(e49)) {
    let i2 = pi(e49, t2, n2, r2);
    if (i2.result === `error`) throw Error(i2.value.map((e50) => `${e50.key}: ${e50.message}`).join(`, `));
    return i2.value;
  }
  {
    let t3 = e49;
    return n2.type === `color` && typeof e49 == `string` ? t3 = V.parse(e49) : n2.type === `padding` && (typeof e49 == `number` || Array.isArray(e49)) ? t3 = dt.parse(e49) : n2.type === `numberArray` && (typeof e49 == `number` || Array.isArray(e49)) ? t3 = ft.parse(e49) : n2.type === `colorArray` && (typeof e49 == `string` || Array.isArray(e49)) ? t3 = pt.parse(e49) : n2.type === `variableAnchorOffsetCollection` && Array.isArray(e49) ? t3 = ht.parse(e49) : n2.type === `projectionDefinition` && typeof e49 == `string` && (t3 = _t.parse(e49)), { globalStateRefs: /* @__PURE__ */ new Set(), _globalState: null, kind: `constant`, evaluate: () => t3 };
  }
}
function gi(e49) {
  let t2 = null;
  if (e49 instanceof Ht) t2 = gi(e49.result);
  else if (e49 instanceof Xt) {
    for (let n2 of e49.args) if (t2 = gi(n2), t2) break;
  } else (e49 instanceof Et || e49 instanceof Ot) && e49.input instanceof Ar && e49.input.name === `zoom` && (t2 = e49);
  return t2 instanceof Dr || e49.eachChild((e50) => {
    let n2 = gi(e50);
    n2 instanceof Dr ? t2 = n2 : !t2 && n2 ? t2 = new Dr(``, `"zoom" expression may only be used as input to a top-level "step" or "interpolate" expression.`) : t2 && n2 && t2 !== n2 && (t2 = new Dr(``, `Only one zoom-based "step" or "interpolate" subexpression may be used in an expression.`));
  }), t2;
}
function _i(e49, t2 = /* @__PURE__ */ new Set()) {
  return e49 instanceof Tr && t2.add(e49.key), e49.eachChild((e50) => {
    _i(e50, t2);
  }), t2;
}
function vi(e49) {
  let t2 = { color: ve, string: I, number: F, enum: I, boolean: L, formatted: Ce, padding: we, numberArray: Ee, colorArray: Te, projectionDefinition: ye, resolvedImage: De, variableAnchorOffsetCollection: Oe };
  return e49.type === `array` ? ke(t2[e49.value] || R, e49.length) : t2[e49.type];
}
function yi(e49) {
  if (e49.type === `color` && Kr(e49.default)) return new V(0, 0, 0, 0);
  switch (e49.type) {
    case `color`:
      return V.parse(e49.default) || null;
    case `padding`:
      return dt.parse(e49.default) || null;
    case `numberArray`:
      return ft.parse(e49.default) || null;
    case `colorArray`:
      return pt.parse(e49.default) || null;
    case `variableAnchorOffsetCollection`:
      return ht.parse(e49.default) || null;
    case `projectionDefinition`:
      return _t.parse(e49.default) || null;
    default:
      return e49.default === void 0 ? null : e49.default;
  }
}
function bi(e49, t2) {
  let { zoom: n2, heatmapDensity: r2, elevation: i2, lineProgress: a2, isSupportedScript: o2, accumulated: s2 } = e49 ?? {};
  return { zoom: n2, heatmapDensity: r2, elevation: i2, lineProgress: a2, isSupportedScript: o2, accumulated: s2, globalState: t2 };
}
function xi(e49) {
  let t2 = false;
  for (let n2 of e49) {
    let e50 = Si(n2);
    if (e50 === `expression`) return `expression`;
    e50 === `legacy` && (t2 = true);
  }
  return t2 ? `legacy` : `neutral`;
}
function Si(e49) {
  if (typeof e49 == `boolean`) return `neutral`;
  if (!Array.isArray(e49) || e49.length === 0) return `legacy`;
  switch (e49[0]) {
    case `has`:
      return e49.length < 2 || e49[1] === `$id` || e49[1] === `$type` ? `legacy` : e49.length === 2 ? `neutral` : `expression`;
    case `in`:
      return e49.length >= 3 && (typeof e49[1] != `string` || Array.isArray(e49[2])) ? `expression` : `legacy`;
    case `!in`:
    case `!has`:
      return `legacy`;
    case `==`:
    case `!=`:
    case `>`:
    case `>=`:
    case `<`:
    case `<=`:
      return e49.length !== 3 || Array.isArray(e49[1]) || Array.isArray(e49[2]) ? `expression` : `legacy`;
    case `none`:
      return `legacy`;
    case `any`:
    case `all`:
      return xi(e49.slice(1));
    default:
      return `expression`;
  }
}
function Ci(e49) {
  return Si(e49) !== `legacy`;
}
function wi(e49) {
  return e49 === `$type` ? [`geometry-type`] : e49 === `$id` ? [`id`] : [`get`, e49];
}
function Ti(e49) {
  switch (e49[0]) {
    case `==`:
    case `!=`:
    case `<`:
    case `<=`:
    case `>`:
    case `>=`:
      return e49.length !== 3 || typeof e49[1] != `string` ? null : [e49[0], wi(e49[1]), e49[2]];
    case `in`:
    case `!in`: {
      if (e49.length < 2 || typeof e49[1] != `string`) return null;
      let t2 = [`in`, wi(e49[1]), [`literal`, e49.slice(2)]];
      return e49[0] === `!in` ? [`!`, t2] : t2;
    }
    case `has`:
    case `!has`: {
      if (e49.length !== 2 || typeof e49[1] != `string` || e49[1] === `$type` || e49[1] === `$id`) return null;
      let t2 = [`has`, e49[1]];
      return e49[0] === `!has` ? [`!`, t2] : t2;
    }
    default:
      return null;
  }
}
function Ei(e49) {
  if ((e49[0] === `<` || e49[0] === `<=` || e49[0] === `>` || e49[0] === `>=`) && e49[1] === `$type`) return `"$type" cannot be use with operator "${e49[0]}"`;
  let t2 = Ti(e49);
  return t2 ? `Mixing deprecated filter syntax with expression syntax is not supported. Replace ${JSON.stringify(e49)} with ${JSON.stringify(t2)}.` : `Mixing deprecated filter syntax with expression syntax is not supported. Convert ${JSON.stringify(e49)} to expression syntax.`;
}
function Di(e49, t2, n2) {
  let r2 = n2[e49];
  return Array.isArray(r2) ? Ci(r2) ? Oi(r2, t2.concat(e49)) : { path: t2.concat(e49), legacyFilter: r2 } : null;
}
function Oi(e49, t2 = []) {
  if (!Array.isArray(e49) || e49.length < 1) return null;
  switch (e49[0]) {
    case `all`:
    case `any`:
    case `none`:
      for (let n2 = 1; n2 < e49.length; n2++) {
        let r2 = Di(n2, t2, e49);
        if (r2) return r2;
      }
      break;
    case `!`: {
      let n2 = Di(1, t2, e49);
      if (n2) return n2;
      break;
    }
    case `case`:
      for (let n2 = 1; n2 < e49.length - 1; n2 += 2) {
        let r2 = Di(n2, t2, e49);
        if (r2) return r2;
      }
  }
  return null;
}
function ki(e49, t2) {
  let n2 = Oi(e49);
  if (!n2 || typeof console > `u`) return;
  let r2 = n2.path.map((e50) => `[${e50}]`).join(``);
  console.warn(`${t2}${r2}: ${Ei(n2.legacyFilter)}`);
}
const Ai = { type: `boolean`, default: false, transition: false, "property-type": `data-driven`, expression: { interpolated: false, parameters: [`zoom`, `feature`] } };
function ji(e49, t2, n2) {
  if (e49 == null) return { filter: () => true, needGeometry: false, getGlobalStateRefs: () => /* @__PURE__ */ new Set() };
  Ci(e49) ? ki(e49, t2) : e49 = Pi(e49);
  let r2 = li(e49, t2, Ai, n2);
  if (r2.result === `error`) throw Error(r2.value.map((e50) => `${e50.key}: ${e50.message}`).join(`, `));
  return { filter: (e50, t3, n3) => r2.value.evaluate(e50, t3, {}, n3), needGeometry: Ni(e49), getGlobalStateRefs: () => _i(r2.value.expression) };
}
function Mi(e49, t2) {
  return e49 < t2 ? -1 : +(e49 > t2);
}
function Ni(e49) {
  if (!Array.isArray(e49)) return false;
  if (e49[0] === `within` || e49[0] === `distance`) return true;
  for (let t2 = 1; t2 < e49.length; t2++) if (Ni(e49[t2])) return true;
  return false;
}
function Pi(e49) {
  if (!e49) return true;
  let t2 = e49[0];
  return e49.length <= 1 ? t2 !== `any` : t2 === `==` ? Fi(e49[1], e49[2], `==`) : t2 === `!=` ? zi(Fi(e49[1], e49[2], `==`)) : t2 === `<` || t2 === `>` || t2 === `<=` || t2 === `>=` ? Fi(e49[1], e49[2], t2) : t2 === `any` ? Ii(e49.slice(1)) : t2 === `all` ? [`all`].concat(e49.slice(1).map(Pi)) : t2 === `none` ? [`all`].concat(e49.slice(1).map(Pi).map(zi)) : t2 === `in` ? Li(e49[1], e49.slice(2)) : t2 === `!in` ? zi(Li(e49[1], e49.slice(2))) : t2 === `has` ? Ri(e49[1]) : t2 !== `!has` || zi(Ri(e49[1]));
}
function Fi(e49, t2, n2) {
  switch (e49) {
    case `$type`:
      return [`filter-type-${n2}`, t2];
    case `$id`:
      return [`filter-id-${n2}`, t2];
    default:
      return [`filter-${n2}`, e49, t2];
  }
}
function Ii(e49) {
  return [`any`].concat(e49.map(Pi));
}
function Li(e49, t2) {
  if (t2.length === 0) return false;
  switch (e49) {
    case `$type`:
      return [`filter-type-in`, [`literal`, t2]];
    case `$id`:
      return [`filter-id-in`, [`literal`, t2]];
    default:
      return t2.length > 200 && !t2.some((e50) => typeof e50 != typeof t2[0]) ? [`filter-in-large`, e49, [`literal`, t2.sort(Mi)]] : [`filter-in-small`, e49, [`literal`, t2]];
  }
}
function Ri(e49) {
  switch (e49) {
    case `$type`:
      return true;
    case `$id`:
      return [`filter-has-id`];
    default:
      return [`filter-has`, e49];
  }
}
function zi(e49) {
  return [`!`, e49];
}
function Bi(e49) {
  let t2 = typeof e49;
  if (t2 === `number` || t2 === `boolean` || t2 === `string` || e49 == null) return JSON.stringify(e49);
  if (Array.isArray(e49)) {
    let t3 = `[`;
    for (let n3 of e49) t3 += `${Bi(n3)},`;
    return `${t3}]`;
  }
  let n2 = Object.keys(e49).sort(), r2 = `{`;
  for (let t3 = 0; t3 < n2.length; t3++) r2 += `${JSON.stringify(n2[t3])}:${Bi(e49[n2[t3]])},`;
  return `${r2}}`;
}
function Vi(e49) {
  let t2 = ``;
  for (let n2 of _e) t2 += `/${Bi(e49[n2])}`;
  return t2;
}
function Hi(e49, t2) {
  let n2 = {};
  for (let r3 = 0; r3 < e49.length; r3++) {
    let i2 = t2 && t2[e49[r3].id] || Vi(e49[r3]);
    t2 && (t2[e49[r3].id] = i2);
    let a2 = n2[i2];
    a2 ||= n2[i2] = [], a2.push(e49[r3]);
  }
  let r2 = [];
  for (let e50 in n2) r2.push(n2[e50]);
  return r2;
}
function Ui(e49) {
  let t2 = e49.key, n2 = e49.value;
  return n2 ? [new N(t2, n2, `constants have been deprecated as of v8`)] : [];
}
function Wi(e49) {
  return e49 instanceof Number || e49 instanceof String || e49 instanceof Boolean ? e49.valueOf() : e49;
}
function Gi(e49) {
  if (Array.isArray(e49)) return e49.map(Gi);
  if (e49 instanceof Object && !(e49 instanceof Number || e49 instanceof String || e49 instanceof Boolean)) {
    let t2 = {};
    for (let n2 in e49) t2[n2] = Gi(e49[n2]);
    return t2;
  }
  return Wi(e49);
}
function Ki(e49) {
  let t2 = e49.key, n2 = e49.value, r2 = e49.valueSpec || {}, i2 = e49.objectElementValidators || {}, a2 = e49.style, o2 = e49.styleSpec, s2 = e49.validateSpec, c2 = [], l2 = W(n2);
  if (l2 !== `object`) return [new N(t2, n2, `object expected, ${l2} found`)];
  for (let e50 in n2) {
    let l3 = e50.split(`.`)[0], u2 = Qe(r2, l3) || r2[`*`], d2;
    if (Qe(i2, l3)) d2 = i2[l3];
    else if (Qe(r2, l3)) {
      if (n2[e50] === void 0) continue;
      d2 = s2;
    } else if (i2[`*`]) d2 = i2[`*`];
    else if (r2[`*`]) d2 = s2;
    else {
      c2.push(new N(t2, n2[e50], `unknown property "${e50}"`));
      continue;
    }
    c2 = c2.concat(d2({ key: (t2 && `${t2}.`) + e50, value: n2[e50], valueSpec: u2, style: a2, styleSpec: o2, object: n2, objectKey: e50, validateSpec: s2 }, n2));
  }
  for (let e50 in r2) i2[e50] || r2[e50].required && r2[e50].default === void 0 && n2[e50] === void 0 && c2.push(new N(t2, n2, `missing required property "${e50}"`));
  return c2;
}
function qi(e49) {
  let t2 = e49.value, n2 = e49.valueSpec, r2 = e49.validateSpec, i2 = e49.style, a2 = e49.styleSpec, o2 = e49.key, s2 = e49.arrayElementValidator || r2;
  if (W(t2) !== `array`) return [new N(o2, t2, `array expected, ${W(t2)} found`)];
  if (n2.length && t2.length !== n2.length) return [new N(o2, t2, `array length ${n2.length} expected, length ${t2.length} found`)];
  let c2 = { type: n2.value, values: n2.values };
  a2.$version < 7 && (c2.function = n2.function), W(n2.value) === `object` && (c2 = n2.value);
  let l2 = [];
  for (let n3 = 0; n3 < t2.length; n3++) l2 = l2.concat(s2({ array: t2, arrayIndex: n3, value: t2[n3], valueSpec: c2, validateSpec: e49.validateSpec, style: i2, styleSpec: a2, key: `${o2}[${n3}]` }));
  return l2;
}
function Ji(e49) {
  let t2 = e49.key, n2 = e49.value, r2 = e49.valueSpec, i2 = W(n2);
  return i2 === `number` && n2 !== n2 && (i2 = `NaN`), i2 === `number` ? `minimum` in r2 && n2 < r2.minimum ? [new N(t2, n2, `${n2} is less than the minimum value ${r2.minimum}`)] : `maximum` in r2 && n2 > r2.maximum ? [new N(t2, n2, `${n2} is greater than the maximum value ${r2.maximum}`)] : [] : [new N(t2, n2, `number expected, ${i2} found`)];
}
function Yi(e49) {
  let t2 = e49.valueSpec, n2 = Wi(e49.value.type), r2, i2 = {}, a2, o2, s2 = n2 !== `categorical` && e49.value.property === void 0, c2 = !s2, l2 = W(e49.value.stops) === `array` && W(e49.value.stops[0]) === `array` && W(e49.value.stops[0][0]) === `object`, u2 = Ki({ key: e49.key, value: e49.value, valueSpec: e49.styleSpec.function, validateSpec: e49.validateSpec, style: e49.style, styleSpec: e49.styleSpec, objectElementValidators: { stops: d2, default: m2 } });
  return n2 === `identity` && s2 && u2.push(new N(e49.key, e49.value, `missing required property "property"`)), n2 !== `identity` && !e49.value.stops && u2.push(new N(e49.key, e49.value, `missing required property "stops"`)), n2 === `exponential` && e49.valueSpec.expression && !Ur(e49.valueSpec) && u2.push(new N(e49.key, e49.value, `exponential functions not supported`)), e49.styleSpec.$version >= 8 && (c2 && !Vr(e49.valueSpec) ? u2.push(new N(e49.key, e49.value, `property functions not supported`)) : s2 && !Hr(e49.valueSpec) && u2.push(new N(e49.key, e49.value, `zoom functions not supported`))), (n2 === `categorical` || l2) && e49.value.property === void 0 && u2.push(new N(e49.key, e49.value, `"property" property is required`)), u2;
  function d2(e50) {
    if (n2 === `identity`) return [new N(e50.key, e50.value, `identity function may not have a "stops" property`)];
    let t3 = [], r3 = e50.value;
    return t3 = t3.concat(qi({ key: e50.key, value: r3, valueSpec: e50.valueSpec, validateSpec: e50.validateSpec, style: e50.style, styleSpec: e50.styleSpec, arrayElementValidator: f2 })), W(r3) === `array` && r3.length === 0 && t3.push(new N(e50.key, r3, `array must have at least one stop`)), t3;
  }
  function f2(e50) {
    let n3 = [], r3 = e50.value, s3 = e50.key;
    if (W(r3) !== `array`) return [new N(s3, r3, `array expected, ${W(r3)} found`)];
    if (r3.length !== 2) return [new N(s3, r3, `array length 2 expected, length ${r3.length} found`)];
    if (l2) {
      if (W(r3[0]) !== `object`) return [new N(s3, r3, `object expected, ${W(r3[0])} found`)];
      if (r3[0].zoom === void 0) return [new N(s3, r3, `object stop key must have zoom`)];
      if (r3[0].value === void 0) return [new N(s3, r3, `object stop key must have value`)];
      if (o2 && o2 > Wi(r3[0].zoom)) return [new N(s3, r3[0].zoom, `stop zoom values must appear in ascending order`)];
      Wi(r3[0].zoom) !== o2 && (o2 = Wi(r3[0].zoom), a2 = void 0, i2 = {}), n3 = n3.concat(Ki({ key: `${s3}[0]`, value: r3[0], valueSpec: { zoom: {} }, validateSpec: e50.validateSpec, style: e50.style, styleSpec: e50.styleSpec, objectElementValidators: { zoom: Ji, value: p2 } }));
    } else n3 = n3.concat(p2({ key: `${s3}[0]`, value: r3[0], valueSpec: {}, validateSpec: e50.validateSpec, style: e50.style, styleSpec: e50.styleSpec }, r3));
    return ci(Gi(r3[1])) ? n3.concat([new N(`${s3}[1]`, r3[1], `expressions are not allowed in function stops.`)]) : n3.concat(e50.validateSpec({ key: `${s3}[1]`, value: r3[1], valueSpec: t2, validateSpec: e50.validateSpec, style: e50.style, styleSpec: e50.styleSpec }));
  }
  function p2(e50, o3) {
    let s3 = W(e50.value), c3 = Wi(e50.value), l3 = e50.value === null ? o3 : e50.value;
    if (!r2) r2 = s3;
    else if (s3 !== r2) return [new N(e50.key, l3, `${s3} stop domain type must match previous stop domain type ${r2}`)];
    if (s3 !== `number` && s3 !== `string` && s3 !== `boolean`) return [new N(e50.key, l3, `stop domain value must be a number, string, or boolean`)];
    if (s3 !== `number` && n2 !== `categorical`) {
      let r3 = `number expected, ${s3} found`;
      return Vr(t2) && n2 === void 0 && (r3 += '\nIf you intended to use a categorical function, specify `"type": "categorical"`.'), [new N(e50.key, l3, r3)];
    }
    return n2 === `categorical` && s3 === `number` && (!isFinite(c3) || Math.floor(c3) !== c3) ? [new N(e50.key, l3, `integer expected, found ${c3}`)] : n2 !== `categorical` && s3 === `number` && a2 !== void 0 && c3 < a2 ? [new N(e50.key, l3, `stop domain values must appear in ascending order`)] : (a2 = c3, n2 === `categorical` && c3 in i2 ? [new N(e50.key, l3, `stop domain values must be unique`)] : (i2[c3] = true, []));
  }
  function m2(e50) {
    return e50.validateSpec({ key: e50.key, value: e50.value, valueSpec: t2, validateSpec: e50.validateSpec, style: e50.style, styleSpec: e50.styleSpec });
  }
}
function Xi(e49) {
  let t2 = (e49.expressionContext === `property` ? pi : li)(Gi(e49.value), e49.key, e49.valueSpec);
  if (t2.result === `error`) return t2.value.map((t3) => new N(`${e49.key}${t3.key}`, e49.value, t3.message));
  let n2 = t2.value.expression || t2.value._styleExpression.expression;
  if (e49.expressionContext === `property` && e49.propertyKey === `text-font` && !n2.outputDefined()) return [new N(e49.key, e49.value, `Invalid data expression for "${e49.propertyKey}". Output values must be contained as literals within the expression.`)];
  if (e49.expressionContext === `property` && e49.propertyType === `layout` && !zr(n2)) return [new N(e49.key, e49.value, `"feature-state" data expressions are not supported with layout properties.`)];
  if (e49.expressionContext === `filter` && !zr(n2)) return [new N(e49.key, e49.value, `"feature-state" data expressions are not supported with filters.`)];
  if (e49.expressionContext && e49.expressionContext.indexOf(`cluster`) === 0) {
    if (!Br(n2, [`zoom`, `feature-state`])) return [new N(e49.key, e49.value, `"zoom" and "feature-state" expressions are not supported with cluster properties.`)];
    if (e49.expressionContext === `cluster-initial` && !Rr(n2)) return [new N(e49.key, e49.value, `Feature data expressions are not supported with initial expression part of cluster properties.`)];
  }
  return [];
}
function Zi(e49) {
  let t2 = e49.value, n2 = e49.key, r2 = W(t2);
  return r2 === `boolean` ? [] : [new N(n2, t2, `boolean expected, ${r2} found`)];
}
function Qi(e49) {
  let t2 = e49.key, n2 = e49.value, r2 = W(n2);
  return r2 === `string` ? V.parse(String(n2)) ? [] : [new N(t2, n2, `color expected, "${n2}" found`)] : [new N(t2, n2, `color expected, ${r2} found`)];
}
function $i(e49) {
  let t2 = e49.key, n2 = e49.value, r2 = e49.valueSpec, i2 = [];
  return Array.isArray(r2.values) ? r2.values.indexOf(Wi(n2)) === -1 && i2.push(new N(t2, n2, `expected one of [${r2.values.join(`, `)}], ${JSON.stringify(n2)} found`)) : Object.keys(r2.values).indexOf(Wi(n2)) === -1 && i2.push(new N(t2, n2, `expected one of [${Object.keys(r2.values).join(`, `)}], ${JSON.stringify(n2)} found`)), i2;
}
function ea(e49, t2) {
  let n2 = e49;
  for (let e50 of t2) n2 = n2[e50];
  return n2;
}
function ta(e49, t2) {
  let n2 = Oi(t2);
  return n2 ? [new N(`${e49.key}${n2.path.map((e50) => `[${e50}]`).join(``)}`, ea(e49.value, n2.path), Ei(n2.legacyFilter), null, `warning`)] : [];
}
function na(e49) {
  let t2 = Gi(e49.value);
  return Ci(t2) ? [...ta(e49, t2), ...Xi(Gr({}, e49, { expressionContext: `filter`, valueSpec: { value: `boolean` } }))] : ra(e49);
}
function ra(e49) {
  let t2 = e49.value, n2 = e49.key;
  if (W(t2) !== `array`) return [new N(n2, t2, `array expected, ${W(t2)} found`)];
  let r2 = e49.styleSpec, i2, a2 = [];
  if (t2.length < 1) return [new N(n2, t2, `filter array must have at least 1 element`)];
  switch (a2 = a2.concat($i({ key: `${n2}[0]`, value: t2[0], valueSpec: r2.filter_operator, style: e49.style, styleSpec: e49.styleSpec })), Wi(t2[0])) {
    case `<`:
    case `<=`:
    case `>`:
    case `>=`:
      t2.length >= 2 && Wi(t2[1]) === `$type` && a2.push(new N(n2, t2, `"$type" cannot be use with operator "${t2[0]}"`));
    case `==`:
    case `!=`:
      t2.length !== 3 && a2.push(new N(n2, t2, `filter array for operator "${t2[0]}" must have 3 elements`));
    case `in`:
    case `!in`:
      t2.length >= 2 && (i2 = W(t2[1]), i2 !== `string` && a2.push(new N(`${n2}[1]`, t2[1], `string expected, ${i2} found`)));
      for (let o2 = 2; o2 < t2.length; o2++) i2 = W(t2[o2]), Wi(t2[1]) === `$type` ? a2 = a2.concat($i({ key: `${n2}[${o2}]`, value: t2[o2], valueSpec: r2.geometry_type, style: e49.style, styleSpec: e49.styleSpec })) : i2 !== `string` && i2 !== `number` && i2 !== `boolean` && a2.push(new N(`${n2}[${o2}]`, t2[o2], `string, number, or boolean expected, ${i2} found`));
      break;
    case `any`:
    case `all`:
    case `none`:
      for (let r3 = 1; r3 < t2.length; r3++) a2 = a2.concat(ra({ key: `${n2}[${r3}]`, value: t2[r3], style: e49.style, styleSpec: e49.styleSpec }));
      break;
    case `has`:
    case `!has`:
      i2 = W(t2[1]), t2.length === 2 ? i2 !== `string` && a2.push(new N(`${n2}[1]`, t2[1], `string expected, ${i2} found`)) : a2.push(new N(n2, t2, `filter array for "${t2[0]}" operator must have 2 elements`));
  }
  return a2;
}
function ia(e49, t2) {
  let n2 = e49.key, r2 = e49.validateSpec, i2 = e49.style, a2 = e49.styleSpec, o2 = e49.value, s2 = e49.objectKey, c2 = a2[`${t2}_${e49.layerType}`];
  if (!c2) return [];
  let l2 = s2.match(Wr);
  if (t2 === `paint` && l2 && c2[l2[1]] && c2[l2[1]].transition) return r2({ key: n2, value: o2, valueSpec: a2.transition, style: i2, styleSpec: a2 });
  let u2 = e49.valueSpec || c2[s2];
  if (!u2) return [new N(n2, o2, `unknown property "${s2}"`)];
  let d2;
  if (W(o2) === `string` && Vr(u2) && !u2.tokens && (d2 = /^{([^}]+)}$/.exec(o2))) return [new N(n2, o2, `"${s2}" does not support interpolation syntax
Use an identity property function instead: \`{ "type": "identity", "property": ${JSON.stringify(d2[1])} }\`.`)];
  let f2 = [];
  return e49.layerType === `symbol` && s2 === `text-font` && Kr(Gi(o2)) && Wi(o2.type) === `identity` && f2.push(new N(n2, o2, `"text-font" does not support identity functions`)), f2.concat(r2({ key: e49.key, value: o2, valueSpec: u2, style: i2, styleSpec: a2, expressionContext: `property`, propertyType: t2, propertyKey: s2 }));
}
function aa(e49) {
  return ia(e49, `paint`);
}
function oa(e49) {
  return ia(e49, `layout`);
}
function sa(e49) {
  var _a3, _b3;
  let t2 = [], n2 = e49.value, r2 = e49.key, i2 = e49.style, a2 = e49.styleSpec;
  if (W(n2) !== `object`) return [new N(r2, n2, `object expected, ${W(n2)} found`)];
  !n2.type && !n2.ref && t2.push(new N(r2, n2, `either "type" or "ref" is required`));
  let o2 = Wi(n2.type), s2 = Wi(n2.ref);
  if (n2.id) {
    let a3 = Wi(n2.id);
    for (let o3 = 0; o3 < e49.arrayIndex; o3++) {
      let e50 = i2.layers[o3];
      Wi(e50.id) === a3 && t2.push(new N(r2, n2.id, `duplicate layer id "${n2.id}", previously used at line ${e50.id.__line__}`));
    }
  }
  if (`ref` in n2) {
    [`type`, `source`, `source-layer`, `filter`, `layout`].forEach((e51) => {
      e51 in n2 && t2.push(new N(r2, n2[e51], `"${e51}" is prohibited for ref layers`));
    });
    let e50;
    i2.layers.forEach((t3) => {
      Wi(t3.id) === s2 && (e50 = t3);
    }), e50 ? e50.ref ? t2.push(new N(r2, n2.ref, `ref cannot reference another ref layer`)) : o2 = Wi(e50.type) : t2.push(new N(r2, n2.ref, `ref layer "${s2}" not found`));
  } else if (o2 !== `background`) {
    if (!n2.source) t2.push(new N(r2, n2, `missing required property "source"`));
    else {
      let e50 = i2.sources && i2.sources[n2.source], a3 = e50 && Wi(e50.type);
      e50 ? a3 === `vector` && o2 === `raster` ? t2.push(new N(r2, n2.source, `layer "${n2.id}" requires a raster source`)) : a3 !== `raster-dem` && o2 === `hillshade` || a3 !== `raster-dem` && o2 === `color-relief` ? t2.push(new N(r2, n2.source, `layer "${n2.id}" requires a raster-dem source`)) : a3 === `raster` && o2 !== `raster` ? t2.push(new N(r2, n2.source, `layer "${n2.id}" requires a vector source`)) : a3 === `vector` && !n2[`source-layer`] ? t2.push(new N(r2, n2, `layer "${n2.id}" must specify a "source-layer"`)) : a3 === `raster-dem` && o2 !== `hillshade` && o2 !== `color-relief` ? t2.push(new N(r2, n2.source, `raster-dem source can only be used with layer type 'hillshade' or 'color-relief'.`)) : o2 === `line` && n2.paint && n2.paint[`line-gradient`] && (a3 !== `geojson` || !e50.lineMetrics) && t2.push(new N(r2, n2, `layer "${n2.id}" specifies a line-gradient, which requires a GeoJSON source with \`lineMetrics\` enabled.`)) : t2.push(new N(r2, n2.source, `source "${n2.source}" not found`));
    }
  }
  return o2 === `raster` && ((_a3 = n2.paint) == null ? void 0 : _a3.resampling) && ((_b3 = n2.paint) == null ? void 0 : _b3[`raster-resampling`]) && t2.push(new N(r2, n2.paint, `layer "${n2.id}" redundantly specifies "resampling" and "raster-resampling" paint properties, but only one is allowed. It is advised to use "resampling".`)), t2 = t2.concat(Ki({ key: r2, value: n2, valueSpec: a2.layer, style: e49.style, styleSpec: e49.styleSpec, validateSpec: e49.validateSpec, objectElementValidators: { "*"() {
    return [];
  }, type() {
    return e49.validateSpec({ key: `${r2}.type`, value: n2.type, valueSpec: a2.layer.type, style: e49.style, styleSpec: e49.styleSpec, validateSpec: e49.validateSpec, object: n2, objectKey: `type` });
  }, filter: na, layout(e50) {
    return Ki({ layer: n2, key: e50.key, value: e50.value, style: e50.style, styleSpec: e50.styleSpec, validateSpec: e50.validateSpec, objectElementValidators: { "*"(e51) {
      return oa(Gr({ layerType: o2 }, e51));
    } } });
  }, paint(e50) {
    return Ki({ layer: n2, key: e50.key, value: e50.value, style: e50.style, styleSpec: e50.styleSpec, validateSpec: e50.validateSpec, objectElementValidators: { "*"(e51) {
      return aa(Gr({ layerType: o2 }, e51));
    } } });
  } } })), t2;
}
function ca(e49) {
  let t2 = e49.value, n2 = e49.key, r2 = W(t2);
  return r2 === `string` ? [] : [new N(n2, t2, `string expected, ${r2} found`)];
}
function la(e49) {
  let t2 = e49.sourceName ?? ``, n2 = e49.value, r2 = e49.styleSpec, i2 = r2.source_raster_dem, a2 = e49.style, o2 = [], s2 = W(n2);
  if (n2 === void 0) return o2;
  if (s2 !== `object`) return o2.push(new N(`source_raster_dem`, n2, `object expected, ${s2} found`)), o2;
  let c2 = Wi(n2.encoding) === `custom`, l2 = [`redFactor`, `greenFactor`, `blueFactor`, `baseShift`], u2 = e49.value.encoding ? `"${e49.value.encoding}"` : `Default`;
  for (let s3 in n2) !c2 && l2.includes(s3) ? o2.push(new N(s3, n2[s3], `In "${t2}": "${s3}" is only valid when "encoding" is set to "custom". ${u2} encoding found`)) : i2[s3] ? o2 = o2.concat(e49.validateSpec({ key: s3, value: n2[s3], valueSpec: i2[s3], validateSpec: e49.validateSpec, style: a2, styleSpec: r2 })) : o2.push(new N(s3, n2[s3], `unknown property "${s3}"`));
  return o2;
}
const ua = { promoteId: fa };
function da(e49) {
  let t2 = e49.value, n2 = e49.key, r2 = e49.styleSpec, i2 = e49.style, a2 = e49.validateSpec;
  if (!t2.type) return [new N(n2, t2, `"type" is required`)];
  let o2 = Wi(t2.type), s2;
  switch (o2) {
    case `vector`:
    case `raster`:
      return s2 = Ki({ key: n2, value: t2, valueSpec: r2[`source_${o2.replace(`-`, `_`)}`], style: e49.style, styleSpec: r2, objectElementValidators: ua, validateSpec: a2 }), s2;
    case `raster-dem`:
      return s2 = la({ sourceName: n2, value: t2, style: e49.style, styleSpec: r2, validateSpec: a2 }), s2;
    case `geojson`:
      if (s2 = Ki({ key: n2, value: t2, valueSpec: r2.source_geojson, style: i2, styleSpec: r2, validateSpec: a2, objectElementValidators: ua }), t2.cluster) for (let e50 in t2.clusterProperties) {
        let [r3, i3] = t2.clusterProperties[e50], o3 = typeof r3 == `string` ? [r3, [`accumulated`], [`get`, e50]] : r3;
        s2.push(...Xi({ key: `${n2}.${e50}.map`, value: i3, validateSpec: a2, expressionContext: `cluster-map` })), s2.push(...Xi({ key: `${n2}.${e50}.reduce`, value: o3, validateSpec: a2, expressionContext: `cluster-reduce` }));
      }
      return s2;
    case `video`:
      return Ki({ key: n2, value: t2, valueSpec: r2.source_video, style: i2, validateSpec: a2, styleSpec: r2 });
    case `image`:
      return Ki({ key: n2, value: t2, valueSpec: r2.source_image, style: i2, validateSpec: a2, styleSpec: r2 });
    case `canvas`:
      return [new N(n2, null, `Please use runtime APIs to add canvas sources, rather than including them in stylesheets.`, `source.canvas`)];
    default:
      return $i({ key: `${n2}.type`, value: t2.type, valueSpec: { values: [`vector`, `raster`, `raster-dem`, `geojson`, `video`, `image`] }, style: i2, validateSpec: a2, styleSpec: r2 });
  }
}
function fa({ key: e49, value: t2 }) {
  if (W(t2) === `string`) return ca({ key: e49, value: t2 });
  {
    let n2 = [];
    for (let r2 in t2) n2.push(...ca({ key: `${e49}.${r2}`, value: t2[r2] }));
    return n2;
  }
}
function pa(e49) {
  let t2 = e49.value, n2 = e49.styleSpec, r2 = n2.light, i2 = e49.style, a2 = [], o2 = W(t2);
  if (t2 === void 0) return a2;
  if (o2 !== `object`) return a2 = a2.concat([new N(`light`, t2, `object expected, ${o2} found`)]), a2;
  for (let o3 in t2) {
    let s2 = o3.match(Wr);
    a2 = s2 && r2[s2[1]] && r2[s2[1]].transition ? a2.concat(e49.validateSpec({ key: o3, value: t2[o3], valueSpec: n2.transition, validateSpec: e49.validateSpec, style: i2, styleSpec: n2 })) : r2[o3] ? a2.concat(e49.validateSpec({ key: o3, value: t2[o3], valueSpec: r2[o3], validateSpec: e49.validateSpec, style: i2, styleSpec: n2 })) : a2.concat([new N(o3, t2[o3], `unknown property "${o3}"`)]);
  }
  return a2;
}
function ma(e49) {
  let t2 = e49.value, n2 = e49.styleSpec, r2 = n2.sky, i2 = e49.style, a2 = W(t2);
  if (t2 === void 0) return [];
  if (a2 !== `object`) return [new N(`sky`, t2, `object expected, ${a2} found`)];
  let o2 = [];
  for (let a3 in t2) {
    let s2 = a3.match(Wr);
    o2 = s2 && r2[s2[1]] && r2[s2[1]].transition ? o2.concat(e49.validateSpec({ key: a3, value: t2[a3], valueSpec: n2.transition, style: i2, styleSpec: n2 })) : r2[a3] ? o2.concat(e49.validateSpec({ key: a3, value: t2[a3], valueSpec: r2[a3], style: i2, styleSpec: n2 })) : o2.concat([new N(a3, t2[a3], `unknown property "${a3}"`)]);
  }
  return o2;
}
function ha(e49) {
  let t2 = e49.value, n2 = e49.styleSpec, r2 = n2.terrain, i2 = e49.style, a2 = [], o2 = W(t2);
  if (t2 === void 0) return a2;
  if (o2 !== `object`) return a2 = a2.concat([new N(`terrain`, t2, `object expected, ${o2} found`)]), a2;
  for (let o3 in t2) a2 = r2[o3] ? a2.concat(e49.validateSpec({ key: o3, value: t2[o3], valueSpec: r2[o3], validateSpec: e49.validateSpec, style: i2, styleSpec: n2 })) : a2.concat([new N(o3, t2[o3], `unknown property "${o3}"`)]);
  return a2;
}
function ga(e49) {
  return ca(e49).length === 0 ? [] : Xi(e49);
}
function _a2(e49) {
  return ca(e49).length === 0 ? [] : Xi(e49);
}
function va(e49) {
  let t2 = e49.key, n2 = e49.value;
  if (W(n2) === `array`) {
    if (n2.length < 1 || n2.length > 4) return [new N(t2, n2, `padding requires 1 to 4 values; ${n2.length} values found`)];
    let r2 = { type: `number` }, i2 = [];
    for (let a2 = 0; a2 < n2.length; a2++) i2 = i2.concat(e49.validateSpec({ key: `${t2}[${a2}]`, value: n2[a2], validateSpec: e49.validateSpec, valueSpec: r2 }));
    return i2;
  }
  return Ji({ key: t2, value: n2, valueSpec: {} });
}
function ya(e49) {
  let t2 = e49.key, n2 = e49.value;
  if (W(n2) === `array`) {
    let r2 = { type: `number` };
    if (n2.length < 1) return [new N(t2, n2, `array length at least 1 expected, length 0 found`)];
    let i2 = [];
    for (let a2 = 0; a2 < n2.length; a2++) i2 = i2.concat(e49.validateSpec({ key: `${t2}[${a2}]`, value: n2[a2], validateSpec: e49.validateSpec, valueSpec: r2 }));
    return i2;
  }
  return Ji({ key: t2, value: n2, valueSpec: {} });
}
function ba(e49) {
  let t2 = e49.key, n2 = e49.value;
  if (W(n2) === `array`) {
    if (n2.length < 1) return [new N(t2, n2, `array length at least 1 expected, length 0 found`)];
    let e50 = [];
    for (let r2 = 0; r2 < n2.length; r2++) e50 = e50.concat(Qi({ key: `${t2}[${r2}]`, value: n2[r2], valueSpec: {} }));
    return e50;
  }
  return Qi({ key: t2, value: n2, valueSpec: {} });
}
function xa(e49) {
  let t2 = e49.key, n2 = e49.value, r2 = W(n2), i2 = e49.styleSpec;
  if (r2 !== `array` || n2.length < 1 || n2.length % 2 != 0) return [new N(t2, n2, `variableAnchorOffsetCollection requires a non-empty array of even length`)];
  let a2 = [];
  for (let r3 = 0; r3 < n2.length; r3 += 2) a2 = a2.concat($i({ key: `${t2}[${r3}]`, value: n2[r3], valueSpec: i2.layout_symbol[`text-anchor`] })), a2 = a2.concat(qi({ key: `${t2}[${r3 + 1}]`, value: n2[r3 + 1], valueSpec: { length: 2, value: `number` }, validateSpec: e49.validateSpec, style: e49.style, styleSpec: i2 }));
  return a2;
}
function Sa(e49) {
  let t2 = [], n2 = e49.value, r2 = e49.key;
  if (Array.isArray(n2)) {
    let i2 = [], a2 = [];
    for (let o2 in n2) n2[o2].id && i2.includes(n2[o2].id) && t2.push(new N(r2, n2, `all the sprites' ids must be unique, but ${n2[o2].id} is duplicated`)), i2.push(n2[o2].id), n2[o2].url && a2.includes(n2[o2].url) && t2.push(new N(r2, n2, `all the sprites' URLs must be unique, but ${n2[o2].url} is duplicated`)), a2.push(n2[o2].url), t2 = t2.concat(Ki({ key: `${r2}[${o2}]`, value: n2[o2], valueSpec: { id: { type: `string`, required: true }, url: { type: `string`, required: true } }, validateSpec: e49.validateSpec }));
    return t2;
  }
  return ca({ key: r2, value: n2 });
}
function Ca(e49) {
  let t2 = e49.value, n2 = e49.styleSpec, r2 = n2.projection, i2 = e49.style, a2 = W(t2);
  if (t2 === void 0) return [];
  if (a2 !== `object`) return [new N(`projection`, t2, `object expected, ${a2} found`)];
  let o2 = [];
  for (let a3 in t2) o2 = r2[a3] ? o2.concat(e49.validateSpec({ key: a3, value: t2[a3], valueSpec: r2[a3], style: i2, styleSpec: n2 })) : o2.concat([new N(a3, t2[a3], `unknown property "${a3}"`)]);
  return o2;
}
function wa(e49) {
  let t2 = e49.key, n2 = e49.value;
  n2 = n2 instanceof String ? n2.valueOf() : n2;
  let r2 = W(n2);
  return r2 === `array` && !Ea(n2) && !Ta(n2) ? [new N(t2, n2, `projection expected, invalid array ${JSON.stringify(n2)} found`)] : [`array`, `string`].includes(r2) ? [] : [new N(t2, n2, `projection expected, invalid type "${r2}" found`)];
}
function Ta(e49) {
  return !![`interpolate`, `step`, `literal`].includes(e49[0]);
}
function Ea(e49) {
  return Array.isArray(e49) && e49.length === 3 && typeof e49[0] == `string` && typeof e49[1] == `string` && typeof e49[2] == `number`;
}
function Da(e49) {
  return !!e49 && e49.constructor === Object;
}
function Oa(e49) {
  return Da(e49.value) ? [] : [new N(e49.key, e49.value, `object expected, ${W(e49.value)} found`)];
}
const ka = 1114111, Aa = /^u\+(?:([0-9a-f]{1,6})(?:-([0-9a-f]{1,6}))?|([0-9a-f]{0,5}\?{1,6}))$/i;
function ja(e49, t2) {
  if (W(t2) !== `string`) return [];
  let n2 = `${t2}`, r2 = () => [new N(e49, t2, `invalid unicode range, expected a value such as "U+26", "U+0-10FFFF" or "U+4??"`)], i2 = n2.match(Aa);
  if (!i2) return r2();
  let [, a2, o2, s2] = i2;
  if (s2 !== void 0) return s2.length > 6 ? r2() : [];
  let c2 = parseInt(a2, 16), l2 = o2 === void 0 ? c2 : parseInt(o2, 16);
  return c2 > ka || l2 > ka ? [new N(e49, t2, `unicode range is out of bounds, the maximum code point is U+10FFFF`)] : c2 > l2 ? [new N(e49, t2, `unicode range start must not be greater than its end, but ${n2} is`)] : [];
}
function Ma(e49) {
  let t2 = e49.key ?? `font-faces`, n2 = e49.value, r2 = e49.validateSpec, i2 = e49.styleSpec ?? j, a2 = e49.style;
  if (!Da(n2)) return [new N(t2, n2, `object expected, ${W(n2)} found`)];
  let o2 = [];
  for (let e50 in n2) {
    let s2 = n2[e50], c2 = W(s2);
    if (c2 === `string`) o2.push(...ca({ key: `${t2}.${e50}`, value: s2 }));
    else if (c2 === `array`) {
      let n3 = { url: { type: `string`, required: true }, "unicode-range": { type: `array`, value: `string` } };
      for (let [c3, l2] of s2.entries()) {
        let s3 = `${t2}.${e50}[${c3}]`;
        o2.push(...Ki({ key: s3, value: l2, valueSpec: n3, styleSpec: i2, style: a2, validateSpec: r2 }));
        let u2 = Da(l2) ? l2[`unicode-range`] : void 0;
        if (W(u2) === `array`) for (let [e51, t3] of u2.entries()) o2.push(...ja(`${s3}.unicode-range[${e51}]`, t3));
      }
    } else o2.push(new N(`${t2}.${e50}`, s2, `string or array expected, ${c2} found`));
  }
  return o2;
}
const Na = { "*"() {
  return [];
}, array: qi, boolean: Zi, number: Ji, color: Qi, constants: Ui, enum: $i, filter: na, function: Yi, layer: sa, object: Ki, source: da, light: pa, sky: ma, terrain: ha, projection: Ca, projectionDefinition: wa, string: ca, formatted: ga, resolvedImage: _a2, padding: va, numberArray: ya, colorArray: ba, variableAnchorOffsetCollection: xa, sprite: Sa, state: Oa, fontFaces: Ma };
function Pa(e49) {
  let t2 = e49.value, n2 = e49.valueSpec, r2 = e49.styleSpec;
  return e49.validateSpec = Pa, n2.expression && Kr(Wi(t2)) ? Yi(e49) : n2.expression && ci(Gi(t2)) ? Xi(e49) : n2.type && Na[n2.type] ? Na[n2.type](e49) : Ki(Gr({}, e49, { valueSpec: n2.type ? r2[n2.type] : n2 }));
}
function Fa(e49) {
  let t2 = e49.value, n2 = e49.key, r2 = ca(e49);
  return r2.length ? r2 : (t2.indexOf(`{fontstack}`) === -1 && r2.push(new N(n2, t2, `"glyphs" url must include a "{fontstack}" token`)), t2.indexOf(`{range}`) === -1 && r2.push(new N(n2, t2, `"glyphs" url must include a "{range}" token`)), r2);
}
function Ia(e49, t2 = M) {
  let n2 = [];
  return n2 = n2.concat(Pa({ key: ``, value: e49, valueSpec: t2.$root, styleSpec: t2, style: e49, validateSpec: Pa, objectElementValidators: { glyphs: Fa, "*"() {
    return [];
  } } })), e49.constants && (n2 = n2.concat(Ui({ key: `constants`, value: e49.constants, style: e49, styleSpec: t2, validateSpec: Pa }))), Ra(n2);
}
Ia.source = za(La(da)), Ia.sprite = za(La(Sa)), Ia.glyphs = za(La(Fa)), Ia.fontFaces = za(La(Ma)), Ia.light = za(La(pa)), Ia.sky = za(La(ma)), Ia.terrain = za(La(ha)), Ia.state = za(La(Oa)), Ia.layer = za(La(sa)), Ia.filter = za(La(na)), Ia.paintProperty = za(La(aa)), Ia.layoutProperty = za(La(oa));
function La(e49) {
  return function(t2) {
    return e49(Object.assign({}, t2, { validateSpec: Pa }));
  };
}
function Ra(e49) {
  return [].concat(e49).sort((e50, t2) => e50.line - t2.line);
}
function za(e49) {
  return function(...t2) {
    return Ra(e49.apply(this, t2));
  };
}
const Ba = { type: `enum`, "property-type": `data-constant`, expression: { interpolated: false, parameters: [`global-state`] }, values: { visible: {}, none: {} }, transition: false, default: `visible` };
var Va = class {
  constructor(e49, t2, n2) {
    this._rootKey = t2, this._globalState = n2, this.setValue(e49);
  }
  evaluate() {
    return this._literalValue ?? this._compiledValue.evaluate({});
  }
  setValue(e49) {
    if (e49 == null || e49 === `visible` || e49 === `none`) {
      this._literalValue = e49 === `none` ? `none` : `visible`, this._compiledValue = void 0, this._globalStateRefs = /* @__PURE__ */ new Set();
      return;
    }
    let t2 = li(e49, this._rootKey, Ba, this._globalState);
    if (t2.result === `error`) throw this._literalValue = `visible`, this._compiledValue = void 0, Error(t2.value.map((e50) => `${e50.key}: ${e50.message}`).join(`, `));
    this._literalValue = void 0, this._compiledValue = t2.value, this._globalStateRefs = _i(t2.value.expression);
  }
  getGlobalStateRefs() {
    return this._globalStateRefs;
  }
};
function Ha(e49, t2, n2) {
  return new Va(e49, t2, n2);
}
const Ua = { MAX_PARALLEL_IMAGE_REQUESTS: 16, MAX_PARALLEL_IMAGE_REQUESTS_PER_FRAME: 8, MAX_TILE_CACHE_ZOOM_LEVELS: 5, REGISTERED_PROTOCOLS: {}, WORKER_URL: `` };
function Wa(e49) {
  return Ua.REGISTERED_PROTOCOLS[e49.substring(0, e49.indexOf(`://`))];
}
function Ga(e49, t2) {
  Ua.REGISTERED_PROTOCOLS[e49] = t2;
}
function Ka(e49) {
  delete Ua.REGISTERED_PROTOCOLS[e49];
}
const qa = `global-dispatcher`;
var Ja = class extends Error {
  constructor(e49, t2, n2, r2) {
    super(`AJAXError: ${t2} (${e49}): ${n2}`), this.status = e49, this.statusText = t2, this.url = n2, this.body = r2;
  }
};
function Ya() {
  var _a3;
  if (ne(self)) return (_a3 = self.worker) == null ? void 0 : _a3.referrer;
  if (window.location.protocol === `blob:`) try {
    return window.parent.location.href;
  } catch {
  }
  return window.location.href;
}
const Xa = (e49) => {
  var _a3;
  return e49.startsWith(`file:`) || ((_a3 = Ya()) == null ? void 0 : _a3.startsWith(`file:`)) && !/^\w+:/.test(e49);
};
async function Za(e49, t2) {
  let n2 = new Request(e49.url, { method: e49.method || `GET`, body: e49.body, credentials: e49.credentials, headers: e49.headers, cache: e49.cache, referrer: Ya(), referrerPolicy: e49.referrerPolicy, signal: t2.signal });
  e49.type === `json` && !n2.headers.has(`Accept`) && n2.headers.set(`Accept`, `application/json`);
  let r2;
  try {
    r2 = await fetch(n2);
  } catch (t3) {
    throw me(t3) ? t3 : new Ja(0, y(t3).message, e49.url, new Blob());
  }
  if (!r2.ok) {
    let t3 = await r2.blob();
    throw new Ja(r2.status, r2.statusText, e49.url, t3);
  }
  let i2;
  i2 = e49.type === `arrayBuffer` || e49.type === `image` ? r2.arrayBuffer() : e49.type === `json` ? r2.json() : r2.text();
  let a2 = await i2;
  return he(t2.signal), { data: a2, cacheControl: r2.headers.get(`Cache-Control`), expires: r2.headers.get(`Expires`), etag: r2.headers.get(`ETag`) };
}
function Qa(e49, t2) {
  return new Promise((n2, r2) => {
    var _a3;
    let i2 = new XMLHttpRequest();
    i2.open(e49.method || `GET`, e49.url, true), (e49.type === `arrayBuffer` || e49.type === `image`) && (i2.responseType = `arraybuffer`);
    for (let t3 in e49.headers) i2.setRequestHeader(t3, e49.headers[t3]);
    e49.type === `json` && (i2.responseType = `text`, ((_a3 = e49.headers) == null ? void 0 : _a3.Accept) || i2.setRequestHeader(`Accept`, `application/json`)), i2.withCredentials = e49.credentials === `include`, i2.onerror = () => {
      r2(Error(i2.statusText));
    }, i2.onload = () => {
      if (!t2.signal.aborted) {
        if ((i2.status >= 200 && i2.status < 300 || i2.status === 0) && i2.response !== null) {
          let t3 = i2.response;
          if (e49.type === `json`) try {
            t3 = JSON.parse(i2.response);
          } catch (e50) {
            r2(e50);
            return;
          }
          n2({ data: t3, cacheControl: i2.getResponseHeader(`Cache-Control`), expires: i2.getResponseHeader(`Expires`), etag: i2.getResponseHeader(`ETag`) });
        } else {
          let t3 = new Blob([i2.response], { type: i2.getResponseHeader(`Content-Type`) });
          r2(new Ja(i2.status, i2.statusText, e49.url, t3));
        }
      }
    }, t2.signal.addEventListener(`abort`, () => {
      i2.abort(), r2(new pe(t2.signal.reason));
    }), i2.send(e49.body);
  });
}
const $a = async function(e49, t2) {
  var _a3, _b3;
  if (e49.url.includes(`://`) && !/^https?:|^file:/.test(e49.url)) {
    let n2 = Wa(e49.url);
    if (n2) {
      let r2 = await n2(e49, t2);
      return !r2.data && e49.type === `arrayBuffer` ? w(r2, { data: /* @__PURE__ */ new ArrayBuffer(0) }) : r2;
    }
    if (ne(self) && ((_a3 = self.worker) == null ? void 0 : _a3.actor)) return self.worker.actor.sendAsync({ type: `GR`, data: e49, targetMapId: qa }, t2);
  }
  if (!Xa(e49.url)) {
    if (fetch && Request && AbortController && Object.hasOwn(Request.prototype, `signal`)) return Za(e49, t2);
    if (ne(self) && ((_b3 = self.worker) == null ? void 0 : _b3.actor)) return self.worker.actor.sendAsync({ type: `GR`, data: e49, mustQueue: true, targetMapId: qa }, t2);
  }
  return Qa(e49, t2);
}, eo = (e49, t2) => $a(w(e49, { type: `json` }), t2), to = (e49, t2) => $a(w(e49, { type: `arrayBuffer` }), t2), no = {};
function G(e49, t2, n2 = {}) {
  if (no[e49]) throw Error(`${e49} is already registered.`);
  Object.defineProperty(t2, "_classRegistryKey", { value: e49, writeable: false }), no[e49] = { klass: t2, omit: n2.omit || [], shallow: n2.shallow || [] };
}
G(`Object`, Object), G(`Set`, Set), G(`TransferableGridIndex`, ge), G(`Color`, V), G(`Error`, Error), G(`AJAXError`, Ja), G(`ResolvedImage`, gt), G(`StylePropertyFunction`, mi), G(`StyleExpression`, ai, { omit: [`_evaluator`] }), G(`ZoomDependentExpression`, di), G(`ZoomConstantExpression`, ui), G(`CompoundExpression`, Ar, { omit: [`_evaluate`] });
for (let e49 in Er) Er[e49]._classRegistryKey || G(`Expression_${e49}`, Er[e49]);
function ro(e49) {
  var _a3;
  return e49 && typeof ArrayBuffer < `u` && (e49 instanceof ArrayBuffer || ((_a3 = e49.constructor) == null ? void 0 : _a3.name) === `ArrayBuffer`);
}
function io(e49) {
  let t2 = e49.constructor;
  return e49.$name || t2._classRegistryKey;
}
function ao(e49) {
  if (typeof e49 != `object` || !e49) return false;
  let t2 = io(e49);
  return t2 && t2 !== `Object`;
}
function oo(e49) {
  return !ao(e49) && (e49 == null || typeof e49 == `boolean` || typeof e49 == `number` || typeof e49 == `string` || e49 instanceof Boolean || e49 instanceof Number || e49 instanceof String || e49 instanceof Date || e49 instanceof RegExp || e49 instanceof Blob || e49 instanceof Error || ro(e49) || re(e49) || ArrayBuffer.isView(e49) || e49 instanceof ImageData);
}
function so(e49, t2) {
  if (oo(e49)) return (ro(e49) || re(e49)) && t2 && t2.push(e49), ArrayBuffer.isView(e49) && t2 && t2.push(e49.buffer), e49 instanceof ImageData && t2 && t2.push(e49.data.buffer), e49;
  if (Array.isArray(e49)) {
    let n3 = [];
    for (let r3 of e49) n3.push(so(r3, t2));
    return n3;
  }
  if (typeof e49 != `object`) throw Error(`can't serialize object of type ${typeof e49}`);
  let n2 = io(e49);
  if (!n2) throw Error(`can't serialize object of unregistered class ${e49.constructor.name}`);
  if (!no[n2]) throw Error(`${n2} is not registered.`);
  let { klass: r2 } = no[n2], i2 = r2.serialize ? r2.serialize(e49, t2) : {};
  if (!r2.serialize) {
    for (let r3 in e49) {
      if (!e49.hasOwnProperty(r3) || no[n2].omit.includes(r3)) continue;
      let a2 = e49[r3];
      a2 !== void 0 && (i2[r3] = no[n2].shallow.includes(r3) ? a2 : so(a2, t2));
    }
    e49 instanceof Error && (i2.message = e49.message);
  } else if (i2 === (t2 == null ? void 0 : t2[t2.length - 1])) throw Error(`statically serialized object won't survive transfer of $name property`);
  if (i2.$name) throw Error(`$name property is reserved for worker serialization logic.`);
  return n2 !== `Object` && (i2.$name = n2), i2;
}
function co(e49) {
  if (oo(e49)) return e49;
  if (Array.isArray(e49)) return e49.map(co);
  if (typeof e49 != `object`) throw Error(`can't deserialize object of type ${typeof e49}`);
  let t2 = io(e49) || `Object`;
  if (!no[t2]) throw Error(`can't deserialize unregistered class ${t2}`);
  let { klass: n2 } = no[t2];
  if (!n2) throw Error(`can't deserialize unregistered class ${t2}`);
  if (n2.deserialize) return n2.deserialize(e49);
  let r2 = Object.create(n2.prototype);
  for (let n3 of Object.keys(e49)) {
    if (n3 === `$name`) continue;
    let i2 = e49[n3];
    r2[n3] = no[t2].shallow.includes(n3) ? i2 : co(i2);
  }
  return r2;
}
var lo = class {
  constructor(e49) {
    this._methodToThrottle = e49, this._triggered = false, this._channel = new MessageChannel(), this._channel.port2.onmessage = () => {
      this._triggered = false, this._methodToThrottle();
    };
  }
  trigger() {
    var _a3;
    this._triggered || (this._triggered = true, (_a3 = this._channel) == null ? void 0 : _a3.port1.postMessage(true));
  }
  remove() {
    delete this._channel, this._methodToThrottle = () => {
    };
  }
};
const uo = { once: true };
var fo = class {
  constructor(e49, t2) {
    this.target = e49, this.mapId = t2, this.resolveRejects = {}, this.tasks = {}, this.taskQueue = [], this.abortControllers = {}, this.messageHandlers = {}, this.invoker = new lo(() => this.process()), this.subscription = ue(this.target, `message`, (e50) => this.receive(e50), false), this.globalScope = ne(self) ? e49 : window;
  }
  registerMessageHandler(e49, t2) {
    this.messageHandlers[e49] = t2;
  }
  unregisterMessageHandler(e49) {
    delete this.messageHandlers[e49];
  }
  sendAsync(e49, t2) {
    return new Promise((n2, r2) => {
      let i2 = Math.round(Math.random() * 1e18).toString(36).substring(0, 10), a2 = t2 ? ue(t2.signal, `abort`, () => {
        a2 == null ? void 0 : a2.unsubscribe(), delete this.resolveRejects[i2];
        let n3 = { id: i2, type: `<cancel>`, origin: location.origin, targetMapId: e49.targetMapId, sourceMapId: this.mapId };
        this.target.postMessage(n3), r2(new pe(t2.signal.reason));
      }, uo) : null;
      this.resolveRejects[i2] = { resolve: (e50) => {
        a2 == null ? void 0 : a2.unsubscribe(), n2(e50);
      }, reject: (e50) => {
        a2 == null ? void 0 : a2.unsubscribe(), r2(e50);
      } };
      let o2 = [], s2 = { ...e49, id: i2, sourceMapId: this.mapId, origin: location.origin, data: so(e49.data, o2) };
      this.target.postMessage(s2, { transfer: o2 });
    });
  }
  receive(e49) {
    let t2 = e49.data, n2 = t2.id, r2 = [`file://`, `resource://android`, `null`], i2 = [t2.origin, location.origin], a2 = t2.origin === location.origin, o2 = i2.some((e50) => r2.includes(e50));
    if ((a2 || o2) && !(t2.targetMapId && this.mapId !== t2.targetMapId)) {
      if (t2.type === `<cancel>`) {
        delete this.tasks[n2];
        let e50 = this.abortControllers[n2];
        delete this.abortControllers[n2], e50 && e50.abort();
        return;
      }
      if (ne(self) || t2.mustQueue) {
        this.tasks[n2] = t2, this.taskQueue.push(n2), this.invoker.trigger();
        return;
      }
      this.processTask(n2, t2);
    }
  }
  process() {
    if (this.taskQueue.length === 0) return;
    let e49 = this.taskQueue.shift(), t2 = this.tasks[e49];
    delete this.tasks[e49], this.taskQueue.length > 0 && this.invoker.trigger(), t2 && this.processTask(e49, t2);
  }
  async processTask(e49, t2) {
    if (t2.type === `<response>`) {
      let n3 = this.resolveRejects[e49];
      if (delete this.resolveRejects[e49], !n3) return;
      t2.error ? n3.reject(y(co(t2.error))) : n3.resolve(co(t2.data));
      return;
    }
    if (!this.messageHandlers[t2.type]) {
      this.completeTask(e49, null, null);
      return;
    }
    let n2 = co(t2.data), r2 = new AbortController();
    this.abortControllers[e49] = r2;
    try {
      let i2 = await this.messageHandlers[t2.type](t2.sourceMapId, n2, r2);
      this.completeTask(e49, null, i2);
    } catch (t3) {
      this.completeTask(e49, y(t3));
    }
  }
  completeTask(e49, t2, n2) {
    let r2 = [];
    delete this.abortControllers[e49];
    let i2 = { id: e49, type: `<response>`, sourceMapId: this.mapId, origin: location.origin, error: t2 ? so(t2) : null, data: so(n2, r2) };
    this.target.postMessage(i2, { transfer: r2 });
  }
  remove() {
    this.invoker.remove(), this.subscription.unsubscribe();
  }
};
function po(e49, t2, n2) {
  var _a3;
  ((_a3 = n2[e49]) == null ? void 0 : _a3.includes(t2)) || (n2[e49] ||= [], n2[e49].push(t2));
}
function mo(e49, t2, n2) {
  if (n2 == null ? void 0 : n2[e49]) {
    let r2 = n2[e49].indexOf(t2);
    r2 !== -1 && n2[e49].splice(r2, 1);
  }
}
var ho = class {
  constructor(e49, t2 = {}) {
    w(this, t2), this.type = e49;
  }
}, go = class extends ho {
  constructor(e49, t2 = {}) {
    super(`error`, w({ error: e49 }, t2));
  }
}, _o = class {
  on(e49, t2) {
    return this._listeners ||= {}, po(e49, t2, this._listeners), { unsubscribe: () => {
      this.off(e49, t2);
    } };
  }
  off(e49, t2) {
    return mo(e49, t2, this._listeners), mo(e49, t2, this._oneTimeListeners), this;
  }
  once(e49, t2) {
    return t2 ? (this._oneTimeListeners ||= {}, po(e49, t2, this._oneTimeListeners), this) : new Promise((t3) => this.once(e49, t3));
  }
  fire(e49, t2) {
    var _a3, _b3, _c3, _d2;
    let n2 = typeof e49 == `string` ? new ho(e49, t2 || {}) : e49, r2 = n2.type;
    if (this.listens(r2)) {
      n2.target = this;
      let e50 = ((_b3 = (_a3 = this._listeners) == null ? void 0 : _a3[r2]) == null ? void 0 : _b3.slice()) ?? [];
      for (let t4 of e50) t4.call(this, n2);
      let t3 = ((_d2 = (_c3 = this._oneTimeListeners) == null ? void 0 : _c3[r2]) == null ? void 0 : _d2.slice()) ?? [];
      for (let e51 of t3) mo(r2, e51, this._oneTimeListeners), e51.call(this, n2);
      let i2 = this._eventedParent;
      i2 && (w(n2, typeof this._eventedParentData == `function` ? this._eventedParentData() : this._eventedParentData), i2.fire(n2));
    } else n2 instanceof go && console.error(n2.error);
    return this;
  }
  listens(e49) {
    var _a3, _b3, _c3, _d2, _e2;
    return !!(((_b3 = (_a3 = this._listeners) == null ? void 0 : _a3[e49]) == null ? void 0 : _b3.length) || ((_d2 = (_c3 = this._oneTimeListeners) == null ? void 0 : _c3[e49]) == null ? void 0 : _d2.length) || ((_e2 = this._eventedParent) == null ? void 0 : _e2.listens(e49)));
  }
  setEventedParent(e49, t2) {
    return this._eventedParent = e49, this._eventedParentData = t2, this;
  }
};
const vo = Ia;
new Set(Object.keys(M).filter((e49) => e49.startsWith(`source_`)).map((e49) => e49.slice(7).replaceAll(`_`, `-`)));
function yo(e49, t2) {
  let n2 = false;
  for (let r2 of t2) {
    if (r2.severity === `warning`) {
      A(r2.message);
      continue;
    }
    e49.fire(new go(Error(r2.message))), n2 = true;
  }
  return n2;
}
function bo(e49, t2, n2, r2) {
  return (r2 == null ? void 0 : r2.validate) !== false && yo(e49, t2({ styleSpec: M, ...n2 }));
}
var xo = class {
  constructor() {
    this.first = true;
  }
  update(e49, t2) {
    let n2 = Math.floor(e49);
    return this.first ? (this.first = false, this.lastIntegerZoom = n2, this.lastIntegerZoomTime = 0, this.lastZoom = e49, this.lastFloorZoom = n2, true) : (this.lastFloorZoom > n2 ? (this.lastIntegerZoom = n2 + 1, this.lastIntegerZoomTime = t2) : this.lastFloorZoom < n2 && (this.lastIntegerZoom = n2, this.lastIntegerZoomTime = t2), e49 !== this.lastZoom && (this.lastZoom = e49, this.lastFloorZoom = n2, true));
  }
}, K = class {
  constructor(e49, t2) {
    this.zoom = e49, t2 ? (this.now = t2.now || 0, this.fadeDuration = t2.fadeDuration || 0, this.zoomHistory = t2.zoomHistory || new xo(), this.transition = t2.transition || {}) : (this.now = 0, this.fadeDuration = 0, this.zoomHistory = new xo(), this.transition = {});
  }
  crossFadingFactor() {
    return this.fadeDuration === 0 ? 1 : Math.min((this.now - this.zoomHistory.lastIntegerZoomTime) / this.fadeDuration, 1);
  }
  getCrossfadeParameters() {
    let e49 = this.zoom, t2 = e49 - Math.floor(e49), n2 = this.crossFadingFactor();
    return e49 > this.zoomHistory.lastIntegerZoom ? { fromScale: 2, toScale: 1, t: t2 + (1 - t2) * n2 } : { fromScale: 0.5, toScale: 1, t: 1 - (1 - n2) * t2 };
  }
}, So = class {
  constructor(e49, t2, n2, r2) {
    this.property = e49, this.value = t2, this.expression = hi(t2 === void 0 ? e49.specification.default : t2, n2, e49.specification, r2);
  }
  isDataDriven() {
    return this.expression.kind === `source` || this.expression.kind === `composite`;
  }
  getGlobalStateRefs() {
    return this.expression.globalStateRefs || /* @__PURE__ */ new Set();
  }
  readsGlobalState(e49) {
    let t2 = this.getGlobalStateRefs();
    return e49.some((e50) => t2.has(e50));
  }
  possiblyEvaluate(e49, t2, n2) {
    return this.property.possiblyEvaluate(this, e49, t2, n2);
  }
}, Co = class {
  constructor(e49, t2, n2) {
    this.property = e49, this.value = new So(e49, void 0, t2, n2);
  }
  transitioned(e49, t2) {
    return t2.value === this.value ? t2 : new To(this.property, this.value, t2, w({}, e49.transition, this.transition), e49.now);
  }
  untransitioned() {
    return new To(this.property, this.value, null, {}, 0);
  }
}, wo = class {
  constructor(e49, t2, n2) {
    this._properties = e49, this._values = Object.create(e49.defaultTransitionablePropertyValues), this._globalState = n2, this._rootKey = t2;
  }
  _propertyRootKey(e49) {
    return `${this._rootKey}.${String(e49)}`;
  }
  hasProperty(e49) {
    return e49 in this._properties.defaultTransitionablePropertyValues;
  }
  getValue(e49) {
    return O(this._values[e49].value.value);
  }
  setValue(e49, t2) {
    Object.hasOwn(this._values, e49) || (this._values[e49] = new Co(this._values[e49].property, this._propertyRootKey(e49), this._globalState)), this._values[e49].value = new So(this._values[e49].property, t2 === null ? void 0 : O(t2), this._propertyRootKey(e49), this._globalState);
  }
  setValues(e49) {
    for (let t2 in e49) {
      let n2 = e49[t2];
      t2.endsWith(`-transition`) ? this.setTransition(t2.slice(0, -11), n2) : ee(this._values[t2].value.value, n2) || this.setValue(t2, n2);
    }
  }
  getTransition(e49) {
    return O(this._values[e49].transition);
  }
  setTransition(e49, t2) {
    Object.hasOwn(this._values, e49) || (this._values[e49] = new Co(this._values[e49].property, this._propertyRootKey(e49), this._globalState)), this._values[e49].transition = O(t2) || void 0;
  }
  retainPriorGlobalState(e49, t2, n2) {
    for (let r2 of Object.keys(n2._values)) for (let i2 = n2._values[r2]; i2; i2 = i2.prior) {
      let { value: n3 } = i2;
      n3.property.specification.transition && !n3.isDataDriven() && n3.expression._globalState === this._globalState && n3.readsGlobalState(e49) && (i2.value = new So(n3.property, n3.value, this._propertyRootKey(r2), t2));
    }
  }
  applyGlobalStateChange(e49, t2, n2, r2) {
    this.retainPriorGlobalState(e49, t2, n2);
    let i2 = false;
    for (let t3 of Object.keys(this._values)) {
      let { value: n3 } = this._values[t3];
      n3.readsGlobalState(e49) && (this.setValue(t3, n3.value), i2 = true);
    }
    return i2 ? this.transitioned(r2, n2) : n2;
  }
  serialize() {
    let e49 = {};
    for (let t2 of Object.keys(this._values)) {
      let n2 = this.getValue(t2);
      n2 !== void 0 && (e49[t2] = n2);
      let r2 = this.getTransition(t2);
      r2 !== void 0 && (e49[`${t2}-transition`] = r2);
    }
    return e49;
  }
  transitioned(e49, t2) {
    let n2 = new Eo(this._properties);
    for (let r2 of Object.keys(this._values)) n2._values[r2] = this._values[r2].transitioned(e49, t2._values[r2]);
    return n2;
  }
  untransitioned() {
    let e49 = new Eo(this._properties);
    for (let t2 of Object.keys(this._values)) e49._values[t2] = this._values[t2].untransitioned();
    return e49;
  }
}, To = class {
  constructor(e49, t2, n2, r2, i2) {
    this.property = e49, this.value = t2, this.begin = i2 + r2.delay || 0, this.end = this.begin + r2.duration || 0, e49.specification.transition && (r2.delay || r2.duration) && (this.prior = n2);
  }
  possiblyEvaluate(e49, t2, n2) {
    let r2 = e49.now || 0, i2 = this.value.possiblyEvaluate(e49, t2, n2), a2 = this.prior;
    if (!a2) return i2;
    if (r2 > this.end || this.value.isDataDriven()) return this.prior = null, i2;
    if (r2 < this.begin) return a2.possiblyEvaluate(e49, t2, n2);
    {
      let o2 = (r2 - this.begin) / (this.end - this.begin);
      return this.property.interpolate(a2.possiblyEvaluate(e49, t2, n2), i2, b(o2));
    }
  }
}, Eo = class {
  constructor(e49) {
    this._properties = e49, this._values = Object.create(e49.defaultTransitioningPropertyValues);
  }
  possiblyEvaluate(e49, t2, n2) {
    let r2 = new ko(this._properties);
    for (let i2 of Object.keys(this._values)) r2._values[i2] = this._values[i2].possiblyEvaluate(e49, t2, n2);
    return r2;
  }
  hasTransition() {
    for (let e49 of Object.keys(this._values)) if (this._values[e49].prior) return true;
    return false;
  }
}, Do = class {
  constructor(e49, t2, n2) {
    this._properties = e49, this._values = Object.create(e49.defaultPropertyValues), this._globalState = n2, this._rootKey = t2;
  }
  _propertyRootKey(e49) {
    return `${this._rootKey}.${String(e49)}`;
  }
  hasValue(e49) {
    return this._values[e49].value !== void 0;
  }
  hasProperty(e49) {
    return e49 in this._properties.defaultPropertyValues;
  }
  getValue(e49) {
    return O(this._values[e49].value);
  }
  setValue(e49, t2) {
    this._values[e49] = new So(this._values[e49].property, t2 === null ? void 0 : O(t2), this._propertyRootKey(e49), this._globalState);
  }
  serialize() {
    let e49 = {};
    for (let t2 of Object.keys(this._values)) {
      let n2 = this.getValue(t2);
      n2 !== void 0 && (e49[t2] = n2);
    }
    return e49;
  }
  possiblyEvaluate(e49, t2, n2) {
    let r2 = new ko(this._properties);
    for (let i2 of Object.keys(this._values)) r2._values[i2] = this._values[i2].possiblyEvaluate(e49, t2, n2);
    return r2;
  }
}, Oo = class {
  constructor(e49, t2, n2) {
    this.property = e49, this.value = t2, this.parameters = n2;
  }
  isConstant() {
    return this.value.kind === `constant`;
  }
  constantOr(e49) {
    return this.value.kind === `constant` ? this.value.value : e49;
  }
  evaluate(e49, t2, n2, r2) {
    return this.property.evaluate(this.value, this.parameters, e49, t2, n2, r2);
  }
}, ko = class {
  constructor(e49) {
    this._properties = e49, this._values = Object.create(e49.defaultPossiblyEvaluatedValues);
  }
  get(e49) {
    return this._values[e49];
  }
};
function Ao(e49) {
  if (Array.isArray(e49)) return e49.length;
  let t2 = e49 == null ? void 0 : e49.values;
  return Array.isArray(t2) ? t2.length : void 0;
}
function jo(e49, t2) {
  let n2 = Ao(e49), r2 = Ao(t2);
  return n2 !== void 0 && r2 !== void 0 && n2 !== r2;
}
var q = class {
  constructor(e49, t2) {
    this.specification = e49, this.name = t2;
  }
  possiblyEvaluate(e49, t2) {
    if (e49.isDataDriven()) throw Error(`Value should not be data driven`);
    return e49.expression.evaluate(t2);
  }
  interpolate(e49, t2, n2) {
    if (jo(e49, t2)) return A(`Property "${this.name}" is trying to interpolate arrays of different lengths. Rendering may 'jump'.`), t2;
    let r2 = this.specification.type, i2 = At[r2];
    return i2 ? i2(e49, t2, n2) : e49;
  }
}, J = class {
  constructor(e49, t2, n2) {
    this.specification = e49, this.name = t2, this.overrides = n2;
  }
  possiblyEvaluate(e49, t2, n2, r2) {
    return e49.expression.kind === `constant` || e49.expression.kind === `camera` ? new Oo(this, { kind: `constant`, value: e49.expression.evaluate(t2, null, {}, n2, r2) }, t2) : new Oo(this, e49.expression, t2);
  }
  interpolate(e49, t2, n2) {
    if (e49.value.kind !== `constant` || t2.value.kind !== `constant`) return e49;
    if (e49.value.value === void 0 || t2.value.value === void 0) return new Oo(this, { kind: `constant`, value: void 0 }, e49.parameters);
    if (jo(e49.value.value, t2.value.value)) return A(`Property "${this.name}" is trying to interpolate arrays of different lengths. Rendering may 'jump'.`), t2;
    let r2 = this.specification.type, i2 = At[r2];
    if (i2) {
      let r3 = i2(e49.value.value, t2.value.value, n2);
      return new Oo(this, { kind: `constant`, value: r3 }, e49.parameters);
    }
    return e49;
  }
  evaluate(e49, t2, n2, r2, i2, a2) {
    return e49.kind === `constant` ? e49.value : e49.evaluate(t2, n2, r2, i2, a2);
  }
}, Mo = class extends J {
  possiblyEvaluate(e49, t2, n2, r2) {
    if (e49.value === void 0) return new Oo(this, { kind: `constant`, value: void 0 }, t2);
    if (e49.expression.kind === `constant`) {
      let i2 = e49.expression.evaluate(t2, null, {}, n2, r2), a2 = e49.property.specification.type === `resolvedImage` && typeof i2 != `string` ? i2.name : i2, o2 = this._calculate(a2, a2, a2, t2);
      return new Oo(this, { kind: `constant`, value: o2 }, t2);
    }
    if (e49.expression.kind === `camera`) {
      let n3 = this._calculate(e49.expression.evaluate({ zoom: t2.zoom - 1 }), e49.expression.evaluate({ zoom: t2.zoom }), e49.expression.evaluate({ zoom: t2.zoom + 1 }), t2);
      return new Oo(this, { kind: `constant`, value: n3 }, t2);
    }
    return new Oo(this, e49.expression, t2);
  }
  evaluate(e49, t2, n2, r2, i2, a2) {
    if (e49.kind === `source`) {
      let o2 = e49.evaluate(t2, n2, r2, i2, a2);
      return this._calculate(o2, o2, o2, t2);
    }
    return e49.kind === `composite` ? this._calculate(e49.evaluate({ zoom: Math.floor(t2.zoom) - 1 }, n2, r2), e49.evaluate({ zoom: Math.floor(t2.zoom) }, n2, r2), e49.evaluate({ zoom: Math.floor(t2.zoom) + 1 }, n2, r2), t2) : e49.value;
  }
  _calculate(e49, t2, n2, r2) {
    return r2.zoom > r2.zoomHistory.lastIntegerZoom ? { from: e49, to: t2 } : { from: n2, to: t2 };
  }
  interpolate(e49) {
    return e49;
  }
}, No = class {
  constructor(e49, t2) {
    this.specification = e49, this.name = t2;
  }
  possiblyEvaluate(e49, t2, n2, r2) {
    if (e49.value !== void 0) {
      if (e49.expression.kind === `constant`) {
        let i2 = e49.expression.evaluate(t2, null, {}, n2, r2);
        return this._calculate(i2, i2, i2, t2);
      }
      return this._calculate(e49.expression.evaluate(new K(Math.floor(t2.zoom - 1), t2)), e49.expression.evaluate(new K(Math.floor(t2.zoom), t2)), e49.expression.evaluate(new K(Math.floor(t2.zoom + 1), t2)), t2);
    }
  }
  _calculate(e49, t2, n2, r2) {
    return r2.zoom > r2.zoomHistory.lastIntegerZoom ? { from: e49, to: t2 } : { from: n2, to: t2 };
  }
  interpolate(e49) {
    return e49;
  }
}, Po = class {
  constructor(e49, t2) {
    this.specification = e49, this.name = t2;
  }
  possiblyEvaluate(e49, t2, n2, r2) {
    return !!e49.expression.evaluate(t2, null, {}, n2, r2);
  }
  interpolate() {
    return false;
  }
}, Fo = class {
  constructor(e49) {
    this.properties = e49, this.defaultPropertyValues = {}, this.defaultTransitionablePropertyValues = {}, this.defaultTransitioningPropertyValues = {}, this.defaultPossiblyEvaluatedValues = {}, this.overridableProperties = [];
    for (let t2 in e49) {
      let n2 = e49[t2];
      n2.specification.overridable && this.overridableProperties.push(t2);
      let r2 = this.defaultPropertyValues[t2] = new So(n2, void 0, n2.name, void 0), i2 = this.defaultTransitionablePropertyValues[t2] = new Co(n2, n2.name, void 0);
      this.defaultTransitioningPropertyValues[t2] = i2.untransitioned(), this.defaultPossiblyEvaluatedValues[t2] = r2.possiblyEvaluate({});
    }
  }
};
G(`DataDrivenProperty`, J), G(`DataConstantProperty`, q), G(`CrossFadedDataDrivenProperty`, Mo), G(`CrossFadedProperty`, No), G(`ColorRampProperty`, Po);
const Io = ` is a PAINT property not a LAYOUT property. Use get/setPaintProperty instead?`, Lo = ` is a LAYOUT property not a PAINT property. Use get/setLayoutProperty instead?`;
var Ro = class extends _o {
  constructor(e49, t2, n2) {
    if (super(), this.id = e49.id, this.type = e49.type, this._globalState = n2, this._featureFilter = { filter: () => true, needGeometry: false, getGlobalStateRefs: () => /* @__PURE__ */ new Set() }, this._visibilityExpression = Ha(this.visibility, `layers[${this.id}].layout.visibility`, n2), e49.type !== `custom` && (this.metadata = e49.metadata, this.minzoom = e49.minzoom, this.maxzoom = e49.maxzoom, e49.type !== `background` && (this.source = e49.source, this.sourceLayer = e49[`source-layer`], this.filter = e49.filter, this._featureFilter = ji(e49.filter, `layers[${this.id}].filter`, n2)), t2.layout && (this._unevaluatedLayout = new Do(t2.layout, `layers[${this.id}].layout`, n2)), t2.paint)) {
      this._transitionablePaint = new wo(t2.paint, `layers[${this.id}].paint`, n2);
      for (let t3 in e49.paint) this.setPaintProperty(t3, e49.paint[t3], { validate: false });
      for (let t3 in e49.layout) this.setLayoutProperty(t3, e49.layout[t3], { validate: false });
      this._transitioningPaint = this._transitionablePaint.untransitioned(), this.paint = new ko(t2.paint);
    }
  }
  setFilter(e49) {
    this.filter = e49, this._featureFilter = ji(e49, `layers[${this.id}].filter`, this._globalState);
  }
  getCrossfadeParameters() {
    return this._crossfadeParameters;
  }
  getLayoutProperty(e49) {
    var _a3;
    if (e49 === `visibility`) return this.visibility;
    if ((_a3 = this._transitionablePaint) == null ? void 0 : _a3.hasProperty(e49)) throw Error(e49 + Io);
    if (!this._unevaluatedLayout) throw Error(`Cannot get layout property "${e49}" on layer type "${this.type}" which has no layout properties.`);
    return this._unevaluatedLayout.getValue(e49);
  }
  getLayoutAffectingGlobalStateRefs() {
    let e49 = /* @__PURE__ */ new Set();
    for (let t2 of this._visibilityExpression.getGlobalStateRefs()) e49.add(t2);
    if (this._unevaluatedLayout) for (let t2 in this._unevaluatedLayout._values) {
      let n2 = this._unevaluatedLayout._values[t2];
      for (let t3 of n2.getGlobalStateRefs()) e49.add(t3);
    }
    for (let t2 of this._featureFilter.getGlobalStateRefs()) e49.add(t2);
    return e49;
  }
  getPaintAffectingGlobalStateRefs() {
    let e49 = new globalThis.Map();
    if (this._transitionablePaint) for (let t2 in this._transitionablePaint._values) {
      let n2 = this._transitionablePaint._values[t2].value;
      for (let r2 of n2.getGlobalStateRefs()) {
        let i2 = e49.get(r2) ?? [];
        i2.push({ name: t2, value: n2.value }), e49.set(r2, i2);
      }
    }
    return e49;
  }
  getVisibilityAffectingGlobalStateRefs() {
    return this._visibilityExpression.getGlobalStateRefs();
  }
  setLayoutProperty(e49, t2, n2 = {}) {
    var _a3;
    if (e49 === `visibility`) {
      this.visibility = t2, this._visibilityExpression.setValue(t2), this.recalculateVisibility();
      return;
    }
    if ((_a3 = this._transitionablePaint) == null ? void 0 : _a3.hasProperty(e49)) {
      this.fire(new go(/* @__PURE__ */ Error(e49 + Io)));
      return;
    }
    t2 != null && this._validate(vo.layoutProperty, `layers.${this.id}.layout.${e49}`, e49, t2, n2) || this._unevaluatedLayout.setValue(e49, t2);
  }
  getPaintProperty(e49) {
    var _a3, _b3;
    if (e49.endsWith(`-transition`)) {
      let t2 = e49.slice(0, -11);
      if (t2 === `visibility` || ((_a3 = this._unevaluatedLayout) == null ? void 0 : _a3.hasProperty(t2))) throw Error(e49 + Lo);
      return this._transitionablePaint.getTransition(t2);
    }
    if (e49 === `visibility` || ((_b3 = this._unevaluatedLayout) == null ? void 0 : _b3.hasProperty(e49))) throw Error(e49 + Lo);
    return this._transitionablePaint.getValue(e49);
  }
  setPaintProperty(e49, t2, n2 = {}) {
    var _a3;
    if (e49 === `visibility` || ((_a3 = this._unevaluatedLayout) == null ? void 0 : _a3.hasProperty(e49))) return this.fire(new go(/* @__PURE__ */ Error(e49 + Lo))), false;
    if (t2 != null && this._validate(vo.paintProperty, `layers.${this.id}.paint.${e49}`, e49, t2, n2)) return false;
    if (e49.endsWith(`-transition`)) return this._transitionablePaint.setTransition(e49.slice(0, -11), t2 || void 0), false;
    {
      let n3 = this._transitionablePaint._values[e49], r2 = n3.property.specification[`property-type`] === `cross-faded-data-driven`, i2 = n3.value.isDataDriven(), a2 = n3.value;
      this._transitionablePaint.setValue(e49, t2), this._handleSpecialPaintPropertyUpdate(e49);
      let o2 = this._transitionablePaint._values[e49].value;
      return o2.isDataDriven() || i2 || r2 || this._handleOverridablePaintPropertyUpdate(e49, a2, o2);
    }
  }
  _handleSpecialPaintPropertyUpdate(e49) {
  }
  _handleOverridablePaintPropertyUpdate(e49, t2, n2) {
    return false;
  }
  isHidden(e49 = this.minzoom, t2 = false) {
    return this.minzoom && e49 < (t2 ? Math.floor(this.minzoom) : this.minzoom) || this.maxzoom && e49 >= this.maxzoom ? true : this._evaluatedVisibility === `none`;
  }
  updateTransitions(e49) {
    this._transitioningPaint = this._transitionablePaint.transitioned(e49, this._transitioningPaint);
  }
  retainPriorGlobalState(e49, t2) {
    var _a3;
    (_a3 = this._transitionablePaint) == null ? void 0 : _a3.retainPriorGlobalState(e49, t2, this._transitioningPaint);
  }
  hasTransition() {
    return this._transitioningPaint.hasTransition();
  }
  recalculateVisibility() {
    this._evaluatedVisibility = this._visibilityExpression.evaluate();
  }
  recalculate(e49, t2) {
    e49.getCrossfadeParameters && (this._crossfadeParameters = e49.getCrossfadeParameters()), this._unevaluatedLayout && (this.layout = this._unevaluatedLayout.possiblyEvaluate(e49, void 0, t2)), this.paint = this._transitioningPaint.possiblyEvaluate(e49, void 0, t2);
  }
  serialize() {
    var _a3, _b3;
    let e49 = { id: this.id, type: this.type, source: this.source, "source-layer": this.sourceLayer, metadata: this.metadata, minzoom: this.minzoom, maxzoom: this.maxzoom, filter: this.filter, layout: (_a3 = this._unevaluatedLayout) == null ? void 0 : _a3.serialize(), paint: (_b3 = this._transitionablePaint) == null ? void 0 : _b3.serialize() };
    return this.visibility && (e49.layout ||= {}, e49.layout.visibility = this.visibility), D(e49, (e50, t2) => e50 !== void 0 && !(t2 === `layout` && !Object.keys(e50).length) && !(t2 === `paint` && !Object.keys(e50).length));
  }
  _validate(e49, t2, n2, r2, i2 = {}) {
    return bo(this, e49, { key: t2, layerType: this.type, objectKey: n2, value: r2 }, i2);
  }
  is3D() {
    return false;
  }
  isTileClipped() {
    return false;
  }
  hasOffscreenPass() {
    return false;
  }
  resize() {
  }
  isStateDependent() {
    for (let e49 in this.paint._values) {
      let t2 = this.paint.get(e49);
      if (t2 instanceof Oo && Vr(t2.property.specification) && (t2.value.kind === `source` || t2.value.kind === `composite`) && t2.value.isStateDependent) return true;
    }
    return false;
  }
};
const zo = { Int8: Int8Array, Uint8: Uint8Array, Int16: Int16Array, Uint16: Uint16Array, Int32: Int32Array, Uint32: Uint32Array, Float32: Float32Array };
var Bo = class {
  constructor(e49, t2) {
    this._structArray = e49, this._pos1 = t2 * this.size, this._pos2 = this._pos1 / 2, this._pos4 = this._pos1 / 4, this._pos8 = this._pos1 / 8;
  }
}, Vo = class {
  constructor() {
    this.isTransferred = false, this.capacity = -1, this.resize(0);
  }
  static serialize(e49, t2) {
    return e49._trim(), t2 && (e49.isTransferred = true, t2.push(e49.arrayBuffer)), { length: e49.length, arrayBuffer: e49.arrayBuffer };
  }
  static deserialize(e49) {
    let t2 = Object.create(this.prototype);
    return t2.arrayBuffer = e49.arrayBuffer, t2.length = e49.length, t2.capacity = e49.arrayBuffer.byteLength / t2.bytesPerElement, t2._refreshViews(), t2;
  }
  _trim() {
    this.length !== this.capacity && (this.capacity = this.length, this.arrayBuffer = this.arrayBuffer.slice(0, this.length * this.bytesPerElement), this._refreshViews());
  }
  clear() {
    this.length = 0;
  }
  resize(e49) {
    this.reserve(e49), this.length = e49;
  }
  reserve(e49) {
    if (e49 > this.capacity) {
      this.capacity = Math.max(e49, Math.floor(this.capacity * 5), 128), this.arrayBuffer = new ArrayBuffer(this.capacity * this.bytesPerElement);
      let t2 = this.uint8;
      this._refreshViews(), t2 && this.uint8.set(t2);
    }
  }
  _refreshViews() {
    throw Error(`_refreshViews() must be implemented by each concrete StructArray layout`);
  }
  freeBufferAfterUpload() {
    this.arrayBuffer = /* @__PURE__ */ new ArrayBuffer(0), this._refreshViews();
  }
};
function Ho(e49, t2 = 1) {
  let n2 = 0, r2 = 0;
  return { members: e49.map((e50) => {
    let i2 = Uo(e50.type), a2 = n2 = Wo(n2, Math.max(t2, i2)), o2 = e50.components || 1;
    return r2 = Math.max(r2, i2), n2 += i2 * o2, { name: e50.name, type: e50.type, components: o2, offset: a2 };
  }), size: Wo(n2, Math.max(r2, t2)), alignment: t2 };
}
function Uo(e49) {
  return zo[e49].BYTES_PER_ELEMENT;
}
function Wo(e49, t2) {
  return Math.ceil(e49 / t2) * t2;
}
var Go = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2) {
    let n2 = this.length;
    return this.resize(n2 + 1), this.emplace(n2, e49, t2);
  }
  emplace(e49, t2, n2) {
    let r2 = e49 * 2;
    return this.int16[r2 + 0] = t2, this.int16[r2 + 1] = n2, e49;
  }
};
Go.prototype.bytesPerElement = 4, G(`StructArrayLayout2i4`, Go);
var Ko = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2) {
    let r2 = this.length;
    return this.resize(r2 + 1), this.emplace(r2, e49, t2, n2);
  }
  emplace(e49, t2, n2, r2) {
    let i2 = e49 * 3;
    return this.int16[i2 + 0] = t2, this.int16[i2 + 1] = n2, this.int16[i2 + 2] = r2, e49;
  }
};
Ko.prototype.bytesPerElement = 6, G(`StructArrayLayout3i6`, Ko);
var qo = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2) {
    let i2 = this.length;
    return this.resize(i2 + 1), this.emplace(i2, e49, t2, n2, r2);
  }
  emplace(e49, t2, n2, r2, i2) {
    let a2 = e49 * 4;
    return this.int16[a2 + 0] = t2, this.int16[a2 + 1] = n2, this.int16[a2 + 2] = r2, this.int16[a2 + 3] = i2, e49;
  }
};
qo.prototype.bytesPerElement = 8, G(`StructArrayLayout4i8`, qo);
var Jo = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2) {
    let o2 = this.length;
    return this.resize(o2 + 1), this.emplace(o2, e49, t2, n2, r2, i2, a2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = e49 * 6;
    return this.int16[s2 + 0] = t2, this.int16[s2 + 1] = n2, this.int16[s2 + 2] = r2, this.int16[s2 + 3] = i2, this.int16[s2 + 4] = a2, this.int16[s2 + 5] = o2, e49;
  }
};
Jo.prototype.bytesPerElement = 12, G(`StructArrayLayout2i4i12`, Jo);
var Yo = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer), this.int8 = new Int8Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2) {
    let c2 = this.length;
    return this.resize(c2 + 1), this.emplace(c2, e49, t2, n2, r2, i2, a2, o2, s2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
    let l2 = e49 * 5, u2 = e49 * 10;
    return this.int16[l2 + 0] = t2, this.int16[l2 + 1] = n2, this.uint8[u2 + 4] = r2, this.uint8[u2 + 5] = i2, this.uint8[u2 + 6] = a2, this.uint8[u2 + 7] = o2, this.int8[u2 + 8] = s2, this.int8[u2 + 9] = c2, e49;
  }
};
Yo.prototype.bytesPerElement = 10, G(`StructArrayLayout2i4ub2b10`, Yo);
var Xo = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2) {
    let n2 = this.length;
    return this.resize(n2 + 1), this.emplace(n2, e49, t2);
  }
  emplace(e49, t2, n2) {
    let r2 = e49 * 2;
    return this.float32[r2 + 0] = t2, this.float32[r2 + 1] = n2, e49;
  }
};
Xo.prototype.bytesPerElement = 8, G(`StructArrayLayout2f8`, Xo);
var Zo = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2) {
    let u2 = this.length;
    return this.resize(u2 + 1), this.emplace(u2, e49, t2, n2, r2, i2, a2, o2, s2, c2, l2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2) {
    let d2 = e49 * 10;
    return this.uint16[d2 + 0] = t2, this.uint16[d2 + 1] = n2, this.uint16[d2 + 2] = r2, this.uint16[d2 + 3] = i2, this.uint16[d2 + 4] = a2, this.uint16[d2 + 5] = o2, this.uint16[d2 + 6] = s2, this.uint16[d2 + 7] = c2, this.uint16[d2 + 8] = l2, this.uint16[d2 + 9] = u2, e49;
  }
};
Zo.prototype.bytesPerElement = 20, G(`StructArrayLayout10ui20`, Zo);
var Qo = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2) {
    let c2 = this.length;
    return this.resize(c2 + 1), this.emplace(c2, e49, t2, n2, r2, i2, a2, o2, s2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
    let l2 = e49 * 8;
    return this.uint16[l2 + 0] = t2, this.uint16[l2 + 1] = n2, this.uint16[l2 + 2] = r2, this.uint16[l2 + 3] = i2, this.uint16[l2 + 4] = a2, this.uint16[l2 + 5] = o2, this.uint16[l2 + 6] = s2, this.uint16[l2 + 7] = c2, e49;
  }
};
Qo.prototype.bytesPerElement = 16, G(`StructArrayLayout8ui16`, Qo);
var $o = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2) {
    let p2 = this.length;
    return this.resize(p2 + 1), this.emplace(p2, e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2) {
    let m2 = e49 * 14, h2 = e49 * 7;
    return this.int16[m2 + 0] = t2, this.int16[m2 + 1] = n2, this.int16[m2 + 2] = r2, this.int16[m2 + 3] = i2, this.uint16[m2 + 4] = a2, this.uint16[m2 + 5] = o2, this.uint16[m2 + 6] = s2, this.uint16[m2 + 7] = c2, this.int16[m2 + 8] = l2, this.int16[m2 + 9] = u2, this.int16[m2 + 10] = d2, this.int16[m2 + 11] = f2, this.float32[h2 + 6] = p2, e49;
  }
};
$o.prototype.bytesPerElement = 28, G(`StructArrayLayout4i4ui4i1f28`, $o);
var es = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2) {
    let r2 = this.length;
    return this.resize(r2 + 1), this.emplace(r2, e49, t2, n2);
  }
  emplace(e49, t2, n2, r2) {
    let i2 = e49 * 3;
    return this.float32[i2 + 0] = t2, this.float32[i2 + 1] = n2, this.float32[i2 + 2] = r2, e49;
  }
};
es.prototype.bytesPerElement = 12, G(`StructArrayLayout3f12`, es);
var ts = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint32 = new Uint32Array(this.arrayBuffer);
  }
  emplaceBack(e49) {
    let t2 = this.length;
    return this.resize(t2 + 1), this.emplace(t2, e49);
  }
  emplace(e49, t2) {
    let n2 = e49 * 1;
    return this.uint32[n2 + 0] = t2, e49;
  }
};
ts.prototype.bytesPerElement = 4, G(`StructArrayLayout1ul4`, ts);
var ns = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer), this.uint32 = new Uint32Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
    let l2 = this.length;
    return this.resize(l2 + 1), this.emplace(l2, e49, t2, n2, r2, i2, a2, o2, s2, c2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2) {
    let u2 = e49 * 10, d2 = e49 * 5;
    return this.int16[u2 + 0] = t2, this.int16[u2 + 1] = n2, this.int16[u2 + 2] = r2, this.int16[u2 + 3] = i2, this.int16[u2 + 4] = a2, this.int16[u2 + 5] = o2, this.uint32[d2 + 3] = s2, this.uint16[u2 + 8] = c2, this.uint16[u2 + 9] = l2, e49;
  }
};
ns.prototype.bytesPerElement = 20, G(`StructArrayLayout6i1ul2ui20`, ns);
var rs = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2) {
    let o2 = this.length;
    return this.resize(o2 + 1), this.emplace(o2, e49, t2, n2, r2, i2, a2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = e49 * 6;
    return this.int16[s2 + 0] = t2, this.int16[s2 + 1] = n2, this.int16[s2 + 2] = r2, this.int16[s2 + 3] = i2, this.int16[s2 + 4] = a2, this.int16[s2 + 5] = o2, e49;
  }
};
rs.prototype.bytesPerElement = 12, G(`StructArrayLayout2i2i2i12`, rs);
var is = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2) {
    let a2 = this.length;
    return this.resize(a2 + 1), this.emplace(a2, e49, t2, n2, r2, i2);
  }
  emplace(e49, t2, n2, r2, i2, a2) {
    let o2 = e49 * 4, s2 = e49 * 8;
    return this.float32[o2 + 0] = t2, this.float32[o2 + 1] = n2, this.float32[o2 + 2] = r2, this.int16[s2 + 6] = i2, this.int16[s2 + 7] = a2, e49;
  }
};
is.prototype.bytesPerElement = 16, G(`StructArrayLayout2f1f2i16`, is);
var as = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2) {
    let o2 = this.length;
    return this.resize(o2 + 1), this.emplace(o2, e49, t2, n2, r2, i2, a2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = e49 * 16, c2 = e49 * 4, l2 = e49 * 8;
    return this.uint8[s2 + 0] = t2, this.uint8[s2 + 1] = n2, this.float32[c2 + 1] = r2, this.float32[c2 + 2] = i2, this.int16[l2 + 6] = a2, this.int16[l2 + 7] = o2, e49;
  }
};
as.prototype.bytesPerElement = 16, G(`StructArrayLayout2ub2f2i16`, as);
var os = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2) {
    let r2 = this.length;
    return this.resize(r2 + 1), this.emplace(r2, e49, t2, n2);
  }
  emplace(e49, t2, n2, r2) {
    let i2 = e49 * 3;
    return this.uint16[i2 + 0] = t2, this.uint16[i2 + 1] = n2, this.uint16[i2 + 2] = r2, e49;
  }
};
os.prototype.bytesPerElement = 6, G(`StructArrayLayout3ui6`, os);
var ss = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer), this.uint32 = new Uint32Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2) {
    let v2 = this.length;
    return this.resize(v2 + 1), this.emplace(v2, e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2, v2) {
    let y2 = e49 * 26, b2 = e49 * 13, x2 = e49 * 52;
    return this.int16[y2 + 0] = t2, this.int16[y2 + 1] = n2, this.uint16[y2 + 2] = r2, this.uint16[y2 + 3] = i2, this.uint32[b2 + 2] = a2, this.uint32[b2 + 3] = o2, this.uint32[b2 + 4] = s2, this.uint16[y2 + 10] = c2, this.uint16[y2 + 11] = l2, this.uint16[y2 + 12] = u2, this.float32[b2 + 7] = d2, this.float32[b2 + 8] = f2, this.uint8[x2 + 36] = p2, this.uint8[x2 + 37] = m2, this.uint8[x2 + 38] = h2, this.uint32[b2 + 10] = g2, this.int16[y2 + 22] = _2, this.float32[b2 + 12] = v2, e49;
  }
};
ss.prototype.bytesPerElement = 52, G(`StructArrayLayout2i2ui3ul3ui2f3ub1ul1i1f52`, ss);
var cs = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.int16 = new Int16Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer), this.uint32 = new Uint32Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2, v2, y2, b2, x2, S2, C2, w2, T2, E2, D2, ee2) {
    let O2 = this.length;
    return this.resize(O2 + 1), this.emplace(O2, e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2, v2, y2, b2, x2, S2, C2, w2, T2, E2, D2, ee2);
  }
  emplace(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2, v2, y2, b2, x2, S2, C2, w2, T2, E2, D2, ee2, O2) {
    let k2 = e49 * 34, A2 = e49 * 17;
    return this.int16[k2 + 0] = t2, this.int16[k2 + 1] = n2, this.int16[k2 + 2] = r2, this.int16[k2 + 3] = i2, this.int16[k2 + 4] = a2, this.int16[k2 + 5] = o2, this.int16[k2 + 6] = s2, this.int16[k2 + 7] = c2, this.uint16[k2 + 8] = l2, this.uint16[k2 + 9] = u2, this.uint16[k2 + 10] = d2, this.uint16[k2 + 11] = f2, this.uint16[k2 + 12] = p2, this.uint16[k2 + 13] = m2, this.uint16[k2 + 14] = h2, this.uint16[k2 + 15] = g2, this.uint16[k2 + 16] = _2, this.uint16[k2 + 17] = v2, this.uint16[k2 + 18] = y2, this.uint16[k2 + 19] = b2, this.uint16[k2 + 20] = x2, this.uint16[k2 + 21] = S2, this.uint16[k2 + 22] = C2, this.uint32[A2 + 12] = w2, this.float32[A2 + 13] = T2, this.float32[A2 + 14] = E2, this.uint16[k2 + 30] = D2, this.uint16[k2 + 31] = ee2, this.float32[A2 + 16] = O2, e49;
  }
};
cs.prototype.bytesPerElement = 68, G(`StructArrayLayout8i15ui1ul2f2ui1f68`, cs);
var ls = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49) {
    let t2 = this.length;
    return this.resize(t2 + 1), this.emplace(t2, e49);
  }
  emplace(e49, t2) {
    let n2 = e49 * 1;
    return this.float32[n2 + 0] = t2, e49;
  }
};
ls.prototype.bytesPerElement = 4, G(`StructArrayLayout1f4`, ls);
var us = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2) {
    let r2 = this.length;
    return this.resize(r2 + 1), this.emplace(r2, e49, t2, n2);
  }
  emplace(e49, t2, n2, r2) {
    let i2 = e49 * 6, a2 = e49 * 3;
    return this.uint16[i2 + 0] = t2, this.float32[a2 + 1] = n2, this.float32[a2 + 2] = r2, e49;
  }
};
us.prototype.bytesPerElement = 12, G(`StructArrayLayout1ui2f12`, us);
var ds = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint32 = new Uint32Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2) {
    let r2 = this.length;
    return this.resize(r2 + 1), this.emplace(r2, e49, t2, n2);
  }
  emplace(e49, t2, n2, r2) {
    let i2 = e49 * 2, a2 = e49 * 4;
    return this.uint32[i2 + 0] = t2, this.uint16[a2 + 2] = n2, this.uint16[a2 + 3] = r2, e49;
  }
};
ds.prototype.bytesPerElement = 8, G(`StructArrayLayout1ul2ui8`, ds);
var fs = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2) {
    let n2 = this.length;
    return this.resize(n2 + 1), this.emplace(n2, e49, t2);
  }
  emplace(e49, t2, n2) {
    let r2 = e49 * 2;
    return this.uint16[r2 + 0] = t2, this.uint16[r2 + 1] = n2, e49;
  }
};
fs.prototype.bytesPerElement = 4, G(`StructArrayLayout2ui4`, fs);
var ps = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.uint16 = new Uint16Array(this.arrayBuffer);
  }
  emplaceBack(e49) {
    let t2 = this.length;
    return this.resize(t2 + 1), this.emplace(t2, e49);
  }
  emplace(e49, t2) {
    let n2 = e49 * 1;
    return this.uint16[n2 + 0] = t2, e49;
  }
};
ps.prototype.bytesPerElement = 2, G(`StructArrayLayout1ui2`, ps);
var ms = class extends Vo {
  _refreshViews() {
    this.uint8 = new Uint8Array(this.arrayBuffer), this.float32 = new Float32Array(this.arrayBuffer);
  }
  emplaceBack(e49, t2, n2, r2) {
    let i2 = this.length;
    return this.resize(i2 + 1), this.emplace(i2, e49, t2, n2, r2);
  }
  emplace(e49, t2, n2, r2, i2) {
    let a2 = e49 * 4;
    return this.float32[a2 + 0] = t2, this.float32[a2 + 1] = n2, this.float32[a2 + 2] = r2, this.float32[a2 + 3] = i2, e49;
  }
};
ms.prototype.bytesPerElement = 16, G(`StructArrayLayout4f16`, ms);
var hs = class extends Bo {
  get anchorPointX() {
    return this._structArray.int16[this._pos2 + 0];
  }
  get anchorPointY() {
    return this._structArray.int16[this._pos2 + 1];
  }
  get x1() {
    return this._structArray.int16[this._pos2 + 2];
  }
  get y1() {
    return this._structArray.int16[this._pos2 + 3];
  }
  get x2() {
    return this._structArray.int16[this._pos2 + 4];
  }
  get y2() {
    return this._structArray.int16[this._pos2 + 5];
  }
  get featureIndex() {
    return this._structArray.uint32[this._pos4 + 3];
  }
  get sourceLayerIndex() {
    return this._structArray.uint16[this._pos2 + 8];
  }
  get bucketIndex() {
    return this._structArray.uint16[this._pos2 + 9];
  }
  get anchorPoint() {
    return new l(this.anchorPointX, this.anchorPointY);
  }
};
hs.prototype.size = 20;
var gs = class extends ns {
  get(e49) {
    return new hs(this, e49);
  }
};
G(`CollisionBoxArray`, gs);
var _s = class extends Bo {
  get anchorX() {
    return this._structArray.int16[this._pos2 + 0];
  }
  get anchorY() {
    return this._structArray.int16[this._pos2 + 1];
  }
  get glyphStartIndex() {
    return this._structArray.uint16[this._pos2 + 2];
  }
  get numGlyphs() {
    return this._structArray.uint16[this._pos2 + 3];
  }
  get vertexStartIndex() {
    return this._structArray.uint32[this._pos4 + 2];
  }
  get lineStartIndex() {
    return this._structArray.uint32[this._pos4 + 3];
  }
  get lineLength() {
    return this._structArray.uint32[this._pos4 + 4];
  }
  get segment() {
    return this._structArray.uint16[this._pos2 + 10];
  }
  get lowerSize() {
    return this._structArray.uint16[this._pos2 + 11];
  }
  get upperSize() {
    return this._structArray.uint16[this._pos2 + 12];
  }
  get lineOffsetX() {
    return this._structArray.float32[this._pos4 + 7];
  }
  get lineOffsetY() {
    return this._structArray.float32[this._pos4 + 8];
  }
  get writingMode() {
    return this._structArray.uint8[this._pos1 + 36];
  }
  get placedOrientation() {
    return this._structArray.uint8[this._pos1 + 37];
  }
  set placedOrientation(e49) {
    this._structArray.uint8[this._pos1 + 37] = e49;
  }
  get hidden() {
    return this._structArray.uint8[this._pos1 + 38];
  }
  set hidden(e49) {
    this._structArray.uint8[this._pos1 + 38] = e49;
  }
  get crossTileID() {
    return this._structArray.uint32[this._pos4 + 10];
  }
  set crossTileID(e49) {
    this._structArray.uint32[this._pos4 + 10] = e49;
  }
  get associatedIconIndex() {
    return this._structArray.int16[this._pos2 + 22];
  }
  get heightOffset() {
    return this._structArray.float32[this._pos4 + 12];
  }
};
_s.prototype.size = 52;
var vs = class extends ss {
  get(e49) {
    return new _s(this, e49);
  }
};
G(`PlacedSymbolArray`, vs);
var ys = class extends Bo {
  get anchorX() {
    return this._structArray.int16[this._pos2 + 0];
  }
  get anchorY() {
    return this._structArray.int16[this._pos2 + 1];
  }
  get rightJustifiedTextSymbolIndex() {
    return this._structArray.int16[this._pos2 + 2];
  }
  get centerJustifiedTextSymbolIndex() {
    return this._structArray.int16[this._pos2 + 3];
  }
  get leftJustifiedTextSymbolIndex() {
    return this._structArray.int16[this._pos2 + 4];
  }
  get verticalPlacedTextSymbolIndex() {
    return this._structArray.int16[this._pos2 + 5];
  }
  get placedIconSymbolIndex() {
    return this._structArray.int16[this._pos2 + 6];
  }
  get verticalPlacedIconSymbolIndex() {
    return this._structArray.int16[this._pos2 + 7];
  }
  get key() {
    return this._structArray.uint16[this._pos2 + 8];
  }
  get textBoxStartIndex() {
    return this._structArray.uint16[this._pos2 + 9];
  }
  get textBoxEndIndex() {
    return this._structArray.uint16[this._pos2 + 10];
  }
  get verticalTextBoxStartIndex() {
    return this._structArray.uint16[this._pos2 + 11];
  }
  get verticalTextBoxEndIndex() {
    return this._structArray.uint16[this._pos2 + 12];
  }
  get iconBoxStartIndex() {
    return this._structArray.uint16[this._pos2 + 13];
  }
  get iconBoxEndIndex() {
    return this._structArray.uint16[this._pos2 + 14];
  }
  get verticalIconBoxStartIndex() {
    return this._structArray.uint16[this._pos2 + 15];
  }
  get verticalIconBoxEndIndex() {
    return this._structArray.uint16[this._pos2 + 16];
  }
  get featureIndex() {
    return this._structArray.uint16[this._pos2 + 17];
  }
  get numHorizontalGlyphVertices() {
    return this._structArray.uint16[this._pos2 + 18];
  }
  get numVerticalGlyphVertices() {
    return this._structArray.uint16[this._pos2 + 19];
  }
  get numIconVertices() {
    return this._structArray.uint16[this._pos2 + 20];
  }
  get numVerticalIconVertices() {
    return this._structArray.uint16[this._pos2 + 21];
  }
  get useRuntimeCollisionCircles() {
    return this._structArray.uint16[this._pos2 + 22];
  }
  get crossTileID() {
    return this._structArray.uint32[this._pos4 + 12];
  }
  set crossTileID(e49) {
    this._structArray.uint32[this._pos4 + 12] = e49;
  }
  get textBoxScale() {
    return this._structArray.float32[this._pos4 + 13];
  }
  get collisionCircleDiameter() {
    return this._structArray.float32[this._pos4 + 14];
  }
  get textAnchorOffsetStartIndex() {
    return this._structArray.uint16[this._pos2 + 30];
  }
  get textAnchorOffsetEndIndex() {
    return this._structArray.uint16[this._pos2 + 31];
  }
  get heightOffset() {
    return this._structArray.float32[this._pos4 + 16];
  }
};
ys.prototype.size = 68;
var bs = class extends cs {
  get(e49) {
    return new ys(this, e49);
  }
};
G(`SymbolInstanceArray`, bs);
var xs = class extends ls {
  getoffsetX(e49) {
    return this.float32[e49 * 1 + 0];
  }
};
G(`GlyphOffsetArray`, xs);
var Ss = class extends Ko {
  getx(e49) {
    return this.int16[e49 * 3 + 0];
  }
  gety(e49) {
    return this.int16[e49 * 3 + 1];
  }
  gettileUnitDistanceFromAnchor(e49) {
    return this.int16[e49 * 3 + 2];
  }
};
G(`SymbolLineVertexArray`, Ss);
var Cs = class extends Bo {
  get textAnchor() {
    return this._structArray.uint16[this._pos2 + 0];
  }
  get textOffset0() {
    return this._structArray.float32[this._pos4 + 1];
  }
  get textOffset1() {
    return this._structArray.float32[this._pos4 + 2];
  }
};
Cs.prototype.size = 12;
var ws = class extends us {
  get(e49) {
    return new Cs(this, e49);
  }
};
G(`TextAnchorOffsetArray`, ws);
var Ts = class extends Bo {
  get featureIndex() {
    return this._structArray.uint32[this._pos4 + 0];
  }
  get sourceLayerIndex() {
    return this._structArray.uint16[this._pos2 + 2];
  }
  get bucketIndex() {
    return this._structArray.uint16[this._pos2 + 3];
  }
};
Ts.prototype.size = 8;
var Es = class extends ds {
  get(e49) {
    return new Ts(this, e49);
  }
};
G(`FeatureIndexArray`, Es);
var Ds = class extends Go {
}, Os = class extends Go {
}, ks = class extends Go {
}, As = class extends Jo {
}, js = class extends Yo {
}, Ms = class extends Xo {
}, Ns = class extends Zo {
}, Ps = class extends Qo {
}, Fs = class extends $o {
}, Is = class extends es {
}, Ls = class extends ts {
}, Rs = class extends rs {
}, zs = class extends as {
}, Bs = class extends os {
}, Vs = class extends fs {
};
const Hs = Ho([{ name: `a_pos`, components: 2, type: `Int16` }], 4), Us = Hs.members;
Hs.size, Hs.alignment;
var Ws = class e37 {
  constructor(e49 = []) {
    this._forceNewSegmentOnNextPrepare = false, this.segments = e49;
  }
  prepareSegment(t2, n2, r2, i2) {
    let a2 = this.segments[this.segments.length - 1];
    return t2 > e37.MAX_VERTEX_ARRAY_LENGTH && A(`Max vertices per segment is ${e37.MAX_VERTEX_ARRAY_LENGTH}: bucket requested ${t2}. Consider using the \`fillLargeMeshArrays\` function if you require meshes with more than ${e37.MAX_VERTEX_ARRAY_LENGTH} vertices.`), this._forceNewSegmentOnNextPrepare || !a2 || a2.vertexLength + t2 > e37.MAX_VERTEX_ARRAY_LENGTH || a2.sortKey !== i2 ? this.createNewSegment(n2, r2, i2) : a2;
  }
  createNewSegment(e49, t2, n2) {
    let r2 = { vertexOffset: e49.length, primitiveOffset: t2.length, vertexLength: 0, primitiveLength: 0, vaos: {} };
    return n2 !== void 0 && (r2.sortKey = n2), this._forceNewSegmentOnNextPrepare = false, this.segments.push(r2), r2;
  }
  getOrCreateLatestSegment(e49, t2, n2) {
    return this.prepareSegment(0, e49, t2, n2);
  }
  forceNewSegmentOnNextPrepare() {
    this._forceNewSegmentOnNextPrepare = true;
  }
  get() {
    return this.segments;
  }
  destroy() {
    for (let e49 of this.segments) for (let t2 in e49.vaos) e49.vaos[t2].destroy();
  }
  static simpleSegment(t2, n2, r2, i2) {
    return new e37([{ vertexOffset: t2, primitiveOffset: n2, vertexLength: r2, primitiveLength: i2, vaos: {}, sortKey: 0 }]);
  }
};
Ws.MAX_VERTEX_ARRAY_LENGTH = 2 ** 16 - 1, G(`SegmentVector`, Ws);
function Gs(e49, t2) {
  return e49 = S(Math.floor(e49), 0, 255), t2 = S(Math.floor(t2), 0, 255), 256 * e49 + t2;
}
const Ks = Ho([{ name: `a_pattern_from`, components: 4, type: `Uint16` }, { name: `a_pattern_to`, components: 4, type: `Uint16` }, { name: `a_pixel_ratio_from`, components: 1, type: `Uint16` }, { name: `a_pixel_ratio_to`, components: 1, type: `Uint16` }]), qs = Ho([{ name: `a_dasharray_from`, components: 4, type: `Uint16` }, { name: `a_dasharray_to`, components: 4, type: `Uint16` }]);
var Js = /* @__PURE__ */ o(((e49, t2) => {
  function n2(e50, t3) {
    for (var n3 = e50.length & 3, r2 = e50.length - n3, i2 = t3, a2, o2 = 3432918353, s2 = 461845907, c2, l2 = 0; l2 < r2; ) c2 = e50.charCodeAt(l2) & 255 | (e50.charCodeAt(++l2) & 255) << 8 | (e50.charCodeAt(++l2) & 255) << 16 | (e50.charCodeAt(++l2) & 255) << 24, ++l2, c2 = (c2 & 65535) * o2 + (((c2 >>> 16) * o2 & 65535) << 16) & 4294967295, c2 = c2 << 15 | c2 >>> 17, c2 = (c2 & 65535) * s2 + (((c2 >>> 16) * s2 & 65535) << 16) & 4294967295, i2 ^= c2, i2 = i2 << 13 | i2 >>> 19, a2 = (i2 & 65535) * 5 + (((i2 >>> 16) * 5 & 65535) << 16) & 4294967295, i2 = (a2 & 65535) + 27492 + (((a2 >>> 16) + 58964 & 65535) << 16);
    switch (c2 = 0, n3) {
      case 3:
        c2 ^= (e50.charCodeAt(l2 + 2) & 255) << 16;
      case 2:
        c2 ^= (e50.charCodeAt(l2 + 1) & 255) << 8;
      case 1:
        c2 ^= e50.charCodeAt(l2) & 255, c2 = (c2 & 65535) * o2 + (((c2 >>> 16) * o2 & 65535) << 16) & 4294967295, c2 = c2 << 15 | c2 >>> 17, c2 = (c2 & 65535) * s2 + (((c2 >>> 16) * s2 & 65535) << 16) & 4294967295, i2 ^= c2;
    }
    return i2 ^= e50.length, i2 ^= i2 >>> 16, i2 = (i2 & 65535) * 2246822507 + (((i2 >>> 16) * 2246822507 & 65535) << 16) & 4294967295, i2 ^= i2 >>> 13, i2 = (i2 & 65535) * 3266489909 + (((i2 >>> 16) * 3266489909 & 65535) << 16) & 4294967295, i2 ^= i2 >>> 16, i2 >>> 0;
  }
  t2 !== void 0 && (t2.exports = n2);
})), Ys = /* @__PURE__ */ o(((e49, t2) => {
  function n2(e50, t3) {
    for (var n3 = e50.length, r2 = t3 ^ n3, i2 = 0, a2; n3 >= 4; ) a2 = e50.charCodeAt(i2) & 255 | (e50.charCodeAt(++i2) & 255) << 8 | (e50.charCodeAt(++i2) & 255) << 16 | (e50.charCodeAt(++i2) & 255) << 24, a2 = (a2 & 65535) * 1540483477 + (((a2 >>> 16) * 1540483477 & 65535) << 16), a2 ^= a2 >>> 24, a2 = (a2 & 65535) * 1540483477 + (((a2 >>> 16) * 1540483477 & 65535) << 16), r2 = (r2 & 65535) * 1540483477 + (((r2 >>> 16) * 1540483477 & 65535) << 16) ^ a2, n3 -= 4, ++i2;
    switch (n3) {
      case 3:
        r2 ^= (e50.charCodeAt(i2 + 2) & 255) << 16;
      case 2:
        r2 ^= (e50.charCodeAt(i2 + 1) & 255) << 8;
      case 1:
        r2 ^= e50.charCodeAt(i2) & 255, r2 = (r2 & 65535) * 1540483477 + (((r2 >>> 16) * 1540483477 & 65535) << 16);
    }
    return r2 ^= r2 >>> 13, r2 = (r2 & 65535) * 1540483477 + (((r2 >>> 16) * 1540483477 & 65535) << 16), r2 ^= r2 >>> 15, r2 >>> 0;
  }
  t2.exports = n2;
})), Xs = /* @__PURE__ */ c((/* @__PURE__ */ o(((e49, t2) => {
  var n2 = Js(), r2 = Ys();
  t2.exports = n2, t2.exports.murmur3 = n2, t2.exports.murmur2 = r2;
})))(), 1), Zs = class e38 {
  constructor() {
    this.ids = [], this.positions = [], this.indexed = false;
  }
  add(e49, t2, n2, r2) {
    this.ids.push(Qs(e49)), this.positions.push(t2, n2, r2);
  }
  getPositions(e49) {
    if (!this.indexed) throw Error(`Trying to get index, but feature positions are not indexed`);
    let t2 = Qs(e49), n2 = 0, r2 = this.ids.length - 1;
    for (; n2 < r2; ) {
      let e50 = n2 + r2 >> 1;
      this.ids[e50] >= t2 ? r2 = e50 : n2 = e50 + 1;
    }
    let i2 = [];
    for (; this.ids[n2] === t2; ) {
      let e50 = this.positions[3 * n2], t3 = this.positions[3 * n2 + 1], r3 = this.positions[3 * n2 + 2];
      i2.push({ index: e50, start: t3, end: r3 }), n2++;
    }
    return i2;
  }
  static serialize(e49, t2) {
    let n2 = new Float64Array(e49.ids), r2 = new Uint32Array(e49.positions);
    return $s(n2, r2, 0, n2.length - 1), t2 && t2.push(n2.buffer, r2.buffer), { ids: n2, positions: r2 };
  }
  static deserialize(t2) {
    let n2 = new e38();
    return n2.ids = t2.ids, n2.positions = t2.positions, n2.indexed = true, n2;
  }
};
function Qs(e49) {
  let t2 = +e49;
  return !isNaN(t2) && t2 <= 2 ** 53 - 1 ? t2 : (0, Xs.default)(String(e49));
}
function $s(e49, t2, n2, r2) {
  for (; n2 < r2; ) {
    let i2 = e49[n2 + r2 >> 1], a2 = n2 - 1, o2 = r2 + 1;
    for (; ; ) {
      do
        a2++;
      while (e49[a2] < i2);
      do
        o2--;
      while (e49[o2] > i2);
      if (a2 >= o2) break;
      ec(e49, a2, o2), ec(t2, 3 * a2, 3 * o2), ec(t2, 3 * a2 + 1, 3 * o2 + 1), ec(t2, 3 * a2 + 2, 3 * o2 + 2);
    }
    o2 - n2 < r2 - o2 ? ($s(e49, t2, n2, o2), n2 = o2 + 1) : ($s(e49, t2, o2 + 1, r2), r2 = o2);
  }
}
function ec(e49, t2, n2) {
  let r2 = e49[t2];
  e49[t2] = e49[n2], e49[n2] = r2;
}
G(`FeaturePositionMap`, Zs);
var tc = class {
  constructor(e49, t2) {
    this.gl = e49.gl, this.location = t2;
  }
}, nc = class extends tc {
  constructor(e49, t2) {
    super(e49, t2), this.current = 0;
  }
  set(e49) {
    this.current !== e49 && (this.current = e49, this.gl.uniform1f(this.location, e49));
  }
}, rc = class extends tc {
  constructor(e49, t2) {
    super(e49, t2), this.current = [0, 0, 0, 0];
  }
  set(e49) {
    (e49[0] !== this.current[0] || e49[1] !== this.current[1] || e49[2] !== this.current[2] || e49[3] !== this.current[3]) && (this.current = e49, this.gl.uniform4f(this.location, e49[0], e49[1], e49[2], e49[3]));
  }
}, ic = class extends tc {
  constructor(e49, t2) {
    super(e49, t2), this.current = V.transparent;
  }
  set(e49) {
    (e49.r !== this.current.r || e49.g !== this.current.g || e49.b !== this.current.b || e49.a !== this.current.a) && (this.current = e49, this.gl.uniform4f(this.location, e49.r, e49.g, e49.b, e49.a));
  }
};
function ac(e49) {
  return [Gs(255 * e49.r, 255 * e49.g), Gs(255 * e49.b, 255 * e49.a)];
}
var oc = class {
  constructor(e49, t2, n2) {
    this.value = e49, this.uniformNames = t2.map((e50) => `u_${e50}`), this.type = n2;
  }
  setUniform(e49, t2, n2) {
    e49.set(n2.constantOr(this.value));
  }
  getBinding(e49, t2, n2) {
    return this.type === `color` ? new ic(e49, t2) : new nc(e49, t2);
  }
}, sc = class {
  constructor(e49, t2) {
    this.uniformNames = t2.map((e50) => `u_${e50}`), this.patternFrom = null, this.patternTo = null, this.pixelRatioFrom = 1, this.pixelRatioTo = 1;
  }
  setConstantPatternPositions(e49, t2) {
    this.pixelRatioFrom = t2.pixelRatio, this.pixelRatioTo = e49.pixelRatio, this.patternFrom = t2.tlbr, this.patternTo = e49.tlbr;
  }
  setConstantDashPositions(e49, t2) {
    this.dashTo = [0, e49.y, e49.height, e49.width], this.dashFrom = [0, t2.y, t2.height, t2.width];
  }
  setUniform(e49, t2, n2, r2) {
    let i2 = null;
    r2 === `u_pattern_to` ? i2 = this.patternTo : r2 === `u_pattern_from` ? i2 = this.patternFrom : r2 === `u_dasharray_to` ? i2 = this.dashTo : r2 === `u_dasharray_from` ? i2 = this.dashFrom : r2 === `u_pixel_ratio_to` ? i2 = this.pixelRatioTo : r2 === `u_pixel_ratio_from` && (i2 = this.pixelRatioFrom), i2 !== null && e49.set(i2);
  }
  getBinding(e49, t2, n2) {
    return n2.startsWith(`u_pattern`) || n2.startsWith(`u_dasharray_`) ? new rc(e49, t2) : new nc(e49, t2);
  }
}, cc = class {
  constructor(e49, t2, n2, r2) {
    this.expression = e49, this.type = n2, this.maxValue = 0, this.paintVertexAttributes = t2.map((e50) => ({ name: `a_${e50}`, type: `Float32`, components: n2 === `color` ? 2 : 1, offset: 0 })), this.paintVertexArray = new r2();
  }
  populatePaintArray(e49, t2, n2) {
    let r2 = this.paintVertexArray.length, i2 = this.expression.evaluate(new K(0, n2), t2, {}, n2.canonical, [], n2.formattedSection);
    this.paintVertexArray.resize(e49), this._setPaintValue(r2, e49, i2);
  }
  updatePaintArray(e49, t2, n2, r2, i2) {
    let a2 = this.expression.evaluate(new K(0, i2), n2, r2);
    this._setPaintValue(e49, t2, a2);
  }
  _setPaintValue(e49, t2, n2) {
    if (this.type === `color`) {
      let r2 = ac(n2);
      for (let n3 = e49; n3 < t2; n3++) this.paintVertexArray.emplace(n3, r2[0], r2[1]);
    } else {
      for (let r2 = e49; r2 < t2; r2++) this.paintVertexArray.emplace(r2, n2);
      this.maxValue = Math.max(this.maxValue, Math.abs(n2));
    }
  }
  upload(e49) {
    var _a3, _b3;
    ((_a3 = this.paintVertexArray) == null ? void 0 : _a3.arrayBuffer.byteLength) && (((_b3 = this.paintVertexBuffer) == null ? void 0 : _b3.buffer) ? this.paintVertexBuffer.updateData(this.paintVertexArray) : this.paintVertexBuffer = e49.createVertexBuffer(this.paintVertexArray, this.paintVertexAttributes, this.expression.isStateDependent));
  }
  destroy() {
    this.paintVertexBuffer && this.paintVertexBuffer.destroy();
  }
}, lc = class {
  constructor(e49, t2, n2, r2, i2, a2) {
    this.expression = e49, this.uniformNames = t2.map((e50) => `u_${e50}_t`), this.type = n2, this.useIntegerZoom = r2, this.zoom = i2, this.maxValue = 0, this.paintVertexAttributes = t2.map((e50) => ({ name: `a_${e50}`, type: `Float32`, components: n2 === `color` ? 4 : 2, offset: 0 })), this.paintVertexArray = new a2();
  }
  populatePaintArray(e49, t2, n2) {
    let r2 = this.expression.evaluate(new K(this.zoom, n2), t2, {}, n2.canonical, [], n2.formattedSection), i2 = this.expression.evaluate(new K(this.zoom + 1, n2), t2, {}, n2.canonical, [], n2.formattedSection), a2 = this.paintVertexArray.length;
    this.paintVertexArray.resize(e49), this._setPaintValue(a2, e49, r2, i2);
  }
  updatePaintArray(e49, t2, n2, r2, i2) {
    let a2 = this.expression.evaluate(new K(this.zoom, i2), n2, r2), o2 = this.expression.evaluate(new K(this.zoom + 1, i2), n2, r2);
    this._setPaintValue(e49, t2, a2, o2);
  }
  _setPaintValue(e49, t2, n2, r2) {
    if (this.type === `color`) {
      let i2 = ac(n2), a2 = ac(r2);
      for (let n3 = e49; n3 < t2; n3++) this.paintVertexArray.emplace(n3, i2[0], i2[1], a2[0], a2[1]);
    } else {
      for (let i2 = e49; i2 < t2; i2++) this.paintVertexArray.emplace(i2, n2, r2);
      this.maxValue = Math.max(this.maxValue, Math.abs(n2), Math.abs(r2));
    }
  }
  upload(e49) {
    var _a3, _b3;
    ((_a3 = this.paintVertexArray) == null ? void 0 : _a3.arrayBuffer.byteLength) && (((_b3 = this.paintVertexBuffer) == null ? void 0 : _b3.buffer) ? this.paintVertexBuffer.updateData(this.paintVertexArray) : this.paintVertexBuffer = e49.createVertexBuffer(this.paintVertexArray, this.paintVertexAttributes, this.expression.isStateDependent));
  }
  destroy() {
    this.paintVertexBuffer && this.paintVertexBuffer.destroy();
  }
  setUniform(e49, t2) {
    let n2 = this.useIntegerZoom ? Math.floor(t2.zoom) : t2.zoom, r2 = S(this.expression.interpolationFactor(n2, this.zoom, this.zoom + 1), 0, 1);
    e49.set(r2);
  }
  getBinding(e49, t2, n2) {
    return new nc(e49, t2);
  }
}, uc = class {
  constructor(e49, t2, n2, r2, i2, a2) {
    this.expression = e49, this.type = t2, this.useIntegerZoom = n2, this.zoom = r2, this.layerId = a2, this.zoomInPaintVertexArray = new i2(), this.zoomOutPaintVertexArray = new i2();
  }
  populatePaintArray(e49, t2, n2) {
    let r2 = this.zoomInPaintVertexArray.length;
    this.zoomInPaintVertexArray.resize(e49), this.zoomOutPaintVertexArray.resize(e49), this._setPaintValues(r2, e49, this.getPositionIds(t2), n2);
  }
  updatePaintArray(e49, t2, n2, r2, i2) {
    this._setPaintValues(e49, t2, this.getPositionIds(n2), i2);
  }
  _setPaintValues(e49, t2, n2, r2) {
    let i2 = this.getPositions(r2);
    if (!i2 || !n2) return;
    let a2 = i2[n2.min], o2 = i2[n2.mid], s2 = i2[n2.max];
    if (a2 && o2 && s2) for (let n3 = e49; n3 < t2; n3++) this.emplace(this.zoomInPaintVertexArray, n3, a2, o2), this.emplace(this.zoomOutPaintVertexArray, n3, s2, o2);
  }
  upload(e49) {
    var _a3, _b3;
    if (((_a3 = this.zoomInPaintVertexArray) == null ? void 0 : _a3.arrayBuffer.byteLength) && ((_b3 = this.zoomOutPaintVertexArray) == null ? void 0 : _b3.arrayBuffer.byteLength)) {
      let t2 = this.getVertexAttributes();
      this.zoomInPaintVertexBuffer = e49.createVertexBuffer(this.zoomInPaintVertexArray, t2, this.expression.isStateDependent), this.zoomOutPaintVertexBuffer = e49.createVertexBuffer(this.zoomOutPaintVertexArray, t2, this.expression.isStateDependent);
    }
  }
  destroy() {
    this.zoomOutPaintVertexBuffer && this.zoomOutPaintVertexBuffer.destroy(), this.zoomInPaintVertexBuffer && this.zoomInPaintVertexBuffer.destroy();
  }
}, dc = class extends uc {
  getPositions(e49) {
    return e49.imagePositions;
  }
  getPositionIds(e49) {
    var _a3;
    return (_a3 = e49.patterns) == null ? void 0 : _a3[this.layerId];
  }
  getVertexAttributes() {
    return Ks.members;
  }
  emplace(e49, t2, n2, r2) {
    e49.emplace(t2, n2.tlbr[0], n2.tlbr[1], n2.tlbr[2], n2.tlbr[3], r2.tlbr[0], r2.tlbr[1], r2.tlbr[2], r2.tlbr[3], n2.pixelRatio, r2.pixelRatio);
  }
}, fc = class extends uc {
  getPositions(e49) {
    return e49.dashPositions;
  }
  getPositionIds(e49) {
    var _a3;
    return (_a3 = e49.dashes) == null ? void 0 : _a3[this.layerId];
  }
  getVertexAttributes() {
    return qs.members;
  }
  emplace(e49, t2, n2, r2) {
    e49.emplace(t2, 0, n2.y, n2.height, n2.width, 0, r2.y, r2.height, r2.width);
  }
}, pc = class {
  constructor(e49, t2, n2) {
    this.binders = {}, this._buffers = [];
    let r2 = [];
    for (let i2 in e49.paint._values) {
      if (!n2(i2)) continue;
      let a2 = e49.paint.get(i2);
      if (!(a2 instanceof Oo) || !Vr(a2.property.specification)) continue;
      let o2 = hc(i2, e49.type), s2 = a2.value, c2 = a2.property.specification.type, l2 = a2.property.useIntegerZoom, u2 = a2.property.specification[`property-type`], d2 = u2 === `cross-faded` || u2 === `cross-faded-data-driven`;
      if (s2.kind === `constant`) this.binders[i2] = d2 ? new sc(s2.value, o2) : new oc(s2.value, o2, c2), r2.push(`/u_${i2}`);
      else if (s2.kind === `source` || d2) {
        let n3 = _c(i2, c2, `source`);
        this.binders[i2] = d2 ? i2 === `line-dasharray` ? new fc(s2, c2, l2, t2, n3, e49.id) : new dc(s2, c2, l2, t2, n3, e49.id) : new cc(s2, o2, c2, n3), r2.push(`/a_${i2}`);
      } else {
        let e50 = _c(i2, c2, `composite`);
        this.binders[i2] = new lc(s2, o2, c2, l2, t2, e50), r2.push(`/z_${i2}`);
      }
    }
    this.cacheKey = r2.sort().join(``);
  }
  getMaxValue(e49) {
    let t2 = this.binders[e49];
    return t2 instanceof cc || t2 instanceof lc ? t2.maxValue : 0;
  }
  populatePaintArrays(e49, t2, n2) {
    for (let r2 in this.binders) {
      let i2 = this.binders[r2];
      (i2 instanceof cc || i2 instanceof lc || i2 instanceof uc) && i2.populatePaintArray(e49, t2, n2);
    }
  }
  setConstantPatternPositions(e49, t2) {
    for (let n2 in this.binders) {
      let r2 = this.binders[n2];
      r2 instanceof sc && r2.setConstantPatternPositions(e49, t2);
    }
  }
  setConstantDashPositions(e49, t2) {
    for (let n2 in this.binders) {
      let r2 = this.binders[n2];
      r2 instanceof sc && r2.setConstantDashPositions(e49, t2);
    }
  }
  updatePaintArrays(e49, t2, n2, r2, i2) {
    let a2 = false;
    for (let o2 of e49) {
      let e50 = t2.getPositions(o2.id);
      for (let t3 of e50) {
        let e51 = n2.feature(t3.index);
        for (let n3 in this.binders) {
          let s2 = this.binders[n3];
          (s2 instanceof cc || s2 instanceof lc || s2 instanceof uc) && s2.expression.isStateDependent === true && (s2.expression = r2.paint.get(n3).value, s2.updatePaintArray(t3.start, t3.end, e51, o2.state, i2), a2 = true);
        }
      }
    }
    return a2;
  }
  defines() {
    let e49 = [];
    for (let t2 in this.binders) {
      let n2 = this.binders[t2];
      (n2 instanceof oc || n2 instanceof sc) && e49.push(...n2.uniformNames.map((e50) => `#define HAS_UNIFORM_${e50}`));
    }
    return e49;
  }
  getBinderAttributes() {
    let e49 = [];
    for (let t2 in this.binders) {
      let n2 = this.binders[t2];
      if (n2 instanceof cc || n2 instanceof lc) for (let t3 of n2.paintVertexAttributes) e49.push(t3.name);
      else if (n2 instanceof uc) {
        let t3 = n2.getVertexAttributes();
        for (let n3 of t3) e49.push(n3.name);
      }
    }
    return e49;
  }
  getBinderUniforms() {
    let e49 = [];
    for (let t2 in this.binders) {
      let n2 = this.binders[t2];
      if (n2 instanceof oc || n2 instanceof sc || n2 instanceof lc) for (let t3 of n2.uniformNames) e49.push(t3);
    }
    return e49;
  }
  getPaintVertexBuffers() {
    return this._buffers;
  }
  getUniforms(e49, t2) {
    let n2 = [];
    for (let r2 in this.binders) {
      let i2 = this.binders[r2];
      if (i2 instanceof oc || i2 instanceof sc || i2 instanceof lc) {
        for (let a2 of i2.uniformNames) if (t2[a2]) {
          let o2 = i2.getBinding(e49, t2[a2], a2);
          n2.push({ name: a2, property: r2, binding: o2 });
        }
      }
    }
    return n2;
  }
  setUniforms(e49, t2, n2, r2) {
    for (let { name: e50, property: i2, binding: a2 } of t2) this.binders[i2].setUniform(a2, r2, n2.get(i2), e50);
  }
  updatePaintBuffers(e49) {
    this._buffers = [];
    for (let t2 in this.binders) {
      let n2 = this.binders[t2];
      if (e49 && n2 instanceof uc) {
        let t3 = e49.fromScale === 2 ? n2.zoomInPaintVertexBuffer : n2.zoomOutPaintVertexBuffer;
        t3 && this._buffers.push(t3);
      } else (n2 instanceof cc || n2 instanceof lc) && n2.paintVertexBuffer && this._buffers.push(n2.paintVertexBuffer);
    }
  }
  upload(e49) {
    for (let t2 in this.binders) {
      let n2 = this.binders[t2];
      (n2 instanceof cc || n2 instanceof lc || n2 instanceof uc) && n2.upload(e49);
    }
    this.updatePaintBuffers();
  }
  destroy() {
    for (let e49 in this.binders) {
      let t2 = this.binders[e49];
      (t2 instanceof cc || t2 instanceof lc || t2 instanceof uc) && t2.destroy();
    }
  }
}, mc = class {
  constructor(e49, t2, n2 = () => true) {
    this.programConfigurations = {};
    for (let r2 of e49) this.programConfigurations[r2.id] = new pc(r2, t2, n2);
    this.needsUpload = false, this._featureMap = new Zs(), this._bufferOffset = 0;
  }
  populatePaintArrays(e49, t2, n2, r2) {
    for (let n3 in this.programConfigurations) this.programConfigurations[n3].populatePaintArrays(e49, t2, r2);
    t2.id !== void 0 && this._featureMap.add(t2.id, n2, this._bufferOffset, e49), this._bufferOffset = e49, this.needsUpload = true;
  }
  updatePaintArrays(e49, t2, n2, r2) {
    for (let i2 of n2) this.needsUpload = this.programConfigurations[i2.id].updatePaintArrays(e49, this._featureMap, t2, i2, r2) || this.needsUpload;
  }
  get(e49) {
    return this.programConfigurations[e49];
  }
  upload(e49) {
    if (this.needsUpload) {
      for (let t2 in this.programConfigurations) this.programConfigurations[t2].upload(e49);
      this.needsUpload = false;
    }
  }
  destroy() {
    for (let e49 in this.programConfigurations) this.programConfigurations[e49].destroy();
  }
};
function hc(e49, t2) {
  return { "text-opacity": [`opacity`], "icon-opacity": [`opacity`], "text-color": [`fill_color`], "icon-color": [`fill_color`], "text-halo-color": [`halo_color`], "icon-halo-color": [`halo_color`], "text-halo-blur": [`halo_blur`], "icon-halo-blur": [`halo_blur`], "text-halo-width": [`halo_width`], "icon-halo-width": [`halo_width`], "line-gap-width": [`gapwidth`], "line-dasharray": [`dasharray_to`, `dasharray_from`], "line-pattern": [`pattern_to`, `pattern_from`, `pixel_ratio_to`, `pixel_ratio_from`], "fill-pattern": [`pattern_to`, `pattern_from`, `pixel_ratio_to`, `pixel_ratio_from`], "fill-extrusion-pattern": [`pattern_to`, `pattern_from`, `pixel_ratio_to`, `pixel_ratio_from`] }[e49] || [e49.replace(`${t2}-`, ``).replace(/-/g, `_`)];
}
function gc(e49) {
  return { "line-pattern": { source: Ns, composite: Ns }, "fill-pattern": { source: Ns, composite: Ns }, "fill-extrusion-pattern": { source: Ns, composite: Ns }, "line-dasharray": { source: Ps, composite: Ps } }[e49];
}
function _c(e49, t2, n2) {
  var _a3;
  let r2 = { color: { source: Xo, composite: ms }, number: { source: ls, composite: Xo } };
  return ((_a3 = gc(e49)) == null ? void 0 : _a3[n2]) || r2[t2][n2];
}
G(`ConstantBinder`, oc), G(`CrossFadedConstantBinder`, sc), G(`SourceExpressionBinder`, cc), G(`CrossFadedPatternBinder`, dc), G(`CrossFadedDasharrayBinder`, fc), G(`CompositeExpressionBinder`, lc), G(`ProgramConfiguration`, pc, { omit: [`_buffers`] }), G(`ProgramConfigurationSet`, mc);
const vc = 2 ** 14 - 1, yc = -vc - 1;
function bc(e49) {
  let t2 = v / e49.extent, n2 = e49.loadGeometry();
  for (let e50 of n2) for (let n3 of e50) {
    let e51 = Math.round(n3.x * t2), r2 = Math.round(n3.y * t2);
    n3.x = S(e51, yc, vc), n3.y = S(r2, yc, vc), (e51 < n3.x || e51 > n3.x + 1 || r2 < n3.y || r2 > n3.y + 1) && A(`Geometry exceeds allowed extent, reduce your vector tile buffer size`);
  }
  return n2;
}
function xc(e49, t2) {
  return { type: e49.type, id: e49.id, properties: e49.properties, geometry: t2 ? bc(e49) : [] };
}
const Sc = -32768;
function Cc(e49, t2, n2, r2, i2) {
  e49.emplaceBack(Sc + t2 * 8 + r2, Sc + n2 * 8 + i2);
}
var wc = class {
  constructor(e49) {
    this.zoom = e49.zoom, this.overscaling = e49.overscaling, this.layers = e49.layers, this.layerIds = this.layers.map((e50) => e50.id), this.index = e49.index, this.hasDependencies = false, this.layoutVertexArray = new Os(), this.indexArray = new Bs(), this.segments = new Ws(), this.programConfigurations = new mc(e49.layers, e49.zoom), this.stateDependentLayerIds = this.layers.filter((e50) => e50.isStateDependent()).map((e50) => e50.id);
  }
  populate(e49, t2, n2) {
    let r2 = this.layers[0], i2 = [], a2 = null, o2 = false, s2 = r2.type === `heatmap`;
    if (r2.type === `circle`) {
      let e50 = r2;
      a2 = e50.layout.get(`circle-sort-key`), o2 = !a2.isConstant(), s2 ||= e50.paint.get(`circle-pitch-alignment`) === `map`;
    }
    let c2 = s2 ? t2.subdivisionGranularity.circle : 1, l2 = new K(this.zoom), u2 = this.layers[0]._featureFilter.needGeometry;
    for (let { feature: t3, id: r3, index: s3, sourceLayerIndex: c3 } of e49) {
      let e50 = xc(t3, u2);
      if (!this.layers[0]._featureFilter.filter(l2, e50, n2)) continue;
      let d2 = o2 ? a2.evaluate(e50, {}, n2) : void 0, f2 = { id: r3, properties: t3.properties, type: t3.type, sourceLayerIndex: c3, index: s3, geometry: u2 ? e50.geometry : bc(t3), patterns: {}, sortKey: d2 };
      i2.push(f2);
    }
    o2 && i2.sort((e50, t3) => e50.sortKey - t3.sortKey);
    for (let r3 of i2) {
      let { geometry: i3, index: a3, sourceLayerIndex: o3 } = r3, s3 = e49[a3].feature;
      this.addFeature(r3, i3, a3, n2, c2), t2.featureIndex.insert(s3, i3, a3, o3, this.index);
    }
  }
  update(e49, t2, n2) {
    this.stateDependentLayers.length && this.programConfigurations.updatePaintArrays(e49, t2, this.stateDependentLayers, { imagePositions: n2 });
  }
  addFeatures(e49) {
  }
  isEmpty() {
    return this.layoutVertexArray.length === 0;
  }
  uploadPending() {
    return !this.uploaded || this.programConfigurations.needsUpload;
  }
  upload(e49) {
    this.uploaded || (this.layoutVertexBuffer = e49.createVertexBuffer(this.layoutVertexArray, Us), this.indexBuffer = e49.createIndexBuffer(this.indexArray)), this.programConfigurations.upload(e49), this.uploaded = true;
  }
  destroy() {
    this.layoutVertexBuffer && (this.layoutVertexBuffer.destroy(), this.indexBuffer.destroy(), this.programConfigurations.destroy(), this.segments.destroy());
  }
  addFeature(e49, t2, n2, r2, i2 = 1) {
    let a2;
    switch (i2) {
      case 1:
        a2 = [0, 7];
        break;
      case 3:
        a2 = [0, 2, 5, 7];
        break;
      case 5:
        a2 = [0, 1, 3, 4, 6, 7];
        break;
      case 7:
        a2 = [0, 1, 2, 3, 4, 5, 6, 7];
        break;
      default:
        throw Error(`Invalid circle bucket granularity: ${i2}; valid values are 1, 3, 5, 7.`);
    }
    let o2 = a2.length;
    for (let n3 of t2) for (let t3 of n3) {
      let n4 = t3.x, r3 = t3.y;
      if (n4 < 0 || n4 >= 8192 || r3 < 0 || r3 >= 8192) continue;
      let i3 = this.segments.prepareSegment(o2 * o2, this.layoutVertexArray, this.indexArray, e49.sortKey), s2 = i3.vertexLength;
      for (let e50 = 0; e50 < o2; e50++) for (let t4 = 0; t4 < o2; t4++) Cc(this.layoutVertexArray, n4, r3, a2[t4], a2[e50]);
      for (let e50 = 0; e50 < o2 - 1; e50++) for (let t4 = 0; t4 < o2 - 1; t4++) {
        let n5 = s2 + e50 * o2 + t4, r4 = s2 + (e50 + 1) * o2 + t4;
        this.indexArray.emplaceBack(n5, r4 + 1, n5 + 1), this.indexArray.emplaceBack(n5, r4, r4 + 1);
      }
      i3.vertexLength += o2 * o2, i3.primitiveLength += (o2 - 1) * (o2 - 1) * 2;
    }
    this.programConfigurations.populatePaintArrays(this.layoutVertexArray.length, e49, n2, { imagePositions: {}, canonical: r2 });
  }
};
G(`CircleBucket`, wc, { omit: [`layers`] });
function Tc(e49, t2) {
  for (let n2 of e49) if (Fc(t2, n2)) return true;
  for (let n2 of t2) if (Fc(e49, n2)) return true;
  return Ac(e49, t2);
}
function Ec(e49, t2, n2) {
  return Fc(e49, t2) ? true : Mc(t2, e49, n2);
}
function Dc(e49, t2) {
  if (e49.length === 1) return Pc(t2, e49[0]);
  for (let n2 of t2) for (let t3 of n2) if (Fc(e49, t3)) return true;
  for (let n2 of e49) if (Pc(t2, n2)) return true;
  for (let n2 of t2) if (Ac(e49, n2)) return true;
  return false;
}
function Oc(e49, t2, n2) {
  for (let r2 of t2) {
    if (e49.length >= 3) {
      for (let t3 of r2) if (Fc(e49, t3)) return true;
    }
    if (kc(e49, r2, n2)) return true;
  }
  return false;
}
function kc(e49, t2, n2) {
  if (e49.length > 1) {
    if (Ac(e49, t2)) return true;
    for (let r2 of t2) if (Mc(r2, e49, n2)) return true;
  }
  for (let r2 of e49) if (Mc(r2, t2, n2)) return true;
  return false;
}
function Ac(e49, t2) {
  if (e49.length === 0 || t2.length === 0) return false;
  for (let n2 = 0; n2 < e49.length - 1; n2++) {
    let r2 = e49[n2], i2 = e49[n2 + 1];
    for (let e50 = 0; e50 < t2.length - 1; e50++) {
      let n3 = t2[e50], a2 = t2[e50 + 1];
      if (jc(r2, i2, n3, a2)) return true;
    }
  }
  return false;
}
function jc(e49, t2, n2, r2) {
  return te(e49, n2, r2) !== te(t2, n2, r2) && te(e49, t2, n2) !== te(e49, t2, r2);
}
function Mc(e49, t2, n2) {
  let r2 = n2 * n2;
  if (t2.length === 1) return e49.distSqr(t2[0]) < r2;
  for (let n3 = 1; n3 < t2.length; n3++) {
    let i2 = t2[n3 - 1], a2 = t2[n3];
    if (Nc(e49, i2, a2) < r2) return true;
  }
  return false;
}
function Nc(e49, t2, n2) {
  let r2 = t2.distSqr(n2);
  if (r2 === 0) return e49.distSqr(t2);
  let i2 = ((e49.x - t2.x) * (n2.x - t2.x) + (e49.y - t2.y) * (n2.y - t2.y)) / r2;
  return i2 < 0 ? e49.distSqr(t2) : i2 > 1 ? e49.distSqr(n2) : e49.distSqr(n2.sub(t2)._mult(i2)._add(t2));
}
function Pc(e49, t2) {
  let n2 = false, r2, i2, a2;
  for (let o2 of e49) {
    r2 = o2;
    for (let e50 = 0, o3 = r2.length - 1; e50 < r2.length; o3 = e50++) i2 = r2[e50], a2 = r2[o3], i2.y > t2.y != a2.y > t2.y && t2.x < (a2.x - i2.x) * (t2.y - i2.y) / (a2.y - i2.y) + i2.x && (n2 = !n2);
  }
  return n2;
}
function Fc(e49, t2) {
  let n2 = false;
  for (let r2 = 0, i2 = e49.length - 1; r2 < e49.length; i2 = r2++) {
    let a2 = e49[r2], o2 = e49[i2];
    a2.y > t2.y != o2.y > t2.y && t2.x < (o2.x - a2.x) * (t2.y - a2.y) / (o2.y - a2.y) + a2.x && (n2 = !n2);
  }
  return n2;
}
function Ic(e49, t2, n2, r2, i2) {
  for (let a3 of e49) if (t2 <= a3.x && n2 <= a3.y && r2 >= a3.x && i2 >= a3.y) return true;
  let a2 = [new l(t2, n2), new l(t2, i2), new l(r2, i2), new l(r2, n2)];
  if (e49.length > 2) {
    for (let t3 of a2) if (Fc(e49, t3)) return true;
  }
  for (let t3 = 0; t3 < e49.length - 1; t3++) {
    let n3 = e49[t3], r3 = e49[t3 + 1];
    if (Lc(n3, r3, a2)) return true;
  }
  return false;
}
function Lc(e49, t2, n2) {
  let r2 = n2[0], i2 = n2[2];
  if (e49.x < r2.x && t2.x < r2.x || e49.x > i2.x && t2.x > i2.x || e49.y < r2.y && t2.y < r2.y || e49.y > i2.y && t2.y > i2.y) return false;
  let a2 = te(e49, t2, n2[0]);
  return a2 !== te(e49, t2, n2[1]) || a2 !== te(e49, t2, n2[2]) || a2 !== te(e49, t2, n2[3]);
}
function Rc(e49, t2, n2) {
  let r2 = t2.paint.get(e49).value;
  return r2.kind === `constant` ? r2.value : n2.programConfigurations.get(t2.id).getMaxValue(e49);
}
function zc(e49) {
  return Math.sqrt(e49[0] * e49[0] + e49[1] * e49[1]);
}
function Bc(e49, t2, n2, r2, i2) {
  if (!t2[0] && !t2[1]) return e49;
  let a2 = l.convert(t2)._mult(i2);
  n2 === `viewport` && a2._rotate(-r2);
  let o2 = [];
  for (let t3 of e49) o2.push(t3.sub(a2));
  return o2;
}
function Vc(e49) {
  let t2 = [];
  for (let n2 = 0; n2 < e49.length; n2++) {
    let r2 = e49[n2], i2 = t2.at(-1);
    (n2 === 0 || i2 && !r2.equals(i2)) && t2.push(r2);
  }
  return t2;
}
function Hc(e49, t2) {
  let n2 = [];
  for (let r2 of e49) {
    let e50 = Vc(r2), i2 = [];
    for (let n3 = 0; n3 < e50.length; n3++) {
      let r3 = e50[n3], a2 = e50[n3 - 1], o2 = e50[n3 + 1], s2 = n3 === 0 ? new l(0, 0) : r3.sub(a2)._unit()._perp(), c2 = n3 === e50.length - 1 ? new l(0, 0) : o2.sub(r3)._unit()._perp(), u2 = s2._add(c2)._unit(), d2 = u2.x * c2.x + u2.y * c2.y;
      d2 !== 0 && u2._mult(1 / d2), i2.push(u2._mult(t2)._add(r3));
    }
    n2.push(i2);
  }
  return n2;
}
function Uc({ queryGeometry: e49, size: t2 }, n2) {
  return Ec(e49, n2, t2);
}
function Wc({ queryGeometry: e49, size: t2, transform: n2, unwrappedTileID: r2, getElevation: i2 }, a2) {
  return Ec(e49, a2, t2 * (n2.projectTileCoordinates(a2.x, a2.y, r2, i2 == null ? void 0 : i2(a2.x, a2.y)).signedDistanceFromCamera / n2.cameraToCenterDistance));
}
function Gc({ queryGeometry: e49, size: t2, transform: n2, unwrappedTileID: r2, getElevation: i2 }, a2) {
  let o2 = n2.projectTileCoordinates(a2.x, a2.y, r2, i2 == null ? void 0 : i2(a2.x, a2.y)).signedDistanceFromCamera, s2 = t2 * (n2.cameraToCenterDistance / o2);
  return Ec(e49, Jc(a2, n2, r2, i2), s2);
}
function Kc({ queryGeometry: e49, size: t2, transform: n2, unwrappedTileID: r2, getElevation: i2 }, a2) {
  return Ec(e49, Jc(a2, n2, r2, i2), t2);
}
function qc({ queryGeometry: e49, size: t2, transform: n2, unwrappedTileID: r2, getElevation: i2, pitchAlignment: a2 = `map`, pitchScale: o2 = `map` }, s2) {
  let c2 = a2 === `map` ? o2 === `map` ? Uc : Wc : o2 === `map` ? Gc : Kc, l2 = { queryGeometry: e49, size: t2, transform: n2, unwrappedTileID: r2, getElevation: i2 };
  for (let e50 of s2) for (let t3 of e50) if (c2(l2, t3)) return true;
  return false;
}
function Jc(e49, t2, n2, r2) {
  let i2 = t2.projectTileCoordinates(e49.x, e49.y, n2, r2 == null ? void 0 : r2(e49.x, e49.y)).point;
  return new l((i2.x * 0.5 + 0.5) * t2.width, (-i2.y * 0.5 + 0.5) * t2.height);
}
function Yc(e49, t2, n2, r2) {
  return e49.map((e50) => Jc(e50, t2, n2, r2));
}
let Xc;
const Zc = () => Xc ||= new Fo({ "circle-sort-key": new J(M.layout_circle[`circle-sort-key`], `circle-sort-key`) });
let Qc;
const $c = () => Qc ||= new Fo({ "circle-radius": new J(M.paint_circle[`circle-radius`], `circle-radius`), "circle-color": new J(M.paint_circle[`circle-color`], `circle-color`), "circle-blur": new J(M.paint_circle[`circle-blur`], `circle-blur`), "circle-opacity": new J(M.paint_circle[`circle-opacity`], `circle-opacity`), "circle-translate": new q(M.paint_circle[`circle-translate`], `circle-translate`), "circle-translate-anchor": new q(M.paint_circle[`circle-translate-anchor`], `circle-translate-anchor`), "circle-pitch-scale": new q(M.paint_circle[`circle-pitch-scale`], `circle-pitch-scale`), "circle-pitch-alignment": new q(M.paint_circle[`circle-pitch-alignment`], `circle-pitch-alignment`), "circle-stroke-width": new J(M.paint_circle[`circle-stroke-width`], `circle-stroke-width`), "circle-stroke-color": new J(M.paint_circle[`circle-stroke-color`], `circle-stroke-color`), "circle-stroke-opacity": new J(M.paint_circle[`circle-stroke-opacity`], `circle-stroke-opacity`) });
var el = { get paint() {
  return $c();
}, get layout() {
  return Zc();
} }, tl = class extends Ro {
  constructor(e49, t2) {
    super(e49, el, t2);
  }
  createBucket(e49) {
    return new wc(e49);
  }
  queryRadius(e49) {
    let t2 = e49;
    return Rc(`circle-radius`, this, t2) + Rc(`circle-stroke-width`, this, t2) + zc(this.paint.get(`circle-translate`));
  }
  queryIntersectsFeature({ queryGeometry: e49, feature: t2, featureState: n2, geometry: r2, transform: i2, pixelsToTileUnits: a2, unwrappedTileID: o2, getElevation: s2 }) {
    let c2 = Bc(e49, this.paint.get(`circle-translate`), this.paint.get(`circle-translate-anchor`), -i2.bearingInRadians, a2), l2 = this.paint.get(`circle-radius`).evaluate(t2, n2) + this.paint.get(`circle-stroke-width`).evaluate(t2, n2), u2 = this.paint.get(`circle-pitch-scale`), d2 = this.paint.get(`circle-pitch-alignment`), f2, p2;
    return d2 === `map` ? (f2 = c2, p2 = l2 * a2) : (f2 = Yc(c2, i2, o2, s2), p2 = l2), qc({ queryGeometry: f2, size: p2, transform: i2, unwrappedTileID: o2, getElevation: s2, pitchAlignment: d2, pitchScale: u2 }, r2);
  }
}, nl = class extends wc {
};
G(`HeatmapBucket`, nl, { omit: [`layers`] });
let rl;
const il = () => rl ||= new Fo({ "heatmap-radius": new J(M.paint_heatmap[`heatmap-radius`], `heatmap-radius`), "heatmap-weight": new J(M.paint_heatmap[`heatmap-weight`], `heatmap-weight`), "heatmap-intensity": new q(M.paint_heatmap[`heatmap-intensity`], `heatmap-intensity`), "heatmap-color": new Po(M.paint_heatmap[`heatmap-color`], `heatmap-color`), "heatmap-opacity": new q(M.paint_heatmap[`heatmap-opacity`], `heatmap-opacity`) });
var al = { get paint() {
  return il();
} };
function ol(e49, { width: t2, height: n2 }, r2, i2) {
  if (!i2) i2 = new Uint8Array(t2 * n2 * r2);
  else if (i2 instanceof Uint8ClampedArray) i2 = new Uint8Array(i2.buffer);
  else if (i2.length !== t2 * n2 * r2) throw RangeError(`mismatched image size. expected: ${i2.length} but got: ${t2 * n2 * r2}`);
  return e49.width = t2, e49.height = n2, e49.data = i2, e49;
}
function sl(e49, { width: t2, height: n2 }, r2) {
  if (t2 === e49.width && n2 === e49.height) return;
  let i2 = ol({}, { width: t2, height: n2 }, r2);
  cl(e49, i2, { x: 0, y: 0 }, { x: 0, y: 0 }, { width: Math.min(e49.width, t2), height: Math.min(e49.height, n2) }, r2), e49.width = t2, e49.height = n2, e49.data = i2.data;
}
function cl(e49, t2, n2, r2, i2, a2) {
  if (i2.width === 0 || i2.height === 0) return t2;
  if (i2.width > e49.width || i2.height > e49.height || n2.x > e49.width - i2.width || n2.y > e49.height - i2.height) throw RangeError(`out of range source coordinates for image copy`);
  if (i2.width > t2.width || i2.height > t2.height || r2.x > t2.width - i2.width || r2.y > t2.height - i2.height) throw RangeError(`out of range destination coordinates for image copy`);
  let o2 = e49.data, s2 = t2.data;
  if (o2 === s2) throw Error(`srcData equals dstData, so image is already copied`);
  for (let c2 = 0; c2 < i2.height; c2++) {
    let l2 = ((n2.y + c2) * e49.width + n2.x) * a2, u2 = ((r2.y + c2) * t2.width + r2.x) * a2;
    for (let e50 = 0; e50 < i2.width * a2; e50++) s2[u2 + e50] = o2[l2 + e50];
  }
  return t2;
}
var ll = class e39 {
  constructor(e49, t2) {
    ol(this, e49, 1, t2);
  }
  resize(e49) {
    sl(this, e49, 1);
  }
  clone() {
    return new e39({ width: this.width, height: this.height }, new Uint8Array(this.data));
  }
  static copy(e49, t2, n2, r2, i2) {
    cl(e49, t2, n2, r2, i2, 1);
  }
}, ul = class e40 {
  constructor(e49, t2) {
    ol(this, e49, 4, t2);
  }
  resize(e49) {
    sl(this, e49, 4);
  }
  replace(e49, t2) {
    t2 ? this.data.set(e49) : this.data = e49 instanceof Uint8ClampedArray ? new Uint8Array(e49.buffer) : e49;
  }
  clone() {
    return new e40({ width: this.width, height: this.height }, new Uint8Array(this.data));
  }
  static copy(e49, t2, n2, r2, i2) {
    cl(e49, t2, n2, r2, i2, 4);
  }
  setPixel(e49, t2, n2) {
    let r2 = (e49 * this.width + t2) * 4;
    this.data[r2 + 0] = Math.round(n2.r * 255 / n2.a), this.data[r2 + 1] = Math.round(n2.g * 255 / n2.a), this.data[r2 + 2] = Math.round(n2.b * 255 / n2.a), this.data[r2 + 3] = Math.round(n2.a * 255);
  }
};
function dl(e49) {
  let t2 = new Uint8Array(e49.length);
  for (let n2 = 0; n2 < e49.length; n2 += 4) {
    let r2 = e49[n2 + 3];
    t2[n2 + 0] = Math.round(e49[n2 + 0] * r2 / 255), t2[n2 + 1] = Math.round(e49[n2 + 1] * r2 / 255), t2[n2 + 2] = Math.round(e49[n2 + 2] * r2 / 255), t2[n2 + 3] = r2;
  }
  return t2;
}
G(`AlphaImage`, ll), G(`RGBAImage`, ul);
function fl(e49) {
  let t2 = {}, n2 = e49.resolution || 256, r2 = e49.clips ? e49.clips.length : 1, i2 = e49.image || new ul({ width: n2, height: r2 });
  if (!T(n2)) throw Error(`width is not a power of 2 - ${n2}`);
  let a2 = (r3, a3, o2) => {
    t2[e49.evaluationKey] = o2;
    let s2 = e49.expression.evaluate(t2);
    i2.setPixel(r3 / 4 / n2, a3 / 4, s2);
  };
  if (e49.clips) for (let t3 = 0, i3 = 0; t3 < r2; ++t3, i3 += n2 * 4) for (let r3 = 0, o2 = 0; r3 < n2; r3++, o2 += 4) {
    let s2 = r3 / (n2 - 1), { start: c2, end: l2 } = e49.clips[t3], u2 = c2 * (1 - s2) + l2 * s2;
    a2(i3, o2, u2);
  }
  else for (let e50 = 0, t3 = 0; e50 < n2; e50++, t3 += 4) {
    let r3 = e50 / (n2 - 1);
    a2(0, t3, r3);
  }
  return i2;
}
var pl = class extends Ro {
  createBucket(e49) {
    return new nl(e49);
  }
  constructor(e49, t2) {
    super(e49, al, t2), this.heatmapFbos = /* @__PURE__ */ new Map(), this._updateColorRamp();
  }
  _handleSpecialPaintPropertyUpdate(e49) {
    e49 === `heatmap-color` && this._updateColorRamp();
  }
  _updateColorRamp() {
    let e49 = this._transitionablePaint._values[`heatmap-color`].value.expression;
    this.colorRamp = fl({ expression: e49, evaluationKey: `heatmapDensity`, image: this.colorRamp }), this.colorRampTexture = null;
  }
  resize() {
    this.heatmapFbos.has(`big-fb`) && this.heatmapFbos.delete(`big-fb`);
  }
  queryRadius(e49) {
    return Rc(`heatmap-radius`, this, e49);
  }
  queryIntersectsFeature({ queryGeometry: e49, feature: t2, featureState: n2, geometry: r2, transform: i2, pixelsToTileUnits: a2, unwrappedTileID: o2, getElevation: s2 }) {
    return qc({ queryGeometry: e49, size: this.paint.get(`heatmap-radius`).evaluate(t2, n2) * a2, transform: i2, unwrappedTileID: o2, getElevation: s2 }, r2);
  }
  hasOffscreenPass() {
    return this.paint.get(`heatmap-opacity`) !== 0 && !this.isHidden();
  }
};
let ml;
const hl = () => ml ||= new Fo({ "hillshade-illumination-direction": new q(M.paint_hillshade[`hillshade-illumination-direction`], `hillshade-illumination-direction`), "hillshade-illumination-altitude": new q(M.paint_hillshade[`hillshade-illumination-altitude`], `hillshade-illumination-altitude`), "hillshade-illumination-anchor": new q(M.paint_hillshade[`hillshade-illumination-anchor`], `hillshade-illumination-anchor`), "hillshade-exaggeration": new q(M.paint_hillshade[`hillshade-exaggeration`], `hillshade-exaggeration`), "hillshade-shadow-color": new q(M.paint_hillshade[`hillshade-shadow-color`], `hillshade-shadow-color`), "hillshade-highlight-color": new q(M.paint_hillshade[`hillshade-highlight-color`], `hillshade-highlight-color`), "hillshade-accent-color": new q(M.paint_hillshade[`hillshade-accent-color`], `hillshade-accent-color`), "hillshade-method": new q(M.paint_hillshade[`hillshade-method`], `hillshade-method`), resampling: new q(M.paint_hillshade.resampling, `resampling`) });
var gl = { get paint() {
  return hl();
} }, _l = class extends Ro {
  constructor(e49, t2) {
    super(e49, gl, t2), this.recalculate({ zoom: 0, zoomHistory: {} }, void 0);
  }
  getIlluminationProperties() {
    let e49 = this.paint.get(`hillshade-illumination-direction`).values, t2 = this.paint.get(`hillshade-illumination-altitude`).values, n2 = this.paint.get(`hillshade-highlight-color`).values, r2 = this.paint.get(`hillshade-shadow-color`).values, i2 = Math.max(e49.length, t2.length, n2.length, r2.length);
    e49 = e49.concat(Array(i2 - e49.length).fill(e49.at(-1))), t2 = t2.concat(Array(i2 - t2.length).fill(t2.at(-1))), n2 = n2.concat(Array(i2 - n2.length).fill(n2.at(-1))), r2 = r2.concat(Array(i2 - r2.length).fill(r2.at(-1)));
    let a2 = t2.map(de);
    return { directionRadians: e49.map(de), altitudeRadians: a2, shadowColor: r2, highlightColor: n2 };
  }
  hasOffscreenPass() {
    return this.paint.get(`hillshade-exaggeration`) !== 0 && !this.isHidden();
  }
};
let vl;
const yl = () => vl ||= new Fo({ "color-relief-opacity": new q(M[`paint_color-relief`][`color-relief-opacity`], `color-relief-opacity`), "color-relief-color": new Po(M[`paint_color-relief`][`color-relief-color`], `color-relief-color`), resampling: new q(M[`paint_color-relief`].resampling, `resampling`) });
var bl = { get paint() {
  return yl();
} };
function xl(e49) {
  return `data` in e49;
}
var Sl = class {
  constructor(e49, t2, n2, r2) {
    this.context = e49, this.format = n2, this.texture = e49.gl.createTexture(), this._ownedHandle = this.texture, this.update(t2, r2);
  }
  update(e49, t2, n2) {
    var _a3;
    let { width: r2, height: i2 } = e49, a2 = (((_a3 = this.size) == null ? void 0 : _a3[0]) !== r2 || this.size[1] !== i2) && !n2, { context: o2 } = this, { gl: s2 } = o2;
    this.useMipmap = !!(t2 == null ? void 0 : t2.useMipmap), a2 && this.size && this.format === s2.RGBA && (s2.deleteTexture(this.texture), this.texture = s2.createTexture(), this._ownedHandle = this.texture, this.magFilter = void 0, this.minFilter = void 0, this.wrap = void 0), s2.bindTexture(s2.TEXTURE_2D, this.texture), o2.pixelStoreUnpackFlipY.set(false), o2.pixelStoreUnpack.set(1);
    let c2 = this.format === s2.RGBA && (t2 == null ? void 0 : t2.premultiply) !== false;
    if (a2) {
      if (this.size = [r2, i2], this.format === s2.RGBA && r2 > 0 && i2 > 0) {
        let t3 = this.useMipmap ? Math.floor(Math.log2(Math.max(r2, i2))) + 1 : 1;
        if (s2.texStorage2D(s2.TEXTURE_2D, t3, s2.RGBA8, r2, i2), xl(e49)) {
          o2.pixelStoreUnpackPremultiplyAlpha.set(false);
          let { data: t4 } = e49;
          c2 && t4 && (t4 = dl(t4)), t4 && s2.texSubImage2D(s2.TEXTURE_2D, 0, 0, 0, r2, i2, s2.RGBA, s2.UNSIGNED_BYTE, t4);
        } else o2.pixelStoreUnpackPremultiplyAlpha.set(c2), s2.texSubImage2D(s2.TEXTURE_2D, 0, 0, 0, s2.RGBA, s2.UNSIGNED_BYTE, e49);
      } else xl(e49) ? (o2.pixelStoreUnpackPremultiplyAlpha.set(false), this._uploadRawData(e49, c2, r2, i2, s2)) : (o2.pixelStoreUnpackPremultiplyAlpha.set(c2), this._uploadDomImage(e49, s2));
    } else {
      let { x: t3, y: a3 } = n2 || { x: 0, y: 0 };
      xl(e49) ? (o2.pixelStoreUnpackPremultiplyAlpha.set(false), this._updateRawData(e49, c2, t3, a3, r2, i2, s2)) : (o2.pixelStoreUnpackPremultiplyAlpha.set(c2), this._updateDomImage(e49, t3, a3, s2));
    }
    this.useMipmap && !(xl(e49) && e49.data === null) && s2.generateMipmap(s2.TEXTURE_2D), o2.pixelStoreUnpackFlipY.setDefault(), o2.pixelStoreUnpack.setDefault(), o2.pixelStoreUnpackPremultiplyAlpha.setDefault();
  }
  _uploadDomImage(e49, t2) {
    t2.texImage2D(t2.TEXTURE_2D, 0, this.format, this.format, t2.UNSIGNED_BYTE, e49);
  }
  _uploadRawData(e49, t2, n2, r2, i2) {
    let { data: a2 } = e49;
    t2 && a2 && (a2 = dl(a2)), i2.texImage2D(i2.TEXTURE_2D, 0, this.format, n2, r2, 0, this.format, i2.UNSIGNED_BYTE, a2);
  }
  _updateDomImage(e49, t2, n2, r2) {
    r2.texSubImage2D(r2.TEXTURE_2D, 0, t2, n2, r2.RGBA, r2.UNSIGNED_BYTE, e49);
  }
  _updateRawData(e49, t2, n2, r2, i2, a2, o2) {
    let { data: s2 } = e49;
    t2 && s2 && (s2 = dl(s2)), o2.texSubImage2D(o2.TEXTURE_2D, 0, n2, r2, i2, a2, o2.RGBA, o2.UNSIGNED_BYTE, s2);
  }
  bind(e49, t2, n2) {
    let { context: r2 } = this, { gl: i2 } = r2;
    this.texture !== this._ownedHandle && (this.texture = this._ownedHandle), i2.bindTexture(i2.TEXTURE_2D, this.texture), (n2 === i2.LINEAR_MIPMAP_NEAREST || n2 === i2.LINEAR_MIPMAP_LINEAR) && !this.useMipmap && (n2 = i2.LINEAR);
    let a2 = n2 || e49;
    e49 !== this.magFilter && (i2.texParameteri(i2.TEXTURE_2D, i2.TEXTURE_MAG_FILTER, e49), this.magFilter = e49), a2 !== this.minFilter && (i2.texParameteri(i2.TEXTURE_2D, i2.TEXTURE_MIN_FILTER, a2), this.minFilter = a2), t2 !== this.wrap && (i2.texParameteri(i2.TEXTURE_2D, i2.TEXTURE_WRAP_S, t2), i2.texParameteri(i2.TEXTURE_2D, i2.TEXTURE_WRAP_T, t2), this.wrap = t2);
  }
  generateMipmap() {
    if (!this.useMipmap) return;
    let { gl: e49 } = this.context;
    e49.bindTexture(e49.TEXTURE_2D, this.texture), e49.generateMipmap(e49.TEXTURE_2D);
  }
  destroy() {
    let { gl: e49 } = this.context;
    e49.deleteTexture(this.texture), this.texture = null, this._ownedHandle = null;
  }
}, Cl = (_b = class {
  constructor(t2, n2, r2, i2 = 1, a2 = 1, o2 = 1, s2 = 0) {
    if (this.uid = t2, n2.height !== n2.width) throw RangeError(`DEM tiles must be square`);
    if (r2 && ![`mapbox`, `terrarium`, `custom`].includes(r2)) {
      A(`"${r2}" is not a valid encoding type. Valid types include "mapbox", "terrarium" and "custom".`);
      return;
    }
    this.stride = n2.height;
    let c2 = this.dim = n2.height - 4;
    switch (this.data = new Uint32Array(n2.data.buffer), _b.byteViewCache.set(this, new Uint8Array(this.data.buffer)), r2) {
      case `terrarium`:
        this.redFactor = 256, this.greenFactor = 1, this.blueFactor = 1 / 256, this.baseShift = 32768;
        break;
      case `custom`:
        this.redFactor = i2, this.greenFactor = a2, this.blueFactor = o2, this.baseShift = s2;
        break;
      default:
        this.redFactor = 6553.6, this.greenFactor = 25.6, this.blueFactor = 0.1, this.baseShift = 1e4;
    }
    for (let e49 = -2; e49 < c2 + 2; e49++) {
      let t3 = this._idx(0, Math.max(0, Math.min(c2 - 1, e49)));
      (e49 < 0 || e49 >= c2) && this.data.copyWithin(this._idx(0, e49), t3, t3 + c2), this.data.fill(this.data[t3], this._idx(-2, e49), this._idx(0, e49)), this.data.fill(this.data[t3 + c2 - 1], this._idx(c2, e49), this._idx(c2 + 1, e49) + 1);
    }
    let l2 = this._getByteView();
    this.min = 2 ** 53 - 1, this.max = -(2 ** 53 - 1);
    for (let e49 = 0; e49 < c2; e49++) for (let t3 = 0; t3 < c2; t3++) {
      let n3 = this._idx(e49, t3) * 4, r3 = this._unpackAtIndex(l2, n3);
      r3 > this.max && (this.max = r3), r3 < this.min && (this.min = r3);
    }
  }
  get(e49, t2) {
    let n2 = this._getByteView(), r2 = this._idx(e49, t2) * 4;
    return this._unpackAtIndex(n2, r2);
  }
  sampleBilinear(e49, t2) {
    let n2 = Math.floor(e49), r2 = Math.floor(t2);
    if (n2 < -1 || n2 >= this.dim || r2 < -1 || r2 >= this.dim) throw RangeError(`Out of range source coordinates for DEM data. x: ${e49}, y: ${t2}, dim: ${this.dim}`);
    let i2 = this._getByteView(), a2 = ((r2 + 2) * this.stride + n2 + 2) * 4, o2 = this.stride * 4, s2 = e49 - n2, c2 = t2 - r2, l2 = this._unpackAtIndex(i2, a2), u2 = this._unpackAtIndex(i2, a2 + 4), d2 = this._unpackAtIndex(i2, a2 + o2), f2 = this._unpackAtIndex(i2, a2 + o2 + 4);
    return l2 * (1 - s2) * (1 - c2) + u2 * s2 * (1 - c2) + d2 * (1 - s2) * c2 + f2 * s2 * c2;
  }
  getUnpackVector() {
    return [this.redFactor, this.greenFactor, this.blueFactor, this.baseShift];
  }
  _idx(e49, t2) {
    if (e49 < -2 || e49 >= this.dim + 2 || t2 < -2 || t2 >= this.dim + 2) throw RangeError(`Out of range source coordinates for DEM data. x: ${e49}, y: ${t2}, dim: ${this.dim}`);
    return (t2 + 2) * this.stride + (e49 + 2);
  }
  unpack(e49, t2, n2) {
    return e49 * this.redFactor + t2 * this.greenFactor + n2 * this.blueFactor - this.baseShift;
  }
  pack(e49) {
    return wl(e49, this.getUnpackVector());
  }
  getPixels() {
    return new ul({ width: this.stride, height: this.stride }, this._getByteView());
  }
  backfillBorder(e49, t2, n2) {
    if (this.dim !== e49.dim) throw Error(`dem dimension mismatch`);
    let r2 = t2 * this.dim, i2 = t2 * this.dim + this.dim, a2 = n2 * this.dim, o2 = n2 * this.dim + this.dim;
    switch (t2) {
      case -1:
        r2 = i2 - 2;
        break;
      case 1:
        i2 = r2 + 2;
    }
    switch (n2) {
      case -1:
        a2 = o2 - 2;
        break;
      case 1:
        o2 = a2 + 2;
    }
    let s2 = -t2 * this.dim, c2 = -n2 * this.dim;
    for (let t3 = a2; t3 < o2; t3++) for (let n3 = r2; n3 < i2; n3++) this.data[this._idx(n3, t3)] = e49.data[this._idx(n3 + s2, t3 + c2)];
  }
  _getByteView() {
    let t2 = _b.byteViewCache.get(this);
    return (t2 == null ? void 0 : t2.buffer) !== this.data.buffer && (t2 = new Uint8Array(this.data.buffer), _b.byteViewCache.set(this, t2)), t2;
  }
  _unpackAtIndex(e49, t2) {
    return this.unpack(e49[t2], e49[t2 + 1], e49[t2 + 2]);
  }
}, _b.byteViewCache = /* @__PURE__ */ new WeakMap(), _b);
function wl(e49, t2) {
  let n2 = t2[0], r2 = t2[1], i2 = t2[2], a2 = t2[3], o2 = [n2, r2, i2].filter((e50) => e50 !== 0);
  if (o2.length === 0) return { r: 0, g: 0, b: 0 };
  let s2 = Math.min(...o2), c2 = Math.round((e49 + a2) / s2), l2 = (e50) => e50 === 0 ? 0 : Math.floor(c2 * s2 / e50) % 256;
  return { r: l2(n2), g: l2(r2), b: l2(i2) };
}
G(`DEMData`, Cl);
var Tl = class extends Ro {
  constructor(e49, t2) {
    super(e49, bl, t2);
  }
  _createColorRamp(e49) {
    let t2 = { elevationStops: [], colorStops: [] }, n2 = this._transitionablePaint._values[`color-relief-color`].value.expression;
    if (n2 instanceof ui && n2._styleExpression.expression instanceof Ot) {
      this.colorRampExpression = n2;
      let e50 = n2._styleExpression.expression;
      t2.elevationStops = e50.labels, t2.colorStops = [];
      for (let n3 of t2.elevationStops) t2.colorStops.push(e50.evaluate({ globals: { elevation: n3 } }));
    }
    if (t2.elevationStops.length < 1 && (t2.elevationStops = [0], t2.colorStops = [V.transparent]), t2.elevationStops.length < 2 && (t2.elevationStops.push(t2.elevationStops[0] + 1), t2.colorStops.push(t2.colorStops[0])), t2.elevationStops.length <= e49) return t2;
    let r2 = { elevationStops: [], colorStops: [] }, i2 = (t2.elevationStops.length - 1) / (e49 - 1);
    for (let e50 = 0; e50 < t2.elevationStops.length - 0.5; e50 += i2) r2.elevationStops.push(t2.elevationStops[Math.round(e50)]), r2.colorStops.push(t2.colorStops[Math.round(e50)]);
    return A(`Too many colors in specification of ${this.id} color-relief layer, may not render properly. Max possible colors: ${e49}, provided: ${t2.elevationStops.length}`), r2;
  }
  _colorRampChanged() {
    return this.colorRampExpression != this._transitionablePaint._values[`color-relief-color`].value.expression;
  }
  getColorRampTextures(e49, t2, n2) {
    if (this.colorRampTextures && !this._colorRampChanged()) return this.colorRampTextures;
    let r2 = this._createColorRamp(t2), i2 = new ul({ width: r2.colorStops.length, height: 1 }), a2 = new ul({ width: r2.colorStops.length, height: 1 });
    for (let e50 = 0; e50 < r2.elevationStops.length; e50++) {
      let t3 = wl(r2.elevationStops[e50], n2);
      a2.setPixel(0, e50, new V(t3.r / 255, t3.g / 255, t3.b / 255, 1)), i2.setPixel(0, e50, r2.colorStops[e50]);
    }
    return this.colorRampTextures = { elevationTexture: new Sl(e49, a2, e49.gl.RGBA), colorTexture: new Sl(e49, i2, e49.gl.RGBA) }, this.colorRampTextures;
  }
  hasOffscreenPass() {
    return !this.isHidden() && !!this.colorRampTextures;
  }
};
const El = Ho([{ name: `a_pos`, components: 2, type: `Int16` }], 4), Dl = El.members;
El.size, El.alignment;
function Ol(e49, t2, n2) {
  let r2 = n2.patternDependencies, i2 = false;
  for (let n3 of t2) {
    let t3 = n3.paint.get(`${e49}-pattern`);
    t3.isConstant() || (i2 = true);
    let a2 = t3.constantOr(null);
    a2 && (i2 = true, r2[a2.to] = true, r2[a2.from] = true);
  }
  return i2;
}
function kl(e49, t2, n2, r2, i2) {
  let { zoom: a2 } = r2, o2 = i2.patternDependencies;
  for (let r3 of t2) {
    let t3 = r3.paint.get(`${e49}-pattern`).value;
    if (t3.kind !== `constant`) {
      let e50 = t3.evaluate({ zoom: a2 - 1 }, n2, {}, i2.availableImages), s2 = t3.evaluate({ zoom: a2 }, n2, {}, i2.availableImages), c2 = t3.evaluate({ zoom: a2 + 1 }, n2, {}, i2.availableImages);
      e50 = (e50 == null ? void 0 : e50.name) ? e50.name : e50, s2 = (s2 == null ? void 0 : s2.name) ? s2.name : s2, c2 = (c2 == null ? void 0 : c2.name) ? c2.name : c2, o2[e50] = true, o2[s2] = true, o2[c2] = true, n2.patterns[r3.id] = { min: e50, mid: s2, max: c2 };
    }
  }
  return n2;
}
const Al = /* @__PURE__ */ new Set();
let jl = false;
function Ml(e49, t2, n2 = 2) {
  let r2 = t2 && t2.length, i2 = r2 ? t2[0] * n2 : e49.length;
  Al.size && Al.clear();
  let a2 = Nl(e49, 0, i2, n2, true), o2 = [];
  if (!a2 || a2.next === a2.prev) return o2;
  let s2 = 0, c2 = 0, l2 = 0;
  if (r2 && (a2 = Vl(e49, t2, a2, n2)), e49.length > 80 * n2) {
    s2 = e49[0], c2 = e49[1];
    let t3 = s2, r3 = c2;
    for (let a3 = n2; a3 < i2; a3 += n2) {
      let n3 = e49[a3], i3 = e49[a3 + 1];
      n3 < s2 && (s2 = n3), i3 < c2 && (c2 = i3), n3 > t3 && (t3 = n3), i3 > r3 && (r3 = i3);
    }
    l2 = Math.max(t3 - s2, r3 - c2), l2 = l2 === 0 ? 0 : 32767 / l2;
  }
  return Fl(a2, o2, s2, c2, l2), o2;
}
function Nl(e49, t2, n2, r2, i2) {
  let a2 = null;
  if (i2 === wu(e49, t2, n2, r2) > 0) for (let i3 = t2; i3 < n2; i3 += r2) a2 = xu(i3 / r2 | 0, e49[i3], e49[i3 + 1], a2);
  else for (let i3 = n2 - r2; i3 >= t2; i3 -= r2) a2 = xu(i3 / r2 | 0, e49[i3], e49[i3 + 1], a2);
  return a2 && mu(a2, a2.next) && (Su(a2), a2 = a2.next), a2;
}
function Pl(e49, t2 = e49) {
  let n2 = t2 === e49, r2 = e49, i2;
  do
    i2 = false, r2 !== r2.next && (Al.size === 0 || !Al.has(r2)) && (mu(r2, r2.next) || pu(r2.prev, r2, r2.next) === 0) ? ((n2 || r2 === t2) && (t2 = r2.prev), jl = true, Su(r2), r2 = r2.prev, i2 = true) : (n2 || r2 !== t2) && (r2 = r2.next, i2 = !n2);
  while (i2 || r2 !== t2);
  return t2;
}
function Fl(e49, t2, n2, r2, i2) {
  i2 && ou(e49, n2, r2, i2);
  let a2 = e49, o2 = false;
  for (; e49.prev !== e49.next; ) {
    let s2 = e49.prev, c2 = e49.next;
    if (pu(s2, e49, c2) < 0 && (i2 ? Ll(e49, n2, r2, i2) : Il(e49))) {
      t2.push(s2.i, e49.i, c2.i), Su(e49), e49 = c2, a2 = c2;
      continue;
    }
    if (e49 = c2, e49 === a2) {
      if (jl = false, e49 = Pl(e49), jl) {
        a2 = e49;
        continue;
      }
      if (!o2) {
        e49 = Rl(e49, t2), a2 = e49, o2 = true;
        continue;
      }
      zl(e49, t2, n2, r2, i2);
      break;
    }
  }
}
function Il(e49) {
  let t2 = e49.prev, n2 = e49, r2 = e49.next, i2 = t2.x, a2 = n2.x, o2 = r2.x, s2 = t2.y, c2 = n2.y, l2 = r2.y, u2 = Math.min(i2, a2, o2), d2 = Math.min(s2, c2, l2), f2 = Math.max(i2, a2, o2), p2 = Math.max(s2, c2, l2), m2 = r2.next;
  for (; m2 !== t2; ) {
    if (m2.x >= u2 && m2.x <= f2 && m2.y >= d2 && m2.y <= p2 && (i2 !== m2.x || s2 !== m2.y) && du(i2, s2, a2, c2, o2, l2, m2.x, m2.y) && pu(m2.prev, m2, m2.next) >= 0) return false;
    m2 = m2.next;
  }
  return true;
}
function Ll(e49, t2, n2, r2) {
  let i2 = e49.prev, a2 = e49, o2 = e49.next, s2 = i2.x, c2 = a2.x, l2 = o2.x, u2 = i2.y, d2 = a2.y, f2 = o2.y, p2 = Math.min(s2, c2, l2), m2 = Math.min(u2, d2, f2), h2 = Math.max(s2, c2, l2), g2 = Math.max(u2, d2, f2), _2 = lu(p2, m2, t2, n2, r2), v2 = lu(h2, g2, t2, n2, r2), y2 = e49.prevZ;
  for (; y2 && y2.z >= _2; ) {
    if (y2.x >= p2 && y2.x <= h2 && y2.y >= m2 && y2.y <= g2 && y2 !== o2 && (s2 !== y2.x || u2 !== y2.y) && du(s2, u2, c2, d2, l2, f2, y2.x, y2.y) && pu(y2.prev, y2, y2.next) >= 0) return false;
    y2 = y2.prevZ;
  }
  let b2 = e49.nextZ;
  for (; b2 && b2.z <= v2; ) {
    if (b2.x >= p2 && b2.x <= h2 && b2.y >= m2 && b2.y <= g2 && b2 !== o2 && (s2 !== b2.x || u2 !== b2.y) && du(s2, u2, c2, d2, l2, f2, b2.x, b2.y) && pu(b2.prev, b2, b2.next) >= 0) return false;
    b2 = b2.nextZ;
  }
  return true;
}
function Rl(e49, t2) {
  let n2 = e49, r2 = false;
  do {
    let i2 = n2.prev, a2 = n2.next.next;
    hu(i2, n2, n2.next, a2, false) && vu(i2, a2) && vu(a2, i2) && (t2.push(i2.i, n2.i, a2.i), Su(n2), Su(n2.next), n2 = e49 = a2, r2 = true), n2 = n2.next;
  } while (n2 !== e49);
  return r2 ? Pl(n2) : n2;
}
function zl(e49, t2, n2, r2, i2) {
  let a2 = e49;
  do {
    let e50 = a2.next.next;
    for (; e50 !== a2.prev; ) {
      if (a2.i !== e50.i && fu(a2, e50)) {
        let o2 = bu(a2, e50);
        a2 = Pl(a2, a2.next), o2 = Pl(o2, o2.next), Fl(a2, t2, n2, r2, i2), Fl(o2, t2, n2, r2, i2);
        return;
      }
      e50 = e50.next;
    }
    a2 = a2.next;
  } while (a2 !== e49);
}
let Bl = false;
function Vl(e49, t2, n2, r2) {
  let i2 = [];
  for (let n3 = 0, a2 = t2.length; n3 < a2; n3++) {
    let o2 = Nl(e49, t2[n3] * r2, n3 < a2 - 1 ? t2[n3 + 1] * r2 : e49.length, r2, false);
    o2 === o2.next && Al.add(o2), i2.push(uu(o2));
  }
  i2.sort(Hl), Jl(e49.length / r2, t2.length), Yl(n2, n2), Bl = true;
  for (let e50 = 0; e50 < i2.length; e50++) n2 = Ul(i2[e50], n2);
  return Bl = false, Pl(n2);
}
function Hl(e49, t2) {
  return e49.x - t2.x || e49.y - t2.y || (e49.next.y - e49.y) / (e49.next.x - e49.x) - (t2.next.y - t2.y) / (t2.next.x - t2.x);
}
function Ul(e49, t2) {
  let n2 = $l(e49, t2);
  if (!n2) return t2;
  let r2 = bu(n2, e49), i2 = r2.next;
  return Yl(n2, i2.next), Pl(r2, r2.next), Pl(n2, n2.next);
}
let Wl = /* @__PURE__ */ new Float64Array(), Gl = 0;
const Kl = [], ql = [];
function Jl(e49, t2) {
  let n2 = Math.ceil((e49 + 2 * t2) / 16) + t2 + 2;
  Wl.length < n2 * 4 && (Wl = new Float64Array(n2 * 4)), Gl = 0;
}
function Yl(e49, t2) {
  let n2 = e49;
  do {
    let e50 = Gl++;
    Kl[e50] = n2;
    let r2 = n2.x, i2 = n2.y, a2 = n2.x, o2 = n2.y, s2 = 0;
    do {
      let t3 = n2.next;
      n2.z = e50, t3.x < r2 && (r2 = t3.x), t3.x > a2 && (a2 = t3.x), t3.y < i2 && (i2 = t3.y), t3.y > o2 && (o2 = t3.y), n2 = t3;
    } while (++s2 < 16 && n2 !== t2);
    ql[e50] = n2;
    let c2 = e50 * 4;
    Wl[c2] = r2, Wl[c2 + 1] = i2, Wl[c2 + 2] = a2, Wl[c2 + 3] = o2;
  } while (n2 !== t2);
}
function Xl(e49, t2) {
  let n2 = e49.z * 4;
  t2.x < Wl[n2] && (Wl[n2] = t2.x), t2.y < Wl[n2 + 1] && (Wl[n2 + 1] = t2.y), t2.x > Wl[n2 + 2] && (Wl[n2 + 2] = t2.x), t2.y > Wl[n2 + 3] && (Wl[n2 + 3] = t2.y);
}
function Zl(e49) {
  let t2 = ql[e49];
  for (; t2.prev.next !== t2; ) t2 = t2.next;
  return ql[e49] = t2, t2;
}
function Ql(e49) {
  let t2 = Kl[e49];
  for (; t2.prev.next !== t2; ) t2 = t2.next;
  return Kl[e49] = t2, t2;
}
function $l(e49, t2) {
  let n2 = t2, r2 = e49.x, i2 = e49.y, a2 = -1 / 0, o2;
  if (mu(e49, n2)) return n2;
  for (let t3 = 0, s3 = 0; t3 < Gl; t3++, s3 += 4) {
    if (i2 < Wl[s3 + 1] || i2 > Wl[s3 + 3] || Wl[s3] > r2 || Wl[s3 + 2] <= a2) continue;
    let c3 = Zl(t3);
    n2 = Ql(t3);
    do {
      if (n2.prev.next === n2) {
        if (mu(e49, n2.next)) return n2.next;
        if (i2 <= n2.y && i2 >= n2.next.y && n2.next.y !== n2.y) {
          let e50 = n2.x + (i2 - n2.y) * (n2.next.x - n2.x) / (n2.next.y - n2.y);
          if (e50 <= r2 && e50 > a2 && (a2 = e50, o2 = n2.x < n2.next.x ? n2 : n2.next, e50 === r2)) return o2;
        }
      }
      n2 = n2.next;
    } while (n2 !== c3);
  }
  if (!o2) return null;
  let s2 = o2.x, c2 = o2.y, l2 = Math.min(i2, c2), u2 = Math.max(i2, c2), d2 = 1 / 0;
  for (let t3 = 0, f2 = 0; t3 < Gl; t3++, f2 += 4) {
    if (Wl[f2 + 2] < s2 || Wl[f2] > r2 || Wl[f2 + 3] < l2 || Wl[f2 + 1] > u2) continue;
    let p2 = Zl(t3);
    n2 = Ql(t3);
    do {
      if (n2.prev.next === n2 && r2 >= n2.x && n2.x >= s2 && r2 !== n2.x && du(i2 < c2 ? r2 : a2, i2, s2, c2, i2 < c2 ? a2 : r2, i2, n2.x, n2.y)) {
        let t4 = Math.abs(i2 - n2.y) / (r2 - n2.x);
        (vu(n2, e49) || n2.y === i2 && n2.next.y === i2 && n2.next.x > r2) && (t4 < d2 || t4 === d2 && (n2.x > o2.x || n2.x === o2.x && eu(o2, n2))) && (o2 = n2, d2 = t4);
      }
      n2 = n2.next;
    } while (n2 !== p2);
  }
  return o2;
}
function eu(e49, t2) {
  return pu(e49.prev, e49, t2.prev) < 0 && pu(t2.next, e49, e49.next) < 0;
}
const tu = [];
let nu = [], ru = /* @__PURE__ */ new Uint32Array(), iu = /* @__PURE__ */ new Uint32Array();
const au = /* @__PURE__ */ new Uint32Array(256);
function ou(e49, t2, n2, r2) {
  let i2 = e49, a2 = 0;
  do
    i2.z = lu(i2.x, i2.y, t2, n2, r2), tu[a2++] = i2, i2 = i2.next;
  while (i2 !== e49);
  su(a2);
  let o2 = null;
  for (let e50 = 0; e50 < a2; e50++) {
    let t3 = tu[e50];
    t3.prevZ = o2, o2 && (o2.nextZ = t3), o2 = t3;
  }
  o2.nextZ = null, tu.fill(null, 0, a2), nu.fill(null, 0, a2);
}
function su(e49) {
  if (e49 <= 32) {
    for (let t2 = 1; t2 < e49; t2++) {
      let e50 = tu[t2], n2 = e50.z, r2 = t2 - 1;
      for (; r2 >= 0 && tu[r2].z > n2; ) tu[r2 + 1] = tu[r2], r2--;
      tu[r2 + 1] = e50;
    }
    return;
  }
  ru.length < e49 && (ru = new Uint32Array(e49), iu = new Uint32Array(e49), nu = Array(e49));
  for (let t2 = 0; t2 < e49; t2++) ru[t2] = tu[t2].z;
  cu(e49, tu, ru, nu, iu, 0), cu(e49, nu, iu, tu, ru, 8), cu(e49, tu, ru, nu, iu, 16), cu(e49, nu, iu, tu, ru, 24);
}
function cu(e49, t2, n2, r2, i2, a2) {
  au.fill(0);
  for (let t3 = 0; t3 < e49; t3++) au[n2[t3] >>> a2 & 255]++;
  let o2 = 0;
  for (let e50 = 0; e50 < 256; e50++) {
    let t3 = au[e50];
    au[e50] = o2, o2 += t3;
  }
  for (let o3 = 0; o3 < e49; o3++) {
    let e50 = n2[o3], s2 = au[e50 >>> a2 & 255]++;
    r2[s2] = t2[o3], i2[s2] = e50;
  }
}
function lu(e49, t2, n2, r2, i2) {
  return e49 = (e49 - n2) * i2 | 0, t2 = (t2 - r2) * i2 | 0, e49 = (e49 | e49 << 8) & 16711935, e49 = (e49 | e49 << 4) & 252645135, e49 = (e49 | e49 << 2) & 858993459, e49 = (e49 | e49 << 1) & 1431655765, t2 = (t2 | t2 << 8) & 16711935, t2 = (t2 | t2 << 4) & 252645135, t2 = (t2 | t2 << 2) & 858993459, t2 = (t2 | t2 << 1) & 1431655765, e49 | t2 << 1;
}
function uu(e49) {
  let t2 = e49, n2 = e49;
  do
    (t2.x < n2.x || t2.x === n2.x && t2.y < n2.y) && (n2 = t2), t2 = t2.next;
  while (t2 !== e49);
  return n2;
}
function du(e49, t2, n2, r2, i2, a2, o2, s2) {
  return (i2 - o2) * (t2 - s2) >= (e49 - o2) * (a2 - s2) && (e49 - o2) * (r2 - s2) >= (n2 - o2) * (t2 - s2) && (n2 - o2) * (a2 - s2) >= (i2 - o2) * (r2 - s2);
}
function fu(e49, t2) {
  let n2 = mu(e49, t2) && pu(e49.prev, e49, e49.next) > 0 && pu(t2.prev, t2, t2.next) > 0;
  return e49.next.i !== t2.i && (n2 || vu(e49, t2) && vu(t2, e49) && (pu(e49.prev, e49, t2.prev) !== 0 || pu(e49, t2.prev, t2) !== 0)) && !_u(e49, t2) && (n2 || yu(e49, t2));
}
function pu(e49, t2, n2) {
  return (t2.y - e49.y) * (n2.x - t2.x) - (t2.x - e49.x) * (n2.y - t2.y);
}
function mu(e49, t2) {
  return e49.x === t2.x && e49.y === t2.y;
}
function hu(e49, t2, n2, r2, i2 = true) {
  let a2 = pu(e49, t2, n2), o2 = pu(e49, t2, r2), s2 = pu(n2, r2, e49), c2 = pu(n2, r2, t2);
  return (a2 > 0 && o2 < 0 || a2 < 0 && o2 > 0) && (s2 > 0 && c2 < 0 || s2 < 0 && c2 > 0) ? true : i2 ? !!(a2 === 0 && gu(e49, n2, t2) || o2 === 0 && gu(e49, r2, t2) || s2 === 0 && gu(n2, e49, r2) || c2 === 0 && gu(n2, t2, r2)) : false;
}
function gu(e49, t2, n2) {
  return t2.x <= Math.max(e49.x, n2.x) && t2.x >= Math.min(e49.x, n2.x) && t2.y <= Math.max(e49.y, n2.y) && t2.y >= Math.min(e49.y, n2.y);
}
function _u(e49, t2) {
  let n2 = Math.min(e49.x, t2.x), r2 = Math.max(e49.x, t2.x), i2 = Math.min(e49.y, t2.y), a2 = Math.max(e49.y, t2.y), o2 = e49;
  do {
    let s2 = o2.next;
    if (o2.x > r2 && s2.x > r2 || o2.x < n2 && s2.x < n2 || o2.y > a2 && s2.y > a2 || o2.y < i2 && s2.y < i2) {
      o2 = s2;
      continue;
    }
    if (o2.i !== e49.i && s2.i !== e49.i && o2.i !== t2.i && s2.i !== t2.i && hu(o2, s2, e49, t2)) return true;
    o2 = s2;
  } while (o2 !== e49);
  return false;
}
function vu(e49, t2) {
  return pu(e49.prev, e49, e49.next) < 0 ? pu(e49, t2, e49.next) >= 0 && pu(e49, e49.prev, t2) >= 0 : pu(e49, t2, e49.prev) < 0 || pu(e49, e49.next, t2) < 0;
}
function yu(e49, t2) {
  let n2 = e49, r2 = false, i2 = (e49.x + t2.x) / 2, a2 = (e49.y + t2.y) / 2;
  do {
    let e50 = n2.next;
    n2.y > a2 != e50.y > a2 && i2 < (e50.x - n2.x) * (a2 - n2.y) / (e50.y - n2.y) + n2.x && (r2 = !r2), n2 = e50;
  } while (n2 !== e49);
  return r2;
}
function bu(e49, t2) {
  let n2 = Cu(e49.i, e49.x, e49.y), r2 = Cu(t2.i, t2.x, t2.y), i2 = e49.next, a2 = t2.prev;
  return e49.next = t2, t2.prev = e49, n2.next = i2, i2.prev = n2, r2.next = n2, n2.prev = r2, a2.next = r2, r2.prev = a2, r2;
}
function xu(e49, t2, n2, r2) {
  let i2 = Cu(e49, t2, n2);
  return r2 ? (i2.next = r2.next, i2.prev = r2, r2.next.prev = i2, r2.next = i2) : (i2.prev = i2, i2.next = i2), i2;
}
function Su(e49) {
  e49.next.prev = e49.prev, e49.prev.next = e49.next, e49.prevZ && (e49.prevZ.nextZ = e49.nextZ), e49.nextZ && (e49.nextZ.prevZ = e49.prevZ), Bl && Xl(e49.prev, e49.next);
}
function Cu(e49, t2, n2) {
  return { i: e49, x: t2, y: n2, prev: null, next: null, z: 0, prevZ: null, nextZ: null };
}
function wu(e49, t2, n2, r2) {
  let i2 = 0;
  for (let a2 = t2, o2 = n2 - r2; a2 < n2; a2 += r2) i2 += (e49[o2] - e49[a2]) * (e49[a2 + 1] + e49[o2 + 1]), o2 = a2;
  return i2;
}
var Tu = class {
  constructor(e49, t2) {
    if (t2 > e49) throw Error(`Min granularity must not be greater than base granularity.`);
    this._baseZoomGranularity = e49, this._minGranularity = t2;
  }
  getGranularityForZoomLevel(e49) {
    let t2 = 1 << e49;
    return Math.max(Math.floor(this._baseZoomGranularity / t2), this._minGranularity, 1);
  }
}, Eu = (_c2 = class {
  constructor(e49) {
    this.fill = e49.fill, this.line = e49.line, this.tile = e49.tile, this.stencil = e49.stencil, this.circle = e49.circle;
  }
}, _c2.noSubdivision = new _c2({ fill: new Tu(0, 0), line: new Tu(0, 0), tile: new Tu(0, 0), stencil: new Tu(0, 0), circle: 1 }), _c2);
G(`SubdivisionGranularityExpression`, Tu), G(`SubdivisionGranularitySetting`, Eu);
const Du = -32768, Ou = 32767;
var ku = class {
  constructor(e49, t2) {
    this._vertexBuffer = [], this._vertexDictionary = /* @__PURE__ */ new Map(), this._used = false, this._granularity = e49, this._granularityCellSize = v / e49, this._canonical = t2;
  }
  _getKey(e49, t2) {
    return e49 += 32768, t2 += 32768, e49 << 16 | t2 << 0;
  }
  _vertexToIndex(e49, t2) {
    if (e49 < -32768 || t2 < -32768 || e49 > 32767 || t2 > 32767) throw Error(`Vertex coordinates are out of signed 16 bit integer range.`);
    let n2 = Math.round(e49) | 0, r2 = Math.round(t2) | 0, i2 = this._getKey(n2, r2), a2 = this._vertexDictionary.get(i2);
    if (a2 !== void 0) return a2;
    let o2 = this._vertexBuffer.length / 2;
    return this._vertexDictionary.set(i2, o2), this._vertexBuffer.push(n2, r2), o2;
  }
  _subdivideTrianglesScanline(e49, t2) {
    if (this._granularity < 2) return Fu(this._vertexBuffer, Pu(e49, t2));
    let n2 = [], r2 = e49.length;
    for (let i2 = 0; i2 < r2; i2 += 3) {
      let r3 = [t2[e49[i2 + 0]], t2[e49[i2 + 1]], t2[e49[i2 + 2]]], a2 = [this._vertexBuffer[r3[0] * 2 + 0], this._vertexBuffer[r3[0] * 2 + 1], this._vertexBuffer[r3[1] * 2 + 0], this._vertexBuffer[r3[1] * 2 + 1], this._vertexBuffer[r3[2] * 2 + 0], this._vertexBuffer[r3[2] * 2 + 1]], o2 = 1 / 0, s2 = 1 / 0, c2 = -1 / 0, l2 = -1 / 0;
      for (let e50 = 0; e50 < 3; e50++) {
        let t3 = a2[e50 * 2], n3 = a2[e50 * 2 + 1];
        o2 = Math.min(o2, t3), c2 = Math.max(c2, t3), s2 = Math.min(s2, n3), l2 = Math.max(l2, n3);
      }
      if (o2 === c2 || s2 === l2) continue;
      let u2 = Math.floor(o2 / this._granularityCellSize), d2 = Math.ceil(c2 / this._granularityCellSize), f2 = Math.floor(s2 / this._granularityCellSize), p2 = Math.ceil(l2 / this._granularityCellSize);
      if (u2 === d2 && f2 === p2) {
        n2.push(...r3);
        continue;
      }
      for (let e50 = f2; e50 < p2; e50++) {
        let t3 = this._scanlineGenerateVertexRingForCellRow(e50, a2, r3);
        Iu(this._vertexBuffer, t3, n2);
      }
    }
    return n2;
  }
  _scanlineGenerateVertexRingForCellRow(e49, t2, n2) {
    let r2 = e49 * this._granularityCellSize, i2 = r2 + this._granularityCellSize, a2 = [];
    for (let e50 = 0; e50 < 3; e50++) {
      let o2 = t2[e50 * 2], s2 = t2[e50 * 2 + 1], c2 = t2[(e50 + 1) * 2 % 6], l2 = t2[((e50 + 1) * 2 + 1) % 6], u2 = t2[(e50 + 2) * 2 % 6], d2 = t2[((e50 + 2) * 2 + 1) % 6], f2 = c2 - o2, p2 = l2 - s2, m2 = f2 === 0, h2 = p2 === 0, g2 = (r2 - s2) / p2, _2 = (i2 - s2) / p2, v2 = Math.min(g2, _2), y2 = Math.max(g2, _2);
      if (!h2 && (v2 >= 1 || y2 <= 0) || h2 && (s2 < r2 || s2 > i2)) {
        l2 >= r2 && l2 <= i2 && a2.push(n2[(e50 + 1) % 3]);
        continue;
      }
      if (!h2 && v2 > 0) {
        let e51 = o2 + f2 * v2, t3 = s2 + p2 * v2;
        a2.push(this._vertexToIndex(e51, t3));
      }
      let b2 = o2 + f2 * Math.max(v2, 0), x2 = o2 + f2 * Math.min(y2, 1);
      if (m2 || this._generateIntraEdgeVertices(a2, o2, s2, c2, l2, b2, x2), !h2 && y2 < 1) {
        let e51 = o2 + f2 * y2, t3 = s2 + p2 * y2;
        a2.push(this._vertexToIndex(e51, t3));
      }
      (h2 || l2 >= r2 && l2 <= i2) && a2.push(n2[(e50 + 1) % 3]), !h2 && (l2 <= r2 || l2 >= i2) && this._generateInterEdgeVertices(a2, o2, s2, c2, l2, u2, d2, x2, r2, i2);
    }
    return a2;
  }
  _generateIntraEdgeVertices(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = r2 - t2, c2 = i2 - n2, l2 = c2 === 0, u2 = l2 ? Math.min(t2, r2) : Math.min(a2, o2), d2 = l2 ? Math.max(t2, r2) : Math.max(a2, o2), f2 = Math.floor(u2 / this._granularityCellSize) + 1, p2 = Math.ceil(d2 / this._granularityCellSize) - 1;
    if (l2 ? t2 < r2 : a2 < o2) for (let r3 = f2; r3 <= p2; r3++) {
      let i3 = r3 * this._granularityCellSize, a3 = n2 + c2 * (i3 - t2) / s2;
      e49.push(this._vertexToIndex(i3, a3));
    }
    else for (let r3 = p2; r3 >= f2; r3--) {
      let i3 = r3 * this._granularityCellSize, a3 = n2 + c2 * (i3 - t2) / s2;
      e49.push(this._vertexToIndex(i3, a3));
    }
  }
  _generateInterEdgeVertices(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2) {
    let u2 = i2 - n2, d2 = a2 - r2, f2 = o2 - i2, p2 = (c2 - i2) / f2, m2 = (l2 - i2) / f2, h2 = Math.min(p2, m2), g2 = Math.max(p2, m2), _2 = r2 + d2 * h2, v2 = Math.floor(Math.min(_2, s2) / this._granularityCellSize) + 1, y2 = Math.ceil(Math.max(_2, s2) / this._granularityCellSize) - 1, b2 = s2 < _2, x2 = f2 === 0;
    if (x2 && (o2 === c2 || o2 === l2)) return;
    if (x2 || h2 >= 1 || g2 <= 0) {
      let e50 = t2 - a2, r3 = n2 - o2, i3 = (c2 - o2) / r3, u3 = (l2 - o2) / r3, d3 = a2 + e50 * Math.min(i3, u3);
      v2 = Math.floor(Math.min(d3, s2) / this._granularityCellSize) + 1, y2 = Math.ceil(Math.max(d3, s2) / this._granularityCellSize) - 1, b2 = s2 < d3;
    }
    let S2 = u2 > 0 ? l2 : c2;
    if (b2) for (let t3 = v2; t3 <= y2; t3++) {
      let n3 = t3 * this._granularityCellSize;
      e49.push(this._vertexToIndex(n3, S2));
    }
    else for (let t3 = y2; t3 >= v2; t3--) {
      let n3 = t3 * this._granularityCellSize;
      e49.push(this._vertexToIndex(n3, S2));
    }
  }
  _generateOutline(e49, t2) {
    let n2 = [], r2 = 0;
    for (let i2 of e49) {
      let e50 = this._granularity < 2 ? this._ringPathIndices(i2, r2, t2) : this._pointArrayToIndices(ju(i2, this._granularity, true));
      r2 += i2.length;
      let a2 = [];
      for (let t3 = 1; t3 < e50.length; t3++) a2.push(e50[t3 - 1]), a2.push(e50[t3]);
      n2.push(a2);
    }
    return n2;
  }
  _ringPathIndices(e49, t2, n2) {
    let r2 = n2.slice(t2, t2 + e49.length);
    return r2.length >= 2 && Mu(e49) && r2.push(r2[0]), r2;
  }
  _handlePoles(e49) {
    let t2 = false, n2 = false;
    this._canonical && (this._canonical.y === 0 && (t2 = true), this._canonical.y === (1 << this._canonical.z) - 1 && (n2 = true)), (t2 || n2) && this._fillPoles(e49, t2, n2);
  }
  _ensureNoPoleVertices() {
    let e49 = this._vertexBuffer;
    for (let t2 = 0; t2 < e49.length; t2 += 2) {
      let n2 = e49[t2 + 1];
      n2 === -32768 && (e49[t2 + 1] = -32767), n2 === 32767 && (e49[t2 + 1] = 32766);
    }
  }
  _generatePoleQuad(e49, t2, n2, r2, i2, a2) {
    r2 > i2 == (a2 === -32768) ? (e49.push(n2), e49.push(t2), e49.push(this._vertexToIndex(r2, a2)), e49.push(this._vertexToIndex(i2, a2)), e49.push(n2), e49.push(this._vertexToIndex(r2, a2))) : (e49.push(t2), e49.push(n2), e49.push(this._vertexToIndex(r2, a2)), e49.push(n2), e49.push(this._vertexToIndex(i2, a2)), e49.push(this._vertexToIndex(r2, a2)));
  }
  _fillPoles(e49, t2, n2) {
    let r2 = this._vertexBuffer, i2 = v, a2 = e49.length;
    for (let o2 = 2; o2 < a2; o2 += 3) {
      let a3 = e49[o2 - 2], s2 = e49[o2 - 1], c2 = e49[o2], l2 = r2[a3 * 2], u2 = r2[a3 * 2 + 1], d2 = r2[s2 * 2], f2 = r2[s2 * 2 + 1], p2 = r2[c2 * 2], m2 = r2[c2 * 2 + 1];
      t2 && (u2 === 0 && f2 === 0 && this._generatePoleQuad(e49, a3, s2, l2, d2, Du), f2 === 0 && m2 === 0 && this._generatePoleQuad(e49, s2, c2, d2, p2, Du), m2 === 0 && u2 === 0 && this._generatePoleQuad(e49, c2, a3, p2, l2, Du)), n2 && (u2 === i2 && f2 === i2 && this._generatePoleQuad(e49, a3, s2, l2, d2, Ou), f2 === i2 && m2 === i2 && this._generatePoleQuad(e49, s2, c2, d2, p2, Ou), m2 === i2 && u2 === i2 && this._generatePoleQuad(e49, c2, a3, p2, l2, Ou));
    }
  }
  _initializeVertices(e49) {
    let t2 = [];
    for (let n2 = 0; n2 < e49.length; n2 += 2) t2.push(this._vertexToIndex(e49[n2], e49[n2 + 1]));
    return t2;
  }
  subdividePolygonInternal(e49, t2) {
    var _a3;
    if (this._used) throw Error(`Subdivision: multiple use not allowed.`);
    this._used = true;
    let { flattened: n2, holeIndices: r2 } = Nu(e49), i2 = this._initializeVertices(n2), a2;
    try {
      let e50 = Ml(n2, r2);
      a2 = this._subdivideTrianglesScanline(e50, i2);
    } catch (e50) {
      console.error(e50);
    }
    let o2 = [];
    return t2 && (o2 = this._generateOutline(e49, i2)), this._ensureNoPoleVertices(), this._handlePoles(a2), this._granularity >= 2 && ((_a3 = this._canonical) == null ? void 0 : _a3.z) === 0 && (a2 = this._removeTrianglesOutsideTileX(a2), o2 = o2.map((e50) => this._removeLinesOutsideTileX(e50))), { verticesFlattened: this._vertexBuffer, indicesTriangles: a2, indicesLineList: o2 };
  }
  _vertexOutsideTileX(e49) {
    let t2 = this._vertexBuffer[e49 * 2];
    return t2 < 0 || t2 > 8192;
  }
  _removeTrianglesOutsideTileX(e49) {
    let t2 = [];
    for (let n2 = 0; n2 < e49.length; n2 += 3) this._vertexOutsideTileX(e49[n2]) || this._vertexOutsideTileX(e49[n2 + 1]) || this._vertexOutsideTileX(e49[n2 + 2]) || t2.push(e49[n2], e49[n2 + 1], e49[n2 + 2]);
    return t2;
  }
  _removeLinesOutsideTileX(e49) {
    let t2 = [];
    for (let n2 = 0; n2 < e49.length; n2 += 2) this._vertexOutsideTileX(e49[n2]) || this._vertexOutsideTileX(e49[n2 + 1]) || t2.push(e49[n2], e49[n2 + 1]);
    return t2;
  }
  _pointArrayToIndices(e49) {
    let t2 = [];
    for (let n2 of e49) t2.push(this._vertexToIndex(n2.x, n2.y));
    return t2;
  }
};
function Au(e49, t2, n2, r2 = true) {
  return new ku(n2, t2).subdividePolygonInternal(e49, r2);
}
function ju(e49, t2, n2 = false) {
  if (!e49 || e49.length < 1 || e49.length < 2) return [];
  let r2 = n2 && Mu(e49);
  if (t2 < 2) return r2 ? [...e49, e49[0]] : [...e49];
  let i2 = Math.floor(v / t2), a2 = [];
  a2.push(new l(e49[0].x, e49[0].y));
  let o2 = e49.length, s2 = r2 ? o2 : o2 - 1;
  for (let t3 = 0; t3 < s2; t3++) {
    let n3 = e49[t3], r3 = t3 < o2 - 1 ? e49[t3 + 1] : e49[0], s3 = n3.x, c2 = n3.y, u2 = r3.x, d2 = r3.y, f2 = s3 !== u2, p2 = c2 !== d2;
    if (!f2 && !p2) continue;
    let m2 = u2 - s3, h2 = d2 - c2, g2 = Math.abs(m2), _2 = Math.abs(h2), v2 = s3, y2 = c2;
    for (; ; ) {
      let e50 = m2 > 0 ? (Math.floor(v2 / i2) + 1) * i2 : (Math.ceil(v2 / i2) - 1) * i2, t4 = h2 > 0 ? (Math.floor(y2 / i2) + 1) * i2 : (Math.ceil(y2 / i2) - 1) * i2, n4 = Math.abs(v2 - e50), r4 = Math.abs(y2 - t4), o3 = Math.abs(v2 - u2), s4 = Math.abs(y2 - d2), c3 = f2 ? n4 / g2 : 1 / 0, b3 = p2 ? r4 / _2 : 1 / 0;
      if ((o3 <= n4 || !f2) && (s4 <= r4 || !p2)) break;
      if (c3 < b3 && f2 || !p2) {
        v2 = e50, y2 += h2 * c3;
        let t5 = new l(v2, Math.round(y2));
        (a2[a2.length - 1].x !== t5.x || a2[a2.length - 1].y !== t5.y) && a2.push(t5);
      } else {
        v2 += m2 * b3, y2 = t4;
        let e51 = new l(Math.round(v2), y2);
        (a2[a2.length - 1].x !== e51.x || a2[a2.length - 1].y !== e51.y) && a2.push(e51);
      }
    }
    let b2 = new l(u2, d2);
    (a2[a2.length - 1].x !== b2.x || a2[a2.length - 1].y !== b2.y) && a2.push(b2);
  }
  return a2;
}
function Mu(e49) {
  return !e49[0].equals(e49[e49.length - 1]);
}
function Nu(e49) {
  let t2 = [], n2 = [];
  for (let r2 of e49) if (r2.length !== 0) {
    r2 !== e49[0] && t2.push(n2.length / 2);
    for (let e50 of r2) n2.push(e50.x), n2.push(e50.y);
  }
  return { flattened: n2, holeIndices: t2 };
}
function Pu(e49, t2) {
  let n2 = Array(e49.length);
  for (let r2 = 0; r2 < e49.length; r2++) n2[r2] = t2[e49[r2]];
  return n2;
}
function Fu(e49, t2) {
  let n2 = [];
  for (let r2 = 0; r2 < t2.length; r2 += 3) {
    let i2 = t2[r2], a2 = t2[r2 + 1], o2 = t2[r2 + 2], s2 = e49[i2 * 2], c2 = e49[i2 * 2 + 1], l2 = e49[a2 * 2], u2 = e49[a2 * 2 + 1], d2 = e49[o2 * 2], f2 = e49[o2 * 2 + 1], p2 = l2 - s2, m2 = u2 - c2, h2 = d2 - s2;
    p2 * (f2 - c2) - m2 * h2 > 0 ? (n2.push(i2), n2.push(o2), n2.push(a2)) : (n2.push(i2), n2.push(a2), n2.push(o2));
  }
  return n2;
}
function Iu(e49, t2, n2) {
  if (t2.length === 0) throw Error(`Subdivision vertex ring is empty.`);
  let r2 = 0, i2 = e49[t2[0] * 2];
  for (let n3 = 1; n3 < t2.length; n3++) {
    let a3 = e49[t2[n3] * 2];
    a3 < i2 && (i2 = a3, r2 = n3);
  }
  let a2 = t2.length, o2 = r2, s2 = (o2 + 1) % a2;
  for (; ; ) {
    let r3 = o2 - 1 >= 0 ? o2 - 1 : a2 - 1, i3 = (s2 + 1) % a2, c2 = e49[t2[r3] * 2], l2 = e49[t2[r3] * 2 + 1], u2 = e49[t2[i3] * 2], d2 = e49[t2[i3] * 2 + 1], f2 = e49[t2[o2] * 2], p2 = e49[t2[o2] * 2 + 1], m2 = e49[t2[s2] * 2], h2 = e49[t2[s2] * 2 + 1], g2 = false;
    if (c2 < u2) g2 = true;
    else if (c2 > u2) g2 = false;
    else {
      let e50 = h2 - p2, t3 = -(m2 - f2), n3 = p2 < h2 ? 1 : -1;
      ((c2 - f2) * e50 + (l2 - p2) * t3) * n3 > ((u2 - f2) * e50 + (d2 - p2) * t3) * n3 && (g2 = true);
    }
    if (g2) {
      let e50 = t2[r3], i4 = t2[o2], c3 = t2[s2];
      e50 !== i4 && e50 !== c3 && i4 !== c3 && n2.push(c3, i4, e50), o2--, o2 < 0 && (o2 = a2 - 1);
    } else {
      let e50 = t2[i3], r4 = t2[o2], c3 = t2[s2];
      e50 !== r4 && e50 !== c3 && r4 !== c3 && n2.push(c3, r4, e50), s2++, s2 >= a2 && (s2 = 0);
    }
    if (r3 === i3) break;
  }
}
function Lu(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
  let l2 = i2.length / 2, u2 = o2 && s2 && c2;
  if (l2 < Ws.MAX_VERTEX_ARRAY_LENGTH) {
    let d2 = t2.prepareSegment(l2, n2, r2), f2 = d2.vertexLength;
    for (let e50 = 0; e50 < a2.length; e50 += 3) r2.emplaceBack(f2 + a2[e50], f2 + a2[e50 + 1], f2 + a2[e50 + 2]);
    d2.vertexLength += l2, d2.primitiveLength += a2.length / 3;
    let p2, m2;
    u2 && (m2 = o2.prepareSegment(l2, n2, s2), p2 = m2.vertexLength, m2.vertexLength += l2);
    for (let t3 = 0; t3 < i2.length; t3 += 2) e49(i2[t3], i2[t3 + 1]);
    if (u2) for (let e50 of c2) {
      for (let t3 = 1; t3 < e50.length; t3 += 2) s2.emplaceBack(p2 + e50[t3 - 1], p2 + e50[t3]);
      m2.primitiveLength += e50.length / 2;
    }
  } else zu(t2, n2, r2, i2, a2, e49), u2 && Bu(o2, n2, s2, i2, c2, e49), t2.forceNewSegmentOnNextPrepare(), o2 == null ? void 0 : o2.forceNewSegmentOnNextPrepare();
}
function Ru(e49, t2, n2, r2, i2, a2, o2) {
  if (a2) {
    let a3 = r2.count;
    return n2(t2[i2 * 2], t2[i2 * 2 + 1]), e49[i2] = r2.count, r2.count++, o2.vertexLength++, a3;
  }
  return e49[i2];
}
function zu(e49, t2, n2, r2, i2, a2) {
  let o2 = [];
  for (let e50 = 0; e50 < r2.length / 2; e50++) o2.push(-1);
  let s2 = { count: 0 }, c2 = 0, l2 = e49.getOrCreateLatestSegment(t2, n2), u2 = l2.vertexLength;
  for (let d2 = 2; d2 < i2.length; d2 += 3) {
    let f2 = i2[d2 - 2], p2 = i2[d2 - 1], m2 = i2[d2], h2 = o2[f2] < c2, g2 = o2[p2] < c2, _2 = o2[m2] < c2, v2 = +!!h2 + +!!g2 + +!!_2;
    l2.vertexLength + v2 > Ws.MAX_VERTEX_ARRAY_LENGTH && (l2 = e49.createNewSegment(t2, n2), c2 = s2.count, h2 = true, g2 = true, _2 = true, u2 = 0);
    let y2 = Ru(o2, r2, a2, s2, f2, h2, l2), b2 = Ru(o2, r2, a2, s2, p2, g2, l2), x2 = Ru(o2, r2, a2, s2, m2, _2, l2);
    n2.emplaceBack(u2 + y2 - c2, u2 + b2 - c2, u2 + x2 - c2), l2.primitiveLength++;
  }
}
function Bu(e49, t2, n2, r2, i2, a2) {
  let o2 = [];
  for (let e50 = 0; e50 < r2.length / 2; e50++) o2.push(-1);
  let s2 = { count: 0 }, c2 = 0, l2 = e49.getOrCreateLatestSegment(t2, n2), u2 = l2.vertexLength;
  for (let d2 of i2) for (let i3 = 1; i3 < d2.length; i3 += 2) {
    let f2 = d2[i3 - 1], p2 = d2[i3], m2 = o2[f2] < c2, h2 = o2[p2] < c2, g2 = +!!m2 + +!!h2;
    l2.vertexLength + g2 > Ws.MAX_VERTEX_ARRAY_LENGTH && (l2 = e49.createNewSegment(t2, n2), c2 = s2.count, m2 = true, h2 = true, u2 = 0);
    let _2 = Ru(o2, r2, a2, s2, f2, m2, l2), v2 = Ru(o2, r2, a2, s2, p2, h2, l2);
    n2.emplaceBack(u2 + _2 - c2, u2 + v2 - c2), l2.primitiveLength++;
  }
}
var Vu = class {
  constructor(e49) {
    this.zoom = e49.zoom, this.overscaling = e49.overscaling, this.layers = e49.layers, this.layerIds = this.layers.map((e50) => e50.id), this.index = e49.index, this.hasDependencies = false, this.sdfPatterns = {}, this.patternFeatures = [], this.layoutVertexArray = new ks(), this.indexArray = new Bs(), this.indexArray2 = new Vs(), this.programConfigurations = new mc(e49.layers, e49.zoom), this.segments = new Ws(), this.segments2 = new Ws(), this.stateDependentLayerIds = this.layers.filter((e50) => e50.isStateDependent()).map((e50) => e50.id);
  }
  populate(e49, t2, n2) {
    this.hasDependencies = Ol(`fill`, this.layers, t2);
    let r2 = this.layers[0].layout.get(`fill-sort-key`), i2 = !r2.isConstant(), a2 = [], o2 = new K(this.zoom), s2 = this.layers[0]._featureFilter.needGeometry;
    for (let { feature: c2, id: l2, index: u2, sourceLayerIndex: d2 } of e49) {
      let e50 = xc(c2, s2);
      if (!this.layers[0]._featureFilter.filter(o2, e50, n2)) continue;
      let f2 = i2 ? r2.evaluate(e50, {}, n2, t2.availableImages) : void 0, p2 = { id: l2, properties: c2.properties, type: c2.type, sourceLayerIndex: d2, index: u2, geometry: s2 ? e50.geometry : bc(c2), patterns: {}, sortKey: f2 };
      a2.push(p2);
    }
    i2 && a2.sort((e50, t3) => e50.sortKey - t3.sortKey);
    for (let r3 of a2) {
      let { geometry: i3, index: a3, sourceLayerIndex: o3 } = r3;
      if (this.hasDependencies) {
        let e50 = kl(`fill`, this.layers, r3, { zoom: this.zoom }, t2);
        this.patternFeatures.push(e50);
      } else this.addFeature(r3, i3, a3, n2, {}, t2.subdivisionGranularity);
      let s3 = e49[a3].feature;
      t2.featureIndex.insert(s3, i3, a3, o3, this.index);
    }
  }
  update(e49, t2, n2) {
    this.stateDependentLayers.length && this.programConfigurations.updatePaintArrays(e49, t2, this.stateDependentLayers, { imagePositions: n2 });
  }
  addFeatures({ options: e49, canonical: t2, patternPositions: n2, patternMap: r2 }) {
    this.detectSdfPatterns(r2);
    for (let r3 of this.patternFeatures) this.addFeature(r3, r3.geometry, r3.index, t2, n2, e49.subdivisionGranularity);
  }
  detectSdfPatterns(e49) {
    for (let t2 of this.patternFeatures) for (let n2 in t2.patterns) {
      let r2 = t2.patterns[n2];
      this.recordSdfPattern(n2, e49[r2.min]), this.recordSdfPattern(n2, e49[r2.mid]), this.recordSdfPattern(n2, e49[r2.max]);
    }
    for (let t2 of this.layers) {
      let n2 = t2.paint.get(`fill-pattern`).constantOr(null);
      n2 && (this.recordSdfPattern(t2.id, e49[n2.from.toString()]), this.recordSdfPattern(t2.id, e49[n2.to.toString()]));
    }
  }
  recordSdfPattern(e49, t2) {
    if (!t2) return;
    let n2 = t2.sdf === true, r2 = this.sdfPatterns[e49];
    r2 === void 0 ? this.sdfPatterns[e49] = n2 : r2 !== n2 && A(`Style sheet warning: Cannot mix SDF and non-SDF fill patterns in layer "${e49}"`);
  }
  isEmpty() {
    return this.layoutVertexArray.length === 0;
  }
  uploadPending() {
    return !this.uploaded || this.programConfigurations.needsUpload;
  }
  upload(e49) {
    this.uploaded || (this.layoutVertexBuffer = e49.createVertexBuffer(this.layoutVertexArray, Dl), this.indexBuffer = e49.createIndexBuffer(this.indexArray), this.indexBuffer2 = e49.createIndexBuffer(this.indexArray2)), this.programConfigurations.upload(e49), this.uploaded = true;
  }
  destroy() {
    this.layoutVertexBuffer && (this.layoutVertexBuffer.destroy(), this.indexBuffer.destroy(), this.indexBuffer2.destroy(), this.programConfigurations.destroy(), this.segments.destroy(), this.segments2.destroy());
  }
  addFeature(e49, t2, n2, r2, i2, a2) {
    for (let e50 of Ft(t2, 500)) {
      let t3 = Au(e50, r2, a2.fill.getGranularityForZoomLevel(r2.z)), n3 = this.layoutVertexArray;
      Lu((e51, t4) => {
        n3.emplaceBack(e51, t4);
      }, this.segments, this.layoutVertexArray, this.indexArray, t3.verticesFlattened, t3.indicesTriangles, this.segments2, this.indexArray2, t3.indicesLineList);
    }
    this.programConfigurations.populatePaintArrays(this.layoutVertexArray.length, e49, n2, { imagePositions: i2, canonical: r2 });
  }
};
G(`FillBucket`, Vu, { omit: [`layers`, `patternFeatures`] });
let Hu;
const Uu = () => Hu ||= new Fo({ "fill-sort-key": new J(M.layout_fill[`fill-sort-key`], `fill-sort-key`) });
let Wu;
const Gu = () => Wu ||= new Fo({ "fill-antialias": new q(M.paint_fill[`fill-antialias`], `fill-antialias`), "fill-opacity": new J(M.paint_fill[`fill-opacity`], `fill-opacity`), "fill-layer-opacity": new q(M.paint_fill[`fill-layer-opacity`], `fill-layer-opacity`), "fill-color": new J(M.paint_fill[`fill-color`], `fill-color`), "fill-outline-color": new J(M.paint_fill[`fill-outline-color`], `fill-outline-color`), "fill-translate": new q(M.paint_fill[`fill-translate`], `fill-translate`), "fill-translate-anchor": new q(M.paint_fill[`fill-translate-anchor`], `fill-translate-anchor`), "fill-pattern": new Mo(M.paint_fill[`fill-pattern`], `fill-pattern`) });
var Ku = { get paint() {
  return Gu();
}, get layout() {
  return Uu();
} }, qu = class extends Ro {
  constructor(e49, t2) {
    super(e49, Ku, t2);
  }
  recalculate(e49, t2) {
    super.recalculate(e49, t2);
    let n2 = this.paint._values[`fill-outline-color`];
    n2.value.kind === `constant` && n2.value.value === void 0 && (this.paint._values[`fill-outline-color`] = this.paint._values[`fill-color`]);
  }
  createBucket(e49) {
    return new Vu(e49);
  }
  queryRadius() {
    return zc(this.paint.get(`fill-translate`));
  }
  queryIntersectsFeature({ queryGeometry: e49, geometry: t2, transform: n2, pixelsToTileUnits: r2 }) {
    return Dc(Bc(e49, this.paint.get(`fill-translate`), this.paint.get(`fill-translate-anchor`), -n2.bearingInRadians, r2), t2);
  }
  isTileClipped() {
    return true;
  }
};
const Ju = Ho([{ name: `a_pos`, components: 2, type: `Int16` }, { name: `a_normal_ed`, components: 4, type: `Int16` }], 4), Yu = Ho([{ name: `a_centroid`, components: 2, type: `Int16` }], 4), Xu = Ju.members;
Ju.size, Ju.alignment;
var Zu = class e41 {
  constructor() {
    this.minX = 1 / 0, this.maxX = -1 / 0, this.minY = 1 / 0, this.maxY = -1 / 0;
  }
  extend(e49) {
    return this.minX = Math.min(this.minX, e49.x), this.minY = Math.min(this.minY, e49.y), this.maxX = Math.max(this.maxX, e49.x), this.maxY = Math.max(this.maxY, e49.y), this;
  }
  expandBy(e49) {
    return this.minX -= e49, this.minY -= e49, this.maxX += e49, this.maxY += e49, (this.minX > this.maxX || this.minY > this.maxY) && (this.minX = 1 / 0, this.maxX = -1 / 0, this.minY = 1 / 0, this.maxY = -1 / 0), this;
  }
  shrinkBy(e49) {
    return this.expandBy(-e49);
  }
  map(t2) {
    let n2 = new e41();
    return n2.extend(t2(new l(this.minX, this.minY))), n2.extend(t2(new l(this.maxX, this.minY))), n2.extend(t2(new l(this.minX, this.maxY))), n2.extend(t2(new l(this.maxX, this.maxY))), n2;
  }
  static fromPoints(t2) {
    let n2 = new e41();
    for (let e49 of t2) n2.extend(e49);
    return n2;
  }
  contains(e49) {
    return e49.x >= this.minX && e49.x <= this.maxX && e49.y >= this.minY && e49.y <= this.maxY;
  }
  empty() {
    return this.minX > this.maxX;
  }
  width() {
    return this.maxX - this.minX;
  }
  height() {
    return this.maxY - this.minY;
  }
  covers(e49) {
    return !this.empty() && !e49.empty() && e49.minX >= this.minX && e49.maxX <= this.maxX && e49.minY >= this.minY && e49.maxY <= this.maxY;
  }
  intersects(e49) {
    return !this.empty() && !e49.empty() && e49.minX <= this.maxX && e49.maxX >= this.minX && e49.minY <= this.maxY && e49.maxY >= this.minY;
  }
};
Zu.fromPoints([new l(0, 0), new l(v, v)]);
function Qu(e49, t2) {
  return e49.x === t2.x && (e49.x < 0 || e49.x > 8192) || e49.y === t2.y && (e49.y < 0 || e49.y > 8192);
}
function $u(e49) {
  return e49.every((e50) => e50.x < 0) || e49.every((e50) => e50.x > 8192) || e49.every((e50) => e50.y < 0) || e49.every((e50) => e50.y > 8192);
}
var ed = class {
  constructor(e49, t2, n2, r2, i2) {
    for (this.properties = /* @__PURE__ */ Object.create(null), this.extent = n2, this.type = 0, this.id = void 0, this._pbf = e49, this._geometry = -1, this._keys = r2, this._values = i2; e49.pos < t2; ) {
      let t3 = e49.readVarint();
      if (t3 === 8) this.id = e49.readVarint();
      else if (t3 === 18) {
        let t4 = e49.readVarint() + e49.pos;
        for (; e49.pos < t4; ) {
          let t5 = r2[e49.readVarint()], n3 = i2[e49.readVarint()];
          this.properties[t5] = n3;
        }
      } else t3 === 24 ? this.type = e49.readVarint() : (t3 === 34 && (this._geometry = e49.pos), e49.skip(t3));
    }
  }
  loadGeometry() {
    if (this._geometry < 0) throw Error(`feature has no geometry`);
    let e49 = this._pbf;
    e49.pos = this._geometry;
    let t2 = e49.readVarint() + e49.pos, n2 = [], r2, i2 = 1, a2 = 0, o2 = 0, s2 = 0;
    for (; e49.pos < t2; ) {
      if (a2 <= 0) {
        let t3 = e49.readVarint();
        if (i2 = t3 & 7, a2 = t3 >> 3, a2 === 0) continue;
      }
      if (a2--, i2 === 1) o2 += e49.readSVarint(), s2 += e49.readSVarint(), r2 && n2.push(r2), r2 = [new l(o2, s2)];
      else if (i2 === 2) o2 += e49.readSVarint(), s2 += e49.readSVarint(), r2 && r2.push(new l(o2, s2));
      else if (i2 === 7) r2 && r2.push(r2[0].clone());
      else throw Error(`unknown command ${i2}`);
    }
    return r2 && n2.push(r2), n2;
  }
  bbox() {
    if (this._geometry < 0) throw Error(`feature has no geometry`);
    let e49 = this._pbf;
    e49.pos = this._geometry;
    let t2 = e49.readVarint() + e49.pos, n2 = 1, r2 = 0, i2 = 0, a2 = 0, o2 = 1 / 0, s2 = -1 / 0, c2 = 1 / 0, l2 = -1 / 0;
    for (; e49.pos < t2; ) {
      if (r2 <= 0) {
        let t3 = e49.readVarint();
        if (n2 = t3 & 7, r2 = t3 >> 3, r2 === 0) continue;
      }
      if (r2--, n2 === 1 || n2 === 2) i2 += e49.readSVarint(), a2 += e49.readSVarint(), i2 < o2 && (o2 = i2), i2 > s2 && (s2 = i2), a2 < c2 && (c2 = a2), a2 > l2 && (l2 = a2);
      else if (n2 !== 7) throw Error(`unknown command ${n2}`);
    }
    return [o2, c2, s2, l2];
  }
  toGeoJSON(e49, t2, n2) {
    let r2 = this.extent * 2 ** n2, i2 = this.extent * e49, a2 = this.extent * t2, o2 = this.loadGeometry();
    function s2(e50) {
      return [(e50.x + i2) * 360 / r2 - 180, 360 / Math.PI * Math.atan(Math.exp((1 - (e50.y + a2) * 2 / r2) * Math.PI)) - 90];
    }
    function c2(e50) {
      return e50.map(s2);
    }
    let l2;
    if (this.type === 1) {
      let e50 = [];
      for (let t4 of o2) e50.push(t4[0]);
      let t3 = c2(e50);
      l2 = e50.length === 1 ? { type: `Point`, coordinates: t3[0] } : { type: `MultiPoint`, coordinates: t3 };
    } else if (this.type === 2) {
      let e50 = o2.map(c2);
      l2 = e50.length === 1 ? { type: `LineString`, coordinates: e50[0] } : { type: `MultiLineString`, coordinates: e50 };
    } else if (this.type === 3) {
      let e50 = td(o2), t3 = [];
      for (let n3 of e50) t3.push(n3.map(c2));
      l2 = t3.length === 1 ? { type: `Polygon`, coordinates: t3[0] } : { type: `MultiPolygon`, coordinates: t3 };
    } else throw Error(`unknown feature type`);
    let u2 = { type: `Feature`, geometry: l2, properties: this.properties };
    return this.id != null && (u2.id = this.id), u2;
  }
};
ed.types = [`Unknown`, `Point`, `LineString`, `Polygon`];
function td(e49) {
  let t2 = e49.length;
  if (t2 <= 1) return [e49];
  let n2 = [], r2, i2;
  for (let a2 = 0; a2 < t2; a2++) {
    let t3 = nd(e49[a2]);
    t3 !== 0 && (i2 === void 0 && (i2 = t3 < 0), i2 === t3 < 0 ? (r2 && n2.push(r2), r2 = [e49[a2]]) : r2 && r2.push(e49[a2]));
  }
  return r2 && n2.push(r2), n2;
}
function nd(e49) {
  let t2 = 0;
  for (let n2 = 0, r2 = e49.length, i2 = r2 - 1, a2, o2; n2 < r2; i2 = n2++) a2 = e49[n2], o2 = e49[i2], t2 += (o2.x - a2.x) * (a2.y + o2.y);
  return t2;
}
var rd = class {
  constructor(e49, t2) {
    for (this.version = 1, this.name = ``, this.extent = 4096, this.length = 0, this._pbf = e49, this._keys = [], this._values = [], this._features = [], t2 === void 0 && (t2 = e49.length); e49.pos < t2; ) {
      let t3 = e49.readVarint();
      t3 === 10 ? this.name = e49.readString() : t3 === 18 ? (this._features.push(e49.pos), e49.skip(t3)) : t3 === 26 ? this._keys.push(e49.readString()) : t3 === 34 ? this._values.push(id(e49)) : t3 === 40 ? this.extent = e49.readVarint() : t3 === 120 ? this.version = e49.readVarint() : e49.skip(t3);
    }
    this.length = this._features.length;
  }
  feature(e49) {
    if (e49 < 0 || e49 >= this._features.length) throw Error(`feature index out of bounds`);
    this._pbf.pos = this._features[e49];
    let t2 = this._pbf.readVarint() + this._pbf.pos;
    return new ed(this._pbf, t2, this.extent, this._keys, this._values);
  }
};
function id(e49) {
  let t2 = null, n2 = e49.readVarint() + e49.pos;
  for (; e49.pos < n2; ) {
    let n3 = e49.readVarint();
    t2 = n3 === 10 ? e49.readString() : n3 === 21 ? e49.readFloat() : n3 === 25 ? e49.readDouble() : n3 === 32 ? e49.readVarint(true) : n3 === 40 ? e49.readVarint() : n3 === 48 ? e49.readSVarint() : n3 === 56 ? e49.readBoolean() : (e49.skip(n3), null);
  }
  if (t2 == null) throw Error(`unknown feature value`);
  return t2;
}
var ad = class {
  constructor(e49, t2 = e49.length) {
    let n2 = /* @__PURE__ */ Object.create(null);
    for (; e49.pos < t2; ) {
      let t3 = e49.readVarint();
      if (t3 === 26) {
        let t4 = new rd(e49, e49.readVarint() + e49.pos);
        t4.length && (n2[t4.name] = t4);
      } else e49.skip(t3);
    }
    this.layers = n2;
  }
};
const od = 63710088e-1;
var sd = class e42 {
  constructor(e49, t2) {
    if (isNaN(e49) || isNaN(t2)) throw Error(`Invalid LngLat object: (${e49}, ${t2})`);
    if (this.lng = +e49, this.lat = +t2, this.lat > 90 || this.lat < -90) throw Error(`Invalid LngLat latitude value: must be between -90 and 90`);
  }
  wrap() {
    return new e42(C(this.lng, -180, 180), this.lat);
  }
  toArray() {
    return [this.lng, this.lat];
  }
  toString() {
    return `LngLat(${this.lng}, ${this.lat})`;
  }
  distanceTo(e49) {
    let t2 = Math.PI / 180, n2 = this.lat * t2, r2 = e49.lat * t2, i2 = Math.sin(n2) * Math.sin(r2) + Math.cos(n2) * Math.cos(r2) * Math.cos((e49.lng - this.lng) * t2);
    return od * Math.acos(Math.min(i2, 1));
  }
  static convert(t2) {
    if (t2 instanceof e42) return t2;
    if (Array.isArray(t2) && (t2.length === 2 || t2.length === 3)) return new e42(Number(t2[0]), Number(t2[1]));
    if (!Array.isArray(t2) && typeof t2 == `object` && t2) return new e42(Number(`lng` in t2 ? t2.lng : t2.lon), Number(t2.lat));
    throw Error("`LngLatLike` argument must be specified as a LngLat instance, an object {lng: <lng>, lat: <lat>}, an object {lon: <lng>, lat: <lat>}, or an array of [<lng>, <lat>]");
  }
};
const cd = 2 * Math.PI * od;
function ld(e49) {
  return cd * Math.cos(e49 * Math.PI / 180);
}
function ud(e49) {
  return (180 + e49) / 360;
}
function dd(e49) {
  return (180 - 180 / Math.PI * Math.log(Math.tan(Math.PI / 4 + e49 * Math.PI / 360))) / 360;
}
function fd(e49, t2) {
  return e49 / ld(t2);
}
function pd(e49) {
  return e49 * 360 - 180;
}
function md(e49) {
  let t2 = 180 - e49 * 360;
  return 360 / Math.PI * Math.atan(Math.exp(t2 * Math.PI / 180)) - 90;
}
function hd(e49, t2) {
  return e49 * ld(md(t2));
}
function gd(e49) {
  return 1 / Math.cos(e49 * Math.PI / 180);
}
var _d = class e43 {
  constructor(e49, t2, n2 = 0) {
    this.x = +e49, this.y = +t2, this.z = +n2;
  }
  static fromLngLat(t2, n2 = 0) {
    let r2 = sd.convert(t2);
    return new e43(ud(r2.lng), dd(r2.lat), fd(n2, r2.lat));
  }
  toLngLat() {
    return new sd(pd(this.x), md(this.y));
  }
  toAltitude() {
    return hd(this.z, this.y);
  }
  meterInMercatorCoordinateUnits() {
    return 1 / cd * gd(md(this.y));
  }
};
function vd(e49, t2, n2) {
  let r2 = 1 / (1 << n2.z);
  return new _d(e49 / v * r2 + n2.x * r2, t2 / v * r2 + n2.y * r2);
}
function yd(e49, t2, n2) {
  return vd(e49, t2, n2).toLngLat();
}
function bd(e49, t2) {
  return t2 <= 0 || !e49 || e49.length === 0 ? e49 : e49.map((e50) => Td(e50, t2));
}
function xd(e49, t2) {
  let n2 = yd(v / 2, v / 2, t2), r2 = _d.fromLngLat(n2).meterInMercatorCoordinateUnits(), i2 = (1 << t2.z) * v;
  return e49 * r2 * i2;
}
const Sd = Math.cos(35 * Math.PI / 180);
function Cd(e49) {
  let t2 = [null];
  for (let n3 = 1; n3 < e49.length; n3++) {
    let r3 = e49[n3].sub(e49[n3 - 1]), i3 = !Qu(e49[n3], e49[n3 - 1]) && r3.mag() > 0;
    t2.push(i3 ? r3._perp() : null);
  }
  let n2 = e49.length - 1, r2 = n2 > 1 && e49[0].equals(e49[n2]), i2 = [null];
  for (let e50 = 1; e50 <= n2; e50++) {
    let a2 = t2[e50];
    if (!a2) {
      i2.push(null);
      continue;
    }
    let o2 = r2 && e50 === 1 ? t2[n2] : t2[e50 - 1], s2 = r2 && e50 === n2 ? t2[1] : t2[e50 + 1];
    i2.push({ start: wd(a2, o2), end: wd(a2, s2) });
  }
  return i2;
}
function wd(e49, t2) {
  if (!t2) return e49.unit();
  let n2 = e49.mag(), r2 = t2.mag();
  return e49.x * t2.x + e49.y * t2.y < Sd * n2 * r2 || n2 >= 2 * r2 ? e49.unit() : r2 >= 2 * n2 ? t2.unit() : e49.add(t2)._unit();
}
function Td(e49, t2) {
  if (!e49 || e49.length < 3) return e49;
  let n2 = e49[0].x === e49[e49.length - 1].x && e49[0].y === e49[e49.length - 1].y, r2 = n2 ? e49.length - 1 : e49.length;
  if (r2 < 3) return e49;
  let i2 = [], a2 = Qu(e49[r2 - 1], e49[0]);
  for (let n3 = 0; n3 < r2; n3++) {
    let o3 = e49[(n3 - 1 + r2) % r2], s2 = e49[n3], c2 = e49[(n3 + 1) % r2], l2 = Qu(s2, c2);
    a2 || l2 ? i2.push(s2.clone()) : Dd(i2, o3, s2, c2, t2), a2 = l2;
  }
  let o2 = Ed(i2);
  return o2.length < 3 ? e49 : (n2 && o2.push(o2[0].clone()), o2);
}
function Ed(e49) {
  let t2 = [];
  for (let n2 of e49) {
    let e50 = n2.round(), r2 = t2[t2.length - 1];
    ((r2 == null ? void 0 : r2.x) !== e50.x || (r2 == null ? void 0 : r2.y) !== e50.y) && t2.push(e50);
  }
  for (; t2.length > 1 && t2[0].x === t2[t2.length - 1].x && t2[0].y === t2[t2.length - 1].y; ) t2.pop();
  return t2;
}
function Dd(e49, t2, n2, r2, i2) {
  let a2 = t2.sub(n2), o2 = r2.sub(n2), s2 = a2.mag(), c2 = o2.mag();
  if (s2 < 1e-6 || c2 < 1e-6) {
    e49.push(n2.clone());
    return;
  }
  a2._div(s2), o2._div(c2);
  let l2 = a2.x * o2.x + a2.y * o2.y;
  if (Math.abs(l2) > Math.cos(5 * Math.PI / 180)) {
    e49.push(n2.clone());
    return;
  }
  let u2 = 0.2, d2 = Math.min(i2, s2 * u2, c2 * u2), f2 = n2.add(a2.mult(d2)), p2 = n2.add(o2.mult(d2)), m2 = Math.sqrt((1 + l2) / 2), h2 = n2.add(a2.add(o2)._unit()._mult(d2 / m2)), g2 = f2.sub(h2).angleWith(p2.sub(h2)), _2 = Math.max(2, Math.ceil(Math.abs(g2) / (Math.PI / 6) - 1e-6));
  for (let t3 = 0; t3 <= _2; t3++) e49.push(f2.rotateAround(t3 / _2 * g2, h2));
}
const Od = 8192;
function kd(e49, t2, n2, r2, i2, a2, o2, s2) {
  e49.emplaceBack(t2, n2, Math.floor(r2 * Od) * 2 + o2, i2 * Od * 2, a2 * Od * 2, Math.round(s2));
}
var Ad = class {
  constructor(e49) {
    this.zoom = e49.zoom, this.overscaling = e49.overscaling, this.layers = e49.layers, this.layerIds = this.layers.map((e50) => e50.id), this.index = e49.index, this.hasDependencies = false, this.layoutVertexArray = new As(), this.centroidVertexArray = new Ds(), this.indexArray = new Bs(), this.programConfigurations = new mc(e49.layers, e49.zoom), this.segments = new Ws(), this.stateDependentLayerIds = this.layers.filter((e50) => e50.isStateDependent()).map((e50) => e50.id);
  }
  populate(e49, t2, n2) {
    this.features = [], this.hasDependencies = Ol(`fill-extrusion`, this.layers, t2);
    let r2 = new K(this.zoom), i2 = this.layers[0], a2 = i2.layout.get(`fill-extrusion-rounded-corner-distance`), o2 = a2 > 0 ? xd(a2, n2) : 0, s2 = i2._featureFilter.needGeometry;
    for (let { feature: a3, id: c2, index: l2, sourceLayerIndex: u2 } of e49) {
      let e50 = xc(a3, s2);
      if (!i2._featureFilter.filter(r2, e50, n2)) continue;
      let d2 = { id: c2, sourceLayerIndex: u2, index: l2, geometry: bd(s2 ? e50.geometry : bc(a3), o2), properties: a3.properties, type: a3.type, patterns: {} };
      this.hasDependencies ? this.features.push(kl(`fill-extrusion`, this.layers, d2, { zoom: this.zoom }, t2)) : this.addFeature(d2, d2.geometry, l2, n2, {}, t2.subdivisionGranularity), t2.featureIndex.insert(a3, d2.geometry, l2, u2, this.index, true);
    }
  }
  addFeatures({ options: e49, canonical: t2, patternPositions: n2 }) {
    for (let r2 of this.features) {
      let { geometry: i2 } = r2;
      this.addFeature(r2, i2, r2.index, t2, n2, e49.subdivisionGranularity);
    }
  }
  update(e49, t2, n2) {
    this.stateDependentLayers.length && this.programConfigurations.updatePaintArrays(e49, t2, this.stateDependentLayers, { imagePositions: n2 });
  }
  isEmpty() {
    return this.layoutVertexArray.length === 0 && this.centroidVertexArray.length === 0;
  }
  uploadPending() {
    return !this.uploaded || this.programConfigurations.needsUpload;
  }
  upload(e49) {
    this.uploaded || (this.layoutVertexBuffer = e49.createVertexBuffer(this.layoutVertexArray, Xu), this.centroidVertexBuffer = e49.createVertexBuffer(this.centroidVertexArray, Yu.members, true), this.indexBuffer = e49.createIndexBuffer(this.indexArray)), this.programConfigurations.upload(e49), this.uploaded = true;
  }
  destroy() {
    this.layoutVertexBuffer && (this.layoutVertexBuffer.destroy(), this.indexBuffer.destroy(), this.programConfigurations.destroy(), this.segments.destroy(), this.centroidVertexBuffer.destroy());
  }
  addFeature(e49, t2, n2, r2, i2, a2) {
    for (let n3 of Ft(t2, 500)) {
      let t3 = { x: 0, y: 0, sampleCount: 0 }, i3 = this.layoutVertexArray.length;
      this.processPolygon(t3, r2, e49, n3, a2);
      let o2 = this.layoutVertexArray.length - i3, s2 = Math.floor(t3.x / t3.sampleCount), c2 = Math.floor(t3.y / t3.sampleCount);
      for (let e50 = 0; e50 < o2; e50++) this.centroidVertexArray.emplaceBack(s2, c2);
    }
    this.programConfigurations.populatePaintArrays(this.layoutVertexArray.length, e49, n2, { imagePositions: i2, canonical: r2 });
  }
  processPolygon(e49, t2, n2, r2, i2) {
    if (r2.length < 1 || $u(r2[0])) return;
    for (let t3 of r2) t3.length !== 0 && jd(e49, t3);
    let a2 = { segment: this.segments.prepareSegment(4, this.layoutVertexArray, this.indexArray) }, o2 = i2.fill.getGranularityForZoomLevel(t2.z), s2 = ed.types[n2.type] === `Polygon`;
    for (let e50 of r2) {
      if (e50.length === 0 || $u(e50)) continue;
      let t3 = ju(e50, o2, s2);
      this._generateSideFaces(t3, a2);
    }
    if (!s2) return;
    let c2 = Au(r2, t2, o2, false), l2 = this.layoutVertexArray;
    Lu((e50, t3) => {
      kd(l2, e50, t3, 0, 0, 1, 1, 0);
    }, this.segments, this.layoutVertexArray, this.indexArray, c2.verticesFlattened, c2.indicesTriangles);
  }
  _generateSideFaces(e49, t2) {
    let n2 = 0, r2 = this.layers[0].layout.get(`fill-extrusion-rounded-corner-distance`) > 0 ? Cd(e49) : null;
    for (let i2 = 1; i2 < e49.length; i2++) {
      let a2 = e49[i2], o2 = e49[i2 - 1];
      if (Qu(a2, o2) || r2 && !r2[i2]) continue;
      t2.segment.vertexLength + 4 > Ws.MAX_VERTEX_ARRAY_LENGTH && (t2.segment = this.segments.prepareSegment(4, this.layoutVertexArray, this.indexArray));
      let s2 = a2.sub(o2)._perp()._unit(), { start: c2, end: l2 } = r2 ? r2[i2] : { start: s2, end: s2 }, u2 = o2.dist(a2);
      n2 + u2 > 32768 && (n2 = 0), kd(this.layoutVertexArray, a2.x, a2.y, l2.x, l2.y, 0, 0, n2), kd(this.layoutVertexArray, a2.x, a2.y, l2.x, l2.y, 0, 1, n2), n2 += u2, kd(this.layoutVertexArray, o2.x, o2.y, c2.x, c2.y, 0, 0, n2), kd(this.layoutVertexArray, o2.x, o2.y, c2.x, c2.y, 0, 1, n2);
      let d2 = t2.segment.vertexLength;
      this.indexArray.emplaceBack(d2, d2 + 2, d2 + 1), this.indexArray.emplaceBack(d2 + 1, d2 + 2, d2 + 3), t2.segment.vertexLength += 4, t2.segment.primitiveLength += 2;
    }
  }
};
function jd(e49, t2) {
  for (let n2 = 0; n2 < t2.length; n2++) {
    let r2 = t2[n2];
    (n2 !== t2.length - 1 || t2[0].x !== r2.x || t2[0].y !== r2.y) && (e49.x += r2.x, e49.y += r2.y, e49.sampleCount++);
  }
}
G(`FillExtrusionBucket`, Ad, { omit: [`layers`, `features`] });
let Md;
const Nd = () => Md ||= new Fo({ "fill-extrusion-rounded-corner-distance": new q(M[`layout_fill-extrusion`][`fill-extrusion-rounded-corner-distance`], `fill-extrusion-rounded-corner-distance`) });
let Pd;
const Fd = () => Pd ||= new Fo({ "fill-extrusion-opacity": new q(M[`paint_fill-extrusion`][`fill-extrusion-opacity`], `fill-extrusion-opacity`), "fill-extrusion-color": new J(M[`paint_fill-extrusion`][`fill-extrusion-color`], `fill-extrusion-color`), "fill-extrusion-translate": new q(M[`paint_fill-extrusion`][`fill-extrusion-translate`], `fill-extrusion-translate`), "fill-extrusion-translate-anchor": new q(M[`paint_fill-extrusion`][`fill-extrusion-translate-anchor`], `fill-extrusion-translate-anchor`), "fill-extrusion-pattern": new Mo(M[`paint_fill-extrusion`][`fill-extrusion-pattern`], `fill-extrusion-pattern`), "fill-extrusion-height": new J(M[`paint_fill-extrusion`][`fill-extrusion-height`], `fill-extrusion-height`), "fill-extrusion-base": new J(M[`paint_fill-extrusion`][`fill-extrusion-base`], `fill-extrusion-base`), "fill-extrusion-vertical-gradient": new q(M[`paint_fill-extrusion`][`fill-extrusion-vertical-gradient`], `fill-extrusion-vertical-gradient`) });
var Id = { get paint() {
  return Fd();
}, get layout() {
  return Nd();
} }, Ld = class extends Ro {
  constructor(e49, t2) {
    super(e49, Id, t2);
  }
  createBucket(e49) {
    return new Ad(e49);
  }
  queryRadius() {
    return zc(this.paint.get(`fill-extrusion-translate`));
  }
  is3D() {
    return true;
  }
  queryIntersectsFeature({ queryGeometry: e49, feature: t2, featureState: n2, geometry: r2, transform: i2, pixelsToTileUnits: a2, pixelPosMatrix: o2 }) {
    let s2 = Bc(e49, this.paint.get(`fill-extrusion-translate`), this.paint.get(`fill-extrusion-translate-anchor`), -i2.bearingInRadians, a2), c2 = this.paint.get(`fill-extrusion-height`).evaluate(t2, n2), l2 = this.paint.get(`fill-extrusion-base`).evaluate(t2, n2), u2 = Hd(s2, o2, 0), d2 = Vd(r2, l2, c2, o2), f2 = d2[0], p2 = d2[1];
    return Bd(f2, p2, u2);
  }
};
function Rd(e49, t2) {
  return e49.x * t2.x + e49.y * t2.y;
}
function zd(e49, t2) {
  if (e49.length === 1) {
    let n2 = 0, r2 = t2[n2++], i2;
    for (; !i2 || r2.equals(i2); ) if (i2 = t2[n2++], !i2) return 1 / 0;
    for (; n2 < t2.length; n2++) {
      let a2 = t2[n2], o2 = e49[0], s2 = i2.sub(r2), c2 = a2.sub(r2), l2 = o2.sub(r2), u2 = Rd(s2, s2), d2 = Rd(s2, c2), f2 = Rd(c2, c2), p2 = Rd(l2, s2), m2 = Rd(l2, c2), h2 = u2 * f2 - d2 * d2, g2 = (f2 * p2 - d2 * m2) / h2, _2 = (u2 * m2 - d2 * p2) / h2, v2 = 1 - g2 - _2, y2 = r2.z * v2 + i2.z * g2 + a2.z * _2;
      if (isFinite(y2)) return y2;
    }
    return 1 / 0;
  }
  {
    let e50 = 1 / 0;
    for (let n2 of t2) e50 = Math.min(e50, n2.z);
    return e50;
  }
}
function Bd(e49, t2, n2) {
  let r2 = 1 / 0;
  Dc(n2, t2) && (r2 = zd(n2, t2[0]));
  for (let i2 = 0; i2 < t2.length; i2++) {
    let a2 = t2[i2], o2 = e49[i2];
    for (let e50 = 0; e50 < a2.length - 1; e50++) {
      let t3 = a2[e50], i3 = a2[e50 + 1], s2 = o2[e50], c2 = [t3, i3, o2[e50 + 1], s2, t3];
      Tc(n2, c2) && (r2 = Math.min(r2, zd(n2, c2)));
    }
  }
  return r2 !== 1 / 0 && r2;
}
function Vd(e49, t2, n2, r2) {
  let i2 = [], a2 = [], o2 = r2[8] * t2, s2 = r2[9] * t2, c2 = r2[10] * t2, u2 = r2[11] * t2, d2 = r2[8] * n2, f2 = r2[9] * n2, p2 = r2[10] * n2, m2 = r2[11] * n2;
  for (let t3 of e49) {
    let e50 = [], n3 = [];
    for (let i3 of t3) {
      let t4 = i3.x, a3 = i3.y, h2 = r2[0] * t4 + r2[4] * a3 + r2[12], g2 = r2[1] * t4 + r2[5] * a3 + r2[13], _2 = r2[2] * t4 + r2[6] * a3 + r2[14], v2 = r2[3] * t4 + r2[7] * a3 + r2[15], y2 = h2 + o2, b2 = g2 + s2, x2 = _2 + c2, S2 = v2 + u2, C2 = h2 + d2, w2 = g2 + f2, T2 = _2 + p2, E2 = v2 + m2, D2 = new l(y2 / S2, b2 / S2);
      D2.z = x2 / S2, e50.push(D2);
      let ee2 = new l(C2 / E2, w2 / E2);
      ee2.z = T2 / E2, n3.push(ee2);
    }
    i2.push(e50), a2.push(n3);
  }
  return [i2, a2];
}
function Hd(e49, t2, n2) {
  let r2 = [];
  for (let i2 of e49) {
    let e50 = [i2.x, i2.y, n2, 1];
    _(e50, e50, t2), r2.push(new l(e50[0] / e50[3], e50[1] / e50[3]));
  }
  return r2;
}
function Ud(e49, t2, n2, r2) {
  let i2 = [t2, n2];
  for (; i2.length; ) n2 = i2.pop(), t2 = i2.pop(), Wd(e49, t2, n2, r2, i2);
}
function Wd(e49, t2, n2, r2, i2) {
  let a2 = r2, o2 = t2 + (n2 - t2 >> 1), s2 = n2 - t2, c2, l2 = e49[t2], u2 = e49[t2 + 1], d2 = e49[n2], f2 = e49[n2 + 1];
  for (let r3 = t2 + 3; r3 < n2; r3 += 3) {
    let t3 = Gd(e49[r3], e49[r3 + 1], l2, u2, d2, f2);
    if (t3 > a2) {
      c2 = r3, a2 = t3;
      continue;
    }
    if (t3 === a2) {
      let e50 = Math.abs(r3 - o2);
      e50 < s2 && (c2 = r3, s2 = e50);
    }
  }
  a2 > r2 && (e49[c2 + 2] = a2, c2 - t2 > 3 && i2.push(t2, c2), n2 - c2 > 3 && i2.push(c2, n2));
}
function Gd(e49, t2, n2, r2, i2, a2) {
  let o2 = i2 - n2, s2 = a2 - r2;
  if (o2 !== 0 || s2 !== 0) {
    let c2 = ((e49 - n2) * o2 + (t2 - r2) * s2) / (o2 * o2 + s2 * s2);
    c2 > 1 ? (n2 = i2, r2 = a2) : c2 > 0 && (n2 += o2 * c2, r2 += s2 * c2);
  }
  return o2 = e49 - n2, s2 = t2 - r2, o2 * o2 + s2 * s2;
}
function Kd(e49, t2, n2, r2) {
  let i2 = { type: t2, geom: n2 }, a2 = { id: e49 ?? null, type: i2.type, geometry: i2.geom, tags: r2, minX: 1 / 0, minY: 1 / 0, maxX: -1 / 0, maxY: -1 / 0 };
  switch (i2.type) {
    case `Point`:
    case `MultiPoint`:
      Jd(a2, i2.geom);
      break;
    case `LineString`:
      Jd(a2, i2.geom.points);
      break;
    case `Polygon`:
      Jd(a2, i2.geom[0].points);
      break;
    case `MultiLineString`:
      for (let e50 of i2.geom) Jd(a2, e50.points);
      break;
    case `MultiPolygon`:
      for (let e50 of i2.geom) Jd(a2, e50[0].points);
  }
  return a2;
}
function qd(e49) {
  let t2 = e49;
  e49.points.length > 64 && (t2.points = new Float64Array(e49.points));
}
function Jd(e49, t2) {
  for (let n2 = 0; n2 < t2.length; n2 += 3) e49.minX = Math.min(e49.minX, t2[n2]), e49.minY = Math.min(e49.minY, t2[n2 + 1]), e49.maxX = Math.max(e49.maxX, t2[n2]), e49.maxY = Math.max(e49.maxY, t2[n2 + 1]);
}
function Yd(e49, t2) {
  let n2 = [];
  switch (e49.type) {
    case `FeatureCollection`:
      for (let r2 = 0; r2 < e49.features.length; r2++) Xd(n2, e49.features[r2], t2, r2);
      break;
    case `Feature`:
      Xd(n2, e49, t2);
      break;
    default:
      Xd(n2, { type: `Feature`, geometry: e49, properties: void 0 }, t2);
  }
  return n2;
}
function Xd(e49, t2, n2, r2, i2 = 0) {
  var _a3;
  if (!t2.geometry) return;
  if (i2 > 1024) throw Error(`GeometryCollection nesting exceeds supported depth: 1024`);
  if (t2.geometry.type === `GeometryCollection`) {
    Qd(e49, t2, t2.geometry, n2, r2, i2 + 1);
    return;
  }
  if (!((_a3 = t2.geometry.coordinates) == null ? void 0 : _a3.length)) return;
  let a2 = Zd(t2, n2, r2), o2 = (n2.tolerance / ((1 << n2.maxZoom) * n2.extent)) ** 2;
  switch (t2.geometry.type) {
    case `Point`:
      $d(e49, a2, t2.geometry, t2.properties);
      return;
    case `MultiPoint`:
      ef(e49, a2, t2.geometry, t2.properties);
      return;
    case `LineString`:
      tf(e49, a2, t2.geometry, o2, t2.properties);
      return;
    case `MultiLineString`:
      nf(e49, a2, t2.geometry, o2, n2, t2.properties);
      return;
    case `Polygon`:
      rf(e49, a2, t2.geometry, o2, t2.properties);
      return;
    case `MultiPolygon`:
      af(e49, a2, t2.geometry, o2, t2.properties);
      return;
    default:
      throw Error(`Input data is not a valid GeoJSON object.`);
  }
}
function Zd(e49, t2, n2) {
  var _a3;
  return t2.promoteId ? (_a3 = e49.properties) == null ? void 0 : _a3[t2.promoteId] : t2.generateId ? n2 || 0 : e49.id;
}
function Qd(e49, t2, n2, r2, i2, a2 = 0) {
  for (let o2 of n2.geometries) Xd(e49, { id: t2.id, type: `Feature`, geometry: o2, properties: t2.properties }, r2, i2, a2);
}
function $d(e49, t2, n2, r2) {
  let i2 = [];
  i2.push(cf(n2.coordinates[0]), lf(n2.coordinates[1]), 0), e49.push(Kd(t2, `Point`, i2, r2));
}
function ef(e49, t2, n2, r2) {
  let i2 = [];
  for (let e50 of n2.coordinates) i2.push(cf(e50[0]), lf(e50[1]), 0);
  e49.push(Kd(t2, `MultiPoint`, i2, r2));
}
function tf(e49, t2, n2, r2, i2) {
  let a2 = { points: [] };
  of(n2.coordinates, a2, r2, false), e49.push(Kd(t2, `LineString`, a2, i2));
}
function nf(e49, t2, n2, r2, i2, a2) {
  if (i2.lineMetrics) for (let i3 of n2.coordinates) {
    let n3 = { points: [] };
    of(i3, n3, r2, false), e49.push(Kd(t2, `LineString`, n3, a2));
  }
  else {
    let i3 = [];
    sf(n2.coordinates, i3, r2, false), e49.push(Kd(t2, `MultiLineString`, i3, a2));
  }
}
function rf(e49, t2, n2, r2, i2) {
  let a2 = [];
  sf(n2.coordinates, a2, r2, true), e49.push(Kd(t2, `Polygon`, a2, i2));
}
function af(e49, t2, n2, r2, i2) {
  let a2 = [];
  for (let e50 of n2.coordinates) {
    let t3 = [];
    sf(e50, t3, r2, true), a2.push(t3);
  }
  e49.push(Kd(t2, `MultiPolygon`, a2, i2));
}
function of(e49, t2, n2, r2) {
  let i2, a2, o2 = 0;
  for (let n3 = 0; n3 < e49.length; n3++) {
    let s3 = cf(e49[n3][0]), c2 = lf(e49[n3][1]);
    t2.points.push(s3, c2, 0), n3 > 0 && (o2 += r2 ? (i2 * c2 - s3 * a2) / 2 : Math.sqrt((s3 - i2) ** 2 + (c2 - a2) ** 2)), i2 = s3, a2 = c2;
  }
  let s2 = t2.points.length - 3;
  t2.points[2] = 1, n2 > 0 && Ud(t2.points, 0, s2, n2), t2.points[s2 + 2] = 1, qd(t2), t2.size = Math.abs(o2), t2.start = 0, t2.end = t2.size;
}
function sf(e49, t2, n2, r2) {
  for (let i2 = 0; i2 < e49.length; i2++) {
    let a2 = { points: [] };
    of(e49[i2], a2, n2, r2), t2.push(a2);
  }
}
function cf(e49) {
  return e49 / 360 + 0.5;
}
function lf(e49) {
  let t2 = Math.sin(e49 * Math.PI / 180), n2 = 0.5 - 0.25 * Math.log((1 + t2) / (1 - t2)) / Math.PI;
  return n2 < 0 ? 0 : n2 > 1 ? 1 : n2;
}
function uf(e49) {
  return { type: `FeatureCollection`, features: e49.map((e50) => df(e50)) };
}
function df(e49) {
  let t2 = { type: `Feature`, geometry: ff(e49), properties: e49.tags };
  return e49.id != null && (t2.id = e49.id), t2;
}
function ff(e49) {
  let { type: t2, geometry: n2 } = e49;
  switch (t2) {
    case `Point`:
      return { type: t2, coordinates: mf(n2[0], n2[1]) };
    case `MultiPoint`:
      return { type: t2, coordinates: pf(n2) };
    case `LineString`:
      return { type: t2, coordinates: pf(n2.points) };
    case `MultiLineString`:
    case `Polygon`:
      return { type: t2, coordinates: n2.map((e50) => pf(e50.points)) };
    case `MultiPolygon`:
      return { type: t2, coordinates: n2.map((e50) => e50.map((e51) => pf(e51.points))) };
  }
}
function pf(e49) {
  let t2 = [];
  for (let n2 = 0; n2 < e49.length; n2 += 3) t2.push(mf(e49[n2], e49[n2 + 1]));
  return t2;
}
function mf(e49, t2) {
  return [hf(e49), gf(t2)];
}
function hf(e49) {
  return (e49 - 0.5) * 360;
}
function gf(e49) {
  let t2 = (180 - e49 * 360) * Math.PI / 180;
  return 360 * Math.atan(Math.exp(t2)) / Math.PI - 90;
}
function _f(e49, t2, n2, r2, i2, a2, o2, s2) {
  if (n2 /= t2, r2 /= t2, a2 >= n2 && o2 < r2) return e49;
  if (o2 < n2 || a2 >= r2) return null;
  let c2 = [];
  for (let t3 of e49) {
    let e50 = i2 === 0 ? t3.minX : t3.minY, a3 = i2 === 0 ? t3.maxX : t3.maxY;
    if (e50 >= n2 && a3 < r2) {
      c2.push(t3);
      continue;
    }
    if (!(a3 < n2 || e50 >= r2)) switch (t3.type) {
      case `Point`:
      case `MultiPoint`:
        vf(t3, c2, n2, r2, i2);
        continue;
      case `LineString`:
        yf(t3, c2, n2, r2, i2, s2);
        continue;
      case `MultiLineString`:
        bf(t3, c2, n2, r2, i2);
        continue;
      case `Polygon`:
        xf(t3, c2, n2, r2, i2);
        continue;
      case `MultiPolygon`:
        Sf(t3, c2, n2, r2, i2);
        continue;
    }
  }
  return c2.length ? c2 : null;
}
function vf(e49, t2, n2, r2, i2) {
  let a2 = [];
  if (Cf(e49.geometry, a2, n2, r2, i2), !a2.length) return;
  let o2 = a2.length === 3 ? `Point` : `MultiPoint`;
  t2.push(Kd(e49.id, o2, a2, e49.tags));
}
function yf(e49, t2, n2, r2, i2, a2) {
  let o2 = [];
  if (wf(e49.geometry, o2, n2, r2, i2, false, a2.lineMetrics), o2.length) {
    if (a2.lineMetrics) {
      for (let n3 of o2) t2.push(Kd(e49.id, `LineString`, n3, e49.tags));
      return;
    }
    if (o2.length > 1) {
      t2.push(Kd(e49.id, `MultiLineString`, o2, e49.tags));
      return;
    }
    t2.push(Kd(e49.id, `LineString`, o2[0], e49.tags));
  }
}
function bf(e49, t2, n2, r2, i2) {
  let a2 = [];
  if (Ef(e49.geometry, a2, n2, r2, i2, false), a2.length) {
    if (a2.length === 1) {
      t2.push(Kd(e49.id, `LineString`, a2[0], e49.tags));
      return;
    }
    t2.push(Kd(e49.id, `MultiLineString`, a2, e49.tags));
  }
}
function xf(e49, t2, n2, r2, i2) {
  let a2 = [];
  Ef(e49.geometry, a2, n2, r2, i2, true), a2.length && t2.push(Kd(e49.id, `Polygon`, a2, e49.tags));
}
function Sf(e49, t2, n2, r2, i2) {
  let a2 = [];
  for (let t3 of e49.geometry) {
    let e50 = [];
    Ef(t3, e50, n2, r2, i2, true), e50.length && a2.push(e50);
  }
  a2.length && t2.push(Kd(e49.id, `MultiPolygon`, a2, e49.tags));
}
function Cf(e49, t2, n2, r2, i2) {
  for (let a2 = 0; a2 < e49.length; a2 += 3) {
    let o2 = e49[a2 + i2];
    o2 >= n2 && o2 <= r2 && Df(t2, e49[a2], e49[a2 + 1], e49[a2 + 2]);
  }
}
function wf(e49, t2, n2, r2, i2, a2, o2) {
  let s2 = Tf(e49), c2 = i2 === 0 ? Of : kf, l2 = e49.start, u2, d2;
  for (let f3 = 0; f3 < e49.points.length - 3; f3 += 3) {
    let p3 = e49.points[f3], m3 = e49.points[f3 + 1], h3 = e49.points[f3 + 2], g3 = e49.points[f3 + 3], _2 = e49.points[f3 + 4], v2 = i2 === 0 ? p3 : m3, y2 = i2 === 0 ? g3 : _2, b2 = false;
    o2 && (u2 = Math.sqrt((p3 - g3) ** 2 + (m3 - _2) ** 2)), v2 < n2 ? y2 > n2 && (d2 = c2(s2, p3, m3, g3, _2, n2), o2 && (s2.start = l2 + u2 * d2)) : v2 > r2 ? y2 < r2 && (d2 = c2(s2, p3, m3, g3, _2, r2), o2 && (s2.start = l2 + u2 * d2)) : Df(s2.points, p3, m3, h3), y2 < n2 && v2 >= n2 && (d2 = c2(s2, p3, m3, g3, _2, n2), b2 = true), y2 > r2 && v2 <= r2 && (d2 = c2(s2, p3, m3, g3, _2, r2), b2 = true), !a2 && b2 && (o2 && (s2.end = l2 + u2 * d2), t2.push(s2), s2 = Tf(e49)), o2 && (l2 += u2);
  }
  let f2 = e49.points.length - 3, p2 = e49.points[f2], m2 = e49.points[f2 + 1], h2 = e49.points[f2 + 2], g2 = i2 === 0 ? p2 : m2;
  g2 >= n2 && g2 <= r2 && Df(s2.points, p2, m2, h2), f2 = s2.points.length - 3, a2 && f2 >= 3 && (s2.points[f2] !== s2.points[0] || s2.points[f2 + 1] !== s2.points[1]) && Df(s2.points, s2.points[0], s2.points[1], s2.points[2]), s2.points.length && (qd(s2), t2.push(s2));
}
function Tf(e49) {
  return { points: [], size: e49.size, start: e49.start, end: e49.end };
}
function Ef(e49, t2, n2, r2, i2, a2) {
  for (let o2 of e49) wf(o2, t2, n2, r2, i2, a2, false);
}
function Df(e49, t2, n2, r2) {
  e49.push(t2, n2, r2);
}
function Of(e49, t2, n2, r2, i2, a2) {
  let o2 = (a2 - t2) / (r2 - t2);
  return Df(e49.points, a2, n2 + (i2 - n2) * o2, 1), o2;
}
function kf(e49, t2, n2, r2, i2, a2) {
  let o2 = (a2 - n2) / (i2 - n2);
  return Df(e49.points, t2 + (r2 - t2) * o2, a2, 1), o2;
}
function Af(e49, t2) {
  let n2 = t2.buffer / t2.extent, r2 = e49, i2 = _f(e49, 1, -1 - n2, n2, 0, -1, 2, t2), a2 = _f(e49, 1, 1 - n2, 2 + n2, 0, -1, 2, t2);
  return !i2 && !a2 ? r2 : (r2 = _f(e49, 1, -n2, 1 + n2, 0, -1, 2, t2) || [], i2 && (r2 = jf(i2, 1).concat(r2)), a2 && (r2 = r2.concat(jf(a2, -1))), r2);
}
function jf(e49, t2) {
  let n2 = [];
  for (let r2 of e49) switch (r2.type) {
    case `Point`:
    case `MultiPoint`: {
      let e50 = Mf(r2.geometry, t2);
      n2.push(Kd(r2.id, r2.type, e50, r2.tags));
      continue;
    }
    case `LineString`: {
      let e50 = Nf(r2.geometry, t2);
      n2.push(Kd(r2.id, r2.type, e50, r2.tags));
      continue;
    }
    case `MultiLineString`:
    case `Polygon`: {
      let e50 = [];
      for (let n3 of r2.geometry) e50.push(Nf(n3, t2));
      n2.push(Kd(r2.id, r2.type, e50, r2.tags));
      continue;
    }
    case `MultiPolygon`: {
      let e50 = [];
      for (let n3 of r2.geometry) {
        let r3 = [];
        for (let e51 of n3) r3.push(Nf(e51, t2));
        e50.push(r3);
      }
      n2.push(Kd(r2.id, r2.type, e50, r2.tags));
      continue;
    }
  }
  return n2;
}
function Mf(e49, t2) {
  let n2 = [];
  for (let r2 = 0; r2 < e49.length; r2 += 3) n2.push(e49[r2] + t2, e49[r2 + 1], e49[r2 + 2]);
  return n2;
}
function Nf(e49, t2) {
  let n2 = { points: [], size: e49.size };
  e49.start !== void 0 && (n2.start = e49.start, n2.end = e49.end);
  for (let r2 = 0; r2 < e49.points.length; r2 += 3) n2.points.push(e49.points[r2] + t2, e49.points[r2 + 1], e49.points[r2 + 2]);
  return qd(n2), n2;
}
function Pf(e49, t2, n2) {
  let r2 = Lf(t2, n2), i2 = [];
  if (r2.removeAll && (i2 = e49, e49 = []), r2.remove.size || r2.add.size) {
    let t3 = [];
    for (let n3 of e49) (r2.remove.has(n3.id) || r2.add.has(n3.id)) && t3.push(n3);
    if (t3.length) {
      i2 = i2.concat(t3);
      let n3 = new Set(t3.map((e50) => e50.id));
      e49 = e49.filter((e50) => !n3.has(e50.id));
    }
    if (r2.add.size) {
      let t4 = Yd({ type: `FeatureCollection`, features: Array.from(r2.add.values()) }, n2);
      t4 = Af(t4, n2), i2 = i2.concat(t4), e49 = e49.concat(t4);
    }
  }
  if (r2.update.size) {
    let t3 = /* @__PURE__ */ new Map(), a2 = [];
    for (let n3 of e49) r2.update.has(n3.id) ? t3.set(n3.id, [...t3.get(n3.id) || [], n3]) : a2.push(n3);
    for (let [e50, o2] of r2.update) {
      let r3 = t3.get(e50);
      if (!r3 || r3.length === 0) continue;
      let s2 = Ff(r3, o2, n2);
      i2 = i2.concat(r3, s2), a2 = a2.concat(s2);
    }
    e49 = a2;
  }
  return { affected: i2, source: e49 };
}
function Ff(e49, t2, n2) {
  var _a3, _b3;
  let r2 = !!t2.newGeometry, i2 = t2.removeAllProperties || ((_a3 = t2.removeProperties) == null ? void 0 : _a3.length) > 0 || ((_b3 = t2.addOrUpdateProperties) == null ? void 0 : _b3.length) > 0;
  if (r2) {
    let r3 = e49[0], a2 = Yd({ type: `FeatureCollection`, features: [{ type: `Feature`, id: r3.id, geometry: t2.newGeometry, properties: i2 ? If(r3.tags, t2) : r3.tags }] }, n2);
    return a2 = Af(a2, n2), a2;
  }
  if (i2) {
    let n3 = [];
    for (let r3 of e49) {
      let e50 = { ...r3 };
      e50.tags = If(e50.tags, t2), n3.push(e50);
    }
    return n3;
  }
  return e49;
}
function If(e49, t2) {
  if (t2.removeAllProperties) return {};
  let n2 = { ...e49 || {} };
  if (t2.removeProperties) for (let e50 of t2.removeProperties) delete n2[e50];
  if (t2.addOrUpdateProperties) for (let { key: e50, value: r2 } of t2.addOrUpdateProperties) n2[e50] = r2;
  return n2;
}
function Lf(e49, t2) {
  var _a3, _b3;
  return e49 ? { removeAll: e49.removeAll, remove: new Set(e49.remove || []), add: new Map((_a3 = e49.add) == null ? void 0 : _a3.map((e50) => [t2.promoteId ? e50.properties[t2.promoteId] : e50.id, e50])), update: new Map((_b3 = e49.update) == null ? void 0 : _b3.map((e50) => [e50.id, e50])) } : { remove: /* @__PURE__ */ new Set(), add: /* @__PURE__ */ new Map(), update: /* @__PURE__ */ new Map() };
}
const Rf = [Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array, Float32Array, Float64Array], zf = /* @__PURE__ */ new Uint32Array(96);
var Bf = class e44 {
  static from(t2) {
    if (!t2 || t2.byteLength === void 0 || t2.buffer) throw Error(`Data must be an instance of ArrayBuffer or SharedArrayBuffer.`);
    let [n2, r2] = new Uint8Array(t2, 0, 2);
    if (n2 !== 219) throw Error(`Data does not appear to be in a KDBush format.`);
    let i2 = r2 >> 4;
    if (i2 !== 1) throw Error(`Got v${i2} data when expected v1.`);
    let a2 = Rf[r2 & 15];
    if (!a2) throw Error(`Unrecognized array type.`);
    let [o2] = new Uint16Array(t2, 2, 1), [s2] = new Uint32Array(t2, 4, 1);
    return new e44(s2, o2, a2, void 0, t2);
  }
  constructor(e49, t2 = 64, n2 = Float64Array, r2 = ArrayBuffer, i2) {
    if (isNaN(e49) || e49 < 0) throw Error(`Unexpected numItems value: ${e49}.`);
    this.numItems = +e49, this.nodeSize = Math.min(Math.max(+t2, 2), 65535), this.ArrayType = n2, this.IndexArrayType = e49 < 65536 ? Uint16Array : Uint32Array;
    let a2 = Rf.indexOf(this.ArrayType), o2 = e49 * 2 * this.ArrayType.BYTES_PER_ELEMENT, s2 = e49 * this.IndexArrayType.BYTES_PER_ELEMENT, c2 = (8 - s2 % 8) % 8;
    if (a2 < 0) throw Error(`Unexpected typed array class: ${n2}.`);
    if (i2) this.data = i2, this.ids = new this.IndexArrayType(i2, 8, e49), this.coords = new n2(i2, 8 + s2 + c2, e49 * 2), this._pos = e49 * 2, this._finished = true;
    else {
      let i3 = this.data = new r2(8 + o2 + s2 + c2);
      this.ids = new this.IndexArrayType(i3, 8, e49), this.coords = new n2(i3, 8 + s2 + c2, e49 * 2), this._pos = 0, this._finished = false, new Uint8Array(i3, 0, 2).set([219, 16 + a2]), new Uint16Array(i3, 2, 1)[0] = t2, new Uint32Array(i3, 4, 1)[0] = e49;
    }
  }
  add(e49, t2) {
    let n2 = this._pos >> 1;
    return this.ids[n2] = n2, this.coords[this._pos++] = e49, this.coords[this._pos++] = t2, n2;
  }
  finish() {
    let e49 = this._pos >> 1;
    if (e49 !== this.numItems) throw Error(`Added ${e49} items when expected ${this.numItems}.`);
    return Vf(this.ids, this.coords, this.nodeSize, 0, this.numItems - 1, 0), this._finished = true, this;
  }
  range(e49, t2, n2, r2) {
    if (!this._finished) throw Error(`Data not yet indexed - call index.finish().`);
    let { ids: i2, coords: a2, nodeSize: o2 } = this;
    zf[0] = 0, zf[1] = i2.length - 1, zf[2] = 0;
    let s2 = 3, c2 = [];
    for (; s2 > 0; ) {
      let l2 = zf[--s2], u2 = zf[--s2], d2 = zf[--s2];
      if (u2 - d2 <= o2) {
        for (let o3 = d2; o3 <= u2; o3++) {
          let s3 = a2[2 * o3], l3 = a2[2 * o3 + 1];
          s3 >= e49 && s3 <= n2 && l3 >= t2 && l3 <= r2 && c2.push(i2[o3]);
        }
        continue;
      }
      let f2 = d2 + u2 >> 1, p2 = a2[2 * f2], m2 = a2[2 * f2 + 1];
      p2 >= e49 && p2 <= n2 && m2 >= t2 && m2 <= r2 && c2.push(i2[f2]), (l2 === 0 ? e49 <= p2 : t2 <= m2) && (zf[s2++] = d2, zf[s2++] = f2 - 1, zf[s2++] = 1 - l2), (l2 === 0 ? n2 >= p2 : r2 >= m2) && (zf[s2++] = f2 + 1, zf[s2++] = u2, zf[s2++] = 1 - l2);
    }
    return c2;
  }
  within(e49, t2, n2) {
    let r2 = [];
    return this.withinInto(e49, t2, n2, r2), r2;
  }
  withinInto(e49, t2, n2, r2) {
    if (!this._finished) throw Error(`Data not yet indexed - call index.finish().`);
    let { ids: i2, coords: a2, nodeSize: o2 } = this;
    zf[0] = 0, zf[1] = i2.length - 1, zf[2] = 0;
    let s2 = 3, c2 = 0, l2 = n2 * n2;
    for (; s2 > 0; ) {
      let u2 = zf[--s2], d2 = zf[--s2], f2 = zf[--s2];
      if (d2 - f2 <= o2) {
        for (let n3 = f2; n3 <= d2; n3++) Gf(a2[2 * n3], a2[2 * n3 + 1], e49, t2) <= l2 && (r2[c2++] = i2[n3]);
        continue;
      }
      let p2 = f2 + d2 >> 1, m2 = a2[2 * p2], h2 = a2[2 * p2 + 1];
      Gf(m2, h2, e49, t2) <= l2 && (r2[c2++] = i2[p2]), (u2 === 0 ? e49 - n2 <= m2 : t2 - n2 <= h2) && (zf[s2++] = f2, zf[s2++] = p2 - 1, zf[s2++] = 1 - u2), (u2 === 0 ? e49 + n2 >= m2 : t2 + n2 >= h2) && (zf[s2++] = p2 + 1, zf[s2++] = d2, zf[s2++] = 1 - u2);
    }
    return c2;
  }
};
function Vf(e49, t2, n2, r2, i2, a2) {
  if (i2 - r2 <= n2) return;
  let o2 = r2 + i2 >> 1;
  Hf(e49, t2, o2, r2, i2, a2), Vf(e49, t2, n2, r2, o2 - 1, 1 - a2), Vf(e49, t2, n2, o2 + 1, i2, 1 - a2);
}
function Hf(e49, t2, n2, r2, i2, a2) {
  for (; i2 > r2; ) {
    if (i2 - r2 > 600) {
      let o3 = i2 - r2 + 1, s3 = n2 - r2 + 1, c3 = Math.log(o3), l2 = 0.5 * Math.exp(2 * c3 / 3), u2 = 0.5 * Math.sqrt(c3 * l2 * (o3 - l2) / o3) * (s3 - o3 / 2 < 0 ? -1 : 1);
      Hf(e49, t2, n2, Math.max(r2, Math.floor(n2 - s3 * l2 / o3 + u2)), Math.min(i2, Math.floor(n2 + (o3 - s3) * l2 / o3 + u2)), a2);
    }
    let o2 = t2[2 * n2 + a2], s2 = r2, c2 = i2;
    for (Uf(e49, t2, r2, n2), t2[2 * i2 + a2] > o2 && Uf(e49, t2, r2, i2); s2 < c2; ) {
      for (Uf(e49, t2, s2, c2), s2++, c2--; t2[2 * s2 + a2] < o2; ) s2++;
      for (; t2[2 * c2 + a2] > o2; ) c2--;
    }
    t2[2 * r2 + a2] === o2 ? Uf(e49, t2, r2, c2) : (c2++, Uf(e49, t2, c2, i2)), c2 <= n2 && (r2 = c2 + 1), n2 <= c2 && (i2 = c2 - 1);
  }
}
function Uf(e49, t2, n2, r2) {
  Wf(e49, n2, r2), Wf(t2, 2 * n2, 2 * r2), Wf(t2, 2 * n2 + 1, 2 * r2 + 1);
}
function Wf(e49, t2, n2) {
  let r2 = e49[t2];
  e49[t2] = e49[n2], e49[n2] = r2;
}
function Gf(e49, t2, n2, r2) {
  let i2 = e49 - n2, a2 = t2 - r2;
  return i2 * i2 + a2 * a2;
}
const Kf = { minZoom: 0, maxZoom: 16, minPoints: 2, radius: 40, extent: 512, nodeSize: 64, log: false, generateId: false, reduce: null, map: (e49) => e49 };
var qf = class {
  constructor(e49) {
    this.options = Object.assign(Object.create(Kf), e49), this.trees = Array(this.options.maxZoom + 1), this.stride = this.options.reduce ? 7 : 6, this.clusterProps = [], this.points = [];
  }
  load(e49) {
    let t2 = [];
    for (let n2 of e49) {
      if (!n2.geometry) continue;
      let [e50, r2] = n2.geometry.coordinates, [i2, a2] = [cf(e50), lf(r2)], o2 = { id: n2.id, type: `Point`, geometry: [i2, a2], tags: n2.properties };
      t2.push(o2);
    }
    this.createIndex(t2);
  }
  initialize(e49) {
    let t2 = [];
    for (let n2 of e49) n2.type === `Point` && t2.push(n2);
    this.createIndex(t2);
  }
  updateIndex(e49, t2, n2) {
    this.options = Object.assign(Object.create(Kf), n2.clusterOptions), this.initialize(e49);
  }
  createIndex(e49) {
    let { log: t2, minZoom: n2, maxZoom: r2 } = this.options;
    t2 && console.time(`total time`);
    let i2 = `prepare ${e49.length} points`;
    t2 && console.time(i2), this.points = e49;
    let a2 = [];
    for (let t3 = 0; t3 < e49.length; t3++) {
      let n3 = e49[t3];
      if (!(n3 == null ? void 0 : n3.geometry)) continue;
      let [r3, i3] = n3.geometry;
      r3 = Math.fround(r3), i3 = Math.fround(i3), a2.push(r3, i3, 1 / 0, t3, -1, 1), this.options.reduce && a2.push(0);
    }
    let o2 = this.trees[r2 + 1] = this.createTree(a2);
    t2 && console.timeEnd(i2);
    for (let e50 = r2; e50 >= n2; e50--) {
      let n3 = Date.now();
      o2 = this.trees[e50] = this.createTree(this.cluster(o2, e50)), t2 && console.log(`z%d: %d clusters in %dms`, e50, o2.numItems, Date.now() - n3);
    }
    t2 && console.timeEnd(`total time`);
  }
  getClusters(e49, t2) {
    return this.getClustersInternal(e49, t2).map((e50) => df(e50));
  }
  getClustersInternal(e49, t2) {
    let n2 = ((e49[0] + 180) % 360 + 360) % 360 - 180, r2 = Math.max(-90, Math.min(90, e49[1])), i2 = e49[2] === 180 ? 180 : ((e49[2] + 180) % 360 + 360) % 360 - 180, a2 = Math.max(-90, Math.min(90, e49[3]));
    if (e49[2] - e49[0] >= 360) n2 = -180, i2 = 180;
    else if (n2 > i2) {
      let e50 = this.getClustersInternal([n2, r2, 180, a2], t2), o3 = this.getClustersInternal([-180, r2, i2, a2], t2);
      return e50.concat(o3);
    }
    let o2 = this.trees[this.limitZoom(t2)], s2 = o2.range(cf(n2), lf(a2), cf(i2), lf(r2)), c2 = o2.flatData, l2 = [];
    for (let e50 of s2) {
      let t3 = this.stride * e50;
      l2.push(c2[t3 + 5] > 1 ? Jf(c2, t3, this.clusterProps) : this.points[c2[t3 + 3]]);
    }
    return l2;
  }
  getChildren(e49) {
    let t2 = this.getOriginId(e49), n2 = this.getOriginZoom(e49), r2 = /* @__PURE__ */ Error(`No cluster with the specified id: ` + e49), i2 = this.trees[n2];
    if (!i2) throw r2;
    let a2 = i2.flatData;
    if (t2 * this.stride >= a2.length) throw r2;
    let o2 = this.options.radius / (this.options.extent * 2 ** (n2 - 1)), s2 = a2[t2 * this.stride], c2 = a2[t2 * this.stride + 1], l2 = i2.within(s2, c2, o2), u2 = [];
    for (let t3 of l2) {
      let n3 = t3 * this.stride;
      a2[n3 + 4] === e49 && u2.push(a2[n3 + 5] > 1 ? Yf(a2, n3, this.clusterProps) : df(this.points[a2[n3 + 3]]));
    }
    if (u2.length === 0) throw r2;
    return u2;
  }
  getLeaves(e49, t2, n2) {
    t2 ||= 10, n2 ||= 0;
    let r2 = [];
    return this.appendLeaves(r2, e49, t2, n2, 0), r2;
  }
  getTile(e49, t2, n2) {
    let r2 = this.trees[this.limitZoom(e49)];
    if (!r2) return null;
    let i2 = 2 ** e49, { extent: a2, radius: o2 } = this.options, s2 = o2 / a2, c2 = (n2 - s2) / i2, l2 = (n2 + 1 + s2) / i2, u2 = { transformed: true, features: [], source: null, x: t2, y: n2, z: e49 };
    return this.addTileFeatures(r2.range((t2 - s2) / i2, c2, (t2 + 1 + s2) / i2, l2), r2.flatData, t2, n2, i2, u2), t2 === 0 && this.addTileFeatures(r2.range(1 - s2 / i2, c2, 1, l2), r2.flatData, i2, n2, i2, u2), t2 === i2 - 1 && this.addTileFeatures(r2.range(0, c2, s2 / i2, l2), r2.flatData, -1, n2, i2, u2), u2;
  }
  getClusterExpansionZoom(e49) {
    return this.getOriginZoom(e49);
  }
  appendLeaves(e49, t2, n2, r2, i2) {
    let a2 = this.getChildren(t2);
    for (let t3 of a2) {
      let a3 = t3.properties;
      if ((a3 == null ? void 0 : a3.cluster) ? i2 + a3.point_count <= r2 ? i2 += a3.point_count : i2 = this.appendLeaves(e49, a3.cluster_id, n2, r2, i2) : i2 < r2 ? i2++ : e49.push(t3), e49.length === n2) break;
    }
    return i2;
  }
  createTree(e49) {
    let t2 = new Bf(e49.length / this.stride | 0, this.options.nodeSize, Float32Array);
    for (let n2 = 0; n2 < e49.length; n2 += this.stride) t2.add(e49[n2], e49[n2 + 1]);
    return t2.finish(), t2.flatData = e49, t2.data = null, t2;
  }
  addTileFeatures(e49, t2, n2, r2, i2, a2) {
    for (let o2 of e49) {
      let e50 = o2 * this.stride, s2 = t2[e50 + 5] > 1, c2, l2, u2;
      if (s2) c2 = Xf(t2, e50, this.clusterProps), l2 = t2[e50], u2 = t2[e50 + 1];
      else {
        let n3 = this.points[t2[e50 + 3]];
        c2 = n3.tags, [l2, u2] = n3.geometry;
      }
      let d2 = { type: 1, geometry: [[Math.round(this.options.extent * (l2 * i2 - n2)), Math.round(this.options.extent * (u2 * i2 - r2))]], tags: c2 }, f2;
      f2 = s2 || this.options.generateId ? t2[e50 + 3] : this.points[t2[e50 + 3]].id, f2 !== void 0 && (d2.id = f2), a2.features.push(d2);
    }
  }
  limitZoom(e49) {
    return Math.max(this.options.minZoom, Math.min(Math.floor(+e49), this.options.maxZoom + 1));
  }
  cluster(e49, t2) {
    let { radius: n2, extent: r2, reduce: i2, minPoints: a2 } = this.options, o2 = n2 / (r2 * 2 ** t2), s2 = e49.flatData, c2 = [], l2 = this.stride;
    for (let n3 = 0; n3 < s2.length; n3 += l2) {
      if (s2[n3 + 2] <= t2) continue;
      s2[n3 + 2] = t2;
      let r3 = s2[n3], u2 = s2[n3 + 1], d2 = e49.within(s2[n3], s2[n3 + 1], o2), f2 = s2[n3 + 5], p2 = f2;
      for (let e50 of d2) {
        let n4 = e50 * l2;
        s2[n4 + 2] > t2 && (p2 += s2[n4 + 5]);
      }
      if (p2 > f2 && p2 >= a2) {
        let e50 = r3 * f2, a3 = u2 * f2, o3, m2 = -1, h2 = ((n3 / l2 | 0) << 5) + (t2 + 1) + this.points.length;
        for (let r4 of d2) {
          let c3 = r4 * l2;
          if (s2[c3 + 2] <= t2) continue;
          s2[c3 + 2] = t2;
          let u3 = s2[c3 + 5];
          e50 += s2[c3] * u3, a3 += s2[c3 + 1] * u3, s2[c3 + 4] = h2, i2 && (o3 || (o3 = this.map(s2, n3, true), m2 = this.clusterProps.length, this.clusterProps.push(o3)), i2(o3, this.map(s2, c3)));
        }
        s2[n3 + 4] = h2, c2.push(e50 / p2, a3 / p2, 1 / 0, h2, -1, p2), i2 && c2.push(m2);
      } else {
        for (let e50 = 0; e50 < l2; e50++) c2.push(s2[n3 + e50]);
        if (p2 > 1) for (let e50 of d2) {
          let n4 = e50 * l2;
          if (!(s2[n4 + 2] <= t2)) {
            s2[n4 + 2] = t2;
            for (let e51 = 0; e51 < l2; e51++) c2.push(s2[n4 + e51]);
          }
        }
      }
    }
    return c2;
  }
  getOriginId(e49) {
    return e49 - this.points.length >> 5;
  }
  getOriginZoom(e49) {
    return (e49 - this.points.length) % 32;
  }
  map(e49, t2, n2) {
    if (e49[t2 + 5] > 1) {
      let r3 = this.clusterProps[e49[t2 + 6]];
      return n2 ? Object.assign({}, r3) : r3;
    }
    let r2 = this.points[e49[t2 + 3]].tags, i2 = this.options.map(r2);
    return n2 && i2 === r2 ? Object.assign({}, i2) : i2;
  }
};
function Jf(e49, t2, n2) {
  return { id: e49[t2 + 3], type: `Point`, tags: Xf(e49, t2, n2), geometry: [e49[t2], e49[t2 + 1]] };
}
function Yf(e49, t2, n2) {
  return { type: `Feature`, id: e49[t2 + 3], properties: Xf(e49, t2, n2), geometry: { type: `Point`, coordinates: [hf(e49[t2]), gf(e49[t2 + 1])] } };
}
function Xf(e49, t2, n2) {
  let r2 = e49[t2 + 5], i2 = r2 >= 1e4 ? `${Math.round(r2 / 1e3)}k` : r2 >= 1e3 ? `${Math.round(r2 / 100) / 10}k` : r2, a2 = e49[t2 + 6], o2 = a2 === -1 ? {} : Object.assign({}, n2[a2]);
  return Object.assign(o2, { cluster: true, cluster_id: e49[t2 + 3], point_count: r2, point_count_abbreviated: i2 });
}
const Zf = `geojsonvt_clip_start`, Qf = `geojsonvt_clip_end`;
function $f(e49, t2, n2, r2, i2) {
  let a2 = t2 === i2.maxZoom ? 0 : i2.tolerance / ((1 << t2) * i2.extent), o2 = { transformed: false, features: [], source: null, x: n2, y: r2, z: t2, minX: 2, minY: 1, maxX: -1, maxY: 0, numPoints: 0, numSimplified: 0, numFeatures: e49.length };
  for (let t3 of e49) ep(o2, t3, a2, i2);
  return o2;
}
function ep(e49, t2, n2, r2) {
  switch (e49.minX = Math.min(e49.minX, t2.minX), e49.minY = Math.min(e49.minY, t2.minY), e49.maxX = Math.max(e49.maxX, t2.maxX), e49.maxY = Math.max(e49.maxY, t2.maxY), t2.type) {
    case `Point`:
    case `MultiPoint`:
      tp(e49, t2);
      return;
    case `LineString`:
      np(e49, t2, n2, r2);
      return;
    case `MultiLineString`:
    case `Polygon`:
      rp(e49, t2, n2);
      return;
    case `MultiPolygon`:
      ip(e49, t2, n2);
      return;
  }
}
function tp(e49, t2) {
  let n2 = [];
  for (let r3 = 0; r3 < t2.geometry.length; r3 += 3) n2.push(t2.geometry[r3], t2.geometry[r3 + 1]), e49.numPoints++, e49.numSimplified++;
  if (!n2.length) return;
  let r2 = { type: 1, tags: t2.tags || null, geometry: n2 };
  t2.id !== null && (r2.id = t2.id), e49.features.push(r2);
}
function np(e49, t2, n2, r2) {
  let i2 = [];
  if (ap(i2, t2.geometry, e49, n2, false, false), !i2.length) return;
  let a2 = t2.tags || null;
  if (r2.lineMetrics) {
    a2 = {};
    for (let e50 in t2.tags) a2[e50] = t2.tags[e50];
    a2[Zf] = t2.geometry.start / t2.geometry.size, a2[Qf] = t2.geometry.end / t2.geometry.size;
  }
  let o2 = { type: 2, tags: a2, geometry: i2 };
  t2.id !== null && (o2.id = t2.id), e49.features.push(o2);
}
function rp(e49, t2, n2) {
  let r2 = [];
  for (let i3 = 0; i3 < t2.geometry.length; i3++) ap(r2, t2.geometry[i3], e49, n2, t2.type === `Polygon`, i3 === 0);
  if (!r2.length) return;
  let i2 = { type: t2.type === `Polygon` ? 3 : 2, tags: t2.tags || null, geometry: r2 };
  t2.id !== null && (i2.id = t2.id), e49.features.push(i2);
}
function ip(e49, t2, n2) {
  let r2 = [];
  for (let i3 = 0; i3 < t2.geometry.length; i3++) {
    let a2 = t2.geometry[i3];
    for (let t3 = 0; t3 < a2.length; t3++) ap(r2, a2[t3], e49, n2, true, t3 === 0);
  }
  if (!r2.length) return;
  let i2 = { type: 3, tags: t2.tags || null, geometry: r2 };
  t2.id !== null && (i2.id = t2.id), e49.features.push(i2);
}
function ap(e49, t2, n2, r2, i2, a2) {
  let o2 = r2 * r2;
  if (r2 > 0 && t2.size < (i2 ? o2 : r2)) {
    n2.numPoints += t2.points.length / 3;
    return;
  }
  let s2 = [];
  for (let e50 = 0; e50 < t2.points.length; e50 += 3) (r2 === 0 || t2.points[e50 + 2] > o2) && (n2.numSimplified++, s2.push(t2.points[e50], t2.points[e50 + 1])), n2.numPoints++;
  i2 && op(s2, a2), e49.push(s2);
}
function op(e49, t2) {
  let n2 = 0;
  for (let t3 = 0, r2 = e49.length, i2 = r2 - 2; t3 < r2; i2 = t3, t3 += 2) n2 += (e49[t3] - e49[i2]) * (e49[t3 + 1] + e49[i2 + 1]);
  if (n2 > 0 === t2) for (let t3 = 0, n3 = e49.length; t3 < n3 / 2; t3 += 2) {
    let r2 = e49[t3], i2 = e49[t3 + 1];
    e49[t3] = e49[n3 - 2 - t3], e49[t3 + 1] = e49[n3 - 1 - t3], e49[n3 - 2 - t3] = r2, e49[n3 - 1 - t3] = i2;
  }
}
function sp(e49, t2) {
  if (e49.transformed) return e49;
  let n2 = 1 << e49.z, r2 = e49.x, i2 = e49.y;
  for (let a2 of e49.features) a2.type === 1 ? cp(a2, t2, n2, r2, i2) : lp(a2, t2, n2, r2, i2);
  return e49.transformed = true, e49;
}
function cp(e49, t2, n2, r2, i2) {
  let a2 = e49, o2 = e49.geometry, s2 = [];
  for (let e50 = 0; e50 < o2.length; e50 += 2) s2.push(up(o2[e50], o2[e50 + 1], t2, n2, r2, i2));
  return a2.geometry = s2, a2;
}
function lp(e49, t2, n2, r2, i2) {
  let a2 = e49, o2 = e49.geometry, s2 = [];
  for (let e50 of o2) {
    let a3 = [];
    for (let o3 = 0; o3 < e50.length; o3 += 2) a3.push(up(e50[o3], e50[o3 + 1], t2, n2, r2, i2));
    s2.push(a3);
  }
  return a2.geometry = s2, a2;
}
function up(e49, t2, n2, r2, i2, a2) {
  return [Math.round(n2 * (e49 * r2 - i2)), Math.round(n2 * (t2 * r2 - a2))];
}
var dp = class {
  constructor(e49) {
    this.options = e49, this.total = 0, this.stats = {}, this.tiles = {}, this.tileCoords = [], this.stats = {}, this.total = 0;
  }
  initialize(e49) {
    this.splitTile(e49, 0, 0, 0), this.options.debug && (e49.length && console.log(`features: %d, points: %d`, this.tiles[0].numFeatures, this.tiles[0].numPoints), console.timeEnd(`generate tiles`), console.log(`tiles generated:`, this.total, JSON.stringify(this.stats)));
  }
  updateIndex(e49, t2, n2) {
    n2.debug > 1 && (console.log(`invalidating tiles`), console.time(`invalidating`)), this.invalidateTiles(t2), n2.debug > 1 && console.timeEnd(`invalidating`);
    let [r2, i2, a2] = [0, 0, 0], o2 = $f(e49, r2, i2, a2, n2);
    o2.source = e49;
    let s2 = fp(r2, i2, a2);
    if (this.tiles[s2] = o2, this.tileCoords.push({ z: r2, x: i2, y: a2, id: s2 }), n2.debug) {
      let e50 = `z${r2}`;
      this.stats[e50] = (this.stats[e50] || 0) + 1, this.total++;
    }
  }
  getClusterExpansionZoom(e49) {
    return null;
  }
  getChildren(e49) {
    return null;
  }
  getLeaves(e49, t2, n2) {
    return null;
  }
  getTile(e49, t2, n2) {
    let { extent: r2, debug: i2 } = this.options, a2 = 1 << e49;
    t2 = t2 + a2 & a2 - 1;
    let o2 = fp(e49, t2, n2);
    if (this.tiles[o2]) return sp(this.tiles[o2], r2);
    i2 > 1 && console.log(`drilling down to z%d-%d-%d`, e49, t2, n2);
    let s2 = e49, c2 = t2, l2 = n2, u2;
    for (; !u2 && s2 > 0; ) s2--, c2 >>= 1, l2 >>= 1, u2 = this.tiles[fp(s2, c2, l2)];
    return !(u2 == null ? void 0 : u2.source) || (i2 > 1 && (console.log(`found parent tile z%d-%d-%d`, s2, c2, l2), console.time(`drilling down`)), this.splitTile(u2.source, s2, c2, l2, e49, t2, n2), i2 > 1 && console.timeEnd(`drilling down`), !this.tiles[o2]) ? null : sp(this.tiles[o2], r2);
  }
  splitTile(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = [e49, t2, n2, r2], c2 = this.options, l2 = c2.debug;
    for (; s2.length; ) {
      r2 = s2.pop(), n2 = s2.pop(), t2 = s2.pop(), e49 = s2.pop();
      let u2 = 1 << t2, d2 = fp(t2, n2, r2), f2 = this.tiles[d2];
      if (!f2 && (l2 > 1 && console.time(`creation`), f2 = this.tiles[d2] = $f(e49, t2, n2, r2, c2), this.tileCoords.push({ z: t2, x: n2, y: r2, id: d2 }), l2)) {
        l2 > 1 && (console.log(`tile z%d-%d-%d (features: %d, points: %d, simplified: %d)`, t2, n2, r2, f2.numFeatures, f2.numPoints, f2.numSimplified), console.timeEnd(`creation`));
        let e50 = `z${t2}`;
        this.stats[e50] = (this.stats[e50] || 0) + 1, this.total++;
      }
      if (f2.source = e49, i2 == null) {
        if (t2 === c2.indexMaxZoom || f2.numPoints <= c2.indexMaxPoints) continue;
      } else if (t2 === c2.maxZoom || t2 === i2) continue;
      else if (i2 != null) {
        let e50 = i2 - t2;
        if (n2 !== a2 >> e50 || r2 !== o2 >> e50) continue;
      }
      if (f2.source = null, !e49.length) continue;
      l2 > 1 && console.time(`clipping`);
      let p2 = 0.5 * c2.buffer / c2.extent, m2 = 0.5 - p2, h2 = 0.5 + p2, g2 = 1 + p2, _2 = null, v2 = null, y2 = null, b2 = null, x2 = _f(e49, u2, n2 - p2, n2 + h2, 0, f2.minX, f2.maxX, c2), S2 = _f(e49, u2, n2 + m2, n2 + g2, 0, f2.minX, f2.maxX, c2);
      x2 && (_2 = _f(x2, u2, r2 - p2, r2 + h2, 1, f2.minY, f2.maxY, c2), v2 = _f(x2, u2, r2 + m2, r2 + g2, 1, f2.minY, f2.maxY, c2)), S2 && (y2 = _f(S2, u2, r2 - p2, r2 + h2, 1, f2.minY, f2.maxY, c2), b2 = _f(S2, u2, r2 + m2, r2 + g2, 1, f2.minY, f2.maxY, c2)), l2 > 1 && console.timeEnd(`clipping`), s2.push(_2 || [], t2 + 1, n2 * 2, r2 * 2), s2.push(v2 || [], t2 + 1, n2 * 2, r2 * 2 + 1), s2.push(y2 || [], t2 + 1, n2 * 2 + 1, r2 * 2), s2.push(b2 || [], t2 + 1, n2 * 2 + 1, r2 * 2 + 1);
    }
  }
  invalidateTiles(e49) {
    if (!e49.length) return;
    let t2 = this.options, { debug: n2 } = t2, r2 = 1 / 0, i2 = -1 / 0, a2 = 1 / 0, o2 = -1 / 0;
    for (let t3 of e49) r2 = Math.min(r2, t3.minX), i2 = Math.max(i2, t3.maxX), a2 = Math.min(a2, t3.minY), o2 = Math.max(o2, t3.maxY);
    let s2 = t2.buffer / t2.extent, c2 = /* @__PURE__ */ new Set();
    for (let t3 in this.tiles) {
      let l2 = this.tiles[t3], u2 = 1 << l2.z, d2 = (l2.x - s2) / u2, f2 = (l2.x + 1 + s2) / u2, p2 = (l2.y - s2) / u2, m2 = (l2.y + 1 + s2) / u2;
      if (i2 < d2 || r2 >= f2 || o2 < p2 || a2 >= m2) continue;
      let h2 = false;
      for (let t4 of e49) if (t4.maxX >= d2 && t4.minX < f2 && t4.maxY >= p2 && t4.minY < m2) {
        h2 = true;
        break;
      }
      if (h2) {
        if (n2) {
          n2 > 1 && console.log(`invalidate tile z%d-%d-%d (features: %d, points: %d, simplified: %d)`, l2.z, l2.x, l2.y, l2.numFeatures, l2.numPoints, l2.numSimplified);
          let e50 = `z${l2.z}`;
          this.stats[e50] = (this.stats[e50] || 0) - 1, this.total--;
        }
        delete this.tiles[t3], c2.add(t3);
      }
    }
    c2.size && (this.tileCoords = this.tileCoords.filter((e50) => !c2.has(e50.id)));
  }
};
function fp(e49, t2, n2) {
  return ((1 << e49) * n2 + t2) * 32 + e49;
}
const pp = { maxZoom: 14, indexMaxZoom: 5, indexMaxPoints: 1e5, tolerance: 3, extent: 4096, buffer: 64, lineMetrics: false, promoteId: null, generateId: false, updateable: false, cluster: false, clusterOptions: Kf, debug: 0 };
var mp = class {
  constructor(e49, t2) {
    t2 = this.options = Object.assign({}, pp, t2);
    let n2 = t2.debug;
    if (n2 && console.time(`preprocess data`), t2.maxZoom < 0 || t2.maxZoom > 24) throw Error(`maxZoom should be in the 0-24 range`);
    if (t2.promoteId && t2.generateId) throw Error(`promoteId and generateId cannot be used together.`);
    let r2 = Yd(e49, t2);
    n2 && (console.timeEnd(`preprocess data`), console.log(`index: maxZoom: %d, maxPoints: %d`, t2.indexMaxZoom, t2.indexMaxPoints), console.time(`generate tiles`)), r2 = Af(r2, t2), t2.updateable && (this.source = r2), this.initializeIndex(r2, t2);
  }
  initializeIndex(e49, t2) {
    this.tileIndex = t2.cluster ? new qf(t2.clusterOptions) : new dp(t2), e49.length && this.tileIndex.initialize(e49);
  }
  getTile(e49, t2, n2) {
    return e49 = +e49, t2 = +t2, n2 = +n2, e49 < 0 || e49 > 24 ? null : this.tileIndex.getTile(e49, t2, n2);
  }
  updateData(e49, t2) {
    let n2 = this.options;
    if (!n2.updateable) throw Error("to update tile geojson `updateable` option must be set to true");
    let { affected: r2, source: i2 } = Pf(this.source, e49, n2);
    t2 && ({ affected: r2, source: i2 } = this.filterUpdate(i2, r2, t2)), r2.length && (this.source = i2, this.tileIndex.updateIndex(i2, r2, n2));
  }
  filterUpdate(e49, t2, n2) {
    let r2 = /* @__PURE__ */ new Set();
    for (let i2 of e49) i2.id != null && (n2(df(i2)) || (t2.push(i2), r2.add(i2.id)));
    return e49 = e49.filter((e50) => !r2.has(e50.id)), { affected: t2, source: e49 };
  }
  getData() {
    if (!this.options.updateable) throw Error("to retrieve data the `updateable` option must be set to true");
    return uf(this.source);
  }
  updateClusterOptions(e49, t2) {
    let n2 = this.options.cluster;
    if (this.options.cluster = e49, this.options.clusterOptions = t2, n2 == e49) {
      this.tileIndex.updateIndex(this.source, [], this.options);
      return;
    }
    this.initializeIndex(this.source, this.options);
  }
  getClusterExpansionZoom(e49) {
    return this.tileIndex.getClusterExpansionZoom(e49);
  }
  getClusterChildren(e49) {
    return this.tileIndex.getChildren(e49);
  }
  getClusterLeaves(e49, t2, n2) {
    return this.tileIndex.getLeaves(e49, t2, n2);
  }
};
const hp = Ho([{ name: `a_pos_normal`, components: 2, type: `Int16` }, { name: `a_data`, components: 4, type: `Uint8` }, { name: `a_offset_normal`, components: 2, type: `Int8` }], 2), gp = hp.members;
hp.size, hp.alignment;
const _p = Ho([{ name: `a_uv_x`, components: 1, type: `Float32` }, { name: `a_split_index`, components: 1, type: `Float32` }]), vp = _p.members;
_p.size, _p.alignment;
const yp = Math.cos(75 / 2 * (Math.PI / 180)), bp = 1 / 2, xp = 2 ** 14 / bp;
var Sp = class {
  constructor(e49) {
    this.zoom = e49.zoom, this.overscaling = e49.overscaling, this.layers = e49.layers, this.layerIds = this.layers.map((e50) => e50.id), this.index = e49.index, this.hasDependencies = false, this.patternFeatures = [], this.lineClipsArray = [], this.gradients = {};
    for (let e50 of this.layers) this.gradients[e50.id] = {};
    this.layoutVertexArray = new js(), this.layoutVertexArray2 = new Ms(), this.indexArray = new Bs(), this.programConfigurations = new mc(e49.layers, e49.zoom), this.segments = new Ws(), this.maxLineLength = 0, this.stateDependentLayerIds = this.layers.filter((e50) => e50.isStateDependent()).map((e50) => e50.id);
  }
  populate(e49, t2, n2) {
    this.hasDependencies = Ol(`line`, this.layers, t2) || this.hasLineDasharray(this.layers);
    let r2 = this.layers[0].layout.get(`line-sort-key`), i2 = !r2.isConstant(), a2 = [], o2 = new K(this.zoom), s2 = this.layers[0]._featureFilter.needGeometry;
    for (let { feature: t3, id: c2, index: l2, sourceLayerIndex: u2 } of e49) {
      let e50 = xc(t3, s2);
      if (!this.layers[0]._featureFilter.filter(o2, e50, n2)) continue;
      let d2 = i2 ? r2.evaluate(e50, {}, n2) : void 0, f2 = { id: c2, properties: t3.properties, type: t3.type, sourceLayerIndex: u2, index: l2, geometry: s2 ? e50.geometry : bc(t3), patterns: {}, dashes: {}, sortKey: d2 };
      a2.push(f2);
    }
    i2 && a2.sort((e50, t3) => e50.sortKey - t3.sortKey);
    for (let r3 of a2) {
      let { geometry: i3, index: a3, sourceLayerIndex: o3 } = r3;
      this.hasDependencies ? (Ol(`line`, this.layers, t2) ? kl(`line`, this.layers, r3, { zoom: this.zoom }, t2) : this.hasLineDasharray(this.layers) && this.addLineDashDependencies(this.layers, r3, this.zoom, t2), this.patternFeatures.push(r3)) : this.addFeature(r3, i3, a3, n2, {}, {}, t2.subdivisionGranularity);
      let s3 = e49[a3].feature;
      t2.featureIndex.insert(s3, i3, a3, o3, this.index);
    }
  }
  update(e49, t2, n2, r2) {
    this.stateDependentLayers.length && this.programConfigurations.updatePaintArrays(e49, t2, this.stateDependentLayers, { imagePositions: n2, dashPositions: r2 });
  }
  addFeatures({ options: e49, canonical: t2, patternPositions: n2, dashPositions: r2 }) {
    for (let i2 of this.patternFeatures) this.addFeature(i2, i2.geometry, i2.index, t2, n2, r2, e49.subdivisionGranularity);
  }
  isEmpty() {
    return this.layoutVertexArray.length === 0;
  }
  uploadPending() {
    return !this.uploaded || this.programConfigurations.needsUpload;
  }
  upload(e49) {
    this.uploaded || (this.layoutVertexArray2.length !== 0 && (this.layoutVertexBuffer2 = e49.createVertexBuffer(this.layoutVertexArray2, vp)), this.layoutVertexBuffer = e49.createVertexBuffer(this.layoutVertexArray, gp), this.indexBuffer = e49.createIndexBuffer(this.indexArray)), this.programConfigurations.upload(e49), this.uploaded = true;
  }
  destroy() {
    this.layoutVertexBuffer && (this.layoutVertexBuffer.destroy(), this.indexBuffer.destroy(), this.programConfigurations.destroy(), this.segments.destroy());
  }
  lineFeatureClips(e49) {
    if (e49.properties && Object.hasOwn(e49.properties, `geojsonvt_clip_start`) && Object.hasOwn(e49.properties, `geojsonvt_clip_end`)) return { start: +e49.properties[Zf], end: +e49.properties[Qf] };
  }
  addFeature(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = this.layers[0].layout, c2 = s2.get(`line-join`).evaluate(e49, {}), l2 = s2.get(`line-cap`).evaluate(e49, {}), u2 = s2.get(`line-miter-limit`).evaluate(e49, {}), d2 = s2.get(`line-round-limit`).evaluate(e49, {});
    this.lineClips = this.lineFeatureClips(e49);
    for (let n3 of t2) this.addLine(n3, e49, c2, l2, u2, d2, r2, o2);
    this.programConfigurations.populatePaintArrays(this.layoutVertexArray.length, e49, n2, { imagePositions: i2, dashPositions: a2, canonical: r2 });
  }
  addLine(e49, t2, n2, r2, i2, a2, o2, s2) {
    this.distance = 0, this.scaledDistance = 0, this.totalDistance = 0;
    let c2 = o2 ? s2.line.getGranularityForZoomLevel(o2.z) : 1;
    if (e49 = ju(e49, c2), this.lineClips) {
      this.lineClipsArray.push(this.lineClips);
      for (let t3 = 0; t3 < e49.length - 1; t3++) this.totalDistance += e49[t3].dist(e49[t3 + 1]);
      this.updateScaledDistance(), this.maxLineLength = Math.max(this.maxLineLength, this.totalDistance);
    }
    let l2 = ed.types[t2.type] === `Polygon`, u2 = e49.length;
    for (; u2 >= 2 && e49[u2 - 1].equals(e49[u2 - 2]); ) u2--;
    let d2 = 0;
    for (; d2 < u2 - 1 && e49[d2].equals(e49[d2 + 1]); ) d2++;
    if (u2 - d2 < (l2 ? 3 : 2)) return;
    n2 === `bevel` && (i2 = 1.05);
    let f2 = this.overscaling <= 16 ? 15 * v / (512 * this.overscaling) : 0, p2 = this.segments.prepareSegment(u2 * 10, this.layoutVertexArray, this.indexArray), m2, h2, g2, _2, y2;
    this.e1 = this.e2 = -1, l2 && (m2 = e49[u2 - 2], y2 = e49[d2].sub(m2)._unit()._perp());
    for (let t3 = d2; t3 < u2; t3++) {
      if (g2 = t3 === u2 - 1 ? l2 ? e49[d2 + 1] : void 0 : e49[t3 + 1], g2 && e49[t3].equals(g2)) continue;
      y2 && (_2 = y2), m2 && (h2 = m2), m2 = e49[t3], y2 = g2 ? g2.sub(m2)._unit()._perp() : _2, _2 ||= y2;
      let o3 = _2.add(y2);
      (o3.x !== 0 || o3.y !== 0) && o3._unit();
      let s3 = _2.x * y2.x + _2.y * y2.y, c3 = o3.x * y2.x + o3.y * y2.y, v2 = c3 === 0 ? 1 / 0 : 1 / c3, b2 = 2 * Math.sqrt(2 - 2 * c3), x2 = c3 < yp && h2 && g2, C2 = _2.x * y2.y - _2.y * y2.x > 0, w2 = o3.mult(Math.min(v2, 2)), T2 = S((v2 - 3) / 5, 0, 1);
      if (x2 && t3 > d2) {
        let e50 = m2.dist(h2);
        if (e50 > 2 * f2) {
          let t4 = m2.sub(m2.sub(h2)._mult(f2 / e50)._round());
          this.updateDistance(h2, t4), this.addCurrentVertex(t4, _2, 0, 0, p2), h2 = t4;
        }
      }
      let E2 = h2 && g2, D2 = E2 ? n2 : l2 ? `butt` : r2;
      if (E2 && D2 === `round` && (v2 < a2 ? D2 = `miter` : v2 <= 2 && (D2 = `fakeround`)), D2 === `miter` && v2 > i2 && (D2 = `bevel`), D2 === `bevel` && (v2 > 2 && (D2 = `flipbevel`), v2 < i2 && (D2 = `miter`)), h2 && this.updateDistance(h2, m2), D2 === `miter`) o3._mult(v2), this.addCurrentVertex(m2, o3, 0, 0, p2, false, w2);
      else if (D2 === `flipbevel`) {
        if (v2 > 100) o3 = y2.mult(-1);
        else {
          let e50 = v2 * _2.add(y2).mag() / _2.sub(y2).mag();
          o3._perp()._mult(e50 * (C2 ? -1 : 1));
        }
        this.addCurrentVertex(m2, o3, 0, 0, p2, false, w2, T2), this.addCurrentVertex(m2, o3.mult(-1), 0, 0, p2, false, w2, T2);
      } else if (D2 === `bevel` || D2 === `fakeround`) {
        let e50 = -Math.sqrt(v2 * v2 - 1), t4 = C2 ? e50 : 0, n3 = C2 ? 0 : e50;
        if (h2 && this.addCurrentVertex(m2, _2, t4, n3, p2, false, w2), D2 === `fakeround`) {
          let e51 = Math.round(b2 * 180 / Math.PI / 20);
          for (let t5 = 1; t5 < e51; t5++) {
            let n4 = t5 / e51;
            if (n4 !== 0.5) {
              let e52 = n4 - 0.5, t6 = 1.0904 + s3 * (-3.2452 + s3 * (3.55645 - s3 * 1.43519)), r4 = 0.848013 + s3 * (-1.06021 + s3 * 0.215638);
              n4 += n4 * e52 * (n4 - 1) * (t6 * e52 * e52 + r4);
            }
            let r3 = y2.sub(_2)._mult(n4)._add(_2)._unit()._mult(C2 ? -1 : 1);
            this.addHalfVertex(m2, r3.x, r3.y, C2 ? -w2.x : w2.x, C2 ? -w2.y : w2.y, false, C2, p2);
          }
        }
        g2 && this.addCurrentVertex(m2, y2, -t4, -n3, p2, false, w2);
      } else if (D2 === `butt`) this.addCurrentVertex(m2, o3, 0, 0, p2, false, w2);
      else if (D2 === `square`) {
        let e50 = h2 ? 1 : -1;
        this.addCurrentVertex(m2, o3, e50, e50, p2, false, w2);
      } else D2 === `round` && (h2 && (this.addCurrentVertex(m2, _2, 0, 0, p2, false, w2, T2), this.addCurrentVertex(m2, _2, 1, 1, p2, true, w2, T2)), g2 && (this.addCurrentVertex(m2, y2, -1, -1, p2, true, w2, T2), this.addCurrentVertex(m2, y2, 0, 0, p2, false, w2, T2)));
      if (x2 && t3 < u2 - 1) {
        let e50 = m2.dist(g2);
        if (e50 > 2 * f2) {
          let t4 = m2.add(g2.sub(m2)._mult(f2 / e50)._round());
          this.updateDistance(m2, t4), this.addCurrentVertex(t4, y2, 0, 0, p2), m2 = t4;
        }
      }
    }
  }
  addCurrentVertex(e49, t2, n2, r2, i2, a2 = false, o2 = t2, s2 = 0) {
    let c2 = t2.x + t2.y * n2, l2 = t2.y - t2.x * n2, u2 = -t2.x + t2.y * r2, d2 = -t2.y - t2.x * r2, f2 = o2.x + (t2.x - o2.x) * s2, p2 = o2.y + (t2.y - o2.y) * s2;
    this.addHalfVertex(e49, c2, l2, f2, p2, a2, false, i2), this.addHalfVertex(e49, u2, d2, -f2, -p2, a2, true, i2), this.distance > xp / 2 && this.totalDistance === 0 && (this.distance = 0, this.updateScaledDistance(), this.addCurrentVertex(e49, t2, n2, r2, i2, a2, o2, s2));
  }
  addHalfVertex({ x: e49, y: t2 }, n2, r2, i2, a2, o2, s2, c2) {
    let l2 = (this.lineClips ? this.scaledDistance * (xp - 1) : this.scaledDistance) * bp;
    if (this.layoutVertexArray.emplaceBack((e49 << 1) + +!!o2, (t2 << 1) + +!!s2, Math.round(63 * n2) + 128, Math.round(63 * r2) + 128, (l2 & 63) << 2, l2 >> 6, Math.round(63 * i2), Math.round(63 * a2)), this.lineClips) {
      let e50 = (this.scaledDistance - this.lineClips.start) / (this.lineClips.end - this.lineClips.start);
      this.layoutVertexArray2.emplaceBack(e50, this.lineClipsArray.length);
    }
    let u2 = c2.vertexLength++;
    this.e1 >= 0 && this.e2 >= 0 && (this.indexArray.emplaceBack(this.e1, u2, this.e2), c2.primitiveLength++), s2 ? this.e2 = u2 : this.e1 = u2;
  }
  updateScaledDistance() {
    this.scaledDistance = this.lineClips ? this.lineClips.start + (this.lineClips.end - this.lineClips.start) * this.distance / this.totalDistance : this.distance;
  }
  updateDistance(e49, t2) {
    this.distance += e49.dist(t2), this.updateScaledDistance();
  }
  hasLineDasharray(e49) {
    for (let t2 of e49) {
      let e50 = t2.paint.get(`line-dasharray`);
      if (e50 && !e50.isConstant()) return true;
    }
    return false;
  }
  addLineDashDependencies(e49, t2, n2, r2) {
    for (let i2 of e49) {
      let e50 = i2.paint.get(`line-dasharray`);
      if (!e50 || e50.value.kind === `constant`) continue;
      let a2 = i2.layout.get(`line-cap`).evaluate(t2, {}) === `round`, o2 = { dasharray: e50.value.evaluate({ zoom: n2 - 1 }, t2, {}), round: a2 }, s2 = { dasharray: e50.value.evaluate({ zoom: n2 }, t2, {}), round: a2 }, c2 = { dasharray: e50.value.evaluate({ zoom: n2 + 1 }, t2, {}), round: a2 }, l2 = `${o2.dasharray.join(`,`)},${o2.round}`, u2 = `${s2.dasharray.join(`,`)},${s2.round}`, d2 = `${c2.dasharray.join(`,`)},${c2.round}`;
      r2.dashDependencies[l2] = o2, r2.dashDependencies[u2] = s2, r2.dashDependencies[d2] = c2, t2.dashes[i2.id] = { min: l2, mid: u2, max: d2 };
    }
  }
};
G(`LineBucket`, Sp, { omit: [`layers`, `patternFeatures`] });
let Cp;
const wp = () => Cp ||= new Fo({ "line-cap": new J(M.layout_line[`line-cap`], `line-cap`), "line-join": new J(M.layout_line[`line-join`], `line-join`), "line-miter-limit": new J(M.layout_line[`line-miter-limit`], `line-miter-limit`), "line-round-limit": new J(M.layout_line[`line-round-limit`], `line-round-limit`), "line-sort-key": new J(M.layout_line[`line-sort-key`], `line-sort-key`) });
let Tp;
const Ep = () => Tp ||= new Fo({ "line-opacity": new J(M.paint_line[`line-opacity`], `line-opacity`), "line-layer-opacity": new q(M.paint_line[`line-layer-opacity`], `line-layer-opacity`), "line-color": new J(M.paint_line[`line-color`], `line-color`), "line-translate": new q(M.paint_line[`line-translate`], `line-translate`), "line-translate-anchor": new q(M.paint_line[`line-translate-anchor`], `line-translate-anchor`), "line-width": new J(M.paint_line[`line-width`], `line-width`), "line-gap-width": new J(M.paint_line[`line-gap-width`], `line-gap-width`), "line-offset": new J(M.paint_line[`line-offset`], `line-offset`), "line-blur": new J(M.paint_line[`line-blur`], `line-blur`), "line-dasharray": new Mo(M.paint_line[`line-dasharray`], `line-dasharray`), "line-pattern": new Mo(M.paint_line[`line-pattern`], `line-pattern`), "line-gradient": new Po(M.paint_line[`line-gradient`], `line-gradient`) });
var Dp = { get paint() {
  return Ep();
}, get layout() {
  return wp();
} }, Op = class extends J {
  possiblyEvaluate(e49, t2) {
    return t2 = new K(Math.floor(t2.zoom), { now: t2.now, fadeDuration: t2.fadeDuration, zoomHistory: t2.zoomHistory, transition: t2.transition }), super.possiblyEvaluate(e49, t2);
  }
  evaluate(e49, t2, n2, r2) {
    return t2 = w({}, t2, { zoom: Math.floor(t2.zoom) }), super.evaluate(e49, t2, n2, r2);
  }
};
let kp;
var Ap = class extends Ro {
  constructor(e49, t2) {
    super(e49, Dp, t2), this.gradientVersion = 0, kp || (kp = new Op(Dp.paint.properties[`line-width`].specification, `line-floorwidth`), kp.useIntegerZoom = true);
  }
  _handleSpecialPaintPropertyUpdate(e49) {
    if (e49 === `line-gradient`) {
      let e50 = this.gradientExpression();
      this.stepInterpolant = fi(e50) ? e50._styleExpression.expression instanceof Et : false, this.gradientVersion = (this.gradientVersion + 1) % (2 ** 53 - 1);
    }
  }
  gradientExpression() {
    return this._transitionablePaint._values[`line-gradient`].value.expression;
  }
  recalculate(e49, t2) {
    super.recalculate(e49, t2), this.paint._values[`line-floorwidth`] = kp.possiblyEvaluate(this._transitioningPaint._values[`line-width`].value, e49);
  }
  createBucket(e49) {
    return new Sp(e49);
  }
  queryRadius(e49) {
    let t2 = e49, n2 = jp(Rc(`line-width`, this, t2), Rc(`line-gap-width`, this, t2)), r2 = Rc(`line-offset`, this, t2);
    return n2 / 2 + Math.abs(r2) + zc(this.paint.get(`line-translate`));
  }
  queryIntersectsFeature({ queryGeometry: e49, feature: t2, featureState: n2, geometry: r2, transform: i2, pixelsToTileUnits: a2 }) {
    let o2 = Bc(e49, this.paint.get(`line-translate`), this.paint.get(`line-translate-anchor`), -i2.bearingInRadians, a2), s2 = a2 / 2 * jp(this.paint.get(`line-width`).evaluate(t2, n2), this.paint.get(`line-gap-width`).evaluate(t2, n2)), c2 = this.paint.get(`line-offset`).evaluate(t2, n2);
    return c2 && (r2 = Hc(r2, c2 * a2)), Oc(o2, r2, s2);
  }
  isTileClipped() {
    return true;
  }
};
function jp(e49, t2) {
  return t2 > 0 ? t2 + 2 * e49 : e49;
}
const Mp = Ho([{ name: `a_pos_offset`, components: 4, type: `Int16` }, { name: `a_data`, components: 4, type: `Uint16` }, { name: `a_pixeloffset`, components: 4, type: `Int16` }, { name: `a_height_offset`, components: 1, type: `Float32` }], 4), Np = Ho([{ name: `a_projected_pos`, components: 3, type: `Float32` }], 4);
Ho([{ name: `a_fade_opacity`, components: 1, type: `Uint32` }], 4);
const Pp = Ho([{ name: `a_placed`, components: 2, type: `Uint8` }, { name: `a_shift`, components: 2, type: `Float32` }, { name: `a_box_real`, components: 2, type: `Int16` }]);
Ho([{ type: `Int16`, name: `anchorPointX` }, { type: `Int16`, name: `anchorPointY` }, { type: `Int16`, name: `x1` }, { type: `Int16`, name: `y1` }, { type: `Int16`, name: `x2` }, { type: `Int16`, name: `y2` }, { type: `Uint32`, name: `featureIndex` }, { type: `Uint16`, name: `sourceLayerIndex` }, { type: `Uint16`, name: `bucketIndex` }]);
const Fp = Ho([{ name: `a_pos`, components: 2, type: `Int16` }, { name: `a_anchor_pos`, components: 2, type: `Int16` }, { name: `a_extrude`, components: 2, type: `Int16` }], 4);
Ho([{ name: `a_pos`, components: 2, type: `Float32` }, { name: `a_radius`, components: 1, type: `Float32` }, { name: `a_flags`, components: 2, type: `Int16` }], 4), Ho([{ name: `triangle`, components: 3, type: `Uint16` }]), Ho([{ type: `Int16`, name: `anchorX` }, { type: `Int16`, name: `anchorY` }, { type: `Uint16`, name: `glyphStartIndex` }, { type: `Uint16`, name: `numGlyphs` }, { type: `Uint32`, name: `vertexStartIndex` }, { type: `Uint32`, name: `lineStartIndex` }, { type: `Uint32`, name: `lineLength` }, { type: `Uint16`, name: `segment` }, { type: `Uint16`, name: `lowerSize` }, { type: `Uint16`, name: `upperSize` }, { type: `Float32`, name: `lineOffsetX` }, { type: `Float32`, name: `lineOffsetY` }, { type: `Uint8`, name: `writingMode` }, { type: `Uint8`, name: `placedOrientation` }, { type: `Uint8`, name: `hidden` }, { type: `Uint32`, name: `crossTileID` }, { type: `Int16`, name: `associatedIconIndex` }, { type: `Float32`, name: `heightOffset` }]), Ho([{ type: `Int16`, name: `anchorX` }, { type: `Int16`, name: `anchorY` }, { type: `Int16`, name: `rightJustifiedTextSymbolIndex` }, { type: `Int16`, name: `centerJustifiedTextSymbolIndex` }, { type: `Int16`, name: `leftJustifiedTextSymbolIndex` }, { type: `Int16`, name: `verticalPlacedTextSymbolIndex` }, { type: `Int16`, name: `placedIconSymbolIndex` }, { type: `Int16`, name: `verticalPlacedIconSymbolIndex` }, { type: `Uint16`, name: `key` }, { type: `Uint16`, name: `textBoxStartIndex` }, { type: `Uint16`, name: `textBoxEndIndex` }, { type: `Uint16`, name: `verticalTextBoxStartIndex` }, { type: `Uint16`, name: `verticalTextBoxEndIndex` }, { type: `Uint16`, name: `iconBoxStartIndex` }, { type: `Uint16`, name: `iconBoxEndIndex` }, { type: `Uint16`, name: `verticalIconBoxStartIndex` }, { type: `Uint16`, name: `verticalIconBoxEndIndex` }, { type: `Uint16`, name: `featureIndex` }, { type: `Uint16`, name: `numHorizontalGlyphVertices` }, { type: `Uint16`, name: `numVerticalGlyphVertices` }, { type: `Uint16`, name: `numIconVertices` }, { type: `Uint16`, name: `numVerticalIconVertices` }, { type: `Uint16`, name: `useRuntimeCollisionCircles` }, { type: `Uint32`, name: `crossTileID` }, { type: `Float32`, name: `textBoxScale` }, { type: `Float32`, name: `collisionCircleDiameter` }, { type: `Uint16`, name: `textAnchorOffsetStartIndex` }, { type: `Uint16`, name: `textAnchorOffsetEndIndex` }, { type: `Float32`, name: `heightOffset` }]), Ho([{ type: `Float32`, name: `offsetX` }]), Ho([{ type: `Int16`, name: `x` }, { type: `Int16`, name: `y` }, { type: `Int16`, name: `tileUnitDistanceFromAnchor` }]), Ho([{ type: `Uint16`, name: `textAnchor` }, { type: `Float32`, components: 2, name: `textOffset` }]);
const Ip = new class {
  constructor() {
    this.TIMEOUT = 5e3, this.applyArabicShaping = null, this.processBidirectionalText = null, this.processStyledBidirectionalText = null, this.pluginStatus = `unavailable`, this.pluginURL = null, this.loadScriptResolve = () => {
    };
  }
  setState(e49) {
    this.pluginStatus = e49.pluginStatus, this.pluginURL = e49.pluginURL;
  }
  getState() {
    return { pluginStatus: this.pluginStatus, pluginURL: this.pluginURL };
  }
  setMethods(e49) {
    if (Ip.isParsed()) throw Error(`RTL text plugin already registered.`);
    this.applyArabicShaping = e49.applyArabicShaping, this.processBidirectionalText = e49.processBidirectionalText, this.processStyledBidirectionalText = e49.processStyledBidirectionalText, this.loadScriptResolve();
  }
  isParsed() {
    return this.applyArabicShaping != null && this.processBidirectionalText != null && this.processStyledBidirectionalText != null;
  }
  getRTLTextPluginStatus() {
    return this.pluginStatus;
  }
  async syncState(e49, t2) {
    if (this.isParsed()) return this.getState();
    if (e49.pluginStatus !== `loading`) return this.setState(e49), e49;
    let n2 = e49.pluginURL, r2 = new Promise((e50) => {
      this.loadScriptResolve = e50;
    }), i2 = new Promise((e50) => setTimeout(() => e50(), this.TIMEOUT));
    if (await t2(n2), await Promise.race([r2, i2]), this.isParsed()) {
      let e50 = { pluginStatus: `loaded`, pluginURL: n2 };
      return this.setState(e50), e50;
    }
    throw this.setState({ pluginStatus: `error`, pluginURL: `` }), Error(`RTL Text Plugin failed to import scripts from ${n2}`);
  }
}();
function Lp(e49) {
  return /[\u0600-\u0604\u0606-\u060B\u060D-\u061A\u061C-\u061E\u0620-\u063F\u0641-\u064A\u0656-\u066F\u0671-\u06DC\u06DE-\u070D\u070F-\u074A\u074D-\u077F\u07C0-\u07FA\u07FD-\u07FF\u0840-\u085B\u085E\u0860-\u086A\u0870-\u0891\u0897-\u08E1\u08E3-\u08FF\u1800\u1801\u1804\u1806-\u1819\u1820-\u1878\u1880-\u18AA\uA840-\uA877\uFB50-\uFD3D\uFD40-\uFDCF\uFDF0-\uFDFF\uFE70-\uFE74\uFE76-\uFEFC]|\uD802[\uDEC0-\uDEE6\uDEEB-\uDEF6\uDF80-\uDF91\uDF99-\uDF9C\uDFA9-\uDFAF]|\uD803[\uDD00-\uDD27\uDD30-\uDD39\uDE60-\uDE7E\uDEC2-\uDEC7\uDED0-\uDED8\uDEFA-\uDEFF\uDF30-\uDF59\uDF70-\uDF89\uDFB0-\uDFCB]|\uD805[\uDE60-\uDE6C]|\uD82F[\uDC00-\uDC6A\uDC70-\uDC7C\uDC80-\uDC88\uDC90-\uDC99\uDC9C-\uDC9F]|\uD83A[\uDD00-\uDD4B\uDD50-\uDD59\uDD5E\uDD5F]|\uD83B[\uDE00-\uDE03\uDE05-\uDE1F\uDE21\uDE22\uDE24\uDE27\uDE29-\uDE32\uDE34-\uDE37\uDE39\uDE3B\uDE42\uDE47\uDE49\uDE4B\uDE4D-\uDE4F\uDE51\uDE52\uDE54\uDE57\uDE59\uDE5B\uDE5D\uDE5F\uDE61\uDE62\uDE64\uDE67-\uDE6A\uDE6C-\uDE72\uDE74-\uDE77\uDE79-\uDE7C\uDE7E\uDE80-\uDE89\uDE8B-\uDE9B\uDEA1-\uDEA3\uDEA5-\uDEA9\uDEAB-\uDEBB\uDEF0\uDEF1]/gim.test(String.fromCodePoint(e49));
}
function Rp(e49) {
  return /[\u0591-\u05C7\u05D0-\u05EA\u05EF-\u05F4\u0600-\u0604\u0606-\u060B\u060D-\u061A\u061C-\u061E\u0620-\u063F\u0641-\u064A\u0656-\u066F\u0671-\u06DC\u06DE-\u070D\u070F-\u074A\u074D-\u07B1\u07C0-\u07FA\u07FD-\u082D\u0830-\u083E\u0840-\u085B\u085E\u0860-\u086A\u0870-\u0891\u0897-\u08E1\u08E3-\u08FF\uFB1D-\uFB36\uFB38-\uFB3C\uFB3E\uFB40\uFB41\uFB43\uFB44\uFB46-\uFD3D\uFD40-\uFDCF\uFDF0-\uFDFF\uFE70-\uFE74\uFE76-\uFEFC]|\uD802[\uDC00-\uDC05\uDC08\uDC0A-\uDC35\uDC37\uDC38\uDC3C\uDC3F-\uDC55\uDC57-\uDC9E\uDCA7-\uDCAF\uDCE0-\uDCF2\uDCF4\uDCF5\uDCFB-\uDD1B\uDD1F-\uDD39\uDD3F-\uDD59\uDD80-\uDDB7\uDDBC-\uDDCF\uDDD2-\uDE03\uDE05\uDE06\uDE0C-\uDE13\uDE15-\uDE17\uDE19-\uDE35\uDE38-\uDE3A\uDE3F-\uDE48\uDE50-\uDE58\uDE60-\uDE9F\uDEC0-\uDEE6\uDEEB-\uDEF6\uDF00-\uDF35\uDF39-\uDF55\uDF58-\uDF72\uDF78-\uDF91\uDF99-\uDF9C\uDFA9-\uDFAF]|\uD803[\uDC00-\uDC48\uDC80-\uDCB2\uDCC0-\uDCF2\uDCFA-\uDD27\uDD30-\uDD39\uDD40-\uDD65\uDD69-\uDD85\uDD8E\uDD8F\uDE60-\uDE7E\uDE80-\uDEA9\uDEAB-\uDEAD\uDEB0\uDEB1\uDEC2-\uDEC7\uDED0-\uDED8\uDEFA-\uDF27\uDF30-\uDF59\uDF70-\uDF89\uDFB0-\uDFCB\uDFE0-\uDFF6]|\uD83A[\uDC00-\uDCC4\uDCC7-\uDCD6\uDD00-\uDD4B\uDD50-\uDD59\uDD5E\uDD5F]|\uD83B[\uDE00-\uDE03\uDE05-\uDE1F\uDE21\uDE22\uDE24\uDE27\uDE29-\uDE32\uDE34-\uDE37\uDE39\uDE3B\uDE42\uDE47\uDE49\uDE4B\uDE4D-\uDE4F\uDE51\uDE52\uDE54\uDE57\uDE59\uDE5B\uDE5D\uDE5F\uDE61\uDE62\uDE64\uDE67-\uDE6A\uDE6C-\uDE72\uDE74-\uDE77\uDE79-\uDE7C\uDE7E\uDE80-\uDE89\uDE8B-\uDE9B\uDEA1-\uDEA3\uDEA5-\uDEA9\uDEAB-\uDEBB\uDEF0\uDEF1]/gim.test(String.fromCodePoint(e49));
}
function zp(e49) {
  return /[\u02EA\u02EB\u2E80-\u2FDF\u2FF0-\u303F\u3041-\u3096\u309D-\u309F\u30A1-\u30FA\u30FD-\u30FF\u3105-\u312F\u31A0-\u4DBF\u4E00-\uA48C\uA490-\uA4C6\uF900-\uFA6D\uFA70-\uFAD9\uFE10-\uFE1F\uFE30-\uFE4F\uFF00-\uFFEF]|\uD81B[\uDFE0-\uDFFF]|[\uD81C-\uD822\uD840-\uD868\uD86A-\uD86D\uD86F-\uD872\uD874-\uD879\uD880-\uD883\uD885-\uD88C][\uDC00-\uDFFF]|\uD823[\uDC00-\uDCD5\uDCFF-\uDD1E\uDD80-\uDDF2]|\uD82B[\uDFF0-\uDFFF]|\uD82C[\uDC00-\uDEFB]|\uD83C[\uDE00-\uDEFF]|\uD869[\uDC00-\uDEDF\uDF00-\uDFFF]|\uD86E[\uDC00-\uDC1D\uDC20-\uDFFF]|\uD873[\uDC00-\uDEAD\uDEB0-\uDFFF]|\uD87A[\uDC00-\uDFE0\uDFF0-\uDFFF]|\uD87B[\uDC00-\uDE5D]|\uD87E[\uDC00-\uDE1D]|\uD884[\uDC00-\uDF4A\uDF50-\uDFFF]|\uD88D[\uDC00-\uDC79]/gim.test(String.fromCodePoint(e49));
}
function Bp(e49) {
  return /[\u02EA\u02EB\u1100-\u11FF\u1401-\u167F\u18B0-\u18FF\u2065\u20DD-\u20E0\u20E2-\u20E4\u2B97\u2BF0-\u2BFF\u2E50\u2E51\u2E80-\u3007\u3012\u3013\u3020-\u302F\u3031-\u30FB\u30FD-\uA4CF\uA960-\uA97F\uAC00-\uD7FF\uF900-\uFAFF\uFE10-\uFE1F\uFE30-\uFE48\uFE50-\uFE57\uFE5F-\uFE62\uFE67-\uFE6F\uFF00-\uFF07\uFF0A-\uFF0C\uFF0E-\uFF19\uFF1F-\uFF3A\uFF3C\uFF3E\uFF40-\uFF5A\uFFE0-\uFFE2\uFFE4-\uFFE7\uFFF0-\uFFF8]|\uD802[\uDD80-\uDD9F]|\uD805[\uDD80-\uDDFF]|\uD806[\uDE00-\uDEBF]|[\uD80C-\uD810\uD81C-\uD822\uD83C\uD83D\uD840-\uD87E\uD880-\uD8BE][\uDC00-\uDFFF]|\uD811[\uDC00-\uDE7F]|\uD81B[\uDFE0-\uDFFF]|\uD823[\uDC00-\uDDFF]|\uD82B[\uDFF0-\uDFFF]|\uD82C[\uDC00-\uDEFF]|\uD833[\uDEC0-\uDFCF]|\uD834[\uDC00-\uDDFF\uDEE0-\uDF7F]|\uD836[\uDC00-\uDEAF]|\uD83E[\uDD00-\uDEFF]|[\uD87F\uD8BF][\uDC00-\uDFFD]/gim.test(String.fromCodePoint(e49));
}
function Vp(e49) {
  return /[\xA7\xA9\xAE\xB1\xBC-\xBE\xD7\xF7\u2016\u2020\u2021\u2030\u2031\u203B\u203C\u2042\u2047-\u2049\u2051\u2100-\u218F\u221E\u2234\u2235\u2300-\u2307\u230C-\u231F\u2324-\u2328\u232B\u237D-\u239A\u23BE-\u23CD\u23CF\u23D1-\u23DB\u23E2-\u2422\u2424-\u24FF\u25A0-\u2619\u2620-\u2767\u2776-\u2793\u2B12-\u2B2F\u2B50-\u2B59\u2BB8-\u2BEB\u3000-\u303F\u30A0-\u30FF\uE000-\uF8FF\uFE30-\uFE6F\uFF00-\uFFEF\uFFFC\uFFFD]|[\uDB80-\uDBFF][\uDC00-\uDFFF]/gim.test(String.fromCodePoint(e49));
}
function Hp(e49) {
  return /[\r\u0300-\u036F\u0483-\u0489\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7\u0600-\u0605\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06DD\u06DF-\u06E4\u06E7\u06E8\u06EA-\u06ED\u070F\u0711\u0730-\u074A\u07A6-\u07B0\u07EB-\u07F3\u07FD\u0816-\u0819\u081B-\u0823\u0825-\u0827\u0829-\u082D\u0859-\u085B\u0890\u0891\u0897-\u089F\u08CA-\u0903\u093A-\u093C\u093E-\u094F\u0951-\u0957\u0962\u0963\u0981-\u0983\u09BC\u09BE-\u09C4\u09C7\u09C8\u09CB-\u09CD\u09D7\u09E2\u09E3\u09FE\u0A01-\u0A03\u0A3C\u0A3E-\u0A42\u0A47\u0A48\u0A4B-\u0A4D\u0A51\u0A70\u0A71\u0A75\u0A81-\u0A83\u0ABC\u0ABE-\u0AC5\u0AC7-\u0AC9\u0ACB-\u0ACD\u0AE2\u0AE3\u0AFA-\u0AFF\u0B01-\u0B03\u0B3C\u0B3E-\u0B44\u0B47\u0B48\u0B4B-\u0B4D\u0B55-\u0B57\u0B62\u0B63\u0B82\u0BBE-\u0BC2\u0BC6-\u0BC8\u0BCA-\u0BCD\u0BD7\u0C00-\u0C04\u0C3C\u0C3E-\u0C44\u0C46-\u0C48\u0C4A-\u0C4D\u0C55\u0C56\u0C62\u0C63\u0C81-\u0C83\u0CBC\u0CBE-\u0CC4\u0CC6-\u0CC8\u0CCA-\u0CCD\u0CD5\u0CD6\u0CE2\u0CE3\u0CF3\u0D00-\u0D03\u0D3B\u0D3C\u0D3E-\u0D44\u0D46-\u0D48\u0D4A-\u0D4E\u0D57\u0D62\u0D63\u0D81-\u0D83\u0DCA\u0DCF-\u0DD4\u0DD6\u0DD8-\u0DDF\u0DF2\u0DF3\u0E31\u0E33-\u0E3A\u0E47-\u0E4E\u0EB1\u0EB3-\u0EBC\u0EC8-\u0ECE\u0F18\u0F19\u0F35\u0F37\u0F39\u0F3E\u0F3F\u0F71-\u0F84\u0F86\u0F87\u0F8D-\u0F97\u0F99-\u0FBC\u0FC6\u102D-\u1037\u1039-\u103E\u1056-\u1059\u105E-\u1060\u1071-\u1074\u1082\u1084-\u1086\u108D\u109D\u1100-\u11FF\u135D-\u135F\u1712-\u1715\u1732-\u1734\u1752\u1753\u1772\u1773\u17B4-\u17D3\u17DD\u180B-\u180D\u180F\u1885\u1886\u18A9\u1920-\u192B\u1930-\u193B\u1A17-\u1A1B\u1A55-\u1A5E\u1A60\u1A62\u1A65-\u1A7C\u1A7F\u1AB0-\u1ADD\u1AE0-\u1AEB\u1B00-\u1B04\u1B34-\u1B44\u1B6B-\u1B73\u1B80-\u1B82\u1BA1-\u1BAD\u1BE6-\u1BF3\u1C24-\u1C37\u1CD0-\u1CD2\u1CD4-\u1CE8\u1CED\u1CF4\u1CF7-\u1CF9\u1DC0-\u1DFF\u200C\u200D\u20D0-\u20F0\u2CEF-\u2CF1\u2D7F\u2DE0-\u2DFF\u302A-\u302F\u3099\u309A\uA66F-\uA672\uA674-\uA67D\uA69E\uA69F\uA6F0\uA6F1\uA802\uA806\uA80B\uA823-\uA827\uA82C\uA880\uA881\uA8B4-\uA8C5\uA8E0-\uA8F1\uA8FF\uA926-\uA92D\uA947-\uA953\uA960-\uA97C\uA980-\uA983\uA9B3-\uA9C0\uA9E5\uAA29-\uAA36\uAA43\uAA4C\uAA4D\uAA7C\uAAB0\uAAB2-\uAAB4\uAAB7\uAAB8\uAABE\uAABF\uAAC1\uAAEB-\uAAEF\uAAF5\uAAF6\uABE3-\uABEA\uABEC\uABED\uAC00-\uD7A3\uD7B0-\uD7C6\uD7CB-\uD7FB\uFB1E\uFE00-\uFE0F\uFE20-\uFE2F\uFF9E\uFF9F]|\uD800[\uDDFD\uDEE0\uDF76-\uDF7A]|\uD802[\uDE01-\uDE03\uDE05\uDE06\uDE0C-\uDE0F\uDE38-\uDE3A\uDE3F\uDEE5\uDEE6]|\uD803[\uDD24-\uDD27\uDD69-\uDD6D\uDEAB\uDEAC\uDEFA-\uDEFF\uDF46-\uDF50\uDF82-\uDF85]|\uD804[\uDC00-\uDC02\uDC38-\uDC46\uDC70\uDC73\uDC74\uDC7F-\uDC82\uDCB0-\uDCBA\uDCBD\uDCC2\uDCCD\uDD00-\uDD02\uDD27-\uDD34\uDD45\uDD46\uDD73\uDD80-\uDD82\uDDB3-\uDDC0\uDDC2\uDDC3\uDDC9-\uDDCC\uDDCE\uDDCF\uDE2C-\uDE37\uDE3E\uDE41\uDEDF-\uDEEA\uDF00-\uDF03\uDF3B\uDF3C\uDF3E-\uDF44\uDF47\uDF48\uDF4B-\uDF4D\uDF57\uDF62\uDF63\uDF66-\uDF6C\uDF70-\uDF74\uDFB8-\uDFC0\uDFC2\uDFC5\uDFC7-\uDFCA\uDFCC-\uDFD2\uDFE1\uDFE2]|\uD805[\uDC35-\uDC46\uDC5E\uDCB0-\uDCC3\uDDAF-\uDDB5\uDDB8-\uDDC0\uDDDC\uDDDD\uDE30-\uDE40\uDEAB-\uDEB7\uDF1D-\uDF1F\uDF22-\uDF2B]|\uD806[\uDC2C-\uDC3A\uDD30-\uDD35\uDD37\uDD38\uDD3B-\uDD43\uDDD1-\uDDD7\uDDDA-\uDDE0\uDDE4\uDE01-\uDE0A\uDE33-\uDE39\uDE3B-\uDE3E\uDE47\uDE51-\uDE5B\uDE84-\uDE99\uDF60-\uDF67]|\uD807[\uDC2F-\uDC36\uDC38-\uDC3F\uDC92-\uDCA7\uDCA9-\uDCB6\uDD31-\uDD36\uDD3A\uDD3C\uDD3D\uDD3F-\uDD47\uDD8A-\uDD8E\uDD90\uDD91\uDD93-\uDD97\uDEF3-\uDEF6\uDF00-\uDF03\uDF34-\uDF3A\uDF3E-\uDF42\uDF5A]|\uD80D[\uDC40\uDC47-\uDC55]|\uD818[\uDD1E-\uDD2F]|\uD81A[\uDEF0-\uDEF4\uDF30-\uDF36]|\uD81B[\uDD63\uDD67-\uDD6A\uDF4F\uDF51-\uDF87\uDF8F-\uDF92\uDFE4\uDFF0\uDFF1]|\uD82F[\uDC9D\uDC9E]|\uD833[\uDF00-\uDF2D\uDF30-\uDF46]|\uD834[\uDD65-\uDD69\uDD6D-\uDD72\uDD7B-\uDD82\uDD85-\uDD8B\uDDAA-\uDDAD\uDE42-\uDE44]|\uD836[\uDE00-\uDE36\uDE3B-\uDE6C\uDE75\uDE84\uDE9B-\uDE9F\uDEA1-\uDEAF]|\uD838[\uDC00-\uDC06\uDC08-\uDC18\uDC1B-\uDC21\uDC23\uDC24\uDC26-\uDC2A\uDC8F\uDD30-\uDD36\uDEAE\uDEEC-\uDEEF]|\uD839[\uDCEC-\uDCEF\uDDEE\uDDEF\uDEE3\uDEE6\uDEEE\uDEEF\uDEF5]|\uD83A[\uDCD0-\uDCD6\uDD44-\uDD4A]|\uD83C[\uDDE6-\uDDFF\uDFFB-\uDFFF]|\uDB40[\uDC20-\uDC7F\uDD00-\uDDEF]/.test(e49);
}
function Up(e49) {
  return /[\u0E01-\u0E3A\u0E40-\u0E4E\u0E81\u0E82\u0E84\u0E86-\u0E8A\u0E8C-\u0EA3\u0EA5\u0EA7-\u0EBD\u0EC0-\u0EC4\u0EC6\u0EC8-\u0ECE\u0EDC-\u0EDF\u0F00-\u0F47\u0F49-\u0F6C\u0F71-\u0F97\u0F99-\u0FBC\u0FBE-\u0FCC\u0FCE-\u0FD4\u0FD9\u0FDA\u1000-\u103F\u1050-\u108F\u109A-\u109F\u1780-\u17D3\u17D7\u17DC\u17DD\u1950-\u196D\u1970-\u1974\u1980-\u19AB\u19B0-\u19C9\u19DE\u19DF\u1A20-\u1A5E\u1A60-\u1A7C\u1AA0-\u1AAD\u1B00-\u1B4C\u1B4E-\u1B7F\uA980-\uA9CD\uA9D0-\uA9D9\uA9DE-\uA9EF\uA9FA-\uA9FE\uAA60-\uAAC2\uAADB-\uAADF]|\uD805[\uDF00-\uDF1A\uDF1D-\uDF2B\uDF3A\uDF3B\uDF3F-\uDF46]/gim.test(String.fromCodePoint(e49));
}
function Wp(e49, t2) {
  return /(?:[\u1039\u17D2\u1A60\u1BAB\u200D\uAAF6]|\uD802\uDE3F|\uD804[\uDD33\uDFD0]|\uD806[\uDD3E\uDE47\uDE99]|\uD807[\uDD45\uDD97\uDF42])$/.test(e49) || new RegExp("^\\p{gc=Mc}", "u").test(t2);
}
const Gp = { C: `18g,1;g2,3;4nb,1`, D: `17k,1;5,1;1,1;1,5;4,d;1,7;1,2;z,2;8,g;i,12;1,2;9,1;1,1;1,2;14,3;2,1;28,9;3,f;2,4;1,1;2,3;2,6;7a,1;2,5;1,1;g,a;5,2;2,6;1,f`, R: `17m,4;1,1;1,1;5,4;l,1;14,3;1,3;g,i;12,1;2,9;1,1;1,1;2,2;1,1;o,2;2x,3;f,2;4,1;1,2;3,2;6u,j;b,1;r,3;1,1;2,2;6,1`, T: `174,b;1,1;1a,l;g,1;2t,7;2,6;2,2;1,4;bt,9;16,o;1,t;1clb,1` }, Kp = { 1569: [65152, 0, 0, 0], 1570: [65153, 65154, 0, 0], 1571: [65155, 65156, 0, 0], 1572: [65157, 65158, 0, 0], 1573: [65159, 65160, 0, 0], 1574: [65161, 65162, 65163, 65164], 1575: [65165, 65166, 0, 0], 1576: [65167, 65168, 65169, 65170], 1577: [65171, 65172, 0, 0], 1578: [65173, 65174, 65175, 65176], 1579: [65177, 65178, 65179, 65180], 1580: [65181, 65182, 65183, 65184], 1581: [65185, 65186, 65187, 65188], 1582: [65189, 65190, 65191, 65192], 1583: [65193, 65194, 0, 0], 1584: [65195, 65196, 0, 0], 1585: [65197, 65198, 0, 0], 1586: [65199, 65200, 0, 0], 1587: [65201, 65202, 65203, 65204], 1588: [65205, 65206, 65207, 65208], 1589: [65209, 65210, 65211, 65212], 1590: [65213, 65214, 65215, 65216], 1591: [65217, 65218, 65219, 65220], 1592: [65221, 65222, 65223, 65224], 1593: [65225, 65226, 65227, 65228], 1594: [65229, 65230, 65231, 65232], 1601: [65233, 65234, 65235, 65236], 1602: [65237, 65238, 65239, 65240], 1603: [65241, 65242, 65243, 65244], 1604: [65245, 65246, 65247, 65248], 1605: [65249, 65250, 65251, 65252], 1606: [65253, 65254, 65255, 65256], 1607: [65257, 65258, 65259, 65260], 1608: [65261, 65262, 0, 0], 1609: [65263, 65264, 64488, 64489], 1610: [65265, 65266, 65267, 65268], 1611: [65136, 0, 0, 65137], 1612: [65138, 0, 0, 0], 1613: [65140, 0, 0, 0], 1614: [65142, 0, 0, 65143], 1615: [65144, 0, 0, 65145], 1616: [65146, 0, 0, 65147], 1617: [65148, 0, 0, 65149], 1618: [65150, 0, 0, 65151], 1649: [64336, 64337, 0, 0], 1655: [64477, 0, 0, 0], 1657: [64358, 64359, 64360, 64361], 1658: [64350, 64351, 64352, 64353], 1659: [64338, 64339, 64340, 64341], 1662: [64342, 64343, 64344, 64345], 1663: [64354, 64355, 64356, 64357], 1664: [64346, 64347, 64348, 64349], 1667: [64374, 64375, 64376, 64377], 1668: [64370, 64371, 64372, 64373], 1670: [64378, 64379, 64380, 64381], 1671: [64382, 64383, 64384, 64385], 1672: [64392, 64393, 0, 0], 1676: [64388, 64389, 0, 0], 1677: [64386, 64387, 0, 0], 1678: [64390, 64391, 0, 0], 1681: [64396, 64397, 0, 0], 1688: [64394, 64395, 0, 0], 1700: [64362, 64363, 64364, 64365], 1702: [64366, 64367, 64368, 64369], 1705: [64398, 64399, 64400, 64401], 1709: [64467, 64468, 64469, 64470], 1711: [64402, 64403, 64404, 64405], 1713: [64410, 64411, 64412, 64413], 1715: [64406, 64407, 64408, 64409], 1722: [64414, 64415, 0, 0], 1723: [64416, 64417, 64418, 64419], 1726: [64426, 64427, 64428, 64429], 1728: [64420, 64421, 0, 0], 1729: [64422, 64423, 64424, 64425], 1733: [64480, 64481, 0, 0], 1734: [64473, 64474, 0, 0], 1735: [64471, 64472, 0, 0], 1736: [64475, 64476, 0, 0], 1737: [64482, 64483, 0, 0], 1739: [64478, 64479, 0, 0], 1740: [64508, 64509, 64510, 64511], 1744: [64484, 64485, 64486, 64487], 1746: [64430, 64431, 0, 0], 1747: [64432, 64433, 0, 0] }, qp = { \u0644\u0622: [65269, 65270], \u0644\u0623: [65271, 65272], \u0644\u0625: [65273, 65274], \u0644\u0627: [65275, 65276] }, Jp = 1604, Yp = /[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]/;
function Xp(e49, t2, n2) {
  let r2 = 0;
  for (let i2 of e49.split(`;`)) {
    let [e50, a2] = i2.split(`,`).map((e51) => parseInt(e51, 36)), o2 = r2 + e50;
    for (let e51 = o2; e51 < o2 + a2; e51++) n2.set(e51, t2);
    r2 = o2 + a2;
  }
}
const Zp = /* @__PURE__ */ new Map();
for (let [e49, t2] of Object.entries(Gp)) Xp(t2, e49, Zp);
function Qp(e49) {
  return Zp.get(e49) ?? `U`;
}
function $p(e49) {
  return e49 === `D` || e49 === `L` || e49 === `C`;
}
function em(e49) {
  return e49 === `D` || e49 === `R` || e49 === `C`;
}
function tm(e49, t2, n2) {
  return e49 === `D` ? t2 && n2 ? 3 : t2 ? 1 : n2 ? 2 : 0 : e49 === `R` ? +!!t2 : e49 === `L` ? n2 ? 2 : 0 : null;
}
function nm(e49) {
  return [...e49].map((e50) => {
    let t2 = e50.codePointAt(0);
    return { codePoint: t2, type: Qp(t2), form: null };
  });
}
function rm(e49) {
  let t2 = e49.filter((e50) => e50.type !== `T`);
  for (let e50 = 0; e50 < t2.length; e50++) {
    let n2 = t2[e50], r2 = e50 > 0 && $p(t2[e50 - 1].type), i2 = e50 + 1 < t2.length && em(t2[e50 + 1].type);
    n2.form = tm(n2.type, r2, i2);
  }
}
function im(e49) {
  let t2 = false;
  for (let n2 of e49) n2.type === `T` ? n2.form = t2 ? 3 : 0 : t2 = n2.form === 2 || n2.form === 3;
}
function am(e49) {
  let t2 = [];
  for (let n2 = 0; n2 < e49.length; n2++) {
    let r2 = e49[n2];
    if (r2.codePoint !== Jp) {
      t2.push(r2);
      continue;
    }
    let i2 = n2 + 1;
    for (; i2 < e49.length && e49[i2].type === `T`; ) i2++;
    let a2 = e49[i2], o2 = a2 && qp[String.fromCodePoint(Jp, a2.codePoint)];
    if (!o2) {
      t2.push(r2);
      continue;
    }
    let s2 = r2.form === 3 || r2.form === 1;
    t2.push({ codePoint: o2[+!!s2], type: `R`, form: 0 }), t2.push(...e49.slice(n2 + 1, i2)), n2 = i2;
  }
  return t2;
}
function om(e49) {
  if (e49.form === null) return e49.codePoint;
  let t2 = Kp[e49.codePoint];
  return t2 && (t2[e49.form] || t2[0]) || e49.codePoint;
}
function sm(e49) {
  if (!Yp.test(e49)) return e49;
  let t2 = nm(e49);
  return rm(t2), im(t2), am(t2).map((e50) => String.fromCodePoint(om(e50))).join(``);
}
function cm(e49, t2, n2) {
  let r2 = t2.layout.get(`text-transform`).evaluate(n2, {});
  return r2 === `uppercase` ? e49 = e49.toLocaleUpperCase() : r2 === `lowercase` && (e49 = e49.toLocaleLowerCase()), (Ip.applyArabicShaping ?? sm)(e49);
}
function lm(e49, t2, n2) {
  for (let r2 of e49.sections) r2.text = cm(r2.text, t2, n2);
  return e49;
}
function um(e49, t2, n2) {
  let r2 = n2 ? t2.last.points : t2.first.points, i2 = n2 ? r2[r2.length - 1] : r2[0];
  return `${e49}:${i2.x}:${i2.y}`;
}
function dm(e49) {
  let t2 = e49.first.points, n2 = t2.length;
  for (let t3 = e49.first.next; t3; t3 = t3.next) n2 += t3.points.length - 1;
  let r2 = Array(n2);
  for (let e50 = 0; e50 < t2.length; e50++) r2[e50] = t2[e50];
  let i2 = t2.length;
  for (let t3 = e49.first.next; t3; t3 = t3.next) {
    let e50 = t3.points;
    for (let t4 = 1; t4 < e50.length; t4++) r2[i2++] = e50[t4];
  }
  return r2;
}
function fm(e49, t2, n2, r2, i2) {
  let a2 = t2[n2];
  delete t2[n2], t2[r2] = a2;
  let o2 = e49[a2];
  return o2.last.next = i2.first, o2.last = i2.last, a2;
}
function pm(e49, t2, n2, r2, i2) {
  let a2 = t2[r2];
  delete t2[r2], t2[n2] = a2;
  let o2 = e49[a2];
  return i2.last.next = o2.first, o2.first = i2.first, a2;
}
function mm(e49) {
  let t2 = {}, n2 = {}, r2 = [];
  for (let i3 of e49) {
    let e50 = i3.text ? i3.text.toString() : null, a2 = { points: i3.geometry[0], next: null }, o2 = { feature: i3, first: a2, last: a2 };
    if (!e50) {
      r2.push(o2);
      continue;
    }
    let s2 = um(e50, o2), c2 = um(e50, o2, true);
    if (s2 in n2 && c2 in t2 && n2[s2] !== t2[c2]) {
      let i4 = pm(r2, t2, s2, c2, o2), a3 = fm(r2, n2, s2, c2, r2[i4]);
      delete t2[s2], delete n2[c2], n2[um(e50, r2[a3], true)] = a3, r2[i4].feature.geometry = null;
    } else if (s2 in n2) fm(r2, n2, s2, c2, o2);
    else if (c2 in t2) pm(r2, t2, s2, c2, o2);
    else {
      let e51 = r2.push(o2) - 1;
      t2[s2] = e51, n2[c2] = e51;
    }
  }
  let i2 = [];
  for (let e50 of r2) {
    let t3 = e50.feature;
    t3.geometry && (e50.first.next && (t3.geometry[0] = dm(e50)), i2.push(t3));
  }
  return i2;
}
const hm = typeof Intl < `u` && `Segmenter` in Intl;
let gm, _m;
function vm(e49) {
  if (!hm || !Hp(e49)) return [...e49];
  gm ??= new Intl.Segmenter(void 0, { granularity: `grapheme` });
  let t2 = [];
  for (let { segment: n2 } of gm.segment(e49)) {
    let e50 = t2.length - 1;
    e50 >= 0 && Wp(t2[e50], n2) ? t2[e50] += n2 : t2.push(n2);
  }
  return t2;
}
function ym(e49) {
  let t2 = /* @__PURE__ */ new Set();
  if (hm) {
    _m ??= new Intl.Segmenter(void 0, { granularity: `word` });
    for (let { index: n3 } of _m.segment(e49)) t2.add(n3);
    return t2;
  }
  let n2 = 0;
  for (let r2 of e49.split(new RegExp("\\b|(?=\\p{Ideo})", "u"))) t2.add(n2), n2 += r2.length;
  return t2;
}
function bm(e49) {
  let t2 = e49.codePointAt(0);
  return e49.length > (t2 > 65535 ? 2 : 1);
}
function xm(e49) {
  return /\s/u.test(String.fromCodePoint(e49));
}
function Sm(e49) {
  for (let t2 of e49) if (Bp(t2.codePointAt(0))) return true;
  return false;
}
function Cm(e49) {
  for (let t2 of e49) if (!wm(t2.codePointAt(0))) return false;
  return true;
}
function wm(e49) {
  return !Lp(e49);
}
function Tm(e49) {
  return !(Bp(e49) || Vp(e49));
}
function Em(e49) {
  return new RegExp("\\p{sc=Arab}", "u").test(String.fromCodePoint(e49));
}
function Dm(e49) {
  return Rp(e49);
}
function Om(e49) {
  for (let t2 of e49) if (Dm(t2.codePointAt(0))) return true;
  return false;
}
const km = { "!": `\uFE15`, "#": `\uFF03`, $: `\uFF04`, "%": `\uFF05`, "&": `\uFF06`, "(": `\uFE35`, ")": `\uFE36`, "*": `\uFF0A`, "+": `\uFF0B`, ",": `\uFE10`, "-": `\uFE32`, ".": `\u30FB`, "/": `\uFF0F`, ":": `\uFE13`, ";": `\uFE14`, "<": `\uFE3F`, "=": `\uFF1D`, ">": `\uFE40`, "?": `\uFE16`, "@": `\uFF20`, "[": `\uFE47`, "\\": `\uFF3C`, "]": `\uFE48`, "^": `\uFF3E`, _: `\uFE33`, "`": `\uFF40`, "{": `\uFE37`, "|": `\u2015`, "}": `\uFE38`, "~": `\uFF5E`, "\xA2": `\uFFE0`, "\xA3": `\uFFE1`, "\xA5": `\uFFE5`, "\xA6": `\uFFE4`, "\xAC": `\uFFE2`, "\xAF": `\uFFE3`, "\u2013": `\uFE32`, "\u2014": `\uFE31`, "\u2018": `\uFE43`, "\u2019": `\uFE44`, "\u201C": `\uFE41`, "\u201D": `\uFE42`, "\u2026": `\uFE19`, "\u22EF": `\uFE19`, "\u2027": `\u30FB`, "\u20A9": `\uFFE6`, "\u3001": `\uFE11`, "\u3002": `\uFE12`, "\u3008": `\uFE3F`, "\u3009": `\uFE40`, "\u300A": `\uFE3D`, "\u300B": `\uFE3E`, "\u300C": `\uFE41`, "\u300D": `\uFE42`, "\u300E": `\uFE43`, "\u300F": `\uFE44`, "\u3010": `\uFE3B`, "\u3011": `\uFE3C`, "\u3014": `\uFE39`, "\u3015": `\uFE3A`, "\u3016": `\uFE17`, "\u3017": `\uFE18`, "\uFF01": `\uFE15`, "\uFF08": `\uFE35`, "\uFF09": `\uFE36`, "\uFF0C": `\uFE10`, "\uFF0D": `\uFE32`, "\uFF0E": `\u30FB`, "\uFF1A": `\uFE13`, "\uFF1B": `\uFE14`, "\uFF1C": `\uFE3F`, "\uFF1E": `\uFE40`, "\uFF1F": `\uFE16`, "\uFF3B": `\uFE47`, "\uFF3D": `\uFE48`, "\uFF3F": `\uFE33`, "\uFF5B": `\uFE37`, "\uFF5C": `\u2015`, "\uFF5D": `\uFE38`, "\uFF5F": `\uFE35`, "\uFF60": `\uFE36`, "\uFF61": `\uFE12`, "\uFF62": `\uFE41`, "\uFF63": `\uFE42` };
function Am(e49) {
  let t2 = ``, n2 = { premature: true, value: void 0 }, r2 = e49[Symbol.iterator](), i2 = r2.next(), a2 = e49[Symbol.iterator]();
  a2.next();
  let o2 = a2.next();
  for (; !i2.done; ) (o2.done || !Tm(o2.value.codePointAt(0)) || km[o2.value]) && (n2.premature || !Tm(n2.value.codePointAt(0)) || km[n2.value]) && km[i2.value] ? t2 += km[i2.value] : t2 += i2.value, n2 = { value: i2.value, premature: false }, i2 = r2.next(), o2 = a2.next();
  return t2;
}
const jm = { 10: true, 13: true, 32: true, 38: true, 41: true, 43: true, 45: true, 47: true, 173: true, 183: true, 8203: true, 8208: true, 8211: true, 8231: true }, Mm = { 40: true };
function Nm(e49, t2, n2, r2, i2, a2) {
  var _a3;
  if (`fontStack` in t2) {
    let r3 = (_a3 = n2[t2.fontStack]) == null ? void 0 : _a3.default, a3 = r3 == null ? void 0 : r3[e49];
    if (a3) return a3.metrics.advance * t2.scale + i2;
    let o2 = 0;
    for (let n3 of e49) {
      let e50 = r3 == null ? void 0 : r3[n3];
      e50 && (o2 += e50.metrics.advance * t2.scale + i2);
    }
    return o2;
  }
  {
    let e50 = r2[t2.imageName];
    return e50 ? e50.displaySize[0] * t2.scale * 24 / a2 + i2 : 0;
  }
}
function Pm(e49, t2, n2, r2) {
  let i2 = (e49 - t2) ** 2;
  return r2 ? e49 < t2 ? i2 / 2 : i2 * 2 : i2 + Math.abs(n2) * n2;
}
function Fm(e49) {
  return /^\s+$/u.test(e49);
}
function Im(e49, t2, n2) {
  let r2 = 0;
  return (e49 === 10 || e49 === 13) && (r2 -= 1e4), n2 && (r2 += 150), (e49 === 40 || e49 === 65288) && (r2 += 50), (t2 === 41 || t2 === 65289) && (r2 += 50), r2;
}
function Lm(e49, t2, n2, r2, i2, a2) {
  let o2 = null, s2 = Pm(t2, n2, i2, a2);
  for (let e50 of r2) {
    let r3 = Pm(t2 - e50.x, n2, i2, a2) + e50.badness;
    r3 <= s2 && (o2 = e50, s2 = r3);
  }
  return { index: e49, x: t2, priorBreak: o2, badness: s2 };
}
function Rm(e49) {
  return e49 ? Rm(e49.priorBreak).concat(e49.index) : [];
}
var zm = class e45 {
  constructor(e49 = ``, t2 = [], n2 = []) {
    this.text = e49, this.sections = t2, this.sectionIndex = n2, this.imageSectionID = null, this._graphemes = null;
  }
  graphemes() {
    return this._graphemes ??= vm(this.text), this._graphemes;
  }
  static fromFeature(t2, n2) {
    let r2 = new e45();
    for (let e49 of t2.sections) e49.image ? r2.addImageSection(e49) : r2.addTextSection(e49, n2);
    return r2;
  }
  length() {
    return this.graphemes().length;
  }
  getSection(e49) {
    return this.sections[this.sectionIndex[e49]];
  }
  getSectionIndex(e49) {
    return this.sectionIndex[e49];
  }
  verticalizePunctuation() {
    this.text = Am(this.text), this._graphemes = null;
  }
  hasZeroWidthSpaces() {
    return this.text.includes(`\u200B`);
  }
  trim() {
    let e49 = this.graphemes(), t2 = 0;
    for (; t2 < e49.length && Fm(e49[t2]); ) t2++;
    let n2 = e49.length;
    for (; n2 > t2 && Fm(e49[n2 - 1]); ) n2--;
    this.text = e49.slice(t2, n2).join(``), this.sectionIndex = this.sectionIndex.slice(t2, n2), this._graphemes = null;
  }
  substring(t2, n2) {
    let r2 = this.graphemes().slice(t2, n2).join(``), i2 = this.sectionIndex.slice(t2, n2);
    return new e45(r2, this.sections, i2);
  }
  toCodeUnitIndex(e49) {
    return this.graphemes().slice(0, e49).join(``).length;
  }
  toString() {
    return this.text;
  }
  getMaxScale() {
    return this.sectionIndex.reduce((e49, t2) => Math.max(e49, this.sections[t2].scale), 0);
  }
  getMaxImageSize(e49) {
    let t2 = 0, n2 = 0;
    for (let r2 = 0; r2 < this.length(); r2++) {
      let i2 = this.getSection(r2);
      if (`imageName` in i2) {
        let r3 = e49[i2.imageName];
        if (!r3) continue;
        let a2 = r3.displaySize;
        t2 = Math.max(t2, a2[0]), n2 = Math.max(n2, a2[1]);
      }
    }
    return { maxImageWidth: t2, maxImageHeight: n2 };
  }
  _appendSection(e49, t2) {
    let n2 = this.graphemes(), r2 = n2.length > 0 ? n2[n2.length - 1] : ``, i2 = vm(r2 + e49);
    this.text += e49, this._graphemes = n2.slice(0, r2 ? -1 : void 0).concat(i2);
    let a2 = i2.length - +!!r2;
    for (let e50 = 0; e50 < a2; e50++) this.sectionIndex.push(t2);
  }
  addTextSection(e49, t2) {
    this.sections.push({ scale: e49.scale || 1, verticalAlign: e49.verticalAlign || `bottom`, fontStack: e49.fontStack || t2 }), this._appendSection(e49.text, this.sections.length - 1);
  }
  addImageSection(e49) {
    let t2 = e49.image ? e49.image.name : ``;
    if (t2.length === 0) {
      A(`Can't add FormattedSection with an empty image.`);
      return;
    }
    let n2 = this.getNextImageSectionCharCode();
    if (!n2) {
      A(`Reached maximum number of images 6401`);
      return;
    }
    this.sections.push({ scale: 1, verticalAlign: e49.verticalAlign || `bottom`, imageName: t2 }), this._appendSection(String.fromCharCode(n2), this.sections.length - 1);
  }
  getNextImageSectionCharCode() {
    return this.imageSectionID ? this.imageSectionID >= 63743 ? null : ++this.imageSectionID : (this.imageSectionID = 57344, this.imageSectionID);
  }
  determineLineBreaks(e49, t2, n2, r2, i2) {
    let a2 = this.determineTotalWidth(e49, n2, r2, i2);
    if (e49 >= 0 && a2 > 0 && a2 <= t2 && !/[\n\r]/.test(this.text)) return [this.length()];
    let o2 = [], s2 = a2 / Math.max(1, Math.ceil(a2 / t2)), c2 = this.hasZeroWidthSpaces(), l2 = this.graphemes(), u2 = null, d2 = 0, f2 = 0;
    for (let t3 = 0; t3 < l2.length; t3++) {
      let a3 = l2[t3];
      if (t3 > 0) {
        let e50 = l2[t3 - 1].codePointAt(0), n3 = a3.codePointAt(0), r3 = zp(e50), i3 = Up(e50) && Up(n3);
        (jm[e50] || r3 || `imageName` in this.getSection(t3 - 1) || l2[t3 + 1] !== void 0 && Mm[n3] || i3 && (u2 ??= ym(this.text)).has(f2)) && o2.push(Lm(t3, d2, s2, o2, Im(e50, n3, r3 && c2), false));
      }
      xm(a3.codePointAt(0)) || (d2 += Nm(a3, this.getSection(t3), n2, r2, e49, i2)), f2 += a3.length;
    }
    return Rm(Lm(this.length(), d2, s2, o2, 0, true));
  }
  determineTotalWidth(e49, t2, n2, r2) {
    let i2 = 0, a2 = 0;
    for (let o2 of this.graphemes()) {
      let s2 = this.getSection(a2);
      i2 += Nm(o2, s2, t2, n2, e49, r2), a2++;
    }
    return i2;
  }
};
function Bm() {
  return (function(e49) {
    var t2 = { R: `13k,1a,2,3,3,2+1j,ch+16,a+1,5+2,2+n,5,a,4,6+16,4+3,h+1b,4mo,179q,2+9,2+11,2i9+7y,2+68,4,3+4,5+13,4+3,2+4k,3+29,8+cf,1t+7z,w+17,3+3m,1t+3z,16o1+5r,8+30,8+mc,29+1r,29+4v,75+73`, EN: `1c+9,3d+1,6,187+9,513,4+5,7+9,sf+j,175h+9,qw+q,161f+1d,4xt+a,25i+9`, ES: `17,2,6dp+1,f+1,av,16vr,mx+1,4o,2`, ET: `z+2,3h+3,b+1,ym,3e+1,2o,p4+1,8,6u,7c,g6,1wc,1n9+4,30+1b,2n,6d,qhx+1,h0m,a+1,49+2,63+1,4+1,6bb+3,12jj`, AN: `16o+5,2j+9,2+1,35,ed,1ff2+9,87+u`, CS: `18,2+1,b,2u,12k,55v,l,17v0,2,3,53,2+1,b`, B: `a,3,f+2,2v,690`, S: `9,2,k`, WS: `c,k,4f4,1vk+a,u,1j,335`, ON: `x+1,4+4,h+5,r+5,r+3,z,5+3,2+1,2+1,5,2+2,3+4,o,w,ci+1,8+d,3+d,6+8,2+g,39+1,9,6+1,2,33,b8,3+1,3c+1,7+1,5r,b,7h+3,sa+5,2,3i+6,jg+3,ur+9,2v,ij+1,9g+9,7+a,8m,4+1,49+x,14u,2+2,c+2,e+2,e+2,e+1,i+n,e+e,2+p,u+2,e+2,36+1,2+3,2+1,b,2+2,6+5,2,2,2,h+1,5+4,6+3,3+f,16+2,5+3l,3+81,1y+p,2+40,q+a,m+13,2r+ch,2+9e,75+hf,3+v,2+2w,6e+5,f+6,75+2a,1a+p,2+2g,d+5x,r+b,6+3,4+o,g,6+1,6+2,2k+1,4,2j,5h+z,1m+1,1e+f,t+2,1f+e,d+3,4o+3,2s+1,w,535+1r,h3l+1i,93+2,2s,b+1,3l+x,2v,4g+3,21+3,kz+1,g5v+1,5a,j+9,n+v,2,3,2+8,2+1,3+2,2,3,46+1,4+4,h+5,r+5,r+a,3h+2,4+6,b+4,78,1r+24,4+c,4,1hb,ey+6,103+j,16j+c,1ux+7,5+g,fsh,jdq+1t,4,57+2e,p1,1m,1m,1m,1m,4kt+1,7j+17,5+2r,d+e,3+e,2+e,2+10,m+4,w,1n+5,1q,4z+5,4b+rb,9+c,4+c,4+37,d+2g,8+b,l+b,5+1j,9+9,7+13,9+t,3+1,27+3c,2+29,2+3q,d+d,3+4,4+2,6+6,a+o,8+6,a+2,e+6,16+42,2+1i`, BN: `0+8,6+d,2s+5,2+p,e,4m9,1kt+2,2b+5,5+5,17q9+v,7k,6p+8,6+1,119d+3,440+7,96s+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+1,1ekf+75,6p+2rz,1ben+1,1ekf+1,1ekf+1`, NSM: `lc+33,7o+6,7c+18,2,2+1,2+1,2,21+a,1d+k,h,2u+6,3+5,3+1,2+3,10,v+q,2k+a,1n+8,a,p+3,2+8,2+2,2+4,18+2,3c+e,2+v,1k,2,5+7,5,4+6,b+1,u,1n,5+3,9,l+1,r,3+1,1m,5+1,5+1,3+2,4,v+1,4,c+1,1m,5+4,2+1,5,l+1,n+5,2,1n,3,2+3,9,8+1,c+1,v,1q,d,1f,4,1m+2,6+2,2+3,8+1,c+1,u,1n,g+1,l+1,t+1,1m+1,5+3,9,l+1,u,21,8+2,2,2j,3+6,d+7,2r,3+8,c+5,23+1,s,2,2,1k+d,2+4,2+1,6+a,2+z,a,2v+3,2+5,2+1,3+1,q+1,5+2,h+3,e,3+1,7,g,jk+2,qb+2,u+2,u+1,v+1,1t+1,2+6,9,3+a,a,1a+2,3c+1,z,3b+2,5+1,a,7+2,64+1,3,1n,2+6,2,2,3+7,7+9,3,1d+g,1s+3,1d,2+4,2,6,15+8,d+1,x+3,3+1,2+2,1l,2+1,4,2+2,1n+7,3+1,49+2,2+c,2+6,5,7,4+1,5j+1l,2+4,k1+w,2db+2,3y,2p+v,ff+3,30+1,n9x+3,2+9,x+1,29+1,7l,4,5,q+1,6,48+1,r+h,e,13+7,q+a,1b+2,1d,3+3,3+1,14,1w+5,3+1,3+1,d,9,1c,1g,2+2,3+1,6+1,2,17+1,9,6n,3,5,fn5,ki+f,h+f,r2,6b,46+4,1af+2,2+1,6+3,15+2,5,4m+1,fy+3,as+1,4a+a,4x,1j+e,1l+2,1e+3,3+1,1y+2,11+4,2+7,1r,d+1,1h+8,b+3,3,2o+2,3,2+1,7,4h,4+7,m+1,1m+1,4,12+6,4+4,5g+7,3+2,2,o,2d+5,2,5+1,2+1,6n+3,7+1,2+1,s+1,2e+7,3,2+1,2z,2,3+5,2,2u+2,3+3,2+4,78+8,2+1,75+1,2,5,41+3,3+1,5,x+5,3+1,15+5,3+3,9,a+5,3+2,1b+c,2+1,bb+6,2+5,2d+l,3+6,2+1,2+1,3f+5,4,2+1,2+6,2,21+1,4,2,9o+1,f0c+4,1o+6,t5,1s+3,2a,f5l+1,43t+2,i+7,3+6,v+3,45+2,1j0+1i,5+1d,9,f,n+4,2+e,11t+6,2+g,3+6,2+1,2+4,7a+6,c6+3,15t+6,32+6,gzhy+6n`, AL: `16w,3,2,e+1b,z+2,2+2s,g+1,8+1,b+m,2+t,s+2i,c+e,4h+f,1d+1e,1bwe+dp,3+3z,x+c,2+1,35+3y,2rm+z,5+7,b+5,dt+l,c+u,17nl+27,1t+27,4x+6n,3+d`, LRO: `6ct`, RLO: `6cu`, LRE: `6cq`, RLE: `6cr`, PDF: `6cs`, LRI: `6ee`, RLI: `6ef`, FSI: `6eg`, PDI: `6eh` }, n2 = {}, r2 = {};
    n2.L = 1, r2[1] = `L`, Object.keys(t2).forEach(function(e50, t3) {
      n2[e50] = 1 << t3 + 1, r2[n2[e50]] = e50;
    }), Object.freeze(n2);
    var i2 = n2.LRI | n2.RLI | n2.FSI, a2 = n2.L | n2.R | n2.AL, o2 = n2.B | n2.S | n2.WS | n2.ON | n2.FSI | n2.LRI | n2.RLI | n2.PDI, s2 = n2.BN | n2.RLE | n2.LRE | n2.RLO | n2.LRO | n2.PDF, c2 = n2.S | n2.WS | n2.B | i2 | n2.PDI | s2, l2 = null;
    function u2() {
      if (!l2) {
        l2 = /* @__PURE__ */ new Map();
        var e50 = 0;
        for (var r3 in t2) if (t2.hasOwnProperty(r3)) for (var i3 = t2[r3], a3 = ``, o3 = void 0, s3 = false, c3 = 0, u3 = 0; u3 <= i3.length + 1; u3 += 1) {
          var d3 = i3[u3];
          if (d3 !== `,` && u3 !== i3.length) d3 === `+` ? (s3 = true, c3 = e50 = c3 + parseInt(a3, 36), a3 = ``) : a3 += d3;
          else {
            s3 ? o3 = e50 + parseInt(a3, 36) : (c3 = e50 = c3 + parseInt(a3, 36), o3 = e50), s3 = false, a3 = ``, c3 = o3;
            for (var f3 = e50; f3 < o3 + 1; f3 += 1) l2.set(f3, n2[r3]);
          }
        }
      }
    }
    function d2(e50) {
      return u2(), l2.get(e50.codePointAt(0)) || n2.L;
    }
    function f2(e50) {
      return r2[d2(e50)];
    }
    var p2 = { pairs: `14>1,1e>2,u>2,2wt>1,1>1,1ge>1,1wp>1,1j>1,f>1,hm>1,1>1,u>1,u6>1,1>1,+5,28>1,w>1,1>1,+3,b8>1,1>1,+3,1>3,-1>-1,3>1,1>1,+2,1s>1,1>1,x>1,th>1,1>1,+2,db>1,1>1,+3,3>1,1>1,+2,14qm>1,1>1,+1,4q>1,1e>2,u>2,2>1,+1`, canonical: `6f1>-6dx,6dy>-6dx,6ec>-6ed,6ee>-6ed,6ww>2jj,-2ji>2jj,14r4>-1e7l,1e7m>-1e7l,1e7m>-1e5c,1e5d>-1e5b,1e5c>-14qx,14qy>-14qx,14vn>-1ecg,1ech>-1ecg,1edu>-1ecg,1eci>-1ecg,1eda>-1ecg,1eci>-1ecg,1eci>-168q,168r>-168q,168s>-14ye,14yf>-14ye` };
    function m2(e50, t3) {
      var n3 = 0, r3 = /* @__PURE__ */ new Map(), i3 = t3 && /* @__PURE__ */ new Map(), a3;
      return e50.split(`,`).forEach(function e51(o3) {
        if (o3.indexOf(`+`) !== -1) for (var s3 = +o3; s3--; ) e51(a3);
        else {
          a3 = o3;
          var c3 = o3.split(`>`), l3 = c3[0], u3 = c3[1];
          l3 = String.fromCodePoint(n3 += parseInt(l3, 36)), u3 = String.fromCodePoint(n3 += parseInt(u3, 36)), r3.set(l3, u3), t3 && i3.set(u3, l3);
        }
      }), { map: r3, reverseMap: i3 };
    }
    var h2, g2, _2;
    function v2() {
      if (!h2) {
        var e50 = m2(p2.pairs, true), t3 = e50.map, n3 = e50.reverseMap;
        h2 = t3, g2 = n3, _2 = m2(p2.canonical, false).map;
      }
    }
    function y2(e50) {
      return v2(), h2.get(e50) || null;
    }
    function b2(e50) {
      return v2(), g2.get(e50) || null;
    }
    function x2(e50) {
      return v2(), _2.get(e50) || null;
    }
    var S2 = n2.L, C2 = n2.R, w2 = n2.EN, T2 = n2.ES, E2 = n2.ET, D2 = n2.AN, ee2 = n2.CS, O2 = n2.B, k2 = n2.S, A2 = n2.ON, te2 = n2.BN, ne2 = n2.NSM, re2 = n2.AL, ie2 = n2.LRO, ae2 = n2.RLO, oe2 = n2.LRE, se2 = n2.RLE, ce2 = n2.PDF, le2 = n2.LRI, ue2 = n2.RLI, de2 = n2.FSI, fe2 = n2.PDI;
    function pe2(e50, t3) {
      for (var n3 = 125, r3 = new Uint32Array(e50.length), l3 = 0; l3 < e50.length; l3++) r3[l3] = d2(e50[l3]);
      var u3 = /* @__PURE__ */ new Map();
      function f3(e51, t4) {
        var n4 = r3[e51];
        r3[e51] = t4, u3.set(n4, u3.get(n4) - 1), n4 & o2 && u3.set(o2, u3.get(o2) - 1), u3.set(t4, (u3.get(t4) || 0) + 1), t4 & o2 && u3.set(o2, (u3.get(o2) || 0) + 1);
      }
      for (var p3 = new Uint8Array(e50.length), m3 = /* @__PURE__ */ new Map(), h3 = [], g3 = null, _3 = 0; _3 < e50.length; _3++) g3 || h3.push(g3 = { start: _3, end: e50.length - 1, level: t3 === `rtl` ? 1 : t3 === `ltr` ? 0 : Zt2(_3, false) }), r3[_3] & O2 && (g3.end = _3, g3 = null);
      for (var v3 = se2 | oe2 | ae2 | ie2 | i2 | fe2 | ce2 | O2, pe3 = function(e51) {
        return e51 + (e51 & 1 ? 1 : 2);
      }, me3 = function(e51) {
        return e51 + (e51 & 1 ? 2 : 1);
      }, he3 = 0; he3 < h3.length; he3++) {
        g3 = h3[he3];
        var ge3 = [{ _level: g3.level, _override: 0, _isolate: 0 }], j3 = void 0, M3 = 0, _e3 = 0, N3 = 0;
        u3.clear();
        for (var P3 = g3.start; P3 <= g3.end; P3++) {
          var F2 = r3[P3];
          if (j3 = ge3[ge3.length - 1], u3.set(F2, (u3.get(F2) || 0) + 1), F2 & o2 && u3.set(o2, (u3.get(o2) || 0) + 1), F2 & v3) {
            if (F2 & (se2 | oe2)) {
              p3[P3] = j3._level;
              var I2 = (F2 === se2 ? me3 : pe3)(j3._level);
              I2 <= n3 && !M3 && !_e3 ? ge3.push({ _level: I2, _override: 0, _isolate: 0 }) : M3 || _e3++;
            } else if (F2 & (ae2 | ie2)) {
              p3[P3] = j3._level;
              var L2 = (F2 === ae2 ? me3 : pe3)(j3._level);
              L2 <= n3 && !M3 && !_e3 ? ge3.push({ _level: L2, _override: F2 & ae2 ? C2 : S2, _isolate: 0 }) : M3 || _e3++;
            } else if (F2 & i2) {
              F2 & de2 && (F2 = Zt2(P3 + 1, true) === 1 ? ue2 : le2), p3[P3] = j3._level, j3._override && f3(P3, j3._override);
              var ve2 = (F2 === ue2 ? me3 : pe3)(j3._level);
              ve2 <= n3 && M3 === 0 && _e3 === 0 ? (N3++, ge3.push({ _level: ve2, _override: 0, _isolate: 1, _isolInitIndex: P3 })) : M3++;
            } else if (F2 & fe2) {
              if (M3 > 0) M3--;
              else if (N3 > 0) {
                for (_e3 = 0; !ge3[ge3.length - 1]._isolate; ) ge3.pop();
                var ye2 = ge3[ge3.length - 1]._isolInitIndex;
                ye2 != null && (m3.set(ye2, P3), m3.set(P3, ye2)), ge3.pop(), N3--;
              }
              j3 = ge3[ge3.length - 1], p3[P3] = j3._level, j3._override && f3(P3, j3._override);
            } else F2 & ce2 ? (M3 === 0 && (_e3 > 0 ? _e3-- : !j3._isolate && ge3.length > 1 && (ge3.pop(), j3 = ge3[ge3.length - 1])), p3[P3] = j3._level) : F2 & O2 && (p3[P3] = g3.level);
          } else p3[P3] = j3._level, j3._override && F2 !== te2 && f3(P3, j3._override);
        }
        for (var be2 = [], R2 = null, xe2 = g3.start; xe2 <= g3.end; xe2++) {
          var Se2 = r3[xe2];
          if (!(Se2 & s2)) {
            var Ce2 = p3[xe2], we2 = Se2 & i2, Te2 = Se2 === fe2;
            R2 && Ce2 === R2._level ? (R2._end = xe2, R2._endsWithIsolInit = we2) : be2.push(R2 = { _start: xe2, _end: xe2, _level: Ce2, _startsWithPDI: Te2, _endsWithIsolInit: we2 });
          }
        }
        for (var Ee2 = [], De2 = 0; De2 < be2.length; De2++) {
          var Oe2 = be2[De2];
          if (!Oe2._startsWithPDI || Oe2._startsWithPDI && !m3.has(Oe2._start)) {
            for (var ke2 = [R2 = Oe2], z2 = void 0; R2 && R2._endsWithIsolInit && (z2 = m3.get(R2._end)) != null; ) for (var Ae2 = De2 + 1; Ae2 < be2.length; Ae2++) if (be2[Ae2]._start === z2) {
              ke2.push(R2 = be2[Ae2]);
              break;
            }
            for (var je2 = [], Me2 = 0; Me2 < ke2.length; Me2++) for (var Ne2 = ke2[Me2], Pe2 = Ne2._start; Pe2 <= Ne2._end; Pe2++) je2.push(Pe2);
            for (var Fe2 = p3[je2[0]], Ie2 = g3.level, Le2 = je2[0] - 1; Le2 >= 0; Le2--) if (!(r3[Le2] & s2)) {
              Ie2 = p3[Le2];
              break;
            }
            var Re2 = je2[je2.length - 1], ze2 = p3[Re2], Be2 = g3.level;
            if (!(r3[Re2] & i2)) {
              for (var Ve2 = Re2 + 1; Ve2 <= g3.end; Ve2++) if (!(r3[Ve2] & s2)) {
                Be2 = p3[Ve2];
                break;
              }
            }
            Ee2.push({ _seqIndices: je2, _sosType: Math.max(Ie2, Fe2) % 2 ? C2 : S2, _eosType: Math.max(Be2, ze2) % 2 ? C2 : S2 });
          }
        }
        for (var He2 = 0; He2 < Ee2.length; He2++) {
          var Ue2 = Ee2[He2], B2 = Ue2._seqIndices, We2 = Ue2._sosType, Ge2 = Ue2._eosType, Ke2 = p3[B2[0]] & 1 ? C2 : S2;
          if (u3.get(ne2)) for (var qe2 = 0; qe2 < B2.length; qe2++) {
            var Je2 = B2[qe2];
            if (r3[Je2] & ne2) {
              for (var Ye2 = We2, Xe2 = qe2 - 1; Xe2 >= 0; Xe2--) if (!(r3[B2[Xe2]] & s2)) {
                Ye2 = r3[B2[Xe2]];
                break;
              }
              f3(Je2, Ye2 & (i2 | fe2) ? A2 : Ye2);
            }
          }
          if (u3.get(w2)) for (var Ze2 = 0; Ze2 < B2.length; Ze2++) {
            var Qe2 = B2[Ze2];
            if (r3[Qe2] & w2) for (var $e2 = Ze2 - 1; $e2 >= -1; $e2--) {
              var et2 = $e2 === -1 ? We2 : r3[B2[$e2]];
              if (et2 & a2) {
                et2 === re2 && f3(Qe2, D2);
                break;
              }
            }
          }
          if (u3.get(re2)) for (var tt2 = 0; tt2 < B2.length; tt2++) {
            var nt2 = B2[tt2];
            r3[nt2] & re2 && f3(nt2, C2);
          }
          if (u3.get(T2) || u3.get(ee2)) for (var rt2 = 1; rt2 < B2.length - 1; rt2++) {
            var it2 = B2[rt2];
            if (r3[it2] & (T2 | ee2)) {
              for (var at2 = 0, ot2 = 0, st2 = rt2 - 1; st2 >= 0 && (at2 = r3[B2[st2]], at2 & s2); st2--) ;
              for (var V2 = rt2 + 1; V2 < B2.length && (ot2 = r3[B2[V2]], ot2 & s2); V2++) ;
              at2 === ot2 && (r3[it2] === T2 ? at2 === w2 : at2 & (w2 | D2)) && f3(it2, at2);
            }
          }
          if (u3.get(w2)) {
            for (var ct2 = 0; ct2 < B2.length; ct2++) if (r3[B2[ct2]] & w2) {
              for (var lt2 = ct2 - 1; lt2 >= 0 && r3[B2[lt2]] & (E2 | s2); lt2--) f3(B2[lt2], w2);
              for (ct2++; ct2 < B2.length && r3[B2[ct2]] & (E2 | s2 | w2); ct2++) r3[B2[ct2]] !== w2 && f3(B2[ct2], w2);
            }
          }
          if (u3.get(E2) || u3.get(T2) || u3.get(ee2)) for (var ut2 = 0; ut2 < B2.length; ut2++) {
            var dt2 = B2[ut2];
            if (r3[dt2] & (E2 | T2 | ee2)) {
              f3(dt2, A2);
              for (var ft2 = ut2 - 1; ft2 >= 0 && r3[B2[ft2]] & s2; ft2--) f3(B2[ft2], A2);
              for (var pt2 = ut2 + 1; pt2 < B2.length && r3[B2[pt2]] & s2; pt2++) f3(B2[pt2], A2);
            }
          }
          if (u3.get(w2)) for (var H2 = 0, mt2 = We2; H2 < B2.length; H2++) {
            var ht2 = B2[H2], gt2 = r3[ht2];
            gt2 & w2 ? mt2 === S2 && f3(ht2, S2) : gt2 & a2 && (mt2 = gt2);
          }
          if (u3.get(o2)) {
            for (var _t2 = C2 | w2 | D2, vt2 = _t2 | S2, yt2 = [], bt2 = [], U2 = 0; U2 < B2.length; U2++) if (r3[B2[U2]] & o2) {
              var xt2 = e50[B2[U2]], St2 = void 0;
              if (y2(xt2) !== null) {
                if (bt2.length < 63) bt2.push({ char: xt2, seqIndex: U2 });
                else break;
              } else if ((St2 = b2(xt2)) !== null) for (var Ct2 = bt2.length - 1; Ct2 >= 0; Ct2--) {
                var wt2 = bt2[Ct2].char;
                if (wt2 === St2 || wt2 === b2(x2(xt2)) || y2(x2(wt2)) === xt2) {
                  yt2.push([bt2[Ct2].seqIndex, U2]), bt2.length = Ct2;
                  break;
                }
              }
            }
            yt2.sort(function(e51, t4) {
              return e51[0] - t4[0];
            });
            for (var Tt2 = 0; Tt2 < yt2.length; Tt2++) {
              for (var Et2 = yt2[Tt2], Dt2 = Et2[0], Ot2 = Et2[1], kt2 = false, At2 = 0, jt2 = Dt2 + 1; jt2 < Ot2; jt2++) {
                var Mt2 = B2[jt2];
                if (r3[Mt2] & vt2) {
                  kt2 = true;
                  var Nt2 = r3[Mt2] & _t2 ? C2 : S2;
                  if (Nt2 === Ke2) {
                    At2 = Nt2;
                    break;
                  }
                }
              }
              if (kt2 && !At2) {
                At2 = We2;
                for (var Pt2 = Dt2 - 1; Pt2 >= 0; Pt2--) {
                  var Ft2 = B2[Pt2];
                  if (r3[Ft2] & vt2) {
                    var It2 = r3[Ft2] & _t2 ? C2 : S2;
                    At2 = It2 === Ke2 ? Ke2 : It2;
                    break;
                  }
                }
              }
              if (At2) {
                if (r3[B2[Dt2]] = r3[B2[Ot2]] = At2, At2 !== Ke2) {
                  for (var Lt2 = Dt2 + 1; Lt2 < B2.length; Lt2++) if (!(r3[B2[Lt2]] & s2)) {
                    d2(e50[B2[Lt2]]) & ne2 && (r3[B2[Lt2]] = At2);
                    break;
                  }
                }
                if (At2 !== Ke2) {
                  for (var Rt2 = Ot2 + 1; Rt2 < B2.length; Rt2++) if (!(r3[B2[Rt2]] & s2)) {
                    d2(e50[B2[Rt2]]) & ne2 && (r3[B2[Rt2]] = At2);
                    break;
                  }
                }
              }
            }
            for (var zt2 = 0; zt2 < B2.length; zt2++) if (r3[B2[zt2]] & o2) {
              for (var Bt2 = zt2, Vt2 = zt2, Ht2 = We2, Ut2 = zt2 - 1; Ut2 >= 0; Ut2--) if (r3[B2[Ut2]] & s2) Bt2 = Ut2;
              else {
                Ht2 = r3[B2[Ut2]] & _t2 ? C2 : S2;
                break;
              }
              for (var Wt2 = Ge2, Gt2 = zt2 + 1; Gt2 < B2.length; Gt2++) if (r3[B2[Gt2]] & (o2 | s2)) Vt2 = Gt2;
              else {
                Wt2 = r3[B2[Gt2]] & _t2 ? C2 : S2;
                break;
              }
              for (var Kt2 = Bt2; Kt2 <= Vt2; Kt2++) r3[B2[Kt2]] = Ht2 === Wt2 ? Ht2 : Ke2;
              zt2 = Vt2;
            }
          }
        }
        for (var qt2 = g3.start; qt2 <= g3.end; qt2++) {
          var Jt2 = p3[qt2], Yt2 = r3[qt2];
          if (Jt2 & 1 ? Yt2 & (S2 | w2 | D2) && p3[qt2]++ : Yt2 & C2 ? p3[qt2]++ : Yt2 & (D2 | w2) && (p3[qt2] += 2), Yt2 & s2 && (p3[qt2] = qt2 === 0 ? g3.level : p3[qt2 - 1]), qt2 === g3.end || d2(e50[qt2]) & (k2 | O2)) for (var Xt2 = qt2; Xt2 >= 0 && d2(e50[Xt2]) & c2; Xt2--) p3[Xt2] = g3.level;
        }
      }
      return { levels: p3, paragraphs: h3 };
      function Zt2(t4, n4) {
        for (var a3 = t4; a3 < e50.length; a3++) {
          var o3 = r3[a3];
          if (o3 & (C2 | re2)) return 1;
          if (o3 & (O2 | S2) || n4 && o3 === fe2) return 0;
          if (o3 & i2) {
            var s3 = Qt2(a3);
            a3 = s3 === -1 ? e50.length : s3;
          }
        }
        return 0;
      }
      function Qt2(t4) {
        for (var n4 = 1, a3 = t4 + 1; a3 < e50.length; a3++) {
          var o3 = r3[a3];
          if (o3 & O2) break;
          if (o3 & fe2) {
            if (--n4 === 0) return a3;
          } else o3 & i2 && n4++;
        }
        return -1;
      }
    }
    var me2 = `14>1,j>2,t>2,u>2,1a>g,2v3>1,1>1,1ge>1,1wd>1,b>1,1j>1,f>1,ai>3,-2>3,+1,8>1k0,-1jq>1y7,-1y6>1hf,-1he>1h6,-1h5>1ha,-1h8>1qi,-1pu>1,6>3u,-3s>7,6>1,1>1,f>1,1>1,+2,3>1,1>1,+13,4>1,1>1,6>1eo,-1ee>1,3>1mg,-1me>1mk,-1mj>1mi,-1mg>1mi,-1md>1,1>1,+2,1>10k,-103>1,1>1,4>1,5>1,1>1,+10,3>1,1>8,-7>8,+1,-6>7,+1,a>1,1>1,u>1,u6>1,1>1,+5,26>1,1>1,2>1,2>2,8>1,7>1,4>1,1>1,+5,b8>1,1>1,+3,1>3,-2>1,2>1,1>1,+2,c>1,3>1,1>1,+2,h>1,3>1,a>1,1>1,2>1,3>1,1>1,d>1,f>1,3>1,1a>1,1>1,6>1,7>1,13>1,k>1,1>1,+19,4>1,1>1,+2,2>1,1>1,+18,m>1,a>1,1>1,lk>1,1>1,4>1,2>1,f>1,3>1,1>1,+3,db>1,1>1,+3,3>1,1>1,+2,14qm>1,1>1,+1,6>1,4j>1,j>2,t>2,u>2,2>1,+1`, he2;
    function ge2() {
      if (!he2) {
        var e50 = m2(me2, true), t3 = e50.map;
        e50.reverseMap.forEach(function(e51, n3) {
          t3.set(n3, e51);
        }), he2 = t3;
      }
    }
    function j2(e50) {
      return ge2(), he2.get(e50) || null;
    }
    function M2(e50, t3, n3, r3) {
      var i3 = e50.length;
      n3 = Math.max(0, n3 == null ? 0 : +n3), r3 = Math.min(i3 - 1, r3 == null ? i3 - 1 : +r3);
      for (var a3 = /* @__PURE__ */ new Map(), o3 = n3; o3 <= r3; o3++) if (t3[o3] & 1) {
        var s3 = j2(e50[o3]);
        s3 !== null && a3.set(o3, s3);
      }
      return a3;
    }
    function _e2(e50, t3, n3, r3) {
      var i3 = e50.length;
      n3 = Math.max(0, n3 == null ? 0 : +n3), r3 = Math.min(i3 - 1, r3 == null ? i3 - 1 : +r3);
      var a3 = [];
      return t3.paragraphs.forEach(function(i4) {
        var o3 = Math.max(n3, i4.start), s3 = Math.min(r3, i4.end);
        if (o3 < s3) {
          for (var l3 = t3.levels.slice(o3, s3 + 1), u3 = s3; u3 >= o3 && d2(e50[u3]) & c2; u3--) l3[u3] = i4.level;
          for (var f3 = i4.level, p3 = 1 / 0, m3 = 0; m3 < l3.length; m3++) {
            var h3 = l3[m3];
            h3 > f3 && (f3 = h3), h3 < p3 && (p3 = h3 | 1);
          }
          for (var g3 = f3; g3 >= p3; g3--) for (var _3 = 0; _3 < l3.length; _3++) if (l3[_3] >= g3) {
            for (var v3 = _3; _3 + 1 < l3.length && l3[_3 + 1] >= g3; ) _3++;
            _3 > v3 && a3.push([v3 + o3, _3 + o3]);
          }
        }
      }), a3;
    }
    function N2(e50, t3, n3, r3) {
      var i3 = P2(e50, t3, n3, r3), a3 = [].concat(e50);
      return i3.forEach(function(n4, r4) {
        a3[r4] = (t3.levels[n4] & 1 ? j2(e50[n4]) : null) || e50[n4];
      }), a3.join(``);
    }
    function P2(e50, t3, n3, r3) {
      for (var i3 = _e2(e50, t3, n3, r3), a3 = [], o3 = 0; o3 < e50.length; o3++) a3[o3] = o3;
      return i3.forEach(function(e51) {
        for (var t4 = e51[0], n4 = e51[1], r4 = a3.slice(t4, n4 + 1), i4 = r4.length; i4--; ) a3[n4 - i4] = r4[i4];
      }), a3;
    }
    return e49.closingToOpeningBracket = b2, e49.getBidiCharType = d2, e49.getBidiCharTypeName = f2, e49.getCanonicalBracket = x2, e49.getEmbeddingLevels = pe2, e49.getMirroredCharacter = j2, e49.getMirroredCharactersMap = M2, e49.getReorderSegments = _e2, e49.getReorderedIndices = P2, e49.getReorderedString = N2, e49.openingToClosingBracket = y2, Object.defineProperty(e49, "__esModule", { value: true }), e49;
  })({});
}
const Vm = Bm(), Hm = /[\u061c\u200e\u200f\u202a-\u202e\u2066-\u2069]/, Um = /* @__PURE__ */ new Set([`WS`, `FSI`, `LRI`, `RLI`, `PDI`]), Wm = /* @__PURE__ */ new Set([`S`, `B`]);
function Gm(e49, t2, n2, r2) {
  let i2 = [], a2 = n2;
  for (let o2 of vm(e49.slice(n2, r2))) i2.push({ index: a2, text: o2, level: t2[a2] }), a2 += o2.length;
  return i2;
}
function Km(e49, t2) {
  let n2 = true;
  for (let r2 = e49.length - 1; r2 >= 0; r2--) {
    let i2 = Vm.getBidiCharTypeName(e49[r2].text[0]);
    Wm.has(i2) ? (e49[r2].level = t2, n2 = true) : n2 && Um.has(i2) ? e49[r2].level = t2 : n2 = false;
  }
}
function qm(e49, t2) {
  let n2 = t2, r2 = 1 / 0;
  for (let { level: t3 } of e49) t3 > n2 && (n2 = t3), (t3 | 1) < r2 && (r2 = t3 | 1);
  let i2 = e49.slice();
  for (let e50 = n2; e50 >= r2; e50--) for (let t3 = 0; t3 < i2.length; t3++) {
    if (i2[t3].level < e50) continue;
    let n3 = t3;
    for (; n3 + 1 < i2.length && i2[n3 + 1].level >= e50; ) n3++;
    for (let e51 = t3, r3 = n3; e51 < r3; e51++, r3--) [i2[e51], i2[r3]] = [i2[r3], i2[e51]];
    t3 = n3;
  }
  return i2;
}
function Jm(e49) {
  return e49.level % 2 == 0 ? e49.text : Vm.getMirroredCharacter(e49.text) ?? e49.text;
}
function Ym(e49, t2) {
  var _a3;
  return ((_a3 = e49.find((e50) => t2 >= e50.start && t2 <= e50.end)) == null ? void 0 : _a3.level) ?? 0;
}
function Xm(e49, t2, n2, r2, i2) {
  let a2 = Gm(e49, t2, r2, i2);
  Km(a2, n2);
  let o2 = ``, s2 = [];
  for (let e50 of qm(a2, n2)) {
    if (Hm.test(e50.text)) continue;
    let t3 = Jm(e50);
    o2 += t3, s2.push(...Array(t3.length).fill(e50.index));
  }
  return { text: o2, sourceIndices: s2 };
}
function Zm(e49, t2) {
  let n2 = t2.filter((t3) => t3 > 0 && t3 < e49.length), r2 = [.../* @__PURE__ */ new Set([0, ...n2, e49.length])].sort((e50, t3) => e50 - t3), i2 = [];
  for (let e50 = 0; e50 + 1 < r2.length; e50++) i2.push([r2[e50], r2[e50 + 1]]);
  return i2;
}
function Qm(e49, t2) {
  let { levels: n2, paragraphs: r2 } = Vm.getEmbeddingLevels(e49);
  return Zm(e49, t2.concat(r2.map((e50) => e50.start))).map(([t3, i2]) => Xm(e49, n2, Ym(r2, t3), t3, i2));
}
function $m(e49, t2) {
  return Qm(e49, t2).map((e50) => e50.text);
}
function eh(e49, t2, n2) {
  return Qm(e49, n2).map((e50) => [e50.text, e50.sourceIndices.map((e51) => t2[e51] ?? 0)]);
}
const th = 4294967296, nh = 1 / th, rh = typeof TextDecoder > `u` ? null : new TextDecoder(`utf-8`);
var ih = class {
  constructor(e49) {
    this.buf = ArrayBuffer.isView(e49) ? e49 : new Uint8Array(e49), this.dataView = new DataView(this.buf.buffer, this.buf.byteOffset, this.buf.byteLength), this.pos = 0, this.type = 0, this._valueStart = -1, this.length = this.buf.length;
  }
  readFields(e49, t2, n2 = this.length) {
    let r2;
    for (; r2 = this.nextField(n2); ) e49(r2, t2, this);
    return t2;
  }
  readMessage(e49, t2) {
    return this.readFields(e49, t2, this.readVarint() + this.pos);
  }
  readFixed32() {
    let e49 = this.dataView.getUint32(this.pos, true);
    return this.pos += 4, e49;
  }
  readSFixed32() {
    let e49 = this.dataView.getInt32(this.pos, true);
    return this.pos += 4, e49;
  }
  readFixed64() {
    let e49 = this.dataView.getUint32(this.pos, true) + this.dataView.getUint32(this.pos + 4, true) * th;
    return this.pos += 8, e49;
  }
  readSFixed64() {
    let e49 = this.dataView.getUint32(this.pos, true) + this.dataView.getInt32(this.pos + 4, true) * th;
    return this.pos += 8, e49;
  }
  readFloat() {
    let e49 = this.dataView.getFloat32(this.pos, true);
    return this.pos += 4, e49;
  }
  readDouble() {
    let e49 = this.dataView.getFloat64(this.pos, true);
    return this.pos += 8, e49;
  }
  readVarint(e49) {
    let t2 = this.buf, n2 = t2[this.pos++];
    if (n2 < 128) return n2;
    let r2 = n2 & 127, i2;
    return i2 = t2[this.pos++], r2 |= (i2 & 127) << 7, i2 < 128 || (i2 = t2[this.pos++], r2 |= (i2 & 127) << 14, i2 < 128) || (i2 = t2[this.pos++], r2 |= (i2 & 127) << 21, i2 < 128) ? r2 : (i2 = t2[this.pos], r2 |= (i2 & 15) << 28, oh(r2, e49, this));
  }
  readSVarint() {
    let e49 = this.readVarint();
    return e49 % 2 == 1 ? (e49 + 1) / -2 : e49 / 2;
  }
  readBoolean() {
    return !!this.readVarint();
  }
  readString() {
    let e49 = this.readVarint() + this.pos, t2 = this.pos;
    return this.pos = e49, e49 - t2 >= 12 && rh ? rh.decode(this.buf.subarray(t2, e49)) : xh(this.buf, t2, e49);
  }
  readBytes() {
    let e49 = this.readVarint() + this.pos, t2 = this.buf.subarray(this.pos, e49);
    return this.pos = e49, t2;
  }
  readPackedVarint(e49 = [], t2) {
    let n2 = this.readPackedEnd();
    for (; this.pos < n2; ) e49.push(this.readVarint(t2));
    return e49;
  }
  readPackedSVarint(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readSVarint());
    return e49;
  }
  readPackedBoolean(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readBoolean());
    return e49;
  }
  readPackedFloat(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readFloat());
    return e49;
  }
  readPackedDouble(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readDouble());
    return e49;
  }
  readPackedFixed32(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readFixed32());
    return e49;
  }
  readPackedSFixed32(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readSFixed32());
    return e49;
  }
  readPackedFixed64(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readFixed64());
    return e49;
  }
  readPackedSFixed64(e49 = []) {
    let t2 = this.readPackedEnd();
    for (; this.pos < t2; ) e49.push(this.readSFixed64());
    return e49;
  }
  readPackedEnd() {
    return this.type === 2 ? this.readVarint() + this.pos : this.pos + 1;
  }
  nextField(e49 = this.length) {
    if (this.pos === this._valueStart && this.skip(this.type), this.pos >= e49) return 0;
    let t2 = this.readVarint();
    return this.type = t2 & 7, this._valueStart = this.pos, t2 >>> 3;
  }
  skip(e49) {
    let t2 = e49 & 7;
    if (t2 === 0) for (; this.buf[this.pos++] > 127; ) ;
    else if (t2 === 2) this.pos = this.readVarint() + this.pos;
    else if (t2 === 5) this.pos += 4;
    else if (t2 === 1) this.pos += 8;
    else throw Error(`Unimplemented type: ${t2}`);
  }
}, ah = class {
  constructor(e49 = /* @__PURE__ */ new Uint8Array(16)) {
    this.buf = ArrayBuffer.isView(e49) ? e49 : new Uint8Array(e49), this.dataView = new DataView(this.buf.buffer, this.buf.byteOffset, this.buf.byteLength), this.pos = 0, this.length = this.buf.length;
  }
  writeTag(e49, t2) {
    this.writeVarint(e49 << 3 | t2);
  }
  realloc(e49) {
    let t2 = this.length || 16;
    for (; t2 < this.pos + e49; ) t2 *= 2;
    if (t2 !== this.length) {
      let e50 = new Uint8Array(t2);
      e50.set(this.buf), this.buf = e50, this.dataView = new DataView(e50.buffer), this.length = t2;
    }
  }
  finish() {
    return this.length = this.pos, this.pos = 0, this.buf.subarray(0, this.length);
  }
  writeFixed32(e49) {
    this.realloc(4), this.dataView.setInt32(this.pos, e49, true), this.pos += 4;
  }
  writeSFixed32(e49) {
    this.realloc(4), this.dataView.setInt32(this.pos, e49, true), this.pos += 4;
  }
  writeFixed64(e49) {
    this.realloc(8), this.dataView.setInt32(this.pos, e49 & -1, true), this.dataView.setInt32(this.pos + 4, Math.floor(e49 * nh), true), this.pos += 8;
  }
  writeSFixed64(e49) {
    this.realloc(8), this.dataView.setInt32(this.pos, e49 & -1, true), this.dataView.setInt32(this.pos + 4, Math.floor(e49 * nh), true), this.pos += 8;
  }
  writeVarint(e49) {
    if (e49 = +e49 || 0, e49 >= 0 && e49 < 128) {
      this.pos >= this.length && this.realloc(1), this.buf[this.pos++] = e49;
      return;
    }
    if (e49 > 268435455 || e49 < 0) {
      ch(e49, this);
      return;
    }
    this.realloc(4), this.buf[this.pos++] = e49 & 127 | (e49 > 127 ? 128 : 0), !(e49 <= 127) && (this.buf[this.pos++] = (e49 >>>= 7) & 127 | (e49 > 127 ? 128 : 0), !(e49 <= 127) && (this.buf[this.pos++] = (e49 >>>= 7) & 127 | (e49 > 127 ? 128 : 0), !(e49 <= 127) && (this.buf[this.pos++] = e49 >>> 7 & 127)));
  }
  writeSVarint(e49) {
    this.writeVarint(e49 < 0 ? -e49 * 2 - 1 : e49 * 2);
  }
  writeBoolean(e49) {
    this.writeVarint(+e49);
  }
  writeString(e49) {
    e49 = String(e49), this.realloc(e49.length * 4), this.pos++;
    let t2 = this.pos;
    this.pos = Sh(this.buf, e49, this.pos);
    let n2 = this.pos - t2;
    n2 >= 128 && dh(t2, n2, this), this.pos = t2 - 1, this.writeVarint(n2), this.pos += n2;
  }
  writeFloat(e49) {
    this.realloc(4), this.dataView.setFloat32(this.pos, e49, true), this.pos += 4;
  }
  writeDouble(e49) {
    this.realloc(8), this.dataView.setFloat64(this.pos, e49, true), this.pos += 8;
  }
  writeBytes(e49) {
    let t2 = e49.length;
    this.writeVarint(t2), this.realloc(t2), this.buf.set(e49, this.pos), this.pos += t2;
  }
  writeRawMessage(e49, t2) {
    this.pos++;
    let n2 = this.pos;
    e49(t2, this);
    let r2 = this.pos - n2;
    r2 >= 128 && dh(n2, r2, this), this.pos = n2 - 1, this.writeVarint(r2), this.pos += r2;
  }
  writeMessage(e49, t2, n2) {
    this.writeTag(e49, 2), this.writeRawMessage(t2, n2);
  }
  writePackedVarint(e49, t2) {
    t2.length && this.writeMessage(e49, fh, t2);
  }
  writePackedSVarint(e49, t2) {
    t2.length && this.writeMessage(e49, ph, t2);
  }
  writePackedBoolean(e49, t2) {
    t2.length && this.writeMessage(e49, gh, t2);
  }
  writePackedFloat(e49, t2) {
    t2.length && this.writeMessage(e49, mh, t2);
  }
  writePackedDouble(e49, t2) {
    t2.length && this.writeMessage(e49, hh, t2);
  }
  writePackedFixed32(e49, t2) {
    t2.length && this.writeMessage(e49, _h, t2);
  }
  writePackedSFixed32(e49, t2) {
    t2.length && this.writeMessage(e49, vh, t2);
  }
  writePackedFixed64(e49, t2) {
    t2.length && this.writeMessage(e49, yh, t2);
  }
  writePackedSFixed64(e49, t2) {
    t2.length && this.writeMessage(e49, bh, t2);
  }
  writeBytesField(e49, t2) {
    this.writeTag(e49, 2), this.writeBytes(t2);
  }
  writeFixed32Field(e49, t2) {
    this.writeTag(e49, 5), this.writeFixed32(t2);
  }
  writeSFixed32Field(e49, t2) {
    this.writeTag(e49, 5), this.writeSFixed32(t2);
  }
  writeFixed64Field(e49, t2) {
    this.writeTag(e49, 1), this.writeFixed64(t2);
  }
  writeSFixed64Field(e49, t2) {
    this.writeTag(e49, 1), this.writeSFixed64(t2);
  }
  writeVarintField(e49, t2) {
    this.writeTag(e49, 0), this.writeVarint(t2);
  }
  writeSVarintField(e49, t2) {
    this.writeTag(e49, 0), this.writeSVarint(t2);
  }
  writeStringField(e49, t2) {
    this.writeTag(e49, 2), this.writeString(t2);
  }
  writeFloatField(e49, t2) {
    this.writeTag(e49, 5), this.writeFloat(t2);
  }
  writeDoubleField(e49, t2) {
    this.writeTag(e49, 1), this.writeDouble(t2);
  }
  writeBooleanField(e49, t2) {
    this.writeVarintField(e49, +t2);
  }
};
function oh(e49, t2, n2) {
  let r2 = n2.buf, i2, a2;
  if (a2 = r2[n2.pos++], i2 = (a2 & 112) >> 4, a2 < 128 || (a2 = r2[n2.pos++], i2 |= (a2 & 127) << 3, a2 < 128) || (a2 = r2[n2.pos++], i2 |= (a2 & 127) << 10, a2 < 128) || (a2 = r2[n2.pos++], i2 |= (a2 & 127) << 17, a2 < 128) || (a2 = r2[n2.pos++], i2 |= (a2 & 127) << 24, a2 < 128) || (a2 = r2[n2.pos++], i2 |= (a2 & 1) << 31, a2 < 128)) return sh(e49, i2, t2);
  throw Error(`Expected varint not more than 10 bytes`);
}
function sh(e49, t2, n2) {
  return n2 ? t2 * 4294967296 + (e49 >>> 0) : (t2 >>> 0) * 4294967296 + (e49 >>> 0);
}
function ch(e49, t2) {
  let n2, r2;
  if (e49 >= 0 ? (n2 = e49 % 4294967296 | 0, r2 = e49 / 4294967296 | 0) : (n2 = ~(-e49 % 4294967296), r2 = ~(-e49 / 4294967296), n2 ^ 4294967295 ? n2 = n2 + 1 | 0 : (n2 = 0, r2 = r2 + 1 | 0)), e49 >= 18446744073709552e3 || e49 < -18446744073709552e3) throw Error(`Given varint doesn't fit into 10 bytes`);
  t2.realloc(10), lh(n2, r2, t2), uh(r2, t2);
}
function lh(e49, t2, n2) {
  n2.buf[n2.pos++] = e49 & 127 | 128, e49 >>>= 7, n2.buf[n2.pos++] = e49 & 127 | 128, e49 >>>= 7, n2.buf[n2.pos++] = e49 & 127 | 128, e49 >>>= 7, n2.buf[n2.pos++] = e49 & 127 | 128, e49 >>>= 7, n2.buf[n2.pos] = e49 & 127;
}
function uh(e49, t2) {
  let n2 = (e49 & 7) << 4;
  t2.buf[t2.pos++] |= n2 | ((e49 >>>= 3) ? 128 : 0), e49 && (t2.buf[t2.pos++] = e49 & 127 | ((e49 >>>= 7) ? 128 : 0), e49 && (t2.buf[t2.pos++] = e49 & 127 | ((e49 >>>= 7) ? 128 : 0), e49 && (t2.buf[t2.pos++] = e49 & 127 | ((e49 >>>= 7) ? 128 : 0), e49 && (t2.buf[t2.pos++] = e49 & 127 | ((e49 >>>= 7) ? 128 : 0), e49 && (t2.buf[t2.pos++] = e49 & 127)))));
}
function dh(e49, t2, n2) {
  let r2 = t2 <= 16383 ? 1 : t2 <= 2097151 ? 2 : t2 <= 268435455 ? 3 : Math.floor(Math.log(t2) / (Math.LN2 * 7));
  n2.realloc(r2), n2.buf.copyWithin(e49 + r2, e49, n2.pos);
}
function fh(e49, t2) {
  let n2 = e49.length, r2 = t2.buf, i2 = t2.pos, a2 = t2.length;
  for (let o2 = 0; o2 < n2; o2++) {
    let n3 = e49[o2];
    if (n3 < 0 || i2 + 10 > a2) {
      t2.pos = i2, t2.writeVarint(n3), r2 = t2.buf, i2 = t2.pos, a2 = t2.length;
      continue;
    }
    for (; n3 > 127; ) r2[i2++] = n3 % 128 | 128, n3 = Math.floor(n3 / 128);
    r2[i2++] = n3;
  }
  t2.pos = i2;
}
function ph(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeSVarint(e49[n2]);
}
function mh(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeFloat(e49[n2]);
}
function hh(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeDouble(e49[n2]);
}
function gh(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeBoolean(e49[n2]);
}
function _h(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeFixed32(e49[n2]);
}
function vh(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeSFixed32(e49[n2]);
}
function yh(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeFixed64(e49[n2]);
}
function bh(e49, t2) {
  for (let n2 = 0; n2 < e49.length; n2++) t2.writeSFixed64(e49[n2]);
}
function xh(e49, t2, n2) {
  let r2 = ``, i2 = t2;
  for (; i2 < n2; ) {
    let t3 = e49[i2], a2 = null, o2 = t3 > 239 ? 4 : t3 > 223 ? 3 : t3 > 191 ? 2 : 1;
    if (i2 + o2 > n2) break;
    let s2, c2, l2;
    o2 === 1 ? t3 < 128 && (a2 = t3) : o2 === 2 ? (s2 = e49[i2 + 1], (s2 & 192) == 128 && (a2 = (t3 & 31) << 6 | s2 & 63, a2 <= 127 && (a2 = null))) : o2 === 3 ? (s2 = e49[i2 + 1], c2 = e49[i2 + 2], (s2 & 192) == 128 && (c2 & 192) == 128 && (a2 = (t3 & 15) << 12 | (s2 & 63) << 6 | c2 & 63, (a2 <= 2047 || a2 >= 55296 && a2 <= 57343) && (a2 = null))) : o2 === 4 && (s2 = e49[i2 + 1], c2 = e49[i2 + 2], l2 = e49[i2 + 3], (s2 & 192) == 128 && (c2 & 192) == 128 && (l2 & 192) == 128 && (a2 = (t3 & 15) << 18 | (s2 & 63) << 12 | (c2 & 63) << 6 | l2 & 63, (a2 <= 65535 || a2 >= 1114112) && (a2 = null))), a2 === null ? (a2 = 65533, o2 = 1) : a2 > 65535 && (a2 -= 65536, r2 += String.fromCharCode(a2 >>> 10 & 1023 | 55296), a2 = 56320 | a2 & 1023), r2 += String.fromCharCode(a2), i2 += o2;
  }
  return r2;
}
function Sh(e49, t2, n2) {
  for (let r2 = 0, i2, a2; r2 < t2.length; r2++) {
    if (i2 = t2.charCodeAt(r2), i2 > 55295 && i2 < 57344) {
      if (a2) {
        if (i2 < 56320) {
          e49[n2++] = 239, e49[n2++] = 191, e49[n2++] = 189, a2 = i2;
          continue;
        }
        i2 = a2 - 55296 << 10 | i2 - 56320 | 65536, a2 = null;
      } else {
        i2 > 56319 || r2 + 1 === t2.length ? (e49[n2++] = 239, e49[n2++] = 191, e49[n2++] = 189) : a2 = i2;
        continue;
      }
    } else a2 &&= (e49[n2++] = 239, e49[n2++] = 191, e49[n2++] = 189, null);
    i2 < 128 ? e49[n2++] = i2 : (i2 < 2048 ? e49[n2++] = i2 >> 6 | 192 : (i2 < 65536 ? e49[n2++] = i2 >> 12 | 224 : (e49[n2++] = i2 >> 18 | 240, e49[n2++] = i2 >> 12 & 63 | 128), e49[n2++] = i2 >> 6 & 63 | 128), e49[n2++] = i2 & 63 | 128);
  }
  return n2;
}
function Ch(e49) {
  return typeof (e49 == null ? void 0 : e49.renderWithWebGL) == `function`;
}
function wh(e49) {
  let t2 = 0, n2 = 0;
  for (let r3 of e49) t2 += r3.w * r3.h, n2 = Math.max(n2, r3.w);
  e49.sort((e50, t3) => t3.h - e50.h);
  let r2 = [{ x: 0, y: 0, w: Math.max(Math.ceil(Math.sqrt(t2 / 0.95)), n2), h: 1 / 0 }], i2 = 0, a2 = 0;
  for (let t3 of e49) for (let e50 = r2.length - 1; e50 >= 0; e50--) {
    let n3 = r2[e50];
    if (!(t3.w > n3.w || t3.h > n3.h)) {
      if (t3.x = n3.x, t3.y = n3.y, a2 = Math.max(a2, t3.y + t3.h), i2 = Math.max(i2, t3.x + t3.w), t3.w === n3.w && t3.h === n3.h) {
        let t4 = r2.pop();
        t4 && e50 < r2.length && (r2[e50] = t4);
      } else t3.h === n3.h ? (n3.x += t3.w, n3.w -= t3.w) : t3.w === n3.w ? (n3.y += t3.h, n3.h -= t3.h) : (r2.push({ x: n3.x + t3.w, y: n3.y, w: n3.w - t3.w, h: t3.h }), n3.y += t3.h, n3.h -= t3.h);
      break;
    }
  }
  return { w: i2, h: a2, fill: t2 / (i2 * a2) || 0 };
}
var Th = class {
  constructor(e49, { pixelRatio: t2, version: n2, isWebGLImage: r2 = false, stretchX: i2, stretchY: a2, content: o2, textFitWidth: s2, textFitHeight: c2 }) {
    this.paddedRect = e49, this.pixelRatio = t2, this.stretchX = i2, this.stretchY = a2, this.content = o2, this.version = n2, this.needsFirstWebGLRender = r2, this.textFitWidth = s2, this.textFitHeight = c2;
  }
  get tl() {
    return [this.paddedRect.x + 1, this.paddedRect.y + 1];
  }
  get br() {
    return [this.paddedRect.x + this.paddedRect.w - 1, this.paddedRect.y + this.paddedRect.h - 1];
  }
  get tlbr() {
    return this.tl.concat(this.br);
  }
  get displaySize() {
    return [(this.paddedRect.w - 2) / this.pixelRatio, (this.paddedRect.h - 2) / this.pixelRatio];
  }
}, Eh = class {
  constructor(e49, t2) {
    let n2 = {}, r2 = {};
    this.haveRenderCallbacks = [], this.patchedUpdateVersion = -1;
    let i2 = [];
    this.addImages(e49, n2, i2), this.addImages(t2, r2, i2);
    let { w: a2, h: o2 } = wh(i2), s2 = new ul({ width: a2 || 1, height: o2 || 1 });
    for (let t3 in e49) {
      let r3 = e49[t3];
      if (r3.isWebGLImage) continue;
      let i3 = n2[t3].paddedRect;
      ul.copy(r3.data, s2, { x: 0, y: 0 }, { x: i3.x + 1, y: i3.y + 1 }, r3.data);
    }
    for (let e50 in t2) {
      let n3 = t2[e50], i3 = r2[e50].paddedRect, a3 = i3.x + 1, o3 = i3.y + 1, c2 = n3.data.width, l2 = n3.data.height;
      ul.copy(n3.data, s2, { x: 0, y: 0 }, { x: a3, y: o3 }, n3.data), ul.copy(n3.data, s2, { x: 0, y: l2 - 1 }, { x: a3, y: o3 - 1 }, { width: c2, height: 1 }), ul.copy(n3.data, s2, { x: 0, y: 0 }, { x: a3, y: o3 + l2 }, { width: c2, height: 1 }), ul.copy(n3.data, s2, { x: c2 - 1, y: 0 }, { x: a3 - 1, y: o3 }, { width: 1, height: l2 }), ul.copy(n3.data, s2, { x: 0, y: 0 }, { x: a3 + c2, y: o3 }, { width: 1, height: l2 });
    }
    this.image = s2, this.iconPositions = n2, this.patternPositions = r2;
  }
  addImages(e49, t2, n2) {
    for (let r2 in e49) {
      let i2 = e49[r2], a2 = { x: 0, y: 0, w: i2.data.width + 2, h: i2.data.height + 2 };
      n2.push(a2), t2[r2] = new Th(a2, i2), i2.hasRenderCallback && this.haveRenderCallbacks.push(r2);
    }
  }
  patchUpdatedImages(e49, t2) {
    if (e49.dispatchRenderCallbacks(this.haveRenderCallbacks), this.patchedUpdateVersion !== e49.updateVersion) {
      this.patchedUpdateVersion = e49.updateVersion;
      for (let n2 in this.iconPositions) this.patchUpdatedImage(this.iconPositions[n2], e49.getImage(n2), t2);
      for (let n2 in this.patternPositions) this.patchUpdatedImage(this.patternPositions[n2], e49.getImage(n2), t2);
    }
  }
  patchUpdatedImage(e49, t2, n2) {
    var _a3;
    if (!e49 || !t2 || !e49.needsFirstWebGLRender && e49.version === t2.version) return;
    e49.needsFirstWebGLRender = false, e49.version = t2.version;
    let [r2, i2] = e49.tl, a2 = (_a3 = t2.userImage) == null ? void 0 : _a3.data;
    if (!Ch(a2)) {
      n2.update(t2.data, void 0, { x: r2, y: i2 });
      return;
    }
    let { width: o2, height: s2 } = t2.data;
    n2.context.setCustomLayerDefaults(), a2.renderWithWebGL({ gl: n2.context.gl, texture: n2.texture, x: r2, y: i2, width: o2, height: s2 }), n2.context.setDirty();
  }
};
G(`ImagePosition`, Th), G(`ImageAtlas`, Eh);
var Dh = /* @__PURE__ */ (function(e49) {
  return e49[e49.none = 0] = `none`, e49[e49.horizontal = 1] = `horizontal`, e49[e49.vertical = 2] = `vertical`, e49[e49.horizontalOnly = 3] = `horizontalOnly`, e49;
})(Dh || {});
function Oh(e49) {
  for (let t2 of e49) if (t2.positionedGlyphs.length !== 0) return false;
  return true;
}
function kh(e49, t2) {
  let n2 = [], r2 = 0;
  for (let i2 of t2) n2.push(e49.substring(r2, i2)), r2 = i2;
  return r2 < e49.length() && n2.push(e49.substring(r2, e49.length())), n2;
}
const Ah = new RegExp("^\\p{gc=M}$", "u");
function jh(e49) {
  let t2 = [], n2 = 0;
  for (; n2 < e49.length; ) {
    if (!Ah.test(e49[n2])) {
      t2.push(n2), n2++;
      continue;
    }
    let r2 = n2;
    for (; r2 < e49.length && Ah.test(e49[r2]); ) r2++;
    let i2 = e49[r2];
    if (i2 !== void 0 && Dm(i2.codePointAt(0))) {
      t2.push(r2);
      for (let e50 = r2 - 1; e50 >= n2; e50--) t2.push(e50);
      n2 = r2 + 1;
    } else {
      for (let e50 = n2; e50 < r2; e50++) t2.push(e50);
      n2 = r2;
    }
  }
  return t2;
}
function Mh(e49) {
  let t2 = [], n2 = 0;
  for (let r2 of e49.graphemes()) t2.push(...Array(r2.length).fill(e49.sectionIndex[n2])), n2++;
  return t2;
}
function Nh(e49, t2) {
  let n2 = t2.map((t3) => e49.toCodeUnitIndex(t3)), r2 = Ip.isParsed() ? Ip : null;
  if (e49.sections.length === 1) return r2 ? r2.processBidirectionalText(e49.toString(), n2).map((t3) => Ih(t3, e49.sections, [...t3].map(() => 0))) : $m(e49.toString(), n2).map((t3) => Ph([...t3], e49.sections, [...t3].map(() => 0)));
  let i2 = Mh(e49);
  return r2 ? r2.processStyledBidirectionalText(e49.text, i2, n2).map(([t3, n3]) => Ih(t3, e49.sections, n3)) : eh(e49.text, i2, n2).map(([t3, n3]) => {
    let r3 = [...t3];
    return Ph(r3, e49.sections, Fh(r3).map((e50) => n3[e50] ?? 0));
  });
}
function Ph(e49, t2, n2) {
  let r2 = new zm(e49.join(``), t2, []), i2 = 0;
  for (let e50 of r2.graphemes()) r2.sectionIndex.push(n2[i2] ?? 0), i2 += [...e50].length;
  return r2;
}
function Fh(e49) {
  let t2 = [], n2 = 0;
  for (let r2 of e49) t2.push(n2), n2 += r2.length;
  return t2;
}
function Ih(e49, t2, n2) {
  let r2 = [...e49], i2 = Fh(r2), a2 = jh(r2);
  return Ph(a2.map((e50) => r2[e50]), t2, a2.map((e50) => n2[i2[e50]] ?? 0));
}
function Lh(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2) {
  let h2 = zm.fromFeature(e49, i2);
  d2 === 2 && h2.verticalizePunctuation();
  let g2 = h2.determineLineBreaks(l2, a2, t2, r2, p2), _2 = Om(h2.text) ? Nh(h2, g2) : kh(h2, g2), v2 = [], y2 = { positionedLines: v2, text: h2.toString(), top: u2[1], bottom: u2[1], left: u2[0], right: u2[0], writingMode: d2, iconsInText: false, verticalizable: false };
  return Xh(y2, t2, n2, r2, _2, o2, s2, c2, d2, l2, f2, m2), !Oh(v2) && y2;
}
function Rh(e49) {
  let t2 = 0.5, n2 = 0.5;
  switch (e49) {
    case `right`:
    case `top-right`:
    case `bottom-right`:
      t2 = 1;
      break;
    case `left`:
    case `top-left`:
    case `bottom-left`:
      t2 = 0;
  }
  switch (e49) {
    case `bottom`:
    case `bottom-right`:
    case `bottom-left`:
      n2 = 1;
      break;
    case `top`:
    case `top-right`:
    case `top-left`:
      n2 = 0;
  }
  return { horizontalAlign: t2, verticalAlign: n2 };
}
function zh(e49, t2, n2) {
  let r2 = t2.getMaxScale() * 24, { maxImageWidth: i2, maxImageHeight: a2 } = t2.getMaxImageSize(e49), o2 = Math.max(r2, a2 * n2);
  return { verticalLineContentWidth: Math.max(r2, i2 * n2), horizontalLineContentHeight: o2 };
}
function Bh(e49) {
  switch (e49) {
    case `top`:
      return 0;
    case `center`:
      return 0.5;
    default:
      return 1;
  }
}
function Vh(e49, t2, n2, r2) {
  var _a3, _b3;
  if (e49 == null ? void 0 : e49.rect) return e49;
  let i2 = (_b3 = (_a3 = t2[n2.fontStack]) == null ? void 0 : _a3.default) == null ? void 0 : _b3[r2];
  return i2 ? { rect: null, metrics: i2.metrics } : null;
}
function Hh(e49, t2, n2) {
  return !(e49 === 1 || !t2 && !Bp(n2) || t2 && (xm(n2) || Em(n2)));
}
function Uh(e49) {
  return new RegExp("\\p{Nd}", "u").test(String.fromCodePoint(e49));
}
function Wh(e49) {
  return new RegExp("\\p{Lu}", "u").test(String.fromCodePoint(e49));
}
function Gh(e49) {
  return /[\p{P}\p{S}]/u.test(String.fromCodePoint(e49));
}
function Kh(e49) {
  let t2 = e49.some(Uh) && e49.every((e50) => Uh(e50) || Gh(e50)), n2 = e49.length <= 3 && e49.every((e50) => Wh(e50) || Uh(e50));
  return t2 || n2;
}
function qh(e49) {
  return (Uh(e49) || Wh(e49)) && !Em(e49);
}
function Jh(e49, t2) {
  let n2 = false;
  for (let r2 = 0; r2 < e49.length; r2++) {
    if (t2[r2]) continue;
    let i2 = km[e49[r2]];
    i2 && (r2 === 0 || t2[r2 - 1]) && (r2 === e49.length - 1 || t2[r2 + 1]) && (e49[r2] = i2, t2[r2] = true, n2 = true);
  }
  return n2;
}
function Yh(e49) {
  let t2 = e49.graphemes().slice(), n2 = t2.map((e50) => e50.codePointAt(0)), r2 = n2.map(Bp), i2 = (t3) => !r2[t3] && !xm(n2[t3]) && !(`imageName` in e49.getSection(t3));
  for (let e50 = 0; e50 < n2.length; e50++) {
    if (!i2(e50)) continue;
    let t3 = e50;
    for (; t3 + 1 < n2.length && i2(t3 + 1); ) t3++;
    if (Kh(n2.slice(e50, t3 + 1))) for (let i3 = e50; i3 <= t3; i3++) r2[i3] = qh(n2[i3]);
    e50 = t3;
  }
  return Jh(t2, r2) && (e49.text = t2.join(``), e49._graphemes = null), r2;
}
function Xh(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2) {
  var _a3, _b3;
  let f2 = 0, p2 = 0, m2 = 0, h2 = 0, g2 = s2 === `right` ? 1 : s2 === `left` ? 0 : 0.5, _2 = 24 / d2, v2 = 0;
  for (let o3 of i2) {
    o3.trim();
    let i3 = o3.getMaxScale(), s3 = { positionedGlyphs: [], lineOffset: 0 };
    e49.positionedLines[v2] = s3;
    let d3 = s3.positionedGlyphs, y3 = 0;
    if (!o3.length()) {
      p2 += a2, ++v2;
      continue;
    }
    let b3 = zh(r2, o3, _2), x2 = c2 === 2 && !u2 ? Yh(o3) : null, S2 = o3.graphemes();
    for (let a3 = 0; a3 < S2.length; a3++) {
      let s4 = o3.getSection(a3), m3 = S2[a3], h3 = m3.codePointAt(0), g3 = x2 ? x2[a3] : Hh(c2, u2, h3), v3 = `fontStack` in s4 && bm(m3) && !((_b3 = (_a3 = t2[s4.fontStack]) == null ? void 0 : _a3.default) == null ? void 0 : _b3[m3]) ? [...m3] : [m3];
      for (let c3 of v3) {
        let u3 = { glyph: c3.codePointAt(0), grapheme: c3, imageName: null, x: f2, y: p2 + -17, vertical: g3, scale: 1, fontStack: ``, sectionIndex: o3.getSectionIndex(a3), metrics: null, rect: null }, m4;
        if (`fontStack` in s4) {
          if (m4 = Zh(s4, c3, g3, b3, t2, n2), !m4) continue;
          u3.fontStack = s4.fontStack;
        } else {
          if (e49.iconsInText = true, s4.scale *= _2, m4 = Qh(s4, g3, i3, b3, r2), !m4) continue;
          y3 = Math.max(y3, m4.imageOffset), u3.imageName = s4.imageName;
        }
        let { rect: h4, metrics: v4, baselineOffset: x3 } = m4;
        if (u3.y += x3, u3.scale = s4.scale, u3.metrics = v4, u3.rect = h4, d3.push(u3), !g3) f2 += v4.advance * s4.scale + l2;
        else {
          e49.verticalizable = true;
          let t3 = `imageName` in s4 ? v4.advance : 24;
          f2 += t3 * s4.scale + l2;
        }
      }
    }
    if (d3.length !== 0) {
      let e50 = f2 - l2;
      m2 = Math.max(e50, m2), $h(d3, 0, d3.length - 1, g2);
    }
    f2 = 0;
    let C2 = (i3 - 1) * 24;
    s3.lineOffset = Math.max(y3, C2);
    let w2 = a2 * i3 + y3;
    p2 += w2, h2 = Math.max(w2, h2), ++v2;
  }
  let { horizontalAlign: y2, verticalAlign: b2 } = Rh(o2);
  eg(e49.positionedLines, g2, y2, b2, m2, h2, a2, p2, i2.length), e49.top += -b2 * p2, e49.bottom = e49.top + p2, e49.left += -y2 * m2, e49.right = e49.left + m2;
}
function Zh(e49, t2, n2, r2, i2, a2) {
  var _a3, _b3;
  let o2 = (_b3 = (_a3 = a2[e49.fontStack]) == null ? void 0 : _a3.default) == null ? void 0 : _b3[t2], s2 = Vh(o2, i2, e49, t2);
  if (s2 === null) return null;
  let c2;
  if (n2) c2 = r2.verticalLineContentWidth - e49.scale * 24;
  else {
    let t3 = Bh(e49.verticalAlign);
    c2 = (r2.horizontalLineContentHeight - e49.scale * 24) * t3;
  }
  return { rect: s2.rect, metrics: s2.metrics, baselineOffset: c2 };
}
function Qh(e49, t2, n2, r2, i2) {
  let a2 = i2[e49.imageName];
  if (!a2) return null;
  let o2 = a2.paddedRect, s2 = a2.displaySize, c2 = { width: s2[0], height: s2[1], left: 1, top: -3, advance: t2 ? s2[1] : s2[0] }, l2;
  if (t2) l2 = r2.verticalLineContentWidth - s2[1] * e49.scale;
  else {
    let t3 = Bh(e49.verticalAlign);
    l2 = (r2.horizontalLineContentHeight - s2[1] * e49.scale) * t3;
  }
  let u2 = (t2 ? s2[0] : s2[1]) * e49.scale - 24 * n2;
  return { rect: o2, metrics: c2, baselineOffset: l2, imageOffset: u2 };
}
function $h(e49, t2, n2, r2) {
  if (r2 === 0) return;
  let i2 = e49[n2], a2 = i2.metrics.advance * i2.scale, o2 = (e49[n2].x + a2) * r2;
  for (let r3 = t2; r3 <= n2; r3++) e49[r3].x -= o2;
}
function eg(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
  let l2 = (t2 - n2) * i2, u2 = 0;
  u2 = a2 === o2 ? -r2 * c2 * o2 + 0.5 * o2 : -s2 * r2 - -17;
  for (let t3 of e49) for (let e50 of t3.positionedGlyphs) e50.x += l2, e50.y += u2;
}
function tg(e49, t2, n2) {
  let { horizontalAlign: r2, verticalAlign: i2 } = Rh(n2), a2 = t2[0], o2 = t2[1], s2 = a2 - e49.displaySize[0] * r2, c2 = s2 + e49.displaySize[0], l2 = o2 - e49.displaySize[1] * i2;
  return { image: e49, top: l2, bottom: l2 + e49.displaySize[1], left: s2, right: c2 };
}
function ng(e49) {
  let t2 = e49.left, n2 = e49.top, r2 = e49.right - t2, i2 = e49.bottom - n2, a2 = e49.image.content[2] - e49.image.content[0], o2 = e49.image.content[3] - e49.image.content[1], s2 = e49.image.textFitWidth ?? `stretchOrShrink`, c2 = e49.image.textFitHeight ?? `stretchOrShrink`, l2 = a2 / o2;
  if (c2 === `proportional`) {
    if (s2 === `stretchOnly` && r2 / i2 < l2 || s2 === `proportional`) {
      let e50 = Math.ceil(i2 * l2);
      t2 *= e50 / r2, r2 = e50;
    }
  } else if (s2 === `proportional` && c2 === `stretchOnly` && l2 !== 0 && r2 / i2 > l2) {
    let e50 = Math.ceil(r2 / l2);
    n2 *= e50 / i2, i2 = e50;
  }
  return { x1: t2, y1: n2, x2: t2 + r2, y2: n2 + i2 };
}
function rg(e49, t2, n2, r2, i2, a2) {
  let o2 = e49.image, s2;
  if (o2.content) {
    let e50 = o2.content, t3 = o2.pixelRatio || 1;
    s2 = [e50[0] / t3, e50[1] / t3, o2.displaySize[0] - e50[2] / t3, o2.displaySize[1] - e50[3] / t3];
  }
  let c2 = t2.left * a2, l2 = t2.right * a2, u2, d2, f2, p2;
  n2 === `width` || n2 === `both` ? (p2 = i2[0] + c2 - r2[3], d2 = i2[0] + l2 + r2[1]) : (p2 = i2[0] + (c2 + l2 - o2.displaySize[0]) / 2, d2 = p2 + o2.displaySize[0]);
  let m2 = t2.top * a2, h2 = t2.bottom * a2;
  return n2 === `height` || n2 === `both` ? (u2 = i2[1] + m2 - r2[0], f2 = i2[1] + h2 + r2[2]) : (u2 = i2[1] + (m2 + h2 - o2.displaySize[1]) / 2, f2 = u2 + o2.displaySize[1]), { image: o2, top: u2, right: d2, bottom: f2, left: p2, collisionPadding: s2 };
}
const ig = 32640;
function ag(e49, t2) {
  let { expression: n2 } = t2;
  if (n2.kind === `constant`) return { kind: `constant`, layoutSize: n2.evaluate(new K(e49 + 1)) };
  if (n2.kind === `source`) return { kind: `source` };
  if (n2.kind === `composite`) {
    let { minZoom: t3, maxZoom: r2 } = og(n2.zoomStops, e49);
    return { kind: `composite`, minZoom: t3, maxZoom: r2, interpolationType: n2.interpolationType };
  }
  {
    let t3 = sg(n2), r2 = n2.evaluate(new K(e49 + 1));
    return { kind: `camera`, zoomStops: n2.zoomStops, sizes: t3, layoutSize: r2, interpolationType: n2.interpolationType };
  }
}
function og(e49, t2) {
  let n2 = 0;
  for (; n2 < e49.length && e49[n2] <= t2; ) n2++;
  n2 = Math.max(0, n2 - 1);
  let r2 = n2;
  for (; r2 < e49.length && e49[r2] < t2 + 1; ) r2++;
  return r2 = Math.min(e49.length - 1, r2), { minZoom: e49[n2], maxZoom: e49[r2] };
}
function sg(e49) {
  return e49.zoomStops.map((t2) => e49.evaluate(new K(t2 === -1 / 0 ? e49.zoomStops[1] - 1 : t2)));
}
var cg = class e46 extends l {
  constructor(e49, t2, n2, r2) {
    super(e49, t2), this.angle = n2, r2 !== void 0 && (this.segment = r2);
  }
  clone() {
    return new e46(this.x, this.y, this.angle, this.segment);
  }
};
G(`Anchor`, cg);
function lg(e49, t2, n2, r2, i2) {
  if (t2.segment === void 0 || n2 === 0) return true;
  let a2 = t2, o2 = t2.segment + 1, s2 = 0;
  for (; s2 > -n2 / 2; ) {
    if (o2--, o2 < 0) return false;
    s2 -= e49[o2].dist(a2), a2 = e49[o2];
  }
  s2 += e49[o2].dist(e49[o2 + 1]), o2++;
  let c2 = [], l2 = 0;
  for (; s2 < n2 / 2; ) {
    let t3 = e49[o2 - 1], n3 = e49[o2], a3 = e49[o2 + 1];
    if (!a3) return false;
    let u2 = t3.angleTo(n3) - n3.angleTo(a3);
    for (u2 = Math.abs((u2 + 3 * Math.PI) % (Math.PI * 2) - Math.PI), c2.push({ distance: s2, angleDelta: u2 }), l2 += u2; s2 - c2[0].distance > r2; ) l2 -= c2.shift().angleDelta;
    if (l2 > i2) return false;
    o2++, s2 += n3.dist(a3);
  }
  return true;
}
function ug(e49) {
  let t2 = 0;
  for (let n2 = 0; n2 < e49.length - 1; n2++) t2 += e49[n2].dist(e49[n2 + 1]);
  return t2;
}
function dg(e49, t2, n2) {
  return e49 ? 3 / 5 * t2 * n2 : 0;
}
function fg(e49, t2) {
  return Math.max(e49 ? e49.right - e49.left : 0, t2 ? t2.right - t2.left : 0);
}
function pg(e49, t2, n2, r2, i2, a2) {
  let o2 = dg(n2, i2, a2), s2 = fg(n2, r2) * a2, c2 = 0, l2 = ug(e49) / 2;
  for (let n3 = 0; n3 < e49.length - 1; n3++) {
    let r3 = e49[n3], i3 = e49[n3 + 1], a3 = r3.dist(i3);
    if (c2 + a3 > l2) {
      let u2 = (l2 - c2) / a3, d2 = new cg(At.number(r3.x, i3.x, u2), At.number(r3.y, i3.y, u2), i3.angleTo(r3), n3);
      return d2._round(), !o2 || lg(e49, d2, s2, o2, t2) ? d2 : void 0;
    }
    c2 += a3;
  }
}
function mg(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
  let l2 = dg(r2, a2, o2), u2 = fg(r2, i2), d2 = u2 * o2, f2 = e49[0].x === 0 || e49[0].x === c2 || e49[0].y === 0 || e49[0].y === c2;
  t2 - d2 < t2 / 4 && (t2 = d2 + t2 / 4);
  let p2 = a2 * 2;
  return hg(e49, f2 ? t2 / 2 * s2 % t2 : (u2 / 2 + p2) * o2 * s2 % t2, t2, l2, n2, d2, f2, false, c2);
}
function hg(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
  let l2 = a2 / 2, u2 = ug(e49), d2 = 0, f2 = t2 - n2, p2 = [];
  for (let t3 = 0; t3 < e49.length - 1; t3++) {
    let o3 = e49[t3], s3 = e49[t3 + 1], m2 = o3.dist(s3), h2 = s3.angleTo(o3);
    for (; f2 + n2 < d2 + m2; ) {
      f2 += n2;
      let g2 = (f2 - d2) / m2, _2 = At.number(o3.x, s3.x, g2), v2 = At.number(o3.y, s3.y, g2);
      if (_2 >= 0 && _2 < c2 && v2 >= 0 && v2 < c2 && f2 - l2 >= 0 && f2 + l2 <= u2) {
        let n3 = new cg(_2, v2, h2, t3);
        n3._round(), (!r2 || lg(e49, n3, a2, r2, i2)) && p2.push(n3);
      }
    }
    d2 += m2;
  }
  return !s2 && !p2.length && !o2 && (p2 = hg(e49, d2 / 2, n2, r2, i2, a2, o2, true, c2)), p2;
}
function gg(e49, t2, n2, r2, i2) {
  let a2 = [];
  for (let o2 of e49) {
    let e50;
    for (let s2 = 0; s2 < o2.length - 1; s2++) {
      let c2 = o2[s2], u2 = o2[s2 + 1];
      c2.x < t2 && u2.x < t2 || (c2.x < t2 ? c2 = new l(t2, c2.y + (u2.y - c2.y) * ((t2 - c2.x) / (u2.x - c2.x)))._round() : u2.x < t2 && (u2 = new l(t2, c2.y + (u2.y - c2.y) * ((t2 - c2.x) / (u2.x - c2.x)))._round()), !(c2.y < n2 && u2.y < n2) && (c2.y < n2 ? c2 = new l(c2.x + (u2.x - c2.x) * ((n2 - c2.y) / (u2.y - c2.y)), n2)._round() : u2.y < n2 && (u2 = new l(c2.x + (u2.x - c2.x) * ((n2 - c2.y) / (u2.y - c2.y)), n2)._round()), !(c2.x >= r2 && u2.x >= r2) && (c2.x >= r2 ? c2 = new l(r2, c2.y + (u2.y - c2.y) * ((r2 - c2.x) / (u2.x - c2.x)))._round() : u2.x >= r2 && (u2 = new l(r2, c2.y + (u2.y - c2.y) * ((r2 - c2.x) / (u2.x - c2.x)))._round()), !(c2.y >= i2 && u2.y >= i2) && (c2.y >= i2 ? c2 = new l(c2.x + (u2.x - c2.x) * ((i2 - c2.y) / (u2.y - c2.y)), i2)._round() : u2.y >= i2 && (u2 = new l(c2.x + (u2.x - c2.x) * ((i2 - c2.y) / (u2.y - c2.y)), i2)._round()), (!e50 || !c2.equals(e50[e50.length - 1])) && (e50 = [c2], a2.push(e50)), e50.push(u2)))));
    }
  }
  return a2;
}
function _g(e49, t2, n2, r2, i2, a2) {
  let o2 = vg(e49, t2, n2, i2, 0);
  return o2 = vg(o2, t2, r2, a2, 1), o2;
}
function vg(e49, t2, n2, r2, i2) {
  switch (t2) {
    case 1:
      return yg(e49, n2, r2, i2);
    case 2:
      return xg(e49, n2, r2, i2, false);
    case 3:
      return xg(e49, n2, r2, i2, true);
  }
  return [];
}
function yg(e49, t2, n2, r2) {
  let i2 = [];
  for (let a2 of e49) for (let e50 of a2) {
    let a3 = r2 === 0 ? e50.x : e50.y;
    a3 >= t2 && a3 <= n2 && i2.push([e50]);
  }
  return i2;
}
function bg(e49, t2, n2, r2, i2) {
  let a2 = r2 === 0 ? Sg : Cg, o2 = [], s2 = [];
  for (let c3 = 0; c3 < e49.length - 1; c3++) {
    let l2 = e49[c3], u3 = e49[c3 + 1], d2 = r2 === 0 ? l2.x : l2.y, f2 = r2 === 0 ? u3.x : u3.y, p2 = false;
    d2 < t2 ? f2 > t2 && o2.push(a2(l2, u3, t2)) : d2 > n2 ? f2 < n2 && o2.push(a2(l2, u3, n2)) : o2.push(l2), f2 < t2 && d2 >= t2 && (o2.push(a2(l2, u3, t2)), p2 = true), f2 > n2 && d2 <= n2 && (o2.push(a2(l2, u3, n2)), p2 = true), !i2 && p2 && (s2.push(o2), o2 = []);
  }
  let c2 = e49.length - 1, u2 = r2 === 0 ? e49[c2].x : e49[c2].y;
  return u2 >= t2 && u2 <= n2 && o2.push(e49[c2]), i2 && o2.length > 0 && !o2[0].equals(o2[o2.length - 1]) && o2.push(new l(o2[0].x, o2[0].y)), o2.length > 0 && s2.push(o2), s2;
}
function xg(e49, t2, n2, r2, i2) {
  let a2 = [];
  for (let o2 of e49) {
    let e50 = bg(o2, t2, n2, r2, i2);
    e50.length > 0 && a2.push(...e50);
  }
  return a2;
}
function Sg(e49, t2, n2) {
  let r2 = (n2 - e49.x) / (t2.x - e49.x);
  return new l(n2, e49.y + (t2.y - e49.y) * r2);
}
function Cg(e49, t2, n2) {
  let r2 = (n2 - e49.y) / (t2.y - e49.y);
  return new l(e49.x + (t2.x - e49.x) * r2, n2);
}
function wg(e49, t2, n2, r2) {
  let i2 = [], a2 = e49.image, o2 = a2.pixelRatio, s2 = a2.paddedRect.w - 2, c2 = a2.paddedRect.h - 2, u2 = { x1: e49.left, y1: e49.top, x2: e49.right, y2: e49.bottom }, d2 = a2.stretchX || [[0, s2]], f2 = a2.stretchY || [[0, c2]], p2 = (e50, t3) => e50 + t3[1] - t3[0], m2 = d2.reduce(p2, 0), h2 = f2.reduce(p2, 0), g2 = s2 - m2, _2 = c2 - h2, v2 = 0, y2 = m2, b2 = 0, x2 = h2, S2 = 0, C2 = g2, w2 = 0, T2 = _2;
  if (a2.content && r2) {
    let t3 = a2.content, n3 = t3[2] - t3[0], r3 = t3[3] - t3[1];
    (a2.textFitWidth || a2.textFitHeight) && (u2 = ng(e49)), v2 = Tg(d2, 0, t3[0]), b2 = Tg(f2, 0, t3[1]), y2 = Tg(d2, t3[0], t3[2]), x2 = Tg(f2, t3[1], t3[3]), S2 = t3[0] - v2, w2 = t3[1] - b2, C2 = n3 - y2, T2 = r3 - x2;
  }
  let E2 = u2.x1, D2 = u2.y1, ee2 = u2.x2 - E2, O2 = u2.y2 - D2, k2 = (e50, r3, i3, s3) => {
    let c3 = Dg(e50.stretch - v2, y2, ee2, E2), u3 = Og(e50.fixed - S2, C2, e50.stretch, m2), d3 = Dg(r3.stretch - b2, x2, O2, D2), f3 = Og(r3.fixed - w2, T2, r3.stretch, h2), p3 = Dg(i3.stretch - v2, y2, ee2, E2), g3 = Og(i3.fixed - S2, C2, i3.stretch, m2), _3 = Dg(s3.stretch - b2, x2, O2, D2), k3 = Og(s3.fixed - w2, T2, s3.stretch, h2), A2 = new l(c3, d3), te2 = new l(p3, d3), ne2 = new l(p3, _3), re2 = new l(c3, _3), ie2 = new l(u3 / o2, f3 / o2), ae2 = new l(g3 / o2, k3 / o2), oe2 = t2 * Math.PI / 180;
    if (oe2) {
      let e51 = Math.sin(oe2), t3 = Math.cos(oe2), n3 = [t3, -e51, e51, t3];
      A2._matMult(n3), te2._matMult(n3), re2._matMult(n3), ne2._matMult(n3);
    }
    let se2 = e50.stretch + e50.fixed, ce2 = i3.stretch + i3.fixed, le2 = r3.stretch + r3.fixed, ue2 = s3.stretch + s3.fixed;
    return { tl: A2, tr: te2, bl: re2, br: ne2, tex: { x: a2.paddedRect.x + 1 + se2, y: a2.paddedRect.y + 1 + le2, w: ce2 - se2, h: ue2 - le2 }, writingMode: void 0, glyphOffset: [0, 0], sectionIndex: 0, pixelOffsetTL: ie2, pixelOffsetBR: ae2, minFontScaleX: C2 / o2 / ee2, minFontScaleY: T2 / o2 / O2, isSDF: n2 };
  };
  if (!r2 || !a2.stretchX && !a2.stretchY) i2.push(k2({ fixed: 0, stretch: -1 }, { fixed: 0, stretch: -1 }, { fixed: 0, stretch: s2 + 1 }, { fixed: 0, stretch: c2 + 1 }));
  else {
    let e50 = Eg(d2, g2, m2), t3 = Eg(f2, _2, h2);
    for (let n3 = 0; n3 < e50.length - 1; n3++) {
      let r3 = e50[n3], a3 = e50[n3 + 1];
      for (let e51 = 0; e51 < t3.length - 1; e51++) {
        let n4 = t3[e51], o3 = t3[e51 + 1];
        i2.push(k2(r3, n4, a3, o3));
      }
    }
  }
  return i2;
}
function Tg(e49, t2, n2) {
  let r2 = 0;
  for (let i2 of e49) r2 += Math.max(t2, Math.min(n2, i2[1])) - Math.max(t2, Math.min(n2, i2[0]));
  return r2;
}
function Eg(e49, t2, n2) {
  let r2 = [{ fixed: -1, stretch: 0 }];
  for (let [t3, n3] of e49) {
    let e50 = r2[r2.length - 1];
    r2.push({ fixed: t3 - e50.stretch, stretch: e50.stretch }), r2.push({ fixed: t3 - e50.stretch, stretch: e50.stretch + (n3 - t3) });
  }
  return r2.push({ fixed: t2 + 1, stretch: n2 }), r2;
}
function Dg(e49, t2, n2, r2) {
  return e49 / t2 * n2 + r2;
}
function Og(e49, t2, n2, r2) {
  return e49 - t2 * n2 / r2;
}
function kg(e49, t2, n2, r2, i2, a2, o2, s2) {
  let c2 = r2.layout.get(`text-rotate`).evaluate(a2, {}) * Math.PI / 180, u2 = [];
  for (let e50 of t2.positionedLines) for (let r3 of e50.positionedGlyphs) {
    if (!r3.rect) continue;
    let a3 = r3.rect || {}, d2 = 4, f2 = true, p2 = 1, m2 = 0, h2 = (i2 || s2) && r3.vertical, g2 = r3.metrics.advance * r3.scale / 2;
    if (s2 && t2.verticalizable) {
      let t3 = (r3.scale - 1) * 24, n3 = (24 - r3.metrics.width * r3.scale) / 2;
      m2 = e50.lineOffset / 2 - (r3.imageName ? -n3 : t3);
    }
    if (r3.imageName) {
      let e51 = o2[r3.imageName];
      f2 = e51.sdf, p2 = e51.pixelRatio, d2 = 1 / p2;
    }
    let _2 = i2 ? [r3.x + g2, r3.y] : [0, 0], v2 = i2 ? [0, 0] : [r3.x + g2 + n2[0], r3.y + n2[1] - m2], y2 = [0, 0];
    h2 && (y2 = v2, v2 = [0, 0]);
    let b2 = r3.metrics.isDoubleResolution ? 2 : 1, x2 = (r3.metrics.left - d2) * r3.scale - g2 + v2[0], S2 = (-r3.metrics.top - d2) * r3.scale + v2[1], C2 = x2 + a3.w / b2 * r3.scale / p2, w2 = S2 + a3.h / b2 * r3.scale / p2, T2 = new l(x2, S2), E2 = new l(C2, S2), D2 = new l(x2, w2), ee2 = new l(C2, w2);
    if (h2) {
      let e51 = new l(-g2, g2 - -17), t3 = -Math.PI / 2, n3 = 12 - g2, i3 = r3.imageName ? n3 : 0, a4 = new l(22 - n3, -i3), o3 = new l(...y2);
      T2._rotateAround(t3, e51)._add(a4)._add(o3), E2._rotateAround(t3, e51)._add(a4)._add(o3), D2._rotateAround(t3, e51)._add(a4)._add(o3), ee2._rotateAround(t3, e51)._add(a4)._add(o3);
    }
    if (c2) {
      let e51 = Math.sin(c2), t3 = Math.cos(c2), n3 = [t3, -e51, e51, t3];
      T2._matMult(n3), E2._matMult(n3), D2._matMult(n3), ee2._matMult(n3);
    }
    let O2 = new l(0, 0), k2 = new l(0, 0);
    u2.push({ tl: T2, tr: E2, bl: D2, br: ee2, tex: a3, writingMode: t2.writingMode, glyphOffset: _2, sectionIndex: r3.sectionIndex, isSDF: f2, pixelOffsetTL: O2, pixelOffsetBR: k2, minFontScaleX: 0, minFontScaleY: 0 });
  }
  return u2;
}
var Ag = class {
  constructor(e49, t2, n2, r2, i2, a2, o2, s2, c2, u2) {
    var _a3;
    if (this.boxStartIndex = e49.length, c2) {
      let e50 = a2.top, t3 = a2.bottom, n3 = a2.collisionPadding;
      n3 && (e50 -= n3[1], t3 += n3[3]);
      let r3 = t3 - e50;
      r3 > 0 && (r3 = Math.max(10, r3), this.circleDiameter = r3);
    } else {
      let c3 = ((_a3 = a2.image) == null ? void 0 : _a3.content) && (a2.image.textFitWidth || a2.image.textFitHeight) ? ng(a2) : { x1: a2.left, y1: a2.top, x2: a2.right, y2: a2.bottom };
      c3.y1 = c3.y1 * o2 - s2[0], c3.y2 = c3.y2 * o2 + s2[2], c3.x1 = c3.x1 * o2 - s2[3], c3.x2 = c3.x2 * o2 + s2[1];
      let d2 = a2.collisionPadding;
      if (d2 && (c3.x1 -= d2[0] * o2, c3.y1 -= d2[1] * o2, c3.x2 += d2[2] * o2, c3.y2 += d2[3] * o2), u2) {
        let e50 = new l(c3.x1, c3.y1), t3 = new l(c3.x2, c3.y1), n3 = new l(c3.x1, c3.y2), r3 = new l(c3.x2, c3.y2), i3 = u2 * Math.PI / 180;
        e50._rotate(i3), t3._rotate(i3), n3._rotate(i3), r3._rotate(i3), c3.x1 = Math.min(e50.x, t3.x, n3.x, r3.x), c3.x2 = Math.max(e50.x, t3.x, n3.x, r3.x), c3.y1 = Math.min(e50.y, t3.y, n3.y, r3.y), c3.y2 = Math.max(e50.y, t3.y, n3.y, r3.y);
      }
      e49.emplaceBack(t2.x, t2.y, c3.x1, c3.y1, c3.x2, c3.y2, n2, r2, i2);
    }
    this.boxEndIndex = e49.length;
  }
}, jg = class {
  constructor(e49 = [], t2 = (e50, t3) => e50 < t3 ? -1 : +(e50 > t3)) {
    if (this.data = e49, this.length = this.data.length, this.compare = t2, this.length > 0) for (let e50 = (this.length >> 1) - 1; e50 >= 0; e50--) this._down(e50);
  }
  push(e49) {
    this.data.push(e49), this._up(this.length++);
  }
  pop() {
    if (this.length === 0) return;
    let e49 = this.data[0], t2 = this.data.pop();
    return --this.length > 0 && (this.data[0] = t2, this._down(0)), e49;
  }
  peek() {
    return this.data[0];
  }
  _up(e49) {
    let { data: t2, compare: n2 } = this, r2 = t2[e49];
    for (; e49 > 0; ) {
      let i2 = e49 - 1 >> 1, a2 = t2[i2];
      if (n2(r2, a2) >= 0) break;
      t2[e49] = a2, e49 = i2;
    }
    t2[e49] = r2;
  }
  _down(e49) {
    let { data: t2, compare: n2 } = this, r2 = this.length >> 1, i2 = t2[e49];
    for (; e49 < r2; ) {
      let r3 = (e49 << 1) + 1, a2 = r3 + 1;
      if (a2 < this.length && n2(t2[a2], t2[r3]) < 0 && (r3 = a2), n2(t2[r3], i2) >= 0) break;
      t2[e49] = t2[r3], e49 = r3;
    }
    t2[e49] = i2;
  }
};
function Mg(e49, t2 = 1) {
  let n2 = Zu.fromPoints(e49[0]), r2 = Math.min(n2.width(), n2.height()), i2 = r2 / 2, a2 = new jg([], Ng), { minX: o2, minY: s2, maxX: c2, maxY: u2 } = n2;
  if (r2 === 0) return new l(o2, s2);
  for (let t3 = o2; t3 < c2; t3 += r2) for (let n3 = s2; n3 < u2; n3 += r2) a2.push(new Pg(t3 + i2, n3 + i2, i2, e49));
  let d2 = Ig(e49), f2 = d2;
  for (; a2.length; ) {
    let n3 = a2.pop();
    (n3.d > f2.d || !f2.d) && (f2 = n3), !(n3.max - f2.d <= t2) && (i2 = n3.h / 2, a2.push(new Pg(n3.p.x - i2, n3.p.y - i2, i2, e49)), a2.push(new Pg(n3.p.x + i2, n3.p.y - i2, i2, e49)), a2.push(new Pg(n3.p.x - i2, n3.p.y + i2, i2, e49)), a2.push(new Pg(n3.p.x + i2, n3.p.y + i2, i2, e49)));
  }
  return d2.d > 0 && f2.d - d2.d <= t2 ? d2.p : f2.p;
}
function Ng(e49, t2) {
  return t2.max - e49.max;
}
var Pg = class {
  constructor(e49, t2, n2, r2) {
    this.p = new l(e49, t2), this.h = n2, this.d = Fg(this.p, r2), this.max = this.d + this.h * Math.SQRT2;
  }
};
function Fg(e49, t2) {
  let n2 = false, r2 = 1 / 0;
  for (let i2 of t2) for (let t3 = 0, a2 = i2.length, o2 = a2 - 1; t3 < a2; o2 = t3++) {
    let a3 = i2[t3], s2 = i2[o2];
    a3.y > e49.y != s2.y > e49.y && e49.x < (s2.x - a3.x) * (e49.y - a3.y) / (s2.y - a3.y) + a3.x && (n2 = !n2), r2 = Math.min(r2, Nc(e49, a3, s2));
  }
  return (n2 ? 1 : -1) * Math.sqrt(r2);
}
function Ig(e49) {
  let t2 = 0, n2 = 0, r2 = 0, i2 = e49[0];
  for (let e50 = 0, a2 = i2.length, o2 = a2 - 1; e50 < a2; o2 = e50++) {
    let a3 = i2[e50], s2 = i2[o2], c2 = a3.x * s2.y - s2.x * a3.y;
    n2 += (a3.x + s2.x) * c2, r2 += (a3.y + s2.y) * c2, t2 += c2 * 3;
  }
  return new Pg(n2 / t2, r2 / t2, 0, e49);
}
let Lg = /* @__PURE__ */ (function(e49) {
  return e49[e49.center = 1] = `center`, e49[e49.left = 2] = `left`, e49[e49.right = 3] = `right`, e49[e49.top = 4] = `top`, e49[e49.bottom = 5] = `bottom`, e49[e49[`top-left`] = 6] = `top-left`, e49[e49[`top-right`] = 7] = `top-right`, e49[e49[`bottom-left`] = 8] = `bottom-left`, e49[e49[`bottom-right`] = 9] = `bottom-right`, e49;
})({});
const Rg = 1 / 0;
function zg(e49, t2) {
  function n2(e50, t3) {
    let n3 = 0, r3 = 0;
    t3 < 0 && (t3 = 0);
    let i2 = t3 / Math.SQRT2;
    switch (e50) {
      case `top-right`:
      case `top-left`:
        r3 = i2 - 7;
        break;
      case `bottom-right`:
      case `bottom-left`:
        r3 = -i2 + 7;
        break;
      case `bottom`:
        r3 = -t3 + 7;
        break;
      case `top`:
        r3 = t3 - 7;
    }
    switch (e50) {
      case `top-right`:
      case `bottom-right`:
        n3 = -i2;
        break;
      case `top-left`:
      case `bottom-left`:
        n3 = i2;
        break;
      case `left`:
        n3 = t3;
        break;
      case `right`:
        n3 = -t3;
    }
    return [n3, r3];
  }
  function r2(e50, t3, n3) {
    let r3 = 0, i2 = 0;
    switch (t3 = Math.abs(t3), n3 = Math.abs(n3), e50) {
      case `top-right`:
      case `top-left`:
      case `top`:
        i2 = n3 - 7;
        break;
      case `bottom-right`:
      case `bottom-left`:
      case `bottom`:
        i2 = -n3 + 7;
    }
    switch (e50) {
      case `top-right`:
      case `bottom-right`:
      case `right`:
        r3 = -t3;
        break;
      case `top-left`:
      case `bottom-left`:
      case `left`:
        r3 = t3;
    }
    return [r3, i2];
  }
  return t2[1] === Rg ? n2(e49, t2[0]) : r2(e49, t2[0], t2[1]);
}
function Bg(e49, t2, n2) {
  var _a3;
  let r2 = e49.layout, i2 = (_a3 = r2.get(`text-variable-anchor-offset`)) == null ? void 0 : _a3.evaluate(t2, {}, n2);
  if (i2) {
    let e50 = i2.values, t3 = [];
    for (let n3 = 0; n3 < e50.length; n3 += 2) {
      let r3 = t3[n3] = e50[n3], i3 = e50[n3 + 1].map((e51) => e51 * 24);
      r3.startsWith(`top`) ? i3[1] -= 7 : r3.startsWith(`bottom`) && (i3[1] += 7), t3[n3 + 1] = i3;
    }
    return new ht(t3);
  }
  let a2 = r2.get(`text-variable-anchor`);
  if (a2) {
    let i3;
    i3 = e49._unevaluatedLayout.getValue(`text-radial-offset`) === void 0 ? r2.get(`text-offset`).evaluate(t2, {}, n2).map((e50) => e50 * 24) : [r2.get(`text-radial-offset`).evaluate(t2, {}, n2) * 24, Rg];
    let o2 = [];
    for (let e50 of a2) o2.push(e50, zg(e50, i3));
    return new ht(o2);
  }
  return null;
}
function Vg(e49) {
  var _a3;
  e49.bucket.createArrays();
  let t2 = 512 * e49.bucket.overscaling;
  e49.bucket.tilePixelRatio = v / t2, e49.bucket.compareText = {}, e49.bucket.iconsNeedLinear = false;
  let n2 = e49.bucket.layers[0], r2 = n2.layout, i2 = n2._unevaluatedLayout._values, a2 = { layoutIconSize: i2[`icon-size`].possiblyEvaluate(new K(e49.bucket.zoom + 1), e49.canonical), layoutTextSize: i2[`text-size`].possiblyEvaluate(new K(e49.bucket.zoom + 1), e49.canonical), textMaxSize: i2[`text-size`].possiblyEvaluate(new K(18)) };
  if (e49.bucket.textSizeData.kind === `composite`) {
    let { minZoom: t3, maxZoom: n3 } = e49.bucket.textSizeData;
    a2.compositeTextSizes = [i2[`text-size`].possiblyEvaluate(new K(t3), e49.canonical), i2[`text-size`].possiblyEvaluate(new K(n3), e49.canonical)];
  }
  if (e49.bucket.iconSizeData.kind === `composite`) {
    let { minZoom: t3, maxZoom: n3 } = e49.bucket.iconSizeData;
    a2.compositeIconSizes = [i2[`icon-size`].possiblyEvaluate(new K(t3), e49.canonical), i2[`icon-size`].possiblyEvaluate(new K(n3), e49.canonical)];
  }
  let o2 = r2.get(`text-line-height`) * 24, s2 = r2.get(`text-rotation-alignment`) !== `viewport` && r2.get(`symbol-placement`) !== `point`, c2 = r2.get(`text-keep-upright`), l2 = r2.get(`text-size`);
  for (let t3 of e49.bucket.features) {
    let i3 = r2.get(`text-font`).evaluate(t3, {}, e49.canonical).join(`,`), u2 = l2.evaluate(t3, {}, e49.canonical), d2 = a2.layoutTextSize.evaluate(t3, {}, e49.canonical), f2 = a2.layoutIconSize.evaluate(t3, {}, e49.canonical), p2 = { horizontal: {}, vertical: void 0 }, m2 = t3.text, h2 = [0, 0];
    if (m2) {
      let a3 = m2.toString(), l3 = r2.get(`text-letter-spacing`).evaluate(t3, {}, e49.canonical) * 24, f3 = Cm(a3) ? l3 : 0, g3 = r2.get(`text-anchor`).evaluate(t3, {}, e49.canonical), _3 = Bg(n2, t3, e49.canonical);
      if (!_3) {
        let n3 = r2.get(`text-radial-offset`).evaluate(t3, {}, e49.canonical);
        h2 = n3 ? zg(g3, [n3 * 24, Rg]) : r2.get(`text-offset`).evaluate(t3, {}, e49.canonical).map((e50) => e50 * 24);
      }
      let v3 = s2 ? `center` : r2.get(`text-justify`).evaluate(t3, {}, e49.canonical), y2 = r2.get(`symbol-placement`) === `point` ? r2.get(`text-max-width`).evaluate(t3, {}, e49.canonical) * 24 : 1 / 0, b2 = () => {
        e49.bucket.allowVerticalPlacement && Sm(a3) && (p2.vertical = Lh(m2, e49.glyphMap, e49.glyphPositions, e49.imagePositions, i3, y2, o2, g3, `left`, f3, h2, 2, true, d2, u2));
      };
      if (!s2 && _3) {
        let t4 = /* @__PURE__ */ new Set();
        if (v3 === `auto`) for (let e50 = 0; e50 < _3.values.length; e50 += 2) t4.add(Hg(_3.values[e50]));
        else t4.add(v3);
        let n3 = false;
        for (let r3 of t4) if (!p2.horizontal[r3]) {
          if (n3) p2.horizontal[r3] = p2.horizontal[0];
          else {
            let t5 = Lh(m2, e49.glyphMap, e49.glyphPositions, e49.imagePositions, i3, y2, o2, `center`, r3, f3, h2, 1, false, d2, u2);
            t5 && (p2.horizontal[r3] = t5, n3 = t5.positionedLines.length === 1);
          }
        }
        b2();
      } else {
        v3 === `auto` && (v3 = Hg(g3));
        let t4 = Lh(m2, e49.glyphMap, e49.glyphPositions, e49.imagePositions, i3, y2, o2, g3, v3, f3, h2, 1, false, d2, u2);
        t4 && (p2.horizontal[v3] = t4), b2(), Sm(a3) && s2 && c2 && (p2.vertical = Lh(m2, e49.glyphMap, e49.glyphPositions, e49.imagePositions, i3, y2, o2, g3, v3, f3, h2, 2, false, d2, u2));
      }
    }
    let g2, _2 = false;
    if ((_a3 = t3.icon) == null ? void 0 : _a3.name) {
      let n3 = e49.imageMap[t3.icon.name];
      n3 && (g2 = tg(e49.imagePositions[t3.icon.name], r2.get(`icon-offset`).evaluate(t3, {}, e49.canonical), r2.get(`icon-anchor`).evaluate(t3, {}, e49.canonical)), _2 = !!n3.sdf, e49.bucket.sdfIcons === void 0 ? e49.bucket.sdfIcons = _2 : e49.bucket.sdfIcons !== _2 && A(`Style sheet warning: Cannot mix SDF and non-SDF icons in one buffer`), n3.pixelRatio === e49.bucket.pixelRatio ? r2.get(`icon-rotate`).constantOr(1) !== 0 && (e49.bucket.iconsNeedLinear = true) : e49.bucket.iconsNeedLinear = true);
    }
    let v2 = Kg(p2.horizontal) || p2.vertical;
    e49.bucket.iconsInText ||= v2 ? v2.iconsInText : false, (v2 || g2) && Ug(e49.bucket, t3, p2, g2, e49.imageMap, a2, d2, f2, h2, _2, e49.canonical, e49.subdivisionGranularity, e49.hasPromoteId);
  }
  e49.showCollisionBoxes && e49.bucket.generateCollisionDebugBuffers();
}
function Hg(e49) {
  switch (e49) {
    case `right`:
    case `top-right`:
    case `bottom-right`:
      return `right`;
    case `left`:
    case `top-left`:
    case `bottom-left`:
      return `left`;
  }
  return `center`;
}
function Ug(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2) {
  let p2 = a2.textMaxSize.evaluate(t2, {});
  p2 === void 0 && (p2 = o2);
  let m2 = e49.layers[0].layout, h2 = m2.get(`icon-offset`).evaluate(t2, {}, u2), g2 = Kg(n2.horizontal), _2 = o2 / 24, y2 = e49.tilePixelRatio * _2, b2 = e49.tilePixelRatio * p2 / 24, x2 = e49.tilePixelRatio * s2, S2 = e49.tilePixelRatio * m2.get(`symbol-spacing`), C2 = m2.get(`text-padding`) * e49.tilePixelRatio, w2 = f_(m2, t2, u2, e49.tilePixelRatio), T2 = m2.get(`text-max-angle`) / 180 * Math.PI, E2 = m2.get(`text-rotation-alignment`) !== `viewport` && m2.get(`symbol-placement`) !== `point`, D2 = m2.get(`icon-rotation-alignment`).constantOr(`viewport`) === `map` && m2.get(`symbol-placement`) !== `point`, ee2 = m2.get(`symbol-placement`), O2 = S2 / 2, k2 = m2.get(`icon-text-fit`), A2;
  r2 && k2 !== `none` && (e49.allowVerticalPlacement && n2.vertical && (A2 = rg(r2, n2.vertical, k2, m2.get(`icon-text-fit-padding`), h2, _2)), g2 && (r2 = rg(r2, g2, k2, m2.get(`icon-text-fit-padding`), h2, _2)));
  let te2 = u2 ? d2.line.getGranularityForZoomLevel(u2.z) : 1, ne2 = (s3, d3) => {
    d3.x < 0 || d3.x >= 8192 || d3.y < 0 || d3.y >= 8192 || qg(e49, d3, s3, n2, r2, i2, A2, e49.layers[0], e49.collisionBoxArray, t2.index, t2.sourceLayerIndex, e49.index, y2, [C2, C2, C2, C2], E2, c2, x2, w2, D2, h2, t2, a2, l2, u2, o2, f2);
  };
  if (ee2 === `line`) for (let i3 of gg(t2.geometry, 0, 0, v, v)) {
    let t3 = ju(i3, te2), a3 = mg(t3, S2, T2, n2.vertical || g2, r2, 24, b2, e49.overscaling, v);
    for (let n3 of a3) {
      let r3 = g2;
      (!r3 || !Jg(e49, r3.text, O2, n3)) && ne2(t3, n3);
    }
  }
  else if (ee2 === `line-center`) {
    for (let e50 of t2.geometry) if (e50.length > 1) {
      let t3 = ju(e50, te2), i3 = pg(t3, T2, n2.vertical || g2, r2, 24, b2);
      i3 && ne2(t3, i3);
    }
  } else if (t2.type === `Polygon`) for (let e50 of Ft(t2.geometry, 0)) {
    let t3 = Mg(e50, 16);
    ne2(ju(e50[0], te2, true), new cg(t3.x, t3.y, 0));
  }
  else if (t2.type === `LineString`) for (let e50 of t2.geometry) {
    let t3 = ju(e50, te2);
    ne2(t3, new cg(t3[0].x, t3[0].y, 0));
  }
  else if (t2.type === `Point`) for (let e50 of t2.geometry) for (let t3 of e50) ne2([t3], new cg(t3.x, t3.y, 0));
}
function Wg(e49, t2) {
  let n2 = e49.length, r2 = t2 == null ? void 0 : t2.values;
  if ((r2 == null ? void 0 : r2.length) > 0) for (let t3 = 0; t3 < r2.length; t3 += 2) {
    let n3 = Lg[r2[t3]], i2 = r2[t3 + 1];
    e49.emplaceBack(n3, i2[0], i2[1]);
  }
  return [n2, e49.length];
}
function Gg(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2) {
  let g2 = kg(t2, n2, s2, i2, a2, o2, r2, e49.allowVerticalPlacement), _2 = e49.textSizeData, v2 = null;
  _2.kind === `source` ? (v2 = [128 * i2.layout.get(`text-size`).evaluate(o2, {})], v2[0] > 32640 && A(`${e49.layerIds[0]}: Value for "text-size" is >= 255. Reduce your "text-size".`)) : _2.kind === `composite` && (v2 = [128 * m2.compositeTextSizes[0].evaluate(o2, {}, h2), 128 * m2.compositeTextSizes[1].evaluate(o2, {}, h2)], (v2[0] > 32640 || v2[1] > 32640) && A(`${e49.layerIds[0]}: Value for "text-size" is >= 255. Reduce your "text-size".`)), e49.addSymbols(e49.text, g2, v2, s2, a2, o2, u2, t2, l2.lineStartIndex, l2.lineLength, p2, h2, c2);
  for (let t3 of d2) f2[t3] = e49.text.placedSymbolArray.length - 1;
  return g2.length * 4;
}
function Kg(e49) {
  for (let t2 in e49) return e49[t2];
  return null;
}
function qg(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2, m2, h2, g2, _2, v2, y2, b2, x2, S2, C2, w2, T2) {
  let E2 = e49.addToLineVertexArray(t2, n2), D2 = s2.layout.get(`symbol-height-offset`).evaluate(b2, {}, C2);
  D2 > e49.maxHeightOffset && (e49.maxHeightOffset = D2);
  let ee2, O2, k2, te2, ne2 = 0, re2 = 0, ie2 = 0, ae2 = 0, oe2 = -1, se2 = -1, ce2 = {}, le2 = (0, Xs.default)(``);
  if (e49.allowVerticalPlacement && r2.vertical) {
    let e50 = s2.layout.get(`text-rotate`).evaluate(b2, {}, C2) + 90, n3 = r2.vertical;
    k2 = new Ag(c2, t2, l2, u2, d2, n3, f2, p2, m2, e50), o2 && (te2 = new Ag(c2, t2, l2, u2, d2, o2, g2, _2, m2, e50));
  }
  if (i2) {
    let n3 = s2.layout.get(`icon-rotate`).evaluate(b2, {}), r3 = s2.layout.get(`icon-text-fit`) !== `none`, a3 = wg(i2, n3, S2, r3), f3 = o2 ? wg(o2, n3, S2, r3) : void 0;
    O2 = new Ag(c2, t2, l2, u2, d2, i2, g2, _2, false, n3), ne2 = a3.length * 4;
    let p3 = e49.iconSizeData, m3 = null;
    p3.kind === `source` ? (m3 = [128 * s2.layout.get(`icon-size`).evaluate(b2, {})], m3[0] > 32640 && A(`${e49.layerIds[0]}: Value for "icon-size" is >= 255. Reduce your "icon-size".`)) : p3.kind === `composite` && (m3 = [128 * x2.compositeIconSizes[0].evaluate(b2, {}, C2), 128 * x2.compositeIconSizes[1].evaluate(b2, {}, C2)], (m3[0] > 32640 || m3[1] > 32640) && A(`${e49.layerIds[0]}: Value for "icon-size" is >= 255. Reduce your "icon-size".`)), e49.addSymbols(e49.icon, a3, m3, y2, v2, b2, 0, t2, E2.lineStartIndex, E2.lineLength, -1, C2, D2), oe2 = e49.icon.placedSymbolArray.length - 1, f3 && (re2 = f3.length * 4, e49.addSymbols(e49.icon, f3, m3, y2, v2, b2, 2, t2, E2.lineStartIndex, E2.lineLength, -1, C2, D2), se2 = e49.icon.placedSymbolArray.length - 1);
  }
  let ue2 = Object.keys(r2.horizontal);
  for (let n3 of ue2) {
    let i3 = r2.horizontal[n3];
    ee2 ||= (le2 = (0, Xs.default)(i3.text), new Ag(c2, t2, l2, u2, d2, i3, f2, p2, m2, s2.layout.get(`text-rotate`).evaluate(b2, {}, C2)));
    let o3 = i3.positionedLines.length === 1;
    if (ie2 += Gg(e49, t2, i3, a2, s2, m2, b2, h2, D2, E2, r2.vertical ? 1 : 3, o3 ? ue2 : [n3], ce2, oe2, x2, C2), o3) break;
  }
  r2.vertical && (ae2 += Gg(e49, t2, r2.vertical, a2, s2, m2, b2, h2, D2, E2, 2, [`vertical`], ce2, se2, x2, C2));
  let de2 = ee2 ? ee2.boxStartIndex : e49.collisionBoxArray.length, fe2 = ee2 ? ee2.boxEndIndex : e49.collisionBoxArray.length, pe2 = k2 ? k2.boxStartIndex : e49.collisionBoxArray.length, me2 = k2 ? k2.boxEndIndex : e49.collisionBoxArray.length, he2 = O2 ? O2.boxStartIndex : e49.collisionBoxArray.length, ge2 = O2 ? O2.boxEndIndex : e49.collisionBoxArray.length, j2 = te2 ? te2.boxStartIndex : e49.collisionBoxArray.length, M2 = te2 ? te2.boxEndIndex : e49.collisionBoxArray.length, _e2 = -1, N2 = (e50, t3) => (e50 == null ? void 0 : e50.circleDiameter) ? Math.max(e50.circleDiameter, t3) : t3;
  _e2 = N2(ee2, _e2), _e2 = N2(k2, _e2), _e2 = N2(O2, _e2), _e2 = N2(te2, _e2);
  let P2 = +(_e2 > -1);
  P2 && (_e2 *= w2 / 24), e49.glyphOffsetArray.length >= e49.maxGlyphs && A(`Too many glyphs being rendered in a tile. See https://github.com/mapbox/mapbox-gl-js/issues/2907`), b2.sortKey !== void 0 && e49.addToSortKeyRanges(e49.symbolInstances.length, b2.sortKey);
  let F2 = Bg(s2, b2, C2), [I2, L2] = Wg(e49.textAnchorOffsets, F2);
  T2 && (le2 = Yg(le2, b2)), e49.symbolInstances.emplaceBack(t2.x, t2.y, ce2.right >= 0 ? ce2.right : -1, ce2.center >= 0 ? ce2.center : -1, ce2.left >= 0 ? ce2.left : -1, ce2.vertical || -1, oe2, se2, le2, de2, fe2, pe2, me2, he2, ge2, j2, M2, l2, ie2, ae2, ne2, re2, P2, 0, f2, _e2, I2, L2, D2);
}
function Jg(e49, t2, n2, r2) {
  let i2 = e49.compareText;
  if (!(t2 in i2)) i2[t2] = [];
  else {
    let e50 = i2[t2];
    for (let t3 = e50.length - 1; t3 >= 0; t3--) if (r2.dist(e50[t3]) < n2) return true;
  }
  return i2[t2].push(r2), false;
}
function Yg(e49, t2) {
  var _a3;
  return t2.id == null || ((_a3 = t2.properties) == null ? void 0 : _a3.cluster) ? e49 : (0, Xs.default)(String(t2.id), e49);
}
function Xg(e49, t2, n2) {
  let r2 = `never`, i2 = e49.get(t2);
  return i2 ? r2 = i2 : e49.get(n2) && (r2 = `always`), r2;
}
const Zg = [{ name: `a_fade_opacity`, components: 1, type: `Uint8`, offset: 0 }];
function Qg(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2, p2) {
  let m2 = s2 ? Math.min(ig, Math.round(s2[0])) : 0, h2 = s2 ? Math.min(ig, Math.round(s2[1])) : 0;
  e49.emplaceBack(t2, n2, Math.round(r2 * 32), Math.round(i2 * 32), a2, o2, (m2 << 1) + +!!c2, h2, l2 * 16, u2 * 16, d2 * 256, f2 * 256, p2);
}
function $g(e49, t2, n2) {
  e49.emplaceBack(t2.x, t2.y, n2), e49.emplaceBack(t2.x, t2.y, n2), e49.emplaceBack(t2.x, t2.y, n2), e49.emplaceBack(t2.x, t2.y, n2);
}
function e_(e49) {
  for (let t2 of e49.sections) if (Om(t2.text)) return true;
  return false;
}
var t_ = class {
  constructor(e49) {
    this.layoutVertexArray = new Fs(), this.indexArray = new Bs(), this.programConfigurations = e49, this.segments = new Ws(), this.dynamicLayoutVertexArray = new Is(), this.opacityVertexArray = new Ls(), this.hasVisibleVertices = false, this.placedSymbolArray = new vs();
  }
  isEmpty() {
    return this.layoutVertexArray.length === 0 && this.indexArray.length === 0 && this.dynamicLayoutVertexArray.length === 0 && this.opacityVertexArray.length === 0;
  }
  upload(e49, t2, n2, r2) {
    this.isEmpty() || (n2 && (this.layoutVertexBuffer = e49.createVertexBuffer(this.layoutVertexArray, Mp.members), this.indexBuffer = e49.createIndexBuffer(this.indexArray, t2), this.dynamicLayoutVertexBuffer = e49.createVertexBuffer(this.dynamicLayoutVertexArray, Np.members, true), this.opacityVertexBuffer = e49.createVertexBuffer(this.opacityVertexArray, Zg, true), this.opacityVertexBuffer.itemSize = 1), (n2 || r2) && this.programConfigurations.upload(e49));
  }
  destroy() {
    this.layoutVertexBuffer && (this.layoutVertexBuffer.destroy(), this.indexBuffer.destroy(), this.programConfigurations.destroy(), this.segments.destroy(), this.dynamicLayoutVertexBuffer.destroy(), this.opacityVertexBuffer.destroy());
  }
};
G(`SymbolBuffers`, t_);
var n_ = class {
  constructor(e49, t2, n2) {
    this.layoutVertexArray = new e49(), this.layoutAttributes = t2, this.indexArray = new n2(), this.segments = new Ws(), this.collisionVertexArray = new zs();
  }
  upload(e49) {
    this.layoutVertexBuffer = e49.createVertexBuffer(this.layoutVertexArray, this.layoutAttributes), this.indexBuffer = e49.createIndexBuffer(this.indexArray), this.collisionVertexBuffer = e49.createVertexBuffer(this.collisionVertexArray, Pp.members, true);
  }
  destroy() {
    this.layoutVertexBuffer && (this.layoutVertexBuffer.destroy(), this.indexBuffer.destroy(), this.segments.destroy(), this.collisionVertexBuffer.destroy());
  }
};
G(`CollisionBuffers`, n_);
var r_ = class {
  constructor(e49) {
    this.collisionBoxArray = e49.collisionBoxArray, this.zoom = e49.zoom, this.overscaling = e49.overscaling, this.layers = e49.layers, this.layerIds = this.layers.map((e50) => e50.id), this.index = e49.index, this.pixelRatio = e49.pixelRatio, this.sourceLayerIndex = e49.sourceLayerIndex, this.hasDependencies = true, this.hasRTLText = false, this.maxHeightOffset = 0, this.sortKeyRanges = [], this.collisionCircleArray = [], this.maxGlyphs = 65535;
    let t2 = this.layers[0]._unevaluatedLayout._values;
    this.textSizeData = ag(this.zoom, t2[`text-size`]), this.iconSizeData = ag(this.zoom, t2[`icon-size`]);
    let n2 = this.layers[0].layout, r2 = n2.get(`symbol-sort-key`), i2 = n2.get(`symbol-z-order`);
    this.canOverlap = Xg(n2, `text-overlap`, `text-allow-overlap`) !== `never` || Xg(n2, `icon-overlap`, `icon-allow-overlap`) !== `never` || n2.get(`text-ignore-placement`) || n2.get(`icon-ignore-placement`), this.sortFeaturesByKey = i2 !== `viewport-y` && !r2.isConstant();
    let a2 = i2 === `viewport-y` || i2 === `auto` && !this.sortFeaturesByKey;
    this.sortFeaturesByY = a2 && this.canOverlap, n2.get(`symbol-placement`) === `point` && (this.writingModes = n2.get(`text-writing-mode`).map((e50) => Dh[e50])), this.stateDependentLayerIds = this.layers.filter((e50) => e50.isStateDependent()).map((e50) => e50.id), this.sourceID = e49.sourceID;
  }
  createArrays() {
    this.text = new t_(new mc(this.layers, this.zoom, (e49) => e49.startsWith(`text`))), this.icon = new t_(new mc(this.layers, this.zoom, (e49) => e49.startsWith(`icon`))), this.glyphOffsetArray = new xs(), this.lineVertexArray = new Ss(), this.symbolInstances = new bs(), this.textAnchorOffsets = new ws();
  }
  calculateGlyphDependencies(e49, t2, n2, r2, i2) {
    let a2 = (r2 || this.allowVerticalPlacement) && i2, o2 = zm.fromFeature(e49, n2), s2 = o2.graphemes();
    for (let e50 = 0; e50 < s2.length; e50++) {
      let n3 = o2.getSection(e50);
      if (`imageName` in n3) continue;
      let r3 = (t2[n3.fontStack] ||= { default: {} }).default, i3 = s2[e50];
      bm(i3) && (r3[i3] = true);
      for (let e51 of i3) {
        if (r3[e51] = true, !a2) continue;
        let t3 = km[e51];
        t3 && (r3[t3] = true);
      }
    }
  }
  populate(e49, t2, n2) {
    var _a3;
    let r2 = this.layers[0], i2 = r2.layout, a2 = i2.get(`text-font`), o2 = i2.get(`text-field`), s2 = i2.get(`icon-image`), c2 = (o2.value.kind !== `constant` || o2.value.value instanceof ut && !o2.value.value.isEmpty() || o2.value.value.toString().length > 0) && (a2.value.kind !== `constant` || a2.value.value.length > 0), l2 = s2.value.kind !== `constant` || !!s2.value.value || Object.keys(s2.parameters).length > 0, u2 = i2.get(`symbol-sort-key`);
    if (this.features = [], !c2 && !l2) return;
    let d2 = t2.iconDependencies, f2 = t2.glyphDependencies, p2 = t2.availableImages, m2 = new K(this.zoom);
    for (let { feature: t3, id: o3, index: s3, sourceLayerIndex: h2 } of e49) {
      let e50 = r2._featureFilter.needGeometry, g2 = xc(t3, e50);
      if (!r2._featureFilter.filter(m2, g2, n2)) continue;
      e50 || (g2.geometry = bc(t3));
      let _2;
      if (c2) {
        let e51 = r2.getValueAndResolveTokens(`text-field`, g2, n2, p2), t4 = ut.factory(e51);
        this.hasRTLText ||= e_(t4), _2 = lm(t4, r2, g2);
      }
      let v2;
      if (l2) {
        let e51 = r2.getValueAndResolveTokens(`icon-image`, g2, n2, p2);
        v2 = e51 instanceof gt ? e51 : gt.fromString(e51);
      }
      if (!_2 && !v2) continue;
      let y2 = this.sortFeaturesByKey ? u2.evaluate(g2, {}, n2) : void 0, b2 = { id: o3, text: _2, icon: v2, index: s3, sourceLayerIndex: h2, geometry: g2.geometry, properties: t3.properties, type: ed.types[t3.type], sortKey: y2 };
      if (this.features.push(b2), v2 && (d2[v2.name] = true), _2) {
        let e51 = a2.evaluate(g2, {}, n2).join(`,`), t4 = i2.get(`text-rotation-alignment`) !== `viewport` && i2.get(`symbol-placement`) !== `point`;
        this.allowVerticalPlacement = (_a3 = this.writingModes) == null ? void 0 : _a3.includes(2);
        let r3 = Sm(_2.toString());
        this.calculateGlyphDependencies(_2, f2, e51, t4, r3);
        for (let e52 of _2.sections) e52.image && (d2[e52.image.name] = true);
      }
    }
    i2.get(`symbol-placement`) === `line` && (this.features = mm(this.features)), this.sortFeaturesByKey && this.features.sort((e50, t3) => e50.sortKey - t3.sortKey);
  }
  update(e49, t2, n2) {
    this.stateDependentLayers.length && (this.text.programConfigurations.updatePaintArrays(e49, t2, this.layers, { imagePositions: n2 }), this.icon.programConfigurations.updatePaintArrays(e49, t2, this.layers, { imagePositions: n2 }));
  }
  addFeatures({ options: e49, canonical: t2, glyphMap: n2, glyphPositions: r2, iconMap: i2, iconPositions: a2, showCollisionBoxes: o2 }) {
    Vg({ bucket: this, glyphMap: n2, glyphPositions: r2, imageMap: i2, imagePositions: a2, showCollisionBoxes: o2, canonical: t2, subdivisionGranularity: e49.subdivisionGranularity, hasPromoteId: e49.featureIndex.promoteId != null });
  }
  isEmpty() {
    return this.symbolInstances.length === 0;
  }
  uploadPending() {
    return !this.uploaded || this.text.programConfigurations.needsUpload || this.icon.programConfigurations.needsUpload;
  }
  upload(e49) {
    !this.uploaded && this.hasDebugData() && (this.textCollisionBox.upload(e49), this.iconCollisionBox.upload(e49)), this.text.upload(e49, this.sortFeaturesByY, !this.uploaded, this.text.programConfigurations.needsUpload), this.icon.upload(e49, this.sortFeaturesByY, !this.uploaded, this.icon.programConfigurations.needsUpload), this.uploaded = true;
  }
  destroyDebugData() {
    this.textCollisionBox.destroy(), this.iconCollisionBox.destroy();
  }
  destroy() {
    this.text.destroy(), this.icon.destroy(), this.hasDebugData() && this.destroyDebugData();
  }
  addToLineVertexArray(e49, t2) {
    let n2 = this.lineVertexArray.length;
    if (e49.segment !== void 0) {
      let n3 = e49.dist(t2[e49.segment + 1]), r2 = e49.dist(t2[e49.segment]), i2 = {};
      for (let r3 = e49.segment + 1; r3 < t2.length; r3++) i2[r3] = { x: t2[r3].x, y: t2[r3].y, tileUnitDistanceFromAnchor: n3 }, r3 < t2.length - 1 && (n3 += t2[r3 + 1].dist(t2[r3]));
      for (let n4 = e49.segment || 0; n4 >= 0; n4--) i2[n4] = { x: t2[n4].x, y: t2[n4].y, tileUnitDistanceFromAnchor: r2 }, n4 > 0 && (r2 += t2[n4 - 1].dist(t2[n4]));
      for (let e50 = 0; e50 < t2.length; e50++) {
        let t3 = i2[e50];
        this.lineVertexArray.emplaceBack(t3.x, t3.y, t3.tileUnitDistanceFromAnchor);
      }
    }
    return { lineStartIndex: n2, lineLength: this.lineVertexArray.length - n2 };
  }
  addSymbols(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2, d2, f2) {
    let p2 = e49.indexArray, m2 = e49.layoutVertexArray, h2 = e49.segments.prepareSegment(4 * t2.length, m2, p2, this.canOverlap ? a2.sortKey : void 0), g2 = this.glyphOffsetArray.length, _2 = h2.vertexLength, v2 = this.allowVerticalPlacement && o2 === 2 ? Math.PI / 2 : 0, y2 = a2.text && a2.text.sections;
    for (let r3 = 0; r3 < t2.length; r3++) {
      let { tl: i3, tr: o3, bl: c3, br: l3, tex: u3, pixelOffsetTL: g3, pixelOffsetBR: _3, minFontScaleX: b2, minFontScaleY: x2, glyphOffset: S2, isSDF: C2, sectionIndex: w2 } = t2[r3], T2 = h2.vertexLength, E2 = S2[1];
      Qg(m2, s2.x, s2.y, i3.x, E2 + i3.y, u3.x, u3.y, n2, C2, g3.x, g3.y, b2, x2, f2), Qg(m2, s2.x, s2.y, o3.x, E2 + o3.y, u3.x + u3.w, u3.y, n2, C2, _3.x, g3.y, b2, x2, f2), Qg(m2, s2.x, s2.y, c3.x, E2 + c3.y, u3.x, u3.y + u3.h, n2, C2, g3.x, _3.y, b2, x2, f2), Qg(m2, s2.x, s2.y, l3.x, E2 + l3.y, u3.x + u3.w, u3.y + u3.h, n2, C2, _3.x, _3.y, b2, x2, f2), $g(e49.dynamicLayoutVertexArray, s2, v2), p2.emplaceBack(T2, T2 + 2, T2 + 1), p2.emplaceBack(T2 + 1, T2 + 2, T2 + 3), h2.vertexLength += 4, h2.primitiveLength += 2, this.glyphOffsetArray.emplaceBack(S2[0]), (r3 === t2.length - 1 || w2 !== t2[r3 + 1].sectionIndex) && e49.programConfigurations.populatePaintArrays(m2.length, a2, a2.index, { imagePositions: {}, canonical: d2, formattedSection: y2 == null ? void 0 : y2[w2] });
    }
    e49.placedSymbolArray.emplaceBack(s2.x, s2.y, g2, this.glyphOffsetArray.length - g2, _2, c2, l2, s2.segment, n2 ? n2[0] : 0, n2 ? n2[1] : 0, r2[0], r2[1], o2, 0, false, 0, u2, f2);
  }
  _addCollisionDebugVertex(e49, t2, n2, r2, i2, a2) {
    return t2.emplaceBack(0, 0), e49.emplaceBack(n2.x, n2.y, r2, i2, Math.round(a2.x), Math.round(a2.y));
  }
  addCollisionDebugVertices(e49, t2, n2, r2, i2, a2, o2) {
    let s2 = i2.segments.prepareSegment(4, i2.layoutVertexArray, i2.indexArray), c2 = s2.vertexLength, u2 = i2.layoutVertexArray, d2 = i2.collisionVertexArray, f2 = o2.anchorX, p2 = o2.anchorY;
    this._addCollisionDebugVertex(u2, d2, a2, f2, p2, new l(e49, t2)), this._addCollisionDebugVertex(u2, d2, a2, f2, p2, new l(n2, t2)), this._addCollisionDebugVertex(u2, d2, a2, f2, p2, new l(n2, r2)), this._addCollisionDebugVertex(u2, d2, a2, f2, p2, new l(e49, r2)), s2.vertexLength += 4;
    let m2 = i2.indexArray;
    m2.emplaceBack(c2, c2 + 1), m2.emplaceBack(c2 + 1, c2 + 2), m2.emplaceBack(c2 + 2, c2 + 3), m2.emplaceBack(c2 + 3, c2), s2.primitiveLength += 4;
  }
  addDebugCollisionBoxes(e49, t2, n2, r2) {
    for (let i2 = e49; i2 < t2; i2++) {
      let e50 = this.collisionBoxArray.get(i2), t3 = e50.x1, a2 = e50.y1, o2 = e50.x2, s2 = e50.y2;
      this.addCollisionDebugVertices(t3, a2, o2, s2, r2 ? this.textCollisionBox : this.iconCollisionBox, e50.anchorPoint, n2);
    }
  }
  generateCollisionDebugBuffers() {
    this.hasDebugData() && this.destroyDebugData(), this.textCollisionBox = new n_(Rs, Fp.members, Vs), this.iconCollisionBox = new n_(Rs, Fp.members, Vs);
    for (let e49 = 0; e49 < this.symbolInstances.length; e49++) {
      let t2 = this.symbolInstances.get(e49);
      this.addDebugCollisionBoxes(t2.textBoxStartIndex, t2.textBoxEndIndex, t2, true), this.addDebugCollisionBoxes(t2.verticalTextBoxStartIndex, t2.verticalTextBoxEndIndex, t2, true), this.addDebugCollisionBoxes(t2.iconBoxStartIndex, t2.iconBoxEndIndex, t2, false), this.addDebugCollisionBoxes(t2.verticalIconBoxStartIndex, t2.verticalIconBoxEndIndex, t2, false);
    }
  }
  _deserializeCollisionBoxesForSymbol(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
    let l2 = {};
    for (let r3 = t2; r3 < n2; r3++) {
      let t3 = e49.get(r3);
      l2.textBox = { x1: t3.x1, y1: t3.y1, x2: t3.x2, y2: t3.y2, anchorPointX: t3.anchorPointX, anchorPointY: t3.anchorPointY }, l2.textFeatureIndex = t3.featureIndex;
      break;
    }
    for (let t3 = r2; t3 < i2; t3++) {
      let n3 = e49.get(t3);
      l2.verticalTextBox = { x1: n3.x1, y1: n3.y1, x2: n3.x2, y2: n3.y2, anchorPointX: n3.anchorPointX, anchorPointY: n3.anchorPointY }, l2.verticalTextFeatureIndex = n3.featureIndex;
      break;
    }
    for (let t3 = a2; t3 < o2; t3++) {
      let n3 = e49.get(t3);
      l2.iconBox = { x1: n3.x1, y1: n3.y1, x2: n3.x2, y2: n3.y2, anchorPointX: n3.anchorPointX, anchorPointY: n3.anchorPointY }, l2.iconFeatureIndex = n3.featureIndex;
      break;
    }
    for (let t3 = s2; t3 < c2; t3++) {
      let n3 = e49.get(t3);
      l2.verticalIconBox = { x1: n3.x1, y1: n3.y1, x2: n3.x2, y2: n3.y2, anchorPointX: n3.anchorPointX, anchorPointY: n3.anchorPointY }, l2.verticalIconFeatureIndex = n3.featureIndex;
      break;
    }
    return l2;
  }
  deserializeCollisionBoxes(e49) {
    this.collisionArrays = [];
    for (let t2 = 0; t2 < this.symbolInstances.length; t2++) {
      let n2 = this.symbolInstances.get(t2);
      this.collisionArrays.push(this._deserializeCollisionBoxesForSymbol(e49, n2.textBoxStartIndex, n2.textBoxEndIndex, n2.verticalTextBoxStartIndex, n2.verticalTextBoxEndIndex, n2.iconBoxStartIndex, n2.iconBoxEndIndex, n2.verticalIconBoxStartIndex, n2.verticalIconBoxEndIndex));
    }
  }
  hasTextData() {
    return this.text.segments.get().length > 0;
  }
  hasIconData() {
    return this.icon.segments.get().length > 0;
  }
  hasDebugData() {
    return this.textCollisionBox && this.iconCollisionBox;
  }
  hasTextCollisionBoxData() {
    return this.hasDebugData() && this.textCollisionBox.segments.get().length > 0;
  }
  hasIconCollisionBoxData() {
    return this.hasDebugData() && this.iconCollisionBox.segments.get().length > 0;
  }
  addIndicesForPlacedSymbol(e49, t2) {
    let n2 = e49.placedSymbolArray.get(t2), r2 = n2.vertexStartIndex + n2.numGlyphs * 4;
    for (let t3 = n2.vertexStartIndex; t3 < r2; t3 += 4) e49.indexArray.emplaceBack(t3, t3 + 2, t3 + 1), e49.indexArray.emplaceBack(t3 + 1, t3 + 2, t3 + 3);
  }
  getSortedSymbolIndexes(e49) {
    if (this.sortedAngle === e49 && this.symbolInstanceIndexes !== void 0) return this.symbolInstanceIndexes;
    let t2 = Math.sin(e49), n2 = Math.cos(e49), r2 = [], i2 = [], a2 = [];
    for (let e50 = 0; e50 < this.symbolInstances.length; ++e50) {
      a2.push(e50);
      let o2 = this.symbolInstances.get(e50);
      r2.push(Math.round(t2 * o2.anchorX + n2 * o2.anchorY) | 0), i2.push(o2.featureIndex);
    }
    return a2.sort((e50, t3) => r2[e50] - r2[t3] || i2[t3] - i2[e50]), a2;
  }
  addToSortKeyRanges(e49, t2) {
    let n2 = this.sortKeyRanges[this.sortKeyRanges.length - 1];
    (n2 == null ? void 0 : n2.sortKey) === t2 ? n2.symbolInstanceEnd = e49 + 1 : this.sortKeyRanges.push({ sortKey: t2, symbolInstanceStart: e49, symbolInstanceEnd: e49 + 1 });
  }
  sortFeatures(e49) {
    if (this.sortFeaturesByY && this.sortedAngle !== e49 && !(this.text.segments.get().length > 1 || this.icon.segments.get().length > 1)) {
      this.symbolInstanceIndexes = this.getSortedSymbolIndexes(e49), this.sortedAngle = e49, this.text.indexArray.clear(), this.icon.indexArray.clear(), this.featureSortOrder = [];
      for (let e50 of this.symbolInstanceIndexes) {
        let t2 = this.symbolInstances.get(e50);
        this.featureSortOrder.push(t2.featureIndex);
        let n2 = [t2.rightJustifiedTextSymbolIndex, t2.centerJustifiedTextSymbolIndex, t2.leftJustifiedTextSymbolIndex];
        for (let e51 = 0; e51 < n2.length; e51++) {
          let t3 = n2[e51];
          t3 >= 0 && n2.indexOf(t3) === e51 && this.addIndicesForPlacedSymbol(this.text, t3);
        }
        t2.verticalPlacedTextSymbolIndex >= 0 && this.addIndicesForPlacedSymbol(this.text, t2.verticalPlacedTextSymbolIndex), t2.placedIconSymbolIndex >= 0 && this.addIndicesForPlacedSymbol(this.icon, t2.placedIconSymbolIndex), t2.verticalPlacedIconSymbolIndex >= 0 && this.addIndicesForPlacedSymbol(this.icon, t2.verticalPlacedIconSymbolIndex);
      }
      this.text.indexBuffer && this.text.indexBuffer.updateData(this.text.indexArray), this.icon.indexBuffer && this.icon.indexBuffer.updateData(this.icon.indexArray);
    }
  }
};
G(`SymbolBucket`, r_, { omit: [`layers`, `collisionBoxArray`, `features`, `compareText`] });
function i_(e49, t2) {
  return t2.replace(/{([^{}]+)}/g, (t3, n2) => e49 && n2 in e49 ? String(e49[n2]) : ``);
}
let a_;
const o_ = () => a_ ||= new Fo({ "symbol-placement": new q(M.layout_symbol[`symbol-placement`], `symbol-placement`), "symbol-spacing": new q(M.layout_symbol[`symbol-spacing`], `symbol-spacing`), "symbol-avoid-edges": new q(M.layout_symbol[`symbol-avoid-edges`], `symbol-avoid-edges`), "symbol-sort-key": new J(M.layout_symbol[`symbol-sort-key`], `symbol-sort-key`), "symbol-z-order": new q(M.layout_symbol[`symbol-z-order`], `symbol-z-order`), "icon-allow-overlap": new q(M.layout_symbol[`icon-allow-overlap`], `icon-allow-overlap`), "icon-overlap": new q(M.layout_symbol[`icon-overlap`], `icon-overlap`), "icon-ignore-placement": new q(M.layout_symbol[`icon-ignore-placement`], `icon-ignore-placement`), "icon-optional": new q(M.layout_symbol[`icon-optional`], `icon-optional`), "icon-rotation-alignment": new J(M.layout_symbol[`icon-rotation-alignment`], `icon-rotation-alignment`), "icon-size": new J(M.layout_symbol[`icon-size`], `icon-size`), "icon-text-fit": new q(M.layout_symbol[`icon-text-fit`], `icon-text-fit`), "icon-text-fit-padding": new q(M.layout_symbol[`icon-text-fit-padding`], `icon-text-fit-padding`), "icon-image": new J(M.layout_symbol[`icon-image`], `icon-image`), "icon-rotate": new J(M.layout_symbol[`icon-rotate`], `icon-rotate`), "icon-padding": new J(M.layout_symbol[`icon-padding`], `icon-padding`), "icon-keep-upright": new q(M.layout_symbol[`icon-keep-upright`], `icon-keep-upright`), "icon-offset": new J(M.layout_symbol[`icon-offset`], `icon-offset`), "icon-anchor": new J(M.layout_symbol[`icon-anchor`], `icon-anchor`), "icon-pitch-alignment": new q(M.layout_symbol[`icon-pitch-alignment`], `icon-pitch-alignment`), "text-pitch-alignment": new q(M.layout_symbol[`text-pitch-alignment`], `text-pitch-alignment`), "text-rotation-alignment": new q(M.layout_symbol[`text-rotation-alignment`], `text-rotation-alignment`), "text-field": new J(M.layout_symbol[`text-field`], `text-field`), "text-font": new J(M.layout_symbol[`text-font`], `text-font`), "text-size": new J(M.layout_symbol[`text-size`], `text-size`), "text-max-width": new J(M.layout_symbol[`text-max-width`], `text-max-width`), "text-line-height": new q(M.layout_symbol[`text-line-height`], `text-line-height`), "text-letter-spacing": new J(M.layout_symbol[`text-letter-spacing`], `text-letter-spacing`), "text-justify": new J(M.layout_symbol[`text-justify`], `text-justify`), "text-radial-offset": new J(M.layout_symbol[`text-radial-offset`], `text-radial-offset`), "text-variable-anchor": new q(M.layout_symbol[`text-variable-anchor`], `text-variable-anchor`), "text-variable-anchor-offset": new J(M.layout_symbol[`text-variable-anchor-offset`], `text-variable-anchor-offset`), "text-anchor": new J(M.layout_symbol[`text-anchor`], `text-anchor`), "text-max-angle": new q(M.layout_symbol[`text-max-angle`], `text-max-angle`), "text-writing-mode": new q(M.layout_symbol[`text-writing-mode`], `text-writing-mode`), "text-rotate": new J(M.layout_symbol[`text-rotate`], `text-rotate`), "text-padding": new q(M.layout_symbol[`text-padding`], `text-padding`), "text-keep-upright": new q(M.layout_symbol[`text-keep-upright`], `text-keep-upright`), "text-transform": new J(M.layout_symbol[`text-transform`], `text-transform`), "text-offset": new J(M.layout_symbol[`text-offset`], `text-offset`), "text-allow-overlap": new q(M.layout_symbol[`text-allow-overlap`], `text-allow-overlap`), "text-overlap": new q(M.layout_symbol[`text-overlap`], `text-overlap`), "text-ignore-placement": new q(M.layout_symbol[`text-ignore-placement`], `text-ignore-placement`), "text-optional": new q(M.layout_symbol[`text-optional`], `text-optional`), "symbol-height-offset": new J(M.layout_symbol[`symbol-height-offset`], `symbol-height-offset`), "symbol-height-anchor": new q(M.layout_symbol[`symbol-height-anchor`], `symbol-height-anchor`) });
let s_;
const c_ = () => s_ ||= new Fo({ "icon-opacity": new J(M.paint_symbol[`icon-opacity`], `icon-opacity`), "icon-color": new J(M.paint_symbol[`icon-color`], `icon-color`), "icon-halo-color": new J(M.paint_symbol[`icon-halo-color`], `icon-halo-color`), "icon-halo-width": new J(M.paint_symbol[`icon-halo-width`], `icon-halo-width`), "icon-halo-blur": new J(M.paint_symbol[`icon-halo-blur`], `icon-halo-blur`), "icon-translate": new q(M.paint_symbol[`icon-translate`], `icon-translate`), "icon-translate-anchor": new q(M.paint_symbol[`icon-translate-anchor`], `icon-translate-anchor`), "text-opacity": new J(M.paint_symbol[`text-opacity`], `text-opacity`), "text-color": new J(M.paint_symbol[`text-color`], `text-color`, { runtimeType: ve, getOverride: (e49) => e49.textColor, hasOverride: (e49) => !!e49.textColor }), "text-halo-color": new J(M.paint_symbol[`text-halo-color`], `text-halo-color`), "text-halo-width": new J(M.paint_symbol[`text-halo-width`], `text-halo-width`), "text-halo-blur": new J(M.paint_symbol[`text-halo-blur`], `text-halo-blur`), "text-translate": new q(M.paint_symbol[`text-translate`], `text-translate`), "text-translate-anchor": new q(M.paint_symbol[`text-translate-anchor`], `text-translate-anchor`) });
var l_ = { get paint() {
  return c_();
}, get layout() {
  return o_();
} }, u_ = class {
  constructor(e49) {
    if (e49.property.overrides === void 0) throw Error(`overrides must be provided to instantiate FormatSectionOverride class`);
    this.type = e49.property.overrides ? e49.property.overrides.runtimeType : P, this.defaultValue = e49;
  }
  evaluate(e49) {
    if (e49.formattedSection) {
      let t2 = this.defaultValue.property.overrides;
      if (t2 == null ? void 0 : t2.hasOverride(e49.formattedSection)) return t2.getOverride(e49.formattedSection);
    }
    return e49.feature && e49.featureState ? this.defaultValue.evaluate(e49.feature, e49.featureState) : this.defaultValue.property.specification.default;
  }
  eachChild(e49) {
    if (!this.defaultValue.isConstant()) {
      let t2 = this.defaultValue.value;
      e49(t2._styleExpression.expression);
    }
  }
  outputDefined() {
    return false;
  }
  serialize() {
    return null;
  }
};
G(`FormatSectionOverride`, u_, { omit: [`defaultValue`] });
var d_ = class e47 extends Ro {
  constructor(e49, t2) {
    super(e49, l_, t2);
  }
  recalculate(e49, t2) {
    super.recalculate(e49, t2);
    let n2 = this.layout.get(`icon-rotation-alignment`);
    if ((n2.value.kind !== `constant` || n2.value.value === `auto`) && (this.layout._values[`icon-rotation-alignment`] = new Oo(n2.property, { kind: `constant`, value: this.layout.get(`symbol-placement`) === `point` ? `viewport` : `map` }, n2.parameters)), this.layout.get(`text-rotation-alignment`) === `auto` && (this.layout.get(`symbol-placement`) === `point` ? this.layout._values[`text-rotation-alignment`] = `viewport` : this.layout._values[`text-rotation-alignment`] = `map`), this.layout.get(`text-pitch-alignment`) === `auto` && (this.layout._values[`text-pitch-alignment`] = this.layout.get(`text-rotation-alignment`) === `map` ? `map` : `viewport`), this.layout.get(`icon-pitch-alignment`) === `auto` && (this.layout._values[`icon-pitch-alignment`] = this.layout.get(`icon-rotation-alignment`).constantOr(`viewport`)), this.layout.get(`symbol-placement`) === `point`) {
      let e50 = this.layout.get(`text-writing-mode`);
      if (e50) {
        let t3 = [];
        for (let n3 of e50) t3.includes(n3) || t3.push(n3);
        this.layout._values[`text-writing-mode`] = t3;
      } else this.layout._values[`text-writing-mode`] = [`horizontal`];
    }
    this._setPaintOverrides();
  }
  getValueAndResolveTokens(e49, t2, n2, r2) {
    let i2 = this.layout.get(e49).evaluate(t2, {}, n2, r2), a2 = this._unevaluatedLayout._values[e49];
    return !a2.isDataDriven() && !ci(a2.value) && i2 ? i_(t2.properties, i2) : i2;
  }
  createBucket(e49) {
    return new r_(e49);
  }
  queryRadius() {
    return 0;
  }
  queryIntersectsFeature() {
    throw Error(`Should take a different path in FeatureIndex`);
  }
  _setPaintOverrides() {
    for (let t2 of l_.paint.overridableProperties) {
      if (!e47.hasPaintOverride(this.layout, t2)) continue;
      let n2 = this.paint.get(t2), r2 = new ai(new u_(n2), `layers[${this.id}].paint.${n2.property.name}`, n2.property.specification), i2 = null;
      i2 = n2.value.kind === `constant` || n2.value.kind === `source` ? new ui(`source`, r2) : new di(`composite`, r2, n2.value.zoomStops), this.paint._values[t2] = new Oo(n2.property, i2, n2.parameters);
    }
  }
  _handleOverridablePaintPropertyUpdate(t2, n2, r2) {
    return !this.layout || n2.isDataDriven() || r2.isDataDriven() ? false : e47.hasPaintOverride(this.layout, t2);
  }
  static hasPaintOverride(e49, t2) {
    let n2 = e49.get(`text-field`), r2 = l_.paint.properties[t2], i2 = false, a2 = (e50) => {
      var _a3;
      for (let t3 of e50) if ((_a3 = r2.overrides) == null ? void 0 : _a3.hasOverride(t3)) {
        i2 = true;
        return;
      }
    };
    if (n2.value.kind === `constant` && n2.value.value instanceof ut) a2(n2.value.value.sections);
    else if (n2.value.kind === `source` || n2.value.kind === `composite`) {
      let e50 = (t4) => {
        if (!i2) {
          if (t4 instanceof St && U(t4.value) === Ce) {
            let e51 = t4.value;
            a2(e51.sections);
          } else t4 instanceof jt ? a2(t4.sections) : t4.eachChild(e50);
        }
      }, t3 = n2.value;
      t3._styleExpression && e50(t3._styleExpression.expression);
    }
    return i2;
  }
};
function f_(e49, t2, n2, r2 = 1) {
  var _a3;
  let i2 = (_a3 = e49.get(`icon-padding`).evaluate(t2, {}, n2)) == null ? void 0 : _a3.values;
  return [i2[0] * r2, i2[1] * r2, i2[2] * r2, i2[3] * r2];
}
let p_;
const m_ = () => p_ ||= new Fo({ "background-color": new q(M.paint_background[`background-color`], `background-color`), "background-pattern": new No(M.paint_background[`background-pattern`], `background-pattern`), "background-opacity": new q(M.paint_background[`background-opacity`], `background-opacity`) });
var h_ = { get paint() {
  return m_();
} }, g_ = class extends Ro {
  constructor(e49, t2) {
    super(e49, h_, t2);
  }
};
let __;
const v_ = () => __ ||= new Fo({ "raster-opacity": new q(M.paint_raster[`raster-opacity`], `raster-opacity`), "raster-hue-rotate": new q(M.paint_raster[`raster-hue-rotate`], `raster-hue-rotate`), "raster-brightness-min": new q(M.paint_raster[`raster-brightness-min`], `raster-brightness-min`), "raster-brightness-max": new q(M.paint_raster[`raster-brightness-max`], `raster-brightness-max`), "raster-saturation": new q(M.paint_raster[`raster-saturation`], `raster-saturation`), "raster-contrast": new q(M.paint_raster[`raster-contrast`], `raster-contrast`), resampling: new q(M.paint_raster.resampling, `resampling`), "raster-resampling": new q(M.paint_raster[`raster-resampling`], `raster-resampling`), "raster-fade-duration": new q(M.paint_raster[`raster-fade-duration`], `raster-fade-duration`) });
var y_ = { get paint() {
  return v_();
} }, b_ = class extends Ro {
  constructor(e49, t2) {
    super(e49, y_, t2);
  }
}, x_ = class extends Ro {
  constructor(e49, t2) {
    super(e49, {}, t2), this.onAdd = (e50) => {
      this.implementation.onAdd && this.implementation.onAdd(e50, e50.painter.context.gl);
    }, this.onRemove = (e50) => {
      this.implementation.onRemove && this.implementation.onRemove(e50, e50.painter.context.gl);
    }, this.implementation = e49;
  }
  is3D() {
    return this.implementation.renderingMode === `3d`;
  }
  hasOffscreenPass() {
    return this.implementation.prerender !== void 0;
  }
  recalculate() {
  }
  updateTransitions() {
  }
  hasTransition() {
    return false;
  }
  serialize() {
    throw Error(`Custom layers cannot be serialized`);
  }
};
function S_(e49, t2) {
  if (e49.type === `custom`) return new x_(e49, t2);
  switch (e49.type) {
    case `background`:
      return new g_(e49, t2);
    case `circle`:
      return new tl(e49, t2);
    case `color-relief`:
      return new Tl(e49, t2);
    case `fill`:
      return new qu(e49, t2);
    case `fill-extrusion`:
      return new Ld(e49, t2);
    case `heatmap`:
      return new pl(e49, t2);
    case `hillshade`:
      return new _l(e49, t2);
    case `line`:
      return new Ap(e49, t2);
    case `raster`:
      return new b_(e49, t2);
    case `symbol`:
      return new d_(e49, t2);
  }
}
var C_ = class {
  constructor(e49, t2) {
    this.feature = e49, this.type = e49.type, this.properties = e49.tags ? e49.tags : {}, this.extent = t2, `id` in e49 && (typeof e49.id == `string` ? this.id = parseInt(e49.id, 10) : typeof e49.id == `number` && !isNaN(e49.id) && (this.id = e49.id));
  }
  loadGeometry() {
    let e49 = [], t2 = this.feature.type === 1 ? [this.feature.geometry] : this.feature.geometry;
    for (let n2 of t2) {
      let t3 = [];
      for (let e50 of n2) t3.push(new l(e50[0], e50[1]));
      e49.push(t3);
    }
    return e49;
  }
};
const w_ = `_geojsonTileLayer`;
var T_ = class {
  constructor(e49, t2) {
    this.layers = { [w_]: this }, this.name = w_, this.version = t2 ? t2.version : 1, this.extent = t2 ? t2.extent : 4096, this.length = e49.length, this.features = e49;
  }
  feature(e49) {
    return new C_(this.features[e49], this.extent);
  }
};
function E_(e49, t2 = ``) {
  let n2 = new ah();
  return D_(e49, n2, t2), n2.finish();
}
function D_(e49, t2, n2 = ``) {
  for (let r2 in e49.layers) t2.writeMessage(3, (e50, t3) => O_(e50, t3, n2), e49.layers[r2]);
}
function O_(e49, t2, n2 = ``) {
  t2.writeVarintField(15, e49.version || 1), t2.writeStringField(1, e49.name || ``), t2.writeVarintField(5, e49.extent || 4096);
  let r2 = { jsonPrefix: n2, keys: [], values: [], keycache: {}, valuecache: {} };
  for (let n3 = 0; n3 < e49.length; n3++) r2.feature = e49.feature(n3), t2.writeMessage(2, k_, r2);
  let i2 = r2.keys;
  for (let e50 of i2) t2.writeStringField(3, e50);
  let a2 = r2.values;
  for (let e50 of a2) t2.writeMessage(4, P_, e50);
}
function k_(e49, t2) {
  if (!e49.feature) return;
  let n2 = e49.feature;
  n2.id !== void 0 && t2.writeVarintField(1, n2.id), t2.writeMessage(2, A_, e49), t2.writeVarintField(3, n2.type), t2.writeMessage(4, N_, n2);
}
function A_(e49, t2) {
  var _a3;
  for (let n2 in (_a3 = e49.feature) == null ? void 0 : _a3.properties) {
    let r2 = e49.feature.properties[n2], i2 = e49.keycache[n2];
    if (r2 == null) continue;
    i2 === void 0 && (e49.keys.push(n2), i2 = e49.keys.length - 1, e49.keycache[n2] = i2), t2.writeVarint(i2), typeof r2 != `string` && typeof r2 != `boolean` && typeof r2 != `number` && (r2 = e49.jsonPrefix + JSON.stringify(r2));
    let a2 = typeof r2 + `:` + r2, o2 = e49.valuecache[a2];
    o2 === void 0 && (e49.values.push(r2), o2 = e49.values.length - 1, e49.valuecache[a2] = o2), t2.writeVarint(o2);
  }
}
function j_(e49, t2) {
  return (t2 << 3) + (e49 & 7);
}
function M_(e49) {
  return e49 << 1 ^ e49 >> 31;
}
function N_(e49, t2) {
  let n2 = e49.loadGeometry(), r2 = e49.type, i2 = 0, a2 = 0;
  for (let o2 of n2) {
    let n3 = 1;
    r2 === 1 && (n3 = o2.length), t2.writeVarint(j_(1, n3));
    let s2 = r2 === 3 ? o2.length - 1 : o2.length;
    for (let e50 = 0; e50 < s2; e50++) {
      e50 === 1 && r2 !== 1 && t2.writeVarint(j_(2, s2 - 1));
      let n4 = o2[e50].x - i2, c2 = o2[e50].y - a2;
      t2.writeVarint(M_(n4)), t2.writeVarint(M_(c2)), i2 += n4, a2 += c2;
    }
    e49.type === 3 && t2.writeVarint(j_(7, 1));
  }
}
function P_(e49, t2) {
  let n2 = typeof e49;
  n2 === `string` ? t2.writeStringField(1, e49) : n2 === `boolean` ? t2.writeBooleanField(7, e49) : n2 === `number` && (e49 % 1 == 0 ? e49 < 0 ? t2.writeSVarintField(6, e49) : t2.writeVarintField(5, e49) : t2.writeDoubleField(3, e49));
}
var F_ = class {
  constructor(e49) {
    this._stringToNumber = {}, this._numberToString = [];
    for (let t2 = 0; t2 < e49.length; t2++) {
      let n2 = e49[t2];
      this._stringToNumber[n2] = t2, this._numberToString[t2] = n2;
    }
  }
  encode(e49) {
    return this._stringToNumber[e49];
  }
  decode(e49) {
    if (e49 >= this._numberToString.length) throw Error(`Out of bounds. Index requested n=${e49} can't be >= this._numberToString.length ${this._numberToString.length}`);
    return this._numberToString[e49];
  }
}, I_ = class {
  constructor(e49, t2, n2, r2, i2) {
    this.type = `Feature`, this._vectorTileFeature = e49, this._x = n2, this._y = r2, this._z = t2;
    for (let t3 in e49.properties) typeof e49.properties[t3] == `string` && e49.properties[t3].startsWith(`__$json__:`) && (e49.properties[t3] = JSON.parse(e49.properties[t3].slice(10)));
    this.properties = e49.properties, this.id = i2;
  }
  projectPoint(e49, t2, n2, r2) {
    return [(e49.x + t2) * 360 / r2 - 180, 360 / Math.PI * Math.atan(Math.exp((1 - (e49.y + n2) * 2 / r2) * Math.PI)) - 90];
  }
  projectLine(e49, t2, n2, r2) {
    return e49.map((e50) => this.projectPoint(e50, t2, n2, r2));
  }
  get geometry() {
    if (this._geometry) return this._geometry;
    let e49 = this._vectorTileFeature, t2 = e49.extent * 2 ** this._z, n2 = e49.extent * this._x, r2 = e49.extent * this._y, i2 = e49.loadGeometry();
    switch (e49.type) {
      case 1: {
        let e50 = [];
        for (let t3 of i2) e50.push(t3[0]);
        let a2 = this.projectLine(e50, n2, r2, t2);
        this._geometry = e50.length === 1 ? { type: `Point`, coordinates: a2[0] } : { type: `MultiPoint`, coordinates: a2 };
        break;
      }
      case 2: {
        let e50 = i2.map((e51) => this.projectLine(e51, n2, r2, t2));
        this._geometry = e50.length === 1 ? { type: `LineString`, coordinates: e50[0] } : { type: `MultiLineString`, coordinates: e50 };
        break;
      }
      case 3: {
        let e50 = td(i2), a2 = [];
        for (let i3 of e50) a2.push(i3.map((e51) => this.projectLine(e51, n2, r2, t2)));
        this._geometry = a2.length === 1 ? { type: `Polygon`, coordinates: a2[0] } : { type: `MultiPolygon`, coordinates: a2 };
        break;
      }
      default:
        throw Error(`unknown feature type: ${e49.type}`);
    }
    return this._geometry;
  }
  set geometry(e49) {
    this._geometry = e49;
  }
  toJSON() {
    let e49 = { geometry: this.geometry };
    for (let t2 in this) t2 !== `_geometry` && t2 !== `_vectorTileFeature` && t2 !== `_x` && t2 !== `_y` && t2 !== `_z` && (e49[t2] = this[t2]);
    return e49;
  }
}, L_ = class {
  constructor(e49, t2, n2) {
    this._name = e49, this.dataBuffer = t2, typeof n2 == `number` ? this._size = n2 : (this.nullabilityBuffer = n2, this._size = n2.size());
  }
  getValue(e49) {
    return this.nullabilityBuffer && !this.nullabilityBuffer.get(e49) ? null : this.getValueFromBuffer(e49);
  }
  has(e49) {
    var _a3;
    return ((_a3 = this.nullabilityBuffer) == null ? void 0 : _a3.get(e49)) || !this.nullabilityBuffer;
  }
  get name() {
    return this._name;
  }
  get size() {
    return this._size;
  }
}, R_ = class extends L_ {
}, z_ = class extends R_ {
  getValueFromBuffer(e49) {
    return this.dataBuffer[e49];
  }
}, B_ = class extends R_ {
  getValueFromBuffer(e49) {
    return this.dataBuffer[e49];
  }
}, V_ = class extends L_ {
  constructor(e49, t2, n2, r2) {
    super(e49, t2, r2), this.delta = n2;
  }
}, H_ = class extends V_ {
  constructor(e49, t2, n2, r2, i2) {
    super(e49, i2 ? Int32Array.of(t2) : Uint32Array.of(t2), n2, r2);
  }
  getValueFromBuffer(e49) {
    return this.dataBuffer[0] + e49 * this.delta;
  }
}, U_ = class extends L_ {
  constructor(e49, t2, n2, r2) {
    super(e49, r2 ? Int32Array.of(t2) : Uint32Array.of(t2), n2);
  }
  getValueFromBuffer(e49) {
    return this.dataBuffer[0];
  }
}, W_ = class {
  constructor(e49, t2, n2, r2, i2 = 4096) {
    if (this._name = e49, this._geometryVector = t2, this._idVector = n2, this._propertyVectors = r2, this._extent = i2, e49.length === 0) throw Error(`Missing layer name`);
  }
  get name() {
    return this._name;
  }
  get idVector() {
    return this._idVector;
  }
  get geometryVector() {
    return this._geometryVector;
  }
  get propertyVectors() {
    return this._propertyVectors ?? [];
  }
  getPropertyVector(e49) {
    return this.propertyVectorsMap ||= new Map(this.propertyVectors.map((e50) => [e50.name, e50])), this.propertyVectorsMap.get(e49);
  }
  get numFeatures() {
    return this.geometryVector.numGeometries;
  }
  get extent() {
    return this._extent;
  }
  getFeatures() {
    let e49 = [], t2 = this.geometryVector.getGeometries();
    for (let n2 = 0; n2 < this.numFeatures; n2++) {
      let r2;
      if (this.idVector) {
        let e50 = this.idVector.getValue(n2);
        e50 !== null && (r2 = this.containsMaxSafeIntegerValues(this.idVector) ? Number(e50) : e50);
      }
      let i2 = { coordinates: t2[n2], type: this.geometryVector.geometryType(n2) }, a2 = {};
      for (let e50 of this.propertyVectors) {
        if (!e50) continue;
        let t3 = e50.name, r3 = e50.getValue(n2);
        r3 !== null && (a2[t3] = r3);
      }
      e49.push({ id: r2, geometry: i2, properties: a2 });
    }
    return e49;
  }
  containsMaxSafeIntegerValues(e49) {
    return e49 instanceof z_ || e49 instanceof U_ || e49 instanceof H_ || e49 instanceof B_;
  }
};
const G_ = { FEATURE: 0, VERTEX: 1 }, Y = { BOOLEAN: 0, INT_8: 1, UINT_8: 2, INT_32: 3, UINT_32: 4, INT_64: 5, UINT_64: 6, FLOAT: 7, DOUBLE: 8, STRING: 9 }, K_ = { GEOMETRY: 0, STRUCT: 1, MAP: 2 }, q_ = { ID: 0 };
var J_ = class {
  constructor(e49) {
    this.value = e49;
  }
  get() {
    return this.value;
  }
  set(e49) {
    this.value = e49;
  }
  increment() {
    return this.value++;
  }
  add(e49) {
    this.value += e49;
  }
}, X;
(function(e49) {
  e49.NONE = `NONE`, e49.DELTA = `DELTA`, e49.COMPONENTWISE_DELTA = `COMPONENTWISE_DELTA`, e49.RLE = `RLE`, e49.MORTON = `MORTON`;
})(X ||= {});
var Y_;
(function(e49) {
  e49.NONE = `NONE`, e49.FAST_PFOR = `FAST_PFOR`, e49.VARINT = `VARINT`;
})(Y_ ||= {});
const X_ = /* @__PURE__ */ new Uint32Array(33);
X_[0] = 0;
for (let e49 = 1; e49 <= 32; e49++) X_[e49] = e49 === 32 ? 4294967295 : 4294967295 >>> 32 - e49;
const Z_ = X_, Q_ = 65536;
function $_(e49, t2) {
  return e49 - e49 % t2;
}
function ev(e49) {
  return $_(e49 + 31, 32);
}
function tv(e49) {
  if (!Number.isFinite(e49) || e49 <= 0) return Q_;
  let t2 = $_(Math.floor(e49), 256);
  return t2 === 0 ? 256 : t2;
}
function nv(e49) {
  let t2 = e49 >>> 0;
  return ((t2 & 255) << 24 | (t2 & 65280) << 8 | t2 >>> 8 & 65280 | t2 >>> 24 & 255) >>> 0;
}
function rv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0;
  n2[i2++] = a2 >>> 0 & 3, n2[i2++] = a2 >>> 2 & 3, n2[i2++] = a2 >>> 4 & 3, n2[i2++] = a2 >>> 6 & 3, n2[i2++] = a2 >>> 8 & 3, n2[i2++] = a2 >>> 10 & 3, n2[i2++] = a2 >>> 12 & 3, n2[i2++] = a2 >>> 14 & 3, n2[i2++] = a2 >>> 16 & 3, n2[i2++] = a2 >>> 18 & 3, n2[i2++] = a2 >>> 20 & 3, n2[i2++] = a2 >>> 22 & 3, n2[i2++] = a2 >>> 24 & 3, n2[i2++] = a2 >>> 26 & 3, n2[i2++] = a2 >>> 28 & 3, n2[i2++] = a2 >>> 30 & 3, n2[i2++] = o2 >>> 0 & 3, n2[i2++] = o2 >>> 2 & 3, n2[i2++] = o2 >>> 4 & 3, n2[i2++] = o2 >>> 6 & 3, n2[i2++] = o2 >>> 8 & 3, n2[i2++] = o2 >>> 10 & 3, n2[i2++] = o2 >>> 12 & 3, n2[i2++] = o2 >>> 14 & 3, n2[i2++] = o2 >>> 16 & 3, n2[i2++] = o2 >>> 18 & 3, n2[i2++] = o2 >>> 20 & 3, n2[i2++] = o2 >>> 22 & 3, n2[i2++] = o2 >>> 24 & 3, n2[i2++] = o2 >>> 26 & 3, n2[i2++] = o2 >>> 28 & 3, n2[i2] = o2 >>> 30 & 3;
}
function iv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0;
  n2[i2++] = a2 >>> 0 & 7, n2[i2++] = a2 >>> 3 & 7, n2[i2++] = a2 >>> 6 & 7, n2[i2++] = a2 >>> 9 & 7, n2[i2++] = a2 >>> 12 & 7, n2[i2++] = a2 >>> 15 & 7, n2[i2++] = a2 >>> 18 & 7, n2[i2++] = a2 >>> 21 & 7, n2[i2++] = a2 >>> 24 & 7, n2[i2++] = a2 >>> 27 & 7, n2[i2++] = (a2 >>> 30 | (o2 & 1) << 2) & 7, n2[i2++] = o2 >>> 1 & 7, n2[i2++] = o2 >>> 4 & 7, n2[i2++] = o2 >>> 7 & 7, n2[i2++] = o2 >>> 10 & 7, n2[i2++] = o2 >>> 13 & 7, n2[i2++] = o2 >>> 16 & 7, n2[i2++] = o2 >>> 19 & 7, n2[i2++] = o2 >>> 22 & 7, n2[i2++] = o2 >>> 25 & 7, n2[i2++] = o2 >>> 28 & 7, n2[i2++] = (o2 >>> 31 | (s2 & 3) << 1) & 7, n2[i2++] = s2 >>> 2 & 7, n2[i2++] = s2 >>> 5 & 7, n2[i2++] = s2 >>> 8 & 7, n2[i2++] = s2 >>> 11 & 7, n2[i2++] = s2 >>> 14 & 7, n2[i2++] = s2 >>> 17 & 7, n2[i2++] = s2 >>> 20 & 7, n2[i2++] = s2 >>> 23 & 7, n2[i2++] = s2 >>> 26 & 7, n2[i2] = s2 >>> 29 & 7;
}
function av(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0;
  n2[i2++] = a2 >>> 0 & 15, n2[i2++] = a2 >>> 4 & 15, n2[i2++] = a2 >>> 8 & 15, n2[i2++] = a2 >>> 12 & 15, n2[i2++] = a2 >>> 16 & 15, n2[i2++] = a2 >>> 20 & 15, n2[i2++] = a2 >>> 24 & 15, n2[i2++] = a2 >>> 28 & 15, n2[i2++] = o2 >>> 0 & 15, n2[i2++] = o2 >>> 4 & 15, n2[i2++] = o2 >>> 8 & 15, n2[i2++] = o2 >>> 12 & 15, n2[i2++] = o2 >>> 16 & 15, n2[i2++] = o2 >>> 20 & 15, n2[i2++] = o2 >>> 24 & 15, n2[i2++] = o2 >>> 28 & 15, n2[i2++] = s2 >>> 0 & 15, n2[i2++] = s2 >>> 4 & 15, n2[i2++] = s2 >>> 8 & 15, n2[i2++] = s2 >>> 12 & 15, n2[i2++] = s2 >>> 16 & 15, n2[i2++] = s2 >>> 20 & 15, n2[i2++] = s2 >>> 24 & 15, n2[i2++] = s2 >>> 28 & 15, n2[i2++] = c2 >>> 0 & 15, n2[i2++] = c2 >>> 4 & 15, n2[i2++] = c2 >>> 8 & 15, n2[i2++] = c2 >>> 12 & 15, n2[i2++] = c2 >>> 16 & 15, n2[i2++] = c2 >>> 20 & 15, n2[i2++] = c2 >>> 24 & 15, n2[i2] = c2 >>> 28 & 15;
}
function ov(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0;
  n2[i2++] = a2 >>> 0 & 31, n2[i2++] = a2 >>> 5 & 31, n2[i2++] = a2 >>> 10 & 31, n2[i2++] = a2 >>> 15 & 31, n2[i2++] = a2 >>> 20 & 31, n2[i2++] = a2 >>> 25 & 31, n2[i2++] = (a2 >>> 30 | (o2 & 7) << 2) & 31, n2[i2++] = o2 >>> 3 & 31, n2[i2++] = o2 >>> 8 & 31, n2[i2++] = o2 >>> 13 & 31, n2[i2++] = o2 >>> 18 & 31, n2[i2++] = o2 >>> 23 & 31, n2[i2++] = (o2 >>> 28 | (s2 & 1) << 4) & 31, n2[i2++] = s2 >>> 1 & 31, n2[i2++] = s2 >>> 6 & 31, n2[i2++] = s2 >>> 11 & 31, n2[i2++] = s2 >>> 16 & 31, n2[i2++] = s2 >>> 21 & 31, n2[i2++] = s2 >>> 26 & 31, n2[i2++] = (s2 >>> 31 | (c2 & 15) << 1) & 31, n2[i2++] = c2 >>> 4 & 31, n2[i2++] = c2 >>> 9 & 31, n2[i2++] = c2 >>> 14 & 31, n2[i2++] = c2 >>> 19 & 31, n2[i2++] = c2 >>> 24 & 31, n2[i2++] = (c2 >>> 29 | (l2 & 3) << 3) & 31, n2[i2++] = l2 >>> 2 & 31, n2[i2++] = l2 >>> 7 & 31, n2[i2++] = l2 >>> 12 & 31, n2[i2++] = l2 >>> 17 & 31, n2[i2++] = l2 >>> 22 & 31, n2[i2] = l2 >>> 27 & 31;
}
function sv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0;
  n2[i2++] = a2 >>> 0 & 63, n2[i2++] = a2 >>> 6 & 63, n2[i2++] = a2 >>> 12 & 63, n2[i2++] = a2 >>> 18 & 63, n2[i2++] = a2 >>> 24 & 63, n2[i2++] = (a2 >>> 30 | (o2 & 15) << 2) & 63, n2[i2++] = o2 >>> 4 & 63, n2[i2++] = o2 >>> 10 & 63, n2[i2++] = o2 >>> 16 & 63, n2[i2++] = o2 >>> 22 & 63, n2[i2++] = (o2 >>> 28 | (s2 & 3) << 4) & 63, n2[i2++] = s2 >>> 2 & 63, n2[i2++] = s2 >>> 8 & 63, n2[i2++] = s2 >>> 14 & 63, n2[i2++] = s2 >>> 20 & 63, n2[i2++] = s2 >>> 26 & 63, n2[i2++] = c2 >>> 0 & 63, n2[i2++] = c2 >>> 6 & 63, n2[i2++] = c2 >>> 12 & 63, n2[i2++] = c2 >>> 18 & 63, n2[i2++] = c2 >>> 24 & 63, n2[i2++] = (c2 >>> 30 | (l2 & 15) << 2) & 63, n2[i2++] = l2 >>> 4 & 63, n2[i2++] = l2 >>> 10 & 63, n2[i2++] = l2 >>> 16 & 63, n2[i2++] = l2 >>> 22 & 63, n2[i2++] = (l2 >>> 28 | (u2 & 3) << 4) & 63, n2[i2++] = u2 >>> 2 & 63, n2[i2++] = u2 >>> 8 & 63, n2[i2++] = u2 >>> 14 & 63, n2[i2++] = u2 >>> 20 & 63, n2[i2] = u2 >>> 26 & 63;
}
function cv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0;
  n2[i2++] = a2 >>> 0 & 127, n2[i2++] = a2 >>> 7 & 127, n2[i2++] = a2 >>> 14 & 127, n2[i2++] = a2 >>> 21 & 127, n2[i2++] = (a2 >>> 28 | (o2 & 7) << 4) & 127, n2[i2++] = o2 >>> 3 & 127, n2[i2++] = o2 >>> 10 & 127, n2[i2++] = o2 >>> 17 & 127, n2[i2++] = o2 >>> 24 & 127, n2[i2++] = (o2 >>> 31 | (s2 & 63) << 1) & 127, n2[i2++] = s2 >>> 6 & 127, n2[i2++] = s2 >>> 13 & 127, n2[i2++] = s2 >>> 20 & 127, n2[i2++] = (s2 >>> 27 | (c2 & 3) << 5) & 127, n2[i2++] = c2 >>> 2 & 127, n2[i2++] = c2 >>> 9 & 127, n2[i2++] = c2 >>> 16 & 127, n2[i2++] = c2 >>> 23 & 127, n2[i2++] = (c2 >>> 30 | (l2 & 31) << 2) & 127, n2[i2++] = l2 >>> 5 & 127, n2[i2++] = l2 >>> 12 & 127, n2[i2++] = l2 >>> 19 & 127, n2[i2++] = (l2 >>> 26 | (u2 & 1) << 6) & 127, n2[i2++] = u2 >>> 1 & 127, n2[i2++] = u2 >>> 8 & 127, n2[i2++] = u2 >>> 15 & 127, n2[i2++] = u2 >>> 22 & 127, n2[i2++] = (u2 >>> 29 | (d2 & 15) << 3) & 127, n2[i2++] = d2 >>> 4 & 127, n2[i2++] = d2 >>> 11 & 127, n2[i2++] = d2 >>> 18 & 127, n2[i2] = d2 >>> 25 & 127;
}
function lv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0, f2 = e49[t2 + 7] >>> 0;
  n2[i2++] = a2 >>> 0 & 255, n2[i2++] = a2 >>> 8 & 255, n2[i2++] = a2 >>> 16 & 255, n2[i2++] = a2 >>> 24 & 255, n2[i2++] = o2 >>> 0 & 255, n2[i2++] = o2 >>> 8 & 255, n2[i2++] = o2 >>> 16 & 255, n2[i2++] = o2 >>> 24 & 255, n2[i2++] = s2 >>> 0 & 255, n2[i2++] = s2 >>> 8 & 255, n2[i2++] = s2 >>> 16 & 255, n2[i2++] = s2 >>> 24 & 255, n2[i2++] = c2 >>> 0 & 255, n2[i2++] = c2 >>> 8 & 255, n2[i2++] = c2 >>> 16 & 255, n2[i2++] = c2 >>> 24 & 255, n2[i2++] = l2 >>> 0 & 255, n2[i2++] = l2 >>> 8 & 255, n2[i2++] = l2 >>> 16 & 255, n2[i2++] = l2 >>> 24 & 255, n2[i2++] = u2 >>> 0 & 255, n2[i2++] = u2 >>> 8 & 255, n2[i2++] = u2 >>> 16 & 255, n2[i2++] = u2 >>> 24 & 255, n2[i2++] = d2 >>> 0 & 255, n2[i2++] = d2 >>> 8 & 255, n2[i2++] = d2 >>> 16 & 255, n2[i2++] = d2 >>> 24 & 255, n2[i2++] = f2 >>> 0 & 255, n2[i2++] = f2 >>> 8 & 255, n2[i2++] = f2 >>> 16 & 255, n2[i2] = f2 >>> 24 & 255;
}
function uv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0, f2 = e49[t2 + 7] >>> 0, p2 = e49[t2 + 8] >>> 0;
  n2[i2++] = a2 >>> 0 & 511, n2[i2++] = a2 >>> 9 & 511, n2[i2++] = a2 >>> 18 & 511, n2[i2++] = (a2 >>> 27 | (o2 & 15) << 5) & 511, n2[i2++] = o2 >>> 4 & 511, n2[i2++] = o2 >>> 13 & 511, n2[i2++] = o2 >>> 22 & 511, n2[i2++] = (o2 >>> 31 | (s2 & 255) << 1) & 511, n2[i2++] = s2 >>> 8 & 511, n2[i2++] = s2 >>> 17 & 511, n2[i2++] = (s2 >>> 26 | (c2 & 7) << 6) & 511, n2[i2++] = c2 >>> 3 & 511, n2[i2++] = c2 >>> 12 & 511, n2[i2++] = c2 >>> 21 & 511, n2[i2++] = (c2 >>> 30 | (l2 & 127) << 2) & 511, n2[i2++] = l2 >>> 7 & 511, n2[i2++] = l2 >>> 16 & 511, n2[i2++] = (l2 >>> 25 | (u2 & 3) << 7) & 511, n2[i2++] = u2 >>> 2 & 511, n2[i2++] = u2 >>> 11 & 511, n2[i2++] = u2 >>> 20 & 511, n2[i2++] = (u2 >>> 29 | (d2 & 63) << 3) & 511, n2[i2++] = d2 >>> 6 & 511, n2[i2++] = d2 >>> 15 & 511, n2[i2++] = (d2 >>> 24 | (f2 & 1) << 8) & 511, n2[i2++] = f2 >>> 1 & 511, n2[i2++] = f2 >>> 10 & 511, n2[i2++] = f2 >>> 19 & 511, n2[i2++] = (f2 >>> 28 | (p2 & 31) << 4) & 511, n2[i2++] = p2 >>> 5 & 511, n2[i2++] = p2 >>> 14 & 511, n2[i2] = p2 >>> 23 & 511;
}
function dv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0, f2 = e49[t2 + 7] >>> 0, p2 = e49[t2 + 8] >>> 0, m2 = e49[t2 + 9] >>> 0;
  n2[i2++] = a2 >>> 0 & 1023, n2[i2++] = a2 >>> 10 & 1023, n2[i2++] = a2 >>> 20 & 1023, n2[i2++] = (a2 >>> 30 | (o2 & 255) << 2) & 1023, n2[i2++] = o2 >>> 8 & 1023, n2[i2++] = o2 >>> 18 & 1023, n2[i2++] = (o2 >>> 28 | (s2 & 63) << 4) & 1023, n2[i2++] = s2 >>> 6 & 1023, n2[i2++] = s2 >>> 16 & 1023, n2[i2++] = (s2 >>> 26 | (c2 & 15) << 6) & 1023, n2[i2++] = c2 >>> 4 & 1023, n2[i2++] = c2 >>> 14 & 1023, n2[i2++] = (c2 >>> 24 | (l2 & 3) << 8) & 1023, n2[i2++] = l2 >>> 2 & 1023, n2[i2++] = l2 >>> 12 & 1023, n2[i2++] = l2 >>> 22 & 1023, n2[i2++] = u2 >>> 0 & 1023, n2[i2++] = u2 >>> 10 & 1023, n2[i2++] = u2 >>> 20 & 1023, n2[i2++] = (u2 >>> 30 | (d2 & 255) << 2) & 1023, n2[i2++] = d2 >>> 8 & 1023, n2[i2++] = d2 >>> 18 & 1023, n2[i2++] = (d2 >>> 28 | (f2 & 63) << 4) & 1023, n2[i2++] = f2 >>> 6 & 1023, n2[i2++] = f2 >>> 16 & 1023, n2[i2++] = (f2 >>> 26 | (p2 & 15) << 6) & 1023, n2[i2++] = p2 >>> 4 & 1023, n2[i2++] = p2 >>> 14 & 1023, n2[i2++] = (p2 >>> 24 | (m2 & 3) << 8) & 1023, n2[i2++] = m2 >>> 2 & 1023, n2[i2++] = m2 >>> 12 & 1023, n2[i2] = m2 >>> 22 & 1023;
}
function fv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0, f2 = e49[t2 + 7] >>> 0, p2 = e49[t2 + 8] >>> 0, m2 = e49[t2 + 9] >>> 0, h2 = e49[t2 + 10] >>> 0;
  n2[i2++] = a2 >>> 0 & 2047, n2[i2++] = a2 >>> 11 & 2047, n2[i2++] = (a2 >>> 22 | (o2 & 1) << 10) & 2047, n2[i2++] = o2 >>> 1 & 2047, n2[i2++] = o2 >>> 12 & 2047, n2[i2++] = (o2 >>> 23 | (s2 & 3) << 9) & 2047, n2[i2++] = s2 >>> 2 & 2047, n2[i2++] = s2 >>> 13 & 2047, n2[i2++] = (s2 >>> 24 | (c2 & 7) << 8) & 2047, n2[i2++] = c2 >>> 3 & 2047, n2[i2++] = c2 >>> 14 & 2047, n2[i2++] = (c2 >>> 25 | (l2 & 15) << 7) & 2047, n2[i2++] = l2 >>> 4 & 2047, n2[i2++] = l2 >>> 15 & 2047, n2[i2++] = (l2 >>> 26 | (u2 & 31) << 6) & 2047, n2[i2++] = u2 >>> 5 & 2047, n2[i2++] = u2 >>> 16 & 2047, n2[i2++] = (u2 >>> 27 | (d2 & 63) << 5) & 2047, n2[i2++] = d2 >>> 6 & 2047, n2[i2++] = d2 >>> 17 & 2047, n2[i2++] = (d2 >>> 28 | (f2 & 127) << 4) & 2047, n2[i2++] = f2 >>> 7 & 2047, n2[i2++] = f2 >>> 18 & 2047, n2[i2++] = (f2 >>> 29 | (p2 & 255) << 3) & 2047, n2[i2++] = p2 >>> 8 & 2047, n2[i2++] = p2 >>> 19 & 2047, n2[i2++] = (p2 >>> 30 | (m2 & 511) << 2) & 2047, n2[i2++] = m2 >>> 9 & 2047, n2[i2++] = m2 >>> 20 & 2047, n2[i2++] = (m2 >>> 31 | (h2 & 1023) << 1) & 2047, n2[i2++] = h2 >>> 10 & 2047, n2[i2] = h2 >>> 21 & 2047;
}
function pv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0, f2 = e49[t2 + 7] >>> 0, p2 = e49[t2 + 8] >>> 0, m2 = e49[t2 + 9] >>> 0, h2 = e49[t2 + 10] >>> 0, g2 = e49[t2 + 11] >>> 0;
  n2[i2++] = a2 >>> 0 & 4095, n2[i2++] = a2 >>> 12 & 4095, n2[i2++] = (a2 >>> 24 | (o2 & 15) << 8) & 4095, n2[i2++] = o2 >>> 4 & 4095, n2[i2++] = o2 >>> 16 & 4095, n2[i2++] = (o2 >>> 28 | (s2 & 255) << 4) & 4095, n2[i2++] = s2 >>> 8 & 4095, n2[i2++] = s2 >>> 20 & 4095, n2[i2++] = c2 >>> 0 & 4095, n2[i2++] = c2 >>> 12 & 4095, n2[i2++] = (c2 >>> 24 | (l2 & 15) << 8) & 4095, n2[i2++] = l2 >>> 4 & 4095, n2[i2++] = l2 >>> 16 & 4095, n2[i2++] = (l2 >>> 28 | (u2 & 255) << 4) & 4095, n2[i2++] = u2 >>> 8 & 4095, n2[i2++] = u2 >>> 20 & 4095, n2[i2++] = d2 >>> 0 & 4095, n2[i2++] = d2 >>> 12 & 4095, n2[i2++] = (d2 >>> 24 | (f2 & 15) << 8) & 4095, n2[i2++] = f2 >>> 4 & 4095, n2[i2++] = f2 >>> 16 & 4095, n2[i2++] = (f2 >>> 28 | (p2 & 255) << 4) & 4095, n2[i2++] = p2 >>> 8 & 4095, n2[i2++] = p2 >>> 20 & 4095, n2[i2++] = m2 >>> 0 & 4095, n2[i2++] = m2 >>> 12 & 4095, n2[i2++] = (m2 >>> 24 | (h2 & 15) << 8) & 4095, n2[i2++] = h2 >>> 4 & 4095, n2[i2++] = h2 >>> 16 & 4095, n2[i2++] = (h2 >>> 28 | (g2 & 255) << 4) & 4095, n2[i2++] = g2 >>> 8 & 4095, n2[i2] = g2 >>> 20 & 4095;
}
function mv(e49, t2, n2, r2) {
  let i2 = r2, a2 = e49[t2] >>> 0, o2 = e49[t2 + 1] >>> 0, s2 = e49[t2 + 2] >>> 0, c2 = e49[t2 + 3] >>> 0, l2 = e49[t2 + 4] >>> 0, u2 = e49[t2 + 5] >>> 0, d2 = e49[t2 + 6] >>> 0, f2 = e49[t2 + 7] >>> 0, p2 = e49[t2 + 8] >>> 0, m2 = e49[t2 + 9] >>> 0, h2 = e49[t2 + 10] >>> 0, g2 = e49[t2 + 11] >>> 0, _2 = e49[t2 + 12] >>> 0, v2 = e49[t2 + 13] >>> 0, y2 = e49[t2 + 14] >>> 0, b2 = e49[t2 + 15] >>> 0;
  n2[i2++] = a2 >>> 0 & 65535, n2[i2++] = a2 >>> 16 & 65535, n2[i2++] = o2 >>> 0 & 65535, n2[i2++] = o2 >>> 16 & 65535, n2[i2++] = s2 >>> 0 & 65535, n2[i2++] = s2 >>> 16 & 65535, n2[i2++] = c2 >>> 0 & 65535, n2[i2++] = c2 >>> 16 & 65535, n2[i2++] = l2 >>> 0 & 65535, n2[i2++] = l2 >>> 16 & 65535, n2[i2++] = u2 >>> 0 & 65535, n2[i2++] = u2 >>> 16 & 65535, n2[i2++] = d2 >>> 0 & 65535, n2[i2++] = d2 >>> 16 & 65535, n2[i2++] = f2 >>> 0 & 65535, n2[i2++] = f2 >>> 16 & 65535, n2[i2++] = p2 >>> 0 & 65535, n2[i2++] = p2 >>> 16 & 65535, n2[i2++] = m2 >>> 0 & 65535, n2[i2++] = m2 >>> 16 & 65535, n2[i2++] = h2 >>> 0 & 65535, n2[i2++] = h2 >>> 16 & 65535, n2[i2++] = g2 >>> 0 & 65535, n2[i2++] = g2 >>> 16 & 65535, n2[i2++] = _2 >>> 0 & 65535, n2[i2++] = _2 >>> 16 & 65535, n2[i2++] = v2 >>> 0 & 65535, n2[i2++] = v2 >>> 16 & 65535, n2[i2++] = y2 >>> 0 & 65535, n2[i2++] = y2 >>> 16 & 65535, n2[i2++] = b2 >>> 0 & 65535, n2[i2] = b2 >>> 16 & 65535;
}
function hv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 1, n2[i2++] = t4 >>> 1 & 1, n2[i2++] = t4 >>> 2 & 1, n2[i2++] = t4 >>> 3 & 1, n2[i2++] = t4 >>> 4 & 1, n2[i2++] = t4 >>> 5 & 1, n2[i2++] = t4 >>> 6 & 1, n2[i2++] = t4 >>> 7 & 1, n2[i2++] = t4 >>> 8 & 1, n2[i2++] = t4 >>> 9 & 1, n2[i2++] = t4 >>> 10 & 1, n2[i2++] = t4 >>> 11 & 1, n2[i2++] = t4 >>> 12 & 1, n2[i2++] = t4 >>> 13 & 1, n2[i2++] = t4 >>> 14 & 1, n2[i2++] = t4 >>> 15 & 1, n2[i2++] = t4 >>> 16 & 1, n2[i2++] = t4 >>> 17 & 1, n2[i2++] = t4 >>> 18 & 1, n2[i2++] = t4 >>> 19 & 1, n2[i2++] = t4 >>> 20 & 1, n2[i2++] = t4 >>> 21 & 1, n2[i2++] = t4 >>> 22 & 1, n2[i2++] = t4 >>> 23 & 1, n2[i2++] = t4 >>> 24 & 1, n2[i2++] = t4 >>> 25 & 1, n2[i2++] = t4 >>> 26 & 1, n2[i2++] = t4 >>> 27 & 1, n2[i2++] = t4 >>> 28 & 1, n2[i2++] = t4 >>> 29 & 1, n2[i2++] = t4 >>> 30 & 1, n2[i2++] = t4 >>> 31 & 1;
  }
}
function gv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 3, n2[i2++] = t4 >>> 2 & 3, n2[i2++] = t4 >>> 4 & 3, n2[i2++] = t4 >>> 6 & 3, n2[i2++] = t4 >>> 8 & 3, n2[i2++] = t4 >>> 10 & 3, n2[i2++] = t4 >>> 12 & 3, n2[i2++] = t4 >>> 14 & 3, n2[i2++] = t4 >>> 16 & 3, n2[i2++] = t4 >>> 18 & 3, n2[i2++] = t4 >>> 20 & 3, n2[i2++] = t4 >>> 22 & 3, n2[i2++] = t4 >>> 24 & 3, n2[i2++] = t4 >>> 26 & 3, n2[i2++] = t4 >>> 28 & 3, n2[i2++] = t4 >>> 30 & 3, n2[i2++] = r3 >>> 0 & 3, n2[i2++] = r3 >>> 2 & 3, n2[i2++] = r3 >>> 4 & 3, n2[i2++] = r3 >>> 6 & 3, n2[i2++] = r3 >>> 8 & 3, n2[i2++] = r3 >>> 10 & 3, n2[i2++] = r3 >>> 12 & 3, n2[i2++] = r3 >>> 14 & 3, n2[i2++] = r3 >>> 16 & 3, n2[i2++] = r3 >>> 18 & 3, n2[i2++] = r3 >>> 20 & 3, n2[i2++] = r3 >>> 22 & 3, n2[i2++] = r3 >>> 24 & 3, n2[i2++] = r3 >>> 26 & 3, n2[i2++] = r3 >>> 28 & 3, n2[i2++] = r3 >>> 30 & 3;
  }
}
function _v(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0, o2 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 7, n2[i2++] = t4 >>> 3 & 7, n2[i2++] = t4 >>> 6 & 7, n2[i2++] = t4 >>> 9 & 7, n2[i2++] = t4 >>> 12 & 7, n2[i2++] = t4 >>> 15 & 7, n2[i2++] = t4 >>> 18 & 7, n2[i2++] = t4 >>> 21 & 7, n2[i2++] = t4 >>> 24 & 7, n2[i2++] = t4 >>> 27 & 7, n2[i2++] = (t4 >>> 30 | (r3 & 1) << 2) & 7, n2[i2++] = r3 >>> 1 & 7, n2[i2++] = r3 >>> 4 & 7, n2[i2++] = r3 >>> 7 & 7, n2[i2++] = r3 >>> 10 & 7, n2[i2++] = r3 >>> 13 & 7, n2[i2++] = r3 >>> 16 & 7, n2[i2++] = r3 >>> 19 & 7, n2[i2++] = r3 >>> 22 & 7, n2[i2++] = r3 >>> 25 & 7, n2[i2++] = r3 >>> 28 & 7, n2[i2++] = (r3 >>> 31 | (o2 & 3) << 1) & 7, n2[i2++] = o2 >>> 2 & 7, n2[i2++] = o2 >>> 5 & 7, n2[i2++] = o2 >>> 8 & 7, n2[i2++] = o2 >>> 11 & 7, n2[i2++] = o2 >>> 14 & 7, n2[i2++] = o2 >>> 17 & 7, n2[i2++] = o2 >>> 20 & 7, n2[i2++] = o2 >>> 23 & 7, n2[i2++] = o2 >>> 26 & 7, n2[i2++] = o2 >>> 29 & 7;
  }
}
function vv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0, o2 = e49[a2++] >>> 0, s2 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 15, n2[i2++] = t4 >>> 4 & 15, n2[i2++] = t4 >>> 8 & 15, n2[i2++] = t4 >>> 12 & 15, n2[i2++] = t4 >>> 16 & 15, n2[i2++] = t4 >>> 20 & 15, n2[i2++] = t4 >>> 24 & 15, n2[i2++] = t4 >>> 28 & 15, n2[i2++] = r3 >>> 0 & 15, n2[i2++] = r3 >>> 4 & 15, n2[i2++] = r3 >>> 8 & 15, n2[i2++] = r3 >>> 12 & 15, n2[i2++] = r3 >>> 16 & 15, n2[i2++] = r3 >>> 20 & 15, n2[i2++] = r3 >>> 24 & 15, n2[i2++] = r3 >>> 28 & 15, n2[i2++] = o2 >>> 0 & 15, n2[i2++] = o2 >>> 4 & 15, n2[i2++] = o2 >>> 8 & 15, n2[i2++] = o2 >>> 12 & 15, n2[i2++] = o2 >>> 16 & 15, n2[i2++] = o2 >>> 20 & 15, n2[i2++] = o2 >>> 24 & 15, n2[i2++] = o2 >>> 28 & 15, n2[i2++] = s2 >>> 0 & 15, n2[i2++] = s2 >>> 4 & 15, n2[i2++] = s2 >>> 8 & 15, n2[i2++] = s2 >>> 12 & 15, n2[i2++] = s2 >>> 16 & 15, n2[i2++] = s2 >>> 20 & 15, n2[i2++] = s2 >>> 24 & 15, n2[i2++] = s2 >>> 28 & 15;
  }
}
function yv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0, o2 = e49[a2++] >>> 0, s2 = e49[a2++] >>> 0, c2 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 31, n2[i2++] = t4 >>> 5 & 31, n2[i2++] = t4 >>> 10 & 31, n2[i2++] = t4 >>> 15 & 31, n2[i2++] = t4 >>> 20 & 31, n2[i2++] = t4 >>> 25 & 31, n2[i2++] = (t4 >>> 30 | (r3 & 7) << 2) & 31, n2[i2++] = r3 >>> 3 & 31, n2[i2++] = r3 >>> 8 & 31, n2[i2++] = r3 >>> 13 & 31, n2[i2++] = r3 >>> 18 & 31, n2[i2++] = r3 >>> 23 & 31, n2[i2++] = (r3 >>> 28 | (o2 & 1) << 4) & 31, n2[i2++] = o2 >>> 1 & 31, n2[i2++] = o2 >>> 6 & 31, n2[i2++] = o2 >>> 11 & 31, n2[i2++] = o2 >>> 16 & 31, n2[i2++] = o2 >>> 21 & 31, n2[i2++] = o2 >>> 26 & 31, n2[i2++] = (o2 >>> 31 | (s2 & 15) << 1) & 31, n2[i2++] = s2 >>> 4 & 31, n2[i2++] = s2 >>> 9 & 31, n2[i2++] = s2 >>> 14 & 31, n2[i2++] = s2 >>> 19 & 31, n2[i2++] = s2 >>> 24 & 31, n2[i2++] = (s2 >>> 29 | (c2 & 3) << 3) & 31, n2[i2++] = c2 >>> 2 & 31, n2[i2++] = c2 >>> 7 & 31, n2[i2++] = c2 >>> 12 & 31, n2[i2++] = c2 >>> 17 & 31, n2[i2++] = c2 >>> 22 & 31, n2[i2++] = c2 >>> 27 & 31;
  }
}
function bv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0, o2 = e49[a2++] >>> 0, s2 = e49[a2++] >>> 0, c2 = e49[a2++] >>> 0, l2 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 63, n2[i2++] = t4 >>> 6 & 63, n2[i2++] = t4 >>> 12 & 63, n2[i2++] = t4 >>> 18 & 63, n2[i2++] = t4 >>> 24 & 63, n2[i2++] = (t4 >>> 30 | (r3 & 15) << 2) & 63, n2[i2++] = r3 >>> 4 & 63, n2[i2++] = r3 >>> 10 & 63, n2[i2++] = r3 >>> 16 & 63, n2[i2++] = r3 >>> 22 & 63, n2[i2++] = (r3 >>> 28 | (o2 & 3) << 4) & 63, n2[i2++] = o2 >>> 2 & 63, n2[i2++] = o2 >>> 8 & 63, n2[i2++] = o2 >>> 14 & 63, n2[i2++] = o2 >>> 20 & 63, n2[i2++] = o2 >>> 26 & 63, n2[i2++] = s2 >>> 0 & 63, n2[i2++] = s2 >>> 6 & 63, n2[i2++] = s2 >>> 12 & 63, n2[i2++] = s2 >>> 18 & 63, n2[i2++] = s2 >>> 24 & 63, n2[i2++] = (s2 >>> 30 | (c2 & 15) << 2) & 63, n2[i2++] = c2 >>> 4 & 63, n2[i2++] = c2 >>> 10 & 63, n2[i2++] = c2 >>> 16 & 63, n2[i2++] = c2 >>> 22 & 63, n2[i2++] = (c2 >>> 28 | (l2 & 3) << 4) & 63, n2[i2++] = l2 >>> 2 & 63, n2[i2++] = l2 >>> 8 & 63, n2[i2++] = l2 >>> 14 & 63, n2[i2++] = l2 >>> 20 & 63, n2[i2++] = l2 >>> 26 & 63;
  }
}
function xv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0, o2 = e49[a2++] >>> 0, s2 = e49[a2++] >>> 0, c2 = e49[a2++] >>> 0, l2 = e49[a2++] >>> 0, u2 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 127, n2[i2++] = t4 >>> 7 & 127, n2[i2++] = t4 >>> 14 & 127, n2[i2++] = t4 >>> 21 & 127, n2[i2++] = (t4 >>> 28 | (r3 & 7) << 4) & 127, n2[i2++] = r3 >>> 3 & 127, n2[i2++] = r3 >>> 10 & 127, n2[i2++] = r3 >>> 17 & 127, n2[i2++] = r3 >>> 24 & 127, n2[i2++] = (r3 >>> 31 | (o2 & 63) << 1) & 127, n2[i2++] = o2 >>> 6 & 127, n2[i2++] = o2 >>> 13 & 127, n2[i2++] = o2 >>> 20 & 127, n2[i2++] = (o2 >>> 27 | (s2 & 3) << 5) & 127, n2[i2++] = s2 >>> 2 & 127, n2[i2++] = s2 >>> 9 & 127, n2[i2++] = s2 >>> 16 & 127, n2[i2++] = s2 >>> 23 & 127, n2[i2++] = (s2 >>> 30 | (c2 & 31) << 2) & 127, n2[i2++] = c2 >>> 5 & 127, n2[i2++] = c2 >>> 12 & 127, n2[i2++] = c2 >>> 19 & 127, n2[i2++] = (c2 >>> 26 | (l2 & 1) << 6) & 127, n2[i2++] = l2 >>> 1 & 127, n2[i2++] = l2 >>> 8 & 127, n2[i2++] = l2 >>> 15 & 127, n2[i2++] = l2 >>> 22 & 127, n2[i2++] = (l2 >>> 29 | (u2 & 15) << 3) & 127, n2[i2++] = u2 >>> 4 & 127, n2[i2++] = u2 >>> 11 & 127, n2[i2++] = u2 >>> 18 & 127, n2[i2++] = u2 >>> 25 & 127;
  }
}
function Sv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 8; t3++) {
    let t4 = e49[a2++] >>> 0, r3 = e49[a2++] >>> 0, o2 = e49[a2++] >>> 0, s2 = e49[a2++] >>> 0, c2 = e49[a2++] >>> 0, l2 = e49[a2++] >>> 0, u2 = e49[a2++] >>> 0, d2 = e49[a2++] >>> 0;
    n2[i2++] = t4 >>> 0 & 255, n2[i2++] = t4 >>> 8 & 255, n2[i2++] = t4 >>> 16 & 255, n2[i2++] = t4 >>> 24 & 255, n2[i2++] = r3 >>> 0 & 255, n2[i2++] = r3 >>> 8 & 255, n2[i2++] = r3 >>> 16 & 255, n2[i2++] = r3 >>> 24 & 255, n2[i2++] = o2 >>> 0 & 255, n2[i2++] = o2 >>> 8 & 255, n2[i2++] = o2 >>> 16 & 255, n2[i2++] = o2 >>> 24 & 255, n2[i2++] = s2 >>> 0 & 255, n2[i2++] = s2 >>> 8 & 255, n2[i2++] = s2 >>> 16 & 255, n2[i2++] = s2 >>> 24 & 255, n2[i2++] = c2 >>> 0 & 255, n2[i2++] = c2 >>> 8 & 255, n2[i2++] = c2 >>> 16 & 255, n2[i2++] = c2 >>> 24 & 255, n2[i2++] = l2 >>> 0 & 255, n2[i2++] = l2 >>> 8 & 255, n2[i2++] = l2 >>> 16 & 255, n2[i2++] = l2 >>> 24 & 255, n2[i2++] = u2 >>> 0 & 255, n2[i2++] = u2 >>> 8 & 255, n2[i2++] = u2 >>> 16 & 255, n2[i2++] = u2 >>> 24 & 255, n2[i2++] = d2 >>> 0 & 255, n2[i2++] = d2 >>> 8 & 255, n2[i2++] = d2 >>> 16 & 255, n2[i2++] = d2 >>> 24 & 255;
  }
}
function Cv(e49, t2, n2, r2) {
  let i2 = r2, a2 = t2;
  for (let t3 = 0; t3 < 128; t3++) {
    let t4 = e49[a2++] >>> 0;
    n2[i2++] = t4 & 65535, n2[i2++] = t4 >>> 16 & 65535;
  }
}
function wv(e49, t2, n2, r2, i2) {
  let a2 = Z_[i2] >>> 0, o2 = t2, s2 = 0, c2 = e49[o2] >>> 0, l2 = r2;
  for (let t3 = 0; t3 < 8; t3++) {
    for (let t4 = 0; t4 < 32; t4++) if (s2 + i2 <= 32) {
      let r3 = c2 >>> s2 & a2;
      n2[l2 + t4] = r3 | 0, s2 += i2, s2 === 32 && (s2 = 0, o2++, t4 !== 31 && (c2 = e49[o2] >>> 0));
    } else {
      let r3 = 32 - s2, u2 = c2 >>> s2;
      o2++, c2 = e49[o2] >>> 0;
      let d2 = i2 - r3, f2 = -1 >>> 32 - d2 >>> 0, p2 = (u2 | (c2 & f2) << r3) & a2;
      n2[l2 + t4] = p2 | 0, s2 = d2;
    }
    l2 += 32, s2 = 0, t3 < 7 && (c2 = e49[o2] >>> 0);
  }
}
const Tv = tv(Q_), Ev = 3 * Tv / 256 + Tv | 0;
function Dv() {
  let e49 = new Uint8Array(Ev);
  return { dataToBePacked: Array(33), dataPointers: /* @__PURE__ */ new Int32Array(33), byteContainer: e49, byteContainerI32: new Int32Array(e49.buffer, e49.byteOffset, e49.byteLength >>> 2), exceptionSizes: /* @__PURE__ */ new Int32Array(33) };
}
function Ov(e49 = 16) {
  if (e49 < 0) throw RangeError(`initialEncodedWordCapacity must be >= 0, got ${e49}`);
  let t2 = Math.max(16, e49 | 0);
  return { encodedWords: new Uint32Array(t2), decoderWorkspace: Dv() };
}
function kv(e49, t2) {
  if (t2 <= e49.encodedWords.length) return e49.encodedWords;
  let n2 = new Uint32Array(Math.max(16, t2 * 2));
  return e49.encodedWords = n2, n2;
}
function Av(e49, t2, n2, r2) {
  r2.byteContainer.length < n2 && (r2.byteContainer = new Uint8Array(n2 * 2), r2.byteContainerI32 = void 0);
  let i2 = r2.byteContainer, a2 = n2 >>> 2;
  if (i2.byteOffset & 3) for (let n3 = 0; n3 < a2; n3 = n3 + 1 | 0) {
    let r3 = e49[t2 + n3 | 0] | 0, a3 = n3 << 2;
    i2[a3] = r3 & 255, i2[a3 + 1 | 0] = r3 >>> 8 & 255, i2[a3 + 2 | 0] = r3 >>> 16 & 255, i2[a3 + 3 | 0] = r3 >>> 24 & 255;
  }
  else {
    let n3 = r2.byteContainerI32;
    (!n3 || n3.buffer !== i2.buffer || n3.byteOffset !== i2.byteOffset || n3.length < a2) && (n3 = r2.byteContainerI32 = new Int32Array(i2.buffer, i2.byteOffset, i2.byteLength >>> 2)), n3.set(e49.subarray(t2, t2 + a2));
  }
  let o2 = n2 & 3;
  if (o2 > 0) {
    let n3 = e49[t2 + a2 | 0] | 0, r3 = a2 << 2;
    for (let e50 = 0; e50 < o2; e50 = e50 + 1 | 0) i2[r3 + e50 | 0] = n3 >>> (e50 << 3) & 255;
  }
  return i2;
}
function jv(e49, t2, n2) {
  let r2 = e49[t2++] | 0, i2 = n2.dataToBePacked;
  for (let a2 = 2; a2 <= 32; a2 = a2 + 1 | 0) {
    if (!(r2 >>> a2 - 1 & 1)) continue;
    if (t2 >= e49.length) throw Error(`FastPFOR decode: truncated exception stream header (bitWidth=${a2}, streamWordIndex=${t2}, needWords=1, availableWords=${e49.length - t2}, encodedWords=${e49.length})`);
    let o2 = e49[t2++] >>> 0, s2 = ev(o2), c2 = o2 * a2 + 31 >>> 5;
    if (t2 + c2 > e49.length) throw Error(`FastPFOR decode: truncated exception stream (bitWidth=${a2}, size=${o2}, streamWordIndex=${t2}, needWords=${c2}, availableWords=${e49.length - t2}, encodedWords=${e49.length})`);
    let l2 = i2[a2];
    (!l2 || l2.length < s2) && (l2 = i2[a2] = new Uint32Array(s2));
    let u2 = 0;
    for (; u2 < o2; u2 = u2 + 32 | 0) Vv(e49, t2, l2, u2, a2), t2 = t2 + a2 | 0;
    let d2 = u2 - o2 | 0;
    t2 = t2 - (d2 * a2 >>> 5) | 0, n2.exceptionSizes[a2] = o2;
  }
  return t2;
}
function Mv(e49, t2, n2, r2, i2) {
  switch (i2) {
    case 1:
      hv(e49, t2, n2, r2);
      break;
    case 2:
      gv(e49, t2, n2, r2);
      break;
    case 3:
      _v(e49, t2, n2, r2);
      break;
    case 4:
      vv(e49, t2, n2, r2);
      break;
    case 5:
      yv(e49, t2, n2, r2);
      break;
    case 6:
      bv(e49, t2, n2, r2);
      break;
    case 7:
      xv(e49, t2, n2, r2);
      break;
    case 8:
      Sv(e49, t2, n2, r2);
      break;
    case 16:
      Cv(e49, t2, n2, r2);
      break;
    default:
      wv(e49, t2, n2, r2, i2);
  }
  return t2 + (i2 << 3) | 0;
}
function Nv(e49, t2, n2, r2) {
  if (n2 + 2 > t2) throw Error(`FastPFOR decode: byteContainer underflow at block=${r2} (need 2 bytes for [bitWidth, exceptionCount], bytePos=${n2}, byteSize=${t2})`);
  let i2 = e49[n2++], a2 = e49[n2++];
  if (i2 > 32) throw Error(`FastPFOR decode: invalid bitWidth=${i2} at block=${r2} (expected 0..32). This likely indicates corrupted or truncated input.`);
  return { bitWidth: i2, exceptionCount: a2, bytePosIn: n2 };
}
function Pv(e49, t2, n2, r2, i2, a2) {
  if (n2 + 1 > t2) throw Error(`FastPFOR decode: exception header underflow at block=${a2} (need 1 byte for maxBits, bytePos=${n2}, byteSize=${t2})`);
  let o2 = e49[n2++];
  if (o2 < r2 || o2 > 32) throw Error(`FastPFOR decode: invalid maxBits=${o2} at block=${a2} (bitWidth=${r2}, expected ${r2}..32)`);
  let s2 = o2 - r2 | 0;
  if (s2 < 1 || s2 > 32) throw Error(`FastPFOR decode: invalid exceptionBitWidth=${s2} at block=${a2} (bitWidth=${r2}, maxBits=${o2})`);
  if (n2 + i2 > t2) throw Error(`FastPFOR decode: exception positions underflow at block=${a2} (need=${i2}, have=${t2 - n2})`);
  return { maxBits: o2, exceptionBitWidth: s2, bytePosIn: n2 };
}
function Fv(e49, t2, n2, r2, i2, a2, o2, s2, c2) {
  let { maxBits: l2, exceptionBitWidth: u2, bytePosIn: d2 } = Pv(i2, a2, o2, n2, r2, c2);
  if (o2 = d2, u2 === 1) {
    let a3 = 1 << n2;
    for (let n3 = 0; n3 < r2; n3 = n3 + 1 | 0) {
      let n4 = i2[o2++];
      e49[n4 + t2 | 0] |= a3;
    }
    return o2;
  }
  let f2 = s2.dataToBePacked[u2];
  if (!f2) throw Error(`FastPFOR decode: missing exception stream for exceptionBitWidth=${u2} (bitWidth=${n2}, maxBits=${l2}) at block ${c2}`);
  let p2 = s2.dataPointers, m2 = p2[u2] | 0, h2 = s2.exceptionSizes[u2] | 0;
  if (m2 + r2 > h2) throw Error(`FastPFOR decode: exception stream overflow for exceptionBitWidth=${u2} (ptr=${m2}, need ${r2}, size=${h2}) at block ${c2}`);
  for (let a3 = 0; a3 < r2; a3 = a3 + 1 | 0) {
    let r3 = i2[o2++], a4 = f2[m2++] | 0;
    e49[r3 + t2 | 0] |= a4 << n2;
  }
  return p2[u2] = m2, o2;
}
function Iv(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2) {
  let u2 = n2 | 0, d2 = 0;
  for (let t3 = 0; t3 < o2; t3 = t3 + 1 | 0) {
    let n3 = Nv(s2, c2, d2, t3);
    d2 = n3.bytePosIn;
    let r3 = n3.bitWidth, o3 = n3.exceptionCount, f2 = a2 + t3 * 256 | 0;
    switch (r3) {
      case 0:
        i2.fill(0, f2, f2 + 256);
        break;
      case 32:
        for (let t4 = 0; t4 < 256; t4 = t4 + 1 | 0) i2[f2 + t4 | 0] = e49[u2 + t4 | 0] | 0;
        u2 = u2 + 256 | 0;
        break;
      default:
        u2 = Mv(e49, u2, i2, f2, r3);
    }
    o3 > 0 && (d2 = Fv(i2, f2, r3, o3, s2, c2, d2, l2, t3));
  }
  if (u2 !== r2) throw Error(`FastPFOR decode: packed region mismatch (pageStart=${t2}, packedStart=${n2}, consumedPackedEnd=${u2}, expectedPackedEnd=${r2}, packedWords=${r2 - n2}, encoded.length=${e49.length})`);
}
function Lv(e49, t2, n2, r2, i2, a2) {
  let o2 = n2 | 0, s2 = e49[o2] | 0;
  if (s2 <= 0 || o2 + s2 > e49.length - 1) throw Error(`FastPFOR decode: invalid whereMeta=${s2} at pageStart=${o2} (expected > 0 and pageStart+whereMeta < encoded.length=${e49.length})`);
  let c2 = o2 + 1 | 0, l2 = o2 + s2 | 0, u2 = e49[l2] >>> 0, d2 = u2 + 3 >>> 2, f2 = l2 + 1, p2 = f2 + d2;
  if (p2 >= e49.length) throw Error(`FastPFOR decode: invalid byteSize=${u2} (metaInts=${d2}, pageStart=${o2}, packedEnd=${l2}, byteContainerStart=${f2}) causes bitmapPos=${p2} out of bounds (encoded.length=${e49.length})`);
  let m2 = Av(e49, f2, u2, a2), h2 = u2, g2 = jv(e49, p2, a2);
  return a2.dataPointers.fill(0), Iv(e49, o2, c2, l2, t2, r2 | 0, i2 / 256 | 0, m2, h2, a2), g2;
}
function Rv(e49, t2, n2, r2, i2, a2) {
  let o2 = r2 + $_(i2, 256), s2 = r2, c2 = n2;
  for (; s2 !== o2; ) {
    let n3 = Math.min(Tv, o2 - s2);
    c2 = Lv(e49, t2, c2, s2, n3, a2), s2 = s2 + n3 | 0;
  }
  return c2;
}
function zv(e49, t2, n2, r2, i2, a2) {
  if (a2 === 0) return t2;
  let o2 = 0, s2 = t2, c2 = t2 + n2, l2 = i2, u2 = i2, d2 = i2 + a2, f2 = 0, p2 = 0;
  for (; s2 < c2 && u2 < d2; ) {
    let t3 = e49[s2] >>> o2 & 255;
    if (o2 += 8, s2 += o2 >>> 5, o2 &= 31, f2 |= (t3 & 127) << p2, t3 & 128) r2[u2++] = f2 | 0, f2 = 0, p2 = 0;
    else if (p2 += 7, p2 > 28) throw Error(`FastPFOR VByte: unterminated value (expected MSB=1 terminator within 5 bytes; shift=${p2}, partial=${f2}, decoded=${u2 - l2}/${a2}, inPos=${s2}, inEnd=${c2})`);
  }
  if (u2 !== d2) throw Error(`FastPFOR VByte: truncated stream (decoded=${u2 - l2}, expected=${a2}, consumedWords=${s2 - t2}/${n2}, vbyteStart=${t2}, vbyteEnd=${c2})`);
  return s2;
}
function Bv(e49, t2, n2) {
  let r2 = 0, i2 = 0, a2 = new Uint32Array(t2), o2 = n2 ?? Dv();
  if (e49.length > 0) {
    let t3 = e49[r2] | 0;
    if (r2 = r2 + 1 | 0, t3 & 255) throw Error(`FastPFOR decode: invalid alignedLength=${t3} (expected multiple of 256)`);
    if (i2 + t3 > a2.length) throw Error(`FastPFOR decode: output buffer too small (outPos=${i2}, alignedLength=${t3}, out.length=${a2.length})`);
    r2 = Rv(e49, a2, r2, i2, t3, o2), i2 = i2 + t3 | 0;
  }
  let s2 = e49.length - r2 | 0, c2 = t2 - i2 | 0;
  return zv(e49, r2, s2, a2, i2, c2), a2;
}
function Vv(e49, t2, n2, r2, i2) {
  switch (i2) {
    case 2:
      rv(e49, t2, n2, r2);
      return;
    case 3:
      iv(e49, t2, n2, r2);
      return;
    case 4:
      av(e49, t2, n2, r2);
      return;
    case 5:
      ov(e49, t2, n2, r2);
      return;
    case 6:
      sv(e49, t2, n2, r2);
      return;
    case 7:
      cv(e49, t2, n2, r2);
      return;
    case 8:
      lv(e49, t2, n2, r2);
      return;
    case 9:
      uv(e49, t2, n2, r2);
      return;
    case 10:
      dv(e49, t2, n2, r2);
      return;
    case 11:
      fv(e49, t2, n2, r2);
      return;
    case 12:
      pv(e49, t2, n2, r2);
      return;
    case 16:
      mv(e49, t2, n2, r2);
      return;
    case 32:
      for (let i3 = 0; i3 < 32; i3 = i3 + 1 | 0) n2[r2 + i3 | 0] = e49[t2 + i3 | 0] | 0;
      return;
  }
  let a2 = Z_[i2] >>> 0, o2 = t2, s2 = 0, c2 = e49[o2] >>> 0;
  for (let t3 = 0; t3 < 32; t3++) if (s2 + i2 <= 32) {
    let l2 = c2 >>> s2 & a2;
    n2[r2 + t3] = l2 | 0, s2 += i2, s2 === 32 && (s2 = 0, o2++, t3 !== 31 && (c2 = e49[o2] >>> 0));
  } else {
    let l2 = 32 - s2, u2 = c2 >>> s2;
    o2++, c2 = e49[o2] >>> 0;
    let d2 = Z_[i2 - l2] >>> 0, f2 = (u2 | (c2 & d2) << l2) & a2;
    n2[r2 + t3] = f2 | 0, s2 = i2 - l2;
  }
}
function Hv(e49, t2, n2, r2) {
  if (t2 < 0 || n2 < 0 || t2 + n2 > e49.length) throw RangeError(`decodeBigEndianInt32sInto: out of bounds (offset=${t2}, byteLength=${n2}, bytes.length=${e49.length})`);
  let i2 = Math.floor(n2 / 4), a2 = n2 % 4 != 0, o2 = a2 ? i2 + 1 : i2;
  if (r2.length < o2) throw RangeError(`decodeBigEndianInt32sInto: out.length=${r2.length} < ${o2}`);
  if (i2 > 0) {
    let n3 = e49.byteOffset + t2;
    if (n3 & 3) for (let n4 = 0; n4 < i2; n4++) {
      let i3 = t2 + n4 * 4;
      r2[n4] = e49[i3] << 24 | e49[i3 + 1] << 16 | e49[i3 + 2] << 8 | e49[i3 + 3] | 0;
    }
    else {
      let t3 = new Uint32Array(e49.buffer, n3, i2);
      for (let e50 = 0; e50 < i2; e50++) r2[e50] = nv(t3[e50]) | 0;
    }
  }
  if (a2) {
    let a3 = t2 + i2 * 4, o3 = n2 - i2 * 4, s2 = 0;
    for (let t3 = 0; t3 < o3; t3++) s2 |= e49[a3 + t3] << 24 - t3 * 8;
    r2[i2] = s2 | 0;
  }
  return o2;
}
function Uv(e49, t2, n2) {
  let r2 = new Uint32Array(n2), i2 = 0, a2 = t2.get();
  for (let t3 = 0; t3 < r2.length; t3++) {
    let t4 = e49[a2++], n3 = t4 & 127;
    if (t4 < 128) {
      r2[i2++] = n3;
      continue;
    }
    if (t4 = e49[a2++], n3 |= (t4 & 127) << 7, t4 < 128) {
      r2[i2++] = n3;
      continue;
    }
    if (t4 = e49[a2++], n3 |= (t4 & 127) << 14, t4 < 128) {
      r2[i2++] = n3;
      continue;
    }
    if (t4 = e49[a2++], n3 |= (t4 & 127) << 21, t4 < 128) {
      r2[i2++] = n3;
      continue;
    }
    t4 = e49[a2++], n3 |= (t4 & 15) << 28, r2[i2++] = n3;
  }
  return t2.set(a2), r2;
}
function Wv(e49, t2, n2) {
  let r2 = new BigUint64Array(n2);
  for (let n3 = 0; n3 < r2.length; n3++) r2[n3] = Gv(e49, t2);
  return r2;
}
function Gv(e49, t2) {
  let n2 = 0n, r2 = 0, i2 = t2.get();
  for (; i2 < e49.length; ) {
    let t3 = e49[i2++];
    if (n2 |= BigInt(t3 & 127) << BigInt(r2), !(t3 & 128)) break;
    if (r2 += 7, r2 >= 64) throw Error(`Varint too long`);
  }
  return t2.set(i2), n2;
}
function Kv(e49, t2, n2) {
  let r2 = new Float64Array(n2);
  for (let i2 = 0; i2 < n2; i2++) r2[i2] = qv(e49, t2);
  return r2;
}
function qv(e49, t2) {
  let n2, r2;
  return r2 = e49[t2.get()], t2.increment(), n2 = r2 & 127, r2 < 128 || (r2 = e49[t2.get()], t2.increment(), n2 |= (r2 & 127) << 7, r2 < 128) || (r2 = e49[t2.get()], t2.increment(), n2 |= (r2 & 127) << 14, r2 < 128) || (r2 = e49[t2.get()], t2.increment(), n2 |= (r2 & 127) << 21, r2 < 128) ? n2 : (r2 = e49[t2.get()], n2 |= (r2 & 15) << 28, Jv(n2, e49, t2));
}
function Jv(e49, t2, n2) {
  let r2, i2;
  if (i2 = t2[n2.get()], n2.increment(), r2 = (i2 & 112) >> 4, i2 < 128 || (i2 = t2[n2.get()], n2.increment(), r2 |= (i2 & 127) << 3, i2 < 128) || (i2 = t2[n2.get()], n2.increment(), r2 |= (i2 & 127) << 10, i2 < 128) || (i2 = t2[n2.get()], n2.increment(), r2 |= (i2 & 127) << 17, i2 < 128) || (i2 = t2[n2.get()], n2.increment(), r2 |= (i2 & 127) << 24, i2 < 128) || (i2 = t2[n2.get()], n2.increment(), r2 |= (i2 & 1) << 31, i2 < 128)) return r2 * 4294967296 + (e49 >>> 0);
  throw Error(`Expected varint not more than 10 bytes`);
}
function Yv(e49, t2, n2, r2) {
  return Xv(e49, t2, n2, r2, Ov(n2 >>> 2));
}
function Xv(e49, t2, n2, r2, i2) {
  let a2 = r2.get();
  if (n2 & 3) throw Error(`FastPFOR: invalid encodedByteLength=${n2} at offset=${a2} (encodedBytes.length=${e49.length}; expected a multiple of 4 bytes for an int32 big-endian word stream)`);
  let o2 = n2 >>> 2, s2 = kv(i2, o2);
  Hv(e49, a2, n2, s2);
  let c2 = Bv(s2.subarray(0, o2), t2, i2.decoderWorkspace);
  return r2.add(n2), c2;
}
function Z(e49) {
  return e49 >>> 1 ^ -(e49 & 1);
}
function Zv(e49) {
  return e49 >> 1n ^ -(e49 & 1n);
}
function Qv(e49) {
  return e49 % 2 == 1 ? (e49 + 1) / -2 : e49 / 2;
}
function $v(e49) {
  let t2 = new Int32Array(e49.length);
  for (let n2 = 0; n2 < e49.length; n2++) t2[n2] = Z(e49[n2]);
  return t2;
}
function ey(e49) {
  let t2 = new BigInt64Array(e49.length);
  for (let n2 = 0; n2 < e49.length; n2++) t2[n2] = Zv(e49[n2]);
  return t2;
}
function ty(e49) {
  for (let t2 = 0; t2 < e49.length; t2++) e49[t2] = Qv(e49[t2]);
}
function ny(e49, t2, n2) {
  if (n2 === void 0) {
    n2 = 0;
    for (let r3 = 0; r3 < t2; r3++) n2 += e49[r3];
  }
  let r2 = new Uint32Array(n2), i2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let a2 = e49[n3], o2 = e49[n3 + t2];
    r2.fill(o2, i2, i2 + a2), i2 += a2;
  }
  return r2;
}
function ry(e49, t2, n2) {
  if (n2 === void 0) {
    n2 = 0;
    for (let r3 = 0; r3 < t2; r3++) n2 += Number(e49[r3]);
  }
  let r2 = new BigUint64Array(n2), i2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let a2 = Number(e49[n3]), o2 = e49[n3 + t2];
    r2.fill(o2, i2, i2 + a2), i2 += a2;
  }
  return r2;
}
function iy(e49, t2, n2) {
  let r2 = new Float64Array(n2), i2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let a2 = e49[n3], o2 = e49[n3 + t2];
    r2.fill(o2, i2, i2 + a2), i2 += a2;
  }
  return r2;
}
function ay(e49) {
  let t2 = new Int32Array(e49.length);
  t2[0] = Z(e49[0]);
  let n2 = e49.length / 4 * 4, r2 = 1;
  if (n2 >= 4) for (; r2 < n2 - 4; r2 += 4) {
    let n3 = e49[r2], i2 = e49[r2 + 1], a2 = e49[r2 + 2], o2 = e49[r2 + 3];
    t2[r2] = Z(n3) + t2[r2 - 1], t2[r2 + 1] = Z(i2) + t2[r2], t2[r2 + 2] = Z(a2) + t2[r2 + 1], t2[r2 + 3] = Z(o2) + t2[r2 + 2];
  }
  for (; r2 !== e49.length; ++r2) t2[r2] = Z(e49[r2]) + t2[r2 - 1];
  return t2;
}
function oy(e49) {
  let t2 = new BigInt64Array(e49.length);
  t2[0] = Zv(e49[0]);
  let n2 = e49.length / 4 * 4, r2 = 1;
  if (n2 >= 4) for (; r2 < n2 - 4; r2 += 4) {
    let n3 = e49[r2], i2 = e49[r2 + 1], a2 = e49[r2 + 2], o2 = e49[r2 + 3];
    t2[r2] = Zv(n3) + t2[r2 - 1], t2[r2 + 1] = Zv(i2) + t2[r2], t2[r2 + 2] = Zv(a2) + t2[r2 + 1], t2[r2 + 3] = Zv(o2) + t2[r2 + 2];
  }
  for (; r2 !== t2.length; ++r2) t2[r2] = Zv(e49[r2]) + t2[r2 - 1];
  return t2;
}
function sy(e49) {
  e49[0] = Qv(e49[0]);
  let t2 = e49.length / 4 * 4, n2 = 1;
  if (t2 >= 4) for (; n2 < t2 - 4; n2 += 4) {
    let t3 = e49[n2], r2 = e49[n2 + 1], i2 = e49[n2 + 2], a2 = e49[n2 + 3];
    e49[n2] = Qv(t3) + e49[n2 - 1], e49[n2 + 1] = Qv(r2) + e49[n2], e49[n2 + 2] = Qv(i2) + e49[n2 + 1], e49[n2 + 3] = Qv(a2) + e49[n2 + 2];
  }
  for (; n2 !== e49.length; ++n2) e49[n2] = Qv(e49[n2]) + e49[n2 - 1];
}
function cy(e49, t2, n2) {
  if (n2 === void 0) {
    n2 = 0;
    for (let r3 = 0; r3 < t2; r3++) n2 += e49[r3];
  }
  let r2 = new Int32Array(n2), i2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let a2 = e49[n3], o2 = e49[n3 + t2];
    o2 = Z(o2), r2.fill(o2, i2, i2 + a2), i2 += a2;
  }
  return r2;
}
function ly(e49, t2, n2) {
  if (n2 === void 0) {
    n2 = 0;
    for (let r3 = 0; r3 < t2; r3++) n2 += Number(e49[r3]);
  }
  let r2 = new BigInt64Array(n2), i2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let a2 = Number(e49[n3]), o2 = e49[n3 + t2];
    o2 = Zv(o2), r2.fill(o2, i2, i2 + a2), i2 += a2;
  }
  return r2;
}
function uy(e49, t2, n2) {
  let r2 = new Float64Array(n2), i2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let a2 = e49[n3], o2 = e49[n3 + t2];
    o2 = Qv(o2), r2.fill(o2, i2, i2 + a2), i2 += a2;
  }
  return r2;
}
function dy(e49) {
  let t2 = e49.length / 4 * 4, n2 = 1;
  if (t2 >= 4) for (let r2 = e49[0]; n2 < t2 - 4; n2 += 4) r2 = e49[n2] += r2, r2 = e49[n2 + 1] += r2, r2 = e49[n2 + 2] += r2, r2 = e49[n2 + 3] += r2;
  for (; n2 !== e49.length; ) e49[n2] += e49[n2 - 1], ++n2;
}
function fy(e49) {
  let t2 = 0;
  for (let n2 = 0; n2 < e49.length; n2++) e49[n2] += t2, t2 = e49[n2];
}
function py(e49) {
  if (e49.length < 2) return new Int32Array(e49);
  let t2 = new Int32Array(e49.length);
  t2[0] = Z(e49[0]), t2[1] = Z(e49[1]);
  let n2 = e49.length / 4 * 4, r2 = 2;
  if (n2 >= 4) for (; r2 < n2 - 4; r2 += 4) {
    let n3 = e49[r2], i2 = e49[r2 + 1], a2 = e49[r2 + 2], o2 = e49[r2 + 3];
    t2[r2] = Z(n3) + t2[r2 - 2], t2[r2 + 1] = Z(i2) + t2[r2 - 1], t2[r2 + 2] = Z(a2) + t2[r2], t2[r2 + 3] = Z(o2) + t2[r2 + 1];
  }
  for (; r2 !== e49.length; r2 += 2) t2[r2] = Z(e49[r2]) + t2[r2 - 2], t2[r2 + 1] = Z(e49[r2 + 1]) + t2[r2 - 1];
  return t2;
}
function my(e49, t2, n2, r2) {
  if (e49.length < 2) return new Int32Array(e49);
  let i2 = new Int32Array(e49.length), a2 = Z(e49[0]), o2 = Z(e49[1]);
  i2[0] = hy(Math.round(a2 * t2), n2, r2), i2[1] = hy(Math.round(o2 * t2), n2, r2);
  let s2 = e49.length / 16, c2 = 2;
  if (s2 >= 4) for (; c2 < s2 - 4; c2 += 4) {
    let s3 = e49[c2], l2 = e49[c2 + 1], u2 = Z(s3) + a2, d2 = Z(l2) + o2;
    i2[c2] = hy(Math.round(u2 * t2), n2, r2), i2[c2 + 1] = hy(Math.round(d2 * t2), n2, r2);
    let f2 = e49[c2 + 2], p2 = e49[c2 + 3];
    a2 = Z(f2) + u2, o2 = Z(p2) + d2, i2[c2 + 2] = hy(Math.round(a2 * t2), n2, r2), i2[c2 + 3] = hy(Math.round(o2 * t2), n2, r2);
  }
  for (; c2 !== e49.length; c2 += 2) a2 += Z(e49[c2]), o2 += Z(e49[c2 + 1]), i2[c2] = hy(Math.round(a2 * t2), n2, r2), i2[c2 + 1] = hy(Math.round(o2 * t2), n2, r2);
  return i2;
}
function hy(e49, t2, n2) {
  return Math.min(n2, Math.max(t2, e49));
}
function gy(e49) {
  let t2 = new Int32Array(e49.length + 1);
  t2[0] = 0, t2[1] = Z(e49[0]);
  let n2 = t2[1];
  for (let r2 = 2; r2 !== t2.length; ++r2) {
    let i2 = e49[r2 - 1], a2 = Z(i2);
    n2 += a2, t2[r2] = t2[r2 - 1] + n2;
  }
  return new Uint32Array(t2);
}
function _y(e49, t2, n2) {
  let r2 = new Int32Array(n2 + 1);
  r2[0] = 0;
  let i2 = 1, a2 = r2[0];
  for (let n3 = 0; n3 < t2; n3++) {
    let o2 = e49[n3], s2 = e49[n3 + t2];
    s2 = Z(s2);
    for (let e50 = i2; e50 < i2 + o2; e50++) r2[e50] = s2 + a2, a2 = r2[e50];
    i2 += o2;
  }
  return r2;
}
function vy(e49, t2, n2) {
  let r2 = new Uint32Array(n2 + 1);
  r2[0] = 0;
  let i2 = 1, a2 = r2[0];
  for (let n3 = 0; n3 < t2; n3++) {
    let o2 = e49[n3], s2 = e49[n3 + t2];
    for (let e50 = i2; e50 < i2 + o2; e50++) r2[e50] = s2 + a2, a2 = r2[e50];
    i2 += o2;
  }
  return r2;
}
function yy(e49, t2, n2) {
  let r2 = new Int32Array(n2), i2 = 0, a2 = 0;
  for (let n3 = 0; n3 < t2; n3++) {
    let o2 = e49[n3], s2 = e49[n3 + t2], c2 = Z(s2);
    for (let e50 = 0; e50 < o2; e50++) a2 += c2, r2[i2++] = a2;
  }
  return r2;
}
function by(e49, t2, n2) {
  let r2 = new BigInt64Array(n2), i2 = 0, a2 = 0n;
  for (let n3 = 0; n3 < t2; n3++) {
    let o2 = Number(e49[n3]), s2 = e49[n3 + t2], c2 = Zv(s2);
    for (let e50 = 0; e50 < o2; e50++) a2 += c2, r2[i2++] = a2;
  }
  return r2;
}
function xy(e49) {
  let t2 = new Uint32Array(e49.length);
  t2[0] = Z(e49[0]) >>> 0;
  for (let n2 = 1; n2 < e49.length; n2++) t2[n2] = t2[n2 - 1] + Z(e49[n2]) >>> 0;
  return t2;
}
function Sy(e49) {
  let t2 = new BigUint64Array(e49.length);
  t2[0] = BigInt.asUintN(64, Zv(e49[0]));
  for (let n2 = 1; n2 < e49.length; n2++) t2[n2] = BigInt.asUintN(64, t2[n2 - 1] + Zv(e49[n2]));
  return t2;
}
function Cy(e49) {
  if (e49.length < 2) return new Uint32Array(e49);
  let t2 = new Uint32Array(e49.length);
  t2[0] = Z(e49[0]) >>> 0, t2[1] = Z(e49[1]) >>> 0;
  for (let n2 = 2; n2 < e49.length; n2 += 2) t2[n2] = t2[n2 - 2] + Z(e49[n2]) >>> 0, t2[n2 + 1] = t2[n2 - 1] + Z(e49[n2 + 1]) >>> 0;
  return t2;
}
function wy(e49, t2, n2, r2) {
  let i2 = my(e49, t2, n2, r2);
  return new Uint32Array(i2);
}
function Ty(e49) {
  return e49[1];
}
function Ey(e49) {
  return Z(e49[1]);
}
function Dy(e49) {
  if (e49.length === 2) {
    let t2 = Z(e49[1]);
    return [t2, t2];
  }
  return [Z(e49[2]), Z(e49[3])];
}
function Oy(e49) {
  return e49[1];
}
function ky(e49) {
  return Zv(e49[1]);
}
function Ay(e49) {
  if (e49.length === 2) {
    let t2 = Zv(e49[1]);
    return [t2, t2];
  }
  return [Zv(e49[2]), Zv(e49[3])];
}
var jy;
(function(e49) {
  e49.PRESENT = `PRESENT`, e49.DATA = `DATA`, e49.OFFSET = `OFFSET`, e49.LENGTH = `LENGTH`;
})(jy ||= {});
var My;
(function(e49) {
  e49.NONE = `NONE`, e49.SINGLE = `SINGLE`, e49.SHARED = `SHARED`, e49.VERTEX = `VERTEX`, e49.MORTON = `MORTON`, e49.FSST = `FSST`;
})(My ||= {});
var Ny;
(function(e49) {
  e49.VERTEX = `VERTEX`, e49.INDEX = `INDEX`, e49.STRING = `STRING`, e49.KEY = `KEY`;
})(Ny ||= {});
var Py;
(function(e49) {
  e49.VAR_BINARY = `VAR_BINARY`, e49.GEOMETRIES = `GEOMETRIES`, e49.PARTS = `PARTS`, e49.RINGS = `RINGS`, e49.TRIANGLES = `TRIANGLES`, e49.SYMBOL = `SYMBOL`, e49.DICTIONARY = `DICTIONARY`;
})(Py ||= {});
const Fy = [jy.PRESENT, jy.DATA, jy.OFFSET, jy.LENGTH], Iy = [X.NONE, X.DELTA, X.COMPONENTWISE_DELTA, X.RLE, X.MORTON], Ly = [Y_.NONE, Y_.FAST_PFOR, Y_.VARINT], Ry = [My.NONE, My.SINGLE, My.SHARED, My.VERTEX, My.MORTON, My.FSST], zy = [Ny.VERTEX, Ny.INDEX, Ny.STRING, Ny.KEY], By = [Py.VAR_BINARY, Py.GEOMETRIES, Py.PARTS, Py.RINGS, Py.TRIANGLES, Py.SYMBOL, Py.DICTIONARY];
function Q(e49, t2) {
  let n2 = Uy(e49, t2);
  return n2.logicalLevelTechnique1 === X.MORTON ? Vy(n2, e49, t2) : (X.RLE === n2.logicalLevelTechnique1 || X.RLE === n2.logicalLevelTechnique2) && Y_.NONE !== n2.physicalLevelTechnique ? Hy(n2, e49, t2) : n2;
}
function Vy(e49, t2, n2) {
  let r2 = Uv(t2, n2, 2);
  return { physicalStreamType: e49.physicalStreamType, logicalStreamType: e49.logicalStreamType, logicalLevelTechnique1: e49.logicalLevelTechnique1, logicalLevelTechnique2: e49.logicalLevelTechnique2, physicalLevelTechnique: e49.physicalLevelTechnique, numValues: e49.numValues, byteLength: e49.byteLength, decompressedCount: e49.decompressedCount, numBits: r2[0], coordinateShift: r2[1] };
}
function Hy(e49, t2, n2) {
  let r2 = Uv(t2, n2, 2);
  return { physicalStreamType: e49.physicalStreamType, logicalStreamType: e49.logicalStreamType, logicalLevelTechnique1: e49.logicalLevelTechnique1, logicalLevelTechnique2: e49.logicalLevelTechnique2, physicalLevelTechnique: e49.physicalLevelTechnique, numValues: e49.numValues, byteLength: e49.byteLength, decompressedCount: r2[1], runs: r2[0], numRleValues: r2[1] };
}
function Uy(e49, t2) {
  let n2 = e49[t2.get()], r2 = Fy[n2 >> 4], i2 = {};
  switch (r2) {
    case jy.DATA:
      i2 = { dictionaryType: Ry[n2 & 15] };
      break;
    case jy.OFFSET:
      i2 = { offsetType: zy[n2 & 15] };
      break;
    case jy.LENGTH:
      i2 = { lengthType: By[n2 & 15] };
  }
  t2.increment();
  let a2 = e49[t2.get()], o2 = Iy[a2 >> 5], s2 = Iy[a2 >> 2 & 7], c2 = Ly[a2 & 3];
  t2.increment();
  let l2 = Uv(e49, t2, 2), u2 = l2[0], d2 = l2[1];
  return { physicalStreamType: r2, logicalStreamType: i2, logicalLevelTechnique1: o2, logicalLevelTechnique2: s2, physicalLevelTechnique: c2, numValues: u2, byteLength: d2, decompressedCount: u2 };
}
var $;
(function(e49) {
  e49[e49.FLAT = 0] = `FLAT`, e49[e49.CONST = 1] = `CONST`, e49[e49.SEQUENCE = 2] = `SEQUENCE`, e49[e49.DICTIONARY = 3] = `DICTIONARY`, e49[e49.FSST_DICTIONARY = 4] = `FSST_DICTIONARY`;
})($ ||= {});
var Wy = class {
  constructor(e49, t2) {
    this.values = e49, this._size = t2;
  }
  get(e49) {
    let t2 = Math.floor(e49 / 8), n2 = e49 % 8;
    return (this.values[t2] >> n2 & 1) == 1;
  }
  set(e49, t2) {
    let n2 = Math.floor(e49 / 8), r2 = e49 % 8;
    this.values[n2] = this.values[n2] | +!!t2 << r2;
  }
  getInt(e49) {
    let t2 = Math.floor(e49 / 8), n2 = e49 % 8;
    return this.values[t2] >> n2 & 1;
  }
  size() {
    return this._size;
  }
  getBuffer() {
    return this.values;
  }
};
function Gy(e49, t2, n2) {
  if (!t2) return e49;
  let r2 = t2.size(), i2 = e49.constructor, a2 = new i2(r2), o2 = 0;
  for (let i3 = 0; i3 < r2; i3++) a2[i3] = t2.get(i3) ? e49[o2++] : n2;
  return a2;
}
function Ky(e49, t2, n2) {
  if (!n2) return e49;
  let r2 = n2.size(), i2 = new Wy(e49, t2), a2 = new Wy(new Uint8Array(Math.ceil(r2 / 8)), r2), o2 = 0;
  for (let e50 = 0; e50 < r2; e50++) {
    let t3 = n2.get(e50) ? i2.get(o2++) : false;
    a2.set(e50, t3);
  }
  return a2.getBuffer();
}
function qy(e49, t2, n2) {
  for (let r2 = 0; r2 < e49; r2++) {
    let e50 = Q(t2, n2);
    n2.add(e50.byteLength);
  }
}
function Jy(e49, t2, n2, r2, i2) {
  let a2 = Yy(e49, Math.ceil(t2 / 8), n2, r2);
  return i2 ? Ky(a2, t2, i2) : a2;
}
function Yy(e49, t2, n2, r2) {
  let i2 = new Uint8Array(t2), a2 = 0, o2 = r2.get() + n2;
  for (; a2 < t2 && !(r2.get() >= o2); ) {
    let n3 = e49[r2.increment()];
    if (n3 <= 127) {
      let o3 = n3 + 3, s2 = e49[r2.increment()], c2 = Math.min(a2 + o3, t2);
      i2.fill(s2, a2, c2), a2 = c2;
    } else {
      let o3 = 256 - n3;
      for (let n4 = 0; n4 < o3 && a2 < t2; n4++) i2[a2++] = e49[r2.increment()];
    }
  }
  return r2.set(o2), i2;
}
function Xy(e49, t2, n2, r2) {
  let i2 = t2.get(), a2 = i2 + n2 * Float32Array.BYTES_PER_ELEMENT, o2 = new Uint8Array(e49.subarray(i2, a2)).buffer, s2 = new Float32Array(o2);
  return t2.set(a2), r2 ? Gy(s2, r2, 0) : s2;
}
function Zy(e49, t2, n2, r2) {
  let i2 = t2.get(), a2 = i2 + n2 * Float64Array.BYTES_PER_ELEMENT, o2 = new Uint8Array(e49.subarray(i2, a2)).buffer, s2 = new Float64Array(o2);
  return t2.set(a2), r2 ? Gy(s2, r2, 0) : s2;
}
function Qy(e49, t2, n2) {
  let r2 = t2.get(), i2 = n2 * Uint32Array.BYTES_PER_ELEMENT, a2 = new DataView(e49.buffer, e49.byteOffset, e49.byteLength), o2 = new Uint32Array(n2);
  for (let e50 = 0; e50 < n2; e50++) o2[e50] = a2.getUint32(r2 + e50 * Uint32Array.BYTES_PER_ELEMENT, true);
  return t2.add(i2), o2;
}
function $y(e49, t2, n2) {
  let r2 = t2.get(), i2 = n2 * BigUint64Array.BYTES_PER_ELEMENT, a2 = new DataView(e49.buffer, e49.byteOffset, e49.byteLength), o2 = new BigUint64Array(n2);
  for (let e50 = 0; e50 < n2; e50++) o2[e50] = a2.getBigUint64(r2 + e50 * BigUint64Array.BYTES_PER_ELEMENT, true);
  return t2.add(i2), o2;
}
const eb = new TextDecoder();
function tb(e49, t2, n2) {
  return n2 - t2 >= 12 ? eb.decode(e49.subarray(t2, n2)) : nb(e49, t2, n2);
}
function nb(e49, t2, n2) {
  let r2 = ``, i2 = t2;
  for (; i2 < n2; ) {
    let t3 = e49[i2], a2 = null, o2 = t3 > 239 ? 4 : t3 > 223 ? 3 : t3 > 191 ? 2 : 1;
    if (i2 + o2 > n2) break;
    let s2, c2, l2;
    o2 === 1 ? t3 < 128 && (a2 = t3) : o2 === 2 ? (s2 = e49[i2 + 1], (s2 & 192) == 128 && (a2 = (t3 & 31) << 6 | s2 & 63, a2 <= 127 && (a2 = null))) : o2 === 3 ? (s2 = e49[i2 + 1], c2 = e49[i2 + 2], (s2 & 192) == 128 && (c2 & 192) == 128 && (a2 = (t3 & 15) << 12 | (s2 & 63) << 6 | c2 & 63, (a2 <= 2047 || a2 >= 55296 && a2 <= 57343) && (a2 = null))) : o2 === 4 && (s2 = e49[i2 + 1], c2 = e49[i2 + 2], l2 = e49[i2 + 3], (s2 & 192) == 128 && (c2 & 192) == 128 && (l2 & 192) == 128 && (a2 = (t3 & 15) << 18 | (s2 & 63) << 12 | (c2 & 63) << 6 | l2 & 63, (a2 <= 65535 || a2 >= 1114112) && (a2 = null))), a2 === null ? (a2 = 65533, o2 = 1) : a2 > 65535 && (a2 -= 65536, r2 += String.fromCharCode(a2 >>> 10 & 1023 | 55296), a2 = 56320 | a2 & 1023), r2 += String.fromCharCode(a2), i2 += o2;
  }
  return r2;
}
function rb(e49, t2, n2, r2, i2) {
  return vb(ob(e49, t2, n2), n2, r2, i2);
}
function ib(e49, t2, n2, r2, i2) {
  return yb(ob(e49, t2, n2), n2, r2, i2);
}
function ab(e49, t2, n2) {
  return Cb(ob(e49, t2, n2), n2);
}
function ob(e49, t2, n2) {
  let r2 = n2.physicalLevelTechnique;
  switch (r2) {
    case Y_.FAST_PFOR:
      return Yv(e49, n2.numValues, n2.byteLength, t2);
    case Y_.VARINT:
      return Uv(e49, t2, n2.numValues);
    case Y_.NONE:
      return Qy(e49, t2, n2.numValues);
    default:
      throw Error(`Specified physicalLevelTechnique ${r2} is not supported (yet).`);
  }
}
function sb(e49, t2, n2) {
  let r2 = n2.physicalLevelTechnique;
  switch (r2) {
    case Y_.VARINT:
      return Wv(e49, t2, n2.numValues);
    case Y_.NONE:
      return $y(e49, t2, n2.numValues);
    default:
      throw Error(`Specified physicalLevelTechnique ${r2} is not supported (yet).`);
  }
}
function cb(e49, t2, n2) {
  let r2 = ob(e49, t2, n2);
  return r2.length === 1 ? Z(r2[0]) : Ey(r2);
}
function lb(e49, t2, n2) {
  let r2 = ob(e49, t2, n2);
  return r2.length === 1 ? n2.logicalLevelTechnique1 === X.DELTA ? Z(r2[0]) : r2[0] : Ty(r2);
}
function ub(e49, t2, n2) {
  return Dy(ob(e49, t2, n2));
}
function db(e49, t2, n2) {
  return Ay(Wv(e49, t2, n2.numValues));
}
function fb(e49, t2, n2, r2) {
  return bb(sb(e49, t2, n2), n2, r2);
}
function pb(e49, t2, n2, r2) {
  return xb(sb(e49, t2, n2), n2, r2);
}
function mb(e49, t2, n2, r2) {
  let i2 = hb(e49, t2, n2, false);
  return r2 ? Gy(i2, r2, 0) : i2;
}
function hb(e49, t2, n2, r2) {
  if (n2.physicalLevelTechnique === Y_.VARINT) return Sb(Kv(e49, t2, n2.numValues), n2, r2);
  let i2 = sb(e49, t2, n2), a2 = r2 ? bb(i2, n2) : xb(i2, n2);
  return Float64Array.from(a2, Number);
}
function gb(e49, t2, n2) {
  let r2 = sb(e49, t2, n2);
  return r2.length === 1 ? Zv(r2[0]) : ky(r2);
}
function _b2(e49, t2, n2) {
  let r2 = sb(e49, t2, n2);
  return r2.length === 1 ? n2.logicalLevelTechnique1 === X.DELTA ? Zv(r2[0]) : r2[0] : Oy(r2);
}
function vb(e49, t2, n2, r2) {
  let i2;
  switch (t2.logicalLevelTechnique1) {
    case X.DELTA:
      if (t2.logicalLevelTechnique2 === X.RLE) {
        let n3 = t2;
        if (!r2) return yy(e49, n3.runs, n3.numRleValues);
        e49 = ny(e49, n3.runs, n3.numRleValues), i2 = ay(e49);
      } else i2 = ay(e49);
      break;
    case X.RLE:
      i2 = cy(e49, t2.runs, t2.numRleValues);
      break;
    case X.MORTON:
      dy(e49), i2 = new Int32Array(e49);
      break;
    case X.COMPONENTWISE_DELTA:
      if (n2 && !r2) return my(e49, n2.scale, n2.min, n2.max);
      i2 = py(e49);
      break;
    case X.NONE:
      i2 = $v(e49);
      break;
    default:
      throw Error(`The specified Logical level technique is not supported: ${t2.logicalLevelTechnique1}`);
  }
  return r2 ? Gy(i2, r2, 0) : i2;
}
function yb(e49, t2, n2, r2) {
  let i2;
  switch (t2.logicalLevelTechnique1) {
    case X.DELTA:
      if (t2.logicalLevelTechnique2 === X.RLE) {
        let n3 = t2;
        i2 = xy(ny(e49, n3.runs, n3.numRleValues));
      } else i2 = xy(e49);
      break;
    case X.RLE:
      i2 = ny(e49, t2.runs, t2.numRleValues);
      break;
    case X.MORTON:
      dy(e49), i2 = e49;
      break;
    case X.COMPONENTWISE_DELTA:
      i2 = n2 && !r2 ? wy(e49, n2.scale, n2.min, n2.max) : Cy(e49);
      break;
    case X.NONE:
      i2 = e49;
      break;
    default:
      throw Error(`The specified Logical level technique is not supported: ${t2.logicalLevelTechnique1}`);
  }
  return r2 ? Gy(i2, r2, 0) : i2;
}
function bb(e49, t2, n2) {
  let r2;
  switch (t2.logicalLevelTechnique1) {
    case X.DELTA:
      if (t2.logicalLevelTechnique2 === X.RLE) {
        let i2 = t2;
        if (!n2) return by(e49, i2.runs, i2.numRleValues);
        e49 = ry(e49, i2.runs, i2.numRleValues), r2 = oy(e49);
      } else r2 = oy(e49);
      break;
    case X.RLE:
      r2 = ly(e49, t2.runs, t2.numRleValues);
      break;
    case X.NONE:
      r2 = ey(e49);
      break;
    default:
      throw Error(`The specified Logical level technique is not supported: ${t2.logicalLevelTechnique1}`);
  }
  return n2 ? Gy(r2, n2, 0n) : r2;
}
function xb(e49, t2, n2) {
  let r2;
  switch (t2.logicalLevelTechnique1) {
    case X.DELTA:
      if (t2.logicalLevelTechnique2 === X.RLE) {
        let n3 = t2;
        r2 = Sy(ry(e49, n3.runs, n3.numRleValues));
      } else r2 = Sy(e49);
      break;
    case X.RLE:
      r2 = ry(e49, t2.runs, t2.numRleValues);
      break;
    case X.NONE:
      r2 = e49;
      break;
    default:
      throw Error(`The specified Logical level technique is not supported: ${t2.logicalLevelTechnique1}`);
  }
  return n2 ? Gy(r2, n2, 0n) : r2;
}
function Sb(e49, t2, n2) {
  switch (t2.logicalLevelTechnique1) {
    case X.DELTA:
      if (t2.logicalLevelTechnique2 === X.RLE) {
        let n3 = t2;
        e49 = iy(e49, n3.runs, n3.numRleValues);
      }
      return sy(e49), e49;
    case X.RLE:
      return Eb(e49, t2, n2);
    case X.NONE:
      return n2 && ty(e49), e49;
    default:
      throw Error(`The specified Logical level technique is not supported: ${t2.logicalLevelTechnique1}`);
  }
}
function Cb(e49, t2) {
  if (t2.logicalLevelTechnique1 === X.DELTA && t2.logicalLevelTechnique2 === X.NONE) return gy(e49);
  if (t2.logicalLevelTechnique1 === X.RLE && t2.logicalLevelTechnique2 === X.NONE) {
    let n2 = t2;
    return vy(e49, n2.runs, n2.numRleValues);
  }
  if (t2.logicalLevelTechnique1 === X.NONE && t2.logicalLevelTechnique2 === X.NONE) {
    fy(e49);
    let n2 = new Uint32Array(t2.numValues + 1);
    return n2[0] = 0, n2.set(e49, 1), n2;
  }
  if (t2.logicalLevelTechnique1 === X.DELTA && t2.logicalLevelTechnique2 === X.RLE) {
    let n2 = t2, r2 = _y(e49, n2.runs, n2.numRleValues);
    return dy(r2), new Uint32Array(r2);
  }
  throw Error(`Only delta encoding is supported for transforming length to offset streams yet.`);
}
function wb(e49, t2, n2, r2, i2 = `int32`) {
  let a2 = e49.logicalLevelTechnique1;
  if (a2 === X.RLE) return e49.runs === 1 ? $.CONST : $.FLAT;
  if (a2 !== X.DELTA || e49.logicalLevelTechnique2 !== X.RLE) return e49.numValues === 1 ? $.CONST : $.FLAT;
  let o2 = t2 instanceof Wy ? t2.size() : t2, s2 = e49;
  if (s2.numRleValues !== o2) return $.FLAT;
  if (s2.runs === 1) return $.SEQUENCE;
  if (s2.runs !== 2) return e49.numValues === 1 ? $.CONST : $.FLAT;
  let c2 = r2.get();
  if (e49.physicalLevelTechnique === Y_.VARINT) return Tb(n2, r2, i2) ? $.SEQUENCE : e49.numValues === 1 ? $.CONST : $.FLAT;
  let l2 = r2.get(), u2 = new Int32Array(n2.buffer, n2.byteOffset + l2, 4);
  return r2.set(c2), u2[2] === 2 && u2[3] === 2 ? $.SEQUENCE : e49.numValues === 1 ? $.CONST : $.FLAT;
}
function Tb(e49, t2, n2) {
  let r2 = new J_(t2.get());
  if (n2 === `int64`) {
    let t3 = Wv(e49, r2, 4);
    return t3[2] === 2n && t3[3] === 2n;
  }
  let i2 = Uv(e49, r2, 4);
  return i2[2] === 2 && i2[3] === 2;
}
function Eb(e49, t2, n2) {
  return n2 ? uy(e49, t2.runs, t2.numRleValues) : iy(e49, t2.runs, t2.numRleValues);
}
var Db = class extends R_ {
  getValueFromBuffer(e49) {
    return this.dataBuffer[e49];
  }
}, Ob = class extends V_ {
  constructor(e49, t2, n2, r2, i2) {
    super(e49, i2 ? BigInt64Array.of(t2) : BigUint64Array.of(t2), n2, r2);
  }
  getValueFromBuffer(e49) {
    return this.dataBuffer[0] + BigInt(e49) * this.delta;
  }
};
function kb(e49, t2, n2) {
  return { x: Ab(e49, t2) - n2, y: Ab(e49 >> 1, t2) - n2 };
}
function Ab(e49, t2) {
  let n2 = 0;
  for (let r2 = 0; r2 < t2; r2++) n2 |= (e49 & 1 << 2 * r2) >> r2;
  return n2;
}
var jb;
(function(e49) {
  e49[e49.POINT = 0] = `POINT`, e49[e49.LINESTRING = 1] = `LINESTRING`, e49[e49.POLYGON = 2] = `POLYGON`, e49[e49.MULTIPOINT = 3] = `MULTIPOINT`, e49[e49.MULTILINESTRING = 4] = `MULTILINESTRING`, e49[e49.MULTIPOLYGON = 5] = `MULTIPOLYGON`;
})(jb ||= {});
var Mb;
(function(e49) {
  e49[e49.POINT = 0] = `POINT`, e49[e49.LINESTRING = 1] = `LINESTRING`, e49[e49.POLYGON = 2] = `POLYGON`;
})(Mb ||= {});
var Nb;
(function(e49) {
  e49[e49.MORTON = 0] = `MORTON`, e49[e49.VEC_2 = 1] = `VEC_2`, e49[e49.VEC_3 = 2] = `VEC_3`;
})(Nb ||= {});
function Pb(e49) {
  let t2 = Array(e49.numGeometries), n2 = 1, r2 = 1, i2 = 1, a2 = 0, o2 = 0, s2 = 0, c2 = e49.mortonSettings, u2 = e49.topologyVector, d2 = u2.geometryOffsets, f2 = u2.partOffsets, p2 = u2.ringOffsets, m2 = e49.vertexOffsets, h2 = !m2 || m2.length === 0, g2 = e49.containsPolygonGeometry(), _2 = e49.vertexBuffer;
  for (let u3 = 0; u3 < e49.numGeometries; u3++) {
    let v2 = e49.geometryType(u3);
    switch (v2) {
      case jb.POINT:
        {
          let u4, g3;
          if (h2) u4 = _2[o2++], g3 = _2[o2++];
          else if (e49.vertexBufferType === Nb.MORTON) {
            let e50 = _2[m2[s2++]], t3 = kb(e50, c2.numBits, c2.coordinateShift);
            u4 = t3.x, g3 = t3.y;
          } else {
            let e50 = m2[s2++] * 2;
            u4 = _2[e50], g3 = _2[e50 + 1];
          }
          t2[a2++] = [[new l(u4, g3)]], d2 && i2++, f2 && n2++, p2 && r2++;
        }
        break;
      case jb.MULTIPOINT:
        {
          let u4 = d2[i2] - d2[i2 - 1];
          i2++;
          let f3;
          if (h2) {
            f3 = Array(u4);
            for (let e50 = 0; e50 < u4; e50++) {
              let t3 = _2[o2++], n3 = _2[o2++];
              f3[e50] = new l(t3, n3);
            }
          } else f3 = Fb(e49.vertexBufferType, _2, m2, s2, u4, false, c2), s2 += u4;
          t2[a2++] = f3.map((e50) => [e50]), n2 += u4, r2 += u4;
        }
        break;
      case jb.LINESTRING:
        {
          let l2;
          g2 ? (l2 = p2[r2] - p2[r2 - 1], r2++) : l2 = f2[n2] - f2[n2 - 1], n2++;
          let u4;
          h2 ? (u4 = Ib(_2, o2, l2, false), o2 += l2 * 2) : (u4 = Fb(e49.vertexBufferType, _2, m2, s2, l2, false, c2), s2 += l2), t2[a2++] = [u4], d2 && i2++;
        }
        break;
      case jb.POLYGON:
        {
          let l2 = f2[n2] - f2[n2 - 1];
          n2++;
          let u4 = Array(l2 - 1), g3, v3 = p2[r2] - p2[r2 - 1];
          if (r2++, h2) {
            g3 = Ib(_2, o2, v3, true), o2 += v3 * 2;
            for (let e50 = 0; e50 < u4.length; e50++) v3 = p2[r2] - p2[r2 - 1], r2++, u4[e50] = Ib(_2, o2, v3, true), o2 += v3 * 2;
          } else {
            g3 = Fb(e49.vertexBufferType, _2, m2, s2, v3, true, c2), s2 += v3;
            for (let t3 = 0; t3 < u4.length; t3++) v3 = p2[r2] - p2[r2 - 1], r2++, u4[t3] = Fb(e49.vertexBufferType, _2, m2, s2, v3, true, c2), s2 += v3;
          }
          t2[a2++] = [g3].concat(u4), d2 && i2++;
        }
        break;
      case jb.MULTILINESTRING:
        {
          let l2 = d2[i2] - d2[i2 - 1];
          i2++;
          let u4 = Array(l2);
          for (let t3 = 0; t3 < l2; t3++) {
            let i3;
            if (g2 ? (i3 = p2[r2] - p2[r2 - 1], r2++) : i3 = f2[n2] - f2[n2 - 1], n2++, h2) u4[t3] = Ib(_2, o2, i3, false), o2 += i3 * 2;
            else {
              let n3 = Fb(e49.vertexBufferType, _2, m2, s2, i3, false, c2);
              u4[t3] = n3, s2 += i3;
            }
          }
          t2[a2++] = u4;
        }
        break;
      case jb.MULTIPOLYGON:
        {
          let l2 = d2[i2] - d2[i2 - 1];
          i2++;
          let u4 = Array(l2);
          for (let t3 = 0; t3 < l2; t3++) {
            let i3 = f2[n2] - f2[n2 - 1];
            n2++;
            let a3, l3 = Array(i3 - 1), d3 = p2[r2] - p2[r2 - 1];
            r2++, h2 ? (a3 = Ib(_2, o2, d3, true), o2 += d3 * 2) : (a3 = Fb(e49.vertexBufferType, _2, m2, s2, d3, true, c2), s2 += d3);
            for (let t4 = 0; t4 < l3.length; t4++) {
              let n3 = p2[r2] - p2[r2 - 1];
              r2++, h2 ? (l3[t4] = Ib(_2, o2, n3, true), o2 += n3 * 2) : (l3[t4] = Fb(e49.vertexBufferType, _2, m2, s2, n3, true, c2), s2 += n3);
            }
            u4[t3] = [a3].concat(l3);
          }
          t2[a2++] = u4.flat();
        }
        break;
      default:
        throw Error(`The specified geometry type (${v2}) is currently not supported.`);
    }
  }
  return t2;
}
function Fb(e49, t2, n2, r2, i2, a2, o2) {
  return e49 === Nb.MORTON ? Rb(t2, n2, r2, i2, a2, o2) : Lb(t2, n2, r2, i2, a2);
}
function Ib(e49, t2, n2, r2) {
  let i2 = Array(r2 ? n2 + 1 : n2);
  for (let r3 = 0; r3 < n2 * 2; r3 += 2) {
    let n3 = e49[t2 + r3], a2 = e49[t2 + r3 + 1];
    i2[r3 / 2] = new l(n3, a2);
  }
  return r2 && (i2[i2.length - 1] = i2[0]), i2;
}
function Lb(e49, t2, n2, r2, i2) {
  let a2 = Array(i2 ? r2 + 1 : r2);
  for (let i3 = 0; i3 < r2 * 2; i3 += 2) {
    let r3 = t2[n2 + i3 / 2] * 2, o2 = e49[r3], s2 = e49[r3 + 1];
    a2[i3 / 2] = new l(o2, s2);
  }
  return i2 && (a2[a2.length - 1] = a2[0]), a2;
}
function Rb(e49, t2, n2, r2, i2, a2) {
  let o2 = Array(i2 ? r2 + 1 : r2);
  for (let i3 = 0; i3 < r2; i3++) {
    let r3 = e49[t2[n2 + i3]], s2 = kb(r3, a2.numBits, a2.coordinateShift);
    o2[i3] = new l(s2.x, s2.y);
  }
  return i2 && (o2[o2.length - 1] = o2[0]), o2;
}
var zb = class {
  constructor(e49, t2, n2, r2, i2) {
    this._vertexBufferType = e49, this._topologyVector = t2, this._vertexOffsets = n2, this._vertexBuffer = r2, this._mortonSettings = i2;
  }
  get vertexBufferType() {
    return this._vertexBufferType;
  }
  get topologyVector() {
    return this._topologyVector;
  }
  get vertexOffsets() {
    return this._vertexOffsets;
  }
  get vertexBuffer() {
    return this._vertexBuffer;
  }
  getSimpleEncodedVertex(e49) {
    let t2 = this.vertexOffsets ? this.vertexOffsets[e49] * 2 : e49 * 2;
    return [this.vertexBuffer[t2], this.vertexBuffer[t2 + 1]];
  }
  getVertex(e49) {
    if (this.vertexOffsets && this.mortonSettings) {
      let t3 = this.vertexOffsets[e49], n2 = this.vertexBuffer[t3], r2 = kb(n2, this.mortonSettings.numBits, this.mortonSettings.coordinateShift);
      return [r2.x, r2.y];
    }
    let t2 = this.vertexOffsets ? this.vertexOffsets[e49] * 2 : e49 * 2;
    return [this.vertexBuffer[t2], this.vertexBuffer[t2 + 1]];
  }
  getGeometries() {
    return Pb(this);
  }
  get mortonSettings() {
    return this._mortonSettings;
  }
};
function Bb(e49, t2, n2, r2, i2) {
  return new Hb(e49, t2, Nb.VEC_2, n2, r2, i2);
}
function Vb(e49, t2, n2, r2, i2, a2) {
  return new Hb(e49, t2, Nb.MORTON, n2, r2, i2, a2);
}
var Hb = class extends zb {
  constructor(e49, t2, n2, r2, i2, a2, o2) {
    super(n2, r2, i2, a2, o2), this._numGeometries = e49, this._geometryType = t2;
  }
  geometryType(e49) {
    return this._geometryType;
  }
  get numGeometries() {
    return this._numGeometries;
  }
  containsPolygonGeometry() {
    return this._geometryType === jb.POLYGON || this._geometryType === jb.MULTIPOLYGON;
  }
  containsSingleGeometryType() {
    return true;
  }
};
function Ub(e49, t2, n2, r2) {
  return new Gb(Nb.VEC_2, e49, t2, n2, r2);
}
function Wb(e49, t2, n2, r2, i2) {
  return new Gb(Nb.MORTON, e49, t2, n2, r2, i2);
}
var Gb = class extends zb {
  constructor(e49, t2, n2, r2, i2, a2) {
    super(e49, n2, r2, i2, a2), this._geometryTypes = t2;
  }
  geometryType(e49) {
    return this._geometryTypes[e49];
  }
  get numGeometries() {
    return this._geometryTypes.length;
  }
  containsPolygonGeometry() {
    for (let e49 = 0; e49 < this.numGeometries; e49++) if (this.geometryType(e49) === jb.POLYGON || this.geometryType(e49) === jb.MULTIPOLYGON) return true;
    return false;
  }
  containsSingleGeometryType() {
    return false;
  }
}, Kb = class {
  constructor(e49, t2, n2, r2) {
    this._triangleOffsets = e49, this._indexBuffer = t2, this._vertexBuffer = n2, this._topologyVector = r2;
  }
  get triangleOffsets() {
    return this._triangleOffsets;
  }
  get indexBuffer() {
    return this._indexBuffer;
  }
  get vertexBuffer() {
    return this._vertexBuffer;
  }
  get topologyVector() {
    return this._topologyVector;
  }
  getGeometries() {
    if (!this._topologyVector) throw Error(`Cannot convert GpuVector to coordinates without topology information`);
    let e49 = new Uint32Array(this.numGeometries);
    for (let t2 = 0; t2 < this.numGeometries; t2++) e49[t2] = this.geometryType(t2);
    return Ub(e49, this._topologyVector, void 0, this._vertexBuffer).getGeometries();
  }
  [Symbol.iterator]() {
    return null;
  }
};
function qb(e49, t2, n2, r2, i2, a2) {
  return new Jb(e49, t2, n2, r2, i2, a2);
}
var Jb = class extends Kb {
  constructor(e49, t2, n2, r2, i2, a2) {
    super(n2, r2, i2, a2), this._numGeometries = e49, this._geometryType = t2;
  }
  geometryType(e49) {
    return this._geometryType;
  }
  get numGeometries() {
    return this._numGeometries;
  }
  containsSingleGeometryType() {
    return true;
  }
};
function Yb(e49, t2, n2, r2, i2) {
  return new Xb(e49, t2, n2, r2, i2);
}
var Xb = class extends Kb {
  constructor(e49, t2, n2, r2, i2) {
    super(t2, n2, r2, i2), this._geometryTypes = e49;
  }
  geometryType(e49) {
    return this._geometryTypes[e49];
  }
  get numGeometries() {
    return this._geometryTypes.length;
  }
  containsSingleGeometryType() {
    return false;
  }
};
function Zb(e49, t2, n2, r2, i2) {
  let a2 = Q(e49, n2), o2 = wb(a2, r2, e49, n2), s2, c2, l2, u2;
  if (o2 === $.CONST) {
    let o3 = lb(e49, n2, a2), d3, f3, p3, m3;
    for (let r3 = 0; r3 < t2 - 1; r3++) {
      let t3 = Q(e49, n2);
      switch (t3.physicalStreamType) {
        case jy.LENGTH:
          switch (t3.logicalStreamType.lengthType) {
            case Py.GEOMETRIES:
              d3 = ab(e49, n2, t3);
              break;
            case Py.PARTS:
              f3 = ab(e49, n2, t3);
              break;
            case Py.RINGS:
              p3 = ab(e49, n2, t3);
              break;
            case Py.TRIANGLES:
              m3 = ab(e49, n2, t3);
          }
          break;
        case jy.OFFSET:
          switch (t3.logicalStreamType.offsetType) {
            case Ny.VERTEX:
              s2 = ib(e49, n2, t3);
              break;
            case Ny.INDEX:
              u2 = ib(e49, n2, t3);
          }
          break;
        case jy.DATA:
          if (My.VERTEX === t3.logicalStreamType.dictionaryType) c2 = rb(e49, n2, t3, i2);
          else {
            let r4 = t3;
            l2 = { numBits: r4.numBits, coordinateShift: r4.coordinateShift }, c2 = ib(e49, n2, t3, i2);
          }
      }
    }
    return u2 ? d3 !== void 0 || f3 !== void 0 ? qb(r2, o3, m3, u2, c2, { geometryOffsets: d3, partOffsets: f3, ringOffsets: p3 }) : qb(r2, o3, m3, u2, c2) : l2 === void 0 ? Bb(r2, o3, { geometryOffsets: d3, partOffsets: f3, ringOffsets: p3 }, s2, c2) : Vb(r2, o3, { geometryOffsets: d3, partOffsets: f3, ringOffsets: p3 }, s2, c2, l2);
  }
  let d2 = ib(e49, n2, a2), f2, p2, m2, h2;
  for (let r3 = 0; r3 < t2 - 1; r3++) {
    let t3 = Q(e49, n2);
    switch (t3.physicalStreamType) {
      case jy.LENGTH:
        switch (t3.logicalStreamType.lengthType) {
          case Py.GEOMETRIES:
            f2 = ib(e49, n2, t3);
            break;
          case Py.PARTS:
            p2 = ib(e49, n2, t3);
            break;
          case Py.RINGS:
            m2 = ib(e49, n2, t3);
            break;
          case Py.TRIANGLES:
            h2 = ab(e49, n2, t3);
        }
        break;
      case jy.OFFSET:
        switch (t3.logicalStreamType.offsetType) {
          case Ny.VERTEX:
            s2 = ib(e49, n2, t3);
            break;
          case Ny.INDEX:
            u2 = ib(e49, n2, t3);
        }
        break;
      case jy.DATA:
        if (My.VERTEX === t3.logicalStreamType.dictionaryType) c2 = rb(e49, n2, t3, i2);
        else {
          let r4 = t3;
          l2 = { numBits: r4.numBits, coordinateShift: r4.coordinateShift }, c2 = ib(e49, n2, t3, i2);
        }
    }
  }
  let g2, _2, v2;
  return f2 ? (g2 = Qb(d2, f2, 2), p2 && m2 ? (_2 = $b(d2, g2, p2, false), v2 = tx(d2, g2, _2, m2)) : p2 && (_2 = ex(d2, g2, p2))) : p2 && m2 ? (_2 = Qb(d2, p2, 1), v2 = $b(d2, _2, m2, true)) : p2 && (_2 = Qb(d2, p2, 0)), u2 && !_2 ? Yb(d2, h2, u2, c2) : u2 ? Yb(d2, h2, u2, c2, { geometryOffsets: g2, partOffsets: _2, ringOffsets: v2 }) : l2 === void 0 ? Ub(d2, { geometryOffsets: g2, partOffsets: _2, ringOffsets: v2 }, s2, c2) : Wb(d2, { geometryOffsets: g2, partOffsets: _2, ringOffsets: v2 }, s2, c2, l2);
}
function Qb(e49, t2, n2) {
  let r2 = new Uint32Array(e49.length + 1), i2 = 0;
  r2[0] = i2;
  let a2 = 0;
  for (let o2 = 0; o2 < e49.length; o2++) i2 = r2[o2 + 1] = i2 + (e49[o2] > n2 ? t2[a2++] : 1);
  return r2;
}
function $b(e49, t2, n2, r2) {
  let i2 = new Uint32Array(t2[t2.length - 1] + 1), a2 = 0;
  i2[0] = a2;
  let o2 = 1, s2 = 0;
  for (let c2 = 0; c2 < e49.length; c2++) {
    let l2 = e49[c2], u2 = t2[c2 + 1] - t2[c2];
    if (l2 === 5 || l2 === 2 || r2 && (l2 === 4 || l2 === 1)) for (let e50 = 0; e50 < u2; e50++) a2 = i2[o2++] = a2 + n2[s2++];
    else for (let e50 = 0; e50 < u2; e50++) i2[o2++] = ++a2;
  }
  return i2;
}
function ex(e49, t2, n2) {
  let r2 = new Uint32Array(t2[t2.length - 1] + 1), i2 = 0;
  r2[0] = i2;
  let a2 = 1, o2 = 0;
  for (let s2 = 0; s2 < e49.length; s2++) {
    let c2 = e49[s2], l2 = t2[s2 + 1] - t2[s2];
    if (c2 === 4 || c2 === 1) for (let e50 = 0; e50 < l2; e50++) i2 = r2[a2++] = i2 + n2[o2++];
    else for (let e50 = 0; e50 < l2; e50++) r2[a2++] = ++i2;
  }
  return r2;
}
function tx(e49, t2, n2, r2) {
  let i2 = new Uint32Array(n2[n2.length - 1] + 1), a2 = 0;
  i2[0] = a2;
  let o2 = 1, s2 = 1, c2 = 0;
  for (let l2 = 0; l2 < e49.length; l2++) {
    let u2 = e49[l2], d2 = t2[l2 + 1] - t2[l2];
    if (u2 !== 0 && u2 !== 3) for (let e50 = 0; e50 < d2; e50++) {
      let e51 = n2[o2] - n2[o2 - 1];
      o2++;
      for (let t3 = 0; t3 < e51; t3++) a2 = i2[s2++] = a2 + r2[c2++];
    }
    else for (let e50 = 0; e50 < d2; e50++) i2[s2++] = ++a2, o2++;
  }
  return i2;
}
var nx = class extends L_ {
  constructor(e49, t2, n2) {
    super(e49, t2.getBuffer(), n2), this.dataVector = t2;
  }
  getValueFromBuffer(e49) {
    return this.dataVector.get(e49);
  }
}, rx = class extends R_ {
  getValueFromBuffer(e49) {
    return this.dataBuffer[e49];
  }
}, ix = class extends L_ {
  constructor(e49, t2, n2, r2) {
    super(e49, r2 ? BigInt64Array.of(t2) : BigUint64Array.of(t2), n2);
  }
  getValueFromBuffer(e49) {
    return this.dataBuffer[0];
  }
}, ax = class extends L_ {
  constructor(e49, t2, n2, r2) {
    super(e49, n2, r2), this.offsetBuffer = t2;
  }
}, ox = class extends ax {
  constructor(e49, t2, n2, r2) {
    super(e49, t2, n2, r2 ?? t2.length - 1);
  }
  getValueFromBuffer(e49) {
    let t2 = this.offsetBuffer[e49], n2 = this.offsetBuffer[e49 + 1];
    return tb(this.dataBuffer, t2, n2);
  }
}, sx = class extends ax {
  constructor(e49, t2, n2, r2, i2) {
    super(e49, n2, r2, i2 ?? t2.length), this.indexBuffer = t2, this.indexBuffer = t2;
  }
  getValueFromBuffer(e49) {
    let t2 = this.indexBuffer[e49], n2 = this.offsetBuffer[t2], r2 = this.offsetBuffer[t2 + 1];
    return tb(this.dataBuffer, n2, r2);
  }
};
function cx(e49, t2) {
  let n2 = 0;
  for (let r2 = 0; r2 < t2.length; r2++) {
    let i2 = t2[r2];
    i2 === 255 ? (n2++, r2++) : n2 += e49[i2];
  }
  return n2;
}
function lx(e49, t2, n2) {
  let r2 = new Uint32Array(t2.length);
  for (let e50 = 1; e50 < t2.length; e50++) r2[e50] = r2[e50 - 1] + t2[e50 - 1];
  let i2 = new Uint8Array(cx(t2, n2)), a2 = 0;
  for (let o2 = 0; o2 < n2.length; o2++) {
    let s2 = n2[o2];
    if (s2 === 255) o2++, i2[a2++] = n2[o2];
    else {
      let n3 = t2[s2], o3 = r2[s2];
      for (; n3-- > 0; ) i2[a2++] = e49[o3++];
    }
  }
  return i2;
}
var ux = class extends ax {
  constructor(e49, t2, n2, r2, i2, a2, o2, s2) {
    super(e49, n2, r2, o2 ?? t2.length), this.indexBuffer = t2, this.symbolOffsetBuffer = i2, this.symbolTableBuffer = a2, this.sharedDictionaryCache = s2;
  }
  getValueFromBuffer(e49) {
    var _a3;
    this.decodedDictionary ?? (this.decodedDictionary = (_a3 = this.sharedDictionaryCache) == null ? void 0 : _a3.decodedDictionary, this.decodedDictionary ?? (this.decodedDictionary = this.decodeDictionary(), this.sharedDictionaryCache && (this.sharedDictionaryCache.decodedDictionary = this.decodedDictionary)));
    let t2 = this.indexBuffer[e49], n2 = this.offsetBuffer[t2], r2 = this.offsetBuffer[t2 + 1];
    return tb(this.decodedDictionary, n2, r2);
  }
  decodeDictionary() {
    return this.symbolLengthBuffer ??= this.offsetToLengthBuffer(this.symbolOffsetBuffer), lx(this.symbolTableBuffer, this.symbolLengthBuffer, this.dataBuffer);
  }
  offsetToLengthBuffer(e49) {
    let t2 = new Uint32Array(e49.length - 1), n2 = e49[0];
    for (let r2 = 1; r2 < e49.length; r2++) {
      let i2 = e49[r2];
      t2[r2 - 1] = i2 - n2, n2 = i2;
    }
    return t2;
  }
};
function dx(e49, t2, n2, r2, i2) {
  let a2, o2, s2, c2, l2, u2 = i2, d2, f2;
  for (let e50 = 0; e50 < r2; e50++) {
    let e51 = Q(t2, n2);
    switch (e51.physicalStreamType) {
      case jy.PRESENT: {
        let r3 = new Wy(Jy(t2, e51.numValues, e51.byteLength, n2), e51.numValues);
        u2 = i2 ?? r3;
        break;
      }
      case jy.OFFSET:
        o2 = ib(t2, n2, e51, void 0, u2);
        break;
      case jy.LENGTH: {
        let r3 = ab(t2, n2, e51);
        Py.DICTIONARY === e51.logicalStreamType.lengthType ? a2 = r3 : Py.SYMBOL === e51.logicalStreamType.lengthType ? c2 = r3 : d2 = r3;
        break;
      }
      case jy.DATA: {
        let r3 = t2.subarray(n2.get(), n2.get() + e51.byteLength);
        n2.add(e51.byteLength);
        let i3 = e51.logicalStreamType.dictionaryType;
        My.FSST === i3 ? l2 = r3 : My.SINGLE === i3 || My.SHARED === i3 ? s2 = r3 : My.NONE === i3 && (f2 = r3);
        break;
      }
    }
  }
  return fx(e49, l2, o2, a2, s2, c2, u2) ?? px(e49, s2, o2, a2, u2) ?? mx(e49, d2, f2, o2, u2);
}
function fx(e49, t2, n2, r2, i2, a2, o2) {
  if (t2) {
    if (!n2 || !r2 || !i2 || !a2) throw Error(`Incomplete FSST dictionary string column "${e49}"`);
    return new ux(e49, n2, r2, i2, a2, t2, o2);
  }
}
function px(e49, t2, n2, r2, i2) {
  if (t2) {
    if (!n2 || !r2) throw Error(`Incomplete dictionary string column "${e49}"`);
    return i2 ? new sx(e49, n2, r2, t2, i2) : new sx(e49, n2, r2, t2);
  }
}
function mx(e49, t2, n2, r2, i2) {
  if (t2 && n2) {
    if (r2) return i2 ? new sx(e49, r2, t2, n2, i2) : new sx(e49, r2, t2, n2);
    if (i2 && i2.size() !== t2.length - 1) {
      let r3 = new Uint32Array(i2.size()), a2 = 0;
      for (let e50 = 0; e50 < i2.size(); e50++) i2.get(e50) ? r3[e50] = a2++ : r3[e50] = 0;
      return new sx(e49, r3, t2, n2, i2);
    }
    return i2 ? new ox(e49, t2, n2, i2) : new ox(e49, t2, n2);
  }
}
function hx(e49, t2, n2, r2) {
  let i2, a2, o2, s2, c2 = false;
  for (; !c2; ) {
    let n3 = Q(e49, t2);
    switch (n3.physicalStreamType) {
      case jy.LENGTH:
        Py.DICTIONARY === n3.logicalStreamType.lengthType ? i2 = ab(e49, t2, n3) : o2 = ab(e49, t2, n3);
        break;
      case jy.DATA:
        My.SINGLE === n3.logicalStreamType.dictionaryType || My.SHARED === n3.logicalStreamType.dictionaryType ? (a2 = e49.subarray(t2.get(), t2.get() + n3.byteLength), c2 = true) : s2 = e49.subarray(t2.get(), t2.get() + n3.byteLength), t2.add(n3.byteLength);
    }
  }
  if (n2.type !== `complexType`) throw Error(`Shared dictionary column ${n2.name} must be a complex (struct) column.`);
  if (!i2 || !a2) throw Error(`Incomplete shared dictionary for column "${n2.name}"`);
  let l2 = n2.complexType.children, u2 = [], d2 = s2 ? {} : void 0, f2 = 0;
  for (let c3 of l2) {
    let l3 = Uv(e49, t2, 1)[0];
    if (l3 === 0) continue;
    let p2 = c3.name ? `${n2.name}${c3.name}` : n2.name;
    if (r2 && !r2.has(p2)) {
      qy(l3, e49, t2);
      continue;
    }
    if (c3.type !== `scalarField` || c3.scalarField.physicalType !== Y.STRING) throw Error(`Currently only scalar string fields are implemented for a struct.`);
    if (l3 > 1 && !c3.nullable || l3 === 1 && c3.nullable) throw Error(`The number of streams for the child field ${c3.name} does not match its nullability. nullibilty: ${c3.nullable}, numStreams: ${l3}`);
    let m2;
    if (c3.nullable) {
      let n3 = Q(e49, t2);
      m2 = new Wy(Jy(e49, n3.numValues, n3.byteLength, t2), n3.numValues);
    }
    let h2 = ib(e49, t2, Q(e49, t2), void 0, m2);
    if (s2) {
      if (!o2) throw Error(`Incomplete shared FSST dictionary for column "${p2}"`);
      u2[f2++] = new ux(p2, h2, i2, a2, o2, s2, m2, d2);
    } else u2[f2++] = new sx(p2, h2, i2, a2, m2);
  }
  return u2;
}
var gx = class extends L_ {
  constructor(e49, t2, n2) {
    super(e49, /* @__PURE__ */ new Uint8Array(), n2 ?? t2.length), this.values = t2;
  }
  getValueFromBuffer(e49) {
    return this.values[e49];
  }
}, _x;
(function(e49) {
  e49[e49.STRING = 1] = `STRING`, e49[e49.INT32 = 2] = `INT32`, e49[e49.UINT32 = 4] = `UINT32`, e49[e49.INT64 = 8] = `INT64`, e49[e49.UINT64 = 16] = `UINT64`, e49[e49.FLOAT = 32] = `FLOAT`, e49[e49.DOUBLE = 64] = `DOUBLE`, e49[e49.PRESENCE = 128] = `PRESENCE`;
})(_x ||= {});
var vx;
(function(e49) {
  e49[e49.FALSE = 0] = `FALSE`, e49[e49.TRUE = 1] = `TRUE`, e49[e49.START_MAP = 2] = `START_MAP`, e49[e49.START_LIST = 3] = `START_LIST`, e49[e49.COUNT = 4] = `COUNT`;
})(vx ||= {});
function yx(e49, t2, n2, r2) {
  let i2 = bx(n2);
  if (r2 === 0) return i2.map((e50) => new gx(e50, []));
  let a2 = xx(e49, t2, r2), o2 = (a2.presentStream ? a2.presentCount : a2.lengthStream.length) / i2.length, s2 = [], c2 = 0, l2 = 0;
  for (let e50 = 0; e50 < i2.length; e50++) {
    let t3 = Ex(a2, e50, o2, c2, l2);
    s2.push(new gx(i2[e50], t3.value, t3.nullabilityBuffer)), c2 = t3.countsEnd, l2 = t3.valuesEnd;
  }
  return s2;
}
function bx(e49) {
  let t2 = e49.type === `complexType` ? e49.complexType.children : void 0;
  return !t2 || t2.length === 0 ? [e49.name] : t2.map((t3) => e49.name + (t3.name ?? ``));
}
function xx(e49, t2, n2) {
  let r2 = e49[t2.get()];
  t2.add(1);
  let i2 = ib(e49, t2, Q(e49, t2)), a2 = n2 - 1, o2 = [];
  r2 & _x.STRING && (a2 -= Sx(e49, t2, o2)), a2 -= Cx(e49, t2, r2, o2), a2 -= wx(e49, t2, r2, o2);
  let s2, c2 = 0;
  if (r2 & _x.PRESENCE) {
    let n3 = Tx(e49, t2);
    s2 = n3.value, c2 = n3.count, a2--;
  }
  let l2 = /* @__PURE__ */ new Uint32Array();
  if (a2 > 0 && (l2 = ib(e49, t2, Q(e49, t2)), a2--), a2 !== 0) throw Error(`Unexpected number of remaining streams while decoding map column: ${a2}`);
  return { lengthStream: i2, dictionary: o2, presentStream: s2, presentCount: c2, flattenedValues: l2 };
}
function Sx(e49, t2, n2) {
  let r2 = e49[t2.get()];
  t2.add(1);
  let i2 = dx(``, e49, t2, r2);
  if (i2) for (let e50 = 0; e50 < i2.size; e50++) n2.push(i2.getValue(e50));
  return r2;
}
function Cx(e49, t2, n2, r2) {
  let i2 = 0;
  return n2 & _x.INT32 ? (Mx(r2, rb(e49, t2, Q(e49, t2))), i2++) : n2 & _x.INT64 && (Mx(r2, fb(e49, t2, Q(e49, t2))), i2++), n2 & _x.UINT32 ? (Mx(r2, ib(e49, t2, Q(e49, t2))), i2++) : n2 & _x.UINT64 && (Mx(r2, pb(e49, t2, Q(e49, t2))), i2++), i2;
}
function wx(e49, t2, n2, r2) {
  let i2 = 0;
  return n2 & _x.FLOAT && (Mx(r2, Xy(e49, t2, Q(e49, t2).numValues)), i2++), n2 & _x.DOUBLE && (Mx(r2, Zy(e49, t2, Q(e49, t2).numValues)), i2++), i2;
}
function Tx(e49, t2) {
  let n2 = Q(e49, t2);
  if (n2.physicalStreamType !== jy.PRESENT) throw Error(`Expected PRESENT stream for map column but found: ${n2.physicalStreamType}`);
  let r2 = n2.numValues, i2 = t2.get(), a2 = new Wy(Jy(e49, r2, n2.byteLength, t2), r2);
  return t2.set(i2 + n2.byteLength), { value: a2, count: r2 };
}
function Ex(e49, t2, n2, r2, i2) {
  let { lengthStream: a2, flattenedValues: o2, presentStream: s2, dictionary: c2 } = e49, l2 = t2 * n2, u2 = n2, d2;
  if (s2) {
    d2 = new Wy(new Uint8Array(Math.ceil(n2 / 8)), n2), u2 = 0;
    for (let e50 = 0; e50 < n2; e50++) s2.get(l2 + e50) && (d2.set(e50, true), u2++);
  }
  let f2 = r2 + u2;
  if (f2 > a2.length) throw Error(`Merged map counts underflow while decoding child streams`);
  let p2 = Array(n2), m2 = r2, h2 = i2;
  for (let e50 = 0; e50 < n2; e50++) {
    if (s2 && !s2.get(l2 + e50)) {
      p2[e50] = null;
      continue;
    }
    let t3 = h2 + a2[m2++];
    if (t3 > o2.length) throw Error(`Map value stream underflow while decoding feature payload`);
    let n3 = Dx(o2, h2, t3, c2);
    p2[e50] = n3.value, h2 = n3.nextIndex;
  }
  let g2 = 0;
  for (let e50 = r2; e50 < f2; e50++) g2 += a2[e50];
  let _2 = i2 + g2;
  if (h2 !== _2) throw Error(`Unused flattened map values remain after decode`);
  return { value: p2, nullabilityBuffer: d2, countsEnd: f2, valuesEnd: _2 };
}
function Dx(e49, t2, n2, r2) {
  return n2 - t2 === 1 || e49[t2] === vx.START_LIST ? kx(e49, t2, n2, r2) : Ox(e49, t2, n2, r2);
}
function Ox(e49, t2, n2, r2) {
  let i2 = /* @__PURE__ */ Object.create(null), a2 = t2;
  for (; a2 < n2; ) {
    let t3 = jx(e49[a2++], r2);
    if (typeof t3 != `string`) throw Error(`Map key dictionary index does not resolve to a string: ${t3}`);
    let o2 = kx(e49, a2, n2, r2);
    i2[t3] = o2.value, a2 = o2.nextIndex;
  }
  return { value: i2, nextIndex: a2 };
}
function kx(e49, t2, n2, r2) {
  if (t2 >= n2) throw Error(`Unexpected end of map value stream`);
  let i2 = e49[t2];
  if (i2 === vx.FALSE) return { value: false, nextIndex: t2 + 1 };
  if (i2 === vx.TRUE) return { value: true, nextIndex: t2 + 1 };
  if (i2 === vx.START_MAP) {
    let i3 = Ax(e49, t2, n2);
    return { value: Ox(e49, t2 + 2, i3, r2).value, nextIndex: i3 };
  }
  if (i2 === vx.START_LIST) {
    let i3 = Ax(e49, t2, n2), a2 = [], o2 = t2 + 2;
    for (; o2 < i3; ) {
      let t3 = kx(e49, o2, i3, r2);
      a2.push(t3.value), o2 = t3.nextIndex;
    }
    return { value: a2, nextIndex: i3 };
  }
  return { value: jx(i2, r2), nextIndex: t2 + 1 };
}
function Ax(e49, t2, n2) {
  if (t2 + 1 >= n2) throw Error(`Missing length for nested map/list payload`);
  let r2 = e49[t2 + 1];
  if (r2 < 2) throw Error(`Invalid nested payload length: ${r2}`);
  let i2 = t2 + r2;
  if (i2 > n2) throw Error(`Nested payload exceeds containing payload bounds`);
  return i2;
}
function jx(e49, t2) {
  let n2 = e49 - vx.COUNT;
  if (n2 < 0 || n2 >= t2.length) throw Error(`Scalar dictionary index out of range: ${e49}`);
  return t2[n2];
}
function Mx(e49, t2) {
  for (let n2 of t2) e49.push(n2);
}
function Nx(e49, t2, n2, r2, i2, a2) {
  var _a3;
  return n2.type === `scalarType` ? a2 && !a2.has(n2.name) ? (qy(r2, e49, t2), null) : Px(r2, e49, t2, i2, n2.scalarType, n2) : ((_a3 = n2.complexType) == null ? void 0 : _a3.physicalType) === K_.MAP ? yx(e49, t2, n2, r2) : r2 === 0 ? null : hx(e49, t2, n2, a2);
}
function Px(e49, t2, n2, r2, i2, a2) {
  let o2;
  if (e49 === 0) return null;
  if (a2.nullable) {
    let e50 = Q(t2, n2), r3 = e50.numValues, i3 = n2.get(), a3 = Jy(t2, r3, e50.byteLength, n2);
    n2.set(i3 + e50.byteLength), o2 = new Wy(a3, e50.numValues);
  }
  let s2 = o2 ?? r2;
  switch (i2.physicalType) {
    case Y.UINT_32:
    case Y.INT_32:
      return zx(t2, n2, a2, i2, s2);
    case Y.STRING: {
      let r3 = a2.nullable ? e49 - 1 : e49;
      return dx(a2.name, t2, n2, r3, o2) ?? null;
    }
    case Y.BOOLEAN:
      return Fx(t2, n2, a2, r2, s2);
    case Y.UINT_64:
    case Y.INT_64:
      return Rx(t2, n2, a2, s2, i2);
    case Y.FLOAT:
      return Ix(t2, n2, a2, s2);
    case Y.DOUBLE:
      return Lx(t2, n2, a2, s2);
    default:
      throw Error(`The specified data type for the field is currently not supported: ${i2}`);
  }
}
function Fx(e49, t2, n2, r2, i2) {
  let a2 = Q(e49, t2), o2 = a2.numValues, s2 = t2.get(), c2 = Bx(i2) ? i2 : void 0, l2 = Jy(e49, o2, a2.byteLength, t2, c2);
  t2.set(s2 + a2.byteLength);
  let u2 = new Wy(l2, o2);
  return new nx(n2.name, u2, i2);
}
function Ix(e49, t2, n2, r2) {
  let i2 = Q(e49, t2), a2 = Bx(r2) ? r2 : void 0, o2 = Xy(e49, t2, i2.numValues, a2);
  return new rx(n2.name, o2, r2);
}
function Lx(e49, t2, n2, r2) {
  let i2 = Q(e49, t2), a2 = Bx(r2) ? r2 : void 0, o2 = Zy(e49, t2, i2.numValues, a2);
  return new B_(n2.name, o2, r2);
}
function Rx(e49, t2, n2, r2, i2) {
  let a2 = Q(e49, t2), o2 = wb(a2, r2, e49, t2, `int64`), s2 = i2.physicalType === Y.INT_64;
  if (o2 === $.FLAT) {
    let i3 = Bx(r2) ? r2 : void 0, o3 = s2 ? fb(e49, t2, a2, i3) : pb(e49, t2, a2, i3);
    return new Db(n2.name, o3, r2);
  }
  if (o2 === $.SEQUENCE) {
    let r3 = db(e49, t2, a2);
    return new Ob(n2.name, r3[0], r3[1], a2.numRleValues, s2);
  }
  let c2 = s2 ? gb(e49, t2, a2) : _b2(e49, t2, a2);
  return new ix(n2.name, c2, r2, s2);
}
function zx(e49, t2, n2, r2, i2) {
  let a2 = Q(e49, t2), o2 = wb(a2, i2, e49, t2), s2 = r2.physicalType === Y.INT_32;
  if (o2 === $.FLAT) {
    let r3 = Bx(i2) ? i2 : void 0, o3 = s2 ? rb(e49, t2, a2, void 0, r3) : ib(e49, t2, a2, void 0, r3);
    return new z_(n2.name, o3, i2);
  }
  if (o2 === $.SEQUENCE) {
    let r3 = ub(e49, t2, a2);
    return new H_(n2.name, r3[0], r3[1], a2.numRleValues, s2);
  }
  let c2 = s2 ? cb(e49, t2, a2) : lb(e49, t2, a2);
  return new U_(n2.name, c2, i2, s2);
}
function Bx(e49) {
  return e49 instanceof Wy;
}
const Vx = { ID: 0, ID_NULLABLE: 1, ID_LONG: 2, GEOMETRY: 4, SCALAR_BASE: 10, STRUCT: 30, MAP: 31 };
function Hx(e49) {
  switch (e49) {
    case Vx.ID:
    case Vx.ID | Vx.ID_NULLABLE:
    case Vx.ID | Vx.ID_LONG:
    case Vx.ID | Vx.ID_LONG | Vx.ID_NULLABLE:
      return { nullable: (e49 & Vx.ID_NULLABLE) !== 0, columnScope: G_.FEATURE, type: `scalarType`, scalarType: { longID: (e49 & Vx.ID_LONG) !== 0, type: `logicalType`, logicalType: q_.ID } };
    case Vx.GEOMETRY:
      return { nullable: false, columnScope: G_.FEATURE, type: `complexType`, complexType: { type: `physicalType`, physicalType: K_.GEOMETRY, children: [] } };
    case Vx.STRUCT:
      return { nullable: false, columnScope: G_.FEATURE, type: `complexType`, complexType: { type: `physicalType`, physicalType: K_.STRUCT, children: [] } };
    case Vx.MAP:
      return { nullable: true, columnScope: G_.FEATURE, type: `complexType`, complexType: { type: `physicalType`, physicalType: K_.MAP, children: [] } };
    default:
      return Jx(e49);
  }
}
function Ux(e49) {
  return e49 >= Vx.SCALAR_BASE;
}
function Wx(e49) {
  return e49 === Vx.STRUCT || e49 === Vx.MAP;
}
function Gx(e49) {
  if (e49.type === `scalarType`) {
    let t2 = e49.scalarType;
    if (t2.type === `physicalType`) switch (t2.physicalType) {
      case Y.BOOLEAN:
      case Y.INT_8:
      case Y.UINT_8:
      case Y.INT_32:
      case Y.UINT_32:
      case Y.INT_64:
      case Y.UINT_64:
      case Y.FLOAT:
      case Y.DOUBLE:
        return false;
      case Y.STRING:
        return true;
      default:
        return false;
    }
    if (t2.type === `logicalType`) return false;
  } else if (e49.type === `complexType`) {
    let t2 = e49.complexType;
    if (t2.type === `physicalType`) switch (t2.physicalType) {
      case K_.GEOMETRY:
      case K_.STRUCT:
      case K_.MAP:
        return true;
      default:
        return false;
    }
  }
  return console.warn(`Unexpected column type in hasStreamCount`, e49), false;
}
function Kx(e49) {
  var _a3;
  return e49.type === `scalarType` && ((_a3 = e49.scalarType) == null ? void 0 : _a3.type) === `logicalType` && e49.scalarType.logicalType === q_.ID;
}
function qx(e49) {
  var _a3;
  return e49.type === `complexType` && ((_a3 = e49.complexType) == null ? void 0 : _a3.type) === `physicalType` && e49.complexType.physicalType === K_.GEOMETRY;
}
function Jx(e49) {
  let t2;
  switch (e49) {
    case 10:
    case 11:
      t2 = Y.BOOLEAN;
      break;
    case 12:
    case 13:
      t2 = Y.INT_8;
      break;
    case 14:
    case 15:
      t2 = Y.UINT_8;
      break;
    case 16:
    case 17:
      t2 = Y.INT_32;
      break;
    case 18:
    case 19:
      t2 = Y.UINT_32;
      break;
    case 20:
    case 21:
      t2 = Y.INT_64;
      break;
    case 22:
    case 23:
      t2 = Y.UINT_64;
      break;
    case 24:
    case 25:
      t2 = Y.FLOAT;
      break;
    case 26:
    case 27:
      t2 = Y.DOUBLE;
      break;
    case 28:
    case 29:
      t2 = Y.STRING;
      break;
    default:
      return null;
  }
  return { nullable: !!(e49 & 1), columnScope: G_.FEATURE, type: `scalarType`, scalarType: { longID: false, type: `physicalType`, physicalType: t2 } };
}
const Yx = new TextDecoder(), Xx = `0-3(ID), 4(GEOMETRY), 10-29(scalars), 30(STRUCT), 31(MAP)`;
function Zx(e49, t2) {
  let n2 = Uv(e49, t2, 1)[0];
  if (n2 === 0) return ``;
  let r2 = t2.get(), i2 = r2 + n2, a2 = e49.subarray(r2, i2);
  return t2.add(n2), Yx.decode(a2);
}
function Qx(e49) {
  let t2 = e49.name, n2 = e49.nullable;
  return e49.type === `scalarType` ? { type: `scalarField`, scalarField: e49.scalarType, name: t2, nullable: n2 } : { type: `complexField`, complexField: e49.complexType, name: t2, nullable: n2 };
}
function $x(e49, t2) {
  let n2 = Uv(e49, t2, 1)[0] >>> 0, r2 = n2 >= Vx.SCALAR_BASE ? Hx(n2) : null;
  if (!r2) throw Error(`Unsupported field type code ${n2}. Supported: 10-29(scalars), 30(STRUCT), 31(MAP)`);
  let i2 = { ...r2, name: Zx(e49, t2) };
  if (i2.type === `complexType` && Wx(n2)) {
    let n3 = i2.complexType, r3 = Uv(e49, t2, 1)[0] >>> 0;
    n3.children = Array(r3);
    for (let i3 = 0; i3 < r3; i3++) n3.children[i3] = $x(e49, t2);
  }
  return Qx(i2);
}
function eS(e49, t2) {
  let n2 = Uv(e49, t2, 1)[0] >>> 0, r2 = Hx(n2);
  if (!r2) throw Error(`Unsupported column type code ${n2}. Supported: ${Xx}`);
  let i2;
  if (Ux(n2)) i2 = Zx(e49, t2);
  else if (n2 < Vx.GEOMETRY) i2 = `id`;
  else if (n2 === Vx.GEOMETRY) i2 = `geometry`;
  else throw Error(`Unsupported column type code ${n2}. Supported: ${Xx}`);
  let a2 = { ...r2, name: i2 };
  if (a2.type === `complexType` && Wx(n2)) {
    let n3 = Uv(e49, t2, 1)[0] >>> 0, r3 = a2.complexType;
    r3.children = Array(n3);
    for (let i3 = 0; i3 < n3; i3++) r3.children[i3] = $x(e49, t2);
  }
  return a2;
}
function tS(e49, t2) {
  let n2 = {};
  n2.featureTables = [];
  let r2 = {};
  if (r2.name = Zx(e49, t2), r2.name.length === 0) throw Error(`Missing layer name`);
  let i2 = Uv(e49, t2, 1)[0] >>> 0, a2 = Uv(e49, t2, 1)[0] >>> 0;
  r2.columns = Array(a2);
  for (let n3 = 0; n3 < a2; n3++) r2.columns[n3] = eS(e49, t2);
  return n2.featureTables.push(r2), [n2, i2];
}
function nS(e49, t2, n2 = true) {
  let r2 = new J_(0), i2 = [];
  for (; r2.get() < e49.length; ) {
    let a2 = Uv(e49, r2, 1)[0] >>> 0, o2 = r2.get() + a2;
    if (o2 > e49.length) throw Error(`Block overruns tile: ${o2} > ${e49.length}`);
    let s2 = Uv(e49, r2, 1)[0] >>> 0;
    if (s2 !== 1 && s2 !== 2) {
      r2.set(o2);
      continue;
    }
    let [c2, l2] = tS(e49, r2), u2 = c2.featureTables[0], d2 = null, f2 = null, p2 = [], m2 = 0;
    for (let i3 of u2.columns) {
      let a3 = i3.name;
      if (Kx(i3)) {
        let t3 = null;
        if (i3.nullable) {
          let n3 = Q(e49, r2), i4 = r2.get(), a4 = Jy(e49, n3.numValues, n3.byteLength, r2);
          r2.set(i4 + n3.byteLength), t3 = new Wy(a4, n3.numValues);
        }
        let o3 = Q(e49, r2);
        m2 = t3 ? t3.size() : o3.decompressedCount, d2 = rS(e49, i3, r2, a3, o3, t3 ?? m2, n2);
      } else if (qx(i3)) {
        let n3 = Uv(e49, r2, 1)[0];
        if (m2 === 0) {
          let t3 = r2.get();
          m2 = Q(e49, r2).decompressedCount, r2.set(t3);
        }
        t2 && (t2.scale = t2.extent / l2), f2 = Zb(e49, n3, r2, m2, t2);
      } else {
        let t3 = Gx(i3) ? Uv(e49, r2, 1)[0] : 1;
        if (t3 === 0) continue;
        let n3 = Nx(e49, r2, i3, t3, m2, void 0);
        if (n3) {
          if (Array.isArray(n3)) for (let e50 of n3) p2.push(e50);
          else p2.push(n3);
        }
      }
    }
    let h2 = new W_(u2.name, f2, d2, p2, l2);
    i2.push(h2), r2.set(o2);
  }
  return i2;
}
function rS(e49, t2, n2, r2, i2, a2, o2 = false) {
  var _a3;
  let s2 = ((_a3 = t2.scalarType) == null ? void 0 : _a3.longID) ? Y.UINT_64 : Y.UINT_32, c2 = typeof a2 == `number` ? void 0 : a2, l2 = wb(i2, a2, e49, n2, s2 === Y.UINT_64 ? `int64` : `int32`);
  if (s2 === Y.UINT_32) switch (l2) {
    case $.FLAT:
      return new z_(r2, ib(e49, n2, i2, void 0, c2), a2);
    case $.SEQUENCE: {
      let t3 = ub(e49, n2, i2);
      return new H_(r2, t3[0], t3[1], i2.numRleValues, false);
    }
    case $.CONST:
      return new U_(r2, lb(e49, n2, i2), a2, false);
  }
  switch (l2) {
    case $.FLAT:
      return o2 ? new B_(r2, mb(e49, n2, i2, c2), a2) : new Db(r2, pb(e49, n2, i2, c2), a2);
    case $.SEQUENCE: {
      let t3 = db(e49, n2, i2);
      return new Ob(r2, t3[0], t3[1], i2.numRleValues, false);
    }
    case $.CONST:
      return new ix(r2, _b2(e49, n2, i2), a2, false);
  }
  throw Error(`Vector type not supported for id column.`);
}
const iS = tv(Q_);
iS / 32 * 4, 3 * iS / 256 + iS | 0;
var aS = class {
  constructor(e49, t2) {
    var _a3;
    switch (this._featureData = e49, this.properties = this._featureData.properties || {}, (_a3 = this._featureData.geometry) == null ? void 0 : _a3.type) {
      case jb.POINT:
      case jb.MULTIPOINT:
        this.type = 1;
        break;
      case jb.LINESTRING:
      case jb.MULTILINESTRING:
        this.type = 2;
        break;
      case jb.POLYGON:
      case jb.MULTIPOLYGON:
        this.type = 3;
        break;
      default:
        this.type = 0;
    }
    this.extent = t2, this.id = Number(this._featureData.id);
  }
  loadGeometry() {
    let e49 = [];
    for (let t2 of this._featureData.geometry.coordinates) {
      let n2 = [];
      for (let e50 of t2) n2.push(new l(e50.x, e50.y));
      e49.push(n2);
    }
    return e49;
  }
}, oS = class {
  constructor(e49) {
    this.features = [], this.featureTable = e49, this.name = e49.name, this.extent = e49.extent, this.version = 2, this.features = e49.getFeatures(), this.length = this.features.length;
  }
  feature(e49) {
    return new aS(this.features[e49], this.extent);
  }
}, sS = class {
  constructor(e49) {
    this.layers = {};
    let t2 = nS(new Uint8Array(e49));
    this.layers = t2.reduce((e50, t3) => ({ ...e50, [t3.name]: new oS(t3) }), {});
  }
}, cS = class {
  constructor(e49, t2) {
    this.tileID = e49, this.x = e49.canonical.x, this.y = e49.canonical.y, this.z = e49.canonical.z, this.grid = new ge(v, 16, 0), this.grid3D = new ge(v, 16, 0), this.featureIndexArray = new Es(), this.promoteId = t2;
  }
  insert(e49, t2, n2, r2, i2, a2) {
    let o2 = this.featureIndexArray.length;
    this.featureIndexArray.emplaceBack(n2, r2, i2);
    let s2 = a2 ? this.grid3D : this.grid;
    for (let e50 of t2) {
      let t3 = [1 / 0, 1 / 0, -1 / 0, -1 / 0];
      for (let n3 of e50) t3[0] = Math.min(t3[0], n3.x), t3[1] = Math.min(t3[1], n3.y), t3[2] = Math.max(t3[2], n3.x), t3[3] = Math.max(t3[3], n3.y);
      t3[0] < 8192 && t3[1] < 8192 && t3[2] >= 0 && t3[3] >= 0 && s2.insert(o2, t3[0], t3[1], t3[2], t3[3]);
    }
  }
  loadVTLayers() {
    if (!this.vtLayers) {
      switch (this.encoding) {
        case `mlt`:
          this.vtLayers = new sS(this.rawTileData).layers;
          break;
        default:
          this.vtLayers = new ad(new ih(this.rawTileData)).layers;
      }
      this.sourceLayerCoder = new F_(this.vtLayers ? Object.keys(this.vtLayers).sort() : [w_]);
    }
    return this.vtLayers;
  }
  query(e49, t2, n2, r2) {
    this.loadVTLayers();
    let i2 = e49.params, a2 = v / e49.tileSize / e49.scale, o2 = ji(i2.filter, `queryRenderedFeatures filter`, i2.globalState), s2 = e49.queryGeometry, c2 = e49.queryPadding * a2, l2 = Zu.fromPoints(s2), u2 = this.grid.query(l2.minX - c2, l2.minY - c2, l2.maxX + c2, l2.maxY + c2), d2 = Zu.fromPoints(e49.cameraQueryGeometry).expandBy(c2), f2 = this.grid3D.query(d2.minX, d2.minY, d2.maxX, d2.maxY, (t3, n3, r3, i3) => Ic(e49.cameraQueryGeometry, t3 - c2, n3 - c2, r3 + c2, i3 + c2));
    for (let e50 of f2) u2.push(e50);
    u2.sort(dS);
    let p2 = {}, m2;
    for (let c3 of u2) {
      if (c3 === m2) continue;
      m2 = c3;
      let l3 = this.featureIndexArray.get(c3), u3 = null;
      this.loadMatchingFeature(p2, l3.bucketIndex, l3.sourceLayerIndex, l3.featureIndex, o2, i2.layers, i2.availableImages, t2, n2, r2, (t3, n3, r3) => (u3 ||= bc(t3), n3.queryIntersectsFeature({ queryGeometry: s2, feature: t3, featureState: r3, geometry: u3, zoom: this.z, transform: e49.transform, pixelsToTileUnits: a2, pixelPosMatrix: e49.pixelPosMatrix, unwrappedTileID: this.tileID.toUnwrapped(), getElevation: e49.getElevation })));
    }
    return p2;
  }
  loadMatchingFeature(e49, t2, n2, r2, i2, a2, o2, s2, c2, l2, u2) {
    let d2 = this.bucketLayerIDs[t2];
    if (a2 && !d2.some((e50) => a2.has(e50))) return;
    let f2 = this.sourceLayerCoder.decode(n2), p2 = this.vtLayers[f2].feature(r2);
    if (i2.needGeometry) {
      let e50 = xc(p2, true);
      if (!i2.filter(new K(this.tileID.overscaledZ), e50, this.tileID.canonical)) return;
    } else if (!i2.filter(new K(this.tileID.overscaledZ), p2)) return;
    let m2 = this.getId(p2, f2);
    for (let t3 of d2) {
      if (a2 && !a2.has(t3)) continue;
      let n3 = s2[t3];
      if (!n3) continue;
      let i3 = {};
      m2 && l2 && (i3 = l2.getState(n3.sourceLayer || `_geojsonTileLayer`, m2));
      let d3 = w({}, c2[t3]);
      d3.paint = uS(d3.paint, n3.paint, p2, i3, o2), d3.layout = uS(d3.layout, n3.layout, p2, i3, o2);
      let f3 = !u2 || u2(p2, n3, i3);
      if (!f3) continue;
      let h2 = new I_(p2, this.z, this.x, this.y, m2);
      h2.layer = d3;
      let g2 = e49[t3];
      g2 === void 0 && (g2 = e49[t3] = []), g2.push({ featureIndex: r2, feature: h2, intersectionZ: f3 });
    }
  }
  lookupSymbolFeatures(e49, t2, n2, r2, i2, a2, o2, s2) {
    let c2 = {};
    this.loadVTLayers();
    let l2 = ji(i2.filterSpec, `queryRenderedFeatures symbol filter`, i2.globalState);
    for (let i3 of e49) this.loadMatchingFeature(c2, n2, r2, i3, l2, a2, o2, s2, t2);
    return c2;
  }
  hasLayer(e49) {
    for (let t2 of this.bucketLayerIDs) for (let n2 of t2) if (e49 === n2) return true;
    return false;
  }
  getId(e49, t2) {
    var _a3;
    let n2 = e49.id;
    if (this.promoteId) {
      let r2 = typeof this.promoteId == `string` ? this.promoteId : this.promoteId[t2];
      n2 = e49.properties[r2], typeof n2 == `boolean` && (n2 = Number(n2)), n2 === void 0 && ((_a3 = e49.properties) == null ? void 0 : _a3.cluster) && this.promoteId && (n2 = Number(e49.properties.cluster_id));
    }
    return n2;
  }
};
G(`FeatureIndex`, cS, { omit: [`rawTileData`, `sourceLayerCoder`] });
function lS(e49) {
  return typeof e49 == `object` && !!e49 && `evaluate` in e49;
}
function uS(e49, t2, n2, r2, i2) {
  return E(e49, (e50, a2) => {
    let o2 = t2 instanceof ko ? t2.get(a2) : null;
    return lS(o2) ? o2.evaluate(n2, r2, void 0, i2) : o2;
  });
}
function dS(e49, t2) {
  return t2 - e49;
}
var fS = class {
  constructor(e49, t2) {
    this.keyCache = {}, e49 && this.replace(e49, t2);
  }
  replace(e49, t2) {
    this._layerConfigs = {}, this._layers = {}, this.update(e49, [], t2);
  }
  update(e49, t2, n2) {
    for (let t3 of e49) {
      this._layerConfigs[t3.id] = t3;
      let e50 = this._layers[t3.id] = S_(t3, n2);
      e50._featureFilter = ji(e50.filter, `layers[${t3.id}].filter`, n2), this.keyCache[t3.id] && delete this.keyCache[t3.id];
    }
    for (let e50 of t2) delete this.keyCache[e50], delete this._layerConfigs[e50], delete this._layers[e50];
    this.familiesBySource = {};
    let r2 = Hi(Object.values(this._layerConfigs), this.keyCache);
    for (let e50 of r2) {
      let t3 = e50.map((e51) => this._layers[e51.id]), n3 = t3[0];
      if (n3.isHidden()) continue;
      let r3 = n3.source || ``, i2 = this.familiesBySource[r3];
      i2 ||= this.familiesBySource[r3] = {};
      let a2 = n3.sourceLayer || `_geojsonTileLayer`, o2 = i2[a2];
      o2 ||= i2[a2] = [], o2.push(t3);
    }
  }
}, pS = class {
  constructor(e49) {
    let t2 = {}, n2 = [];
    for (let r3 in e49) {
      let i3 = t2[r3] = {};
      for (let t3 in e49[r3]) {
        let a3 = e49[r3][t3];
        i3[t3] = {};
        for (let e50 in a3) {
          let r4 = a3[e50];
          if (!r4 || r4.bitmap.width === 0 || r4.bitmap.height === 0) continue;
          let o2 = { x: 0, y: 0, w: r4.bitmap.width + 2, h: r4.bitmap.height + 2 };
          n2.push(o2), i3[t3][e50] = { rect: o2, metrics: r4.metrics };
        }
      }
    }
    let { w: r2, h: i2 } = wh(n2), a2 = new ll({ width: r2 || 1, height: i2 || 1 });
    for (let n3 in e49) for (let r3 in e49[n3]) {
      let i3 = e49[n3][r3];
      for (let e50 in i3) {
        let o2 = i3[e50];
        if (!o2 || o2.bitmap.width === 0 || o2.bitmap.height === 0) continue;
        let s2 = t2[n3][r3][e50].rect;
        ll.copy(o2.bitmap, a2, { x: 0, y: 0 }, { x: s2.x + 1, y: s2.y + 1 }, o2.bitmap);
      }
    }
    this.image = a2, this.positions = t2;
  }
};
G(`GlyphAtlas`, pS);
function mS(e49, t2, n2) {
  return !(e49 < 0 || e49 > 25 || n2 < 0 || n2 >= 2 ** e49 || t2 < 0 || t2 >= 2 ** e49);
}
var hS = class {
  constructor(e49, t2, n2) {
    if (!mS(e49, t2, n2)) throw Error(`x=${t2}, y=${n2}, z=${e49} outside of bounds. 0<=x<${2 ** e49}, 0<=y<${2 ** e49} 0<=z<=25 `);
    this.z = e49, this.x = t2, this.y = n2, this.key = vS(0, e49, e49, t2, n2);
  }
  equals(e49) {
    return this.z === e49.z && this.x === e49.x && this.y === e49.y;
  }
  url(e49, t2, n2) {
    let r2 = bS(this.x, this.y, this.z), i2 = SS(this.z, this.x, this.y);
    return e49[(this.x + this.y) % e49.length].replace(/{prefix}/g, (this.x % 16).toString(16) + (this.y % 16).toString(16)).replace(/{z}/g, String(this.z)).replace(/{x}/g, String(this.x)).replace(/{y}/g, String(n2 === `tms` ? 2 ** this.z - this.y - 1 : this.y)).replace(/{ratio}/g, t2 > 1 ? `@2x` : ``).replace(/{quadkey}/g, i2).replace(/{bbox-epsg-3857}/g, r2);
  }
  isChildOf(e49) {
    let t2 = this.z - e49.z;
    return t2 > 0 && e49.x === this.x >> t2 && e49.y === this.y >> t2;
  }
  getTilePoint(e49) {
    let t2 = 2 ** this.z;
    return new l((e49.x * t2 - this.x) * v, (e49.y * t2 - this.y) * v);
  }
  toString() {
    return `${this.z}/${this.x}/${this.y}`;
  }
}, gS = class {
  constructor(e49, t2) {
    this.wrap = e49, this.canonical = t2, this.key = vS(e49, t2.z, t2.z, t2.x, t2.y);
  }
}, _S = class e48 {
  constructor(e49, t2, n2, r2, i2) {
    if (this.terrainRttPosMatrix32f = null, e49 < n2) throw Error(`overscaledZ should be >= z; overscaledZ = ${e49}; z = ${n2}`);
    this.overscaledZ = e49, this.wrap = t2, this.canonical = new hS(n2, +r2, +i2), this.key = vS(t2, e49, n2, r2, i2);
  }
  clone() {
    return new e48(this.overscaledZ, this.wrap, this.canonical.z, this.canonical.x, this.canonical.y);
  }
  equals(e49) {
    return this.overscaledZ === e49.overscaledZ && this.wrap === e49.wrap && this.canonical.equals(e49.canonical);
  }
  scaledTo(t2) {
    if (t2 > this.overscaledZ) throw Error(`targetZ > this.overscaledZ; targetZ = ${t2}; overscaledZ = ${this.overscaledZ}`);
    let n2 = this.canonical.z - t2;
    return t2 > this.canonical.z ? new e48(t2, this.wrap, this.canonical.z, this.canonical.x, this.canonical.y) : new e48(t2, this.wrap, t2, this.canonical.x >> n2, this.canonical.y >> n2);
  }
  isOverscaled() {
    return this.overscaledZ > this.canonical.z;
  }
  calculateScaledKey(e49, t2) {
    if (e49 > this.overscaledZ) throw Error(`targetZ > this.overscaledZ; targetZ = ${e49}; overscaledZ = ${this.overscaledZ}`);
    let n2 = this.canonical.z - e49;
    return e49 > this.canonical.z ? vS(this.wrap * +t2, e49, this.canonical.z, this.canonical.x, this.canonical.y) : vS(this.wrap * +t2, e49, e49, this.canonical.x >> n2, this.canonical.y >> n2);
  }
  isChildOf(e49) {
    if (e49.wrap !== this.wrap || this.overscaledZ - e49.overscaledZ <= 0) return false;
    if (e49.overscaledZ === 0) return this.overscaledZ > 0;
    let t2 = this.canonical.z - e49.canonical.z;
    return t2 < 0 ? false : e49.canonical.x === this.canonical.x >> t2 && e49.canonical.y === this.canonical.y >> t2;
  }
  children(t2) {
    if (this.overscaledZ >= t2) return [new e48(this.overscaledZ + 1, this.wrap, this.canonical.z, this.canonical.x, this.canonical.y)];
    let n2 = this.canonical.z + 1, r2 = this.canonical.x * 2, i2 = this.canonical.y * 2;
    return [new e48(n2, this.wrap, n2, r2, i2), new e48(n2, this.wrap, n2, r2 + 1, i2), new e48(n2, this.wrap, n2, r2, i2 + 1), new e48(n2, this.wrap, n2, r2 + 1, i2 + 1)];
  }
  isLessThan(e49) {
    return this.wrap < e49.wrap ? true : this.wrap > e49.wrap ? false : this.overscaledZ < e49.overscaledZ ? true : this.overscaledZ > e49.overscaledZ ? false : this.canonical.x < e49.canonical.x ? true : this.canonical.x > e49.canonical.x ? false : this.canonical.y < e49.canonical.y;
  }
  wrapped() {
    return new e48(this.overscaledZ, 0, this.canonical.z, this.canonical.x, this.canonical.y);
  }
  unwrapTo(t2) {
    return new e48(this.overscaledZ, t2, this.canonical.z, this.canonical.x, this.canonical.y);
  }
  overscaleFactor() {
    return 2 ** (this.overscaledZ - this.canonical.z);
  }
  toUnwrapped() {
    return new gS(this.wrap, this.canonical);
  }
  toString() {
    return `${this.overscaledZ}/${this.canonical.x}/${this.canonical.y}`;
  }
  getTilePoint(e49) {
    return this.canonical.getTilePoint(new _d(e49.x - this.wrap, e49.y));
  }
  normalizeCoordinates(t2, n2, r2 = v) {
    if (t2 >= 0 && t2 < r2 && n2 >= 0 && n2 < r2) return { tileID: this, x: t2, y: n2 };
    let i2 = Math.floor(t2 / r2), a2 = Math.floor(n2 / r2), o2 = t2 - i2 * r2, s2 = n2 - a2 * r2, c2 = this.canonical.z, l2 = 1 << c2, u2 = this.canonical.y + a2;
    if (u2 < 0 || u2 >= l2) return null;
    let d2 = this.canonical.x + i2, f2 = this.wrap;
    return d2 < 0 ? (f2 -= Math.ceil(-d2 / l2), d2 = (d2 % l2 + l2) % l2) : d2 >= l2 && (f2 += Math.floor(d2 / l2), d2 %= l2), { tileID: new e48(this.overscaledZ, f2, c2, d2, u2), x: o2, y: s2 };
  }
};
function vS(e49, t2, n2, r2, i2) {
  e49 *= 2, e49 < 0 && (e49 = e49 * -1 - 1);
  let a2 = 1 << n2;
  return (a2 * a2 * e49 + a2 * i2 + r2).toString(36) + n2.toString(36) + t2.toString(36);
}
const yS = Math.PI * 6378137;
function bS(e49, t2, n2) {
  t2 = 2 ** n2 - t2 - 1;
  let r2 = xS(e49 * 256, t2 * 256, n2), i2 = xS((e49 + 1) * 256, (t2 + 1) * 256, n2);
  return `${r2[0]},${r2[1]},${i2[0]},${i2[1]}`;
}
function xS(e49, t2, n2) {
  let r2 = 2 * yS / 256 / 2 ** n2;
  return [e49 * r2 - yS, t2 * r2 - yS];
}
function SS(e49, t2, n2) {
  let r2 = ``;
  for (let i2 = e49; i2 > 0; i2--) {
    let e50 = 1 << i2 - 1;
    r2 += (t2 & e50 ? 1 : 0) + (n2 & e50 ? 2 : 0);
  }
  return r2;
}
G(`CanonicalTileID`, hS), G(`OverscaledTileID`, _S, { omit: [`terrainRttPosMatrix32f`] });
var CS = class {
  constructor(e49) {
    this.tileID = new _S(e49.tileID.overscaledZ, e49.tileID.wrap, e49.tileID.canonical.z, e49.tileID.canonical.x, e49.tileID.canonical.y), this.uid = e49.uid, this.zoom = e49.zoom, this.pixelRatio = e49.pixelRatio, this.tileSize = e49.tileSize, this.source = e49.source, this.overscaling = this.tileID.overscaleFactor(), this.showCollisionBoxes = e49.showCollisionBoxes, this.collectResourceTiming = !!e49.collectResourceTiming, this.returnDependencies = !!e49.returnDependencies, this.promoteId = e49.promoteId, this.inFlightDependencies = [];
  }
  async parse(e49, t2, n2, r2, i2) {
    this.data = e49, this.collisionBoxArray = new gs();
    let a2 = new F_(Object.keys(e49.layers).sort()), o2 = new cS(this.tileID, this.promoteId);
    o2.bucketLayerIDs = [];
    let s2 = {}, c2 = { featureIndex: o2, iconDependencies: {}, patternDependencies: {}, glyphDependencies: {}, dashDependencies: {}, availableImages: n2, subdivisionGranularity: i2 }, l2 = t2.familiesBySource[this.source];
    for (let t3 in l2) {
      let r3 = e49.layers[t3];
      if (!r3) continue;
      r3.version === 1 && A(`Vector tile source "${this.source}" layer "${t3}" does not use vector tile spec v2 and therefore may have some rendering errors.`);
      let i3 = a2.encode(t3), u3 = [];
      for (let e50 = 0; e50 < r3.length; e50++) {
        let n3 = r3.feature(e50), a3 = o2.getId(n3, t3);
        u3.push({ feature: n3, id: a3, index: e50, sourceLayerIndex: i3 });
      }
      for (let e50 of l2[t3]) {
        let t4 = e50[0];
        t4.source !== this.source && A(`layer.source = ${t4.source} does not equal this.source = ${this.source}`), !t4.isHidden(this.zoom, true) && (wS(e50, this.zoom, n2), (s2[t4.id] = t4.createBucket({ index: o2.bucketLayerIDs.length, layers: e50, zoom: this.zoom, pixelRatio: this.pixelRatio, overscaling: this.overscaling, collisionBoxArray: this.collisionBoxArray, sourceLayerIndex: i3, sourceID: this.source })).populate(u3, c2, this.tileID.canonical), o2.bucketLayerIDs.push(e50.map((e51) => e51.id)));
      }
    }
    let u2 = E(c2.glyphDependencies, (e50) => E(e50, (e51) => Object.keys(e51)));
    for (let e50 of this.inFlightDependencies) e50 == null ? void 0 : e50.abort();
    this.inFlightDependencies = [];
    let d2 = Promise.resolve({});
    if (Object.keys(u2).length) {
      let e50 = new AbortController();
      this.inFlightDependencies.push(e50), d2 = r2.sendAsync({ type: `GG`, data: { stacks: u2, source: this.source, tileID: this.tileID, type: `glyphs` } }, e50);
    }
    let f2 = Object.keys(c2.iconDependencies), p2 = Promise.resolve({});
    if (f2.length) {
      let e50 = new AbortController();
      this.inFlightDependencies.push(e50), p2 = r2.sendAsync({ type: `GI`, data: { icons: f2, source: this.source, tileID: this.tileID, type: `icons` } }, e50);
    }
    let m2 = Object.keys(c2.patternDependencies), h2 = Promise.resolve({});
    if (m2.length) {
      let e50 = new AbortController();
      this.inFlightDependencies.push(e50), h2 = r2.sendAsync({ type: `GI`, data: { icons: m2, source: this.source, tileID: this.tileID, type: `patterns` } }, e50);
    }
    let g2 = c2.dashDependencies, _2 = Promise.resolve({});
    if (Object.keys(g2).length) {
      let e50 = new AbortController();
      this.inFlightDependencies.push(e50), _2 = r2.sendAsync({ type: `GDA`, data: { dashes: g2 } }, e50);
    }
    let [v2, y2, b2, x2] = await Promise.all([d2, p2, h2, _2]), S2 = new pS(v2), C2 = new Eh(y2, b2);
    for (let e50 in s2) {
      let t3 = s2[e50];
      t3.hasDependencies && (wS(t3.layers, this.zoom, n2), t3.addFeatures({ options: c2, canonical: this.tileID.canonical, glyphMap: v2, glyphPositions: S2.positions, iconMap: y2, iconPositions: C2.iconPositions, patternMap: b2, patternPositions: C2.patternPositions, dashPositions: x2, showCollisionBoxes: this.showCollisionBoxes }));
    }
    return { buckets: Object.values(s2).filter((e50) => !e50.isEmpty()), featureIndex: o2, collisionBoxArray: this.collisionBoxArray, glyphAtlasImage: S2.image, imageAtlas: C2, dashPositions: x2, glyphMap: this.returnDependencies ? v2 : null, iconMap: this.returnDependencies ? y2 : null, glyphPositions: this.returnDependencies ? S2.positions : null };
  }
};
function wS(e49, t2, n2) {
  let r2 = new K(t2);
  for (let t3 of e49) t3.recalculate(r2, n2);
}
var TS = class {
  constructor() {
    this.loading = {}, this.loaded = {}, this.parsing = {};
  }
  startLoading(e49, t2) {
    this.loading[e49] = t2;
  }
  finishLoading(e49) {
    delete this.loading[e49];
  }
  abort(e49) {
    let t2 = this.loading[e49];
    (t2 == null ? void 0 : t2.abort) && (t2.abort.abort(), delete this.loading[e49]);
  }
  getParsing(e49) {
    return this.parsing[e49];
  }
  setParsing(e49, t2) {
    this.parsing[e49] = t2;
  }
  removeParsing(e49) {
    delete this.parsing[e49];
  }
  markLoaded(e49, t2) {
    this.loaded[e49] = t2;
  }
  getLoaded(e49) {
    let t2 = this.loaded[e49];
    if (t2) return t2;
  }
  removeLoaded(e49) {
    delete this.loaded[e49];
  }
  clearLoaded() {
    this.loaded = {};
  }
}, ES = class {
  constructor(e49) {
    this.maxEntries = e49, this.map = /* @__PURE__ */ new Map();
  }
  get(e49) {
    let t2 = this.map.get(e49);
    return t2 !== void 0 && (this.map.delete(e49), this.map.set(e49, t2)), t2;
  }
  set(e49, t2) {
    if (this.map.has(e49)) this.map.delete(e49);
    else if (this.map.size >= this.maxEntries) {
      let e50 = this.map.keys().next().value;
      this.map.delete(e50);
    }
    this.map.set(e49, t2);
  }
  clear() {
    this.map.clear();
  }
}, DS = class {
  constructor(e49) {
    this.start = `${e49}#start`, this.end = `${e49}#end`, this.measure = e49, performance.mark(this.start);
  }
  finish() {
    performance.mark(this.end);
    let e49 = performance.getEntriesByName(this.measure);
    return e49.length === 0 && (performance.measure(this.measure, this.start, this.end), e49 = performance.getEntriesByName(this.measure), performance.clearMarks(this.start), performance.clearMarks(this.end), performance.clearMeasures(this.measure)), e49;
  }
}, OS = class {
  constructor(e49, t2, n2, r2, i2) {
    this.type = e49, this.properties = n2 || {}, this.extent = i2, this.pointsArray = t2, this.id = r2;
  }
  loadGeometry() {
    return this.pointsArray.map((e49) => e49.map((e50) => new l(e50.x, e50.y)));
  }
}, kS = class {
  constructor(e49, t2, n2) {
    this.version = 2, this._myFeatures = e49, this.name = t2, this.length = e49.length, this.extent = n2;
  }
  feature(e49) {
    return this._myFeatures[e49];
  }
}, AS = class {
  constructor() {
    this.layers = {};
  }
  addLayer(e49) {
    this.layers[e49.name] = e49;
  }
};
function jS(e49, t2, n2) {
  let { extent: r2 } = e49, i2 = 2 ** (n2.z - t2.z), a2 = (n2.x - t2.x * i2) * r2, o2 = (n2.y - t2.y * i2) * r2, s2 = [];
  for (let t3 = 0; t3 < e49.length; t3++) {
    let n3 = e49.feature(t3), c2 = n3.loadGeometry();
    for (let e50 of c2) for (let t4 of e50) t4.x = t4.x * i2 - a2, t4.y = t4.y * i2 - o2;
    c2 = _g(c2, n3.type, -128, -128, r2 + 128, r2 + 128), c2.length !== 0 && s2.push(new OS(n3.type, c2, n3.properties, n3.id, r2));
  }
  return new kS(s2, e49.name, r2);
}
var MS = class {
  constructor(e49, t2, n2) {
    this.actor = e49, this.layerIndex = t2, this.availableImages = n2, this.tileState = new TS(), this.overzoomedTileResultCache = new ES(1e3);
  }
  loadVectorTile(e49, t2) {
    try {
      return { vectorTile: e49.encoding === `mlt` ? new sS(t2) : new ad(new ih(t2)), rawData: t2 };
    } catch (n2) {
      let r2 = new Uint8Array(t2), i2 = r2[0] === 31 && r2[1] === 139, a2 = `Unable to parse the tile at ${e49.request.url}, `;
      throw a2 += i2 ? `please make sure the data is not gzipped and that you have configured the relevant header in the server` : `got error: ${y(n2).message}`, Error(a2);
    }
  }
  async loadTile(e49) {
    let { uid: t2, overzoomParameters: n2 } = e49;
    n2 && (e49.request = n2.overzoomRequest);
    let r2 = this._startRequestTiming(e49), i2 = new CS(e49);
    this.tileState.startLoading(t2, i2);
    let a2 = new AbortController();
    i2.abort = a2;
    try {
      let o2 = await to(e49.request, a2);
      if (e49.etag && e49.etag === o2.etag) return this.tileState.finishLoading(t2), this._getEtagUnmodifiedResult(o2, r2);
      let s2 = this.loadVectorTile(e49, o2.data);
      if (this.tileState.finishLoading(t2), !s2) return null;
      let { vectorTile: c2, rawData: l2 } = s2;
      n2 && ({ vectorTile: c2, rawData: l2 } = this._getOverzoomTile(e49, c2));
      let u2 = this._getExpiryData(o2), d2 = this._finishRequestTiming(r2);
      i2.vectorTile = c2, i2.etag = o2.etag, this.tileState.markLoaded(t2, i2);
      let f2 = { rawData: l2, cacheControl: u2, resourceTiming: d2 };
      return this.tileState.setParsing(t2, f2), await this._parseWorkerTile(i2, e49);
    } catch (e50) {
      throw this.tileState.finishLoading(t2), this.tileState.markLoaded(t2, i2), e50;
    }
  }
  _getEtagUnmodifiedResult(e49, t2) {
    return w({ etagUnmodified: true }, this._getExpiryData(e49), this._finishRequestTiming(t2));
  }
  async _parseWorkerTile(e49, t2) {
    let n2 = this.tileState.getParsing(e49.uid), r2 = await e49.parse(e49.vectorTile, this.layerIndex, this.availableImages, this.actor, t2.subdivisionGranularity);
    if (n2) {
      let { rawData: i2, cacheControl: a2, resourceTiming: o2 } = n2, s2 = t2.overzoomParameters ? `mvt` : t2.encoding;
      r2 = w({ rawTileData: i2.slice(0), encoding: s2 }, r2, a2, o2), this.tileState.removeParsing(e49.uid);
    } else e49.etag && (r2 = w(r2, { etag: e49.etag }));
    return r2;
  }
  _getExpiryData({ expires: e49, cacheControl: t2, etag: n2 }) {
    let r2 = {};
    return e49 && (r2.expires = e49), t2 && (r2.cacheControl = t2), n2 && (r2.etag = n2), r2;
  }
  _startRequestTiming(e49) {
    var _a3;
    if ((_a3 = e49.request) == null ? void 0 : _a3.collectResourceTiming) return new DS(e49.request.url);
  }
  _finishRequestTiming(e49) {
    let t2 = e49 == null ? void 0 : e49.finish();
    return t2 ? { resourceTiming: JSON.parse(JSON.stringify(t2)) } : {};
  }
  _getOverzoomTile(e49, t2) {
    var _a3;
    let { tileID: n2, source: r2, overzoomParameters: i2 } = e49, { maxZoomTileID: a2 } = i2, o2 = `${a2.key}_${n2.key}_${(_a3 = e49.request) == null ? void 0 : _a3.url}`, s2 = this.overzoomedTileResultCache.get(o2);
    if (s2) return s2;
    let c2 = new AS(), l2 = this.layerIndex.familiesBySource[r2];
    for (let e50 in l2) {
      let r3 = t2.layers[e50];
      if (!r3) continue;
      let i3 = jS(r3, a2, n2.canonical);
      i3.length > 0 && c2.addLayer(i3);
    }
    let u2 = { vectorTile: c2, rawData: E_(c2).buffer };
    return this.overzoomedTileResultCache.set(o2, u2), u2;
  }
  async reloadTile(e49) {
    let t2 = e49.uid, n2 = this.tileState.getLoaded(t2);
    if (!n2) throw Error(`Should not be trying to reload a tile that was never loaded or has been removed`);
    if (n2.vectorTile) return n2.showCollisionBoxes = e49.showCollisionBoxes, await this._parseWorkerTile(n2, e49);
  }
  async abortTile(e49) {
    this.tileState.abort(e49.uid);
  }
  async removeTile(e49) {
    this.tileState.removeLoaded(e49.uid);
  }
}, NS = class {
  constructor() {
    this.loaded = {};
  }
  async loadTile(e49) {
    let { uid: t2, encoding: n2, rawImageData: r2, redFactor: i2, greenFactor: a2, blueFactor: o2, baseShift: s2 } = e49, c2 = r2.width + 4, l2 = r2.height + 4, u2 = new Cl(t2, re(r2) ? new ul({ width: c2, height: l2 }, await le(r2, -2, -2, c2, l2)) : r2, n2, i2, a2, o2, s2);
    return this.loaded ||= {}, this.loaded[t2] = u2, u2;
  }
  removeTile(e49) {
    let t2 = this.loaded, n2 = e49.uid;
    (t2 == null ? void 0 : t2[n2]) && delete t2[n2];
  }
}, PS = class {
  constructor(e49, t2, n2, r2 = FS) {
    this.actor = e49, this.layerIndex = t2, this.availableImages = n2, this.tileState = new TS(), this._createGeoJSONIndex = r2;
  }
  loadVectorTile(e49) {
    if (!this._geoJSONIndex) throw Error(`Unable to parse the data into a cluster or geojson`);
    let { z: t2, x: n2, y: r2 } = e49.tileID.canonical, i2 = this._geoJSONIndex.getTile(t2, n2, r2);
    if (!i2) return null;
    let a2 = new T_(i2.features, { version: 2, extent: v });
    return { vectorTile: a2, rawData: E_(a2, `__$json__:`).buffer };
  }
  async loadTile(e49) {
    let { uid: t2 } = e49, n2 = new CS(e49);
    n2.abort = new AbortController();
    try {
      let r2 = this.loadVectorTile(e49);
      if (!r2) return null;
      let { vectorTile: i2, rawData: a2 } = r2;
      n2.vectorTile = i2, this.tileState.markLoaded(t2, n2);
      let o2 = { rawData: a2 };
      return this.tileState.setParsing(t2, o2), await this._parseWorkerTile(n2, e49);
    } catch (e50) {
      throw this.tileState.markLoaded(t2, n2), e50;
    }
  }
  async _parseWorkerTile(e49, t2) {
    let n2 = this.tileState.getParsing(e49.uid), r2 = await e49.parse(e49.vectorTile, this.layerIndex, this.availableImages, this.actor, t2.subdivisionGranularity);
    if (n2) {
      let { rawData: t3 } = n2;
      r2 = w({ rawTileData: t3.slice(0), encoding: `mvt` }, r2), this.tileState.removeParsing(e49.uid);
    }
    return r2;
  }
  async abortTile(e49) {
    this.tileState.abort(e49.uid);
  }
  async removeTile(e49) {
    this.tileState.removeLoaded(e49.uid);
  }
  async loadData(e49) {
    var _a3;
    (_a3 = this._pendingRequest) == null ? void 0 : _a3.abort();
    let t2 = this._startRequestTiming(e49);
    this._pendingRequest = new AbortController();
    try {
      await this.loadAndProcessGeoJSON(e49, this._pendingRequest), delete this._pendingRequest, this.tileState.clearLoaded();
      let n2 = {};
      return e49.request && (n2.data = e49.data), this._finishRequestTiming(t2, e49, n2), n2;
    } catch (e50) {
      if (delete this._pendingRequest, !me(e50)) throw e50;
      return { abandoned: true };
    }
  }
  _startRequestTiming(e49) {
    var _a3;
    if ((_a3 = e49.request) == null ? void 0 : _a3.collectResourceTiming) return new DS(e49.request.url);
  }
  _finishRequestTiming(e49, t2, n2) {
    let r2 = e49 == null ? void 0 : e49.finish();
    r2 && (n2.resourceTiming = { [t2.source]: JSON.parse(JSON.stringify(r2)) });
  }
  async reloadTile(e49) {
    let t2 = e49.uid, n2 = this.tileState.getLoaded(t2);
    if (!n2) return await this.loadTile(e49);
    if (n2.vectorTile) return n2.showCollisionBoxes = e49.showCollisionBoxes, await this._parseWorkerTile(n2, e49);
  }
  async loadAndProcessGeoJSON(e49, t2) {
    if (e49.request && (e49.data = (await eo(e49.request, t2)).data), e49.data) {
      e49.data = this._filterGeoJSON(e49.data, e49.filter, e49.source), this._geoJSONIndex = this._createGeoJSONIndex(e49.data, e49);
      return;
    }
    if (e49.dataDiff && (this._geoJSONIndex ??= this._createGeoJSONIndex({ type: `FeatureCollection`, features: [] }, e49), this._geoJSONIndex.updateData(e49.dataDiff, this._getFilterPredicate(e49.filter, e49.source))), e49.updateCluster && this._geoJSONIndex.updateClusterOptions(e49.geojsonVtOptions.cluster, IS(e49)), this._geoJSONIndex == null) throw Error(`Input data given to '${e49.source}' is not a valid GeoJSON object.`);
  }
  _filterGeoJSON(e49, t2, n2) {
    if (e49.type !== `FeatureCollection`) return e49;
    let r2 = this._getFilterPredicate(t2, n2);
    return r2 ? { type: `FeatureCollection`, features: e49.features.filter((e50) => r2(e50)) } : e49;
  }
  _getFilterPredicate(e49, t2) {
    if (typeof e49 != `boolean` && !(e49 == null ? void 0 : e49.length)) return;
    let n2 = li(e49, `sources.${t2}.filter`, { type: `boolean`, "property-type": `data-driven`, overridable: false, transition: false });
    if (n2.result === `error`) throw Error(n2.value.map((e50) => `${e50.key}: ${e50.message}`).join(`, `));
    return (e50) => n2.value.evaluate({ zoom: 0 }, e50);
  }
  async removeSource(e49) {
    var _a3;
    (_a3 = this._pendingRequest) == null ? void 0 : _a3.abort();
  }
  getClusterExpansionZoom(e49) {
    return this._geoJSONIndex.getClusterExpansionZoom(e49.clusterId);
  }
  getClusterChildren(e49) {
    return this._geoJSONIndex.getClusterChildren(e49.clusterId);
  }
  getClusterLeaves(e49) {
    return this._geoJSONIndex.getClusterLeaves(e49.clusterId, e49.limit, e49.offset);
  }
};
function FS(e49, t2) {
  return new mp(e49, w(t2.geojsonVtOptions || {}, { updateable: true, clusterOptions: IS(t2) }));
}
function IS({ geojsonVtOptions: e49, clusterProperties: t2, source: n2 }) {
  if (!t2 || !e49.clusterOptions) return e49.clusterOptions;
  let r2 = {}, i2 = {}, a2 = { accumulated: null, zoom: 0 }, o2 = { properties: null }, s2 = Object.keys(t2);
  for (let e50 of s2) {
    let [a3, o3] = t2[e50], s3 = li(o3, `sources.${n2}.clusterProperties.${e50}[1]`), c2 = li(typeof a3 == `string` ? [a3, [`accumulated`], [`get`, e50]] : a3, `sources.${n2}.clusterProperties.${e50}[0]`);
    r2[e50] = s3.value, i2[e50] = c2.value;
  }
  return e49.clusterOptions.map = (e50) => {
    o2.properties = e50;
    let t3 = {};
    for (let e51 of s2) t3[e51] = r2[e51].evaluate(a2, o2);
    return t3;
  }, e49.clusterOptions.reduce = (e50, t3) => {
    o2.properties = t3;
    for (let t4 of s2) a2.accumulated = e50[t4], e50[t4] = i2[t4].evaluate(a2, o2);
  }, e49.clusterOptions;
}
async function LS(e49) {
  if (e49.endsWith(`.mjs`)) {
    await import(
      /* @vite-ignore */
      e49
    );
    return;
  }
  let t2 = await fetch(e49, { credentials: `same-origin` });
  if (!t2.ok) throw Error(`Failed to load ${e49}: ${t2.status}`);
  let n2 = await t2.text();
  if (/^[ \t]*(import|export)\s/m.test(n2)) {
    let e50 = URL.createObjectURL(new Blob([n2], { type: `text/javascript` }));
    try {
      await import(
        /* @vite-ignore */
        e50
      );
    } finally {
      URL.revokeObjectURL(e50);
    }
    return;
  }
  globalThis.eval(n2);
}
var RS = class {
  constructor(e49) {
    this.self = e49, this.actor = new fo(e49), this.layerIndexes = {}, this.availableImages = {}, this.workerSources = {}, this.demWorkerSources = {}, this.externalWorkerSourceTypes = {}, this.globalStates = /* @__PURE__ */ new Map(), this.self.registerWorkerSource = (e50, t2) => {
      if (this.externalWorkerSourceTypes[e50]) throw Error(`Worker source with name "${e50}" already registered.`);
      this.externalWorkerSourceTypes[e50] = t2;
    }, this.self.addProtocol = Ga, this.self.removeProtocol = Ka, this.self.registerRTLTextPlugin = (e50) => {
      Ip.setMethods(e50);
    }, this.self.makeRequest = $a, this.actor.registerMessageHandler(`LDT`, (e50, t2) => this._getDEMWorkerSource(e50, t2.source).loadTile(t2)), this.actor.registerMessageHandler(`RDT`, async (e50, t2) => {
      this._getDEMWorkerSource(e50, t2.source).removeTile(t2);
    }), this.actor.registerMessageHandler(`GCEZ`, async (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).getClusterExpansionZoom(t2)), this.actor.registerMessageHandler(`GCC`, async (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).getClusterChildren(t2)), this.actor.registerMessageHandler(`GCL`, async (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).getClusterLeaves(t2)), this.actor.registerMessageHandler(`LD`, (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).loadData(t2)), this.actor.registerMessageHandler(`LT`, (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).loadTile(t2)), this.actor.registerMessageHandler(`RT`, (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).reloadTile(t2)), this.actor.registerMessageHandler(`AT`, (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).abortTile(t2)), this.actor.registerMessageHandler(`RMT`, (e50, t2) => this._getWorkerSource(e50, t2.type, t2.source).removeTile(t2)), this.actor.registerMessageHandler(`RS`, async (e50, t2) => {
      var _a3, _b3;
      if (!((_b3 = (_a3 = this.workerSources[e50]) == null ? void 0 : _a3[t2.type]) == null ? void 0 : _b3[t2.source])) return;
      let n2 = this.workerSources[e50][t2.type][t2.source];
      delete this.workerSources[e50][t2.type][t2.source], n2.removeSource !== void 0 && n2.removeSource(t2);
    }), this.actor.registerMessageHandler(`RM`, async (e50) => {
      delete this.layerIndexes[e50], delete this.availableImages[e50], delete this.workerSources[e50], delete this.demWorkerSources[e50], this.globalStates.delete(e50);
    }), this.actor.registerMessageHandler(`SR`, async (e50, t2) => {
      this.referrer = t2;
    }), this.actor.registerMessageHandler(`SRPS`, (e50, t2) => this._syncRTLPluginState(e50, t2)), this.actor.registerMessageHandler(`IS`, async (e50, t2) => {
      await LS(t2);
    }), this.actor.registerMessageHandler(`SI`, (e50, t2) => this._setImages(e50, t2)), this.actor.registerMessageHandler(`UL`, async (e50, t2) => {
      this._getLayerIndex(e50).update(t2.layers, t2.removedIds, this._getGlobalState(e50));
    }), this.actor.registerMessageHandler(`UGS`, async (e50, t2) => {
      let n2 = this._getGlobalState(e50);
      for (let e51 in t2) n2[e51] = t2[e51];
    }), this.actor.registerMessageHandler(`SL`, async (e50, t2) => {
      this._getLayerIndex(e50).replace(t2, this._getGlobalState(e50));
    });
  }
  _getGlobalState(e49) {
    let t2 = this.globalStates.get(e49);
    return t2 || (t2 = {}, this.globalStates.set(e49, t2)), t2;
  }
  async _setImages(e49, t2) {
    this.availableImages[e49] = t2;
    for (let n2 in this.workerSources[e49]) {
      let r2 = this.workerSources[e49][n2];
      for (let e50 in r2) r2[e50].availableImages = t2;
    }
  }
  async _syncRTLPluginState(e49, t2) {
    return await Ip.syncState(t2, LS);
  }
  _getAvailableImages(e49) {
    let t2 = this.availableImages[e49];
    return t2 ||= [], t2;
  }
  _getLayerIndex(e49) {
    let t2 = this.layerIndexes[e49];
    return t2 ||= this.layerIndexes[e49] = new fS(), t2;
  }
  _getWorkerSource(e49, t2, n2) {
    if (this.workerSources[e49] ||= {}, this.workerSources[e49][t2] ||= {}, !this.workerSources[e49][t2][n2]) {
      let r2 = { sendAsync: (t3, n3) => (t3.targetMapId = e49, this.actor.sendAsync(t3, n3)) };
      switch (t2) {
        case `vector`:
          this.workerSources[e49][t2][n2] = new MS(r2, this._getLayerIndex(e49), this._getAvailableImages(e49));
          break;
        case `geojson`:
          this.workerSources[e49][t2][n2] = new PS(r2, this._getLayerIndex(e49), this._getAvailableImages(e49));
          break;
        default:
          this.workerSources[e49][t2][n2] = new this.externalWorkerSourceTypes[t2](r2, this._getLayerIndex(e49), this._getAvailableImages(e49));
      }
    }
    return this.workerSources[e49][t2][n2];
  }
  _getDEMWorkerSource(e49, t2) {
    return this.demWorkerSources[e49] ||= {}, this.demWorkerSources[e49][t2] ||= new NS(), this.demWorkerSources[e49][t2];
  }
};
ne(self) && (self.worker = new RS(self));
export {
  RS as default
};
