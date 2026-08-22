export type SelectionBounds = {
  height: number
  width: number
  x: number
  y: number
}

export type SelectionPoint = { x: number; y: number }

function toSvgPoint(
  point: DOMPoint,
  elementMatrix: DOMMatrix,
  svgMatrixInverse: DOMMatrix,
) {
  return point.matrixTransform(elementMatrix).matrixTransform(svgMatrixInverse)
}

function getMeasurementTarget(element: SVGGraphicsElement) {
  const child = element.firstElementChild

  return child instanceof SVGGraphicsElement ? child : element
}

export function getSelectionBounds(
  element: SVGGraphicsElement | undefined,
): SelectionBounds | null {
  if (!element) {
    return null
  }

  const measurementTarget = getMeasurementTarget(element)
  const svg = measurementTarget.ownerSVGElement
  const elementMatrix = measurementTarget.getScreenCTM()
  const svgMatrix = svg?.getScreenCTM()

  if (!svg || !elementMatrix || !svgMatrix) {
    return null
  }

  const boundingBox = measurementTarget.getBBox()
  const svgMatrixInverse = svgMatrix.inverse()
  const corners = [
    new DOMPoint(boundingBox.x, boundingBox.y),
    new DOMPoint(boundingBox.x + boundingBox.width, boundingBox.y),
    new DOMPoint(boundingBox.x, boundingBox.y + boundingBox.height),
    new DOMPoint(
      boundingBox.x + boundingBox.width,
      boundingBox.y + boundingBox.height,
    ),
  ].map((point) => toSvgPoint(point, elementMatrix, svgMatrixInverse))
  const xCoordinates = corners.map((point) => point.x)
  const yCoordinates = corners.map((point) => point.y)
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

export function getSelectionOutline(
  element: SVGGraphicsElement | undefined,
  padding = 0,
): SelectionPoint[] | null {
  if (!element) {
    return null
  }

  const measurementTarget = getMeasurementTarget(element)
  const svg = measurementTarget.ownerSVGElement
  const elementMatrix = measurementTarget.getScreenCTM()
  const svgMatrix = svg?.getScreenCTM()

  if (!svg || !elementMatrix || !svgMatrix) {
    return null
  }

  const boundingBox = measurementTarget.getBBox()
  const svgMatrixInverse = svgMatrix.inverse()

  return [
    new DOMPoint(boundingBox.x - padding, boundingBox.y - padding),
    new DOMPoint(
      boundingBox.x + boundingBox.width + padding,
      boundingBox.y - padding,
    ),
    new DOMPoint(
      boundingBox.x + boundingBox.width + padding,
      boundingBox.y + boundingBox.height + padding,
    ),
    new DOMPoint(
      boundingBox.x - padding,
      boundingBox.y + boundingBox.height + padding,
    ),
  ].map((point) => toSvgPoint(point, elementMatrix, svgMatrixInverse))
}
