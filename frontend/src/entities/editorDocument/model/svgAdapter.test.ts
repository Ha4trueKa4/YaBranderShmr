import { describe, expect, it } from 'vitest'

import { parseSvg } from './svgAdapter'

describe('parseSvg', () => {
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
        transform: { rotation: 5, scaleX: 1.2, scaleY: 0.8, x: 20, y: 10 },
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
