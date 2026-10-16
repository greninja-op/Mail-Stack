(function () {
  "use strict";
  /* ---------- Bubble: two-edge spring jelly (matches the app) ---------- */
  var bar = document.getElementById("bubbleBar");
  var pill = document.getElementById("bubblePill");
  var btns = Array.prototype.slice.call(document.querySelectorAll(".navbtn"));
  var REST_W = 64, REST_H = 32;
  var selected = 1;
  var curL = 0, curR = REST_W, velL = 0, velR = 0;
  var tgtL = 0, tgtR = REST_W, raf = null, lastT = 0;

  function targetsFor(i) {
    var b = btns[i].getBoundingClientRect();
    var r = bar.getBoundingClientRect();
    var c = b.left - r.left + b.width / 2;
    return [c - REST_W / 2, c + REST_W / 2];
  }
  function paint() {
    pill.style.left = curL + "px";
    pill.style.width = Math.max(8, curR - curL) + "px";
    var stretch = Math.max(0, (curR - curL - REST_W) / REST_W);
    var squash = Math.min(0.18, stretch * 0.35);
    pill.style.transform = "translateY(-50%) scaleY(" + (1 - squash) + ")";
  }
  function step(ts) {
    if (!lastT) lastT = ts;
    var dt = Math.min(1 / 30, (ts - lastT) / 1000);
    lastT = ts;
    var dir = (tgtL + tgtR) - (curL + curR) >= 0 ? 1 : -1;
    // Leading edge (direction of travel) = stiff spring 550.
    // Trailing edge = soft spring 300. Damping ratio 0.70 both.
    var kLead = 550, kTrail = 300, zeta = 0.70;
    var kL = dir > 0 ? kTrail : kLead;
    var kR = dir > 0 ? kLead : kTrail;
    var sub = 8, h = dt / sub;
    for (var s = 0; s < sub; s++) {
      var cL = 2 * zeta * Math.sqrt(kL), aL = -kL * (curL - tgtL) - cL * velL;
      velL += aL * h; curL += velL * h;
      var cR = 2 * zeta * Math.sqrt(kR), aR = -kR * (curR - tgtR) - cR * velR;
      velR += aR * h; curR += velR * h;
    }
    paint();
    var settled = Math.abs(curL - tgtL) < 0.1 && Math.abs(curR - tgtR) < 0.1 &&
                  Math.abs(velL) < 0.1 && Math.abs(velR) < 0.1;
    if (settled) { curL = tgtL; curR = tgtR; velL = 0; velR = 0; paint(); raf = null; return; }
    raf = requestAnimationFrame(step);
  }
  function goTo(i) {
    if (i === selected) return;
    selected = i;
    btns.forEach(function (b, j) {
      var on = j === i;
      b.classList.toggle("selected", on);
      b.setAttribute("aria-selected", on ? "true" : "false");
    });
    var t = targetsFor(i);
    tgtL = t[0]; tgtR = t[1];
    if (!raf) { lastT = 0; raf = requestAnimationFrame(step); }
  }
  function snap() {
    var t = targetsFor(selected);
    curL = tgtL = t[0]; curR = tgtR = t[1]; velL = 0; velR = 0;
    paint();
  }
  btns.forEach(function (b) {
    b.addEventListener("click", function () { goTo(parseInt(b.dataset.index, 10)); });
  });
  window.addEventListener("load", snap);
  window.addEventListener("resize", function () { if (!raf) snap(); else { var t = targetsFor(selected); tgtL = t[0]; tgtR = t[1]; } });
  snap();

  /* ---------- Scroll edge fade: fade only where more content waits ---------- */
  var list = document.getElementById("fadeList");
  function updateFade() {
    var top = list.scrollTop > 0;
    var bottom = list.scrollTop + list.clientHeight < list.scrollHeight - 1;
    var stops = [];
    if (top) { stops.push("rgba(0,0,0,0) 0px", "rgba(0,0,0,1) 28px"); }
    else { stops.push("rgba(0,0,0,1) 0px"); }
    if (bottom) { stops.push("rgba(0,0,0,1) calc(100% - 28px)", "rgba(0,0,0,0) 100%"); }
    else { stops.push("rgba(0,0,0,1) 100%"); }
    var g = "linear-gradient(to bottom, " + stops.join(", ") + ")";
    list.style.webkitMaskImage = g;
    list.style.maskImage = g;
  }
  list.addEventListener("scroll", updateFade, { passive: true });
  window.addEventListener("load", updateFade);
  updateFade();
})();
