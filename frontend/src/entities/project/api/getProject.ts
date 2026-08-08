import { api } from '@shared/api/api';
import { zGetProjectsByIdResponse, type Project } from '@shared/api/types';

export async function getProject(id: string): Promise<Project> {
    const response = await api<unknown>(`/projects/${encodeURIComponent(id)}`);

    return zGetProjectsByIdResponse.parse(response);
}
