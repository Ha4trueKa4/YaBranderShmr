import type { EditorElement, Transform } from '@entities/editorDocument'
import { Range } from '@shared/ui'
import { useTranslation } from 'react-i18next'

import styles from '../Properties.module.css'
import { getCenteredScaleTransform } from './model/getCenteredScaleTransform'

const maximumScalePercent = 200
const minimumScalePercent = 10

interface TransformPropertiesProps {
  layer: EditorElement
  measuredRotationCenter?: NonNullable<Transform['rotationCenter']>
  onRotationChange: (
    rotation: number,
    rotationCenter: NonNullable<Transform['rotationCenter']>,
  ) => void
  onTransformChange: (
    transform: Pick<
      Transform,
      'rotationCenter' | 'scaleX' | 'scaleY' | 'x' | 'y'
    >,
  ) => void
}

function getRotationCenter(
  layer: EditorElement,
  measuredRotationCenter?: NonNullable<Transform['rotationCenter']>,
) {
  if (layer.transform.rotationCenter) {
    return layer.transform.rotationCenter
  }

  if (measuredRotationCenter) {
    return measuredRotationCenter
  }

  if (layer.type === 'text') {
    return {
      x: layer.x * layer.transform.scaleX,
      y: layer.y * layer.transform.scaleY,
    }
  }

  switch (layer.geometry.type) {
    case 'circle':
    case 'ellipse':
      return {
        x: layer.geometry.cx * layer.transform.scaleX,
        y: layer.geometry.cy * layer.transform.scaleY,
      }
    case 'line':
      return {
        x:
          ((layer.geometry.x1 + layer.geometry.x2) / 2) *
          layer.transform.scaleX,
        y:
          ((layer.geometry.y1 + layer.geometry.y2) / 2) *
          layer.transform.scaleY,
      }
    case 'rect':
      return {
        x:
          (layer.geometry.x + layer.geometry.width / 2) *
          layer.transform.scaleX,
        y:
          (layer.geometry.y + layer.geometry.height / 2) *
          layer.transform.scaleY,
      }
    case 'path':
      return { x: 0, y: 0 }
  }
}

export function TransformProperties({
  layer,
  measuredRotationCenter,
  onRotationChange,
  onTransformChange,
}: TransformPropertiesProps) {
  const { t } = useTranslation()
  const scalePercent = Math.round(
    ((layer.transform.scaleX + layer.transform.scaleY) / 2) * 100,
  )

  function handleScaleChange(nextScalePercent: number) {
    const scale = nextScalePercent / 100

    onTransformChange(
      getCenteredScaleTransform(
        layer.transform,
        getRotationCenter(layer, measuredRotationCenter),
        scale,
      ),
    )
  }

  return (
    <section className={styles.transformProperties}>
      <p className={styles.sectionTitle}>{t('Transform')}</p>
      <div className={styles.field}>
        <span className={styles.rangeHeader}>
          <span>{t('Rotation')}</span>
          <strong>{Math.round(layer.transform.rotation)}°</strong>
        </span>
        <Range
          centered
          max={180}
          min={-180}
          onChange={(event) =>
            onRotationChange(
              Number(event.target.value),
              getRotationCenter(layer, measuredRotationCenter),
            )
          }
          step={1}
          value={layer.transform.rotation}
        />
      </div>
      <div className={styles.field}>
        <span className={styles.rangeHeader}>
          <span>{t('Scale')}</span>
          <strong>{scalePercent}%</strong>
        </span>
        <Range
          max={maximumScalePercent}
          min={minimumScalePercent}
          onChange={(event) => handleScaleChange(Number(event.target.value))}
          step={1}
          value={scalePercent}
        />
      </div>
    </section>
  )
}
