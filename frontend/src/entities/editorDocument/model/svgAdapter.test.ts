import { describe, expect, it } from 'vitest'

import { parseSvg } from './svgAdapter'

describe('parseSvg', () => {
  it('rejects invalid SVG markup with multiple root elements', () => {
    expect(() => parseSvg('<svg></svg><svg></svg>')).toThrow(
      'Invalid SVG markup',
    )
  })

  it('rejects an SVG without a viewBox', () => {
    expect(() =>
      parseSvg('<svg xmlns="http://www.w3.org/2000/svg"></svg>'),
    ).toThrow('SVG root must define a viewBox')
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

  it('rejects text with independently styled nested fragments', () => {
    expect(() =>
      parseSvg(`
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
          <text id="title" x="20" y="40">Hello <tspan fill="#6C3BFF">world</tspan></text>
        </svg>
      `),
    ).toThrow('Unsupported SVG element: tspan')
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

  it('rejects an unsupported SVG element', () => {
    expect(() =>
      parseSvg(`
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
          <image id="external-image" href="https://example.com/logo.png" />
        </svg>
      `),
    ).toThrow('Unsupported SVG element: image')
  })
})
