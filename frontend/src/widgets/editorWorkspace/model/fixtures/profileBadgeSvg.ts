/** Независимая от Taxi фикстура: профильная карточка с текстом и градиентом. */
export const profileBadgeSvg = `
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
    <defs>
      <linearGradient id="badge-gradient" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#6C3BFF" />
        <stop offset="1" stop-color="#A855F7" />
      </linearGradient>
    </defs>
    <g id="badge-background" data-editor-layer="true" data-name="Фон">
      <rect id="badge-background-rect" data-role="background" x="0" y="0" width="500" height="500" fill="#F5F3FF" />
    </g>
    <g id="badge-card" data-editor-layer="true" data-name="Карточка">
      <rect id="badge-card-shape" x="70" y="70" width="360" height="360" rx="32" fill="#FFFFFF" />
    </g>
    <g id="badge-avatar" data-editor-layer="true" data-name="Аватар">
      <circle id="badge-avatar-circle" cx="250" cy="190" r="82" fill="url(#badge-gradient)" />
      <path id="badge-avatar-mark" d="M215 190 L240 215 L292 163" fill="none" stroke="#FFFFFF" stroke-width="18" stroke-linecap="round" stroke-linejoin="round" />
    </g>
    <g id="badge-copy" data-editor-layer="true" data-name="Текст">
      <text id="badge-title" x="250" y="330" font-family="Inter" font-size="32" font-weight="700" fill="#1D1D1F" text-anchor="middle">AVATARA</text>
      <text id="badge-subtitle" x="250" y="365" font-family="Inter" font-size="16" fill="#6B7280" text-anchor="middle">design your identity</text>
    </g>
  </svg>
`
