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
  getSelectionOutline,
  type SelectionBounds,
  type SelectionPoint,
} from '../selectionOverlay/model/getSelectionBounds'
import { getRotatedScaledTransform } from './model/getRotatedScaledTransform'
import { getPointerAngle, getRotation } from './model/getRotation'
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
  center: PointerPosition
  handleBounds: SelectionBounds
  handle: ScaleHandle
  initialTransform: EditorShape['transform']
  nodeId: string
  pointerId: number
  rotation: number
}
type RotateState = {
  center: PointerPosition
  initialRotation: number
  rotationCenter: PointerPosition
  nodeId: string
  pointerId: number
  startPointerAngle: number
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
    transform: Pick<
      EditorShape['transform'],
      'rotationCenter' | 'scaleX' | 'scaleY' | 'x' | 'y'
    >,
  ) => void
  onShapeRotationChange: (
    nodeId: string,
    rotation: number,
    rotationCenter: PointerPosition,
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

function hasSameOutline(
  firstOutline: SelectionPoint[] | null,
  secondOutline: SelectionPoint[] | null,
) {
  if (!firstOutline || !secondOutline) {
    return firstOutline === secondOutline
  }

  return (
    firstOutline.length === secondOutline.length &&
    firstOutline.every(
      (point, index) =>
        point.x === secondOutline[index]?.x &&
        point.y === secondOutline[index]?.y,
    )
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

function rotatePoint(
  point: PointerPosition,
  center: PointerPosition,
  angle: number,
): PointerPosition {
  const angleInRadians = (angle * Math.PI) / 180
  const cosine = Math.cos(angleInRadians)
  const sine = Math.sin(angleInRadians)
  const relativeX = point.x - center.x
  const relativeY = point.y - center.y

  return {
    x: center.x + relativeX * cosine - relativeY * sine,
    y: center.y + relativeX * sine + relativeY * cosine,
  }
}

function getBoundsFromPoints(points: SelectionPoint[]): SelectionBounds {
  const xCoordinates = points.map((point) => point.x)
  const yCoordinates = points.map((point) => point.y)
  const minimumX = Math.min(...xCoordinates)
  const maximumX = Math.max(...xCoordinates)
  const minimumY = Math.min(...yCoordinates)
  const maximumY = Math.max(...yCoordinates)

  return {
    height: maximumY - minimumY,
    width: maximumX - minimumX,
    x: minimumX,
    y: minimumY,
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
  onShapeRotationChange,
  selectedNode,
  selectedNodes,
  svgReference,
}: TransformHandlesProps) {
  const [bounds, setBounds] = useState<SelectionBounds | null>(null)
  const [outline, setOutline] = useState<SelectionPoint[] | null>(null)
  const scaleStateReference = useRef<ScaleState | null>(null)
  const rotateStateReference = useRef<RotateState | null>(null)
  const selectedShape =
    selectedNodes.length === 1 && selectedNode?.type === 'shape'
      ? selectedNode
      : null
  const shapeRenderSignature = getShapeRenderSignature(selectedShape)
  const canTransform =
    activeTool === 'select' && selectedShape !== null && !selectedShape.isLocked
  const canScale = canTransform

  useLayoutEffect(() => {
    const nextBounds = shapeRenderSignature
      ? getSelectionBounds(getEditableElement(selectedShape?.id ?? ''))
      : null
    const nextOutline =
      selectedShape?.transform.rotation === 0
        ? null
        : getSelectionOutline(getEditableElement(selectedShape?.id ?? ''), 4)

    setBounds((currentBounds) =>
      hasSameBounds(currentBounds, nextBounds) ? currentBounds : nextBounds,
    )
    setOutline((currentOutline) =>
      hasSameOutline(currentOutline, nextOutline)
        ? currentOutline
        : nextOutline,
    )
  }, [getEditableElement, selectedShape, shapeRenderSignature])

  function startScaling(
    handle: ScaleHandle,
    event: ReactPointerEvent<SVGElement>,
  ) {
    const svg = svgReference.current

    if (!canScale || !selectedShape || !bounds || !svg || event.button !== 0) {
      return
    }

    const rotationCenter = selectedShape.transform.rotationCenter ?? {
      x: bounds.x + bounds.width / 2 - selectedShape.transform.x,
      y: bounds.y + bounds.height / 2 - selectedShape.transform.y,
    }
    const center = {
      x: selectedShape.transform.x + rotationCenter.x,
      y: selectedShape.transform.y + rotationCenter.y,
    }
    const editableElement = getEditableElement(selectedShape.id)
    const unrotatedOutline =
      selectedShape.transform.rotation === 0
        ? null
        : getSelectionOutline(editableElement)
    const unrotatedHandleOutline =
      selectedShape.transform.rotation === 0
        ? null
        : getSelectionOutline(editableElement, 4)
    const scalingBounds = unrotatedOutline
      ? getBoundsFromPoints(
          unrotatedOutline.map((point) =>
            rotatePoint(point, center, -selectedShape.transform.rotation),
          ),
        )
      : bounds
    const scalingHandleBounds = unrotatedHandleOutline
      ? getBoundsFromPoints(
          unrotatedHandleOutline.map((point) =>
            rotatePoint(point, center, -selectedShape.transform.rotation),
          ),
        )
      : handleBounds

    event.preventDefault()
    event.stopPropagation()
    event.currentTarget.setPointerCapture(event.pointerId)
    scaleStateReference.current = {
      bounds: scalingBounds,
      center,
      handleBounds: scalingHandleBounds,
      handle,
      initialTransform: { ...selectedShape.transform },
      nodeId: selectedShape.id,
      pointerId: event.pointerId,
      rotation: selectedShape.transform.rotation,
    }
    onDocumentChangeStart()
  }

  function handlePointerMove(event: ReactPointerEvent<SVGElement>) {
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
    const pointerInLocalAxes =
      scaleState.rotation === 0
        ? pointer
        : rotatePoint(pointer, scaleState.center, -scaleState.rotation)
    const scalingPointer = getScalingPointer(
      pointerInLocalAxes,
      scaleState.bounds,
      scaleState.handleBounds,
      scaleState.handle,
    )
    const nextTransform =
      scaleState.rotation === 0
        ? getScaledTransform(
            scaleState.bounds,
            scaleState.handle,
            scaleState.initialTransform,
            scalingPointer,
          )
        : getRotatedScaledTransform(
            scaleState.bounds,
            scaleState.handle,
            scaleState.initialTransform,
            scalingPointer,
          )

    onShapeTransformChange(scaleState.nodeId, nextTransform)
  }

  function stopScaling(event: ReactPointerEvent<SVGElement>) {
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

  function startRotating(event: ReactPointerEvent<SVGCircleElement>) {
    const svg = svgReference.current

    if (
      !canTransform ||
      !selectedShape ||
      !bounds ||
      !svg ||
      event.button !== 0
    ) {
      return
    }

    const pointer = getSvgPointerPosition(svg, event.clientX, event.clientY)

    if (!pointer) {
      return
    }

    const center = selectedShape.transform.rotationCenter
      ? {
          x:
            selectedShape.transform.x +
            selectedShape.transform.rotationCenter.x,
          y:
            selectedShape.transform.y +
            selectedShape.transform.rotationCenter.y,
        }
      : {
          x: bounds.x + bounds.width / 2,
          y: bounds.y + bounds.height / 2,
        }

    event.preventDefault()
    event.stopPropagation()
    event.currentTarget.setPointerCapture(event.pointerId)
    rotateStateReference.current = {
      center,
      initialRotation: selectedShape.transform.rotation,
      nodeId: selectedShape.id,
      pointerId: event.pointerId,
      rotationCenter: {
        x: center.x - selectedShape.transform.x,
        y: center.y - selectedShape.transform.y,
      },
      startPointerAngle: getPointerAngle(center, pointer),
    }
    onDocumentChangeStart()
  }

  function handleRotatePointerMove(event: ReactPointerEvent<SVGCircleElement>) {
    const rotateState = rotateStateReference.current
    const svg = svgReference.current

    if (!rotateState || rotateState.pointerId !== event.pointerId || !svg) {
      return
    }

    const pointer = getSvgPointerPosition(svg, event.clientX, event.clientY)

    if (!pointer) {
      return
    }

    event.preventDefault()
    onShapeRotationChange(
      rotateState.nodeId,
      getRotation(
        rotateState.initialRotation,
        rotateState.startPointerAngle,
        getPointerAngle(rotateState.center, pointer),
      ),
      rotateState.rotationCenter,
    )
  }

  function stopRotating(event: ReactPointerEvent<SVGCircleElement>) {
    const rotateState = rotateStateReference.current

    if (!rotateState || rotateState.pointerId !== event.pointerId) {
      return
    }

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    rotateStateReference.current = null
    onDocumentChangeEnd()
  }

  if (!canTransform || !bounds || !selectedShape) {
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
    outline?.[0] ?? getCornerCenter(handleBounds, 'northWest'),
    CORNER_HANDLE_SIZE,
  )
  const northEast = getHandlePosition(
    outline?.[1] ?? getCornerCenter(handleBounds, 'northEast'),
    ROTATION_HANDLE_SIZE,
  )
  const southWest = getHandlePosition(
    outline?.[3] ?? getCornerCenter(handleBounds, 'southWest'),
    CORNER_HANDLE_SIZE,
  )
  const southEast = getHandlePosition(
    outline?.[2] ?? getCornerCenter(handleBounds, 'southEast'),
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
  const rotatedSideHandles: Array<{
    end: SelectionPoint
    handle: 'east' | 'north' | 'south' | 'west'
    start: SelectionPoint
  }> = outline
    ? [
        { end: outline[1], handle: 'north', start: outline[0] },
        { end: outline[2], handle: 'east', start: outline[1] },
        { end: outline[3], handle: 'south', start: outline[2] },
        { end: outline[0], handle: 'west', start: outline[3] },
      ]
    : []
  const sideOffset = SIDE_HIT_AREA_SIZE / 2
  const sideHandlers = {
    onPointerCancel: stopScaling,
    onPointerMove: handlePointerMove,
    onPointerUp: stopScaling,
  }

  return (
    <g data-editor-node-id={selectedShape.id}>
      {canScale && selectedShape.transform.rotation === 0 && (
        <>
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
        </>
      )}
      {canScale &&
        selectedShape.transform.rotation !== 0 &&
        rotatedSideHandles.map(({ end, handle, start }) => (
          <line
            className={`${styles.rotatedSideHitArea} ${styles[handle]}`}
            key={handle}
            onPointerCancel={stopScaling}
            onPointerDown={(event) => startScaling(handle, event)}
            onPointerMove={handlePointerMove}
            onPointerUp={stopScaling}
            x1={start.x}
            x2={end.x}
            y1={start.y}
            y2={end.y}
          />
        ))}
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
          onPointerCancel={stopRotating}
          onPointerDown={startRotating}
          onPointerMove={handleRotatePointerMove}
          onPointerUp={stopRotating}
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
