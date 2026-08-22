import type { EditorNode, EditorShape } from '@entities/editorDocument'
import { observer } from 'mobx-react-lite'
import {
  type PointerEvent as ReactPointerEvent,
  type RefObject,
  useLayoutEffect,
  useRef,
  useState,
} from 'react'

import type { CanvasTool } from '../../model/canvasTool'
import { constrainSelectionBounds } from '../selectionOverlay/model/constrainSelectionBounds'
import {
  getSelectionBounds,
  type SelectionBounds,
} from '../selectionOverlay/model/getSelectionBounds'
import {
  getScaledTransform,
  type ScaleHandle,
} from './model/getScaledTransform'
import styles from './TransformHandles.module.css'

const CORNER_HANDLE_SIZE = 6
const ROTATION_HANDLE_SIZE = 13
const SIDE_HIT_AREA_SIZE = 16

type PointerPosition = { x: number; y: number }
type ScaleState = {
  bounds: SelectionBounds
  handleBounds: SelectionBounds
  handle: ScaleHandle
  initialTransform: EditorShape['transform']
  nodeId: string
  pointerId: number
}

interface TransformHandlesProps {
  activeTool: CanvasTool
  canvasHeight: number
  canvasWidth: number
  canvasX: number
  canvasY: number
  getEditableElement: (nodeId: string) => SVGGElement | undefined
  onDocumentChangeEnd: () => void
  onDocumentChangeStart: () => void
  onShapeTransformChange: (
    nodeId: string,
    transform: Pick<EditorShape['transform'], 'scaleX' | 'scaleY' | 'x' | 'y'>,
  ) => void
  selectedNode: EditorNode | null
  selectedNodes: EditorNode[]
  svgReference: RefObject<SVGSVGElement | null>
}

function getSvgPointerPosition(
  svg: SVGSVGElement,
  clientX: number,
  clientY: number,
): PointerPosition | null {
  const screenMatrix = svg.getScreenCTM()

  if (!screenMatrix) {
    return null
  }

  const point = new DOMPoint(clientX, clientY).matrixTransform(
    screenMatrix.inverse(),
  )

  return { x: point.x, y: point.y }
}

function hasSameBounds(
  firstBounds: SelectionBounds | null,
  secondBounds: SelectionBounds | null,
) {
  return (
    firstBounds?.height === secondBounds?.height &&
    firstBounds?.width === secondBounds?.width &&
    firstBounds?.x === secondBounds?.x &&
    firstBounds?.y === secondBounds?.y
  )
}

function getCornerCenter(bounds: SelectionBounds, handle: ScaleHandle) {
  const isWest = handle === 'northWest' || handle === 'southWest'
  const isNorth = handle === 'northWest' || handle === 'northEast'

  return {
    x: isWest ? bounds.x : bounds.x + bounds.width,
    y: isNorth ? bounds.y : bounds.y + bounds.height,
  }
}

function getHandlePosition(center: PointerPosition, size: number) {
  return { x: center.x - size / 2, y: center.y - size / 2 }
}

function getScalingPointer(
  pointer: PointerPosition,
  bounds: SelectionBounds,
  handleBounds: SelectionBounds,
  handle: ScaleHandle,
): PointerPosition {
  const isWest =
    handle === 'northWest' || handle === 'southWest' || handle === 'west'
  const isNorth =
    handle === 'north' || handle === 'northEast' || handle === 'northWest'
  const isHorizontal = handle !== 'north' && handle !== 'south'
  const isVertical = handle !== 'east' && handle !== 'west'

  return {
    x: isHorizontal
      ? pointer.x +
        (isWest
          ? bounds.x - handleBounds.x
          : bounds.x + bounds.width - (handleBounds.x + handleBounds.width))
      : pointer.x,
    y: isVertical
      ? pointer.y +
        (isNorth
          ? bounds.y - handleBounds.y
          : bounds.y + bounds.height - (handleBounds.y + handleBounds.height))
      : pointer.y,
  }
}

function getShapeRenderSignature(shape: EditorShape | null) {
  if (!shape) {
    return ''
  }

  return [
    shape.id,
    shape.transform.rotation,
    shape.transform.scaleX,
    shape.transform.scaleY,
    shape.transform.x,
    shape.transform.y,
    JSON.stringify(shape.geometry),
    shape.style.strokeWidth,
  ].join(':')
}

