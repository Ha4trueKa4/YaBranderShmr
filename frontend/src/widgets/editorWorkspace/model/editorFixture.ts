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
          fill: '#FFFFFF',
          id: 'background',
          isVisible: true,
          name: 'Background',
          type: 'background',
        },
        {
          fill: '#7C3AED',
          id: 'icon-shape',
          isVisible: true,
          name: 'Icon shape',
          stroke: '#06B6D4',
          type: 'shape',
          x: 120,
          y: 140,
        },
        {
          fontFamily: 'Inter',
          fontSize: 48,
          id: 'company-name',
          isVisible: true,
          name: 'Company Name',
          text: 'Orion Labs',
          type: 'text',
        },
        {
          fontFamily: 'Inter',
          fontSize: 24,
          id: 'tagline',
          isVisible: true,
          name: 'Tagline',
          text: 'Build better together',
          type: 'text',
        },
      ],
      id: 'logo',
      isVisible: true,
      name: 'Logo',
      type: 'group',
    },
  ],
  width: 1200,
}
