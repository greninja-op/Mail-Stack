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

  /* ---------- Top-bar reshape demo (see §3): hamburger 0<->48, pill pushed ---------- */
  var tbHam = document.getElementById("tbHamburger");
  var tbMailActs = document.getElementById("tbActionsMail");
  var tbOtherActs = document.getElementById("tbActionsOther");
  var tbBtns = Array.prototype.slice.call(document.querySelectorAll(".tb-tab"));
  tbBtns.forEach(function (b) {
    b.addEventListener("click", function () {
      tbBtns.forEach(function (x) { x.classList.remove("sel"); });
      b.classList.add("sel");
      var isMail = b.dataset.mail === "1";
      tbHam.classList.toggle("open", isMail);
      tbMailActs.classList.toggle("show", isMail);
      tbOtherActs.classList.toggle("show", !isMail);
    });
  });

  /* ---------- Directional tab slide demo (see §4): content enters from its tab's side ---------- */
  var slidePanes = Array.prototype.slice.call(document.querySelectorAll(".slide-pane"));
  var slideBtns = Array.prototype.slice.call(document.querySelectorAll(".slide-tab"));
  var slideCurrent = 0, slideBusy = false;
  function goSlide(i) {
    if (i === slideCurrent || slideBusy) return;
    slideBusy = true;
    var dir = i > slideCurrent ? 1 : -1;
    var incoming = slidePanes[i], outgoing = slidePanes[slideCurrent];
    slideBtns.forEach(function (b, j) { b.classList.toggle("sel", j === i); });
    incoming.style.visibility = "visible";
    incoming.style.transition = "none";
    incoming.style.transform = "translateX(" + (dir * 100) + "%)";
    void incoming.offsetWidth;
    incoming.style.transition = "";
    incoming.style.transform = "translateX(0)";
    outgoing.style.transform = "translateX(" + (-dir * 100) + "%)";
    setTimeout(function () {
      outgoing.style.visibility = "hidden";
      outgoing.style.transition = "none";
      outgoing.style.transform = "translateX(0)";
      void outgoing.offsetWidth;
      outgoing.style.transition = "";
      slideCurrent = i;
      slideBusy = false;
    }, 360);
  }
  slideBtns.forEach(function (b) {
    b.addEventListener("click", function () { goSlide(parseInt(b.dataset.index, 10)); });
  });

  /* ---------- Scroll-hide bars demo (see §6): scroll down hides, scroll up shows ---------- */
  var hideStage = document.getElementById("hideStage");
  var hideList = document.getElementById("hideList");
  var hideLast = 0;
  hideList.addEventListener("scroll", function () {
    if (hideList.scrollHeight <= hideList.clientHeight) return;
    var st = hideList.scrollTop;
    if (st <= 0) { hideStage.classList.remove("bars-hidden"); }
    else if (st > hideLast) { hideStage.classList.add("bars-hidden"); }
    else if (st < hideLast) { hideStage.classList.remove("bars-hidden"); }
    hideLast = st;
  }, { passive: true });

  /* ---------- Instagram-style pull to refresh demo (see §7) ---------- */
  var ptrViewport = document.getElementById("ptrViewport");
  var ptrList = document.getElementById("ptrList");
  var ptrPill = document.getElementById("ptrPill");
  var ptrArc = document.getElementById("ptrArc");
  var ptrArcSvg = document.getElementById("ptrArcSvg");
  var ptrLabel = document.getElementById("ptrLabel");
  if (ptrViewport && ptrList && ptrPill && ptrArc && ptrArcSvg && ptrLabel) {
    var PTR_THRESHOLD = 64;   // px of list travel = the release threshold
    var PTR_DAMP = 0.5;       // list travels half the finger — the smooth, easy pull
    var PTR_CIRC = 56.5;      // arc circumference (r = 9)
    var ptrStartY = 0, ptrPulling = false, ptrRefreshing = false, ptrProgress = 0;
    function ptrRender(progress, label) {
      ptrList.style.transform = "translateY(" + (progress * PTR_THRESHOLD) + "px)";
      ptrPill.style.opacity = progress > 0 ? Math.min(progress, 1) : 0;
      ptrArc.style.strokeDashoffset = String(PTR_CIRC * (1 - Math.min(progress, 1)));
      ptrLabel.textContent = label;
    }
    function ptrSnapBack() {
      ptrList.style.transition = "transform 220ms ease";
      ptrPill.style.transition = "opacity 220ms ease";
      ptrList.style.transform = "translateY(0)";
      ptrPill.style.opacity = 0;
      setTimeout(function () {
        ptrList.style.transition = "";
        ptrPill.style.transition = "";
        ptrRefreshing = false;
      }, 240);
    }
    ptrViewport.addEventListener("pointerdown", function (e) {
      if (ptrRefreshing) return;
      ptrPulling = true;
      ptrStartY = e.clientY;
      ptrProgress = 0;
      ptrList.style.transition = "none";
      ptrPill.style.transition = "none";
      ptrViewport.setPointerCapture(e.pointerId);
    });
    ptrViewport.addEventListener("pointermove", function (e) {
      if (!ptrPulling || ptrRefreshing) return;
      var dy = e.clientY - ptrStartY;
      if (dy <= 0) { ptrProgress = 0; ptrRender(0, "Pull to refresh"); return; }
      ptrProgress = (dy * PTR_DAMP) / PTR_THRESHOLD;
      ptrRender(ptrProgress, ptrProgress >= 1 ? "Release to refresh" : "Pull to refresh");
    });
    function ptrEnd() {
      if (!ptrPulling) return;
      ptrPulling = false;
      if (ptrRefreshing) return;
      if (ptrProgress >= 1) {
        ptrRefreshing = true;
        ptrList.style.transition = "transform 180ms ease";
        ptrList.style.transform = "translateY(" + PTR_THRESHOLD + "px)";
        ptrPill.style.opacity = 1;
        ptrArc.style.strokeDashoffset = "14";
        ptrArcSvg.classList.add("ptr-spinning");
        ptrLabel.textContent = "Refreshing\u2026";
        setTimeout(function () {
          ptrArcSvg.classList.remove("ptr-spinning");
          ptrArc.style.strokeDashoffset = String(PTR_CIRC);
          ptrSnapBack();
        }, 1200);
      } else {
        ptrSnapBack();
      }
    }
    ptrViewport.addEventListener("pointerup", ptrEnd);
    ptrViewport.addEventListener("pointercancel", ptrEnd);
  }

  /* ---------- Account switcher sheet demo (see §8) ---------- */
  var asStage = document.getElementById("asStage");
  var asTopAvatar = document.getElementById("asTopAvatar");
  var asSheet = document.getElementById("asSheet");
  var asScrim = document.getElementById("asScrim");
  var asArrow = document.getElementById("asArrow");
  var asHeadAvatar = document.getElementById("asHeadAvatar");
  var asHeadName = document.getElementById("asHeadName");
  var asHeadEmail = document.getElementById("asHeadEmail");
  if (asStage && asTopAvatar && asSheet && asScrim && asArrow) {
    function asClose() {
      asStage.classList.remove("sheet-open");
      asSheet.classList.remove("collapsed");
      asArrow.textContent = "\u2227";
    }
    asTopAvatar.addEventListener("click", function () {
      asStage.classList.add("sheet-open");
      asSheet.classList.remove("collapsed");
      asArrow.textContent = "\u2227";
    });
    asScrim.addEventListener("click", asClose);
    asArrow.addEventListener("click", function () {
      var collapsed = asSheet.classList.toggle("collapsed");
      asArrow.textContent = collapsed ? "\u2228" : "\u2227";
    });
    Array.prototype.forEach.call(
      document.querySelectorAll("#asBody .as-row"),
      function (row) {
        row.addEventListener("click", function () {
          // Switch: the header + the top-bar DP follow the picked account.
          asHeadAvatar.textContent = row.dataset.letter || "A";
          asHeadName.textContent = row.dataset.name || "";
          asHeadEmail.textContent = row.dataset.email || "";
          asTopAvatar.textContent = row.dataset.letter || "A";
          asClose();
        });
      }
    );
    ["asAdd", "asSettings"].forEach(function (id) {
      var el = document.getElementById(id);
      if (el) el.addEventListener("click", asClose);
    });
  }

  /* ---------- Settings panel demo (see §9) ---------- */
  var spStage = document.getElementById("spStage");
  var spTopAvatar = document.getElementById("spTopAvatar");
  var spPanel = document.getElementById("spPanel");
  var spBack = document.getElementById("spBack");
  var spAccHead = document.getElementById("spAccHead");
  var spArrow = document.getElementById("spArrow");
  var spHeadAvatar = document.getElementById("spHeadAvatar");
  var spHeadName = document.getElementById("spHeadName");
  var spHeadEmail = document.getElementById("spHeadEmail");
  if (spStage && spTopAvatar && spPanel && spBack && spAccHead && spArrow) {
    function spClose() {
      spStage.classList.remove("panel-open");
      spPanel.classList.add("acc-collapsed");
      spArrow.textContent = "\u2228";
    }
    spTopAvatar.addEventListener("click", function () {
      spStage.classList.add("panel-open");
    });
    spBack.addEventListener("click", spClose);
    spAccHead.addEventListener("click", function () {
      var collapsed = spPanel.classList.toggle("acc-collapsed");
      spArrow.textContent = collapsed ? "\u2228" : "\u2227";
    });
    Array.prototype.forEach.call(
      document.querySelectorAll("#spAccBody .as-row"),
      function (row) {
        row.addEventListener("click", function () {
          // Switch: the header + the top-bar DP follow the picked account.
          spHeadAvatar.textContent = row.dataset.letter || "A";
          spHeadName.textContent = row.dataset.name || "";
          spHeadEmail.textContent = row.dataset.email || "";
          spTopAvatar.textContent = row.dataset.letter || "A";
          spClose();
        });
      }
    );
    var spAdd = document.getElementById("spAdd");
    if (spAdd) spAdd.addEventListener("click", function () { /* Connect flow: the panel stays */ });
  }
})();
