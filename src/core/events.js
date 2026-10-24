/**
 * Lightweight Event Emitter for decoupled module communication.
 */
class EventEmitter {
  constructor() {
    this._events = new Map();
  }

  on(event, handler) {
    if (typeof handler !== 'function') return this;
    if (!this._events.has(event)) {
      this._events.set(event, []);
    }
    this._events.get(event).push(handler);
    return this;
  }

  off(event, handler) {
    if (!this._events.has(event)) return this;
    if (!handler) {
      this._events.delete(event);
      return this;
    }
    const handlers = this._events.get(event);
    const idx = handlers.indexOf(handler);
    if (idx !== -1) {
      handlers.splice(idx, 1);
    }
    if (handlers.length === 0) {
      this._events.delete(event);
    }
    return this;
  }

  once(event, handler) {
    const wrapper = (...args) => {
      this.off(event, wrapper);
      handler.apply(this, args);
    };
    return this.on(event, wrapper);
  }

  emit(event, ...args) {
    if (!this._events.has(event)) return false;
    const handlers = [...this._events.get(event)];
    for (const h of handlers) {
      try {
        h(...args);
      } catch (err) {
        console.error(`[EventEmitter] Error in event '${event}':`, err);
      }
    }
    return true;
  }

  clear() {
    this._events.clear();
  }
}

if (typeof module !== 'undefined') {
  module.exports = { EventEmitter };
}
