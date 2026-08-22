import type { EditorDefinitions, EditorNode } from '@entities/editorDocument'
import { useTranslation } from 'react-i18next'
import { getLayerPresentation } from '../layerPresentation'
import { GroupProperties } from './groupProperties/GroupProperties'
import styles from './InspectorPanel.module.css'
import { ShapeProperties } from './shapeProperties/ShapeProperties'
import { TextProperties } from './textProperties/TextProperties'

interface InspectorPanelProps {
  definitions?: EditorDefinitions
  onShapeFillChange: (nodeId: string, color: string) => void
  onShapeFillOpacityChange: (nodeId: string, opacity: number) => void
  onGradientStopColorChange: (
    gradientId: string,
    stopIndex: number,
    color: string,
  ) => void
  onShapeOpacityChange: (nodeId: string, opacity: number) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
  onShapeStrokeAdd: (nodeId: string) => void
  onShapeStrokeColorChange: (nodeId: string, color: string) => void
  onShapeStrokeRemove: (nodeId: string) => void
  onShapeStrokeWidthChange: (nodeId: string, strokeWidth: number) => void
  onTextAnchorChange: (
    nodeId: string,
    textAnchor: 'end' | 'middle' | 'start',
  ) => void
  onTextContentChange: (nodeId: string, text: string) => void
  onTextFillColorChange: (nodeId: string, color: string) => void
  onTextFontFamilyChange: (nodeId: string, fontFamily: string) => void
  onTextFontSizeChange: (nodeId: string, fontSize: number) => void
  onTextFontStyleChange: (
    nodeId: string,
    fontStyle: 'italic' | 'normal',
  ) => void
  onTextFontWeightChange: (nodeId: string, fontWeight: number) => void
  onTextPositionChange: (nodeId: string, x: number, y: number) => void
  selectedNode: EditorNode | null
  showHeader?: boolean
}

function InspectorHeader({
  label,
  showHeader,
}: {
  label?: string
  showHeader: boolean
}) {
  const { t } = useTranslation()
  if (!showHeader) {
    return null
  }

  return (
    <div className={styles.header}>
      <h2>{t('Inspector')}</h2>
      {label && <span className={styles.typeLabel}>{label}</span>}
    </div>
  )
}

export function InspectorPanel({
  definitions,
  onShapeFillChange,
  onShapeFillOpacityChange,
  onGradientStopColorChange,
  onShapeOpacityChange,
  onShapePositionChange,
  onShapeStrokeAdd,
  onShapeStrokeColorChange,
  onShapeStrokeRemove,
  onShapeStrokeWidthChange,
  onTextAnchorChange,
  onTextContentChange,
  onTextFillColorChange,
  onTextFontFamilyChange,
  onTextFontSizeChange,
  onTextFontStyleChange,
  onTextFontWeightChange,
  onTextPositionChange,
  selectedNode,
  showHeader = true,
}: InspectorPanelProps) {
  const { t } = useTranslation()
  if (!selectedNode) {
    return (
      <section className={styles.panel}>
        <InspectorHeader showHeader={showHeader} />
        <div className={styles.content}>
          <p>{t('Select a layer to edit its properties.')}</p>
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'group') {
    return (
      <section className={styles.panel}>
        <InspectorHeader
          label={t(getLayerPresentation(selectedNode).label)}
          showHeader={showHeader}
        />
        <div className={styles.content}>
          <GroupProperties layer={selectedNode} />
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'raw') {
    return (
      <section className={styles.panel}>
        <InspectorHeader
          label={t(getLayerPresentation(selectedNode).label)}
          showHeader={showHeader}
        />
        <div className={styles.content}>
          <p>
            {t(
              'This SVG layer is locked because its features are not editable yet.',
            )}
          </p>
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'shape') {
    return (
      <section className={styles.panel}>
        <InspectorHeader
          label={t(getLayerPresentation(selectedNode).label)}
          showHeader={showHeader}
        />
        <div className={styles.content}>
          <ShapeProperties
            definitions={definitions}
            layer={selectedNode}
            onFillChange={(color) => onShapeFillChange(selectedNode.id, color)}
            onFillOpacityChange={(opacity) =>
              onShapeFillOpacityChange(selectedNode.id, opacity)
            }
            onGradientStopColorChange={(gradientId, stopIndex, color) =>
              onGradientStopColorChange(gradientId, stopIndex, color)
            }
            onOpacityChange={(opacity) =>
              onShapeOpacityChange(selectedNode.id, opacity)
            }
            onPositionChange={(x, y) =>
              onShapePositionChange(selectedNode.id, x, y)
            }
            onStrokeAdd={() => onShapeStrokeAdd(selectedNode.id)}
            onStrokeColorChange={(color) =>
              onShapeStrokeColorChange(selectedNode.id, color)
            }
            onStrokeRemove={() => onShapeStrokeRemove(selectedNode.id)}
            onStrokeWidthChange={(strokeWidth) =>
              onShapeStrokeWidthChange(selectedNode.id, strokeWidth)
            }
          />
        </div>
      </section>
    )
  }

  return (
    <section className={styles.panel}>
      <InspectorHeader
        label={t(getLayerPresentation(selectedNode).label)}
        showHeader={showHeader}
      />
      <div className={styles.content}>
        <TextProperties
          layer={selectedNode}
          onAnchorChange={(textAnchor) =>
            onTextAnchorChange(selectedNode.id, textAnchor)
          }
          onContentChange={(text) => onTextContentChange(selectedNode.id, text)}
          onFillColorChange={(color) =>
            onTextFillColorChange(selectedNode.id, color)
          }
          onFontFamilyChange={(fontFamily) =>
            onTextFontFamilyChange(selectedNode.id, fontFamily)
          }
          onFontSizeChange={(fontSize) =>
            onTextFontSizeChange(selectedNode.id, fontSize)
          }
          onFontStyleChange={(fontStyle) =>
            onTextFontStyleChange(selectedNode.id, fontStyle)
          }
          onFontWeightChange={(fontWeight) =>
            onTextFontWeightChange(selectedNode.id, fontWeight)
          }
          onPositionChange={(x, y) =>
            onTextPositionChange(selectedNode.id, x, y)
          }
        />
      </div>
    </section>
  )
}
