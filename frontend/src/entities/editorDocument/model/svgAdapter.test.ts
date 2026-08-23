import { describe, expect, it } from 'vitest'

import { importSvg, parseSvg } from './svgAdapter'

describe('parseSvg', () => {
  it('rejects invalid SVG markup with multiple root elements', () => {
    expect(() => parseSvg('<svg></svg><svg></svg>')).toThrow(
      'Invalid SVG markup',
    )
  })

  it('keeps the first duplicate attribute so the SVG can be imported', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        <path id="background" data-name="First name" data-name="Ignored name" d="M0 0H100V100H0Z" />
      </svg>
    `)

    expect(result.document.layers[0]).toMatchObject({
      name: 'First name',
      type: 'shape',
    })
    expect(result.warnings).toContainEqual({
      attributeName: 'data-name',
      code: 'ignored-duplicate-attribute',
      elementName: 'path',
    })
  })

  it('adds a fallback viewBox when it is missing', () => {
    const result = importSvg('<svg xmlns="http://www.w3.org/2000/svg"></svg>')

    expect(result.document).toMatchObject({ height: 1000, width: 1000 })
    expect(result.warnings).toContainEqual({
      code: 'missing-view-box',
      height: 1000,
      width: 1000,
    })
  })

  it('rejects a root that is not an SVG element', () => {
    expect(() => parseSvg('<div viewBox="0 0 500 500"></div>')).toThrow(
      'SVG document must have an <svg> root element',
    )
  })

  it('parses the canvas and a rectangle', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <rect id="background" x="0" y="0" width="500" height="500" fill="#FFFFFF" />
      </svg>
    `)

    expect(document).toEqual({
      height: 500,
      layers: [
        {
          geometry: {
            height: 500,
            type: 'rect',
            width: 500,
            x: 0,
            y: 0,
          },
          id: 'background',
          isLocked: false,
          isVisible: true,
          name: 'background',
          opacity: 1,
          shapeType: 'rect',
          style: {
            fill: { type: 'color', value: '#FFFFFF' },
            fillOpacity: 1,
            stroke: { type: 'none' },
            strokeOpacity: 1,
            strokeWidth: 0,
          },
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'shape',
        },
      ],
      width: 500,
    })
  })

  it('preserves a non-zero viewBox origin', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="-250 100 500 300">
        <text id="title" x="0" y="200">A</text>
      </svg>
    `)

    expect(document).toMatchObject({
      height: 300,
      viewBoxX: -250,
      viewBoxY: 100,
      width: 500,
    })
  })

  it('keeps transforms independent when source nodes have no transform', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <rect id="first" width="10" height="10" />
        <rect id="second" width="10" height="10" />
      </svg>
    `)
    const [first, second] = document.layers

    if (first?.type !== 'shape' || second?.type !== 'shape') {
      throw new Error('Expected two editable shapes')
    }

    first.transform.x = 10

    expect(second.transform.x).toBe(0)
  })

  it('locks transforms that cannot be represented without changing their order', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <rect id="scaled" width="10" height="10" transform="scale(2) translate(100 0)" />
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      { id: 'scaled', isLocked: true, type: 'raw' },
    ])
    expect(result.warnings).toContainEqual({
      code: 'locked-raw-node',
      editorId: 'scaled',
      elementName: 'rect',
      reason: 'Unsupported SVG transform order: scale(2) translate(100 0)',
    })
  })

  it('generates type-based names for editable layers without an id', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">
        <rect x="0" y="0" width="400" height="300" fill="#FFFFFF" />
        <circle cx="100" cy="100" r="50" fill="#000000" />
        <circle cx="300" cy="100" r="50" fill="#000000" />
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      { id: 'shape-1', name: 'rect-1', type: 'shape' },
      { id: 'shape-2', name: 'circle-1', type: 'shape' },
      { id: 'shape-3', name: 'circle-2', type: 'shape' },
    ])
    expect(result.warnings).toEqual(
      expect.arrayContaining([
        { code: 'missing-view-box', height: 300, width: 400 },
        {
          code: 'generated-editor-id',
          editorId: 'shape-1',
          elementName: 'rect',
        },
      ]),
    )
  })

  it('parses groups recursively and converts transform into an object', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <g id="logo" data-name="Logo" transform="translate(20 10) rotate(5 250 250) scale(1.2 0.8)">
          <circle id="mark" cx="250" cy="210" r="80" fill="#6C3BFF" />
        </g>
      </svg>
    `)

    expect(document.layers).toEqual([
      {
        children: [
          {
            geometry: { cx: 250, cy: 210, r: 80, type: 'circle' },
            id: 'mark',
            isLocked: false,
            isVisible: true,
            name: 'mark',
            opacity: 1,
            shapeType: 'circle',
            style: {
              fill: { type: 'color', value: '#6C3BFF' },
              fillOpacity: 1,
              stroke: { type: 'none' },
              strokeOpacity: 1,
              strokeWidth: 0,
            },
            transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
            type: 'shape',
          },
        ],
        id: 'logo',
        isLocked: false,
        isVisible: true,
        name: 'Logo',
        opacity: 1,
        transform: {
          rotation: 5,
          rotationCenter: { x: 250, y: 250 },
          scaleX: 1.2,
          scaleY: 0.8,
          x: 20,
          y: 10,
        },
        type: 'group',
      },
    ])
  })

  it('parses text content and its editable typography', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <text id="title" x="250" y="365" fill="#1D1D1F" font-family="Inter" font-size="36" font-weight="700" text-anchor="middle">AVATARA</text>
      </svg>
    `)

    expect(document.layers).toEqual([
      {
        id: 'title',
        isLocked: false,
        isVisible: true,
        name: 'title',
        opacity: 1,
        style: {
          fill: { type: 'color', value: '#1D1D1F' },
          fontFamily: 'Inter',
          fontSize: 36,
          fontWeight: 700,
          textAnchor: 'middle',
        },
        text: 'AVATARA',
        transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
        type: 'text',
        x: 250,
        y: 365,
      },
    ])
  })

  it('normalizes a keyword font weight and parses letter spacing', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <text id="caption" x="20" y="40" font-weight="bold" letter-spacing="2">TAXI</text>
      </svg>
    `)

    expect(document.layers[0]).toMatchObject({
      id: 'caption',
      style: { fontWeight: 700, letterSpacing: 2 },
      text: 'TAXI',
      type: 'text',
    })
  })

  it('parses tspan lines as one editable text layer', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <text id="title" x="20" y="40" font-size="20"><tspan x="20" dy="0">Hello</tspan><tspan x="20" dy="24">world</tspan></text>
      </svg>
    `)

    expect(document.layers[0]).toMatchObject({
      id: 'title',
      isLocked: false,
      style: { lineHeight: 24 },
      text: 'Hello\nworld',
      type: 'text',
    })
  })

  it('locks text with individually positioned tspans', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <text id="title" x="20" y="40"><tspan x="20" dy="0">Hello</tspan><tspan x="100" dy="24">world</tspan></text>
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      { id: 'title', isLocked: true, type: 'raw' },
    ])
    expect(result.warnings).toContainEqual({
      code: 'locked-raw-node',
      editorId: 'title',
      elementName: 'text',
      reason: 'Text with individually positioned lines is not editable',
    })
  })

  it('parses a path with its stroke properties', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <path id="logo-cut" d="M220 210 L242 232 L287 185" fill="none" stroke="#FFFFFF" stroke-width="16" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    `)

    expect(document.layers).toEqual([
      {
        geometry: { d: 'M220 210 L242 232 L287 185', type: 'path' },
        id: 'logo-cut',
        isLocked: false,
        isVisible: true,
        name: 'logo-cut',
        opacity: 1,
        shapeType: 'path',
        style: {
          fill: { type: 'none' },
          fillOpacity: 1,
          stroke: { type: 'color', value: '#FFFFFF' },
          strokeLineCap: 'round',
          strokeLineJoin: 'round',
          strokeOpacity: 1,
          strokeWidth: 16,
        },
        transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
        type: 'shape',
      },
    ])
  })

  it('keeps an explicitly specified path fill instead of using the SVG black default', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <path id="pelican-neck" d="M290 240 Q320 200 330 170" fill="#ffffff" stroke="#ffffff" stroke-width="20" />
      </svg>
    `)

    expect(document.layers[0]).toMatchObject({
      id: 'pelican-neck',
      style: {
        fill: { type: 'color', value: '#ffffff' },
        stroke: { type: 'color', value: '#ffffff' },
      },
      type: 'shape',
    })
  })

  it('keeps a contract editor group and its path editable', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <g data-editor-layer="true" data-name="heart-shape" id="heart-shape">
          <path id="heart-path" d="M250 420 C120 330 40 150 150 40 C200 40 240 70 250 110 C260 70 300 40 350 40 C410 40 460 80 460 150 C460 240 380 330 250 420 Z" fill="#e74c3c" stroke="#c0392b" stroke-width="6" stroke-linejoin="round" />
        </g>
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      {
        children: [
          {
            id: 'heart-path',
            isLocked: false,
            shapeType: 'path',
            type: 'shape',
          },
        ],
        id: 'heart-shape',
        isLocked: false,
        type: 'group',
      },
    ])
    expect(result.warnings).not.toContainEqual(
      expect.objectContaining({ editorId: 'heart-shape' }),
    )
  })

  it('preserves gradients in definitions and parses an ellipse', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500">
        <defs>
          <radialGradient id="wheelGradient" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stop-color="#E0E0E0" />
            <stop offset="100%" stop-color="#606060" />
          </radialGradient>
        </defs>
        <ellipse id="shadow" cx="400" cy="380" rx="300" ry="30" fill="url(#wheelGradient)" />
      </svg>
    `)

    expect(document).toEqual({
      definitions: {
        gradients: [
          {
            attributes: { cx: '50%', cy: '50%', r: '50%' },
            id: 'wheelGradient',
            stops: [
              { color: '#E0E0E0', offset: '0%' },
              { color: '#606060', offset: '100%' },
            ],
            type: 'radialGradient',
          },
        ],
      },
      height: 500,
      layers: [
        {
          geometry: { cx: 400, cy: 380, rx: 300, ry: 30, type: 'ellipse' },
          id: 'shadow',
          isLocked: false,
          isVisible: true,
          name: 'shadow',
          opacity: 1,
          shapeType: 'ellipse',
          style: {
            fill: { gradientId: 'wheelGradient', type: 'gradient' },
            fillOpacity: 1,
            stroke: { type: 'none' },
            strokeOpacity: 1,
            strokeWidth: 0,
          },
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'shape',
        },
      ],
      width: 800,
    })
  })

  it('parses a line as a shape with line geometry', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500">
        <line id="door-line" x1="380" y1="200" x2="380" y2="350" stroke="#D9A300" stroke-width="2" />
      </svg>
    `)

    expect(document.layers[0]).toMatchObject({
      geometry: { type: 'line', x1: 380, x2: 380, y1: 200, y2: 350 },
      id: 'door-line',
      shapeType: 'line',
      style: {
        stroke: { type: 'color', value: '#D9A300' },
        strokeWidth: 2,
      },
      type: 'shape',
    })
  })

  it('parses a dashed line as an editable shape', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500">
        <line xmlns="http://www.w3.org/2000/svg" id="dashed-line" x1="0" y1="0" x2="100" y2="0" stroke="#000" stroke-dasharray="4 6" />
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      {
        id: 'dashed-line',
        isLocked: false,
        style: { strokeDasharray: '4 6' },
        type: 'shape',
      },
    ])
    expect(result.warnings).not.toContainEqual(
      expect.objectContaining({ editorId: 'dashed-line' }),
    )
  })

  it('keeps an inherited group fill on editable children', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        <defs>
          <linearGradient id="logo-gradient"><stop offset="0%" stop-color="#fff" /></linearGradient>
        </defs>
        <g id="logo" fill="url(#logo-gradient)">
          <path id="logo-mark" d="M0 0 L20 20" />
        </g>
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      {
        children: [{ id: 'logo-mark', isLocked: false, type: 'shape' }],
        fill: { gradientId: 'logo-gradient', type: 'gradient' },
        id: 'logo',
        isLocked: false,
        type: 'group',
      },
    ])
    expect(result.warnings).not.toContainEqual(
      expect.objectContaining({ editorId: 'logo' }),
    )
  })

  it('locks unsupported elements without discarding editable siblings', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <defs>
          <filter id="shadow"><feGaussianBlur stdDeviation="4" /></filter>
        </defs>
        <rect id="editable" x="0" y="0" width="500" height="500" fill="#FFFFFF" />
        <rect id="locked" x="0" y="0" width="500" height="500" filter="url(#shadow)" />
      </svg>
    `)

    expect(result.document.definitions).toMatchObject({
      rawSvg: expect.stringContaining('<filter'),
    })
    expect(result.document.layers).toMatchObject([
      { id: 'editable', isLocked: false, type: 'shape' },
      { id: 'locked', isLocked: true, type: 'raw' },
    ])
    expect(result.warnings).toContainEqual({
      code: 'locked-raw-node',
      editorId: 'locked',
      elementName: 'rect',
      reason: 'Unsupported SVG attribute: filter',
    })
  })

  it('keeps malformed supported nodes as raw layers without aborting import', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <circle id="invalid-radius" cx="250" cy="250" r="not-a-number" />
        <rect id="editable" x="0" y="0" width="500" height="500" />
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      { id: 'invalid-radius', isLocked: true, type: 'raw' },
      { id: 'editable', isLocked: false, type: 'shape' },
    ])
    expect(result.warnings).toContainEqual({
      code: 'locked-raw-node',
      editorId: 'invalid-radius',
      elementName: 'circle',
      reason: 'SVG attribute r on <circle> must be a number',
    })
  })

  it('preserves invalid supported definitions without aborting import', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <defs>
          <linearGradient id="incomplete-gradient"><stop offset="0%" /></linearGradient>
        </defs>
        <rect id="editable" x="0" y="0" width="500" height="500" />
      </svg>
    `)

    expect(result.document.definitions).toMatchObject({
      rawSvg: expect.stringContaining('incomplete-gradient'),
    })
    expect(result.document.layers).toMatchObject([
      { id: 'editable', isLocked: false, type: 'shape' },
    ])
  })

  it('removes unsafe SVG content before parsing editable layers', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <script>alert('unsafe')</script>
        <rect id="safe" x="0" y="0" width="500" height="500" onload="alert('unsafe')" />
      </svg>
    `)

    expect(result.document.layers[0]).toMatchObject({
      id: 'safe',
      isLocked: false,
      type: 'shape',
    })
    expect(result.warnings).toEqual(
      expect.arrayContaining([
        { code: 'removed-unsafe-element', elementName: 'script' },
        {
          attributeName: 'onload',
          code: 'removed-unsafe-attribute',
          elementName: 'rect',
        },
      ]),
    )
  })

  it('inherits a group fill for editable and raw child elements', () => {
    const result = importSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <defs>
          <linearGradient id="building-gradient">
            <stop offset="0%" stop-color="#F2E9E4" />
            <stop offset="100%" stop-color="#C9ADA7" />
          </linearGradient>
        </defs>
        <g id="city" fill="url(#building-gradient)">
          <rect id="editable-building" x="20" y="280" width="40" height="60" />
          <rect x="65" y="260" width="35" height="80" filter="url(#building-shadow)" />
        </g>
      </svg>
    `)

    expect(result.document.layers).toMatchObject([
      {
        children: [
          {
            id: 'editable-building',
            style: {
              fill: { gradientId: 'building-gradient', type: 'gradient' },
            },
            type: 'shape',
          },
          {
            isLocked: true,
            rawSvg: expect.stringContaining('fill="url(#building-gradient)"'),
            type: 'raw',
          },
        ],
        id: 'city',
        type: 'group',
      },
    ])
  })
})
