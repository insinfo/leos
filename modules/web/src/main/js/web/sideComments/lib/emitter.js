/*
 * Copyright 2015 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
/*
 * The MIT License (MIT)
 * https://github.com/component/emitter
 */

; // jshint ignore:line
define(function Emitter(require) {
    "use strict";

    /**
     * Initialize a new `Emitter`.
     * 
     * @api public
     */

    function Emitter(obj) {
        if (obj)
            return mixin(obj);
    }
    ;

    /**
     * Mixin the emitter properties.
     * 
     * @param {Object}
     *         obj
     * @return {Object}
     * @api private
     */

    function mixin(obj) {
        for ( var key in Emitter.prototype) {
            obj[key] = Emitter.prototype[key];
        }
        return obj;
    }

    /**
     * Listen on the given `event` with `fn`.
     * 
     * @param {String}
     *         event
     * @param {Function}
     *         fn
     * @return {Emitter}
     * @api public
     */

    Emitter.prototype.on = Emitter.prototype.addEventListener = function(event, fn) {
        this._callbacks = this._callbacks || {};
        (this._callbacks[event] = this._callbacks[event] || []).push(fn);
        return this;
    };

    /**
     * Adds an `event` listener that will be invoked a single time then automatically removed.
     * 
     * @param {String}
     *         event
     * @param {Function}
     *         fn
     * @return {Emitter}
     * @api public
     */

    Emitter.prototype.once = function(event, fn) {
        var self = this;
        this._callbacks = this._callbacks || {};

        function on() {
            self.off(event, on);
            fn.apply(this, arguments);
        }

        on.fn = fn;
        this.on(event, on);
        return this;
    };

    /**
     * Remove the given callback for `event` or all registered callbacks.
     * 
     * @param {String}
     *         event
     * @param {Function}
     *         fn
     * @return {Emitter}
     * @api public
     */

    Emitter.prototype.off = Emitter.prototype.removeListener = Emitter.prototype.removeAllListeners = Emitter.prototype.removeEventListener = function(event,
            fn) {
        this._callbacks = this._callbacks || {};

        // all
        if (0 == arguments.length) {
            this._callbacks = {};
            return this;
        }

        // specific event
        var callbacks = this._callbacks[event];
        if (!callbacks)
            return this;

        // remove all handlers
        if (1 == arguments.length) {
            delete this._callbacks[event];
            return this;
        }

        // remove specific handler
        var cb;
        for (var i = 0; i < callbacks.length; i++) {
            cb = callbacks[i];
            if (cb === fn || cb.fn === fn) {
                callbacks.splice(i, 1);
                break;
            }
        }
        return this;
    };

    /**
     * Emit `event` with the given args.
     * 
     * @param {String}
     *         event
     * @param {Mixed}
     *         ...
     * @return {Emitter}
     */

    Emitter.prototype.emit = function(event) {
        this._callbacks = this._callbacks || {};
        var args = [].slice.call(arguments, 1), callbacks = this._callbacks[event];

        if (callbacks) {
            callbacks = callbacks.slice(0);
            for (var i = 0, len = callbacks.length; i < len; ++i) {
                callbacks[i].apply(this, args);
            }
        }

        return this;
    };

    /**
     * Return array of callbacks for `event`.
     *
     * @param {String} event
     * @return {Array}
     * @api public
     */

    Emitter.prototype.listeners = function(event) {
        this._callbacks = this._callbacks || {};
        return this._callbacks[event] || [];
    };

    /**
     * Check if this emitter has `event` handlers.
     *
     * @param {String} event
     * @return {Boolean}
     * @api public
     */

    Emitter.prototype.hasListeners = function(event) {
        return !!this.listeners(event).length;
    };

    return Emitter;
});