export const TransformHandles = observer(function TransformHandles({
  activeTool,
  canvasHeight,
  canvasWidth,
  canvasX,
  canvasY,
  getEditableElement,
  onDocumentChangeEnd,
  onDocumentChangeStart,
  onShapeTransformChange,
  selectedNode,
  selectedNodes,
  svgReference,
}: TransformHandlesProps) {
  const [bounds, setBounds] = useState<SelectionBounds | null>(null)
  const scaleStateReference = useRef<ScaleState | null>(null)
  const selectedShape =
    selectedNodes.length === 1 && selectedNode?.type === 'shape'
      ? selectedNode
      : null
  const shapeRenderSignature = getShapeRenderSignature(selectedShape)
  const canScale =
    activeTool === 'select' &&
    selectedShape !== null &&
    !selectedShape.isLocked &&
    selectedShape.transform.rotation === 0

  useLayoutEffect(() => {
    const nextBounds = shapeRenderSignature
      ? getSelectionBounds(getEditableElement(selectedShape?.id ?? ''))
      : null

    setBounds((currentBounds) =>
      hasSameBounds(currentBounds, nextBounds) ? currentBounds : nextBounds,
    )
  }, [getEditableElement, selectedShape, shapeRenderSignature])

  function startScaling(
    handle: ScaleHandle,
    event: ReactPointerEvent<SVGRectElement>,
  ) {
    const svg = svgReference.current

    if (!canScale || !selectedShape || !bounds || !svg || event.button !== 0) {
      return
    }

    event.preventDefault()
    event.stopPropagation()
    event.currentTarget.setPointerCapture(event.pointerId)
    scaleStateReference.current = {
      bounds,
      handleBounds,
      handle,
      initialTransform: { ...selectedShape.transform },
      nodeId: selectedShape.id,
      pointerId: event.pointerId,
    }
    onDocumentChangeStart()
  }

  function handlePointerMove(event: ReactPointerEvent<SVGRectElement>) {
    const scaleState = scaleStateReference.current
    const svg = svgReference.current

    if (!scaleState || scaleState.pointerId !== event.pointerId || !svg) {
      return
    }

    const pointer = getSvgPointerPosition(svg, event.clientX, event.clientY)

    if (!pointer) {
      return
    }

    event.preventDefault()
    onShapeTransformChange(
      scaleState.nodeId,
      getScaledTransform(
        scaleState.bounds,
        scaleState.handle,
        scaleState.initialTransform,
        getScalingPointer(
          pointer,
          scaleState.bounds,
          scaleState.handleBounds,
          scaleState.handle,
        ),
      ),
    )
  }

  function stopScaling(event: ReactPointerEvent<SVGRectElement>) {
    const scaleState = scaleStateReference.current

    if (!scaleState || scaleState.pointerId !== event.pointerId) {
      return
    }

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    scaleStateReference.current = null
    onDocumentChangeEnd()
  }

  if (!canScale || !bounds || !selectedShape) {
    return null
  }

  const handleBounds = constrainSelectionBounds(
    bounds,
    canvasWidth,
    canvasHeight,
    canvasX,
    canvasY,
  )
  const northWest = getHandlePosition(
    getCornerCenter(handleBounds, 'northWest'),
    CORNER_HANDLE_SIZE,
  )
  const northEast = getHandlePosition(
    getCornerCenter(handleBounds, 'northEast'),
    ROTATION_HANDLE_SIZE,
  )
  const southWest = getHandlePosition(
    getCornerCenter(handleBounds, 'southWest'),
    CORNER_HANDLE_SIZE,
  )
  const southEast = getHandlePosition(
    getCornerCenter(handleBounds, 'southEast'),
    CORNER_HANDLE_SIZE,
  )
  const scaleCorners: Array<{
    handle: 'northWest' | 'southEast' | 'southWest'
    position: PointerPosition
  }> = [
    { handle: 'northWest', position: northWest },
    { handle: 'southWest', position: southWest },
    { handle: 'southEast', position: southEast },
  ]
  const sideOffset = SIDE_HIT_AREA_SIZE / 2
  const sideHandlers = {
    onPointerCancel: stopScaling,
    onPointerMove: handlePointerMove,
    onPointerUp: stopScaling,
  }

  return (
    <g data-editor-node-id={selectedShape.id}>
      <rect
        className={`${styles.sideHitArea} ${styles.north}`}
        height={SIDE_HIT_AREA_SIZE}
        onPointerDown={(event) => startScaling('north', event)}
        {...sideHandlers}
        width={Math.max(handleBounds.width - CORNER_HANDLE_SIZE, 0)}
        x={handleBounds.x + CORNER_HANDLE_SIZE / 2}
        y={handleBounds.y - sideOffset}
      />
      <rect
        className={`${styles.sideHitArea} ${styles.east}`}
        height={Math.max(handleBounds.height - CORNER_HANDLE_SIZE, 0)}
        onPointerDown={(event) => startScaling('east', event)}
        {...sideHandlers}
        width={SIDE_HIT_AREA_SIZE}
        x={handleBounds.x + handleBounds.width - sideOffset}
        y={handleBounds.y + CORNER_HANDLE_SIZE / 2}
      />
      <rect
        className={`${styles.sideHitArea} ${styles.south}`}
        height={SIDE_HIT_AREA_SIZE}
        onPointerDown={(event) => startScaling('south', event)}
        {...sideHandlers}
        width={Math.max(handleBounds.width - CORNER_HANDLE_SIZE, 0)}
        x={handleBounds.x + CORNER_HANDLE_SIZE / 2}
        y={handleBounds.y + handleBounds.height - sideOffset}
      />
      <rect
        className={`${styles.sideHitArea} ${styles.west}`}
        height={Math.max(handleBounds.height - CORNER_HANDLE_SIZE, 0)}
        onPointerDown={(event) => startScaling('west', event)}
        {...sideHandlers}
        width={SIDE_HIT_AREA_SIZE}
        x={handleBounds.x - sideOffset}
        y={handleBounds.y + CORNER_HANDLE_SIZE / 2}
      />
      {scaleCorners.map(({ handle, position }) => (
        <rect
          className={`${styles.cornerHandle} ${styles[handle]}`}
          height={CORNER_HANDLE_SIZE}
          key={handle}
          onPointerDown={(event) => startScaling(handle, event)}
          {...sideHandlers}
          width={CORNER_HANDLE_SIZE}
          x={position.x}
          y={position.y}
        />
      ))}
      <g
        className={styles.rotationHandle}
        transform={`translate(${northEast.x} ${northEast.y})`}
      >
        <circle
          cx={ROTATION_HANDLE_SIZE / 2}
          cy={ROTATION_HANDLE_SIZE / 2}
          r={ROTATION_HANDLE_SIZE / 2 - 1}
        />
        <g transform="translate(2 0.48) scale(0.92)">
          <path d="M7.7 4A3.4 3.4 0 1 0 8.5 8" />
          <path d="M7.7 2v2.5H5.2" />
        </g>
      </g>
    </g>
  )
})
