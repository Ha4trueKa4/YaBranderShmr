/** Подробный пример Taxi от бэкенда: градиенты, текст и вложенные слои. */
export const taxiDetailedSvg = `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500">
    <defs>
      <linearGradient id="body-gradient" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#FFE44D" />
        <stop offset="0.4" stop-color="#FFCC00" />
        <stop offset="1" stop-color="#E6B800" />
      </linearGradient>
      <linearGradient id="window-gradient" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#3A5A7A" />
        <stop offset="1" stop-color="#0F1A26" />
      </linearGradient>
      <radialGradient id="wheel-gradient" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="#E0E0E0" />
        <stop offset="0.7" stop-color="#A0A0A0" />
        <stop offset="1" stop-color="#606060" />
      </radialGradient>
      <radialGradient id="shadow-gradient" cx="0.5" cy="0.5" r="0.5">
        <stop offset="0" stop-color="#000000" stop-opacity="0.4" />
        <stop offset="1" stop-color="#000000" stop-opacity="0" />
      </radialGradient>
    </defs>
    <g id="background" data-editor-layer="true" data-name="Фон">
      <rect id="background-rect" data-role="background" x="0" y="0" width="800" height="500" fill="#F4F7FB" />
    </g>
    <g id="shadow" data-editor-layer="true" data-name="Тень">
      <ellipse id="car-shadow" cx="400" cy="380" rx="300" ry="30" fill="url(#shadow-gradient)" />
    </g>
    <g id="car-body" data-editor-layer="true" data-name="Кузов">
      <path id="body-base" d="M90 300 L120 300 L140 350 L660 350 L680 300 L710 300 L710 330 Q710 360 680 360 L120 360 Q90 360 90 330 Z" fill="#D9A300" />
      <path id="body-main" d="M120 300 L140 220 Q150 200 180 200 L620 200 Q650 200 660 220 L680 300 Z" fill="url(#body-gradient)" />
      <path id="body-roof" d="M240 200 L260 130 Q270 110 300 110 L500 110 Q530 110 540 130 L560 200 Z" fill="url(#body-gradient)" />
      <line id="body-divider-left" x1="380" y1="200" x2="380" y2="350" stroke="#D9A300" stroke-width="2" />
      <line id="body-divider-right" x1="520" y1="200" x2="520" y2="350" stroke="#D9A300" stroke-width="2" />
      <rect id="handle-left" x="390" y="270" width="30" height="8" rx="4" fill="#333333" />
      <rect id="handle-right" x="530" y="270" width="30" height="8" rx="4" fill="#333333" />
    </g>
    <g id="windows" data-editor-layer="true" data-name="Стёкла">
      <path id="front-window" d="M390 200 L400 130 L500 130 L530 200 Z" fill="url(#window-gradient)" />
      <path id="rear-window" d="M260 200 L280 130 L370 130 L370 200 Z" fill="url(#window-gradient)" />
    </g>
    <g id="details" data-editor-layer="true" data-name="Детали">
      <path id="front-light" d="M680 240 L715 240 L710 280 L680 280 Z" fill="#FFF7CC" />
      <path id="rear-light" d="M90 240 L120 240 L120 280 L95 280 Z" fill="#FF3333" />
      <rect id="license-plate" x="650" y="315" width="40" height="20" rx="3" fill="#FFFFFF" stroke="#333333" stroke-width="2" />
      <text id="license-text" x="670" y="330" font-family="Arial" font-size="10" font-weight="700" fill="#111111" text-anchor="middle">TAXI</text>
    </g>
    <g id="wheels" data-editor-layer="true" data-name="Колёса">
      <g id="rear-wheel" data-editor-layer="true" data-name="Заднее колесо">
        <circle id="rear-tire" cx="220" cy="350" r="50" fill="#1A1A1A" />
        <circle id="rear-rim" cx="220" cy="350" r="35" fill="url(#wheel-gradient)" />
        <circle id="rear-hub" cx="220" cy="350" r="15" fill="#333333" />
      </g>
      <g id="front-wheel" data-editor-layer="true" data-name="Переднее колесо">
        <circle id="front-tire" cx="580" cy="350" r="50" fill="#1A1A1A" />
        <circle id="front-rim" cx="580" cy="350" r="35" fill="url(#wheel-gradient)" />
        <circle id="front-hub" cx="580" cy="350" r="15" fill="#333333" />
      </g>
    </g>
  </svg>
`
