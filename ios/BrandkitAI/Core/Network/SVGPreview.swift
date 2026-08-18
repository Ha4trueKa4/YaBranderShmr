import CoreGraphics
import SVGView

/// Загруженное и распарсенное SVG-превью проекта.
struct SVGPreview {
    let node: SVGNode
    /// Пропорции из viewBox; 1 — если viewBox не задан.
    let aspectRatio: CGFloat
}
