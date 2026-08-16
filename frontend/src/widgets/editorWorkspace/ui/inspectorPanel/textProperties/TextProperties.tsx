import type { EditorText } from '@entities/editorDocument'
import { Input } from '@shared/ui'
import { observer } from 'mobx-react-lite'

import { FillProperty } from '../components/FillProperty'
import styles from '../Properties.module.css'

const fontFamilies = [
  'Arial',
  'Helvetica',
  'Inter',
  'Roboto',
  'sans-serif',
  'serif',
  'monospace',
] as const

interface TextPropertiesProps {
  layer: EditorText
  onAnchorChange: (textAnchor: 'end' | 'middle' | 'start') => void
  onContentChange: (text: string) => void
  onFillColorChange: (color: string) => void
  onFontFamilyChange: (fontFamily: string) => void
  onFontSizeChange: (fontSize: number) => void
  onFontStyleChange: (fontStyle: 'italic' | 'normal') => void
  onFontWeightChange: (fontWeight: number) => void
  onPositionChange: (x: number, y: number) => void
}

function getFiniteNumber(value: string): number | null {
  const number = Number(value)

  return Number.isFinite(number) ? number : null
}

export const TextProperties = observer(function TextProperties({
  layer,
  onAnchorChange,
  onContentChange,
  onFillColorChange,
  onFontFamilyChange,
  onFontSizeChange,
  onFontStyleChange,
  onFontWeightChange,
  onPositionChange,
}: TextPropertiesProps) {
  const fill =
    layer.style.fill.type === 'color' ? layer.style.fill.value : '#000000'

  function handlePositionChange(axis: 'x' | 'y', value: string) {
    const coordinate = getFiniteNumber(value)

    if (coordinate === null) {
      return
    }

    onPositionChange(
      axis === 'x' ? coordinate : layer.x,
      axis === 'y' ? coordinate : layer.y,
    )
  }

  function handleFontSizeChange(value: string) {
    const fontSize = getFiniteNumber(value)

    if (fontSize !== null) {
      onFontSizeChange(fontSize)
    }
  }

  return (
    <div className={styles.properties}>
      <p className={styles.sectionTitle}>Text</p>
      <div className={styles.field}>
        <span>Content</span>
        <textarea
          className={styles.textarea}
          maxLength={1000}
          onChange={(event) => onContentChange(event.target.value)}
          value={layer.text}
        />
      </div>
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>X</span>
          <Input
            onChange={(event) => handlePositionChange('x', event.target.value)}
            type="number"
            value={layer.x}
          />
        </div>
        <div className={styles.field}>
          <span>Y</span>
          <Input
            onChange={(event) => handlePositionChange('y', event.target.value)}
            type="number"
            value={layer.y}
          />
        </div>
      </div>
      <FillProperty
        fill={fill}
        onFillChange={onFillColorChange}
        title="Color"
      />
      <label className={styles.field}>
        Font family
        <select
          className={styles.select}
          onChange={(event) => onFontFamilyChange(event.target.value)}
          value={layer.style.fontFamily}
        >
          {fontFamilies.map((fontFamily) => (
            <option key={fontFamily} value={fontFamily}>
              {fontFamily}
            </option>
          ))}
        </select>
      </label>
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>Font size</span>
          <Input
            min="1"
            onChange={(event) => handleFontSizeChange(event.target.value)}
            type="number"
            value={layer.style.fontSize}
          />
        </div>
        <label className={styles.field}>
          Weight
          <select
            className={styles.select}
            onChange={(event) => onFontWeightChange(Number(event.target.value))}
            value={layer.style.fontWeight ?? 400}
          >
            <option value="400">Regular</option>
            <option value="500">Medium</option>
            <option value="600">Semibold</option>
            <option value="700">Bold</option>
          </select>
        </label>
      </div>
      <div className={styles.fieldGrid}>
        <label className={styles.field}>
          Style
          <select
            className={styles.select}
            onChange={(event) =>
              onFontStyleChange(event.target.value as 'italic' | 'normal')
            }
            value={layer.style.fontStyle ?? 'normal'}
          >
            <option value="normal">Normal</option>
            <option value="italic">Italic</option>
          </select>
        </label>
        <label className={styles.field}>
          Align
          <select
            className={styles.select}
            onChange={(event) =>
              onAnchorChange(event.target.value as 'end' | 'middle' | 'start')
            }
            value={layer.style.textAnchor ?? 'start'}
          >
            <option value="start">Left</option>
            <option value="middle">Center</option>
            <option value="end">Right</option>
          </select>
        </label>
      </div>
    </div>
  )
})
