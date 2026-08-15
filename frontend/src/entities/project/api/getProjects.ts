import type { Project } from '@shared/api/types'

const mockProjects: Project[] = [
  {
    created_at: '2026-08-11T09:00:00Z',
    id: 'orion-labs',
    is_public: true,
    name: 'Orion Labs',
  },
  {
    created_at: '2026-08-12T09:00:00Z',
    id: 'nord-coffee',
    is_public: false,
    name: 'Nord Coffee',
  },
  {
    created_at: '2026-08-06T09:00:00Z',
    id: 'field-notes',
    is_public: true,
    name: 'Field Notes',
  },
  {
    created_at: '2026-08-13T09:00:00Z',
    id: 'unpublished-mark',
    is_public: false,
    name: 'Unpublished mark',
  },
  {
    created_at: '2026-08-10T09:00:00Z',
    id: 'nova-studio',
    is_public: true,
    name: 'Nova Studio',
  },
  {
    created_at: '2026-08-09T09:00:00Z',
    id: 'kite-finance',
    is_public: true,
    name: 'Kite Finance',
  },
  {
    created_at: '2026-08-08T09:00:00Z',
    id: 'echo-market',
    is_public: false,
    name: 'Echo Market',
  },
  {
    created_at: '2026-08-07T09:00:00Z',
    id: 'form-and-field',
    is_public: true,
    name: 'Form & Field',
  },
  {
    created_at: '2026-08-05T09:00:00Z',
    id: 'pulse-summit',
    is_public: true,
    name: 'Pulse Summit',
  },
  {
    created_at: '2026-08-04T09:00:00Z',
    id: 'moss-and-co',
    is_public: false,
    name: 'Moss & Co',
  },
  {
    created_at: '2026-08-03T09:00:00Z',
    id: 'paper-plane',
    is_public: true,
    name: 'Paper Plane',
  },
  {
    created_at: '2026-08-02T09:00:00Z',
    id: 'studio-north',
    is_public: false,
    name: 'Studio North',
  },
]

export async function getProjects(): Promise<Project[]> {
  // TODO: Replace mock data with GET /projects when the endpoint is available.
  // return api<Project[]>('/projects')
  return mockProjects
}
