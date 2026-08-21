import { describe, expect, it } from 'vitest'

import { parseSvg } from './parseSvg'
import { serializeSvg } from './serializeSvg'

describe('serializeSvg', () => {
  it('round-trips editable layers, definitions, locked SVG and text safely', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500">
        <defs>
          <filter id="shadow"><feGaussianBlur stdDeviation="4" /></filter>
          <linearGradient id="body-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#FFFFFF" />
            <stop offset="100%" stop-color="#000000" stop-opacity="0.5" />
          </linearGradient>
        </defs>
        <rect id="background" x="0" y="0" width="800" height="500" fill="url(#body-gradient)" />
        <g id="logo" transform="translate(20 10) rotate(5 250 250) scale(1.2 0.8)">
          <path id="mark" d="M0 0 L20 20" fill="none" stroke="#fff" stroke-width="2" />
          <text id="title" x="250" y="365" font-family="Inter" font-size="36">A &amp; B &lt; C</text>
        </g>
        <rect id="locked" x="0" y="0" width="10" height="10" filter="url(#shadow)" />
      </svg>
    `)
    document.layers[0].isVisible = false

    const serialized = serializeSvg(document)

    expect(serialized).toContain('fill="url(#body-gradient)"')
    expect(serialized).toContain('display="none"')
    expect(serialized).toContain('A &amp; B &lt; C')
    expect(serialized).toContain('<filter')
    expect(serialized).toContain('id="shadow"')
    expect(serialized.indexOf('data-editor-id="background"')).toBeLessThan(
      serialized.indexOf('data-editor-id="logo"'),
    )
    expect(parseSvg(serialized)).toEqual(document)
  })

  it('serializes multiline text as tspans and imports it as one text layer', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500">
        <text id="title" x="50" y="80" font-size="20">First line</text>
      </svg>
    `)
    const title = document.layers[0]

    if (title?.type !== 'text') {
      throw new Error('Expected a text layer')
    }

    title.text = 'First line\nSecond line'
    title.style.lineHeight = 26

    const serialized = serializeSvg(document)

    expect(serialized).toContain('<tspan dy="0" x="50">First line</tspan>')
    expect(serialized).toContain('<tspan dy="26" x="50">Second line</tspan>')
    expect(parseSvg(serialized)).toEqual(document)
  })

  it('round-trips a stroke dash array', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
        <line id="dashed-line" x1="0" y1="0" x2="100" y2="0" stroke="#000" stroke-dasharray="4 6" />
      </svg>
    `)

    const serialized = serializeSvg(document)

    expect(serialized).toContain('stroke-dasharray="4 6"')
    expect(parseSvg(serialized)).toEqual(document)
  })

  it('preserves the viewBox origin when serializing', () => {
    const document = parseSvg(`
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="-250 100 500 300">
        <text id="title" x="0" y="200">A</text>
      </svg>
    `)

    expect(serializeSvg(document)).toContain('viewBox="-250 100 500 300"')
  })
})
