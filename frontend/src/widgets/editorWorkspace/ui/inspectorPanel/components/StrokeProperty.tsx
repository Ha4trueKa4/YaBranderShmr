import { PropertyField } from './PropertyField'

interface StrokePropertyProps {
  stroke?: string
}

export function StrokeProperty({ stroke }: StrokePropertyProps) {
  return <PropertyField label="Stroke" value={stroke ?? 'None'} />
}
