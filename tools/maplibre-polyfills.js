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
