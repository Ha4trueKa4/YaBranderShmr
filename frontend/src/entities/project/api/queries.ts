import { useQuery } from '@tanstack/react-query';

import { getProject } from './getProject';

export const projectQueryKey = (id: string) => ['project', id] as const;

export function useProjectQuery(id: string) {
    return useQuery({
        queryKey: projectQueryKey(id),
        queryFn: () => getProject(id),
        enabled: Boolean(id),
        retry: false,
    });
}
