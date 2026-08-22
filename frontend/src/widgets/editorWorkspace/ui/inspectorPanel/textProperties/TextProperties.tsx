import type { EditorText } from '@entities/editorDocument'
import { Input } from '@shared/ui'
import { observer } from 'mobx-react-lite'
import { useTranslation } from 'react-i18next'

import { FillProperty } from '../components/FillProperty'
import { ScrubbableNumberInput } from '../components/ScrubbableNumberInput'
import { TransformProperties } from '../components/TransformProperties'
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
  measuredRotationCenter?: { x: number; y: number }
  onAnchorChange: (textAnchor: 'end' | 'middle' | 'start') => void
  onContentChange: (text: string) => void
  onFillColorChange: (color: string) => void
  onEyeDropperEnd?: () => void
  onEyeDropperStart?: () => void
  onFontFamilyChange: (fontFamily: string) => void
  onFontSizeChange: (fontSize: number) => void
  onFontStyleChange: (fontStyle: 'italic' | 'normal') => void
  onFontWeightChange: (fontWeight: number) => void
  onPositionChange: (x: number, y: number) => void
  onRotationChange: (
    rotation: number,
    rotationCenter: { x: number; y: number },
  ) => void
  onTransformChange: (transform: {
    rotationCenter?: { x: number; y: number }
    scaleX: number
    scaleY: number
    x: number
    y: number
  }) => void
}

function getFiniteNumber(value: string): number | null {
  const number = Number(value)

  return Number.isFinite(number) ? number : null
}

export const TextProperties = observer(function TextProperties({
  layer,
  measuredRotationCenter,
  onAnchorChange,
  onContentChange,
  onFillColorChange,
  onEyeDropperEnd,
  onEyeDropperStart,
  onFontFamilyChange,
  onFontSizeChange,
  onFontStyleChange,
  onFontWeightChange,
  onPositionChange,
  onRotationChange,
  onTransformChange,
}: TextPropertiesProps) {
  const { t } = useTranslation()
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
      <p className={styles.sectionTitle}>{t('Text')}</p>
      <div className={styles.field}>
        <span>{t('Content')}</span>
        <textarea
          className={styles.textarea}
          maxLength={1000}
          onChange={(event) => onContentChange(event.target.value)}
          value={layer.text}
        />
      </div>
      <div className={styles.fieldGrid}>
        <div className={styles.field}>
          <span>{t('X')}</span>
          <ScrubbableNumberInput
            onValueChange={(value) => handlePositionChange('x', String(value))}
            value={layer.x}
          />
        </div>
        <div className={styles.field}>
          <span>{t('Y')}</span>
          <ScrubbableNumberInput
            axis="vertical"
            onValueChange={(value) => handlePositionChange('y', String(value))}
            value={layer.y}
          />
        </div>
      </div>
      <TransformProperties
        layer={layer}
        measuredRotationCenter={measuredRotationCenter}
        onRotationChange={onRotationChange}
        onTransformChange={onTransformChange}
      />
      <FillProperty
        fill={fill}
        onFillChange={onFillColorChange}
        onEyeDropperEnd={onEyeDropperEnd}
        onEyeDropperStart={onEyeDropperStart}
        title={t('Color')}
      />
      <label className={styles.field}>
        {t('Font family')}
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
          <span>{t('Font size')}</span>
          <Input
            min="1"
            onChange={(event) => handleFontSizeChange(event.target.value)}
            type="number"
            value={layer.style.fontSize}
          />
        </div>
        <label className={styles.field}>
          {t('Weight')}
          <select
            className={styles.select}
            onChange={(event) => onFontWeightChange(Number(event.target.value))}
            value={layer.style.fontWeight ?? 400}
          >
            <option value="400">{t('Regular')}</option>
            <option value="500">{t('Medium')}</option>
            <option value="600">{t('Semibold')}</option>
            <option value="700">{t('Bold')}</option>
          </select>
        </label>
      </div>
      <div className={styles.fieldGrid}>
        <label className={styles.field}>
          {t('Style')}
          <select
            className={styles.select}
            onChange={(event) =>
              onFontStyleChange(event.target.value as 'italic' | 'normal')
            }
            value={layer.style.fontStyle ?? 'normal'}
          >
            <option value="normal">{t('Normal')}</option>
            <option value="italic">{t('Italic')}</option>
          </select>
        </label>
        <label className={styles.field}>
          {t('Align')}
          <select
            className={styles.select}
            onChange={(event) =>
              onAnchorChange(event.target.value as 'end' | 'middle' | 'start')
            }
            value={layer.style.textAnchor ?? 'start'}
          >
            <option value="start">{t('Left')}</option>
            <option value="middle">{t('Center')}</option>
            <option value="end">{t('Right')}</option>
          </select>
        </label>
      </div>
    </div>
  )
})
