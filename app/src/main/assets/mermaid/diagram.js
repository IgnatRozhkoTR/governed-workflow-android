(function () {
  'use strict';

  var bridge = window.GwDiagram;
  var isFullScreen = new URLSearchParams(location.search).get('mode') === 'fullscreen';
  var naturalWidth = { useMaxWidth: false };

  // Same dark palette as the web panel, so a diagram looks identical in both clients.
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'strict',
    suppressErrorRendering: true,
    theme: 'dark',
    themeVariables: {
      background: 'transparent',
      fontFamily: 'sans-serif',
      fontSize: '13px',
      primaryColor: '#2d3250', primaryTextColor: '#f0ede8', primaryBorderColor: '#4a5078',
      lineColor: '#8888a0', secondaryColor: '#252840', tertiaryColor: '#1e2035',
      actorBkg: '#2d3250', actorBorder: '#4a5078', actorTextColor: '#f0ede8',
      activationBkgColor: '#252840', activationBorderColor: '#4a5078',
      signalColor: '#8888a0', signalTextColor: '#f0ede8',
      labelBoxBkgColor: '#1e2035', labelBoxBorderColor: '#4a5078', labelTextColor: '#f0ede8',
      loopTextColor: '#f0ede8',
      noteBkgColor: '#3a3a2d', noteBorderColor: '#c7b76b', noteTextColor: '#f0ede8',
      mainBkg: '#2d3250', nodeBorder: '#4a5078', clusterBkg: '#1e2035', clusterBorder: '#4a5078',
      edgeLabelBackground: '#1e2035', classText: '#f0ede8'
    },
    flowchart: { useMaxWidth: false, nodeSpacing: 25, rankSpacing: 45, padding: 10 },
    sequence: naturalWidth, class: naturalWidth, state: naturalWidth, er: naturalWidth,
    gantt: naturalWidth, journey: naturalWidth, pie: naturalWidth, mindmap: naturalWidth,
    timeline: naturalWidth, gitGraph: naturalWidth
  });

  // Agents write "\n" inside labels to mean a line break, as the web panel renders it.
  var source = bridge.source().replace(/\\n/g, '<br/>');

  mermaid.render('gwDiagram', source).then(function (result) {
    var host = document.getElementById('diagram');
    host.innerHTML = result.svg;
    var svg = host.querySelector('svg');
    pinToNaturalSize(svg);
    if (isFullScreen) fitToScreen(svg);
    var box = host.getBoundingClientRect();
    var width = Math.ceil(box.width);
    var height = Math.ceil(box.height);
    bridge.onRendered(width, height);
  }).catch(function (error) {
    bridge.onFailed(String((error && error.message) || error));
  });

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
})();
