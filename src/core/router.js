const { EventEmitter } = require('./events');

/**
 * Robust Central Navigation Router with History Stack and Lifecycle Management.
 * Solves the navigation failures of the previous project completely.
 */
class ScreenRouter extends EventEmitter {
  constructor(options = {}) {
    super();
    this.routes = new Map();
    this.history = [];
    this.currentRoute = null;
    this.currentScreen = null;
    this.defaultRoute = options.defaultRoute || 'main_menu';
    this.domContainer = options.domContainer || null;
  }

  registerRoute(name, screen) {
    if (!name || typeof name !== 'string') {
      throw new Error(`[Router] Route name must be a non-empty string.`);
    }
    if (!screen || typeof screen !== 'object') {
      throw new Error(`[Router] Screen handler for '${name}' must be an object.`);
    }
    this.routes.set(name, screen);
    if (typeof screen.init === 'function') {
      try {
        screen.init(this);
      } catch (err) {
        console.error(`[Router] Error initializing screen '${name}':`, err);
      }
    }
    return this;
  }

  hasRoute(name) {
    return this.routes.has(name);
  }

  getCurrentRoute() {
    return this.currentRoute;
  }

  getHistory() {
    return [...this.history];
  }

  navigate(routeName, params = {}) {
    if (!this.routes.has(routeName)) {
      console.error(`[Router] Unknown destination: '${routeName}'. Aborting navigation.`);
      return false;
    }

    if (this.currentRoute === routeName && !params.forceReload) {
      // Already on this route
      return true;
    }

    const previousRoute = this.currentRoute;
    const previousScreen = this.currentScreen;
    const nextScreen = this.routes.get(routeName);

    // Call exit on previous screen if it exists
    if (previousScreen && typeof previousScreen.onExit === 'function') {
      try {
        previousScreen.onExit();
      } catch (err) {
        console.error(`[Router] Error exiting screen '${previousRoute}':`, err);
      }
    }

    // Push to history if navigating from a valid route and not marked as replace
    if (previousRoute && !params.replace) {
      this.history.push({ route: previousRoute, params: params.previousParams || {} });
    }

    this.currentRoute = routeName;
    this.currentScreen = nextScreen;

    // DOM UI synchronization if available
    this._syncDom(routeName);

    // Call enter lifecycle
    if (typeof nextScreen.onEnter === 'function') {
      try {
        nextScreen.onEnter(params);
      } catch (err) {
        console.error(`[Router] Error entering screen '${routeName}':`, err);
      }
    }

    this.emit('route_changed', {
      current: routeName,
      previous: previousRoute,
      historyDepth: this.history.length,
      params
    });

    return true;
  }

  replace(routeName, params = {}) {
    return this.navigate(routeName, { ...params, replace: true });
  }

  back() {
    if (this.history.length > 0) {
      const prev = this.history.pop();
      return this.navigate(prev.route, { ...prev.params, replace: true });
    }
    // Fallback if history is empty
    if (this.currentRoute !== this.defaultRoute) {
      return this.navigate(this.defaultRoute, { replace: true });
    }
    return false;
  }

  clearHistory() {
    this.history = [];
  }

  _syncDom(activeRoute) {
    if (typeof document === 'undefined') return;

    // Remove active and show states from all registered screens
    const allScreens = document.querySelectorAll('.app-screen');
    allScreens.forEach(el => {
      el.classList.remove('active');
      el.setAttribute('aria-hidden', 'true');
      el.style.display = 'none'; // Guarantee no invisible panel intercepts touches
      el.style.pointerEvents = 'none';
    });

    // Activate the targeted screen
    const targetEl = document.getElementById(`screen-${activeRoute}`);
    if (targetEl) {
      targetEl.classList.add('active');
      targetEl.setAttribute('aria-hidden', 'false');
      targetEl.style.display = 'flex';
      targetEl.style.pointerEvents = 'auto';
    }
  }
}

if (typeof module !== 'undefined') {
  module.exports = { ScreenRouter };
}
