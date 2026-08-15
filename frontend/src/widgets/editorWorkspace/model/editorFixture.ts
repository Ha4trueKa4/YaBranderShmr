import type { EditorDocument } from '@entities/editorDocument'

/**
 * Временная замена `project.data` для UI редактора, пока проект не загружается с API.
 * После загрузки проекта данные нужно валидировать и преобразовать в EditorDocument.
 */
export const editorFixture: EditorDocument = {
  height: 630,
  layers: [
    {
      children: [
        {
          geometry: {
            height: 630,
            type: 'rect',
            width: 1200,
            x: 0,
            y: 0,
          },
          id: 'background',
          isLocked: false,
          isVisible: true,
          name: 'Background',
          opacity: 1,
          role: 'background',
          shapeType: 'rect',
          style: {
            fill: { type: 'color', value: '#FFFFFF' },
            fillOpacity: 1,
            stroke: { type: 'none' },
            strokeOpacity: 1,
            strokeWidth: 0,
          },
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'shape',
        },
        {
          geometry: {
            cx: 220,
            cy: 240,
            r: 100,
            type: 'circle',
          },
          id: 'icon-shape',
          isLocked: false,
          isVisible: true,
          name: 'Icon shape',
          opacity: 1,
          shapeType: 'circle',
          style: {
            fill: { type: 'color', value: '#7C3AED' },
            fillOpacity: 1,
            stroke: { type: 'color', value: '#06B6D4' },
            strokeOpacity: 1,
            strokeWidth: 4,
          },
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'shape',
        },
        {
          id: 'company-name',
          isLocked: false,
          isVisible: true,
          name: 'Company Name',
          opacity: 1,
          style: {
            fill: { type: 'color', value: '#1D1D1F' },
            fontFamily: 'Inter',
            fontSize: 48,
            textAnchor: 'middle',
          },
          text: 'Orion Labs',
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'text',
          x: 600,
          y: 440,
        },
        {
          id: 'tagline',
          isLocked: false,
          isVisible: true,
          name: 'Tagline',
          opacity: 1,
          style: {
            fill: { type: 'color', value: '#6B7280' },
            fontFamily: 'Inter',
            fontSize: 24,
            textAnchor: 'middle',
          },
          text: 'Build better together',
          transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
          type: 'text',
          x: 600,
          y: 490,
        },
      ],
      id: 'logo',
      isLocked: false,
      isVisible: true,
      name: 'Logo',
      opacity: 1,
      transform: { rotation: 0, scaleX: 1, scaleY: 1, x: 0, y: 0 },
      type: 'group',
    },
  ],
  width: 1200,
}
