/** Версия Taxi от Qwen без исходных pattern и filter, запрещённых контрактом. */
export const taxiQwenSvg = `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
    <defs>
      <linearGradient id="qwen-body-gradient" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#FFD740" />
        <stop offset="0.6" stop-color="#FFC107" />
        <stop offset="1" stop-color="#FFA000" />
      </linearGradient>
      <linearGradient id="qwen-window-gradient" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#37474F" />
        <stop offset="1" stop-color="#1A252C" />
      </linearGradient>
      <linearGradient id="qwen-road-gradient" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0" stop-color="#BDBDBD" />
        <stop offset="1" stop-color="#9E9E9E" />
      </linearGradient>
    </defs>
    <g id="qwen-background" data-editor-layer="true" data-name="Фон">
      <rect id="qwen-background-rect" data-role="background" x="0" y="0" width="500" height="500" fill="#E8F5E9" />
    </g>
    <g id="road" data-editor-layer="true" data-name="Дорога">
      <rect id="road-surface" x="0" y="390" width="500" height="110" fill="url(#qwen-road-gradient)" />
      <rect id="road-edge" x="0" y="390" width="500" height="6" fill="#757575" />
      <line id="road-marking" x1="0" y1="440" x2="500" y2="440" stroke="#FFC107" stroke-width="3" />
    </g>
    <ellipse id="qwen-car-shadow" data-name="Тень машины" cx="250" cy="395" rx="190" ry="12" fill="#000000" fill-opacity="0.13" />
    <g id="qwen-taxi" data-editor-layer="true" data-name="Такси">
      <path id="qwen-body-base" d="M60 340 L60 300 Q60 285 75 280 L130 270 L160 260 L200 255 L380 255 L420 260 Q445 265 450 280 L455 300 L455 340 Q455 355 440 355 L75 355 Q60 355 60 340 Z" fill="url(#qwen-body-gradient)" />
      <path id="qwen-body-roof" d="M160 260 L190 200 Q195 190 210 185 L310 185 Q325 185 330 195 L360 260 Z" fill="url(#qwen-body-gradient)" />
      <path id="qwen-checker-sign" d="M175 200 L195 160 Q200 150 215 148 L305 148 Q320 148 325 158 L345 200 Z" fill="#212121" />
      <path id="qwen-front-window" d="M195 255 L215 205 Q218 198 228 195 L260 195 L260 255 Z" fill="url(#qwen-window-gradient)" />
      <path id="qwen-rear-window" d="M260 255 L260 195 L295 195 Q305 195 308 202 L325 255 Z" fill="url(#qwen-window-gradient)" />
      <line id="qwen-body-line" x1="60" y1="310" x2="455" y2="310" stroke="#E6A800" stroke-width="2" />
      <ellipse id="qwen-front-light" cx="452" cy="315" rx="8" ry="12" fill="#FFF9C4" stroke="#F9A825" stroke-width="1.5" />
      <rect id="qwen-rear-light" x="38" y="305" width="12" height="25" rx="3" fill="#E53935" />
      <g id="qwen-taxi-sign" data-editor-layer="true" data-name="Надпись TAXI">
        <rect id="qwen-taxi-sign-plate" x="175" y="290" width="60" height="22" rx="4" fill="#212121" />
        <text id="qwen-taxi-sign-text" x="205" y="306" text-anchor="middle" font-family="Arial" font-size="13" font-weight="700" fill="#FFD740">TAXI</text>
      </g>
      <g id="qwen-front-wheel" data-editor-layer="true" data-name="Переднее колесо">
        <circle id="qwen-front-tire" cx="370" cy="355" r="32" fill="#212121" />
        <circle id="qwen-front-rim" cx="370" cy="355" r="20" fill="#757575" />
        <circle id="qwen-front-hub" cx="370" cy="355" r="8" fill="#9E9E9E" />
      </g>
      <g id="qwen-rear-wheel" data-editor-layer="true" data-name="Заднее колесо">
        <circle id="qwen-rear-tire" cx="130" cy="355" r="32" fill="#212121" />
        <circle id="qwen-rear-rim" cx="130" cy="355" r="20" fill="#757575" />
        <circle id="qwen-rear-hub" cx="130" cy="355" r="8" fill="#9E9E9E" />
      </g>
    </g>
    <g id="clouds" data-editor-layer="true" data-name="Облака" opacity="0.6">
      <ellipse id="cloud-left" cx="80" cy="60" rx="40" ry="18" fill="#FFFFFF" />
      <ellipse id="cloud-right" cx="400" cy="80" rx="35" ry="15" fill="#FFFFFF" />
    </g>
  </svg>
`
