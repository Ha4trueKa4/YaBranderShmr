export type SelectionBounds = {
  height: number
  width: number
  x: number
  y: number
}

function toSvgPoint(
  point: DOMPoint,
  elementMatrix: DOMMatrix,
  svgMatrixInverse: DOMMatrix,
) {
  return point.matrixTransform(elementMatrix).matrixTransform(svgMatrixInverse)
}

export function getSelectionBounds(
  element: SVGGraphicsElement | undefined,
): SelectionBounds | null {
  if (!element) {
    return null
  }

  const svg = element.ownerSVGElement
  const elementMatrix = element.getScreenCTM()
  const svgMatrix = svg?.getScreenCTM()

  if (!svg || !elementMatrix || !svgMatrix) {
    return null
  }

  const boundingBox = element.getBBox()
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
