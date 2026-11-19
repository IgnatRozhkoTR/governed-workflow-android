// xterm measures the cell size once at open, so the emulator starts only after JetBrains Mono has loaded.
function startTerminal() {
  'use strict';

  var bridge = window.GwBridge;
  var config = JSON.parse(bridge.config());
  var FONT_FAMILY = '"JetBrains Mono", monospace';
  var LONG_PRESS_MS = 450;
  var TAP_SLOP_PX = 10;
  var FIT_DEBOUNCE_MS = 60;
  // tmux scrolls 5 lines per wheel event in copy mode, so mouse-mode scrolling needs a longer finger travel per event.
  var MOUSE_MODE_ROWS_PER_WHEEL = 2.5;
  var FLING_MIN_VELOCITY = 0.35;
  var FLING_STOP_VELOCITY = 0.04;
  var FLING_DECAY_PER_FRAME = 0.94;
  var VELOCITY_WINDOW_MS = 90;

  var container = document.getElementById('terminal');
  var term = new Terminal({
    fontSize: config.fontSize,
    fontFamily: FONT_FAMILY,
    theme: config.theme,
    cursorBlink: false,
    cursorInactiveStyle: 'block',
    scrollback: 5000,
    allowProposedApi: true,
    smoothScrollDuration: 0,
    rightClickSelectsWord: false
  });
  var fitAddon = new FitAddon.FitAddon();
  term.loadAddon(fitAddon);
  term.loadAddon(new Unicode11Addon.Unicode11Addon());
  term.unicode.activeVersion = '11';
  term.open(container);
  if (term.textarea) {
    term.textarea.readOnly = true;
    term.textarea.tabIndex = -1;
  }

  var screenElement = term.element.querySelector('.xterm-screen');
  var isApplicationCursor = false;
  var fitTimer = null;
  var gesture = null;
  var flingFrame = null;

  var pendingReplays = 0;
  term.onData(function (data) {
    if (pendingReplays === 0) bridge.onData(data);
  });
  term.onResize(function (size) { bridge.onResize(size.cols, size.rows); });
  term.onSelectionChange(function () { bridge.onSelectionChanged(term.hasSelection()); });
  term.onWriteParsed(function () {
    var mode = term.modes.applicationCursorKeysMode;
    if (mode !== isApplicationCursor) {
      isApplicationCursor = mode;
      bridge.onApplicationCursorChanged(mode);
    }
  });
  term.parser.registerOscHandler(52, function (data) {
    var payload = data.substring(data.indexOf(';') + 1);
    if (payload === '?' || payload === '') return true;
    try {
      var binary = atob(payload);
      var bytes = new Uint8Array(binary.length);
      for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
      bridge.onClipboardCopy(new TextDecoder().decode(bytes));
    } catch (error) {
      bridge.onClipboardCopy('');
    }
    return true;
  });

  function fitNow() {
    var dims = fitAddon.proposeDimensions();
    if (!dims || !isFinite(dims.cols) || !isFinite(dims.rows) || dims.cols < 2 || dims.rows < 1) return;
    if (dims.cols !== term.cols || dims.rows !== term.rows) term.resize(dims.cols, dims.rows);
  }

  function scheduleFit() {
    clearTimeout(fitTimer);
    fitTimer = setTimeout(fitNow, FIT_DEBOUNCE_MS);
  }

  new ResizeObserver(scheduleFit).observe(container);

  function cellHeight() {
    return screenElement.getBoundingClientRect().height / term.rows;
  }

  function cellAt(x, y) {
    var rect = screenElement.getBoundingClientRect();
    var col = Math.floor((x - rect.left) / (rect.width / term.cols));
    var row = Math.floor((y - rect.top) / (rect.height / term.rows));
    return {
      col: Math.max(0, Math.min(term.cols - 1, col)),
      row: Math.max(0, Math.min(term.rows - 1, row)) + term.buffer.active.viewportY
    };
  }

  // Synthetic wheel events let xterm.js pick the right encoding: SGR mouse reports when tmux or an app
  // tracks the mouse, arrow keys on an alternate screen without scrollback, local scrollback otherwise.
  function dispatchWheel(direction, x, y) {
    screenElement.dispatchEvent(new WheelEvent('wheel', {
      deltaY: direction, deltaMode: WheelEvent.DOM_DELTA_LINE,
      clientX: x, clientY: y, bubbles: true, cancelable: true
    }));
  }

  function wheelStepPx() {
    var isMouseMode = term.modes.mouseTrackingMode !== 'none';
    return cellHeight() * (isMouseMode ? MOUSE_MODE_ROWS_PER_WHEEL : 1);
  }

  function scrollByPixels(scroller, deltaY, x, y) {
    scroller.pendingScroll += deltaY;
    var step = wheelStepPx();
    while (Math.abs(scroller.pendingScroll) >= step) {
      var direction = scroller.pendingScroll > 0 ? 1 : -1;
      dispatchWheel(direction, x, y);
      scroller.pendingScroll -= direction * step;
    }
  }

  function recordSample(y) {
    var now = performance.now();
    gesture.samples.push({ y: y, time: now });
    while (gesture.samples.length > 2 && now - gesture.samples[0].time > VELOCITY_WINDOW_MS) gesture.samples.shift();
  }

  function releaseVelocity() {
    var samples = gesture.samples;
    if (samples.length < 2) return 0;
    var first = samples[0];
    var last = samples[samples.length - 1];
    var elapsed = last.time - first.time;
    return elapsed > 0 ? (first.y - last.y) / elapsed : 0;
  }

  function stopFling() {
    if (flingFrame !== null) cancelAnimationFrame(flingFrame);
    flingFrame = null;
  }

  function startFling(velocity, x, y) {
    if (Math.abs(velocity) < FLING_MIN_VELOCITY) return;
    var scroller = { pendingScroll: 0 };
    var lastTime = performance.now();
    function frame(now) {
      var elapsed = now - lastTime;
      lastTime = now;
      scrollByPixels(scroller, velocity * elapsed, x, y);
      velocity *= Math.pow(FLING_DECAY_PER_FRAME, elapsed / 16);
      flingFrame = Math.abs(velocity) > FLING_STOP_VELOCITY ? requestAnimationFrame(frame) : null;
    }
    flingFrame = requestAnimationFrame(frame);
  }

  function isWordCharacter(character) {
    return character !== undefined && character !== ' ' && character !== '';
  }

  function selectWordAt(cell) {
    var line = term.buffer.active.getLine(cell.row);
    var text = line ? line.translateToString(false) : '';
    var start = cell.col;
    var end = cell.col;
    if (isWordCharacter(text[cell.col])) {
      while (start > 0 && isWordCharacter(text[start - 1])) start--;
      while (end < text.length - 1 && isWordCharacter(text[end + 1])) end++;
    }
    term.select(start, cell.row, end - start + 1);
  }

  function selectRange(anchor, focus) {
    var isForward = anchor.row < focus.row || (anchor.row === focus.row && anchor.col <= focus.col);
    var start = isForward ? anchor : focus;
    var end = isForward ? focus : anchor;
    term.select(start.col, start.row, (end.row - start.row) * term.cols + (end.col - start.col) + 1);
  }

  function beginSelection() {
    if (!gesture || gesture.kind !== 'pending') return;
    gesture.kind = 'select';
    gesture.anchor = cellAt(gesture.startX, gesture.startY);
    selectWordAt(gesture.anchor);
    bridge.onLongPress();
  }

  function touchDistance(touches) {
    return Math.hypot(touches[0].clientX - touches[1].clientX, touches[0].clientY - touches[1].clientY);
  }

  function beginPinch(touches) {
    if (gesture && gesture.timer) clearTimeout(gesture.timer);
    gesture = { kind: 'pinch', startDistance: Math.max(1, touchDistance(touches)), startSize: term.options.fontSize };
  }

  function updatePinch(touches) {
    var scaled = Math.round(gesture.startSize * touchDistance(touches) / gesture.startDistance);
    var size = Math.max(config.minFontSize, Math.min(config.maxFontSize, scaled));
    if (size !== term.options.fontSize) {
      term.options.fontSize = size;
      scheduleFit();
    }
  }

  function consume(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  container.addEventListener('touchstart', function (event) {
    consume(event);
    stopFling();
    if (event.touches.length === 2) {
      beginPinch(event.touches);
      return;
    }
    if (event.touches.length !== 1) return;
    var touch = event.touches[0];
    gesture = {
      kind: 'pending', startX: touch.clientX, startY: touch.clientY, lastY: touch.clientY,
      pendingScroll: 0, samples: [], timer: setTimeout(beginSelection, LONG_PRESS_MS)
    };
    recordSample(touch.clientY);
  }, { passive: false, capture: true });

  container.addEventListener('touchmove', function (event) {
    consume(event);
    if (!gesture) return;
    if (gesture.kind === 'pinch') {
      if (event.touches.length >= 2) updatePinch(event.touches);
      return;
    }
    var touch = event.touches[0];
    if (gesture.kind === 'pending' &&
        Math.hypot(touch.clientX - gesture.startX, touch.clientY - gesture.startY) > TAP_SLOP_PX) {
      clearTimeout(gesture.timer);
      gesture.kind = 'scroll';
    }
    if (gesture.kind === 'scroll') {
      scrollByPixels(gesture, gesture.lastY - touch.clientY, touch.clientX, touch.clientY);
      gesture.lastY = touch.clientY;
      recordSample(touch.clientY);
    } else if (gesture.kind === 'select') {
      selectRange(gesture.anchor, cellAt(touch.clientX, touch.clientY));
    }
  }, { passive: false, capture: true });

  function endGesture(event, isCancelled) {
    consume(event);
    if (!gesture) return;
    if (gesture.kind === 'pinch') {
      if (event.touches.length === 0) {
        bridge.onFontSizeChanged(term.options.fontSize);
        gesture = null;
      }
      return;
    }
    clearTimeout(gesture.timer);
    if (!isCancelled && gesture.kind === 'pending') {
      if (term.hasSelection()) term.clearSelection();
      else bridge.onTap();
    } else if (!isCancelled && gesture.kind === 'scroll') {
      startFling(releaseVelocity(), gesture.startX, gesture.lastY);
    }
    gesture = null;
  }

  container.addEventListener('touchend', function (event) { endGesture(event, false); }, { passive: false, capture: true });
  container.addEventListener('touchcancel', function (event) { endGesture(event, true); }, { passive: false, capture: true });

  window.gw = {
    write: function (data) { term.write(data); },
    replay: function (data) {
      pendingReplays++;
      term.write(data, function () { pendingReplays--; });
    },
    paste: function (text) { term.paste(text); },
    setFontSize: function (size) {
      term.options.fontSize = size;
      fitNow();
    },
    takeSelection: function () {
      var text = term.getSelection();
      term.clearSelection();
      return text;
    },
    clearSelection: function () { term.clearSelection(); },
    setTheme: function (theme) { term.options.theme = theme; }
  };

  fitNow();
  bridge.onResize(term.cols, term.rows);
  bridge.onReady();
}

document.fonts.load('13px "JetBrains Mono"').then(startTerminal, startTerminal);
