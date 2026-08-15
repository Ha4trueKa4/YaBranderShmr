/**
 * Временный пример ответа API: упрощённая версия SVG такси от бэкенда,
 * приведённая к контракту редактора.
 */
export const mockTaxiSvg = `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
    <rect id="canvas-background" data-name="Фон" data-role="background" x="0" y="0" width="500" height="500" fill="#E8F5E9" />
    <g id="taxi" data-name="Такси">
      <rect id="taxi-body" data-name="Кузов" x="100" y="250" width="300" height="100" rx="10" ry="10" fill="#FFD700" />
      <rect id="taxi-window" data-name="Стекло" x="120" y="250" width="260" height="50" rx="5" ry="5" fill="#87CEFA" />
      <circle id="rear-wheel" data-name="Заднее колесо" cx="175" cy="350" r="30" fill="#333333" />
      <circle id="front-wheel" data-name="Переднее колесо" cx="325" cy="350" r="30" fill="#333333" />
      <g id="taxi-sign" data-name="Шашка" transform="translate(250 150)">
        <rect id="taxi-sign-base" x="-50" y="-50" width="100" height="100" fill="#FFD700" />
        <rect id="taxi-sign-middle" x="-40" y="-40" width="80" height="80" fill="#FFFFFF" />
        <rect id="taxi-sign-top" x="-30" y="-30" width="60" height="60" fill="#FFD700" />
      </g>
    </g>
  </svg>
`
