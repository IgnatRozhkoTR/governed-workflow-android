(function () {
  'use strict';

  var MIN_LANE_WIDTH = 150;
  var LANE_TITLE_PADDING = 24;
  var VIEW_MARGIN = 12;
  var TIP_VISIBLE_MS = 3500;
  var LANE_TITLE = /^(\s*)subgraph\s+([\w-]+)\s*\[(.+)\]\s*$/gm;
  var LANE_TITLE_PLACEHOLDER = '…';

  var bridge = window.GwDiagram;
  var palette = JSON.parse(bridge.palette());
  var isFullScreen = new URLSearchParams(location.search).get('mode') === 'fullscreen';
  var naturalWidth = { useMaxWidth: false };
  var tipTimer = null;

  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    suppressErrorRendering: true,
    theme: 'base',
    themeVariables: {
      background: 'transparent',
      fontFamily: '"JetBrains Mono", monospace',
      fontSize: '12px',
      primaryColor: palette.fill, primaryTextColor: palette.text, primaryBorderColor: palette.border,
      secondaryColor: palette.fill, secondaryTextColor: palette.text, secondaryBorderColor: palette.border,
      tertiaryColor: palette.fill, tertiaryTextColor: palette.text, tertiaryBorderColor: palette.border,
      lineColor: palette.line, textColor: palette.text, mainBkg: palette.fill, nodeBorder: palette.border,
      clusterBkg: 'transparent', clusterBorder: palette.border, titleColor: palette.dim,
      edgeLabelBackground: palette.fill,
      actorBkg: palette.fill, actorBorder: palette.border, actorTextColor: palette.text, actorLineColor: palette.line,
      signalColor: palette.line, signalTextColor: palette.text,
      labelBoxBkgColor: palette.fill, labelBoxBorderColor: palette.border, labelTextColor: palette.text,
      loopTextColor: palette.text, noteBkgColor: palette.accentSoft, noteBorderColor: palette.accent,
      noteTextColor: palette.accentText, classText: palette.text
    },
    themeCSS: '.cluster rect { stroke-dasharray: 4 3; rx: 12; ry: 12; } .cluster-label { fill: ' + palette.dim + '; color: ' + palette.dim + '; }',
    flowchart: {
      useMaxWidth: false, htmlLabels: true, nodeSpacing: 25, rankSpacing: 45, padding: 10,
      subGraphTitleMargin: { top: 8, bottom: 24 }
    },
    sequence: naturalWidth, class: naturalWidth, state: naturalWidth, er: naturalWidth,
    gantt: naturalWidth, journey: naturalWidth, pie: naturalWidth, mindmap: naturalWidth,
    timeline: naturalWidth, gitGraph: naturalWidth
  });

  var tip = document.getElementById('tip');
  tip.style.background = palette.accentSoft;
  tip.style.color = palette.accentText;

  // Agents write "\n" inside labels to mean a line break, as the web panel renders it.
  var source = bridge.source().replace(/\\n/g, '<br/>');

  // Mermaid widens a lane to fit its title, so long titles push lanes over their neighbours. Lanes are laid out with a
  // placeholder title and get the real one back, clipped to the lane, once the layout is done.
  var laneTitles = {};
  source = source.replace(LANE_TITLE, function (line, indent, id, title) {
    laneTitles[id] = title.replace(/^"(.*)"$/, '$1').replace(/<br\/>/g, ' ').trim();
    return indent + 'subgraph ' + id + '["' + LANE_TITLE_PLACEHOLDER + '"]';
  });

  var fontReady = document.fonts && document.fonts.load
    ? document.fonts.load('12px "JetBrains Mono"').catch(function () {})
    : Promise.resolve();

  fontReady.then(function () {
    return mermaid.render('gwDiagram', source);
  }).then(function (result) {
    var host = document.getElementById('diagram');
    host.innerHTML = result.svg;
    var svg = host.querySelector('svg');
    widenLanes(svg);
    pinToNaturalSize(svg);
    if (isFullScreen) fitToScreen(svg);
    var box = host.getBoundingClientRect();
    bridge.onRendered(Math.ceil(box.width), Math.ceil(box.height));
  }).catch(function (error) {
    bridge.onFailed(String((error && error.message) || error));
  });

  // Every lane gets room for a readable title row: lanes narrower than the minimum grow around their centre, and a
  // long title is cut with an ellipsis; the full title shows on tap or long press.
  function widenLanes(svg) {
    svg.querySelectorAll('g.cluster').forEach(function (cluster) {
      var rect = cluster.querySelector('rect');
      var label = cluster.querySelector('.cluster-label');
      if (!rect) return;
      var width = rect.width.baseVal.value;
      var grown = Math.max(width, MIN_LANE_WIDTH);
      rect.setAttribute('x', rect.x.baseVal.value - (grown - width) / 2);
      rect.setAttribute('width', grown);
      if (label) limitTitle(label, grown, laneTitles[cluster.id] || label.textContent.trim());
    });
    fitViewBoxToContent(svg);
  }

  function limitTitle(label, laneWidth, text) {
    var box = label.querySelector('foreignObject');
    var inner = label.querySelector('span, .nodeLabel');
    if (!box || !inner) return;
    inner.textContent = text;
    var room = laneWidth - LANE_TITLE_PADDING;
    var shift = (Number(box.getAttribute('width')) - room) / 2;
    box.setAttribute('width', room);
    box.setAttribute('x', Number(box.getAttribute('x') || 0) + shift);
    box.style.overflow = 'hidden';
    clipToOneLine(box.firstElementChild || inner, room);
    label.querySelectorAll('span, p').forEach(function (part) { part.style.display = 'inline'; });
    label.setAttribute('data-title', text);
  }

  function clipToOneLine(holder, width) {
    holder.style.display = 'block';
    holder.style.width = width + 'px';
    holder.style.maxWidth = width + 'px';
    holder.style.overflow = 'hidden';
    holder.style.textOverflow = 'ellipsis';
    holder.style.whiteSpace = 'nowrap';
  }

  function fitViewBoxToContent(svg) {
    var content = svg.getBBox();
    var x = content.x - VIEW_MARGIN;
    var y = content.y - VIEW_MARGIN;
    var width = content.width + VIEW_MARGIN * 2;
    var height = content.height + VIEW_MARGIN * 2;
    svg.setAttribute('viewBox', [x, y, width, height].join(' '));
  }

  // Mermaid sizes some diagrams as width="100%", which would stretch them with the view that is about to be resized
  // to fit them; fixing the size to the viewBox keeps one diagram unit equal to one CSS pixel.
  function pinToNaturalSize(svg) {
    var viewBox = svg.viewBox.baseVal;
    if (!viewBox || !viewBox.width) return;
    svg.setAttribute('width', Math.ceil(viewBox.width));
    svg.setAttribute('height', Math.ceil(viewBox.height));
    svg.style.maxWidth = 'none';
  }

  // Full screen starts with the whole diagram in view: a wide one is scaled down to the screen width, and pinch zoom
  // (vector, so it stays sharp) brings the detail back.
  function fitToScreen(svg) {
    document.querySelector('meta[name=viewport]').setAttribute('content',
      'width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=8, user-scalable=yes');
    var available = window.innerWidth - 8;
    var width = Number(svg.getAttribute('width'));
    if (!width || width <= available) return;
    svg.setAttribute('height', Math.ceil(Number(svg.getAttribute('height')) * available / width));
    svg.setAttribute('width', available);
  }

  function showTitleAt(x, y) {
    var element = document.elementFromPoint(x, y);
    var label = element && element.closest('.cluster-label');
    if (!label) return;
    var rect = label.getBoundingClientRect();
    tip.textContent = label.getAttribute('data-title');
    tip.style.display = 'block';
    tip.style.left = Math.max(4, Math.min(rect.left, window.innerWidth - tip.offsetWidth - 4)) + 'px';
    tip.style.top = Math.min(rect.bottom + 4, window.innerHeight - tip.offsetHeight - 4) + 'px';
    clearTimeout(tipTimer);
    tipTimer = setTimeout(function () { tip.style.display = 'none'; }, TIP_VISIBLE_MS);
  }

  window.gwShowTitleAt = showTitleAt;
  document.addEventListener('click', function (event) { showTitleAt(event.clientX, event.clientY); });
})();
