/**
 * Path and Waypoints Navigation Engine.
 */

class PathFollower {
  constructor(waypoints = []) {
    this.waypoints = waypoints;
    this.currentIndex = 0;
    this.x = waypoints.length > 0 ? waypoints[0].x : 0;
    this.y = waypoints.length > 0 ? waypoints[0].y : 0;
    this.completed = false;
    this.totalDistance = this._calculateTotalDistance();
    this.distanceTraveled = 0;
  }

  _calculateTotalDistance() {
    let total = 0;
    for (let i = 0; i < this.waypoints.length - 1; i++) {
      const p1 = this.waypoints[i];
      const p2 = this.waypoints[i + 1];
      const dx = p2.x - p1.x;
      const dy = p2.y - p1.y;
      total += Math.hypot(dx, dy);
    }
    return total;
  }

  update(speed, dt) {
    if (this.completed || this.waypoints.length < 2) return;

    let moveDistance = speed * dt;
    this.distanceTraveled += moveDistance;

    while (moveDistance > 0 && !this.completed) {
      const targetPoint = this.waypoints[this.currentIndex + 1];
      if (!targetPoint) {
        this.completed = true;
        break;
      }

      const dx = targetPoint.x - this.x;
      const dy = targetPoint.y - this.y;
      const distToTarget = Math.hypot(dx, dy);

      if (distToTarget <= moveDistance) {
        // Reached this waypoint, snap to it and consume distance
        this.x = targetPoint.x;
        this.y = targetPoint.y;
        moveDistance -= distToTarget;
        this.currentIndex += 1;

        if (this.currentIndex >= this.waypoints.length - 1) {
          this.completed = true;
          break;
        }
      } else {
        // Step partially toward target waypoint
        const ratio = moveDistance / distToTarget;
        this.x += dx * ratio;
        this.y += dy * ratio;
        moveDistance = 0;
      }
    }
  }

  getRemainingDistance() {
    return Math.max(0, this.totalDistance - this.distanceTraveled);
  }
}

if (typeof module !== 'undefined') {
  module.exports = { PathFollower };
}
