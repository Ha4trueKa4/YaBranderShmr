import type { EditorNode } from '@entities/editorDocument'

import { getLayerPresentation } from '../layerPresentation'
import { GroupProperties } from './groupProperties/GroupProperties'
import styles from './InspectorPanel.module.css'
import { ShapeProperties } from './shapeProperties/ShapeProperties'
import { TextProperties } from './textProperties/TextProperties'

interface InspectorPanelProps {
  onShapeFillChange: (nodeId: string, color: string) => void
  onShapeFillOpacityChange: (nodeId: string, opacity: number) => void
  onShapeOpacityChange: (nodeId: string, opacity: number) => void
  onShapePositionChange: (nodeId: string, x: number, y: number) => void
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
}

export function InspectorPanel({
  onShapeFillChange,
  onShapeFillOpacityChange,
  onShapeOpacityChange,
  onShapePositionChange,
  onTextAnchorChange,
  onTextContentChange,
  onTextFillColorChange,
  onTextFontFamilyChange,
  onTextFontSizeChange,
  onTextFontStyleChange,
  onTextFontWeightChange,
  onTextPositionChange,
  selectedNode,
}: InspectorPanelProps) {
  if (!selectedNode) {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
        </div>
        <div className={styles.content}>
          <p>Select a layer to edit its properties.</p>
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'group') {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
          <span className={styles.typeLabel}>
            {getLayerPresentation(selectedNode).label}
          </span>
        </div>
        <div className={styles.content}>
          <GroupProperties layer={selectedNode} />
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'raw') {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
          <span className={styles.typeLabel}>
            {getLayerPresentation(selectedNode).label}
          </span>
        </div>
        <div className={styles.content}>
          <p>
            This SVG layer is locked because its features are not editable yet.
          </p>
        </div>
      </section>
    )
  }

  if (selectedNode.type === 'shape') {
    return (
      <section className={styles.panel}>
        <div className={styles.header}>
          <h2>Inspector</h2>
          <span className={styles.typeLabel}>
            {getLayerPresentation(selectedNode).label}
          </span>
        </div>
        <div className={styles.content}>
          <ShapeProperties
            layer={selectedNode}
            onFillChange={(color) => onShapeFillChange(selectedNode.id, color)}
            onFillOpacityChange={(opacity) =>
              onShapeFillOpacityChange(selectedNode.id, opacity)
            }
            onOpacityChange={(opacity) =>
              onShapeOpacityChange(selectedNode.id, opacity)
            }
            onPositionChange={(x, y) =>
              onShapePositionChange(selectedNode.id, x, y)
            }
          />
        </div>
      </section>
    )
  }

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <h2>Inspector</h2>
        <span className={styles.typeLabel}>
          {getLayerPresentation(selectedNode).label}
        </span>
      </div>
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
