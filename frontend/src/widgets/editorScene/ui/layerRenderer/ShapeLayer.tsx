import type { EditorShape } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'

import { getPaintValue } from './model/getPaintValue'
import { getTransformValue } from './model/getTransformValue'

interface ShapeLayerProps {
  node: EditorShape
}

export const ShapeLayer = observer(function ShapeLayer({
  node,
}: ShapeLayerProps) {
  const commonProps = {
    fill: getPaintValue(node.style.fill),
    fillOpacity: node.style.fillOpacity,
    opacity: node.opacity,
    stroke: getPaintValue(node.style.stroke),
    strokeDasharray: node.style.strokeDasharray,
    strokeLinecap: node.style.strokeLineCap,
    strokeLinejoin: node.style.strokeLineJoin,
    strokeOpacity: node.style.strokeOpacity,
    strokeWidth: node.style.strokeWidth,
    transform: getTransformValue(node.transform),
  }

  switch (node.shapeType) {
    case 'rect':
      return <rect {...commonProps} {...node.geometry} />
    case 'circle':
      return <circle {...commonProps} {...node.geometry} />
    case 'ellipse':
      return <ellipse {...commonProps} {...node.geometry} />
    case 'line':
      return <line {...commonProps} {...node.geometry} />
    case 'path':
      return <path {...commonProps} {...node.geometry} />
  }
})
